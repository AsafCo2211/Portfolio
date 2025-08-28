package tests;

import game.GameManager;
import game.generator.DeterministicGenerator;
import game.callbacks.MessageCallback;
import game.callbacks.DeathCallback;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class WithGameManager {


    @BeforeAll
    void bootstrapGM() {
        GameManager.create(new DeterministicGenerator(15))
                .setCallbacks(MessageCallback.ignore(), DeathCallback.ignore());
    }

    protected GameManager gm() { return GameManager.I(); }

    @AfterAll
    void nukeSingleton() throws Exception {
        var field = GameManager.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);      // reflection hack: clear static field
    }
}
