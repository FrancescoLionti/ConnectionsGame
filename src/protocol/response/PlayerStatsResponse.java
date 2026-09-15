package protocol.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Risposta alla richiesta delle statistiche personali di un giocatore.
 */
public class PlayerStatsResponse extends Response {

    public int puzzlesCompleted;      // Totle giocate 
    public double winRate;            // % Vittorie
    public double lossRate;           // % Sconfitte
    public int currentStreak;         // serie vittorie consecutive attuale
    public int maxStreak;             // record storico di vittorie consecutive
    public int perfectPuzzles;        // Numero di partite vinte commettendo 0 errori

    // È una mappa dove la chiave (da 0 a 4) rappresenta lo scenario e il valore il numero di partite:
    // 0 -> numero partite concluse con 0 errori
    // 1 -> numero partite concluse con un errore
    //ecc
    public Map<Integer, Integer> mistakeHistogram;

    public PlayerStatsResponse() {
        super("requestPlayerStats", 0, null);
        this.mistakeHistogram = new HashMap<>();
    }

    public PlayerStatsResponse(int responseCode, String errorMessage,
            int puzzlesCompleted, double winRate, double lossRate,
            int currentStreak, int maxStreak, int perfectPuzzles,
            Map<Integer, Integer> mistakeHistogram) {
        super("requestPlayerStats", responseCode, errorMessage);
        this.puzzlesCompleted = puzzlesCompleted;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.perfectPuzzles = perfectPuzzles;
        this.mistakeHistogram = mistakeHistogram;
    }
}
