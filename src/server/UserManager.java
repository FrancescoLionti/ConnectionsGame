package server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import server.model.UserDatabase;

/**
 * Gestisce la persistenza e le operazioni base degli utenti del sistema. Come
 * richiesto dalle specifiche, il server deve garantire la persistenza delle
 * informazioni sugli utenti in formato JSON, per permettere il riavvio del
 * server mantenendo lo stato. Questa classe utilizza la libreria Gson per
 * serializzare/deserializzare la classe UserDatabase.
 */
public class UserManager {

    private final UserDatabase utenti; // Struttura dati in memoria che mantiene le associazioni (cioe una mappa: username -> password
    private final Gson gson; // Istanza di Gson per la gestione del formato JSON
    private final String userFilePath; // Percorso del file in cui salvare/caricare i dati degli utenti (fornito dal config)

    /**
     * Inizializza il manager degli utenti. (All'avvio del server, carica in
     * memoria gli utenti precedentemente registrati)
     *
     * @param userFilePath Il percorso del file JSON da cui caricare e in cui
     * salvare gli utenti.
     */
    public UserManager(String userFilePath) {

        this.gson = new GsonBuilder().setPrettyPrinting().create();// Inizializza Gson con PrettyPrinting per rendere il file Json leggibile
        this.userFilePath = userFilePath;
        this.utenti = loadUsers();
        System.out.println("UsersManager caricato: " + utenti.size() + " utenti trovati.");
    }

    /**
     * Carica gli utenti dal file JSON specificato. Se il file non esiste (es.
     * al primo avvio del server), crea e restituisce un "database" vuoto.
     *
     * @return L'istanza di UserDatabase contenente i dati letti dal file, o
     * un'istanza vuota in caso di errore/file assente.
     */
    private UserDatabase loadUsers() {

        File file = new File(userFilePath);
        if (!file.exists()) {
            System.out.println("File users.json non trovato, ne creo uno vuoto.");
            return new UserDatabase();
        }

        try (Reader reader = new FileReader(file)) {
            // Deserializza l'intero file JSON nella classe UserDatabase
            UserDatabase loaded = gson.fromJson(reader, UserDatabase.class);
            return loaded != null ? loaded : new UserDatabase();
        } catch (IOException e) {
            System.err.println("Errore caricamento users.json: " + e.getMessage());
            return new UserDatabase();
        }
    }

    /**
     * Inserisce un utente nella mappa e salva immediatamente su file. Il metodo
     * non è synchronized perche e' usato soltanto dentro metodi syncro
     *
     * @param username Il nome utente scelto.
     * @param password La password associata all'utente.
     */
    public void register(String username, String password) {

        utenti.put(username, password);
        saveUsers();
    }

    /**
     * Verifica l'esistenza di un utente nel sistema. Utilizzato in fase di
     * registrazione per evitare duplicati o in fase di login per verificare che
     * l'username sia valido.
     *
     * @param username L'username da cercare.
     * @return true se l'utente è registrato, false altrimenti.
     */
    public boolean exists(String username) {
        return utenti.containsKey(username);
    }

    /**
     * Verifica la correttezza della password per un determinato utente.
     * Utilizzato durante l'operazione di login o aggiornamento credenziali.
     *
     * @param username Il nome utente.
     * @param password La password fornita dal client.
     * @return true se la password corrisponde a quella salvata, false
     * altrimenti.
     */
    public boolean checkPassword(String username, String password) {
        return utenti.getOrDefault(username, "").equals(password);
    }

    /**
     * Rimuove un utente dal database(ovvero la mappa) in memoria. Il metodo non
     * è synchronized perche e' usato soltanto dentro metodi synchronized
     *
     * @param username Il nome dell'utente da eliminare.
     */
    public void deleteUser(String username) {
        utenti.remove(username);
    }

    /**
     * Scrive lo stato attuale del database in memoria nel file JSON. Viene
     * chiamato automaticamente dopo ogni registrazione per mantenere
     * consistente il file su disco, come richiesto dalle specifiche sulla
     * persistenza.
     */
    private void saveUsers() {
        try (Writer writer = new FileWriter(userFilePath)) {
            // Serializza l'intero oggetto UserDatabase e lo scrive su file
            gson.toJson(utenti, writer);
        } catch (IOException e) {
            System.err.println("Errore salvataggio users.json: " + e.getMessage());
        }
    }

}
