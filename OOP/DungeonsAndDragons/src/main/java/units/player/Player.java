package units.player;

import abilities.Ability;
import game.GameManager;
import tiles.*;
import units.HeroicUnit;
import units.Unit;
import units.UnitVisitor;
import values.Experience;
import values.Position;


public abstract class Player extends Unit implements HeroicUnit {
    protected Ability ability;
    protected final Experience xp = new Experience();

    protected Player(String name, char symbol, Position pos, int hp, int atk, int def) {
        super(name, symbol, pos, hp, atk, def);
    }

    @Override
    public void accept(TileVisitor v) {
        v.visit(this);
    }

    @Override
    public void accept(UnitVisitor v) {
        v.visit(this);
    }

    public abstract void accept(IPlayerVisitor v);

    public void setAbility(Ability a) { ability = a; }
    public void castAbility() { if (ability != null) ability.cast(this); }
    public void resetAbilityCooldown() { if (ability != null) ability.reset(); }
    public Experience getXP() { return xp; }
    public Ability getAbility() { return ability; }

    public void gainXP(int amount) {
        int before = xp.getLevel();
        xp.gain(amount);
        int after = xp.getLevel();

        for (int lvl = before + 1; lvl <= after; lvl++) {
            levelUp(lvl);
            GameManager.I().msg("=== " + name + " reached level " + lvl + " ===\n");
        }
    }

    public void levelUp(int lvl) {
        health.increasePool(10 * lvl);
        health.healFull();
        attack += 4 * lvl;
        defense += lvl;
    }

    @Override
    public void visit(Player p) { }

    @Override
    public void visit(units.enemy.Enemy e) {
        GameManager.I().msg(this.getName() + " engaged into a battle with " + e.getName());

        int attRoll = rollAttack();
        int defRoll = e.rollDefense();
        int dmg = Math.max(0, attRoll - defRoll);

        GameManager.I().msg(this.getName() + " rolled " + attRoll + " attack points");
        GameManager.I().msg(e.getName() + " rolled " + defRoll + " defence points");
        GameManager.I().msg(this.getName() + " dealt " + dmg + " damage to " + e.getName() + "\n");

        e.takeTrueDamage(dmg);

        if (e.getHealth().isDead()) {
            Position formerEnemyPos = e.getPosition();
            this.gainXP(e.getExpValue());
            Tile newTile = GameManager.I().tileAt(formerEnemyPos);
            newTile.accept(this);
        }
    }

    @Override
    public String toString() {
        return health.isDead() ? "X" : String.valueOf(tileChar);
    }

    public String description() {
        StringBuilder sb = new StringBuilder()
                .append("name: ").append(name)
                .append(",  Health: ").append(health.get()).append('/').append(health.getMax())
                .append(",  Attack: ").append(attack)
                .append(",  Defence: ").append(defense)
                .append(",  Experience: ").append(xp.getXP()).append('/').append(xp.nextThreshold())
                .append(",  Level: ").append(xp.getLevel());

        return sb.toString();
    }

    public void onTick() { }
}