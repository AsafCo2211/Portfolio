package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles a single client connection using Non-Blocking I/O (NIO).
 * <p>
 * This handler is designed for the Reactor pattern. It does not block on I/O operations.
 * Instead, it manages a write-queue and interacts with the {@link Reactor} to register
 * interest in Reading or Writing events.
 *
 * @param <T> The type of message (e.g., String).
 */
public class NonBlockingConnectionHandler<T> implements ConnectionHandler<T> {

    // --- Buffer Management ---
    // A pool of Direct ByteBuffers is used to reduce allocation overhead and GC pressure.
    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; // 8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final StompMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;

    // A queue of buffers waiting to be written to the socket
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();

    private final SocketChannel chan;
    private final Reactor reactor;
    // J5: stored so continueWrite() can call connections.disconnect() after the
    //     ERROR frame is fully delivered, rather than closing the channel immediately.
    private int connectionId = -1;

    /**
     * Constructor.
     * Registers the handler with the global Connections service and initializes the protocol.
     *
     * @param reader   The encoder/decoder for this client.
     * @param protocol The protocol instance.
     * @param chan     The non-blocking socket channel.
     * @param reactor  Reference to the Reactor engine.
     */
    public NonBlockingConnectionHandler(
            MessageEncoderDecoder<T> reader,
            StompMessagingProtocol<T> protocol,
            SocketChannel chan,
            Reactor reactor) {
        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;

        // Register to the global Connections singleton
        ConnectionsImpl<T> connections = ConnectionsImpl.getInstance();
        this.connectionId = connections.addActiveHandler(this);
        protocol.start(this.connectionId, connections);
    }

    /**
     * Reads data from the socket channel into a buffer.
     * <p>
     * If data is read successfully, it returns a Runnable task. This task will
     * be executed by the Reactor (or a thread pool) to decode the bytes and
     * process the resulting messages via the protocol.
     *
     * @return A Runnable containing the processing logic, or null if connection is closed/failed.
     */
    public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();

        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            buf.flip();
            return () -> {
                try {
                    while (buf.hasRemaining()) {
                        T nextMessage = encdec.decodeNextByte(buf.get());
                        if (nextMessage != null) {
                            protocol.process(nextMessage);
                        }
                    }
                } finally {
                    releaseBuffer(buf);
                }
            };
        } else {
            releaseBuffer(buf);
            // J5: use disconnect() so the connection is removed from all maps, not just closed.
            ConnectionsImpl<T> connections = ConnectionsImpl.getInstance();
            connections.disconnect(connectionId);
            return null;
        }
    }

    public void close() {
        try {
            chan.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !chan.isOpen();
    }

    /**
     * Writes pending data from the write-queue to the socket channel.
     * <p>
     * This method is called by the Reactor when the channel is ready for writing.
     * It attempts to drain the queue. If the queue becomes empty, it un-registers
     * the write interest (OP_WRITE).
     */
    public void continueWrite() {
        while (!writeQueue.isEmpty()) {
            try {
                ByteBuffer top = writeQueue.peek();
                chan.write(top);
                if (top.hasRemaining()) {
                    return; // Socket buffer is full, return and wait for next OP_WRITE trigger
                } else {
                    writeQueue.remove();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
                writeQueue.clear();
            }
        }

        // If queue is empty, we stop listening for write events to save CPU cycles.
        if (writeQueue.isEmpty()) {
            if (protocol.shouldTerminate()) {
                // J5: call disconnect() so that the connection is removed from the
                //     activeConnections map and all channel subscriptions — not just
                //     the raw socket closed.  disconnect() calls close() internally
                //     (idempotent).
                ConnectionsImpl<T> connections = ConnectionsImpl.getInstance();
                connections.disconnect(connectionId);
            } else {
                reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
                // Race guard: a send() may have added data while we switched ops.
                if (!writeQueue.isEmpty()) {
                    reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
            }
        }
    }

    // --- Buffer Pool Helpers ---

    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }

        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

    /**
     * Sends a message to the client.
     * <p>
     * Unlike the blocking handler, this method does not write directly to the socket.
     * Instead, it:
     * 1. Encodes the message.
     * 2. Queues the bytes.
     * 3. Signals the Reactor to monitor the channel for "Ready to Write" events.
     *
     * @param msg The message to send.
     */
    @Override
    public void send(T msg) {
        if (msg != null) {
            try {
                // 1. Encode the message to bytes
                byte[] encodedMsg = encdec.encode(msg);
                
                // 2. Add to the write queue
                writeQueue.add(ByteBuffer.wrap(encodedMsg));
                
                // 3. Update the Reactor that we are interested in writing (OP_WRITE)
                reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}