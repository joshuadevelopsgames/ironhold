package kingdom.smp.seasons.client;

import kingdom.smp.seasons.SeasonConfig;
import kingdom.smp.seasons.SeasonState;
import kingdom.smp.seasons.SeasonTime;
import kingdom.smp.seasons.network.SyncSeasonPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-dimension client cache of the season cycle. Snaps to the server value on each
 * {@link SyncSeasonPayload} (~1 Hz) and self-advances between packets so the visible
 * sub-season transition isn't quantized to 20-tick steps.
 */
public final class SeasonClientState {
    private SeasonClientState() {}

    private static final ConcurrentMap<ResourceKey<Level>, Integer> cycleTicks = new ConcurrentHashMap<>();

    public static SeasonState current(ResourceKey<Level> dim) {
        Integer ticks = cycleTicks.get(dim);
        if (ticks == null) return SeasonTime.of(SeasonConfig.STARTING_CYCLE_TICKS);
        return SeasonTime.of(ticks);
    }

    /** Called from the payload handler — snaps to server-authoritative value. */
    public static void receive(SyncSeasonPayload payload) {
        cycleTicks.put(payload.dimension(), payload.cycleTicks());
    }

    /** Self-advance once per client tick so color/visuals don't visibly step between sync packets. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        ResourceKey<Level> dim = mc.level.dimension();
        Integer v = cycleTicks.get(dim);
        if (v != null) {
            cycleTicks.put(dim, (v + 1) % SeasonConfig.CYCLE_DURATION_TICKS);
        }
    }

    public static void reset() { cycleTicks.clear(); }
}
