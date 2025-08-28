package tiles;

import values.Position;

public class Empty extends Tile {
    public Empty(Position pos) { super(pos, '.'); }
    @Override public void accept(TileVisitor visitor) { visitor.visit(this); }
}