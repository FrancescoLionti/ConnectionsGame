package utils;

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;

/**
 * classe usata per caricare la configurazione generale del server. Mappa
 * direttamente la struttura del file JSON di configurazione in un oggetto Java.
 */
public class ConfigServer {

    public int serverPort;               // Porta TCP su cui il server si mette in ascolto per i client
    public int udpServerPort;            // Porta utilizzata dal server per l'invio delle notifiche asincrone via UDP
    public int gameDurationSeconds;      // Durata massima impostata per ogni partita (espressa in secondi)

    // Percorsi dei file utilizzati per la persistenza dei dati e per la lettura del dataset
    public String gameHistoryFile;       // File JSON dove viene salvato lo storico delle partite passate
    public String rankingFilePath;       // file JSON in cui risiedono le classifiche e le statistiche degli utenti
    public String userFilePath;          // File Json dedicato alle credenziali di accesso degli utenti
    public String connectionsDataPath;   // File JSON (fornito dal progetto) con le parole e i gruppi da indovinare

    /**
     * Legge la configurazione dal file JSON specificato e la restituisce come
     * oggetto ConfigServer. utilizza la libreria Gson per eseguire la
     * deserializzazione automatica dei campi.
     *
     * @param filePath Il percorso del file JSON da cui caricare la
     * configurazione.
     * @return L'oggetto ConfigServer istanziato e popolato con i valori del
     * file.
     * @throws IOException Se il file non esiste, è inaccessibile o si
     * verificano errori in lettura.
     */
    public static ConfigServer load(String filePath) throws IOException {
        Gson gson = new Gson();
        // Il costrutto try-with-resources assicura la chiusura del FileReader alla fine del blocco
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, ConfigServer.class);
        }
    }
}
