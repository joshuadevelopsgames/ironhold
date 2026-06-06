package kingdom.smp.music;

import kingdom.smp.client.screen.VillagerDialogueScreen;
import kingdom.smp.client.screen.WardenDialogueScreen;
import kingdom.smp.entity.AbstractVoicedNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;

/**
 * Recomputes the set of active {@link MusicTrigger}s each client tick. Clean-room
 * equivalent of ReactiveMusic's {@code SongPicker.tickEventMap()}, in Mojang mappings.
 *
 * <p>Cheap checks (time, weather, dimension, riding, depth, dying, dialogue, PvP) run
 * every tick. Proximity scans that allocate an entity list (NPC / village / hostiles)
 * are throttled to {@link #SCAN_INTERVAL} ticks to stay TPS-friendly.
 */
public final class ReactiveMusicState {
    public static final ReactiveMusicState INSTANCE = new ReactiveMusicState();

    private static final int SCAN_INTERVAL = 10;
    private static final double NPC_RANGE = 16.0;
    private static final double VILLAGE_RANGE_XZ = 30.0;
    private static final double VILLAGE_RANGE_Y = 15.0;
    private static final double HOSTILE_RANGE_XZ = 12.0;
    private static final double HOSTILE_RANGE_Y = 6.0;

    private final EnumSet<MusicTrigger> active = EnumSet.noneOf(MusicTrigger.class);

    // Throttled scan results, retained between scans.
    private boolean nearNpc;
    private boolean village;
    private boolean nearbyMobs;
    private long lastScanTick = Long.MIN_VALUE;

    // Reflection handle for the boss-bar map (Mojang-named field at runtime on NeoForge).
    private static Field bossEventsField;
    private static boolean bossReflectFailed;

    private ReactiveMusicState() {}

    public boolean is(MusicTrigger t) {
        return active.contains(t);
    }

    /** Recompute the active-trigger set for this tick. */
    public void refresh(Minecraft mc, long gameTime) {
        active.clear();

        LocalPlayer player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) {
            active.add(MusicTrigger.MAIN_MENU);
            return;
        }

        active.add(MusicTrigger.GENERIC);

        // ── Dialogue (an Ironhold NPC screen is open) ──
        if (mc.screen instanceof VillagerDialogueScreen || mc.screen instanceof WardenDialogueScreen) {
            active.add(MusicTrigger.IN_DIALOGUE);
        }

        // ── Time of day (overworld clock, valid in any dimension) ──
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        boolean night = time >= 13000 && time < 23000;
        if (night) active.add(MusicTrigger.NIGHT); else active.add(MusicTrigger.DAY);
        if (time >= 12000 && time < 13000) active.add(MusicTrigger.SUNSET);
        if (time >= 23000) active.add(MusicTrigger.SUNRISE);

        // ── Weather ──
        if (level.isThundering()) active.add(MusicTrigger.STORM);
        else if (level.isRaining()) active.add(MusicTrigger.RAIN);

        // ── Dimension ──
        boolean overworld = level.dimension() == Level.OVERWORLD;
        if (level.dimension() == Level.NETHER) active.add(MusicTrigger.NETHER);
        if (level.dimension() == Level.END) active.add(MusicTrigger.END);

        // ── Depth / height ──
        BlockPos pos = player.blockPosition();
        boolean underground = !level.canSeeSkyFromBelowWater(pos);
        if (overworld && underground && pos.getY() < 55) active.add(MusicTrigger.UNDERGROUND);
        if (overworld && underground && pos.getY() < 15) active.add(MusicTrigger.DEEP_UNDERGROUND);
        if (overworld && !underground && pos.getY() > 128) active.add(MusicTrigger.HIGH_UP);
        if (player.isUnderWater()) active.add(MusicTrigger.UNDERWATER);

        // ── Riding ──
        var vehicle = player.getVehicle();
        if (vehicle instanceof AbstractMinecart) active.add(MusicTrigger.MINECART);
        if (vehicle instanceof AbstractBoat) active.add(MusicTrigger.BOAT);
        if (vehicle instanceof AbstractHorse) active.add(MusicTrigger.HORSE);

        // ── State ──
        if (player.fishing != null) active.add(MusicTrigger.FISHING);
        if (player.getHealth() / player.getMaxHealth() < 0.35f) active.add(MusicTrigger.DYING);

        // ── Boss bar ──
        if (isBossBarActive(mc)) active.add(MusicTrigger.BOSS);

        // ── Proximity scans (throttled) ──
        if (gameTime - lastScanTick >= SCAN_INTERVAL) {
            lastScanTick = gameTime;
            runScans(level, player);
        }
        if (nearNpc) active.add(MusicTrigger.NEAR_NPC);
        if (village) active.add(MusicTrigger.VILLAGE);
        if (nearbyMobs) active.add(MusicTrigger.NEARBY_MOBS);

        // ── PvP escalation ──
        switch (PvpEscalation.INSTANCE.tier()) {
            case CLIMAX -> active.add(MusicTrigger.PVP_CLIMAX);
            case SKIRMISH -> active.add(MusicTrigger.PVP_SKIRMISH);
            case NONE -> {}
        }
    }

    private void runScans(Level level, Player player) {
        AABB npcBox = new AABB(player.blockPosition()).inflate(NPC_RANGE, NPC_RANGE, NPC_RANGE);
        nearNpc = !level.getEntitiesOfClass(AbstractVoicedNpcEntity.class, npcBox, e -> true).isEmpty();

        AABB villageBox = new AABB(player.blockPosition())
            .inflate(VILLAGE_RANGE_XZ, VILLAGE_RANGE_Y, VILLAGE_RANGE_XZ);
        village = !level.getEntitiesOfClass(Villager.class, villageBox, e -> true).isEmpty();

        AABB hostileBox = new AABB(player.blockPosition())
            .inflate(HOSTILE_RANGE_XZ, HOSTILE_RANGE_Y, HOSTILE_RANGE_XZ);
        nearbyMobs = !level.getEntitiesOfClass(Monster.class, hostileBox, e -> true).isEmpty();
    }

    private static boolean isBossBarActive(Minecraft mc) {
        if (bossReflectFailed || mc.gui == null) return false;
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay == null) return false;
        try {
            if (bossEventsField == null) {
                for (Field f : BossHealthOverlay.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        bossEventsField = f;
                        break;
                    }
                }
                if (bossEventsField == null) {
                    bossReflectFailed = true;
                    return false;
                }
            }
            Object map = bossEventsField.get(overlay);
            return map instanceof Map<?, ?> m && !m.isEmpty();
        } catch (Throwable t) {
            bossReflectFailed = true;
            return false;
        }
    }
}
