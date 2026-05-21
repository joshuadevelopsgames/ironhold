package kingdom.smp.seasons;

import kingdom.smp.seasons.client.SeasonClientState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Public facade for the seasons system. Call sites in farming logic, biome hooks, etc.
 * should go through here rather than reaching into {@link SeasonSavedData} directly.
 */
public final class Seasons {
    private Seasons() {}

    /** Current season state for the given level (server-authoritative on logical server, sampled client cache otherwise). */
    public static SeasonState current(Level level) {
        if (level instanceof ServerLevel server) {
            return SeasonTime.of(serverData(server).cycleTicks());
        }
        return SeasonClientState.current(level.dimension());
    }

    public static SeasonSavedData serverData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SeasonSavedData.TYPE);
    }

    public static boolean isEnabled(Level level) {
        return SeasonConfig.isDimensionEnabled(level.dimension());
    }
}
