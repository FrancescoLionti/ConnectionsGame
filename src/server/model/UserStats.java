package server.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Modello dati che rappresenta le statistiche storiche e cumulate di un singolo
 * utente. Viene salvato in formato JSON e aggiornato al termine di ogni partita
 * giocata.
 */
public class UserStats {

    public String username; // Nome utente associato a queste statistiche
    public int totalScore; // Punteggio globale accumulato in tutte le partite
    public int puzzlesCompleted; // Numero totale di partite giocate fino in fondo
    public int numWin; // Numero totale di vittorie ottenute
    public float winRate;
    public float lossRate;
    public int currentStreak; // Serie attuale ininterrotta di vittorie
    public int maxStreak; // Record massimo di vittorie consecutive 
    public int perfectPuzzles;// Numero di vittorie ottenute senza commettere nessun errore
    public Map<Integer, Integer> mistakeHistogram; // Mappa che traccia quante volte l'utente ha vinto/perso con uno specifico numero di errori (da 0 a 4)

    /**
     * Costruttore di default. Inizializza un nuovo profilo utente azzerando
     * tutti i contatori e preparando l'istogramma degli errori.
     */
    public UserStats() {
        this.username = "";
        this.totalScore = 0;
        this.numWin = 0;
        this.puzzlesCompleted = 0;
        this.winRate = 0.0f;
        this.lossRate = 0.0f;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.perfectPuzzles = 0;

        this.mistakeHistogram = new HashMap<>();
        for (int i = 0; i <= 4; i++) {
            mistakeHistogram.put(i, 0);
        }

    }

}
