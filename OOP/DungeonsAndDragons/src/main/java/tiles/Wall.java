package tiles;

import values.Position;

public class Wall extends Tile {
    public Wall(Position pos) { super(pos, '#'); }
    @Override public void accept(TileVisitor visitor) { visitor.visit(this); }
}