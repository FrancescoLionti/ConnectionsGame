package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import protocol.request.LoginRequest;
import protocol.request.LogoutRequest;
import protocol.request.RegisterRequest;
import protocol.request.RequestConfigUDP;
import protocol.request.RequestGameInfoRequest;
import protocol.request.RequestGameStatsRequest;
import protocol.request.RequestLeaderboardRequest;
import protocol.request.RequestPlayerStatsRequest;
import protocol.request.SubmitProposalRequest;
import protocol.request.UpdateCredentialRequest;
import protocol.response.EndGameResponse;
import protocol.response.GameInfoResponse;
import protocol.response.GameStatsResponse;
import protocol.response.LeaderboardResponse;
import protocol.response.LoginResponse;
import protocol.response.PlayerStatsResponse;
import protocol.response.ProposalResponse;
import protocol.response.Response;
import protocol.response.ResponseCodes;
import server.model.Game;
import server.model.GameHistoryDB;
import server.model.Group;
import server.model.Partita;
import server.model.PlayerGameState;
import server.model.UserStats;

/**
 * Controller centrale del server . È l'unica istanza condivisa che gestisce lo
 * stato di gioco globale, i login degli utenti, le modifiche ai database e
 * calcola gli esiti delle proposte. I metodi pubblici esposti ai ServerWorker
 * sono `synchronized` (o agiscono su strutture thread-safe) per garantire
 * l'accesso concorrente senza race condition.
 */
public class ServerController {

    private UserManager utentiRegistrati;
    private ConcurrentHashMap<String, Boolean> utentiOnline; // Mappa thread-safe degli utenti attualmente loggati
    private Partita partitaCorrente;
    private GameProvider gameProvider;
    private ExecutorService timerGameMenager; // avvia e termine le partite in corso, allo scadere del tempo
    private GameHistoryManager historyGameManager;
    private RankingsManager rankingsManager;
    private UdpNotificationManager udpManager;
    private int durataPartita;  // Durata prefissata per ogni partita in millisecondi
    private static final Gson gson = new Gson();

    /**
     * Inizializza il controller caricando i vari file di persistenza e avviando
     * il timer di gioco.
     */
    public ServerController(int durataPartita, String gameHistoryFile, String rankingFilePath, String userFilePath, String ConnectionsDataPath) {
        this.utentiRegistrati = new UserManager(userFilePath);
        this.utentiOnline = new ConcurrentHashMap<>();
        this.historyGameManager = new GameHistoryManager(gameHistoryFile);
        this.rankingsManager = new RankingsManager(rankingFilePath);
        this.durataPartita = durataPartita;
        timerGameMenager = Executors.newSingleThreadExecutor();

        try {
            gameProvider = new GameProvider(ConnectionsDataPath);
            timerGameMenager.execute(new GameTimerManager()); // avvio immediato del ciclo di partite

        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.exit(1);
        }

    }

    /**
     * Runnable interno che rappresenta il "ciclo di vita" temporale continuo
     * del server. Scandisce la durata di ogni singola partita e, allo scadere
     * del timer, ne forza la terminazione.
     */
    private class GameTimerManager implements Runnable {

        @Override
        public void run() {

            avviaNuovaPartita();  // Avvia la prima partita al boot del server

            // Continua in un ciclo finché il thread (o il server) non viene interrotto
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Mette in sleep il thread per la durata esatta della partita
                    Thread.sleep(partitaCorrente.getDurata());
                    // Al risveglio, il tempo è scaduto: la partita in corso viene terminata
                    terminaPartitaAutomatica();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("GameTimerManager terminato");
        }
    }

    /**
     * Setter per inizializzare l'udpManager
     *
     * @param udpManager gestore per le notifiche UDP asincrone
     */
    public void setUdpManager(UdpNotificationManager udpManager) {
        this.udpManager = udpManager;
    }

    /**
     * Crea e avvia una nuova partita globale attingendo al dizionario del
     * GameProvider. È synchronized per evitare che venga interrotto da
     * modifiche concorrenti alla partita corrente.
     */
    public synchronized void avviaNuovaPartita() {
        try {
            if (gameProvider != null) {
                Game nextGame = gameProvider.getNextGame(); // Estrae un "seed" di partita dal JSON

                if (nextGame != null) {
                    // Calcolo dell'ID globale incrementale basato sullo storico delle partite concluse
                    int nuovoId = historyGameManager.getPartiteConcluse().size() + 1;

                    // Inizializza la partita 
                    this.partitaCorrente = new Partita(nextGame, nuovoId, durataPartita);

                    System.out.println("Nuova partita avviata: ID GLOBALE " + nuovoId + " (Seed orginale: " + nextGame.getGameId() + ")");
                } else {
                    System.err.println("CRITICO: File partite vuoto!");
                }
            }
        } catch (IOException e) {
            System.err.println("Errore caricamento partita: " + e.getMessage());
        }
    }

    /**
     * Termina la partita in corso, storicizza i risultati, aggiorna le
     * classifiche e notifica in broadcast tutti i giocatori tramite UDP, prima
     * di avviare il prossimo match.
     */
    private synchronized void terminaPartitaAutomatica() {
        System.out.println(partitaCorrente);
        if (partitaCorrente != null && !partitaCorrente.getIsTerminata()) {
            System.out.println("tempo della partita SCADUTO!");
            partitaCorrente.setTerminata();
            System.out.println("Partita terminata!");
            historyGameManager.archiviaPartita(partitaCorrente);
            System.out.println("partita salvata nel DB. Avvio nuova...");
            Map<String, PlayerGameState> classificaConclusa = partitaCorrente.getState();
            aggiornaClassificaPartita();
            System.out.println("classifica salvata nel DB. Avvio nuova Partita...");
            avviaNuovaPartita();
            // Crea e invia il pacchetto UDP di broadcast con i risultati della partita appena conclusa
            EndGameResponse messaggioFinePartita = new EndGameResponse(classificaConclusa, partitaCorrente.getParole());
            udpManager.broadcastNotification(gson.toJson(messaggioFinePartita));
        }

    }

    /**
     * Itera su tutti i giocatori che hanno preso parte alla partita conclusa e
     * aggiorna le loro statistiche nel file delle classifiche globali.
     */
    private void aggiornaClassificaPartita() {
        for (String username : partitaCorrente.getPlayersName()) {
            PlayerGameState stato = partitaCorrente.getStatePlayer(username);

            boolean haVinto = stato.isVittoria();
            int punteggioFinale = stato.getPunteggio();
            int erroriFinali = stato.getErrori();
            // Aggiorna la classifica globale persistente
            rankingsManager.updateScore(username, punteggioFinale, haVinto, erroriFinali);
        }

        System.out.println("Classifica aggiornata per partita " + partitaCorrente.getIdPartita());
    }

    /**
     * Da richiamare durante lo shutdown del server. Chiude il timer, salva
     * l'ultima partita (anche se a metà) e i file JSON aperti.
     */
    public void chiudiProvider() {
        timerGameMenager.shutdownNow();
        try {
            if (!timerGameMenager.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Timer non terminato!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (partitaCorrente != null && !partitaCorrente.getIsTerminata()) {
            terminaPartitaAutomatica();  // Salva l'ultima partita pendente
        }
        historyGameManager.saveGames();
        try {
            if (gameProvider != null) {
                gameProvider.close();
            }
            // Richiama la chiusura del gestore UDP 
            if (udpManager != null) {
                udpManager.close();
            }
        } catch (IOException e) {
            System.err.println("Errore chiusura provider");
        }
    }

    /**
     * Disconnette forzatamente un utente rimuovendolo dalla lista degli online.
     * Viene solitamente invocato quando si verifica una disconnessione anomala
     * (es. caduta della connessione TCP o crash del client).
     *
     * @param username L'username dell'utente da disconnettere. Se null,
     * l'operazione viene ignorata.
     */
    public synchronized void disconnettiUtente(String username) {
        if (username != null) {
            utentiOnline.remove(username);
            System.out.println(" Clean-up sessione per: " + username);
        }
    }

    /**
     * Rimuove il client specificato dal gestore delle notifiche UDP. Da
     * invocare quando il client si scollega o effettua il logout, per evitare
     * l'invio di datagrammi a una socket ormai chiusa.
     *
     * @param clientSocket Il socket TCP associato al client che deve essere
     * disiscritto dalle notifiche UDP.
     */
    public synchronized void rimuoviClientUDP(Socket clientSocket) {
        if (udpManager != null && clientSocket != null) {
            udpManager.unregisterClient(clientSocket);
        }
    }

    /**
     * Gestisce la richiesta di registrazione di un nuovo utente. Verifica la
     * validità dell'input e l'assenza di duplicati. In caso di successo, salva
     * immediatamente i dati tramite lo UserManager.
     *
     * @param req richiesta fatta dal client
     * @return ritorna la risposta del tipo corretto in base alla richiesta
     * effettuata
     */
    public synchronized Response handleRegister(RegisterRequest req) {

        // Validazione Input: impedisce username o password nulli o vuoti
        if (req.username == null || req.username.isEmpty() || req.psw == null || req.psw.isEmpty()) {
            return new Response("register", ResponseCodes.BAD_REQUEST, ResponseCodes.getDefaultMessage(ResponseCodes.BAD_REQUEST));
        }

        // Controllo duplicati: verifica se lo username esiste già nel database
        if (utentiRegistrati.exists(req.username)) {
            return new Response("register", ResponseCodes.CONFLICT, ResponseCodes.getDefaultMessage(ResponseCodes.CONFLICT));
        }

        // Registrazione e Persistenza
        utentiRegistrati.register(req.username, req.psw);

        return new Response("register", ResponseCodes.OK, ResponseCodes.getDefaultMessage(ResponseCodes.OK));
    }

    /**
     * Gestisce la richiesta di login di un utente. Verifica le credenziali e,
     * se corrette, inserisce il giocatore nella partita in corso, recuperando e
     * restituendo lo stato attuale di gioco (parole rimanenti, errori,
     * punteggio, tempo).
     *
     * @param req La richiesta di login inviata dal client contenente username e
     * password.
     * @return Una LoginResponse contenente l'esito (es. OK, UNAUTHORIZED) e i
     * dati della partita se il login ha successo.
     */
    public synchronized Response handleLogin(LoginRequest req) {
        System.out.println("!! Login richiesto: " + req.username);

        // Validazione nome
        if (!utentiRegistrati.exists(req.username)) {
            return new LoginResponse(ResponseCodes.UNAUTHORIZED, ResponseCodes.getDefaultMessage(ResponseCodes.UNAUTHORIZED));
        }

        // Validazione password
        if (!utentiRegistrati.checkPassword(req.username, req.psw)) {
            return new LoginResponse(ResponseCodes.UNAUTHORIZED, ResponseCodes.getDefaultMessage(ResponseCodes.UNAUTHORIZED));
        }

        // Evita accessi multipli contemporanei per lo stesso utente
        if (utentiOnline.containsKey(req.username)) {
            return new LoginResponse(ResponseCodes.CONFLICT, "Utente gia loggato");
        }

        // Login completato con successo
        utentiOnline.put(req.username, true);

        //  Recupero stato personale del giocatore all'interno della partita corrente
        PlayerGameState statoGiocatore = partitaCorrente.getStatePlayer(req.username);
        List<String> parole = partitaCorrente.getParole();
        int errori = statoGiocatore.getErrori();
        int punteggio = statoGiocatore.getPunteggio();
        long tempoRimasto = partitaCorrente.getTempoRimasto();
        List<List<String>> gruppiIndovinati = statoGiocatore.getGruppiIndovinati();

        // rsposta: ritorna al client tutte le informazioni per poter giocare
        return new LoginResponse(
                ResponseCodes.OK,
                Integer.toString(partitaCorrente.getIdPartita()),
                parole,
                gruppiIndovinati,
                errori,
                punteggio,
                tempoRimasto
        );
    }

    /**
     * Gestisce la richiesta logout di un utente. Verifica che l'utente sia
     * effettivamente loggato e, in caso affermativo, lo rimuove dalla mappa
     * degli utenti attualmente online.
     *
     * @param req La richiesta di logout inviata dal client.
     * @param usernameLoggato L'username attualmente associato al worker.
     * @return Una Response contenente l'esito dell'operazione (OK se successo,
     * UNAUTHORIZEDLOG se non era loggato).
     */
    public synchronized Response handleLogout(LogoutRequest req, String usernameLoggato) {
        // Se il server riceve logout ma l'utente non risultava loggato nel worker
        if (usernameLoggato == null) {
            return new Response("logout", ResponseCodes.UNAUTHORIZEDLOG, ResponseCodes.getDefaultMessage(ResponseCodes.UNAUTHORIZEDLOG));
        }

        utentiOnline.remove(usernameLoggato);
        return new Response("logout", ResponseCodes.OK, ResponseCodes.getDefaultMessage(ResponseCodes.OK));
    }

    /**
     * Gestisce l'aggiornamento delle credenziali di un utente (username,
     * password o entrambi). Si occupa di mantenere la consistenza dell'account
     * propagando la modifica in tutte le strutture dati collegate: DB utenti,
     * statistiche (classifica), stato della sessione online e stato all'interno
     * della partita corrente.
     *
     * @param req La richiesta contenente le vecchie credenziali per
     * l'autenticazione e i nuovi dati da impostare.
     * @return Una Response che indica se l'aggiornamento è andato a buon fine
     * (OK) oppure i motivi del fallimento (es. BAD_REQUEST, UNAUTHORIZED,
     * CONFLICT).
     */
    public synchronized Response handleUpdateCredential(UpdateCredentialRequest req) {

        // Validazione input
        if (req.oldUsername == null || req.oldUsername.isEmpty()
                || req.oldPsw == null || req.oldPsw.isEmpty()
                || (req.newUsername == null || req.newUsername.isEmpty())
                && (req.newPsw == null || req.newPsw.isEmpty())) {
            return new Response("updateCredentials", ResponseCodes.BAD_REQUEST, ResponseCodes.getDefaultMessage(ResponseCodes.BAD_REQUEST));
        }

        // Validazione input: assicura che ci siano i vecchi dati e almeno un nuovo dato da aggiornare
        if (!utentiRegistrati.exists(req.oldUsername)) {
            return new Response("updateCredentials", ResponseCodes.UNAUTHORIZED, "NomeUtente attuale inserito non valido");
        }

        if (!utentiRegistrati.checkPassword(req.oldUsername, req.oldPsw)) {
            return new Response("updateCredentials", ResponseCodes.UNAUTHORIZED, "Password attuale errata");
        }

        // check se newUsername è già usato da qualcun altro 
        if (req.newUsername != null && !req.newUsername.equals(req.oldUsername)) {
            if (utentiRegistrati.exists(req.newUsername)) {
                return new Response("updateCredentials", ResponseCodes.CONFLICT, "Nuovo username già in uso");
            }
        }

        // rimuove le vecchie credenziali e inserisce quelle nuove dal db deiregistrati
        utentiRegistrati.deleteUser(req.oldUsername);
        utentiRegistrati.register(req.newUsername, req.newPsw);

        // rimuove il vecchio nome utente e inserisce quello nuovo nella classifica
        // la classifica viene scritta ogni volta che finisce la partita quindi 
        //potrebbe non esserci il vecchio nome utente, quindi controllo player!=null
        UserStats player = rankingsManager.getPlayer(req.oldUsername);
        if (player != null) {
            player.username = req.newUsername;
            rankingsManager.getRanking().remove(req.oldUsername);
            rankingsManager.getRanking().put(req.newUsername, player);
            rankingsManager.saveRankings();
        }

        // rimuove il vecchio nome utente ed inserisce quello nuovo dagli utenti online (se l'utente sta facendo la modifica mentre è loggato)
        if (utentiOnline.containsKey(req.oldUsername)) {
            utentiOnline.remove(req.oldUsername);
            utentiOnline.put(req.newUsername, true);
        }

        // aggiorna il nome del player, nella partita corrente
        if (partitaCorrente != null && partitaCorrente.playerInState(req.oldUsername)) {
            partitaCorrente.renamePlayer(req.oldUsername, req.newUsername);
        }

        //Aggiorna il nome nello storico delle partite concluse
        historyGameManager.renamePlayerInHistory(req.oldUsername, req.newUsername);

        return new Response("updateCredentials", ResponseCodes.OK, ResponseCodes.getDefaultMessage(ResponseCodes.OK));
    }

    /**
     * Valuta una proposta di raggruppamento di parole, inviata da un utente per
     * la partita in corso. Esegue prima una serie di controlli formali: se la
     * proposta è malformata, viene rifiutata senza penalità per il giocatore.
     * Se è valida, viene confrontata con le soluzioni: - Se corretta: +6 punti.
     * Al terzo gruppo indovinato, scatta la vittoria automatica. - Se errata:
     * -4 punti e incremento degli errori. Al quarto errore, scatta la
     * sconfitta.
     *
     * @param req La richiesta contenente la lista delle 4 parole proposte dal
     * giocatore.
     * @param username L'username del giocatore che ha inviato la proposta.
     * @return Una Response che indica se la proposta era malformata
     * (BAD_REQUEST) o una ProposalResponse contenente l'esito della giocata
     * (corretta/errata), il punteggio aggiornato e gli eventuali flag di
     * vittoria/sconfitta.
     */
    public synchronized Response HandleSubmitProposalRequest(SubmitProposalRequest req, String username) {
        List<String> inWords = req.words;
        PlayerGameState statoGiocatore = partitaCorrente.getStatePlayer(username);
        Game gruppiDaIndovinare = partitaCorrente.getGameDefinition();

        // trasforma tutte le parole in maiuscolo per effettuare confronti case-insensitive
        List<String> inputWords = new ArrayList<>();
        for (String elem : inWords) {
            inputWords.add(elem.toUpperCase());
        }

        // Verifica condizioni di stato terminale: se l'utente ha già perso o vinto, non può più giocare
        if (statoGiocatore.isSconfitta()) {
            return new Response("submitProposal", ResponseCodes.LOSE_ALLERT, "Hai fornito 4 proposte sbagliate, devi aspettare il termine della partita in corso per poter giocare ");
        }

        if (statoGiocatore.isVittoria()) {
            return new Response("submitProposal", ResponseCodes.WINNER_ALLERT, "Hai indovinato tutti i gruppi, devi aspettare il termine della partita in corso per poter giocare");
        }

        // Controllo univocità: le parole devono essere tutte distinte
        if (inputWords.stream().distinct().count() != 4) {
            return new Response("submitProposal", ResponseCodes.BAD_REQUEST, "Le parole devono essere tutte distinte");
        }

        // tutte appartenenti all’elenco delle 16 parole della partita
        if (!partitaCorrente.getGameDefinition().getWords().containsAll(inputWords)) {
            return new Response("submitProposal", ResponseCodes.BAD_REQUEST, "Una o più parole non fanno parte della partita");
        }

        // nessuna parola già in un gruppo corretto di questo utente
        for (List<String> gruppo : statoGiocatore.getGruppiIndovinati()) {
            for (String w : inputWords) {
                if (gruppo.contains(w)) {
                    return new Response("submitProposal", ResponseCodes.BAD_REQUEST, "Parole già usate in un gruppo corretto");
                }
            }
        }

        //Resposte a richieste corrette:
        //Verifica correttezza logica: le 4 parole coincidono con uno dei gruppi previsti
        boolean gruppoCorretto = false;
        for (Group gruppo : gruppiDaIndovinare.getGroups()) {
            List<String> paroleGruppo = gruppo.getWords();
            if (paroleGruppo.containsAll(inputWords) && inputWords.containsAll(paroleGruppo)) {
                gruppoCorretto = true;
                // se questo gruppo non è ancora stato segnato per l’utente, aggiungilo
                if (!statoGiocatore.getGruppiIndovinati().contains(paroleGruppo)) {
                    statoGiocatore.getGruppiIndovinati().add(paroleGruppo);
                }
                break;
            }
        }

        //gestione del punteggio
        int punteggio = statoGiocatore.getPunteggio();
        int numIndovinati = statoGiocatore.getGruppiIndovinati().size();
        int numErrori = statoGiocatore.getErrori();

        if (gruppoCorretto) {
            punteggio += 6;
        } else {
            punteggio -= 4;
            numErrori++;
        }

        if (numIndovinati == 3) {
            // ultimo gruppo è implcito per esclusione: l'utente vince la partita
            statoGiocatore.setVittoria(true);
            partitaCorrente.incrementaVincitori();
            partitaCorrente.incrementaPlayerTerminati();
        }
        if (numErrori == 4) {
            // Raggiunto il limite massimo di errori consentiti: l'utente perde
            statoGiocatore.setSconfitta(true);
            partitaCorrente.incrementaPlayerTerminati();
        }

        // aggiorna lo stato dell'utente in memoria
        statoGiocatore.setPunteggio(punteggio);
        statoGiocatore.setErrori(numErrori);

        // risponde con l'esito della proposta e i nuovi dati aggiornati
        return new ProposalResponse(ResponseCodes.OK, "Esito proposta", statoGiocatore.isVittoria(), statoGiocatore.isSconfitta(), punteggio, gruppoCorretto);
    }

    /**
     * Gestisce la richiesta di statistiche generali per una specifica partita.
     * Le informazioni restituite cambiano in base allo stato della partita: -
     * Se è la partita in corso, restituisce dati "live" (partecipanti,
     * vincitori, chi ha terminato, tempo residuo). - Se è una partita storica
     * conclusa, restituisce i dati finali consolidati, includendo il punteggio
     * medio.
     *
     * @param req La richiesta del client, contenente un flag booleano per
     * indicare se si vuole la partita in corso e l'ID della partita desiderata.
     * @return Una GameStatsResponse con le statistiche richieste, oppure una
     * Response di errore (es. NOT_FOUND) se l'ID specificato non esiste nello
     * storico.
     */
    public synchronized Response handleRequestGameStatsRequest(RequestGameStatsRequest req) {

        // Caso 1: L'utente ha richiesto le statistiche della partita attualmente in corso
        if (req.inCorso) {
            int vincitori = partitaCorrente.getVincitori();
            int terminati = partitaCorrente.getPlayerTerminati();
            int partecipanti = partitaCorrente.getPartecipanti();
            long tempoRimanente = partitaCorrente.getTempoRimasto();
            int giocatoriInCorso = partecipanti - terminati;
            return new GameStatsResponse(ResponseCodes.OK, "Ecco i dati relativi alla partita attuale", tempoRimanente, giocatoriInCorso, terminati, vincitori);
        }

        // Caso 2: L'utente ha richiesto le statistiche di una partita passata
        GameHistoryDB partiteConcluse = historyGameManager.getPartiteConcluse();
        String idRichiesto = Integer.toString(req.gameId);
        if (partiteConcluse.containsKey(idRichiesto)) {
            Partita partitaRichiesta = partiteConcluse.get(idRichiesto);

            int vincitori = partitaRichiesta.getVincitori();
            int terminati = partitaRichiesta.getPlayerTerminati();
            int partecipanti = partitaRichiesta.getPartecipanti();
            double punteggioMedio = partitaRichiesta.getPunteggioMedio();
            return new GameStatsResponse(ResponseCodes.OK, idRichiesto, partecipanti, terminati, vincitori, punteggioMedio);
        }

        // Se l'ID non è stato trovato nello storico, restituisce una Risposta con errore
        return new Response("requestGameStats", ResponseCodes.NOT_FOUND, "Codice id della partita non esiste");

    }

    /**
     * Gestisce la richiesta di informazioni per una specifica partita. Le
     * informazioni restituite differiscono in base allo stato della partita: -
     * Partita in corso: fornisce lo stato personale del giocatore (tempo
     * residuo, gruppi indovinati, errori, punteggio) e aggiorna la lista delle
     * parole ancora disponibili sottraendo quelle dei gruppi già individuati. -
     * Partita conclusa (storico): fornisce la soluzione completa (i 4
     * raggruppamenti esatti) e le statistiche personali ottenute in quel match,
     * ma solo se l'utente vi aveva effettivamente partecipato.
     *
     * @param req La richiesta del client, che specifica se si desiderano i dati
     * della partita corrente o un ID passato.
     * @param username L'username del giocatore richiedente.
     * @return Una GameInfoResponse con le info di dettaglio oppure una Response
     * d'errore (es. NOT_FOUND se ID inesistente o utente non partecipante).
     */
    public synchronized Response handleRequestGameInfoRequest(RequestGameInfoRequest req, String username) {

        // Caso 1: L'utente richiede le informazioni sulla partita attualmente in svolgimento
        if (req.inCorso) {
            PlayerGameState statoGiocatore = partitaCorrente.getStatePlayer(username);
            long tempoRimanente = partitaCorrente.getTempoRimasto();
            int numErrori = statoGiocatore.getErrori();
            int punteggio = statoGiocatore.getPunteggio();
            List<List<String>> gruppiIndovinati = statoGiocatore.getGruppiIndovinati();

            // Crea una lista con tutte le 16 parole iniziali
            List<String> paroleRimanenti = partitaCorrente.getParole();
            // Filtra le parole: rimuove quelle che l'utente ha già indovinato
            // per far sì che il client mostri a video solo quelle ancora disponibili
            for (List<String> gruppo : gruppiIndovinati) {
                paroleRimanenti.removeAll(gruppo);
            }

            return new GameInfoResponse(ResponseCodes.OK, "Ecco  le tue info relative alla partita attuale", false, gruppiIndovinati, paroleRimanenti, punteggio, numErrori, tempoRimanente);
        } else {
            // Caso 2: L'utente richiede informazioni su una partita passata presente nello storico
            GameHistoryDB partiteConcluse = historyGameManager.getPartiteConcluse();
            String idRichiesto = Integer.toString(req.gameId);
            // Controlla che l'ID fornito esista effettivamente nello storico delle partite terminate
            if (partiteConcluse.containsKey(idRichiesto)) {
                Partita partitaRichiesta = partiteConcluse.get(idRichiesto);
                List<Group> soluzioni = partitaRichiesta.getSoluzioniGruppi();
                // le specifiche richiedono che solo chi ha giocato alla partita puo vedere i risultati
                if (partitaRichiesta.playerInState(username)) {
                    PlayerGameState statoGiocatore = partitaRichiesta.getStatePlayer(username);
                    int numProposteCorrette = statoGiocatore.getGruppiIndovinati().size();
                    int numErrori = statoGiocatore.getErrori();
                    int punteggio = statoGiocatore.getPunteggio();
                    return new GameInfoResponse(ResponseCodes.OK, idRichiesto, true, soluzioni, numProposteCorrette, punteggio, numErrori);
                } else {
                    // L'utente esiste, la partita esiste, ma l'utente non vi ha preso parte
                    return new Response("requestGameInfo", ResponseCodes.NOT_FOUND, "Non hai partecipato alla partita ID: " + partitaRichiesta.getIdPartita());
                }
            }
        }
        // è stato richiesto un ID partita storico non presente nel DB
        return new Response("requestGameInfo", ResponseCodes.NOT_FOUND, "Codice id della partita non esiste");
    }

    /**
     * Gestisce la richiesta per visualizzare la classifica dei giocatori
     * (Leaderboard). Il metodo si adatta in base ai parametri della richiesta,
     * gestendo tre scenari: 1. Top K: restituisce solo i migliori K giocatori
     * (se topPlayers > 0). 2. Globale: restituisce la classifica completa di
     * tutti gli iscritti. 3. Relativa: restituisce il punteggio e la posizione
     * assoluta di un singolo utente specifico.
     *
     * @param req La richiesta del client contenente i flag che indicano il tipo
     * di classifica desiderata (globale, top K, o nome utente).
     * @return Una LeaderboardResponse con i dati richiesti, o una Response di
     * errore se il singolo utente cercato non esiste.
     */
    public synchronized Response handleRequestLeaderboardRequest(RequestLeaderboardRequest req) {
        //  L'utente ha richiesto una classifica globale (anziché la posizione di un singolo giocatore)
        if (req.global) {
            // L'utente vuole solo i primi K giocatori (Top Players)
            if (req.topPlayers > 0) {
                return new LeaderboardResponse(ResponseCodes.OK, "Classifica top player", rankingsManager.getTopLeaderboard(req.topPlayers));
            }
            // Sotto-caso: L'utente vuole l'intera classifica generale di tutti i giocatori registrati
            return new LeaderboardResponse(ResponseCodes.OK, "Classifica globale", rankingsManager.getSortedLeaderboard());
        } else {
            // L'utente vuole conoscere esclusivamente la posizione di un giocatore specifico (es. se stesso)
            if (rankingsManager.playerInRank(req.playerName)) {
                return new LeaderboardResponse(ResponseCodes.OK, "Player posizione", rankingsManager.getPositionPlayer(req.playerName), req.playerName);
            }
            // Se il nome specificato non ha mai giocato o non esiste, ritorna un errore
            return new Response("requestLeaderboard", ResponseCodes.NOT_FOUND, "Utente non presente in classifica");
        }
    }

    /**
     * Gestisce la richiesta per visualizzare le statistiche personali e
     * storiche di un giocatore (es. Win Rate, Loss Rate, Current Streak, Max
     * Streak, e l'istogramma degli errori).
     *
     * @param req La richiesta inviata dal client.
     * @param username Il nome del giocatore (solitamente quello attualmente
     * loggato) di cui si vogliono vedere le statistiche.
     * @return Una PlayerStatsResponse contenente tutti i contatori e le
     * percentuali del giocatore, oppure una Response d'errore se l'utente non
     * ha ancora partecipato a nessuna partita.
     */
    public synchronized Response handleRequestPlayerStatsRequest(RequestPlayerStatsRequest req, String username) {
        // Verifica che l'utente esista nel database delle classifiche/statistiche
        if (rankingsManager.playerInRank(username)) {
            // Estrazione di tutti i parametri per la risposta strutturata
            UserStats statische = rankingsManager.getPlayer(username);
            int puzzlesCompleted = statische.puzzlesCompleted;
            double winRate = statische.winRate;
            double lossRate = statische.lossRate;
            int currentStreak = statische.currentStreak;
            int maxStreak = statische.maxStreak;
            int perfectPuzzles = statische.perfectPuzzles; // Partite vinte con 0 errori
            Map<Integer, Integer> mistakeHistogram = statische.mistakeHistogram;  // Mappa numero errori -> quantità di partite
            return new PlayerStatsResponse(ResponseCodes.OK, "Dati giocatore", puzzlesCompleted, winRate, lossRate, currentStreak, maxStreak, perfectPuzzles, mistakeHistogram);
        }
        // Se l'utente si è appena registrato ma non ha mai giocato, non sarà presente in classifica
        return new Response("requestLeaderboard", ResponseCodes.NOT_FOUND, "Utente non presente in classifica");
    }

    public synchronized Response handleConfigUDP(RequestConfigUDP req, Socket clientSocket) {
        if (udpManager == null) {
            return new Response("configUDP", ResponseCodes.INTERNAL_ERROR, "UDP Manager non attivo");
        }

        // rgistra il client usando il suo socket TCP attuale e la porta UDP dichiarata
        udpManager.registerClient(clientSocket, req.udpClientPort);

        return new Response("configUDP", ResponseCodes.OK, "Porta UDP registrata con successo");
    }

}
