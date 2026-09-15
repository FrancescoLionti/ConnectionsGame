package client;

/**
 * Classe di utilità per la gestione e la formattazione del tempo.
 */
public class Tempo {

    /**
     * Converte i millisecondi in una stringa di testo formattata come HH:MM:SS.
     * Utilizzata per mostrare all'utente il tempo residuo della partita in modo
     * leggibile.
     *
     * @param remainingTime Il tempo rimanente in millisecondi.
     * @return Una stringa formattata (es. "00:05:30" per 5 minuti e 30
     * secondi).
     */
    public static String displayTempo(long remainingTime) {
        // conversione millisecondi in ore, minuti, secondi
        long totalSeconds = remainingTime / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // String.format con %02d garantisce che ogni valore abbia almeno 2 cifre 
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
