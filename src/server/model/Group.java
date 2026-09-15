package server.model;

import java.util.List;

/**
 * Rappresenta un singolo raggruppamento all'interno di una partita. Contiene la
 * categoria tematica e le rispettive parole che vi appartengono.
 */
public class Group {

    private final String theme; //tema del gruppo
    private final List<String> words; //lista che contine le 4 parole del gruppo

    /**
     * Costruisce un nuovo gruppo tematico.
     *
     * @param theme Il titolo/tema del gruppo.
     * @param words Le parole associate a tale tema.
     */
    public Group(String theme, List<String> words) {
        this.theme = theme;
        this.words = words;
    }

    /**
     * Restituisce il tema logico che accomuna le parole del gruppo.
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Restituisce la lista esatta delle parole appartenenti a questo tema.
     */
    public List<String> getWords() {
        return words;
    }
}
