package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

/**
 * The main entry point for the STOMP Server.
 * <p>
 * This class is responsible for:
 * 1. Parsing command-line arguments (port and server mode).
 * 2. registering a shutdown hook to print database statistics upon exit.
 * 3. Initializing and running the server using the specified concurrency model
 * (Thread-Per-Client or Reactor).
 */
public class StompServer {

    /**
     * Main method to start the server.
     *
     * @param args Command line arguments:
     * 1. port - The port number to listen on (e.g., 7777).
     * 2. server_type - The concurrency strategy: "tpc" (Thread-Per-Client) or "reactor".
     */
    public static void main(String[] args) {

        // Register a Shutdown Hook
        // This thread runs when the server is terminated (e.g., via Ctrl+C).
        // It prints the final state/report from the Database (SQL logs).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server... Printing SQL Report:");
            bgu.spl.net.impl.data.Database.getInstance().printReport();
        }));
        
        // Validate arguments
        if (args.length < 2) {
            System.out.println("Usage: StompServer <port> <server_type(tpc/reactor)>");
            System.exit(1);
        }

        // Parse Port
        int port = 7777;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " + args[0]);
            System.exit(1);
        }

        String serverType = args[1];

        // Initialize Server based on the requested strategy
        if (serverType.equals("tpc")) {
            // Thread-Per-Client Strategy:
            // Creates a dedicated thread for every active connection.
            Server.threadPerClient(
                port, 
                () -> new StompMessagingProtocolImpl(), // Protocol Factory
                () -> new StompEncoderDecoder()         // Encoder/Decoder Factory
            ).serve();

        } else if (serverType.equals("reactor")) {
            // Reactor Strategy:
            // Uses non-blocking I/O with a fixed pool of threads (based on CPU cores).
            Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port, 
                () -> new StompMessagingProtocolImpl(), // Protocol Factory
                () -> new StompEncoderDecoder()         // Encoder/Decoder Factory
            ).serve();

        } else {
            // Invalid server type
            System.out.println("Unknown server type: " + serverType);
            System.out.println("Usage: StompServer <port> <server_type(tpc/reactor)>");
            System.exit(1);
        }
    }
}