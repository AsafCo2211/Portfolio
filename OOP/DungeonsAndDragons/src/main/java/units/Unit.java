package units;

import values.Health;
import tiles.*;
import units.enemy.Enemy;
import units.player.Player;
import game.GameManager;
import game.callbacks.MessageCallback;
import values.Position;

public abstract class Unit extends Tile implements TileVisitor {

    protected final String name;
    protected final Health health;
    protected int attack;
    protected int defense;

    protected MessageCallback out = s -> {};

    protected Unit(String name, char symbol, Position pos, int hp, int att, int def) {
        super(pos, symbol);
        this.name = name;
        this.health = new Health(hp);
        this.attack = att;
        this.defense = def;
    }

    public abstract void accept(UnitVisitor v);

    public String getName() { return name; }
    public Health getHealth() { return health; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public void move(Position dst) { pos = dst; }
    public int rollAttack() { return GameManager.I().roll(0, attack); }
    public int rollDefense() { return GameManager.I().roll(0, defense); }

    public void takeTrueDamage(int dmg) {
        health.damage(dmg);
        GameManager.I().msg(name + " takes " + dmg + " damage (" + health.get() + "/" + health.getMax() + ")\n");
        if (health.isDead()) {
            die();
        }
    }

    public void die() {
        GameManager.I().handleUnitDeath(this);
    }

    @Override
    public void visit(Empty empty) {
        GameManager gm = GameManager.I();
        Position from = pos;
        Position to = empty.getPosition();
        gm.setTile(from, new Empty(from));
        gm.setTile(to, this);
        move(to);
    }

    @Override
    public void visit(Wall wall) { }

    @Override
    public abstract void visit(Player p);

    @Override
    public abstract void visit(Enemy e);
}