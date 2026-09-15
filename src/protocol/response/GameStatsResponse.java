package protocol.response;

/**
 * Risposta alla richiesta di statistiche globali su una specifica partita.
 * Adatta i dati restituiti a seconda che la partita sia ancora in corso o sia
 * già conclusa.
 */
public class GameStatsResponse extends Response {

    public boolean finish;
    public long remainingTime; // Tempo rimasto globale
    public int playersInGame;       // Giocatori che non hanno finito 
    public int playersFinished;     // Giocatori che hanno finito (vinto o perso/abbandonato)
    public int playersWon;          // Gicatori che hanno VINTO
    public int totalParticipants;   // Totale utenti che hanno partecipato
    public double averageScore;     // Punteggio medio di tutti i giocatori che hanno partecipato

    public GameStatsResponse() {
        super("requestGameStats", 0, null);
    }

    /**
     * Costruttore per le statistiche "live" di una partita in corso.
     */
    public GameStatsResponse(int responseCode, String errorMessage,
            long remainingTime, int playersInGame, int playersFinished,
            int playersWon) {
        super("requestGameStats", responseCode, errorMessage);
        this.remainingTime = remainingTime;
        this.playersInGame = playersInGame;
        this.playersFinished = playersFinished;
        this.playersWon = playersWon;
        this.finish = false;
    }

    /**
     * Costruttore per le statistiche di una partita conclusa.
     */
    public GameStatsResponse(int responseCode, String errorMessage,
            int totalParticipants, int playersFinished,
            int playersWon, double averageScore) {
        super("requestGameStats", responseCode, errorMessage);

        this.playersFinished = playersFinished;
        this.playersWon = playersWon;
        this.totalParticipants = totalParticipants;
        this.averageScore = averageScore;
        this.finish = true;
    }
}
