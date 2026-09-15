package protocol.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * classe Factory per la gestione e il parsing dei messaggi di risposta. viene
 * utilizzata principalmente dal client per convertire le stringhe JSON ricevute
 * dal server nell'oggetto Java (Response) appropriato, sfruttando il
 * polimorfismo.
 */
public class HandlerResponse {

    private static final Gson gson = new Gson();

    /**
     * Factory Method che analizza una stringa JSON e restituisce la specifica
     * classe figlia di Response. Utilizza il campo "operation" per capire quale
     * cast e deserializzazione applicare.
     *
     * @param jsonLine La stringa JSON grezza ricevuta in risposta dal server.
     * @return L'istanza dell'oggetto Response specifico (es. LoginResponse,
     * GameInfoResponse).
     * @throws IllegalArgumentException Se il JSON è nullo, vuoto, privo di
     * 'operation' o malformato.
     */
    public static Response parse(String jsonLine) throws IllegalArgumentException {
        if (jsonLine == null || jsonLine.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON risposta vuoto");
        }
        try {
            // conversione generica per estrarre la chiave di instradamento
            JsonObject jsonObject = gson.fromJson(jsonLine, JsonObject.class);

            if (!jsonObject.has("operation")) {
                throw new IllegalArgumentException("Campo 'operation' mancante nella risposta");
            }
            // estrae il nome dell'operazione per instanziare la classe coretta
            String operation = jsonObject.get("operation").getAsString();

            switch (operation) {
                case "login":
                    return gson.fromJson(jsonLine, LoginResponse.class);

                case "submitProposal":
                    return gson.fromJson(jsonLine, ProposalResponse.class);

                case "requestGameInfo":
                    return gson.fromJson(jsonLine, GameInfoResponse.class);

                case "requestGameStats":
                    return gson.fromJson(jsonLine, GameStatsResponse.class);

                case "requestLeaderboard":
                    return gson.fromJson(jsonLine, LeaderboardResponse.class);

                case "requestPlayerStats":
                    return gson.fromJson(jsonLine, PlayerStatsResponse.class);

                // Operazioni base che non necessitano di payload extra, 
                // quindi deserializzate direttamente nella classe padre Response
                case "register":
                case "logout":
                case "configUDP":
                case "updateCredentials":
                    return gson.fromJson(jsonLine, Response.class);

                default:
                    // se l'operazione è sconosciuta, genero una risposta base 
                    // per evitare il crash del client e consentire la lettura dell'eventuale errorMessage.
                    return gson.fromJson(jsonLine, Response.class);
            }
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON risposta malformato: " + e.getMessage());
        }
    }

    /**
     * Metodo di utilità per convertire un oggetto Response nella sua
     * rappresentazione stringa JSON. Utilizzato dal server (ServerWorker) prima
     * di inviare i messaggi TCP al client.
     *
     * @param response L'oggetto di risposta da serializzare.
     * @return La stringa JSON da inviare in rete.
     */
    public static String toJson(Response response) {
        return gson.toJson(response);
    }
}
