package abilities;

import game.GameManager;
import game.callbacks.MessageCallback;
import units.player.Player;

public abstract class Ability {
    protected final MessageCallback out;
    protected final String name;

    public int cooldownMax = 0;
    protected int cooldownLeft = 0;

    protected Ability(String name, MessageCallback cb) {
        this.name = name;
        this.out = cb;
    }

    public String name() { return name; }
    public boolean ready() { return cooldownLeft == 0; }
    public int turnsLeft() { return cooldownLeft; }

    public void tick() { if (cooldownLeft > 0) cooldownLeft--; }

    public void reset() { cooldownLeft = 0; }

    public abstract void cast(Player p);

    protected boolean startCooldown() {
        if (!ready()) {
            GameManager.I().msg(name + " ability on cooldown (" + cooldownLeft + ")");
            return false;
        }
        cooldownLeft = cooldownMax;
        return true;
    }
}
