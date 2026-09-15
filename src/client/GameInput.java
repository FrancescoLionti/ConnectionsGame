package client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

/**
 * Gestisce l'acquisizione degli input dell'utente da terminale e l'invio delle
 * rispettive richieste JSON al server.
 */
public class GameInput {

    private static final Gson gson = new Gson();

    /**
     * Verifica che una stringa in input sia convertibile in un intero
     * strettamente positivo.
     */
    public static boolean isInteroPositivo(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            int numero = Integer.parseInt(str.trim());
            return numero > 0;
        } catch (NumberFormatException e) {
            return false; // La stringa contiene lettere o non è un numero valido
        }
    }

    /**
     * Verifica che una stringa in input sia un numero intero valido.
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Invia la porta UDP associata a questo client verso il server.
     */
    public static void doPresentetionUdp(SocketChannel socket, int actualPortUDP) throws IOException {
        RequestConfigUDP udpMessage = new RequestConfigUDP(actualPortUDP);
        NioUtils.sendJson(socket, gson.toJson(udpMessage));
    }

    /**
     * Raccoglie username e password e invia la richiesta di registrazione.
     */
    public static void doRegister(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ REGISTRAZIONE UTENTE ════");
        System.out.print("  • Inserisci Username: ");
        String username = userIn.nextLine().trim();
        System.out.print("  • Inserisci Password: ");
        String password = userIn.nextLine().trim();

        RegisterRequest req = new RegisterRequest(username, password);
        NioUtils.sendJson(socket, gson.toJson(req));
    }

    /**
     * Raccoglie le credenziali dell'utente e invia la richiesta di login.
     */
    public static void doLogin(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ ACCESSO UTENTE ════");
        System.out.print("  • Username: ");
        String username = userIn.nextLine().trim();
        System.out.print("  • Password: ");
        String password = userIn.nextLine().trim();

        LoginRequest req = new LoginRequest(username, password);
        NioUtils.sendJson(socket, gson.toJson(req));
    }

    /**
     * Invia la richiesta di logout per la sessione corrente.
     */
    public static void doLogout(SocketChannel socket) throws IOException {
        LogoutRequest req = new LogoutRequest();
        NioUtils.sendJson(socket, gson.toJson(req));
    }

    /**
     * Raccoglie i dati necessari per aggiornare le credenziali, garantendo
     * tramite controlli ciclici che non vengano inseriti campi vuoti. E invia
     * la richiesta al server
     */
    public static void doUpdateCredentials(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ AGGIORNAMENTO CREDENZIALI ════");
        System.out.print("  • Username attuale: ");
        String username = userIn.nextLine().trim();
        System.out.print("  • Password attuale: ");
        String password = userIn.nextLine().trim();

        System.out.println("  ─────────────────────────");

        System.out.print("  • Nuovo Username:   ");
        String newUsername = userIn.nextLine().trim();

        System.out.print("  • Nuova Password:   ");
        String newPassword = userIn.nextLine().trim();

        while (newUsername.equals("") && newPassword.equals("")) {
            System.out.println("  • Attenzione: è necessario inserire almeno un nuovo Username o una nuova Password.");

            System.out.print("\n  • Nuovo Username:   ");
            newUsername = userIn.nextLine().trim();

            System.out.print("  • Nuova Password:   ");
            newPassword = userIn.nextLine().trim();
        }

        if (newUsername.equals("")) {
            newUsername = username;
        }

        if (newPassword.equals("")) {
            newPassword = password;
        }

        UpdateCredentialRequest req = new UpdateCredentialRequest(username, newUsername, password, newPassword);
        NioUtils.sendJson(socket, gson.toJson(req));
    }

    /**
     * Acquisisce esattamente 4 stringhe (la giocata/proposta dell'utente) e le
     * invia al server.
     */
    public static void doProposal(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ INVIA PROPOSTA ════");
        List<String> parole = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            System.out.printf("  • Parola %d/4: ", i);
            String inputWord = userIn.nextLine().trim();
            parole.add(inputWord);
        }

        SubmitProposalRequest req = new SubmitProposalRequest(parole);
        NioUtils.sendJson(socket, gson.toJson(req));
    }

    /**
     * Guida l'utente nella formulazione della richiesta di informazioni di una
     * partita (scegliendo se si tratta di quella corrente o di una specifica
     * tramite ID). E invia la richiesta al server
     */
    public static void doRequestGameInfo(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ INFO PARTITA ════");
        System.out.print("  - Vuoi info sulla partita attuale? (SI/NO): ");
        String scelta = userIn.nextLine().trim().toUpperCase();

        while ((!scelta.equals("SI")) && (!scelta.equals("NO"))) {
            System.out.print("  !! Errore: Inserisci SI o NO: ");
            scelta = userIn.nextLine().trim().toUpperCase();
        }

        if (scelta.equals("SI")) {
            // ID convenzionale -1 e flag true indicano al server di cercare lo stato corrente del giocatore
            RequestGameInfoRequest req = new RequestGameInfoRequest(-1, true);
            NioUtils.sendJson(socket, gson.toJson(req));
        } else if (scelta.equals("NO")) {
            System.out.print("  - Inserisci l'ID della partita desiderata: ");
            String id = userIn.nextLine().trim();

            while (!isNumeric(id)) {
                System.out.print("  ! Errore: Inserisci un numero valido: ");
                id = userIn.nextLine().trim(); // Leggi di nuovo
            }

            int numId = Integer.parseInt(id);
            // Invia la richiesta specificando un ID storico e flag inCorso = false
            RequestGameInfoRequest req = new RequestGameInfoRequest(numId, false);
            NioUtils.sendJson(socket, gson.toJson(req));

        }
    }

    /**
     * Guida l'utente nella formulazione della richiesta delle statistiche
     * globali, consentendo di scegliere tra partita in corso (live) e partita
     * passata. Ed invia la richiesta al server
     */
    public static void doRequestGameStats(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ STATISTICHE GIOCO ════");
        System.out.print("  - Vuoi le statistiche della partita attuale? (SI/NO): ");
        String scelta = userIn.nextLine().trim().toUpperCase();

        while ((!scelta.equals("SI")) && (!scelta.equals("NO"))) {
            System.out.print("  !! Errore: Inserisci SI o NO: ");
            scelta = userIn.nextLine().trim().toUpperCase();
        }

        if (scelta.equals("SI")) {
            RequestGameStatsRequest req = new RequestGameStatsRequest(-1, true);
            NioUtils.sendJson(socket, gson.toJson(req));
        } else if (scelta.equals("NO")) {
            System.out.print("  - Inserisci l'ID della partita desiderata: ");
            String id = userIn.nextLine().trim();

            while (!isNumeric(id)) {
                System.out.print("  ! Errore: Inserisci un numero valido: ");
                id = userIn.nextLine().trim(); // Leggi di nuovo
            }

            int numId = Integer.parseInt(id);
            RequestGameStatsRequest req = new RequestGameStatsRequest(numId, false);
            NioUtils.sendJson(socket, gson.toJson(req));

        }
    }

    /**
     * Menu di selezione per la visualizzazione della Leaderboard. Supporta tre
     * percorsi: Posizione di un giocatore, Classifica Globale o Top K
     * Giocatori.In fine invia la richiesta al server
     */
    public static void doLeaderboardRequest(Scanner userIn, SocketChannel socket) throws IOException {
        System.out.println("\n════ RICHIESTA CLASSIFICA ════");
        System.out.print("• Vuoi visualizzare la posizione di un giocatore o la classifica globale? ");
        System.out.print("\n • 1 Posizione giocatore\n • 2 Classifica\n");
        System.out.print(" \033[1;32mTU >> \033[0m");
        String scelta = userIn.nextLine().trim();

        while ((!scelta.equals("1")) && (!scelta.equals("2"))) {
            System.out.print("\n  • Devi scegliere tra 1 ed 2: ");
            scelta = userIn.nextLine().trim();
        }

        if (scelta.equals("1")) {
            System.out.print("• Inserisci il nome del giocatore desiderato:\n ");
            System.out.print(" \033[1;32mTU >> \033[0m");
            String username = userIn.nextLine().trim();
            // Flag global = false, top = -1 indica la ricerca dell'utente
            RequestLeaderboardRequest req = new RequestLeaderboardRequest(false, username, -1);
            NioUtils.sendJson(socket, gson.toJson(req));
        } else {

            System.out.print("\n• Vuoi visualizzare la classifica globale o i primi K giocatori? ");
            System.out.print("\n • 1 Classifica globale\n • 2 Classifica top K giocatori\n");
            System.out.print(" \033[1;32mTU >> \033[0m");

            String selezione = userIn.nextLine().trim();
            while ((!selezione.equals("1")) && (!selezione.equals("2"))) {
                System.out.print("\n  • Devi scegliere tra 1 ed 2: ");
                selezione = userIn.nextLine().trim();
            }

            if (selezione.equals("1")) {
                // Classifica intera
                RequestLeaderboardRequest req = new RequestLeaderboardRequest(true, "", -1);
                NioUtils.sendJson(socket, gson.toJson(req));
            } else {
                // Classifica limitata a K elementi
                System.out.print("\n• Inserisci il numero dei primi k giocatori che vuoi visualizzare:\n  ");
                System.out.print(" \033[1;32mTU >> \033[0m");
                String num = userIn.nextLine().trim();

                while (!(isInteroPositivo(num))) {
                    System.out.print("  • Devi inserire un numero positivo : ");
                    num = userIn.nextLine().trim();
                }

                RequestLeaderboardRequest req = new RequestLeaderboardRequest(true, "", Integer.parseInt(num));
                NioUtils.sendJson(socket, gson.toJson(req));
            }
        }
    }

    /**
     * Invia la richiesta per ottenere le statistiche personali (es. streak e
     * percentuale vittorie).
     */
    public static void doPlayerStatsRequest(SocketChannel socket) throws IOException {
        RequestPlayerStatsRequest req = new RequestPlayerStatsRequest();
        NioUtils.sendJson(socket, gson.toJson(req));
    }

}
