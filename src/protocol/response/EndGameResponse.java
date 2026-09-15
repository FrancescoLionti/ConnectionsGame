package protocol.response;

import java.util.List;
import java.util.Map;

import server.model.PlayerGameState;

/**
 * Risposta asincrona inviata in broadcast (UDP) allo scadere del timer della
 * partita. Contiene la classifica finale dei giocatori e le nuove parole per la
 * partita successiva.
 */
public class EndGameResponse {

    public List<String> newWard; // Lista delle 16 nuove parole per la prossima partita, inviata per far continuare il gioco in automatico
    public Map<String, PlayerGameState> classifica; // Mappa che associa ad ogni username il suo stato finale (punteggio, errori, ecc.) nella partita appena conclusa

    /**
     * Costruisce la risposta di fine partita da inviare ai client.
     *
     * @param classifica La mappa con i risultati finali di tutti i
     * partecipanti.
     * @param newWard Le nuove parole estratte per il prossimo turno.
     */
    public EndGameResponse(Map<String, PlayerGameState> classifica, List<String> newWard) {
        this.newWard = newWard;
        this.classifica = classifica;
    }

}
