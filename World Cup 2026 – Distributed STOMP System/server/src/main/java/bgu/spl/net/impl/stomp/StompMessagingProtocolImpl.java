package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the STOMP Messaging Protocol.
 * <p>
 * This class handles the protocol logic for a <b>single</b> client connection.
 * It processes incoming STOMP frames, manages the client's state (connected/disconnected),
 * and interacts with the {@link Database} and {@link Connections} to route messages.
 */
public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;

    /**
     * Maps topic names to subscription IDs for this specific client.
     * Key: Destination (Topic), Value: Subscription ID.
     * J4: ConcurrentHashMap guards against any cross-thread visibility issues
     *     in the Reactor model where shouldTerminate() may be read from a different thread.
     */
    private final Map<String, String> subscribedTopics = new ConcurrentHashMap<>();

    // --- State and Concurrency Management ---

    /**
     * Indicates whether the client has successfully performed a generic login.
     * Clients cannot send messages or subscribe until this is true.
     */
    private boolean isConnected = false;

    /**
     * The username of the currently logged-in client.
     */
    private String currentUsername = null;

    /**
     * Global message ID counter.
     * Uses {@link AtomicInteger} to ensure unique IDs across all threads/connections
     * without needing complex synchronization locks.
     */
    private static AtomicInteger messageIdCounter = new AtomicInteger(0);


    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    /**
     * Processes a raw STOMP message received from the client.
     * <p>
     * 1. Parses the frame into headers and body.
     * 2. Validates connection state (Client must send CONNECT first).
     * 3. Routes to the specific handler based on the Command (CONNECT, SEND, etc.).
     *
     * @param msg The raw string message received from the network.
     */
    @Override
    public void process(String msg) {
        // 1. Parsing the frame (Header vs Body)
        // Split by the first double newline to separate headers from body
        String[] parts = msg.split("\n\n", 2);
        String[] headerLines = parts[0].split("\n");
        String command = headerLines[0].trim();

        Map<String, String> headers = new ConcurrentHashMap<>();
        for (int i = 1; i < headerLines.length; i++) {
            String[] pair = headerLines[i].split(":", 2);
            if (pair.length == 2) {
                headers.put(pair[0].trim(), pair[1].trim());
            }
        }
        String body = parts.length > 1 ? parts[1] : "";

        // 2. Critical State Check
        // If the client is not connected, they are ONLY allowed to send a CONNECT frame.
        if (!isConnected && !command.equals("CONNECT")) {
            sendError(headers, "Not Connected", "You must login first using CONNECT command");
            return;
        }

        // 3. Command Dispatch
        switch (command) {
            case "CONNECT":
                handleConnect(headers);
                break;
            case "SEND":
                handleSend(headers, body);
                break;
            case "SUBSCRIBE":
                handleSubscribe(headers);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(headers);
                break;
            case "DISCONNECT":
                handleDisconnect(headers);
                break;
            default:
                sendError(headers, "Malformed frame received", "Undefined command: " + command);
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    /**
     * Handles the CONNECT frame.
     * Validates protocol version, host, and credentials against the Database.
     */
    private void handleConnect(Map<String, String> headers) {
        String acceptVersion = headers.get("accept-version");
        String host = headers.get("host");
        String login = headers.get("login");
        String passcode = headers.get("passcode");

        // Protocol validation
        if (!"1.2".equals(acceptVersion) || !"stomp.cs.bgu.ac.il".equals(host)) {
            sendError(headers, "Connection failed", "The version must be 1.2 and host stomp.cs.bgu.ac.il");
            return;
        }
        if (login == null || passcode == null) {
            sendError(headers, "Malformed Frame", "Missing login or passcode");
            return;
        }

        // Database Authentication
        LoginStatus status = Database.getInstance().login(connectionId, login, passcode);

        if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER) {
            isConnected = true;
            currentUsername = login; 
            
            String response = "CONNECTED\nversion:1.2\n\n";
            connections.send(connectionId, response);

        } else if (status == LoginStatus.WRONG_PASSWORD) {
            sendError(headers, "Bad Credentials", "Wrong password");
        } else if (status == LoginStatus.ALREADY_LOGGED_IN) {
            sendError(headers, "User already logged in", "User is already active");
        } else if (status == LoginStatus.CLIENT_ALREADY_CONNECTED) {
            sendError(headers, "Client error", "Client is already connected");
        }
    }

    /**
     * Handles the SEND frame.
     * Publishes a message to a specific topic (destination).
     * Validates that the client is subscribed to the topic before sending.
     */
    private void handleSend(Map<String, String> headers, String body) {
        String destination = headers.get("destination");
        if (destination == null) {
            sendError(headers, "Did not provide a destination", body);
            return;
        }

        // Verify the user is subscribed to the topic they are writing to
        if (!subscribedTopics.containsKey(destination)) {
            sendError(headers, "Access Denied", "User is not subscribed to topic " + destination);
            return;
        }

        // Track file uploads in DB if header exists (Assignment requirement)
        if (headers.containsKey("filename")) {
            String filename = headers.get("filename");
            Database.getInstance().trackFileUpload(currentUsername, filename, destination);
        }

        // Generate a unique, thread-safe Message ID
        int messageId = messageIdCounter.incrementAndGet();

        // J3: Do NOT embed a subscription header here. ConnectionsImpl.send(channel, msg)
        //     inserts the per-subscriber subscription ID after the "MESSAGE\n" command line,
        //     so the body is never scanned for placeholder text.
        String messageFrame = "MESSAGE\n" +
                "message-id:" + messageId + "\n" +
                "destination:" + destination + "\n" +
                "\n" +
                body;

        // Send to all subscribers via the Connections interface
        connections.send(destination, messageFrame);

        // Send receipt if requested
        if (headers.containsKey("receipt")) {
            connections.send(connectionId, "RECEIPT\nreceipt-id:" + headers.get("receipt") + "\n\n");
        }
    }

    /**
     * Handles the SUBSCRIBE frame.
     * Registers the client to a topic.
     */
    private void handleSubscribe(Map<String, String> headers) {
        String destination = headers.get("destination");
        String id = headers.get("id");

        if (destination == null || destination.equals("/") || id == null) {
            sendError(headers, "Missing headers", "Missing destination or id in SUBSCRIBE frame");
            return;
        }

        // Local tracking
        subscribedTopics.put(destination, id);

        // Global tracking via Connections
        // Cast is necessary because the generic interface doesn't strictly support 'subscribe'
        if (connections instanceof ConnectionsImpl) {
            ((ConnectionsImpl<String>) connections).subscribe(destination, connectionId, id);
        }

        if (headers.containsKey("receipt")) {
            connections.send(connectionId, "RECEIPT\nreceipt-id:" + headers.get("receipt") + "\n\n");
        }
    }

    /**
     * Handles the UNSUBSCRIBE frame.
     * Removes the client from a topic using the Subscription ID.
     */
    private void handleUnsubscribe(Map<String, String> headers) {
        String id = headers.get("id");
        if (id == null) {
            sendError(headers, "Missing id", "Missing id in UNSUBSCRIBE frame");
            return;
        }
        
        // Find the topic associated with this Subscription ID
        String topicToRemove = null;
        for (Map.Entry<String, String> topic : subscribedTopics.entrySet()) {
            if (topic.getValue().equals(id)) {
                topicToRemove = topic.getKey();
                break;
            }
        }

        if (topicToRemove != null && !topicToRemove.equals("/")) {
            subscribedTopics.remove(topicToRemove);
            
            if (connections instanceof ConnectionsImpl) {
                ((ConnectionsImpl<String>) connections).unsubscribe(topicToRemove, connectionId);
            }
            
            if (headers.containsKey("receipt")) {
                connections.send(connectionId, "RECEIPT\nreceipt-id:" + headers.get("receipt") + "\n\n");
            }
        } else {
            sendError(headers, "No subscription found", "No subscription found for id: " + id);
        }
    }

    /**
     * Handles the DISCONNECT frame.
     * Performs a graceful shutdown of the connection.
     */
    private void handleDisconnect(Map<String, String> headers) {
        String receipt = headers.get("receipt");
        if (receipt != null) {
            connections.send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n");
        }
        
        // Update Database state
        if (isConnected) {
            Database.getInstance().logout(connectionId);
        }
        isConnected = false;
        
        // Signal termination
        shouldTerminate = true;
        connections.disconnect(connectionId);
    }

    /**
     * Helper method to send an ERROR frame.
     * <p>
     * J2/J8: Sets shouldTerminate = true so that the connection handler's loop
     * (BlockingConnectionHandler.run() or NonBlockingConnectionHandler.continueWrite())
     * will call connections.disconnect() after the ERROR frame is fully delivered.
     * This ensures subscriptions and the activeConnections map are cleaned up
     * without closing the socket before the ERROR frame is written.
     *
     * @param headers   Headers from the original message (to extract receipt-id if needed).
     * @param message   Short error summary.
     * @param extraInfo Detailed error description.
     */
    private void sendError(Map<String, String> headers, String message, String extraInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR\n");
        if (headers.containsKey("receipt")) {
            sb.append("receipt-id:").append(headers.get("receipt")).append("\n");
        }
        sb.append("message:").append(message).append("\n\n");
        sb.append("The message:\n-----\n");
        sb.append("Failed: ").append(message).append("\n");
        sb.append("Info: ").append(extraInfo).append("\n");
        sb.append("-----\n");

        // J2: Log out before marking for termination so that cleanup state is consistent.
        if (isConnected) {
            Database.getInstance().logout(connectionId);
            isConnected = false;
        }
        // Clear local subscription tracking (J8).
        subscribedTopics.clear();

        // Enqueue the ERROR frame.  The handler loop will call connections.disconnect()
        // after this frame is delivered, which removes this connection from all maps.
        connections.send(connectionId, sb.toString());
        shouldTerminate = true;
    }
}