package server;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonReader;

import server.model.Game;
import server.model.Group;

/**
 * Fornisce dinamicamente le nuove partite estraendole dal file JSON di
 * configurazione (ConnectionsData). la classe NON deserializza l'intero file in
 * memoria, ma legge sequenzialmente un singolo Game alla volta.
 */
public class GameProvider {

    private JsonReader reader;
    private final String filePath;// Percorso del file contenente il dizionario delle partite (es. connections_data.json)

    /**
     * Inizializza il provider di partite aprendo il file in modalità streaming.
     *
     * @param filePath Il percorso relativo o assoluto del file JSON delle
     * partite.
     * @throws IOException Se il file non esiste o non è accessibile.
     */
    public GameProvider(String filePath) throws IOException {
        this.filePath = filePath;
        apriFile();
    }

    /**
     * Apre o riapre il file JSON inizializzando il JsonReader e consumando la
     * parentesi quadra iniziale dell'array principale '['.
     */
    private void apriFile() throws IOException {
        this.reader = new JsonReader(new FileReader(filePath));
        // Consuma l'inizio dell'array principale, posizionandosi prima del primo oggetto "Game"
        this.reader.beginArray();
    }

    /**
     * Estrae la prossima partita disponibile (prossimo oggetto Game) dal flusso
     * JSON. Se il file arriva alla fine, lo chiude e ricomincia a leggere
     * dall'inizio (loop infinito).
     *
     * @return L'oggetto Game appena parsato, o null in caso di file
     * completamente vuoto.
     * @throws IOException In caso di errori durante la lettura dal file.
     */
    public synchronized Game getNextGame() throws IOException {
        if (reader.hasNext()) {
            return readGame(reader);
        } else {
            // Fine del file raggiunta: chiude il reader e riparte dall'inizio 
            reader.close();
            apriFile();
            if (reader.hasNext()) {
                return readGame(reader);
            }
            // Ritorna null solo se il file JSON è completamente privo di elementi (es. "[]")
            return null;
        }
    }

    /**
     * Da invocare esplicitamente quando il server si spegne per rilasciare il
     * descrittore del file.
     *
     * @throws IOException In caso di problemi durante la chiusura dello stream.
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /**
     * Legge un array di stringhe JSON (le 4 parole) e le inserisce in una
     * lista.
     */
    private List<String> readStringList(JsonReader reader) throws IOException {
        List<String> list = new ArrayList<>();
        reader.beginArray(); // [
        while (reader.hasNext()) { // finchè nell'array Json ci sono elementi
            list.add(reader.nextString()); //aggiungo alla lista la stringa
        }
        reader.endArray(); // Chiude l'array delle parole: ]
        return list;
    }

    /**
     * Legge e costruisce un singolo Gruppo logico, composto da un tema (String)
     * e dalle relative parole (List).
     */
    private Group readGroup(JsonReader reader) throws IOException {
        String theme = null;
        List<String> words = null;

        reader.beginObject(); // {
        while (reader.hasNext()) { // finchè ci sono campi nell'oggetto Json
            String name = reader.nextName();//leggo il nome del campo 
            switch (name) {
                case "theme" -> // se è "theme" leggo la stringa del tema
                    theme = reader.nextString();
                case "words" -> //se è "words" leggo la lista di parole
                    words = readStringList(reader);
                default -> // se c'è un campo extra lo ignoro
                    reader.skipValue();// Ignora in modo sicuro eventuali chiavi non previste
            }
        }
        reader.endObject(); // chiude l'ogetto gruppo: }

        return new Group(theme, words);
    }

    /**
     * Legge l'array JSON "groups" e restituisce una lista di oggetti Group (i 4
     * raggruppamenti del match).
     */
    private List<Group> readGroups(JsonReader reader) throws IOException {
        List<Group> list = new ArrayList<>();
        reader.beginArray(); // Apre l'array dei gruppi: [
        while (reader.hasNext()) { // finchè ci sono gruppi nell'array Json
            list.add(readGroup(reader)); // aggiungo alla lista il gruppo letto
        }
        reader.endArray(); // Chiude l'array dei gruppi: ]
        return list;
    }

    /**
     * Legge e costruisce l'oggetto Game (una singola partita completa).
     */
    private Game readGame(JsonReader reader) throws IOException {
        int gameId = -1;
        List<Group> groups = null;

        reader.beginObject(); // Apre l'oggetto partita: {
        while (reader.hasNext()) { // finchè ci sono campi nell'oggetto Json
            String name = reader.nextName();
            switch (name) {
                case "gameId" ->
                    gameId = reader.nextInt(); // Estrae l'identificativo originale
                case "groups" ->
                    groups = readGroups(reader); //leggo e creo la lista di gruppi
                default ->
                    reader.skipValue(); // Ignora campi extra
            }
        }

        reader.endObject(); // Chiude l'oggetto partita: }
        return new Game(gameId, groups);
    }
}
