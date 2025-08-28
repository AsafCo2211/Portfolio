package tests;

import game.GameManager;
import org.junit.jupiter.api.*;
import values.Position;
import units.enemy.Monster;
import units.enemy.Trap;
import units.player.Hunter;
import units.player.Warrior;

import static org.junit.jupiter.api.Assertions.*;

class EnemyTest extends WithGameManager {

    Warrior bait;

    @BeforeEach
    void setUp() {
        bait = new Warrior("Bait", '@', new Position(0, 0),
                100, 10, 5, 1);
        gm().setPlayer(bait);
    }

    @Test
    void monster_chases_player_when_in_vision() {
        Monster m = new Monster("Soldier", 's',
                new Position(5, 5),
                80, 8, 3, 3, 25);     // vision = 3 cells
        /* Player is 2 cells up (same col) -> chase up */
        Position playerPos = new Position(5, 3);
        Position newPos = m.calcNextStepToward(playerPos); // <-- helper you expose
        assertEquals(new Position(5, 4), newPos);
    }

    @Test
    void trap_visibility_cycle() {
        Trap t = new Trap("Ninja", 'B', new Position(1, 0), 1, 1, 1, 2, 3, 1);
        Hunter bait = new Hunter("Bait", 'p', new Position(0, 0), 100, 10, 1, 10);
        GameManager.I().setPlayer(bait);

        for (int i = 0; i < 6; i++) {
            boolean expectedToAttack = t.isVisible();
            int hpBefore = bait.getHealth().get();

            t.act();
            if (expectedToAttack)
                assertTrue(bait.getHealth().get() < hpBefore, "Expected attack on tick " + i);
            else
                assertEquals(hpBefore, bait.getHealth().get(), "Expected no attack on tick " + i);

            bait.getHealth().healFull();
        }
    }


}
