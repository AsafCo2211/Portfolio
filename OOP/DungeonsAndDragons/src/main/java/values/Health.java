package values;

public final class Health {
    private int current, max;

    public Health(int max) {
        if (max <= 0) throw new IllegalArgumentException("max HP must be positive");
        this.max = max;
        this.current = max;
    }

    public int get(){ return current; }
    public int getMax() { return max; }
    public boolean isDead() { return current <= 0; }

    public void damage(int amount) { current = Math.max(0, current - amount); }
    public void heal(int amount)   { current = Math.min(max, current + amount); }

    public void healFull() { current = max; }

    public void increasePool(int delta) {
        if (delta <= 0) return;
        max += delta;
        current += delta;
    }
}
