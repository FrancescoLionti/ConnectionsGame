package server.model;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Struttura dati dedicata alla memorizzazione della classifica globale e delle
 * statistiche degli utenti. Estende ConcurrentHashMap per permettere letture e
 * scritture sicure in un ambiente multi-thread. L'utilizzo di questa classe
 * derivata fornisce a Gson il "tipo" esatto necessario per la corretta
 * deserializzazione e serializzazione verso il file JSON delle classifiche.
 */
public class Ranking extends ConcurrentHashMap<String, UserStats> {

    // Costruttore vuoto necessario per Gson
    public Ranking() {
        super();
    }
}
