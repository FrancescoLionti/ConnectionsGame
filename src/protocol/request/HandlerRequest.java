package protocol.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Factory per la gestione e il parsing delle richieste. Centralizza la logica
 * di conversione da stringhe JSON ricevute sulla rete alle classi concrete del
 * Request.
 */
public class HandlerRequest {

    private static final Gson gson = new Gson();

    /**
     * Factory Method che analizza una stringa JSON e restituisce l'oggetto
     * Request specifico corrispondente. Utilizza un approccio a due fasi: 1.
     * Deserializza la stringa in un JsonObject generico per leggere il campo
     * obbligatorio "operation". 2. Usa uno switch per delegare a Gson la
     * creazione della sottoclasse corretta (es. LoginRequest, RegisterRequest).
     *
     * @param jsonLine La stringa JSON grezza ricevuta dal client tramite il
     * SocketChannel.
     * @return Un'istanza della sottoclasse concreta di Request corrispondente
     * all'operazione.
     * @throws IllegalArgumentException Se la stringa è nulla, vuota,
     * malformata, se manca il campo "operation", o se l'operazione specificata
     * non è supportata dal protocollo.
     */
    public static Request parse(String jsonLine) throws IllegalArgumentException {
        if (jsonLine == null || jsonLine.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON vuoto");
        }
        try {
            //converto la stringa JSON in un oggetto generico Json
            JsonObject jsonObject = gson.fromJson(jsonLine, JsonObject.class);

            //se non ha il campo operation lancio un errore
            if (!jsonObject.has("operation")) {
                throw new IllegalArgumentException("Campo 'operation' mancante");
            }

            //prendo la stringa che rappresenta il tipo di richiesta
            String operation = jsonObject.get("operation").getAsString();

            //istanzio l'oggetto Request specifico in base al tipo di operazione
            switch (operation) {
                case "register":
                    return gson.fromJson(jsonLine, RegisterRequest.class);
                case "login":
                    return gson.fromJson(jsonLine, LoginRequest.class);
                case "logout":
                    return gson.fromJson(jsonLine, LogoutRequest.class);
                case "updateCredentials":
                    return gson.fromJson(jsonLine, UpdateCredentialRequest.class);
                case "submitProposal":
                    return gson.fromJson(jsonLine, SubmitProposalRequest.class);
                case "requestPlayerStats":
                    return gson.fromJson(jsonLine, RequestPlayerStatsRequest.class);
                case "requestGameInfo":
                    return gson.fromJson(jsonLine, RequestGameInfoRequest.class);
                case "requestLeaderboard":
                    return gson.fromJson(jsonLine, RequestLeaderboardRequest.class);
                case "requestGameStats":
                    return gson.fromJson(jsonLine, RequestGameStatsRequest.class);
                case "presentationUDP":
                    return gson.fromJson(jsonLine, RequestConfigUDP.class);
                default:
                    throw new IllegalArgumentException("Operazione sconosciuta: " + operation);
            }
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON malformato: " + e.getMessage());
        }
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
