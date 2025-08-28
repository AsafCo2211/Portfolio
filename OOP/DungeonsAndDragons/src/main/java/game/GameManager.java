package game;

import tiles.Empty;
import tiles.Tile;
import tiles.TileVisitor;
import tiles.Wall;
import values.Position;
import units.enemy.Enemy;
import units.Unit;
import units.player.Player;
import units.UnitVisitor;

import game.callbacks.MessageCallback;
import game.callbacks.DeathCallback;
import game.generator.ValueGenerator;

import java.util.*;

public final class GameManager {
    private static GameManager INSTANCE;
    public static GameManager create(ValueGenerator rng) {
        if (INSTANCE != null) throw new IllegalStateException("GameManager already initialised");
        return INSTANCE = new GameManager(rng);
    }
    public static GameManager I() { return INSTANCE; }

    private final ValueGenerator rng;
    private MessageCallback out = s -> {};
    private DeathCallback death = u -> {};

    private final List<Enemy> enemies = new ArrayList<>();
    private Player player;
    private Tile[][] board;

    private final BoardController boardCtl = new BoardController();
    private final EnemyController enemyCtl = new EnemyController();

    private final StringBuilder roundLog = new StringBuilder();

    public void gameTick() {
        if (player != null)
            player.onTick();

        enemyCtl.runAI();
    }

    private class UnitDeathHandler implements UnitVisitor {
        @Override
        public void visit(Player p) {
            msg("You have been defeated. GAME OVER.");
        }

        @Override
        public void visit(Enemy e) {
            enemies.remove(e);
            death.onDeath(e);
            setTile(e.getPosition(), new Empty(e.getPosition()));
            msg(e.getName() + " was defeated.");
            player.gainXP(e.getExpValue());
            msg(player.getName() + " gained " + e.getExpValue() + " EX from " + e.getName());
        }
    }

    private class EnemyAdder implements TileVisitor {
        @Override public void visit(Empty e) {}
        @Override public void visit(Wall w) {}
        @Override public void visit(Player p) {}

        @Override
        public void visit(Enemy e) {
            addEnemy(e);
        }
    }


    private GameManager(ValueGenerator rng) { this.rng = rng; }

    public GameManager setCallbacks(MessageCallback m, DeathCallback d) {
        out = m != null ? m : s -> {};
        death = d != null ? d : u -> {};
        return this;
    }
    public void loadBoard(Tile[][] b) { board = b; }
    public void setPlayer(Player p) { player = p; }
    public Player getPlayer() { return player; }

    public List<Enemy> getEnemies() { return enemies; }
    public void addEnemy(Enemy e) { enemies.add(e); }

    public void handleUnitDeath(Unit u) {
        u.accept(new UnitDeathHandler());
    }

    public void registerTile(Tile t) {
        if (t != null) {
            t.accept(new EnemyAdder());
        }
    }

    public BoardController getBoardController() { return boardCtl; }
    public EnemyController getEnemyController() { return enemyCtl; }

    public int roll(int min, int max) { return rng.roll(min, max); }
    public Tile tileAt(Position p) { return board[p.getY()][p.getX()]; }

    public List<Enemy> getEnemiesWithin(Position p, int range) {
        List<Enemy> list = new ArrayList<>();
        for (Enemy e : enemies)
            if (e.distanceTo(p) <= range) list.add(e);
        return list;
    }

    public void setTile(Position p, tiles.Tile t) {
        board[p.getY()][p.getX()] = t;
    }

    public void msg(String text) {
        roundLog.append(text).append("\n");
    }


    public String flushRoundMessages() {
        String messages = roundLog.toString();
        roundLog.setLength(0);
        return messages;
    }
}