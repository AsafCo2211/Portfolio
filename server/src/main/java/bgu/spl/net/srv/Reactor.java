package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * An implementation of the Reactor pattern for non-blocking server architecture.
 * <p>
 * The Reactor waits for I/O events (Accept, Read, Write) on multiple channels using a single thread (the selector thread).
 * When an event occurs:
 * 1. **Accept**: Creates a new connection handler.
 * 2. **Read**: Delegates the reading and processing task to a thread pool (ActorThreadPool).
 * 3. **Write**: Performs the writing immediately (as it is usually fast and non-blocking).
 *
 * @param <T> The type of message (e.g., String).
 */
public class Reactor<T> implements Server<T> {

    private final int port;
    private final Supplier<StompMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> readerFactory;
    private final ActorThreadPool pool;
    private Selector selector;

    private Thread selectorThread;
    
    // Tasks that need to be run by the selector thread (e.g., changing interestOps)
    private final ConcurrentLinkedQueue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();

    public Reactor(
            int numThreads,
            int port,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> readerFactory) {

        this.pool = new ActorThreadPool(numThreads);
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.readerFactory = readerFactory;
    }

    /**
     * Main Reactor loop.
     * <p>
     * 1. Opens the Selector and ServerSocket.
     * 2. Registers the ServerSocket for OP_ACCEPT.
     * 3. Enters the infinite loop:
     * a. Waits for events (selector.select()).
     * b. Executes pending tasks (like updating interest ops).
     * c. Iterates over keys with ready events and dispatches them (handleAccept / handleReadWrite).
     */
    @Override
    public void serve() {
        selectorThread = Thread.currentThread();
        try (Selector selector = Selector.open();
                ServerSocketChannel serverSock = ServerSocketChannel.open()) {

            this.selector = selector; // Save reference for closing later

            serverSock.bind(new InetSocketAddress(port));
            serverSock.configureBlocking(false); // Must be non-blocking for Selector
            serverSock.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started");

            while (!Thread.currentThread().isInterrupted()) {

                // Blocks until at least one channel is ready or wakeup() is called
                selector.select();
                
                // Run internal tasks (e.g., updates from worker threads)
                runSelectionThreadTasks();

                // Iterate over ready channels
                for (SelectionKey key : selector.selectedKeys()) {

                    if (!key.isValid()) {
                        continue;
                    } else if (key.isAcceptable()) {
                        handleAccept(serverSock, selector);
                    } else {
                        handleReadWrite(key);
                    }
                }

                selector.selectedKeys().clear(); // Must manually clear handled keys
            }

        } catch (ClosedSelectorException ex) {
            // Normal shutdown behavior
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("server closed!!!");
        pool.shutdown();
    }

    /**
     * Updates the operations a channel is interested in (Read/Write).
     * <p>
     * <b>Thread Safety:</b> This method can be called from any thread (e.g., a worker thread wanting to write).
     * However, the Selector logic is strictly single-threaded. Therefore:
     * - If called from the selector thread, update immediately.
     * - If called from another thread, queue a task and wake up the selector.
     *
     * @param chan The channel to update.
     * @param ops  The new operation set (e.g., OP_READ | OP_WRITE).
     */
    /*package*/ void updateInterestedOps(SocketChannel chan, int ops) {
        final SelectionKey key = chan.keyFor(selector);
        if (Thread.currentThread() == selectorThread) {
            key.interestOps(ops);
        } else {
            selectorTasks.add(() -> {
                if (key.isValid()) {
                    key.interestOps(ops);
                }
            });
            selector.wakeup();
        }
    }

    /**
     * Handles a new client connection.
     * Accepts the socket, sets it to non-blocking, creates a handler, and registers it for READ events.
     */
    private void handleAccept(ServerSocketChannel serverChan, Selector selector) throws IOException {
        SocketChannel clientChan = serverChan.accept();
        clientChan.configureBlocking(false);
        final NonBlockingConnectionHandler<T> handler = new NonBlockingConnectionHandler<>(
                readerFactory.get(),
                protocolFactory.get(),
                clientChan,
                this);
        // Note: We attach the handler to the key to retrieve it later
        clientChan.register(selector, SelectionKey.OP_READ, handler);
    }

    /**
     * Handles READ and WRITE events for an existing connection.
     */
    private void handleReadWrite(SelectionKey key) {
        @SuppressWarnings("unchecked")
        NonBlockingConnectionHandler<T> handler = (NonBlockingConnectionHandler<T>) key.attachment();

        if (key.isReadable()) {
            // Read data. If successful, we get a Runnable task to process the data.
            Runnable task = handler.continueRead();
            if (task != null) {
                // Submit the processing task to the thread pool (Reactor pattern)
                pool.submit(handler, task);
            }
        }

        if (key.isValid() && key.isWritable()) {
            // Write pending data to the socket
            handler.continueWrite();
        }
    }

    /**
     * Executes internal tasks queued by other threads.
     */
    private void runSelectionThreadTasks() {
        while (!selectorTasks.isEmpty()) {
            selectorTasks.remove().run();
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }
}