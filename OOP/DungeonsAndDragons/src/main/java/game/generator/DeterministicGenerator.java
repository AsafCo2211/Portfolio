package game.generator;
public final class DeterministicGenerator implements ValueGenerator {
    private final int[] seq;
    private int idx = 0;

    public DeterministicGenerator(int... seq) {
        this.seq = seq;
    }

    @Override
    public int roll(int min, int max) {
        if (seq.length == 0)
            throw new IllegalStateException("DeterministicGenerator initialized with empty sequence.");
        return seq[idx++ % seq.length];
    }
}