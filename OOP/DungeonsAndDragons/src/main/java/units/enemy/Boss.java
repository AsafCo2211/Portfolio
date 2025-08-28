package units.enemy;

import game.GameManager;
import units.player.Player;
import values.Position;
import units.HeroicUnit;

public class Boss extends Enemy implements HeroicUnit {

    private final int abilityFreq;
    private int combatTicks = 0;

    public Boss(String n, char sym, Position pos, int hp, int atk, int def, int vision, int abilityFreq, int xp) {
        super(n, sym, pos, hp, atk, def, vision, xp);
        this.abilityFreq = abilityFreq;
    }

    @Override
    public void act() {
        GameManager gm = GameManager.I();
        Player player = gm.getPlayer();
        if (player == null || player.getHealth().isDead())
            return;

        int dist = distanceTo(player.getPosition());

        if (dist <= visionRange) {
            if (combatTicks == abilityFreq) {
                combatTicks = 0;
                castAbility();
                return;
            } else combatTicks++;
        } else {
            combatTicks = 0;
        }

        gm.getBoardController().move(this, calcNextStepToward(player.getPosition()));
    }

    @Override
    public void castAbility() {
        var player = GameManager.I().getPlayer();
        if (player == null) return;
        if (distanceTo(player.getPosition()) > visionRange) return;

        GameManager.I().msg(name + " casts Shoebodybop!");

        int attRoll = attack;
        int defRoll = player.rollDefense();
        int dmg = Math.max(0, attRoll - defRoll);

        GameManager.I().msg(name + " rolled " + attRoll + " attack points");
        GameManager.I().msg(player.getName() + " rolled " + defRoll + " defence points");
        GameManager.I().msg(name + " dealt " + dmg + " damage to " + player.getName() + "\n");

        player.takeTrueDamage(dmg);
    }
}
