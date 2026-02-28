package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the Connections interface.
 * <p>
 * This class holds the state of all active connections and manages the broadcasting of messages.
 * It is implemented as a thread-safe Singleton to ensure all threads (TPC or Reactor) share the same state.
 *
 * @param <T> The type of message (e.g., String).
 */
public class ConnectionsImpl<T> implements Connections<T> {

    // --- Singleton Pattern (Thread Safe) ---
    // J6: Use the concrete type parameter so callers don't need an unchecked cast.
    private static class ConnectionsHolder {
        private static final ConnectionsImpl<String> instance = new ConnectionsImpl<>();
    }

    @SuppressWarnings("unchecked")
    public static <T> ConnectionsImpl<T> getInstance() {
        return (ConnectionsImpl<T>) ConnectionsHolder.instance;
    }

    // --- Data Structures ---

    /**
     * Map of all active connections.
     * Key: Unique Connection ID (int)
     * Value: The ConnectionHandler responsible for that client.
     */
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> activeConnections;

    /**
     * Map of channel subscriptions.
     * Key: Channel Name (String)
     * Value: A Map where Key is Connection ID and Value is the Subscription ID (String).
     * <p>
     * Logic: We need to know not just *who* is subscribed to a channel, but also
     * what specific Subscription ID they used, so we can attach it to the message header.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> channelSubscribers;

    /**
     * Atomic counter to generate unique connection IDs.
     */
    private final AtomicInteger connectionIdCounter = new AtomicInteger(0);
    
    /**
     * Private Constructor (Singleton).
     */
    private ConnectionsImpl() {
        this.activeConnections = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new connection handler and assigns it a unique ID.
     *
     * @param handler The new connection handler.
     * @return The assigned unique connection ID.
     */
    public int addActiveHandler(ConnectionHandler<T> handler) {
        int id = connectionIdCounter.incrementAndGet();
        activeConnections.put(id, handler);
        return id;
    }

    /**
     * Sends a message to a specific client.
     *
     * @param connectionId The target client's unique ID.
     * @param msg          The message to send.
     * @return true if the message was sent, false if the connection ID was not found.
     */
    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeConnections.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false; 
    }

    /**
     * Sends a message to all subscribers of a specific channel.
     * <p>
     * <b>Crucial Logic:</b> The STOMP protocol requires that when a client receives a MESSAGE
     * frame it must contain the {@code subscription} header matching the ID the client used
     * when it originally subscribed.
     * <p>
     * J3: The previous approach searched for a {@code "subscription:null"} placeholder and
     * replaced it — a fragile strategy that would corrupt messages whose <em>body</em>
     * happens to contain that literal string.  We now insert the subscription header
     * directly after the {@code "MESSAGE\n"} command line, so the body is never touched.
     *
     * @param channel The channel/topic name.
     * @param msg     The base MESSAGE frame (starts with "MESSAGE\n", no subscription header).
     */
    @Override
    public void send(String channel, T msg) {
        ConcurrentHashMap<Integer, String> subscribers = channelSubscribers.get(channel);

        if (subscribers != null) {
            String originalMsg = (String) msg; // T is always String for this protocol
            // The frame starts with "MESSAGE\n"; insert subscription header right after that.
            final String prefix = "MESSAGE\n";
            String rest = originalMsg.startsWith(prefix)
                    ? originalMsg.substring(prefix.length())
                    : originalMsg;

            for (Map.Entry<Integer, String> entry : subscribers.entrySet()) {
                Integer connId = entry.getKey();
                String subId   = entry.getValue();
                // Build a personalised frame: MESSAGE\nsubscription:<id>\n<rest of headers + body>
                String personalizedMsg = prefix + "subscription:" + subId + "\n" + rest;
                send(connId, (T) personalizedMsg);
            }
        }
    }

    /**
     * Disconnects a client.
     * Removes them from the active connections map and all subscription lists.
     * Closes the socket handler.
     *
     * @param connectionId The ID of the client to disconnect.
     */
    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler<T> handler = activeConnections.get(connectionId);
        if (handler != null) {
            // Remove from active connections
            activeConnections.remove(connectionId);
            
            // Remove from all topics they might be subscribed to
            for (Map<Integer, String> subs : channelSubscribers.values()) {
                subs.remove(connectionId);
            }
            
            // Close the actual socket handler
            try {
                handler.close(); 
            } catch (Exception e) {
               // Ignore errors during close (idempotent)
            }
        }
    }
    
    /**
     * Legacy/Helper method to manually add a connection (mostly for testing).
     */
    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        activeConnections.put(connectionId, handler);
    }

    /**
     * Subscribes a client to a channel.
     *
     * @param channel        The topic name.
     * @param connectionId   The client's connection ID.
     * @param subscriptionId The unique subscription ID provided by the client frame.
     */
    public void subscribe(String channel, int connectionId, String subscriptionId) {
        channelSubscribers.computeIfAbsent(channel, k -> new ConcurrentHashMap<>())
                          .put(connectionId, subscriptionId);
    }
    
    /**
     * Unsubscribes a client from a channel.
     *
     * @param channel      The topic name.
     * @param connectionId The client's connection ID.
     */
    public void unsubscribe(String channel, int connectionId) {
        if (channelSubscribers.containsKey(channel)) {
            channelSubscribers.get(channel).remove(connectionId);
        }
    }
}