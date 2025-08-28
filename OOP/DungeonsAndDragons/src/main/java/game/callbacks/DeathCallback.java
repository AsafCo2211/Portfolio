package game.callbacks;
import units.Unit;

public interface DeathCallback {
    void onDeath(Unit u);

    static DeathCallback ignore() {
        return unit -> {}; // Do nothing
    }
}