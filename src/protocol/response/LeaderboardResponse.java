package protocol.response;

import java.util.ArrayList;
import java.util.List;

import server.model.UserStats;

/**
 * Risposta alla richiesta di visualizzazione della classifica (Leaderboard).
 * Gestisce sia la restituzione dell'intera classifica (o top K) che della
 * posizione di un singolo utente.
 */
public class LeaderboardResponse extends Response {

    // Lista ordinata di StatiUtenti;
    public List<UserStats> leaderboard;
    public int posUser;
    public boolean global;
    public String username;

    /**
     * Costruttore utilizzato per restituire l'intera classifica o la top K.
     */
    public LeaderboardResponse(int code, String msg, List<UserStats> leaderboard) {
        super("requestLeaderboard", code, msg);
        this.leaderboard = leaderboard;
        this.posUser = -1; // cioe tutta la classifica
        this.global = true; //classifica globale
    }

    /**
     * Costruttore utilizzato per restituire la posizione di un singolo
     * giocatore specifico.
     */
    public LeaderboardResponse(int code, String msg, int position, String username) {
        super("requestLeaderboard", code, msg);
        this.leaderboard = new ArrayList<>();
        this.posUser = position; //posizione del player
        this.global = false;
        this.username = username;
    }

    /**
     * Costruttore utilizzato per restituire top K.
     */
    public LeaderboardResponse(int code, String msg, List<UserStats> leaderboard, int position) {
        super("requestLeaderboard", code, msg);
        this.leaderboard = leaderboard;
        this.posUser = position;    //cioe la top k
        this.global = true; //classifica globale
    }

}
