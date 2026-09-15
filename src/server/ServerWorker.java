package server;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import client.NioUtils;
import protocol.request.HandlerRequest;
import protocol.request.LoginRequest;
import protocol.request.LogoutRequest;
import protocol.request.RegisterRequest;
import protocol.request.Request;
import protocol.request.RequestConfigUDP;
import protocol.request.RequestGameInfoRequest;
import protocol.request.RequestGameStatsRequest;
import protocol.request.RequestLeaderboardRequest;
import protocol.request.RequestPlayerStatsRequest;
import protocol.request.SubmitProposalRequest;
import protocol.request.UpdateCredentialRequest;
import protocol.response.HandlerResponse;
import protocol.response.Response;
import protocol.response.ResponseCodes;

/**
 * Gestisce la comunicazione TCP con un singolo client in un thread separato. Il
 * ServerWorker funge da "ponte": riceve i messaggi JSON dal client tramite NIO,
 * li traduce in oggetti Java, invoca la logica di gestione nel ServerController
 * condiviso e rispedisce l'esito formattato in JSON al client. Mantiene anche
 * lo "stato della sessione" locale, memorizzando quale utente è loggato su
 * questa connessione.
 */
public class ServerWorker implements Runnable {

    private final SocketChannel clientChannel;// Il canale socket(NIO) assegnato specificamente a questo client
    private final ServerController controller;// Il riferimento all'unica istanza condivisa che contiene la logica del gioco
    private String loggedUser = null;  // Lo stato locale della sessione: tiene traccia di chi sta usando questa connessione

    /**
     * Costruisce un nuovo worker per servire un client appena connesso.
     *
     * @param clientChannel Il canale TCP stabilito con il client.
     * @param controller Il gestore centrale per processare le richieste.
     */
    public ServerWorker(SocketChannel clientChannel, ServerController controller) {
        this.clientChannel = clientChannel;
        this.controller = controller;
    }

    /**
     * Metodo principale del thread. Implementa un ciclo di lettura/scrittura
     * continuo che si interrompe solo quando il client si scollega o invia un
     * comando di uscita.
     */
    @Override
    public void run() {
        System.out.println("Worker avviato per: " + clientChannel.socket().getInetAddress());

        try {
            String jsonLine;
            // Legge dal client usando i ByteBuffer NIO finché non cade la connessione
            while ((jsonLine = NioUtils.readJson(clientChannel)) != null) {
                System.out.println("Ricevuto: " + jsonLine);

                // Comando di interruzione manuale della connessione
                if (jsonLine.trim().equalsIgnoreCase("ESCI")) {
                    break;
                }

                Response response;

                try {
                    // Deserializza la stringa JSON in un oggetto Request del tipo specifico  
                    Request request = HandlerRequest.parse(jsonLine);

                    // ROUTING DELLE RICHIESTE: Smista in base all'attributo "operation"
                    switch (request.operation) {
                        case "register":
                            response = controller.handleRegister((RegisterRequest) request);
                            break;

                        case "login":
                            // Se il login ha successo, memorizza l'username nella sessione corrente del worker
                            response = controller.handleLogin((LoginRequest) request);
                            if (response.responseCode == ResponseCodes.OK) {
                                this.loggedUser = ((LoginRequest) request).username;
                            }
                            break;

                        case "logout":
                            // Se il logout ha successo, "pulisce" la sessione locale
                            response = controller.handleLogout((LogoutRequest) request, this.loggedUser);
                            if (response.responseCode == ResponseCodes.OK) {
                                this.loggedUser = null;
                            }
                            break;

                        case "updateCredentials":
                            UpdateCredentialRequest updateReq = (UpdateCredentialRequest) request;
                            response = controller.handleUpdateCredential(updateReq);

                            // Se l'aggiornamento è andato a buon fine e c'è un nuovo username, aggiorna la sessione
                            if (response.responseCode == ResponseCodes.OK) {
                                if (updateReq.newUsername != null && !updateReq.newUsername.isEmpty()) {
                                    this.loggedUser = updateReq.newUsername;
                                }
                            }
                            break;

                        case "submitProposal":
                            // Usa l'utente memorizzato nella sessione per garantirne l'identità
                            response = controller.HandleSubmitProposalRequest((SubmitProposalRequest) request, loggedUser);
                            break;

                        case "requestGameStats":
                            response = controller.handleRequestGameStatsRequest((RequestGameStatsRequest) request);
                            break;
                        case "requestGameInfo":
                            response = controller.handleRequestGameInfoRequest((RequestGameInfoRequest) request, loggedUser);
                            break;
                        case "requestLeaderboard":
                            response = controller.handleRequestLeaderboardRequest((RequestLeaderboardRequest) request);
                            break;
                        case "requestPlayerStats":
                            response = controller.handleRequestPlayerStatsRequest((RequestPlayerStatsRequest) request, loggedUser);
                            break;
                        case "presentationUDP":
                            // Associa la porta UDP inviata dal client alla connessione TCP corrente (indirizzo IP)
                            response = controller.handleConfigUDP((RequestConfigUDP) request, clientChannel.socket());
                            break;
                        default:
                            // Operazione sconosciuta o formato JSON non previsto dalle specifiche
                            response = new Response(
                                    request.operation,
                                    ResponseCodes.BAD_REQUEST,
                                    ResponseCodes.getDefaultMessage(ResponseCodes.BAD_REQUEST) + " (Op sconosciuta)"
                            );
                    }
                } catch (IllegalArgumentException e) {
                    // Errore scatenato se il JSON ricevuto è sintatticamente malformato o manca di campi obbligatori
                    response = new Response(
                            "error",
                            ResponseCodes.BAD_REQUEST,
                            ResponseCodes.getDefaultMessage(ResponseCodes.BAD_REQUEST) + ": " + e.getMessage()
                    );
                }

                // Scrivo la risposta al client in Json tramite NIO ByteBuffer
                if (response != null) {
                    NioUtils.sendJson(clientChannel, HandlerResponse.toJson(response));
                }
            }

        } catch (IOException e) {
            System.err.println("Errore I/O: " + e.getMessage());
        } finally {
            // Se il client era loggato ma la connessione è caduta, libero la sua presenza nel controller centrale
            if (loggedUser != null) {
                controller.disconnettiUtente(loggedUser);
            }

            // tolgo il client dalla mappa di broadcast UDP per non inviare datagrammi "a vuoto"
            controller.rimuoviClientUDP(clientChannel.socket());

            // chido esplicitamente il SocketChannel NIO
            try {
                clientChannel.close();
                System.out.println("Client chiuso.");
            } catch (IOException e) {
            }
        }
    }
}
