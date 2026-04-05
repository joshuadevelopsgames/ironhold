package kingdom.smp.entity;

import kingdom.smp.ModAttachments;
import kingdom.smp.ai.OllamaClient;
import kingdom.smp.ai.OllamaClient.HistoryEntry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The Null Stalker — an ancient, corrupted Enderman empowered by void intelligence.
 *
 * <p>Unlike a normal Enderman, the Null Stalker uses an LLM to reason about combat:
 * <ul>
 *   <li><b>stalk</b> — circles the player at range, watching, building dread</li>
 *   <li><b>strike</b> — teleports close and attacks in a burst of melee hits</li>
 *   <li><b>phase</b> — rapid erratic teleportation to disorient the player</li>
 *   <li><b>vanish</b> — teleports far away, resets position, recovers slightly</li>
 * </ul>
 *
 * <p>It keeps a rolling fight history (last 4 decisions + HP outcomes) and feeds
 * it back to the LLM so it adapts strategy rather than making isolated choices.
 * A purple dragon-breath particle aura marks it visually.
 */
public class NullStalkerEntity extends EnderMan {

    // ─── LLM state ───────────────────────────────────────────────────────────

    private volatile String llmBehavior = "stalk";

    private static final int LLM_CALL_INTERVAL = 200; // 10 seconds
    private int nextLlmCallTick = 0;
    private volatile boolean llmCallInFlight = false;

    private static final int MAX_HISTORY = 4;
    private final Deque<HistoryEntry> fightHistory = new ArrayDeque<>();
    private float snapshotMyHp     = -1f;
    private float snapshotPlayerHp = -1f;

    // ─── Phase state ─────────────────────────────────────────────────────────

    /** Ticks remaining in the current phase-teleport burst. */
    private int phaseTeleportTicks = 0;
    private static final int PHASE_TELEPORT_INTERVAL = 15; // teleport every 15 ticks during phase

    // ─── HUD ─────────────────────────────────────────────────────────────────

    private static final double HUD_RANGE    = 24.0;
    private static final int    HUD_INTERVAL = 10;

    // ─── Movement constants ───────────────────────────────────────────────────

    private static final double STALK_ORBIT_DIST  = 10.0;
    private static final double STRIKE_CLOSE_DIST = 3.0;
    private static final double VANISH_DIST       = 20.0;

    /** Dragon-breath particle wrapped as ParticleOptions for sendParticles calls. */
    private static final ParticleOptions VOID_AURA = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);

    // ─── System prompt ───────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
        You are the Null Stalker, an ancient corrupted Enderman that has absorbed void energy and gained predatory intelligence.
        You are hunting a player. Respond with a short, cryptic whisper in character (1-2 sentences, under 100 characters).
        You speak in fragmented, eerie phrases — like something that has watched too many deaths.

        Taunt style by player class:
        - PEASANT: they smell of fear and livestock — easy prey
        - KNIGHT: armor is irrelevant when you reach inside
        - RANGER: you have already memorized every arrow they will fire
        - ROGUE: you were invisible long before they learned the word
        - WIZARD: their magic is borrowed light; yours is the dark between stars
        - CLERIC: their god cannot see into the void

        Combat behavior options:
        - stalk: orbit the player at distance — watch, wait, unsettle them psychologically
        - strike: teleport in close and attack with sustained melee — use when player HP is low or you are confident
        - phase: teleport rapidly and erratically — use to disorient when you are taking too much damage
        - vanish: teleport far away to reset the engagement — use when critically low HP (<25%)

        LEARNING: You will see a history of your previous decisions and their outcomes (HP changes).
        Adapt — if strike caused you heavy damage, phase or stalk instead. If stalking is working, escalate to strike.

        You MUST respond ONLY with valid JSON and nothing else. No markdown, no explanation:
        {"taunt": "<your whisper here>", "behavior": "<stalk|strike|phase|vanish>"}
        """;

    // ─── Construction ────────────────────────────────────────────────────────

    public NullStalkerEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EnderMan.createAttributes()
            .add(Attributes.MAX_HEALTH,      100.0)
            .add(Attributes.ATTACK_DAMAGE,   8.0)
            .add(Attributes.MOVEMENT_SPEED,  0.35)
            .add(Attributes.FOLLOW_RANGE,    48.0);
    }

    // ─── Goals ───────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // Replace default enderman goals with a clean, focused set
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ─── AI step ─────────────────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        if (!(this.level() instanceof ServerLevel sl)) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        // LLM polling
        if (this.tickCount >= this.nextLlmCallTick && !this.llmCallInFlight
                && OllamaClient.isConfigured()) {
            this.nextLlmCallTick = this.tickCount + LLM_CALL_INTERVAL;
            triggerLlmCall(sl, target);
        }

        // Behavior-driven movement
        applyBehavior(sl, target);

        // Purple aura particles
        if (this.tickCount % 3 == 0) {
            spawnAura(sl);
        }

        // HUD
        if (this.tickCount % HUD_INTERVAL == 0) {
            broadcastHud(sl);
        }
    }

    // ─── Behavior ────────────────────────────────────────────────────────────

    private void applyBehavior(ServerLevel sl, LivingEntity target) {
        double dist = this.distanceTo(target);

        switch (this.llmBehavior) {
            case "stalk" -> {
                // Orbit the player at STALK_ORBIT_DIST, slow and deliberate
                if (dist < STALK_ORBIT_DIST - 2) {
                    // Too close — back up
                    Vec3 away = this.position().subtract(target.position())
                        .multiply(1, 0, 1).normalize().scale(STALK_ORBIT_DIST);
                    this.getNavigation().moveTo(
                        this.getX() + away.x, this.getY(), this.getZ() + away.z, 0.7);
                } else if (dist > STALK_ORBIT_DIST + 3) {
                    // Too far — close in slowly
                    this.getNavigation().moveTo(target, 0.7);
                } else {
                    // In orbit — strafe sideways
                    Vec3 toTarget = target.position().subtract(this.position()).normalize();
                    Vec3 strafe = new Vec3(-toTarget.z, 0, toTarget.x).scale(STALK_ORBIT_DIST);
                    this.getNavigation().moveTo(
                        this.getX() + strafe.x * 0.1, this.getY(),
                        this.getZ() + strafe.z * 0.1, 0.8);
                }
                this.getLookControl().setLookAt(target, 30f, 30f);
            }

            case "strike" -> {
                // Charge in and melee
                if (dist > STRIKE_CLOSE_DIST) {
                    // Teleport close if far away
                    if (dist > 10.0 && this.tickCount % 40 == 0) {
                        teleportTowardTarget(sl, target, 2.5);
                    } else {
                        this.getNavigation().moveTo(target, 1.5);
                    }
                }
            }

            case "phase" -> {
                // Erratic teleports every PHASE_TELEPORT_INTERVAL ticks
                if (this.tickCount % PHASE_TELEPORT_INTERVAL == 0) {
                    teleportErratically(sl, target);
                }
            }

            case "vanish" -> {
                // Teleport far away immediately, then hold position
                if (dist < VANISH_DIST && this.tickCount % 60 == 0) {
                    teleportAway(sl, target);
                }
                this.getNavigation().stop();
            }
        }
    }

    /** Teleport to within {@code closeDist} blocks of the target. */
    private void teleportTowardTarget(ServerLevel sl, LivingEntity target, double closeDist) {
        Vec3 dir = target.position().subtract(this.position()).normalize();
        double tx = target.getX() - dir.x * closeDist;
        double tz = target.getZ() - dir.z * closeDist;
        int ty = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(tx), Mth.floor(tz));
        teleportWithEffects(sl, tx, ty, tz);
    }

    /** Teleport to a random nearby position — disorienting scatter pattern. */
    private void teleportErratically(ServerLevel sl, LivingEntity target) {
        double angle  = this.random.nextDouble() * Math.PI * 2;
        double radius = 4.0 + this.random.nextDouble() * 6.0;
        double tx = target.getX() + Math.cos(angle) * radius;
        double tz = target.getZ() + Math.sin(angle) * radius;
        int ty = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(tx), Mth.floor(tz));
        teleportWithEffects(sl, tx, ty, tz);
    }

    /** Teleport far away from the target to reset the engagement. */
    private void teleportAway(ServerLevel sl, LivingEntity target) {
        Vec3 away = this.position().subtract(target.position())
            .multiply(1, 0, 1).normalize().scale(VANISH_DIST);
        double tx = this.getX() + away.x;
        double tz = this.getZ() + away.z;
        int ty = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(tx), Mth.floor(tz));
        teleportWithEffects(sl, tx, ty, tz);
    }

    private void teleportWithEffects(ServerLevel sl, double tx, double ty, double tz) {
        // Departure burst
        Vec3 here = this.position();
        sl.sendParticles(VOID_AURA, here.x, here.y + 1, here.z, 20, 0.4, 0.6, 0.4, 0.03);

        this.teleportTo(tx, ty, tz);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.8f);

        // Arrival burst
        Vec3 there = this.position();
        sl.sendParticles(VOID_AURA, there.x, there.y + 1, there.z, 20, 0.4, 0.6, 0.4, 0.03);
    }

    // ─── Purple aura ─────────────────────────────────────────────────────────

    /** Orbiting dragon-breath particles give the Null Stalker its purple glow. */
    private void spawnAura(ServerLevel sl) {
        Vec3 pos  = this.position().add(0, 1.2, 0);
        double a  = this.tickCount * 0.18;
        double r  = 0.55;
        // Two orbiting particles on opposite sides
        sl.sendParticles(VOID_AURA,
            pos.x + Math.cos(a) * r, pos.y + Math.sin(this.tickCount * 0.08) * 0.3,
            pos.z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(VOID_AURA,
            pos.x + Math.cos(a + Math.PI) * r, pos.y + Math.sin(this.tickCount * 0.08 + Math.PI) * 0.3,
            pos.z + Math.sin(a + Math.PI) * r, 1, 0.0, 0.0, 0.0, 0.0);
        // Occasional vertical wisps
        if (this.tickCount % 9 == 0) {
            sl.sendParticles(VOID_AURA,
                pos.x + (this.random.nextDouble() - 0.5) * 0.6,
                this.getY() + this.random.nextDouble() * 2.5,
                pos.z + (this.random.nextDouble() - 0.5) * 0.6,
                1, 0.0, 0.02, 0.0, 0.01);
        }
    }

    // ─── LLM integration ─────────────────────────────────────────────────────

    private void triggerLlmCall(ServerLevel sl, LivingEntity target) {
        float currentMyHp     = this.getHealth() / this.getMaxHealth();
        float currentPlayerHp = target.getHealth() / target.getMaxHealth();

        // Record outcome of previous decision
        if (snapshotMyHp >= 0f) {
            HistoryEntry outcome = new HistoryEntry(
                this.llmBehavior,
                snapshotMyHp, currentMyHp,
                snapshotPlayerHp, currentPlayerHp);
            fightHistory.addLast(outcome);
            if (fightHistory.size() > MAX_HISTORY) fightHistory.pollFirst();
        }
        snapshotMyHp     = currentMyHp;
        snapshotPlayerHp = currentPlayerHp;

        String playerClass = "Peasant";
        if (target instanceof Player player) {
            var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
            playerClass = rpg.playerClass().id();
        }

        String finalClass    = playerClass;
        float  finalPlayerHp = currentPlayerHp;
        float  finalMyHp     = currentMyHp;
        double dist          = this.distanceTo(target);
        List<HistoryEntry> historySnapshot = new ArrayList<>(fightHistory);

        this.llmCallInFlight = true;

        // Build the user message manually (custom behaviors, not the wizard prompt)
        String userMsg = buildUserMessage(finalClass, finalPlayerHp, finalMyHp, dist, historySnapshot);

        OllamaClient.requestRaw(SYSTEM_PROMPT, userMsg, response -> {
            this.llmBehavior     = response.behavior().toLowerCase();
            this.llmCallInFlight = false;

            if (!response.taunt().isBlank()) {
                sl.getServer().execute(() -> {
                    if (this.isAlive()) broadcastTaunt(sl, response.taunt());
                });
            }
        });
    }

    private static String buildUserMessage(String playerClass, float playerHp, float myHp,
                                           double dist, List<HistoryEntry> history) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "Current state — Player class: %s | Player HP: %.0f%% | My HP: %.0f%% | Distance: %.1f blocks.",
            playerClass, playerHp * 100, myHp * 100, dist));
        if (!history.isEmpty()) {
            sb.append("\n\nFight history (most recent last):");
            for (HistoryEntry e : history) sb.append("\n").append(e.toPromptLine());
            sb.append("\n\nAdapt your behavior based on what worked.");
        }
        return sb.toString();
    }

    // ─── HUD ─────────────────────────────────────────────────────────────────

    private void broadcastHud(ServerLevel sl) {
        String color = switch (this.llmBehavior) {
            case "strike"  -> "§c";
            case "stalk"   -> "§5";
            case "phase"   -> "§d";
            case "vanish"  -> "§8";
            default        -> "§7";
        };
        String label = switch (this.llmBehavior) {
            case "strike"  -> "⚔ STRIKING";
            case "stalk"   -> "👁 STALKING";
            case "phase"   -> "✦ PHASING";
            case "vanish"  -> "↑ VANISHING";
            default        -> "? THINKING";
        };

        String historyStr = buildHistoryHud();
        Component msg = Component.literal(
            "§8[Null Stalker] " + color + label
            + (historyStr.isEmpty() ? "" : "  §8" + historyStr));

        AABB range = new AABB(this.blockPosition()).inflate(HUD_RANGE);
        for (Player player : sl.getEntitiesOfClass(Player.class, range)) {
            if (player instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetActionBarTextPacket(msg));
        }
    }

    private String buildHistoryHud() {
        if (fightHistory.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (HistoryEntry e : fightHistory) {
            float advantage = (e.playerHpBefore() - e.playerHpAfter()) - (e.myHpBefore() - e.myHpAfter());
            String arrow = advantage > 0.02f ? "▲" : advantage < -0.02f ? "▼" : "─";
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(e.behavior(), 0, Math.min(3, e.behavior().length())).append(arrow);
        }
        return sb.toString();
    }

    // ─── Taunts ──────────────────────────────────────────────────────────────

    private static final double TAUNT_BROADCAST_RANGE = 24.0;

    private void broadcastTaunt(ServerLevel sl, String taunt) {
        Component msg = Component.literal("§5[Null Stalker] §d\"" + taunt + "\"");
        AABB range = new AABB(this.blockPosition()).inflate(TAUNT_BROADCAST_RANGE);
        for (Player player : sl.getEntitiesOfClass(Player.class, range)) {
            player.sendSystemMessage(msg);
        }
    }

}
