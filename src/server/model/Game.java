package server.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta la struttura base (o "seed") di un puzzle. Corrisponde
 * all'oggetto JSON letto dal file di configurazione delle partite e contiene le
 * soluzioni esatte (i 4 raggruppamenti tematici).
 */
public class Game {

    private final int gameId; //  Identificativo originale del puzzle estratto dal file JSON
    private final List<Group> groups; // La lista dei 4 gruppi che compongono il gioco

    /**
     * Costruisce la definizione di un nuovo gioco.
     *
     * @param gameId L'ID identificativo del puzzle.
     * @param groups La lista dei raggruppamenti validi per questo puzzle.
     */
    public Game(int gameId, List<Group> groups) {
        this.gameId = gameId;
        this.groups = groups;
    }

    /**
     * Restituisce le soluzioni del gioco sotto forma di lista di gruppi
     * tematici.
     *
     * @return La lista contenente i raggruppamenti logici.
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * Estrae tutte le parole del puzzle e le unisce in un'unica lista piatta.
     * Viene utilizzato dal server per generare l'elenco delle 16 parole
     * mischiate da inviare al client.
     *
     * @return Una lista contenente tutte le parole presenti nei gruppi della
     * partita.
     */
    public List<String> getWords() {
        List<String> ListWords = new ArrayList<>();
        for (Group g : groups) {
            ListWords.addAll(g.getWords());
        }
        return ListWords;
    }

    /**
     * Restituisce l'ID originale del puzzle associato a questa istanza.
     */
    public int getGameId() {
        return gameId;
    }

}
