package bgu.spl.net.impl.stomp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

/**
 * Encodes and decodes STOMP frames over a byte stream.
 *
 * <p>STOMP frames are terminated by the NUL byte ({@code 0}). This implementation
 * accumulates incoming bytes until the terminator is reached, then returns the
 * decoded UTF-8 string representing the complete frame.</p>
 *
 * <p>Note: This class is stateful and is expected to be used per-connection
 * (not shared across multiple connections concurrently).</p>
 */
public class StompEncoderDecoder implements MessageEncoderDecoder<String> {

    /** Initial buffer size (1KB). The buffer grows automatically if needed. */
    private static final int BUFFER_INITIAL_SIZE = 1 << 10;

    /** Accumulates incoming bytes until a full STOMP frame is received. */
    private byte[] buffer = new byte[BUFFER_INITIAL_SIZE];

    /** Number of valid bytes currently stored in {@link #buffer}. */
    private int length = 0;

    /**
     * Consumes the next byte from the stream and returns a full STOMP frame
     * string once the frame terminator (NUL) is encountered.
     *
     * @param nextByte the next byte from the connection input stream
     * @return the decoded STOMP frame as a String when complete; otherwise {@code null}
     */
    @Override
    public String decodeNextByte(byte nextByte) {
        // STOMP frames end with the NUL byte (0).
        if (nextByte == 0) {
            return popString();
        }

        // Frame not complete yet; keep buffering.
        pushByte(nextByte);
        return null;
    }

    /**
     * Encodes a STOMP frame for sending over the wire.
     *
     * <p>The STOMP protocol requires appending a terminating NUL byte to each frame.</p>
     *
     * @param message a STOMP frame string (without the terminating NUL)
     * @return UTF-8 bytes of the frame + terminating NUL
     */
    @Override
    public byte[] encode(String message) {
        // Always specify UTF-8 explicitly (avoid platform-dependent defaults).
        return (message + '\u0000').getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Appends a byte to the internal buffer, growing it when needed.
     *
     * @param nextByte the byte to append
     */
    private void pushByte(byte nextByte) {
        if (length == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 2);
        }
        buffer[length++] = nextByte;
    }

    /**
     * Converts the currently buffered bytes into a UTF-8 string and resets the buffer.
     *
     * @return the decoded string representing the accumulated frame
     */
    private String popString() {
        String result = new String(buffer, 0, length, StandardCharsets.UTF_8);
        length = 0; // ready for the next frame
        return result;
    }
}
