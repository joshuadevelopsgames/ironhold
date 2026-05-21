package kingdom.smp.seasons;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Posted on {@code NeoForge.EVENT_BUS} when a level's sub-season or tropical sub-season rolls over.
 * Sub-classes distinguish the two kinds so listeners can opt into just one.
 */
public abstract class SeasonChangedEvent extends Event {
    private final Level level;
    private final SeasonState previous;
    private final SeasonState current;

    protected SeasonChangedEvent(Level level, SeasonState previous, SeasonState current) {
        this.level = level;
        this.previous = previous;
        this.current = current;
    }

    public Level level() { return level; }
    public SeasonState previous() { return previous; }
    public SeasonState current() { return current; }

    /** Fires when the standard SubSeason changes (12 transitions per year). */
    public static final class SubSeason extends SeasonChangedEvent {
        public SubSeason(Level level, SeasonState previous, SeasonState current) { super(level, previous, current); }
    }

    /** Fires when the TropicalSeason changes (6 transitions per year). */
    public static final class Tropical extends SeasonChangedEvent {
        public Tropical(Level level, SeasonState previous, SeasonState current) { super(level, previous, current); }
    }
}
