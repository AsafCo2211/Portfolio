package game.generator;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomGenerator implements ValueGenerator {
    @Override public int roll(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max+1);
    }
}


