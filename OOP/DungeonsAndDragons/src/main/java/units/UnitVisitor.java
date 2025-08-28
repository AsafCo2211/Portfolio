package units;

import units.player.Player;
import units.enemy.Enemy;

/**
 * A visitor interface for Units. Used for operations that need to
 * distinguish between different types of Units, such as Players and Enemies.
 */
public interface UnitVisitor {
    void visit(Player p);
    void visit(Enemy e);
}