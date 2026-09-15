package protocol.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Risposta alla richiesta di autenticazione. Se il login ha successo, fornisce
 * al client tutti i dati necessari per entrare immediatamente nella partita
 * attualmente in corso.
 */
public class LoginResponse extends Response {

    public List<String> words; //set di parole da indovinare
    public List<List<String>> correctProposals; //proposte corrette
    public int mistakes; //numero di errori
    public int score; //punteggio
    public long remainingTime; //tempo rimanente

    public LoginResponse() {
        super("login", 0, null);
        this.words = new ArrayList<>();
        this.correctProposals = new ArrayList<>();
    }

    /**
     * Costruttore utilizzato in caso di esito negativo (es. credenziali errate
     * o utente già loggato). Inizializza i dati di gioco a valori vuoti o zero
     */
    public LoginResponse(int responseCode, String errorMessage) {
        super("login", responseCode, errorMessage);
        this.words = new ArrayList<>();
        this.correctProposals = new ArrayList<>();
        this.mistakes = 0;
        this.score = 0;
        this.remainingTime = 0;
    }

    /**
     * Costruttore utilizzato quando il login ha successo (200 OK). Questo
     * costruttore aggrega nella risposta tutto il payload necessario al client
     * per renderizzare l'interfaccia di gioco aggiornata o per riprendere una
     * partita precedentemente interrotta .
     *
     * @param responseCode Il codice di stato (es. 200 OK).
     * @param errorMessage Un eventuale messaggio di stato (
     * @param words Il pool delle 16 parole iniziali disponibili in questa
     * partita.
     * @param correctProposals La lista dei gruppi che *questo specifico
     * giocatore* ha già individuato.
     * @param mistakes Il numero di errori (da 0 a 4) già commessi *da questo
     * giocatore*.
     * @param score I punti già accumulati dal giocatore nel turno corrente.
     * @param remainingTime I millisecondi mancanti al termine della partita
     * globale sul server.
     */
    public LoginResponse(int responseCode, String errorMessage, List<String> words, List<List<String>> correctProposals, int mistakes, int score, long remainingTime) {
        super("login", responseCode, errorMessage);
        this.words = words;
        this.correctProposals = correctProposals;
        this.mistakes = mistakes;
        this.score = score;
        this.remainingTime = remainingTime;
    }
}
