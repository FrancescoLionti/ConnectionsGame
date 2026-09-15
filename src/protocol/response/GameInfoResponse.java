package protocol.response;

import java.util.ArrayList;
import java.util.List;

import server.model.Group;

public class GameInfoResponse extends Response {

    public boolean isGameEnded; // specifica se la partita è finita o no.
    public int score;   // Punteggio attuale
    public int mistakes; // Numero di errori

    // dati specifici per partita IN CORSO
    public long remainingTime;  // Tempo residuo
    public List<String> remainingWords;// parole ancora da raggruppare 
    public List<List<String>> correctGroups;

    // Dati specifici per partita COCLUSA
    public List<Group> solutions; // la soluzione completa 
    public int numCorrect; // Numero totale di gruppi indovinati a fine partita

    public GameInfoResponse() {
        super("requestGameInfo", 0, null);
        this.correctGroups = new ArrayList<>();
    }

    /**
     * Costruttore utilizzato per inviare i dettagli di una partita IN CORSO.
     */
    public GameInfoResponse(int responseCode, String errorMessage, boolean isGameEnded, List<List<String>> correctGroups, List<String> remainingWords, int score, int mistakes, long remainingTime) {
        super("requestGameInfo", responseCode, errorMessage);
        this.isGameEnded = isGameEnded;
        this.correctGroups = correctGroups;
        this.score = score;
        this.mistakes = mistakes;
        this.remainingTime = remainingTime;
        this.remainingWords = remainingWords;
    }

    /**
     * Costruttore utilizzato per inviare i dettagli e le soluzioni di una
     * partita CONCLUSA .
     */
    public GameInfoResponse(int responseCode, String errorMessage, boolean isGameEnded, List<Group> solutions, int numCorrect, int score, int mistakes) {
        super("requestGameInfo", responseCode, errorMessage);
        this.isGameEnded = isGameEnded;
        this.solutions = solutions;
        this.score = score;
        this.mistakes = mistakes;
        this.numCorrect = numCorrect;
    }
}
