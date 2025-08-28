package tiles;

import units.player.Player;
import units.enemy.Enemy;

public interface TileVisitor {
    void visit(Empty e);
    void visit(Wall w);
    void visit(Player p);
    void visit(Enemy e);
}