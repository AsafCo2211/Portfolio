package tests.values_tests;

import org.junit.jupiter.api.*;
import values.Experience;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceTest {

    @Test
    void gaining_exact_threshold_levels_up_once() {
        Experience xp = new Experience();
        assertTrue(xp.gain(50));        // 50 * level(1)
        assertEquals(2, xp.getLevel());
        assertEquals(0, xp.getXP());
    }

    @Test
    void gaining_enough_for_two_levels_handles_loop() {
        Experience xp = new Experience();
        assertTrue(xp.gain(150));       // 50 + 60
        assertEquals(3, xp.getLevel());
        assertEquals(0, xp.getXP());
    }
}
