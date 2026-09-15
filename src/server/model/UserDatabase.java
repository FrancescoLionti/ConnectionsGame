package server.model;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Struttura dati dedicata alla memorizzazione delle credenziali di accesso
 * degli utenti. Estende ConcurrentHashMap<String, String> dove la chiave è
 * l'username e il valore è la password. Assicura la thread-safety e fornisce a
 * Gson il "tipo" esplicito per serializzare/deserializzare correttamente la
 * mappa da e verso il file JSON degli utenti registrati.
 */
public class UserDatabase extends ConcurrentHashMap<String, String> {

    // Costruttore vuoto necessario per Gson
    public UserDatabase() {
        super();
    }
}
