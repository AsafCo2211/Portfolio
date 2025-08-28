package game.callbacks;

public interface MessageCallback {
    void msg(String s);

    static MessageCallback ignore() {
        return message -> {}; // Do nothing
    }

}