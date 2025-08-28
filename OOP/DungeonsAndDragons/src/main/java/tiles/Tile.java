package tiles;

import values.Position;

public abstract class Tile {
    protected Position pos;
    protected char tileChar;

    protected Tile(Position pos, char tileChar) {
        this.pos = pos;
        this.tileChar = tileChar;
    }

    public Position getPosition() { return pos; }
    public char getTileChar() { return tileChar; }

    public abstract void accept(TileVisitor v);

    @Override public String toString() { return String.valueOf(tileChar); }
}
