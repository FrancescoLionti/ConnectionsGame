package protocol.request;

/**
 * Classe astratta base per tutte le richieste inviate dal client al server.
 * Impone il campo obbligatorio "operation".
 */
public abstract class Request {

    public String operation;

    /**
     * Costruttore base invocato dalle sottoclassi.
     *
     * @param operation Il tipo della richiesta specifica.
     */
    public Request(String operation) {
        this.operation = operation;
    }

}
