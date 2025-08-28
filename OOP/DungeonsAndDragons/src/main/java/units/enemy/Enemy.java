package units.enemy;

import game.GameManager;
import values.Position;
import tiles.TileVisitor;
import units.Unit;
import units.UnitVisitor;
import units.player.Player;

public abstract class Enemy extends Unit {

    protected final int visionRange;
    protected final int expValue;

    public Enemy(String n, char sym, Position pos, int hp, int att, int def, int vision, int expValue) {
        super(n, sym, pos, hp, att, def);
        this.visionRange = vision;
        this.expValue = expValue;
    }

    @Override
    public void accept(TileVisitor visitor) { visitor.visit(this); }

    @Override
    public void accept(UnitVisitor v) { v.visit(this); }

    public int getExpValue() { return expValue; }
    public int distanceTo(Position p) { return pos.distance(p); }

    @Override
    public void visit(units.player.Player player) {
        hitPlayer(player);
    }

    @Override
    public void visit(Enemy e) { }

    protected void hitPlayer(Player p) {
        GameManager.I().msg(name + " engaged into a battle with " + p.getName());

        int attRoll = rollAttack();
        int defRoll = p.rollDefense();
        int dmg = Math.max(0, attRoll - defRoll);

        GameManager.I().msg(name + " rolled " + attRoll + " attack points");
        GameManager.I().msg(p.getName() + " rolled " + defRoll + " defence points");
        GameManager.I().msg(name + " dealt " + dmg + " damage to " + p.getName());
        GameManager.I().msg("");

        p.takeTrueDamage(dmg);
    }

    public Position calcNextStepToward(Position playerPos) {
        GameManager gm = GameManager.I();

        int dist = distanceTo(playerPos);
        Position next;

        if (dist <= visionRange) {
            int dx = Integer.compare(playerPos.getX(), pos.getX());
            int dy = Integer.compare(playerPos.getY(), pos.getY());

            next = pos.translate(Math.abs(dx) > Math.abs(dy) ? dx : 0, Math.abs(dy) >= Math.abs(dx) ? dy : 0);
        }
        else {
            int dir = gm.roll(0, 4);
            next = switch (dir) {
                case 0 -> pos.translate( 1, 0);
                case 1 -> pos.translate(-1, 0);
                case 2 -> pos.translate( 0, 1);
                case 3 -> pos.translate( 0,-1);
                default -> pos;
            };
        }
        return next;
    }

    public void act() { }
}