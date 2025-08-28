package cli;

import units.player.*;
import values.Health;
import values.Experience;

class MenuStatVisitor implements IPlayerVisitor {
    private String tailA = "";
    private String tailB = "";

    public String getTailA() { return tailA; }
    public String getTailB() { return tailB; }

    @Override
    public void visit(Warrior w) {
        tailA = "Cooldown: " + w.getAbility().turnsLeft() + "/" + w.getAbility().cooldownMax;
    }
    @Override
    public void visit(Mage m) {
        tailA = "Mana: " + m.getMana().get() + "/" + m.getMana().getPool();
        tailB = "Spell Power: " + m.getSpellPower();
    }
    @Override
    public void visit(Rogue r) {
        tailA = "Energy: " + r.getEnergy().get() + "/" + r.getEnergy().getPool();
    }
    @Override
    public void visit(Hunter h) {
        tailA = "Arrows: " + h.getArrows();
        tailB = "Range: "  + h.getRange();
    }
}


public final class MenuFormatter {

    private static final int W_IDX   = 2;
    private static final int W_NAME  = 17;
    private static final int W_STAT  = 17;
    private static final int W_LEVEL = 8;
    private static final int W_XP    = 22;
    private static final int W_TAIL_A = 24;

    private MenuFormatter() {}

    public static String line(int number, Player p) {
        StringBuilder sb = new StringBuilder();

        sb.append(pad(number + ".", W_IDX)).append(' ').append(pad(p.getName(), W_NAME));

        Health hp = p.getHealth();
        sb.append(pad("Health: " + hp.get() + "/" + hp.getMax(), W_STAT))
                .append(pad("Attack: "  + p.getAttack(), W_STAT))
                .append(pad("Defense: " + p.getDefense(), W_STAT));

        Experience xp = p.getXP();
        sb.append(pad("Level: " + xp.getLevel(), W_LEVEL)).append(pad("Experience: " + xp.getXP() + "/" + xp.nextThreshold(), W_XP));

        MenuStatVisitor visitor = new MenuStatVisitor();
        p.accept(visitor);
        String tailA = visitor.getTailA();
        String tailB = visitor.getTailB();

        sb.append(pad(tailA, W_TAIL_A));
        if (!tailB.isEmpty()) {
            sb.append(tailB);
        }

        return sb.toString();
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }
}