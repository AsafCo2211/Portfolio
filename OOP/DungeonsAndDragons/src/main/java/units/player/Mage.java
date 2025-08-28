package units.player;

import abilities.Blizzard;
import game.GameManager;
import values.Position;
import values.Mana;

public final class Mage extends Player implements ManaUser {

    private final Mana mana;
    private int spellPower;

    public Mage(String name, char sym, Position pos, int hp, int att, int def, int manaPool, int manaCost, int spellPower, int hits, int range) {
        super(name, sym, pos, hp, att, def);
        this.mana = new Mana(manaPool);
        this.spellPower = spellPower;

        setAbility(new Blizzard(manaCost, hits, range, GameManager.I()::msg));
    }

    @Override
    public void accept(IPlayerVisitor v) {
        v.visit(this);
    }

    @Override public Mana getMana() { return mana; }
    public int  getSpellPower()     { return spellPower; }

    @Override
    public void onTick() {
        mana.regen(xp.getLevel());
    }

    @Override
    public void levelUp(int lvl) {
        super.levelUp(lvl);
        mana.increasePool(25 * lvl);
        mana.regen(mana.get() + mana.getPool() / 4);
        spellPower += 10 * lvl;
    }

    @Override
    public String description() {
        return super.description() + String.format(",  Mana: %d/%d,  SpellPower: %d", getMana().get(), getMana().getPool(), getSpellPower());
    }
}