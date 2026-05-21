package kingdom.smp.seasons;

import kingdom.smp.seasons.network.SyncSeasonPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Advances each enabled level's season cycle counter on the server, fires change events on
 * sub-season / tropical-season rollover, and broadcasts the new cycle tick to clients at a
 * fixed cadence. Tracks day-time deltas (not raw game-time) so {@code /time set} and player
 * sleep skip season time correctly along with the day.
 */
public final class SeasonTickHandler {
    private SeasonTickHandler() {}

    /** Per-dimension previous day-time value, used to compute the delta each tick. */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, Long> previousDayTime = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!SeasonConfig.isDimensionEnabled(level.dimension())) return;

        long currentDayTime = level.getOverworldClockTime();
        Long previous = previousDayTime.put(level.dimension(), currentDayTime);
        if (previous == null) return; // first tick — record baseline only
        long delta = currentDayTime - previous;
        if (delta < 0) delta += 24_000L; // wrap on /time set or sleep

        SeasonSavedData data = Seasons.serverData(level);
        SeasonState before = SeasonTime.of(data.cycleTicks());
        if (delta > 0) {
            data.setCycleTicks(data.cycleTicks() + (int) delta);
        }
        SeasonState after = SeasonTime.of(data.cycleTicks());

        if (before.subSeason() != after.subSeason()) {
            NeoForge.EVENT_BUS.post(new SeasonChangedEvent.SubSeason(level, before, after));
        }
        if (before.tropicalSeason() != after.tropicalSeason()) {
            NeoForge.EVENT_BUS.post(new SeasonChangedEvent.Tropical(level, before, after));
        }

        if ((level.getGameTime() % SeasonConfig.SYNC_INTERVAL_TICKS) == 0) {
            broadcast(level, data.cycleTicks());
        }
    }

    /** Send the current cycle to a single joining player so they see the right tint immediately. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (!SeasonConfig.isDimensionEnabled(level.dimension())) return;
        PacketDistributor.sendToPlayer(sp, new SyncSeasonPayload(level.dimension(), Seasons.serverData(level).cycleTicks()));
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (!SeasonConfig.isDimensionEnabled(level.dimension())) return;
        PacketDistributor.sendToPlayer(sp, new SyncSeasonPayload(level.dimension(), Seasons.serverData(level).cycleTicks()));
    }

    private static void broadcast(ServerLevel level, int ticks) {
        SyncSeasonPayload payload = new SyncSeasonPayload(level.dimension(), ticks);
        for (ServerPlayer sp : level.players()) {
            PacketDistributor.sendToPlayer(sp, payload);
        }
    }
}
