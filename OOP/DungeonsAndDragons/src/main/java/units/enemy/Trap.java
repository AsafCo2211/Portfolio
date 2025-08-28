package units.enemy;

import values.Position;
import units.player.Player;
import game.GameManager;

public class Trap extends Enemy {
    private final int visibleFor;
    private final int invisibleFor;
    private int timer;
    private boolean visible;

    public Trap(String name, char symbol, Position pos, int hp, int attack, int defense, int vis, int invis, int xp) {
        super(name, symbol, pos, hp, attack, defense, 1, xp);
        this.visibleFor = vis;
        this.invisibleFor = invis;
        this.visible = true;
        this.timer = 0;
    }

    public boolean isVisible() { return visible; }

    @Override public void act() {
        if (--timer == 0) {
            visible = !visible;
            timer = visible ? visibleFor : invisibleFor;
        }
        if (!visible) return;

        Player p = GameManager.I().getPlayer();
        if (p == null) return;
        if (distanceTo(p.getPosition()) < 2) {
            int dmg = rollAttack();
            p.takeTrueDamage(dmg);
            GameManager.I().msg(name + " ambushes " + p.getName() + " for " + dmg);
        }

    }

    public char getTileChar() {
        return visible ? tileChar : '.';
    }

    @Override
    public String toString() { return String.valueOf(getTileChar()); }
}