package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce l'invio di notifiche asincrone UDP ai client registrati. Come
 * richiesto dalle specifiche, il server utilizza comunicazioni asincrone
 * (tramite UDP) per eventi come la notifica di fine partita allo scadere del
 * tempo. Questa classe mappa la connessione TCP (Socket) di un client al suo
 * indirizzo UDP per sapere esattamente a chi e dove inviare i datagrammi.
 */
public class UdpNotificationManager {

    // Socket utilizzato dal server per inviare i pacchetti UDP
    private final DatagramSocket udpSocket;

    // Struttura dati thread-safe per memorizzare i client registrati alle notifiche.
    // La chiave è il Socket TCP (identificativo univoco della sessione), il valore è l'indirizzo UDP.
    private final Map<Socket, InetSocketAddress> clientAddresses;

    /**
     * Inizializza il gestore delle notifiche UDP sulla porta specificata.
     *
     * @param udpPort La porta su cui il server aprirà il DatagramSocket.
     * @throws SocketException Se c'è un errore nell'apertura del socket
     */
    public UdpNotificationManager(int udpPort) throws SocketException {
        this.udpSocket = new DatagramSocket(udpPort);
        this.clientAddresses = new ConcurrentHashMap<>();
        System.out.println("[UDP] Notification Manager attivo sulla porta " + udpPort);
    }

    /**
     * Registra un client per ricevere notifiche UDP. L'indirizzo IP del client
     * viene ricavato direttamente dalla sua connessione TCP affidabile, mentre
     * la porta UDP è quella comunicata dal client stesso durante la fase di
     * setup/login.
     *
     * @param tcpSocket Il socket TCP della connessione attiva col client.
     * @param udpPort La porta UDP su cui il client è in ascolto per le
     * notifiche.
     */
    public void registerClient(Socket tcpSocket, int udpPort) {

        InetAddress address = tcpSocket.getInetAddress();
        InetSocketAddress udpAddress = new InetSocketAddress(address, udpPort);

        clientAddresses.put(tcpSocket, udpAddress);
        System.out.println("[UDP] Client registrato: " + address.getHostAddress() + ":" + udpPort);
    }

    /**
     * Rimuove un client dalla mappa delle notifiche. Questo metodo deve essere
     * chiamato quando un client effettua il logout, si disconnette
     * volontariamente o la sua connessione TCP cade inaspettatamente.
     *
     * @param tcpSocket Il socket TCP, (che funge da chaive nella mappa)
     * associato al client da rimuovere.
     */
    public void unregisterClient(Socket tcpSocket) {
        InetSocketAddress removed = clientAddresses.remove(tcpSocket);
        if (removed != null) {
            System.out.println("[UDP] Client rimosso dalle notifiche: " + removed);
        }
    }

    /**
     * Invia una singola notifica UDP a uno specifico client.
     *
     * @param tcpSocket Identificativo del client destinatario.
     * @param message Il messaggio da inviare (formattato in JSON).
     */
    public void sendNotification(Socket tcpSocket, String message) {
        InetSocketAddress clientAddress = clientAddresses.get(tcpSocket);

        // controllo se il client è effettivamente registrato per ricevere UDP
        if (clientAddress == null) {
            System.err.println("[UDP] Tentativo di notifica a client non registrato");
            return;
        }

        try {
            // Conversione del messaggio in array di byte usando codifica standard UTF-8
            byte[] data = message.getBytes("UTF-8");
            // Creazione del datagramma specificando payload, lunghezza, IP e porta di destinazione
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    clientAddress.getAddress(),
                    clientAddress.getPort()
            );

            udpSocket.send(packet);//invio del pacchetto udp, 
            System.out.println("[UDP] Notifica inviata a " + clientAddress + ": " + message);
        } catch (IOException e) {
            System.err.println("[UDP] Errore invio notifica: " + e.getMessage());
        }
    }

    /**
     * Invia un messaggio in broadcast a tutti i client in gioco . Questo metodo
     * è essenziale per notificare a tutti gli utenti attivi la fine di una
     * partita.
     *
     * @param message Il messaggio json da inoltrare a tutti.
     */
    public void broadcastNotification(String message) {
        //se non c'e nessuno in gioco, non invio il messaggio
        if (clientAddresses.isEmpty()) {
            return;
        }
        System.out.println("[UDP] Broadcasting a " + clientAddresses.size() + " client");

        //invia la notifica a tutti i client in gioco
        for (Socket socket : clientAddresses.keySet()) {
            sendNotification(socket, message);
        }
    }

    /**
     * Chiude il DatagramSocket e svuota la lista dei client. Da invocare
     * unicamente durante lo spegnimento (shutdown) del server per rilasciare le
     * risorse di rete.
     */
    public void close() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        clientAddresses.clear();
    }
}
