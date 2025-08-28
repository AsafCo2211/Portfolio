package values;

public final class Mana {
    private int current, pool;

    public Mana(int pool) {
        if (pool <= 0) throw new IllegalArgumentException("mana pool must be positive");
        this.pool = pool;
        this.current = pool;
    }

    public int  get() { return current; }
    public int  getPool() { return pool;    }

    public boolean spend(int amount) {
        if (current < amount) return false;
        current -= amount;
        return true;
    }

    public void regen(int amt) { current = Math.min(pool, current + amt); }

    public void refillFull() { current = pool; }

    public void increasePool(int delta) {
        if (delta <= 0) return;
        pool += delta;
        current += delta;
    }
}