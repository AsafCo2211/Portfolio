package tests;

import org.junit.jupiter.api.*;
import values.Position;
import units.player.Warrior;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest extends WithGameManager {

    Warrior hero;

    @BeforeEach
    void setUp() {
        hero = new Warrior("Conan", '@', new Position(0, 0),
                100, 10, 5, 1);
        gm().setPlayer(hero);
    }

    @Test
    void level_up_increases_stats() {
        int lvl0HpPool = hero.getHealth().getMax();
        hero.gainXP(50);                        // triggers level-up

        assertEquals(2, hero.getXP().getLevel());
        assertTrue(hero.getHealth().getMax() > lvl0HpPool);
        assertEquals(hero.getHealth().getMax(), hero.getHealth().get()); // full heal
    }
}
