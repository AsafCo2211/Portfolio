package abilities;

import game.GameManager;
import game.callbacks.MessageCallback;
import units.enemy.Enemy;
import units.player.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Blizzard extends Ability implements IPlayerVisitor {

    private final int manaCost;
    private final int hits;
    private final int range;

    public Blizzard(int manaCost, int hits, int range, MessageCallback cb) {
        super("Blizzard", cb);
        this.manaCost = manaCost;
        this.hits = hits;
        this.range = range;
    }

    @Override
    public void cast(Player player) {
        player.accept(this);
    }

    @Override
    public void visit(Mage m) {
        if (!m.getMana().spend(manaCost)) {
            GameManager.I().msg("Not enough mana!");
            return;
        }

        int hitCnt = 0;
        while (hitCnt < hits) {
            List<Enemy> pool = GameManager.I().getEnemiesWithin(m.getPosition(), range);

            if (pool.isEmpty()) break;

            Enemy e = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

            int dmg = m.getSpellPower();
            int def = e.rollDefense();
            int taken = Math.max(0, dmg - def);
            GameManager.I().msg(m.getName() + " have " + dmg + " spell power; " + e.getName() + " rolled " + def + " defence; " + m.getName() + " dealt " + taken);
            e.takeTrueDamage(taken);

            hitCnt++;
        }
    }

    @Override
    public void visit(Warrior w) {
        GameManager.I().msg("A Warrior cannot cast Blizzard!");
    }

    @Override
    public void visit(Rogue r) {
        GameManager.I().msg("A Rogue cannot cast Blizzard!");
    }

    @Override
    public void visit(Hunter h) {
        GameManager.I().msg("A Hunter cannot cast Blizzard!");
    }
}