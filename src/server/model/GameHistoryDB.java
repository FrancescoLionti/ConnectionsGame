package server.model;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Struttura dati dedicata alla memorizzazione dello storico delle partite
 * concluse. Estende ConcurrentHashMap per garantire thread-safety durante gli
 * accessi concorrenti in memoria. Questa classe funge da "tipo contenitore"
 * specifico: è necessaria a Gson per poter serializzare e deserializzare
 * correttamente la mappa da/verso il file JSON (Game_History.json).
 */
public class GameHistoryDB extends ConcurrentHashMap<String, Partita> {

    public GameHistoryDB() {
        super();
    }
}
