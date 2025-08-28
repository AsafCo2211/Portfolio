package abilities;

import game.GameManager;
import game.callbacks.MessageCallback;
import units.player.*;

public final class FanOfKnives extends Ability implements IPlayerVisitor {

    private final int cost;

    public FanOfKnives(int cost, MessageCallback cb) {
        super("Fan of Knives", cb);
        this.cost = cost;
    }

    @Override
    public void cast(Player player) {
        player.accept(this);
    }

    @Override
    public void visit(Rogue r) {
        if (!r.getEnergy().spend(cost)) {
            GameManager.I().msg("Not enough energy!");
            return;
        }

        GameManager.I().getEnemiesWithin(r.getPosition(), 2).forEach(e -> e.takeTrueDamage(r.getAttack()));

        GameManager.I().msg(r.getName() + " unleashes Fan of Knives!");
    }

    @Override
    public void visit(Warrior w) {
        GameManager.I().msg("A Warrior cannot use Fan of Knives!");
    }

    @Override
    public void visit(Mage m) {
        GameManager.I().msg("A Mage cannot use Fan of Knives!");
    }

    @Override
    public void visit(Hunter h) {
        GameManager.I().msg("A Hunter cannot use Fan of Knives!");
    }
}