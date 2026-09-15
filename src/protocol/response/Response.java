package protocol.response;

/**
 * Classe base che rappresenta una risposta standard del server. Contiene
 * l'operazione eseguita, il codice di stato (es. 200, 400) e un eventuale
 * messaggio. Viene estesa da altre classi per aggiungere payload specifici (es.
 * dati di gioco).
 */
public class Response {

    public String operation; // L'operazione a cui questa risposta fa riferimento (es. "login",°register°,ecc)
    public int responseCode; // Codice di esito dell'operazione (es. 200 = OK, 401 = Unauthorized)
    public String errorMessage;// il messagggio associato all'esito o all'errore

    /**
     * Inizializza una risposta generica.
     *
     * @param operation L'operazione richiesta.
     * @param responseCode Il codice numerico di esito.
     * @param errorMessage Il messaggio di accompagnamento o di errore.
     */
    public Response(String operation, int responseCode, String errorMessage) {
        this.operation = operation;
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
    }

}
