package tests;

import org.junit.jupiter.api.*;
import values.Position;
import units.player.Warrior;

import static org.junit.jupiter.api.Assertions.*;

class UnitTest extends WithGameManager {

    Warrior dummy;

    @BeforeEach
    void setUp() {
        dummy = new Warrior("Stub", '@', new Position(0, 0),
                100, 10, 5, 1);
        gm().setPlayer(dummy);          // only needed for GameManager.msg()
    }

    @Test
    void take_damage_cannot_drop_below_zero() {
        dummy.takeTrueDamage(999);
        assertEquals(0, dummy.getHealth().get());
    }

    @Test
    void heal_cannot_exceed_pool() {
        dummy.takeTrueDamage(50);       // 50 / 100 left
        dummy.getHealth().heal(80);     // should cap at pool (=100)
        assertEquals(100, dummy.getHealth().get());
    }
}
