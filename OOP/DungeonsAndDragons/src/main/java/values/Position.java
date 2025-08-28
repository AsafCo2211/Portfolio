package values;

public final class Position {
    private final int x, y;

    public Position(int x, int y) { this.x = x; this.y = y; }

    public int getX() { return x; }
    public int getY() { return y; }

    public int distance(Position other) {
        double dx = x - other.x, dy = y - other.y;
        return (int) Math.floor(Math.hypot(dx, dy));
    }

    public Position translate(int dx, int dy) { return new Position(x + dx, y + dy); }

    @Override public boolean equals(Object o) {
        return o instanceof Position p && p.x == x && p.y == y;
    }
    @Override public String toString() { return "(" + x + "," + y + ")"; }
}