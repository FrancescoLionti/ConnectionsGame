package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import protocol.response.EndGameResponse;
import server.model.PlayerGameState;

/**
 * Thread dedicato all'ascolto delle notifiche asincrone in formato UDP
 * provenienti dal server. Questo thread intercetta il messaggio dal server,
 * processa la classifica e aggiorna in tempo reale l'interfaccia dell'utente.
 */
public class UdpListener implements Runnable {

    private static final Gson gson = new Gson();

    private final DatagramSocket udpSocket;
    private final int port;
    private volatile boolean running = true;
    private volatile boolean isLogged = false;

    /**
     * Costruttore del listener UDP.
     *
     * @param udpSocket Il socket UDP già inizializzato e associato alla porta
     * corretta.
     * @param port La porta UDP su cui si è in ascolto (per log).
     */
    public UdpListener(DatagramSocket udpSocket, int port) {
        this.udpSocket = udpSocket;
        this.port = port;
    }

    /**
     * Aggiorna lo stato di autenticazione dell'utente. Invocato dal clientMain
     * quando ha seguito un login o logout (riuscito).
     *
     * @param logged true se l'utente è connesso, false altrimenti.
     */
    public void setLogged(boolean logged) {
        this.isLogged = logged;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[65507];
        System.out.println("[UDP] In ascolto sulla porta " + port);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length); //conterra il messaggio ricevuto dal server
        try {

            while (running && !Thread.currentThread().isInterrupted()) {

                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8"); //converto i byte in una stringa

                //  Se non loggato, ignora il pacchetto
                if (!isLogged) {
                    continue;
                }
                // deserializza il mesaggio di fine partita
                EndGameResponse messaggioFine = gson.fromJson(message, EndGameResponse.class);

                // stampa dell'interfaccia e della classifica della partita conclusa 
                System.out.println(Menu.GAME_ENDED);
                System.out.println("\n" + "═".repeat(72));
                System.out.println("                      CLASSIFICA PARTITA CONCLUSA");
                System.out.println("═".repeat(72));

                Map<String, PlayerGameState> classificaMap = messaggioFine.classifica;

                // converto la mappa in una lista di Entry (Chiave-Valore) per poterla ordinare
                List<Map.Entry<String, PlayerGameState>> classifica = new ArrayList<>(classificaMap.entrySet());

                // ordino la lista per punteggio in ordine decrescente
                classifica.sort((e1, e2) -> Integer.compare(e2.getValue().getPunteggio(), e1.getValue().getPunteggio()));

                //struttura grafica della classifica
                System.out.printf("| %-3s | %-15s | %-10s | %-8s | %-17s |%n", "POS", "GIOCATORE", "PUNTEGGIO", "ERRORI", "GRUPPI INDOVINATI");
                System.out.println("|" + "─".repeat(70) + "|");

                // Riga per ogni giocatore
                for (int i = 0; i < classifica.size(); i++) {
                    Map.Entry<String, PlayerGameState> entry = classifica.get(i);

                    String username = entry.getKey();          // prendo il nome del player (è la chiave della mappa)
                    PlayerGameState player = entry.getValue(); // prendo I dati del giocatore (sono il valore della mappa)
                    int pos = i + 1;
                    String posStr = String.format("%3d", pos);

                    // previene eccezione se la lista dei gruppi indovinati dovesse essere nulla
                    int numeroGruppi = (player.getGruppiIndovinati() != null) ? player.getGruppiIndovinati().size() : 0;

                    //stmapa della riga
                    System.out.printf("| %s | %-15s | %10d | %8d | %17d |%n",
                            posStr,
                            username,
                            player.getPunteggio(),
                            player.getErrori(),
                            numeroGruppi);
                }

                //stampa a schermo della chiusura grafica della classifica
                System.out.println("═".repeat(72));
                System.out.printf("Giocatori totali: %d%n", classifica.size());
                System.out.println();
                System.out.println("═══════════════════════════════════════\n");

                //stampa a schermo delle parole della nuova partita
                System.out.println("• Parole della nuova partita :" + messaggioFine.newWard);
                System.out.println(Menu.LOGGED);//ristampa il menu
                System.out.print("\033[1;32mTU >> \033[0m"); // Ristampa il prompt
            }
        } catch (IOException e) {
            if (!udpSocket.isClosed()) {
                System.err.println("[UDP] Errore ricezione: " + e.getMessage());
            }
        }

        System.out.println("[UDP] Listener terminato");
    }

    /**
     * Ferma il thread impostando il flag a false. Usato in fase di chiusura del
     * client.
     */
    public void stop() {
        running = false;
    }
}
