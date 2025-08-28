package tests.values_tests;

import values.Mana;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManaTest {

    @Test
    void testInitialization() {
        Mana m = new Mana(100);
        assertEquals(100, m.getPool());
        assertEquals(100, m.get());
    }

    @Test
    void testCostAndRegen() {
        Mana m = new Mana(100);
        m.spend(30);
        assertEquals(70, m.get());

        m.regen(10);
        assertEquals(80, m.get());

        m.regen(100);
        assertEquals(100, m.get());

        assertFalse(m.spend(150));
    }

    @Test
    void testFullRegen() {
        Mana m = new Mana(50);
        m.spend(40);
        m.refillFull();
        assertEquals(50, m.get());
    }
}
