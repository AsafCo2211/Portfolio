package util;

import tiles.*;
import units.enemy.Boss;
import units.enemy.Enemy;
import units.enemy.Monster;
import units.enemy.Trap;
import units.player.*;
import values.Position;

import java.util.*;
import java.util.function.Function;

public final class UnitFactory {

    /* ─────────────────────────────── PLAYERS ─────────────────────────────── */
    private static final List<Function<Position, Player>> PLAYERS = List.of(
            p -> new Warrior("Jon Snow", '@', p, 300, 30, 4, 3),
            p -> new Warrior("The Hound", '@', p, 400, 20, 6, 5),

            p -> new Mage("Melisandre", '@', p, 100, 5, 1, 300, 30, 15, 5, 6),
            p -> new Mage("Thoros of Myr", '@', p, 250, 25, 4, 150, 20, 20, 3, 4),

            p -> new Rogue("Arya Stark", '@', p, 150, 40, 2, 100),
            p -> new Rogue("Bronn", '@', p, 250, 35, 3, 100),

            p -> new Hunter("Ygritte", '@', p, 220, 30, 2, 6)
    );


    public static List<Player> catalogue() {
        Position dummy = new Position(0, 0);
        return PLAYERS.stream().map(f -> f.apply(dummy)).toList();
    }

    public static Player createPlayer(int idx, Position pos) {
        if (idx < 0 || idx >= PLAYERS.size())
            throw new IllegalArgumentException("Bad player index: " + idx);

        return PLAYERS.get(idx).apply(pos);
    }

    /* ─────────────────────────────── ENEMIES ─────────────────────────────── */
    private static final Map<Character, Function<Position, Enemy>> ENEMIES =
            Map.ofEntries(
                    entry('s', p -> new Monster("Lannister Soldier", 's', p, 80, 8, 3, 3, 25)),
                    entry('k', p -> new Monster("Lannister Knight", 'k', p, 200, 14, 8, 4, 50)),
                    entry('q', p -> new Monster("Queen's Guard", 'q', p, 400, 20, 15, 5, 100)),

                    entry('z', p -> new Monster("Wright", 'z', p, 600, 30, 15, 3, 100)),
                    entry('b', p -> new Monster("Bear-Wight", 'b', p, 1000, 75, 30, 4, 250)),
                    entry('g', p -> new Monster("Giant-Wight", 'g', p, 1500, 100, 40, 5, 500)),
                    entry('w', p -> new Monster("White Walker", 'w', p, 2000, 150, 50, 6, 1000)),

                    entry('M', p -> new Boss("The Mountain", 'M', p, 1000, 60, 25, 6, 5, 500)),
                    entry('C', p -> new Boss("Queen Cersei", 'C', p, 100, 10, 10, 1, 8, 1000)),
                    entry('K', p -> new Boss("Night’s King", 'K', p, 5000, 300, 150, 8, 3, 5000)),

                    entry('B', p -> new Trap("Bonus Trap", 'B', p, 1, 1, 1, 1, 5, 250)),
                    entry('Q', p -> new Trap("Queen’s Trap", 'Q', p, 250, 50, 10, 3, 7, 100)),
                    entry('D', p -> new Trap("Death Trap", 'D', p, 500, 100, 20, 1, 10, 250))
            );

    public static Enemy spawnEnemy(char tile, Position pos) {
        Function<Position, Enemy> maker = ENEMIES.get(tile);
        return maker == null ? null : maker.apply(pos);
    }

    /* ───────────────────────────── TILE PARSING ─────────────────────────── */
    public static Tile tileFromChar(char c, Position pos) {
        return switch (c) {
            case '#' -> new Wall(pos);
            case '.' -> new Empty(pos);
            case '@' -> null;                    // player handled elsewhere
            default  -> {
                Enemy e = spawnEnemy(c, pos);
                if (e != null) yield e;
                throw new IllegalArgumentException("Unknown tile char '" + c + '\'');
            }
        };
    }

    private static <K, V> Map.Entry<K, V> entry(K k, V v) { return Map.entry(k, v); }

    private UnitFactory() {}
}
