package units.enemy;

import values.Position;
import units.player.Player;
import game.GameManager;

public class Monster extends Enemy {

    public Monster(String n, char sym, Position pos, int hp, int atk, int def, int vision, int xp) {
        super(n, sym, pos, hp, atk, def, vision, xp);
    }

    @Override
    public void act() {
        GameManager gm = GameManager.I();
        Player player = gm.getPlayer();
        if (player == null) return;

        Position pPos = player.getPosition();
        Position mPos = this.getPosition();
        if ((Math.abs(pPos.getX() - mPos.getX()) + Math.abs(pPos.getY() - mPos.getY())) == 1) {
            hitPlayer(player);
            return;
        }

        gm.getBoardController().move(this, calcNextStepToward(player.getPosition()));
    }
}