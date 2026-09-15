package server.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene lo stato di gioco e le statistiche relative a un singolo giocatore
 * all'interno di una specifica partita. Traccia i progressi correnti
 * (punteggio, errori) e determina le condizioni di termine
 * (vittoria/sconfitta).
 */
public class PlayerGameState {

    private int errori;  // Contatore degli errori commessi 
    private int punteggio; // Punteggio accumulato fino ad adesso (modificato ad ogni giocata)
    private final List<List<String>> gruppiIndovinati = new ArrayList<>(); // I gruppi (da 4 parole ciascuno) individuati con successo
    private boolean partitaFinita; // indica se l'utente ha concluso la sua partita
    private boolean vittoria; // true se l'utente ha indovinato tutti i gruppi
    private boolean sconfitta; //true se l'utente ha raggiunto il limite di errori consentito

    public int getErrori() {
        return errori;
    }

    public void setErrori(int numErr) {
        this.errori = numErr;
    }

    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    public List<List<String>> getGruppiIndovinati() {
        return gruppiIndovinati;
    }

    public boolean isPartitaFinita() {
        return partitaFinita;
    }

    public void setPartitaFinita(boolean partitaFinita) {
        this.partitaFinita = partitaFinita;
    }

    public boolean isVittoria() {
        return vittoria;
    }

    public void setVittoria(boolean vittoria) {
        this.vittoria = vittoria;
    }

    public boolean isSconfitta() {
        return sconfitta;
    }

    public void setSconfitta(boolean sconfitta) {
        this.sconfitta = sconfitta;
    }
}
