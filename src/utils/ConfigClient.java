package utils;

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;

/**
 * Classe usata per caricare la configurazione di rete del client. Mappa
 * direttamente la struttura del file JSON di configurazione in un oggetto Java.
 */
public class ConfigClient {

    public String serverAddress;     // Indirizzo IP o hostname del server a cui connettersi
    public int clientPortUDP;        // Porta locale su cui il client si mette in ascolto per notifiche asincrone UDP
    public int serverPort;           // Porta TCP del server a cui inviare le richieste

    /**
     * Legge una configurazione da un file JSON e la restituisce come oggetto
     * ConfigClient. Utilizza la libreria Gson per deserializzare
     * automaticamente i campi.
     *
     * @param filePath Il percorso del file di configurazione JSON.
     * @return L'oggetto ConfigClient popolato con i valori letti dal file.
     * @throws IOException Se si verifica un errore durante l'apertura o la
     * lettura del file.
     */
    public static ConfigClient load(String filePath) throws IOException {
        Gson gson = new Gson();
        // Utilizza il costrutto try-with-resources per chiudere automaticamente il reader
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, ConfigClient.class);
        }
    }
}
