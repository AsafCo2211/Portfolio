package values;

public class Experience {
    private int level = 1;
    private int xp = 0;

    public int  getLevel() { return level; }
    public int  getXP() { return xp; }

    public int nextThreshold() { return 50 * level; }

    public boolean gain(int amount) {
        xp += amount;
        boolean levelled = false;
        while (xp >= nextThreshold()) {
            xp -= nextThreshold();
            level++;
            levelled = true;
        }
        return levelled;
    }
}