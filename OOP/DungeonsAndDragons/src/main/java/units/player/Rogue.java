package units.player;

import abilities.FanOfKnives;
import game.GameManager;
import values.Position;
import values.Mana;   // reused as Energy

public final class Rogue extends Player {

    private final Mana energy;
    private final int MAX_ENERGY = 100;

    public Rogue(String n, char sym, Position pos, int hp, int atk, int def, int cost) {
        super(n, sym, pos, hp, atk, def);
        this.energy = new Mana(MAX_ENERGY);

        setAbility(new FanOfKnives(cost, GameManager.I()::msg));
    }

    @Override
    public void accept(IPlayerVisitor v) {
        v.visit(this);
    }

    public Mana getEnergy() { return energy; }

    @Override
    public void onTick() {
        energy.regen(10);
    }

    @Override
    public void levelUp(int lvl) {
        super.levelUp(lvl);
        energy.refillFull();
        attack += 3 * lvl;
    }

    @Override
    public String description() {
        return super.description() + String.format(",  Energy: %d/%d", getEnergy().get(), getEnergy().getPool());
    }
}