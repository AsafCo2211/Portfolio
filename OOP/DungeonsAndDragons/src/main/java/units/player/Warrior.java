package units.player;

import abilities.AvengersShield;
import game.GameManager;
import values.Position;

public final class Warrior extends Player {

    public Warrior(String name, char sym, Position pos, int hp, int att, int def, int cd) {
        super(name, sym, pos, hp, att, def);
        setAbility(new AvengersShield(cd, GameManager.I()::msg));
    }

    @Override
    public void accept(IPlayerVisitor v) {
        v.visit(this);
    }

    @Override
    public void onTick() {
        if (ability.turnsLeft() > 0) ability.tick();
    }

    @Override
    public void levelUp(int lvl) {
        super.levelUp(lvl);

        health.increasePool(5 * lvl);
        attack  += 2 * lvl;
        defense += 1 * lvl;

        resetAbilityCooldown();
    }

    @Override
    public String description() {
        return super.description() + String.format(",  Cooldown: %d/%d", getAbility().turnsLeft(), getAbility().cooldownMax);
    }
}