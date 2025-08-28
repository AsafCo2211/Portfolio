package abilities;

import game.GameManager;
import units.enemy.Enemy;
import units.player.*;
import game.callbacks.MessageCallback;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AvengersShield extends Ability implements IPlayerVisitor {

    public AvengersShield(int cooldown, MessageCallback cb) {
        super("Avenger's Shield", cb);
        this.cooldownMax = cooldown;
    }

    @Override
    public void cast(Player player) {
        player.accept(this);
    }

    @Override
    public void visit(Warrior w) {
        if (!startCooldown()) {
            return;
        }

        List<Enemy> pool = GameManager.I().getEnemiesWithin(w.getPosition(), 3);
        if (pool.isEmpty()) {
            GameManager.I().msg("No target in range!");
            return;
        }

        Enemy target = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        int dmg = w.getHealth().getMax() / 10;
        target.takeTrueDamage(dmg);
        w.getHealth().heal(10 * w.getDefense());

        GameManager.I().msg(w.getName() + " hurled Avenger’s Shield at " + target.getName() + " for " + dmg);
    }

    @Override
    public void visit(Mage m) {
        GameManager.I().msg("A Mage cannot use Avenger's Shield!");
    }

    @Override
    public void visit(Rogue r) {
        GameManager.I().msg("A Rogue cannot use Avenger's Shield!");
    }

    @Override
    public void visit(Hunter h) {
        GameManager.I().msg("A Hunter cannot use Avenger's Shield!");
    }
}