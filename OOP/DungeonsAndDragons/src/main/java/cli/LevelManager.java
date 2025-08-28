package cli;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import tiles.*;
import units.player.*;
import util.UnitFactory;
import game.GameManager;
import values.Position;

public final class LevelManager {

    private final Path dir;
    private int  lvl = 1;
    private final int playerIdx;

    public LevelManager(Path dir, int playerChoice) {
        this.dir = dir;
        this.playerIdx = playerChoice;
    }

    public boolean loadNextLevel() throws IOException {
        Path file = dir.resolve("level" + lvl + ".txt");
        if (!Files.exists(file)) return false;

        List<String> lines = Files.readAllLines(file);
        int rows = lines.size(), cols = lines.get(0).length();

        GameManager gm = GameManager.I();
        gm.getEnemies().clear();

        Tile[][] board = new Tile[rows][cols];
        Player player;

        for (int y = 0; y < rows; y++) {
            String row = lines.get(y);
            for (int x = 0; x < cols; x++) {
                char c = (x < row.length()) ? row.charAt(x) : '#';
                Position pos = new Position(x, y);

                if (c == '@') {
                    if (gm.getPlayer() == null) {
                        player = UnitFactory.createPlayer(playerIdx, pos);
                    } else {
                        player = gm.getPlayer();
                        player.move(pos);
                    }

                    board[y][x] = player;
                    gm.setPlayer(player);
                }
                else {
                    Tile t = UnitFactory.tileFromChar(c, pos);
                    board[y][x] = (t == null) ? new Empty(pos) : t;
                    gm.registerTile(t);
                }
            }
        }

        if (gm.getPlayer() == null)
            throw new IOException("Player not found in level " + lvl);

        gm.loadBoard(board);
        lvl++;
        return true;
    }

    public int getCurrentLevel() { return lvl - 1; }
}