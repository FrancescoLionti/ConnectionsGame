package protocol.response;

/**
 * Codici di risposta standard per il protocollo
 */
public class ResponseCodes {

    // ===== SUCCESSI (2xx) =====
    public static final int OK = 200;           // Operazione riuscita
    public static final int CREATED = 201;      // Risorsa creata (es. registrazione)

    // ===== ERRORI CLIENT (4xx) =====
    public static final int BAD_REQUEST = 400;  // Input malformato o mancante
    public static final int UNAUTHORIZED = 401; // Credenziali errate
    public static final int UNAUTHORIZEDLOG = 402; // Logout da non loggato
    public static final int FORBIDDEN = 403;    // Operazione non permessa
    public static final int NOT_FOUND = 404;    // Risorsa non trovata (es. partita inesistente)
    public static final int CONFLICT = 409;     // Username già in uso

    // ===== ERRORI SERVER (5xx) =====
    public static final int INTERNAL_ERROR = 500; // Errore generico del server
    public static final int SERVICE_UNAVAILABLE = 503; // Server offline o in chiusura

    // ===== CODICI GIOCO (1xxx) =====
    public static final int WINNER_ALLERT = 1000; // Il giocatore ha già vinto la partita
    public static final int LOSE_ALLERT = 1001;   // Il giocatore ha già perso la partita

    /**
     * Restituisce un messaggio descrittivo standard associato a uno specifico
     * codice di risposta. Viene utilizzato per popolare il campo errorMessage
     * delle Response quando non è richiesto un testo custom.
     *
     * @param code Il codice intero della risposta.
     * @return Una stringa contenente la spiegazione dell'esito o dell'errore.
     */
    public static String getDefaultMessage(int code) {
        switch (code) {
            case OK:
                return "Operazione completata con successo";
            case CREATED:
                return "Risorsa creata con successo";
            case BAD_REQUEST:
                return "Richiesta non valida, reinserire correttamente i dati";
            case UNAUTHORIZED:
                return "Credenziali errate";
            case UNAUTHORIZEDLOG:
                return "Logout non permesso ad utente non loggato";
            case FORBIDDEN:
                return "Operazione non consentita";
            case NOT_FOUND:
                return "Risorsa non trovata";
            case CONFLICT:
                return "Conflitto, nome utente gia registrato";
            case INTERNAL_ERROR:
                return "Errore interno del server";
            case SERVICE_UNAVAILABLE:
                return "Servizio temporaneamente non disponibile";
            default:
                return "Errore sconosciuto";
        }
    }

}
