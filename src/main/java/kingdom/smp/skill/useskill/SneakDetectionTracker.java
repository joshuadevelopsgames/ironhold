package kingdom.smp.skill.useskill;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.SneakDetectionPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side tracker for the sneak-eye HUD and Sneak/Pickpocket coupling.
 *
 * <p>Recomputes detection state for every crouching {@link ServerPlayer} every
 * {@link #TICK_INTERVAL} ticks. Pushes a {@link SneakDetectionPayload} when
 * the state changes and caches the latest state so other systems (notably
 * {@link PickpocketHandler}) can read it without re-scanning.
 *
 * <p>Only <em>humanoid witnesses</em> (other players, villagers, monsters,
 * golems) count toward NEARBY/SEEN. Animals, fish, and ambient mobs (bats)
 * are ignored — a chicken seeing you doesn't make you "seen."
 *
 * <p>Caution radius scales with the player's Sneak skill: a level-100 sneak
 * skill halves the effective radius from {@value #BASE_CAUTION_RADIUS} to
 * {@value #MIN_CAUTION_RADIUS}, making the player effectively invisible
 * inside their reduced bubble.
 *
 * <p>Sneak XP is awarded each tracker tick the player is in NEARBY state
 * (humanoids within range but no line of sight on the player) — i.e. when
 * the player is actually sneaking past a witness.
 *
 * <p>"Watching" is stricter than geometric line of sight: a humanoid is only
 * counted as a watcher if the player is also within the humanoid's front
 * facing cone (so a villager with its back turned isn't a watcher even if
 * an unobstructed line exists). This synergizes with
 * {@link VillagerStealthHandler}, which clears villagers' {@code LOOK_TARGET}
 * so their default head direction points where they're walking rather than
 * at the player.
 *
 * <p>State semantics (matching client-side ordinals):
 * <ol start="0">
 *   <li><b>HIDDEN</b> — nobody is actively watching you (closed eye).</li>
 *   <li><b>NEARBY</b> — someone is watching you, but they're NOT in your
 *       view (almost-closed eye) — typically behind or beside you.</li>
 *   <li><b>SEEN</b> — someone in your line of sight is watching you
 *       (start-closing eye) — mutual visibility, face to face.</li>
 *   <li><b>DETECTED</b> — a mob has this player as its current target
 *       (full-open eye).</li>
 * </ol>
 */
public final class SneakDetectionTracker {
    private SneakDetectionTracker() {}

    public static final byte HIDDEN = 0;
    public static final byte NEARBY = 1;
    public static final byte SEEN = 2;
    public static final byte DETECTED = 3;

    private static final int TICK_INTERVAL = 5;
    private static final double DETECT_RADIUS = 32.0;
    private static final double BASE_CAUTION_RADIUS = 16.0;
    private static final double MIN_CAUTION_RADIUS = 8.0;

    /** XP awarded per tracker tick (every 5 ticks) to a player in NEARBY state. */
    private static final float SNEAK_XP_PER_TICK = 0.08f;

    /** Dot threshold for "watcher is facing the player." 0.3 ≈ 140° front cone. */
    private static final double WATCHER_DOT_THRESHOLD = 0.3;
    /** Dot threshold for "watcher is in player's view." 0.0 ≈ 180° front hemisphere. */
    private static final double PLAYER_VIEW_DOT_THRESHOLD = 0.0;

    private static final ConcurrentHashMap<UUID, Byte> stateCache = new ConcurrentHashMap<>();

    /** Last computed state for this player, or HIDDEN if uncrouched / unknown. */
    public static byte getCachedState(UUID playerUuid) {
        return stateCache.getOrDefault(playerUuid, HIDDEN);
    }

    /**
     * Caution radius scaled by the player's Sneak skill. Returns
     * {@link #BASE_CAUTION_RADIUS} at level 0, {@link #MIN_CAUTION_RADIUS}
     * at level {@link UseSkill#MAX_LEVEL}, linearly interpolated.
     */
    public static double effectiveCautionRadius(ServerPlayer player) {
        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int sneakLevel = skills.levelFor(UseSkill.SNEAK);
        double t = sneakLevel / (double) UseSkill.MAX_LEVEL;
        return BASE_CAUTION_RADIUS - (BASE_CAUTION_RADIUS - MIN_CAUTION_RADIUS) * t;
    }

    /**
     * True if this entity counts as a "judgmental" witness — a player,
     * villager, monster, or golem that is alive and awake. Animals, fish,
     * and ambient mobs (bats) don't count; neither do sleeping or dead
     * entities (no awareness while in bed).
     */
    public static boolean countsAsWitness(LivingEntity entity) {
        if (!entity.isAlive()) return false;
        if (entity.isSleeping()) return false;
        if (entity instanceof Player) return true;
        if (entity instanceof Animal) return false;
        if (entity instanceof AmbientCreature) return false;
        if (entity instanceof AbstractFish) return false;
        return entity instanceof Mob;
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if ((player.tickCount % TICK_INTERVAL) != 0) return;

        if (!player.isCrouching()) {
            Byte prev = stateCache.put(player.getUUID(), HIDDEN);
            if (prev != null && prev != HIDDEN) {
                PacketDistributor.sendToPlayer(player, new SneakDetectionPayload(HIDDEN));
            }
            return;
        }

        byte state = computeState(player);
        Byte prev = stateCache.put(player.getUUID(), state);
        if (prev == null || prev != state) {
            PacketDistributor.sendToPlayer(player, new SneakDetectionPayload(state));
        }

        if (state == NEARBY) {
            grantSneakXp(player);
        }
    }

    private static byte computeState(ServerPlayer player) {
        double cautionRadius = effectiveCautionRadius(player);
        double cautionSq = cautionRadius * cautionRadius;

        AABB box = player.getBoundingBox().inflate(DETECT_RADIUS);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, box);

        Vec3 playerEye = player.getEyePosition(1.0f);
        Vec3 playerLook = player.getViewVector(1.0f);
        boolean anyWatcherInView = false;     // → SEEN
        boolean anyWatcherOutOfView = false;  // → NEARBY

        for (LivingEntity entity : nearby) {
            if (entity == player) continue;
            if (entity instanceof Mob mob && mob.getTarget() == player) return DETECTED;
            if (!countsAsWitness(entity)) continue;
            if (entity.distanceToSqr(player) > cautionSq) continue;
            if (!isWatching(entity, player)) continue;

            if (isInPlayerView(playerEye, playerLook, entity)) {
                anyWatcherInView = true;
            } else {
                anyWatcherOutOfView = true;
            }
        }

        if (anyWatcherInView) return SEEN;
        if (anyWatcherOutOfView) return NEARBY;
        return HIDDEN;
    }

    /**
     * True iff {@code watcher} has unobstructed line of sight to {@code player}
     * AND the player is inside the watcher's front facing cone (so a turned-
     * away villager is not a watcher even if geometry would allow it).
     */
    private static boolean isWatching(LivingEntity watcher, ServerPlayer player) {
        if (!watcher.hasLineOfSight(player)) return false;
        Vec3 watcherEye = watcher.getEyePosition(1.0f);
        Vec3 watcherLook = watcher.getViewVector(1.0f);
        Vec3 toPlayer = player.getEyePosition(1.0f).subtract(watcherEye).normalize();
        return watcherLook.dot(toPlayer) > WATCHER_DOT_THRESHOLD;
    }

    private static boolean isInPlayerView(Vec3 playerEye, Vec3 playerLook, LivingEntity other) {
        Vec3 toOther = other.getEyePosition(1.0f).subtract(playerEye).normalize();
        return playerLook.dot(toOther) > PLAYER_VIEW_DOT_THRESHOLD;
    }

    private static void grantSneakXp(ServerPlayer player) {
        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int prevLevel = skills.levelFor(UseSkill.SNEAK);
        PlayerUseSkills next = skills.withAddedXp(UseSkill.SNEAK, SNEAK_XP_PER_TICK);
        player.setData(ModAttachments.USE_SKILLS.get(), next);
        int newLevel = next.levelFor(UseSkill.SNEAK);
        if (newLevel > prevLevel) {
            player.sendSystemMessage(Component.literal(
                "§6§lSneak §r§7level up §8(§f" + prevLevel + " §8→ §a" + newLevel + "§8)"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }
}
