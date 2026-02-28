package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Handles a single client connection using Blocking I/O.
 * <p>
 * This class wraps the client socket and runs the read-loop in the current thread.
 * It is responsible for:
 * 1. Reading data from the socket.
 * 2. Decoding raw bytes into messages.
 * 3. Passing messages to the Protocol.
 * 4. Sending responses back to the client (via {@link #send(Object)}).
 *
 * @param <T> The type of message (e.g., String).
 */
public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final StompMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    // J5: stored so run() can call connections.disconnect() for cleanup on exit.
    private int connectionId = -1;

    /**
     * Constructor.
     * Initializes the I/O streams and registers this handler with the global Connections service.
     *
     * @param sock     The client socket.
     * @param reader   The encoder/decoder for this client.
     * @param protocol The protocol instance for this client.
     */
    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, StompMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;

        // --- Critical Initialization Order ---
        // We initialize the I/O streams *before* registering the handler with the global Connections map.
        // This prevents a potential race condition where a broadcast message (e.g., from another client)
        // attempts to write to this handler's 'out' stream before it is fully instantiated.
        try {
            this.in = new BufferedInputStream(sock.getInputStream());
            this.out = new BufferedOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }

        // Once streams are ready, register to the Connections singleton
        ConnectionsImpl<T> connections = ConnectionsImpl.getInstance();

        // Add to active handlers map and get a unique Connection ID
        this.connectionId = connections.addActiveHandler(this);

        // Start the protocol with the assigned ID
        protocol.start(this.connectionId, connections);
    }

    /**
     * The main read-loop.
     * Continuously reads bytes from the socket, decodes them, and processes complete messages.
     * J5: After the loop exits (graceful disconnect, error, or EOF) we call
     *     connections.disconnect() to remove this connection from the active-connections map
     *     and all channel subscription lists, preventing resource leaks.
     */
    @Override
    public void run() {
        try (Socket sock = this.sock) { // Try-with-resources ensures socket closes on exit
            int read;

            // Note: 'in' and 'out' are already initialized in the constructor.
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // J5: ensure cleanup even when shouldTerminate was set by sendError().
            // disconnect() is idempotent — if handleDisconnect() already called it, the
            // handler entry will be null and the method returns immediately.
            ConnectionsImpl<T> connections = ConnectionsImpl.getInstance();
            connections.disconnect(connectionId);
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    /**
     * Sends a message to the client.
     * <p>
     * This method is thread-safe. It synchronizes on the output stream to ensure
     * that multiple threads (e.g., the handler's read-loop vs. a broadcast from another thread)
     * do not corrupt the data stream.
     *
     * @param msg The message to send.
     */
    @Override
    public void send(T msg) {
        if (msg != null) {
            try {
                // Safety check: ensure 'out' was initialized successfully
                if (out != null) {
                    synchronized (out) {
                        byte[] encodedMsg = encdec.encode(msg);
                        out.write(encodedMsg);
                        out.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }   
        }
    }
}