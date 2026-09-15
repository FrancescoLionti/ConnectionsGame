package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import utils.ConfigServer;

/**
 * Classe principale del server per il gioco Connections. Si occupa
 * dell'inizializzazione del sistema, caricando la configurazione da file e
 * aviando i servizi principali: il controller di gioco, il manager per le
 * notifiche UDP e il ServerSocketChannel per accettare le connessioni TCP in
 * ingresso tramite NIO. Gestisce inoltre il pool di thread per i worker che
 * serviranno i singoli client.
 */
public class ServerMain {

    public static void main(String[] args) {
        System.out.println("--- CONNESSIONE AL SERVER ---");

        //carico la configurazione del server, prendendo le informazioni dall'apposito file
        ConfigServer config;
        try {
            config = ConfigServer.load("config/ServerConfig.json");
            System.out.println("Configurazione caricata correttemente.");
            System.out.println("PortaTCP: " + config.serverPort);
            System.out.println("PortaUDP: " + config.udpServerPort);
        } catch (IOException e) {
            System.err.println("Errore nel caricamento del file di configurazione: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Creazione del controller centrale e' un'unica istanza condivisa tra tutti i thread, responsabile della gestione 
        // dello stato del gioco, degli utenti e delle relative strutture dati (concorrenti).
        ServerController controller = new ServerController(config.gameDurationSeconds, config.gameHistoryFile, config.rankingFilePath, config.userFilePath, config.connectionsDataPath);

        // creazione di CachedThreadPool che crea nuovi thread secondo necessità e 
        // riutilizza quelli precedentemente costruiti quando sono disponibili.
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // Inizializzazione del gestore per le notifiche UDP asincrone (ovvero partita terminata).
        try {
            int udpPort = config.udpServerPort;
            UdpNotificationManager udpManager = new UdpNotificationManager(udpPort);
            controller.setUdpManager(udpManager);
            System.out.println("Porta UDP: " + udpPort);
        } catch (Exception e) {
            System.err.println("Errore inizializzazione UDP: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Avvio del Server Socket utilizzando Java NIO (ServerSocketChannel)
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            // il server si mette in ascolto sulla porta specificata nel file di configurazione
            serverSocket.bind(new InetSocketAddress(config.serverPort));
            System.out.println("Server in ascolto sulla porta " + config.serverPort + "...");

            // ciclo infinito per accettare le connessioni in ingresso dai client
            while (true) {
                try {
                    SocketChannel clientSocket = serverSocket.accept();
                    System.out.println("Nuovo client connesso: " + clientSocket.getRemoteAddress());

                    // Creazione ed inizializzazione di un nuovo ServerWorker dedicato a questo specifico client.
                    ServerWorker worker = new ServerWorker(clientSocket, controller);

                    // sottomissiione del task del worker al thread pool per l'esecuzione concorrente
                    threadPool.submit(worker);

                } catch (IOException e) {
                    System.err.println("Errore nell'accettazione client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Errore critico del server: " + e.getMessage());
        } finally {
            // Chiusura pulita delle risorse in caso di interruzione del ciclo principale
            threadPool.shutdown();
            controller.chiudiProvider();
        }
    }
}
