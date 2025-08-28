package game;

import units.enemy.Enemy;

import java.util.ArrayList;
import java.util.Iterator;

public class EnemyController {
    public void runAI() {
        GameManager gm = GameManager.I();

        for (Enemy e : new ArrayList<>(gm.getEnemies())) {
            if (e.getHealth().isDead())
                continue;

            e.act();
        }
    }
}