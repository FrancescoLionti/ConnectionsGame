package client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioUtils {

    /**
     * Invia una stringa JSON al server tramite SocketChannel.
     */
    public static void sendJson(SocketChannel channel, String json) throws IOException {
        String message = json + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    /**
     * Legge una risposta JSON dal SocketChannel fermandosi al carattere '\n'.
     * Supporta correttamente la codifica UTF-8 per le lettere accentate.
     */
    public static String readJson(SocketChannel channel) throws IOException {
        // Usiamo un ByteArrayOutputStream per accumulare i byte raw
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1);

        boolean hasData = false;

        while (channel.read(buffer) > 0) {
            hasData = true;
            buffer.flip();
            byte b = buffer.get();
            buffer.clear();

            if (b == '\n') {
                break; // Fine del messaggio

            }
            if (b != '\r') {
                baos.write(b); // Accumula il byte

            }
        }

        // Se non abbiamo letto nulla, significa che la connessione è chiusa
        if (!hasData && baos.size() == 0) {
            return null;
        }

        // Converte tutti i byte accumulati in una vera Stringa UTF-8
        return baos.toString(StandardCharsets.UTF_8);
    }
}
