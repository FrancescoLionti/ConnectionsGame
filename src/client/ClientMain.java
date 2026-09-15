package client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import protocol.response.GameInfoResponse;
import protocol.response.GameStatsResponse;
import protocol.response.HandlerResponse;
import protocol.response.LeaderboardResponse;
import protocol.response.LoginResponse;
import protocol.response.PlayerStatsResponse;
import protocol.response.Response;
import utils.ConfigClient;

/**
 * Punto di ingresso principale dell'applicazione Client. Gestisce
 * l'inizializzazione delle connessioni di rete (TCP per comandi, UDP per
 * notifiche asincrone), l'interazione con l'utente tramite riga di comando e
 * l'instradamento delle risposte ricevute dal server verso i metodi di
 * rendering della UI.
 */
public class ClientMain {

    // flag di stato: determina quale menu (LOGGED o NOT_LOGGED) deve essere mostrato all'utente
    private static boolean interfacciaLog = false;

    public static void main(String[] args) {

        //Carica la configurazione dal file di configurazione
        ConfigClient config = null;
        try {
            config = ConfigClient.load("config/ClientConfig.json");
            System.out.println("Connesso a " + config.serverAddress + ":" + config.serverPort);
        } catch (IOException e) {
            System.err.println("Errore config: " + e.getMessage());
            System.exit(1);
        }

        // inizializzazione rete UDP e Trhead asincrono.
        DatagramSocket udpSocket = null;
        ExecutorService udpExecutor = null;
        try {
            // Creazione del socket UDP per ricevere le notifiche di fine partita dal server
            udpSocket = new DatagramSocket(config.clientPortUDP);
            int actualPort = udpSocket.getLocalPort(); //ottiene la porta assegnata dall OS 

            // crezione ExecutorService con single thread (per ascolto UDP)
            udpExecutor = Executors.newSingleThreadExecutor();

            // avvio del thread che sta in ascolto
            UdpListener listener = new UdpListener(udpSocket, actualPort);
            udpExecutor.execute(listener);

            // inizializzazione rete TCP 
            try (SocketChannel socket = SocketChannel.open()) {
                socket.connect(new InetSocketAddress(config.serverAddress, config.serverPort));

                try (Scanner userIn = new Scanner(System.in)) {

                    // Comunica subito al server su quale porta locale UDP questo client è in ascolto
                    GameInput.doPresentetionUdp(socket, actualPort);

                    // Atende la risposta del server alla registrazione UDP
                    String udpResponse = NioUtils.readJson(socket);
                    if (udpResponse != null) {
                        Response response = HandlerResponse.parse(udpResponse);
                        if ("configUDP".equals(response.operation)) {
                            GameUI.printPresentation(response); //mostra a schermo Il titolo di presentazione
                        }
                    }

                    // CICLO PRINCIPALE DI GIOCO
                    while (true) {
                        // STAMPA MENU E ACQUISIZIONE INPUT 
                        if (!interfacciaLog) {
                            // Utente NON connesso
                            System.out.println(Menu.NOT_LOGGED);
                            System.out.print("\033[1;32mTU >> \033[0m");
                            String input = userIn.nextLine().trim();

                            switch (input) {
                                case "1": {
                                    GameInput.doRegister(userIn, socket);
                                    break;
                                }
                                case "2": {
                                    GameInput.doLogin(userIn, socket);
                                    break;
                                }

                                case "3": {
                                    GameInput.doUpdateCredentials(userIn, socket);
                                    break;
                                }

                                case "0":
                                case "exit":
                                    System.out.println("Disconnesso!");
                                    return; // Esce dal main chiudendo il programma
                                default:
                                    System.out.println("!! Devi inserire il numero corrispondente all'operazione ammessa");
                                    continue;
                            }
                        } else {
                            // Utente CONNESSO (Autorizzato a giocare)
                            System.out.println(Menu.LOGGED);
                            System.out.print("\033[1;32mTU >> \033[0m");

                            String input = userIn.nextLine().trim();
                            switch (input) {

                                case "1": {
                                    GameInput.doUpdateCredentials(userIn, socket);
                                    break;
                                }

                                case "2": {
                                    GameInput.doLogout(socket);
                                    break;
                                }

                                case "3": {
                                    GameInput.doProposal(userIn, socket);
                                    break;
                                }
                                case "4": {
                                    GameInput.doRequestGameInfo(userIn, socket);
                                    break;
                                }
                                case "5": {
                                    GameInput.doRequestGameStats(userIn, socket);
                                    break;
                                }
                                case "6": {
                                    GameInput.doLeaderboardRequest(userIn, socket);
                                    break;
                                }
                                case "7": {
                                    GameInput.doPlayerStatsRequest(socket);
                                    break;
                                }
                                case "0":
                                case "exit":
                                    System.out.println("Disconnesso!");
                                    return; // Esce dal main chiudendo il programma

                                default:
                                    System.out.println("!! Devi inserire il numero corrispondente all'operazione ammessa");
                                    continue;
                            }
                        }
                        // RICEZIONE E DISPATCHING DELLA RISPOSTA SERVER tramite buffer NIO
                        String responseJson = NioUtils.readJson(socket); // Chiamata bloccante

                        if (responseJson != null) {
                            //parsing della risposta usando la factory
                            Response response = HandlerResponse.parse(responseJson);
                            switch (response.operation) {
                                case "register":
                                    GameUI.printRegister(response);
                                    break;
                                case "login":
                                    if (response instanceof LoginResponse loginResponse) {
                                        // se il login ha successo printLogin ritorna true, aggiornando il menu al prossimo giro
                                        interfacciaLog = GameUI.printLogin(loginResponse);
                                        // somunica al trhead asincrono che l'utente è loggato e può ricevere broadcast
                                        listener.setLogged(interfacciaLog);
                                    }
                                    break;
                                case "logout":
                                    // printLogout ritorna sempre false, resettando l'interfaccia a NOT_LOGGED
                                    interfacciaLog = GameUI.printLogout(response);
                                    listener.setLogged(interfacciaLog);
                                    break;
                                case "updateCredentials":
                                    GameUI.printUpdateCredential(response);
                                    break;
                                case "submitProposal":
                                    GameUI.printProposal(response);
                                    break;
                                case "requestGameStats":
                                    if (response instanceof GameStatsResponse statsResponse) {
                                        GameUI.printGameStats(statsResponse);
                                    }
                                    break;
                                case "requestGameInfo":
                                    if (response instanceof GameInfoResponse infoResponse) {
                                        GameUI.printGameInfo(infoResponse);
                                    }
                                    break;
                                case "requestLeaderboard":
                                    if (response instanceof LeaderboardResponse leadResponse) {
                                        GameUI.printLeaderboard(leadResponse);
                                    }
                                    break;
                                case "requestPlayerStats":
                                    if (response instanceof PlayerStatsResponse leadResponse) {
                                        GameUI.printPlayerStats(leadResponse);
                                    }
                                    break;
                                default:
                                    System.out.println(" !!!! Risposta del server corrotta, !!!!");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore: " + e.getMessage());
        } finally {
            // Shutdown executor, con chiusura controllata
            if (udpExecutor != null) {
                udpExecutor.shutdown(); //ferma l'accettazione di nuovi task, ma completa quelli in esecuzione
                try {
                    if (!udpExecutor.awaitTermination(4, TimeUnit.SECONDS)) { //aspetta max 4 secndi che il thread UDP finisca
                        udpExecutor.shutdownNow();// forza la chiusura
                    }
                } catch (InterruptedException e) { // se il thrad corrente viene interrotto durante l'attesa
                    udpExecutor.shutdownNow();// Forza comunque la chiusura
                    Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
                }
            }

            // ciusura socket UDP
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close(); // Chiude il socket e rilascia la porta
                System.out.println("UDP Socket chiuso");
            }
        }
    } // Chiusura main

} // Chiusura classe
