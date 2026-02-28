package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

/**
 * Abstract skeleton for a Server.
 * <p>
 * This class implements the main accept loop for a TCP server.
 * It handles the low-level socket listening and acceptance, but delegates
 * the specific execution strategy (concurrency model) to the subclasses via the {@link #execute(BlockingConnectionHandler)} method.
 *
 * @param <T> The type of message the server handles (e.g., String for STOMP).
 */
public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<StompMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;

    /**
     * Constructor.
     *
     * @param port            The port number to listen on.
     * @param protocolFactory A factory to create a new protocol instance for each client.
     * @param encdecFactory   A factory to create a new encoder/decoder instance for each client.
     */
    public BaseServer(
            int port,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.sock = null;
    }

    /**
     * The main server loop.
     * <p>
     * 1. Binds to the specified port.
     * 2. Loops infinitely to accept new client connections.
     * 3. For each connection, creates the necessary Protocol and Encoder/Decoder.
     * 4. Wraps them in a ConnectionHandler and passes it to {@link #execute(BlockingConnectionHandler)}.
     */
    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");

            this.sock = serverSock; // Reference saved to allow closing later

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                
                // Initialize the protocol and encoder/decoder for this specific client
                StompMessagingProtocol<T> protocol = protocolFactory.get();
                MessageEncoderDecoder<T> encdec = encdecFactory.get();

                // Create the Handler.
                // Note: The Handler's constructor automatically registers itself with the 
                // global Connections interface and initializes the protocol. 
                // No further setup is required here.
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdec,
                        protocol);

                // Execute the handler according to the specific concurrency strategy
                // (e.g., spawn a new thread in Thread-Per-Client mode)
                execute(handler);
            }
        } catch (IOException ex) {
            // Server socket closed, loop ends
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    /**
     * Executes the connection handler.
     * <p>
     * Subclasses must implement this to define the concurrency model
     * (e.g., Thread-Per-Client, Thread Pool, etc.).
     *
     * @param handler The connection handler to execute.
     */
    protected abstract void execute(BlockingConnectionHandler<T> handler);
}