package server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import server.model.GameHistoryDB;
import server.model.Partita;

/**
 * Gestisce la persistenza e il salvataggio dello storico delle partite
 * terminate. Come richiesto dalle specifiche, memorizza in formato JSON le
 * informazioni sulle partite concluse per permettere interrogazioni storiche
 * (es. calcolo statistiche passate) e per garantire il recupero dei dati dopo
 * un riavvio del server.
 */
public class GameHistoryManager {

    private final GameHistoryDB partiteConcluse;// mappa per memorizzare le partite in memoria
    private final Gson gson;
    private final String gameHistoryFile; // percorso del file 

    /**
     * Inizializza il manager dello storico caricando i dati precedentemente
     * salvati.
     *
     * @param gameHistoryFile Percorso del file JSON utilizzato per il
     * salvataggio/caricamento.
     */
    public GameHistoryManager(String gameHistoryFile) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.gameHistoryFile = gameHistoryFile;
        this.partiteConcluse = loadGames();// Al boot del server carica immediatamente lo storico delle partite passate
        System.out.println("GameHistoryManager caricato: " + partiteConcluse.size() + " partite trovate.");
    }

    /**
     * Tenta di caricare il database delle partite concluse dal file JSON. Se il
     * file non esiste (primo avvio), ne inizializza uno vuoto.
     *
     * @return L'oggetto GameHistoryDB popolato con i dati letti, oppure uno
     * vuoto se il file non esiste o in caso di errore.
     */
    private GameHistoryDB loadGames() {
        File file = new File(gameHistoryFile);
        if (!file.exists()) {
            System.out.println("File Game_History.json non trovato, creo uno vuoto.");
            return new GameHistoryDB();
        }

        try (Reader reader = new FileReader(file)) {
            // Deserializza l'intero contenuto JSON nella classe custom
            GameHistoryDB loaded = gson.fromJson(reader, GameHistoryDB.class);
            return loaded != null ? loaded : new GameHistoryDB();
        } catch (IOException e) {
            System.err.println("Errore caricamento Game_History.json: " + e.getMessage());
            return new GameHistoryDB();
        }
    }

    /**
     * Archivia una partita appena conclusa, la inserisce in memoria e forza la
     * scrittura su file. NB: non synchronized perche usata solo dentro metodi
     * synchronized
     *
     * @param partita L'oggetto Partita da archiviare. La partita deve essere
     * già flaggata come 'terminata'.
     */
    public void archiviaPartita(Partita partita) {
        if (partita == null || !partita.getIsTerminata()) {
            System.err.println("Partita non archiviabile (null o non terminata)");
            return;
        }

        String gameId = Integer.toString(partita.getIdPartita());// L'ID numerico della partita viene convertito in stringa per fungere da chiave della mappa (e del JSON)
        partiteConcluse.put(gameId, partita);  // Salva con gameId come chiave
        saveGames();
    }

    /**
     * Cerca una partita all'interno dello storico utilizzando il suo
     * identificativo globale. Utilizzato dai metodi del controller (es.
     * requestGameInfo o requestGameStats) quando un client richiede i dettagli
     * di un match già concluso.
     *
     * @param gameId L'identificativo in formato stringa della partita
     * desiderata.
     * @return L'oggetto Partita storico, o null se non presente.
     */
    public Partita trovaPartita(String gameId) {
        return partiteConcluse.get(gameId);
    }

    /**
     * Scrive l'intero stato della memoria in un file JSON persistente. È
     * synchronized per evitare che due thread (es. la chiusura del server e la
     * fine temporizzata di una partita) richiamino contemporaneamente
     * FileWriter sullo stesso file.
     */
    public synchronized void saveGames() {
        try (Writer writer = new FileWriter(gameHistoryFile)) {
            gson.toJson(partiteConcluse, writer);
            System.out.println("Game_History.json salvato (" + partiteConcluse.size() + " partite)");
        } catch (IOException e) {
            System.err.println("Errore salvataggio Game_History.json: " + e.getMessage());
        }
    }

    /**
     * Chiude in modo sicuro il manager, assicurandosi che gli ultimi dati
     * vengano scritti.
     */
    public void close() {
        saveGames();
        System.out.println("GameHistoryManager chiuso");
    }

    /**
     * Restituisce la mappa contenente tutte le partite archiviate.
     *
     * @return L'oggetto GameHistoryDB attualmente in uso.
     */
    public GameHistoryDB getPartiteConcluse() {
        return partiteConcluse;
    }

    /**
     * Aggiorna l'username di un giocatore in tutte le partite storiche a cui ha
     * partecipato. Serve per mantenere la coerenza quando un utente cambia
     * nome.
     */
    public synchronized void renamePlayerInHistory(String oldUsername, String newUsername) {
        boolean isUpdated = false;

        // Itera su tutte le partite concluse
        for (Partita partita : partiteConcluse.values()) {
            if (partita.playerInState(oldUsername)) {
                partita.renamePlayer(oldUsername, newUsername);
                isUpdated = true;
            }
        }

        // Se almeno una partita è stata modificata, salva le modifiche nel file JSON
        if (isUpdated) {
            saveGames();
            System.out.println("Storico aggiornato per il cambio nome: " + oldUsername + " -> " + newUsername);
        }
    }

}
