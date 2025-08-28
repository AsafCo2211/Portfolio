package tests.values_tests;

import values.Health;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HealthTest {

    @Test
    void testInitialization() {
        Health h = new Health(100);
        assertEquals(100, h.getMax());
        assertEquals(100, h.get());
    }

    @Test
    void testDamageAndHeal() {
        Health h = new Health(100);
        h.damage(30);
        assertEquals(70, h.get());

        h.heal(10);
        assertEquals(80, h.get());

        h.heal(50); // Should cap at max
        assertEquals(100, h.get());

        h.damage(200); // Should not go below 0
        assertEquals(0, h.get());
    }

    @Test
    void testHealFull() {
        Health h = new Health(100);
        h.damage(80);
        h.healFull();
        assertEquals(100, h.get());
    }
}
