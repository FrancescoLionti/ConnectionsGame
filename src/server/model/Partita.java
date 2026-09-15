package server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rappresenta una singola istanza di una partita (in corso o terminata).
 * Gestisce il tempo a disposizione, la struttura del gioco da indovinare, le
 * statistiche globali della sessione e lo stato individuale di ciascun
 * giocatore.
 */
public class Partita {

    private final long durataPartitaMs; // Durata massima della partita in millisecondi
    private final int idPartita;  // Identificativo univoco globale della partita
    private final long timestampInizio;          // quando è iniziata questa partita
    private final Game gameDefinition;            // Struttura del gioco (soluzioni, gruppi e parole originali)
    private final List<String> paroleMescolate;  // Le 16 parole mescolate da inviare ai client
    private boolean terminata; // indica se il tempo è scaduto 
    private int playerTerminati; // contatore dei giocatori che hanno concluso il loro gioco
    private int vincitori; // Contatore dei giocatori che hanno indovinato tutti i gruppi
    private double punteggioMedio;  // Punteggio medio calcolato a fine partita

    // Mappa thread-safe che associa l'username di ogni partecipante al suo stato di gioco corrente
    private final ConcurrentHashMap<String, PlayerGameState> statiGiocatori;

    /**
     * Inizializza una nuova partita, fissa il tempo di inizio e prepara le
     * parole mescolate.
     *
     * @param game La configurazione dei gruppi e delle parole da indovinare.
     * @param idPartitaGlobale ID univoco per tracciare la partita.
     * @param durataPartita Durata concessa in secondi.
     */
    public Partita(Game game, int idPartitaGlobale, int durataPartita) {
        this.gameDefinition = game;
        this.statiGiocatori = new ConcurrentHashMap<>();
        this.timestampInizio = System.currentTimeMillis();
        this.durataPartitaMs = durataPartita * 1000L;
        this.idPartita = idPartitaGlobale;
        terminata = false;
        playerTerminati = 0;
        vincitori = 0;
        punteggioMedio = 0.0;

        // estrae le parole dalla definizione del gioco e le mescola casualmente per i client
        this.paroleMescolate = new ArrayList<>(game.getWords());
        Collections.shuffle(this.paroleMescolate);
    }

    /**
     * @return L'intera mappa degli stati dei giocatori.
     */
    public ConcurrentHashMap<String, PlayerGameState> getState() {
        return statiGiocatori;
    }

    /**
     * Verifica se un determinato utente sta partecipando a questa partita.
     *
     * @param username nome dell'utente da cercare
     * @return true se l'utente sta partecipando, false altrimenti
     */
    public boolean playerInState(String username) {
        return statiGiocatori.containsKey(username);
    }

    /**
     * @return Una lista contenente gli username di tutti i partecipanti.
     */
    public List<String> getPlayersName() {
        return new ArrayList<>(statiGiocatori.keySet());
    }

    /**
     * Recupera lo stato di gioco di un utente. Se l'utente non è ancora
     * presente nella mappa, ne crea uno nuovo e lo inserisce.
     */
    public PlayerGameState getStatePlayer(String username) {
        return statiGiocatori.computeIfAbsent(username, u -> new PlayerGameState());
    }

    /**
     * @return Una copia della lista delle parole mescolate
     */
    public List<String> getParole() {
        return new ArrayList<>(paroleMescolate);
    }

    public Game getGameDefinition() {
        return gameDefinition;
    }

    /**
     * Restituisce le soluzioni del gioco sotto forma di lista di gruppi
     * tematici.
     *
     * @return La lista contenente i raggruppamenti logici.
     */
    public List<Group> getSoluzioniGruppi() {
        return gameDefinition.getGroups();
    }

    /**
     * Calcola il tempo rimanente prima della fine della partita.
     *
     * @return Millisecondi rimanenti (non scende mai sotto lo 0).
     */
    public long getTempoRimasto() {
        long adesso = System.currentTimeMillis();
        long fine = timestampInizio + durataPartitaMs;
        long rimasto = fine - adesso;
        return Math.max(rimasto, 0);  // mai negativo
    }

    /**
     * @return true se la partita e' terminata, false altrimenti r
     */
    public boolean getIsTerminata() {
        return terminata;
    }

    /**
     * Segna la partita come conclusa e calcola il punteggio medio di tutti i
     * partecipanti.
     */
    public void setTerminata() {
        aggiornaPunteggioMedio();
        this.terminata = true;
    }

    public long getDurata() {
        return durataPartitaMs;
    }

    /**
     * @return Il numero totale di giocatori attualmente registrati in questa
     * partita.
     */
    public int getPartecipanti() {
        Collection<PlayerGameState> tuttiStati = statiGiocatori.values();
        int numGiocatori = tuttiStati.size();
        return numGiocatori;
    }

    public int getPlayerTerminati() {
        return playerTerminati;
    }

    public void incrementaPlayerTerminati() {
        this.playerTerminati++;
    }

    public int getVincitori() {
        return vincitori;
    }

    public void incrementaVincitori() {
        this.vincitori++;
    }

    /**
     * Ricalcola il punteggio medio basandosi sui punti accumulati da ciascun
     * giocatore.
     */
    public void aggiornaPunteggioMedio() {

        Collection<PlayerGameState> tuttiStati = statiGiocatori.values();
        int numGiocatori = tuttiStati.size();

        int punteggioTot = 0;
        for (PlayerGameState state : tuttiStati) {
            punteggioTot += state.getPunteggio();
        }

        if (numGiocatori > 0) {
            this.punteggioMedio = (double) punteggioTot / numGiocatori;
        }

    }

    public double getPunteggioMedio() {
        return punteggioMedio;
    }

    public int getIdPartita() {
        return idPartita;
    }

    /**
     * Permette di aggiornare la chiave (username) di un giocatore all'interno
     * della mappa nel caso in cui cambi nome durante l'esecuzione della
     * partita.
     *
     * @param oldUsername L'username precedente.
     * @param newUsername Il nuovo username.
     */
    public void renamePlayer(String oldUsername, String newUsername) {
        // remove() toglie la vecchia chiave e restituisce l'oggetto PlayerGameState
        PlayerGameState stato = statiGiocatori.remove(oldUsername);

        // Se il giocatore era nella partita, viene reinserito con il nuovo nome
        if (stato != null) {
            statiGiocatori.put(newUsername, stato);
        }
    }

}
