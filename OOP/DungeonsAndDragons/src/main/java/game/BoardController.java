package game;

import tiles.*;
import units.Unit;
import values.Position;

public class BoardController {
    public void move(Unit mover, Position to) {
        Tile dst = GameManager.I().tileAt(to);
        dst.accept(mover);
    }
}