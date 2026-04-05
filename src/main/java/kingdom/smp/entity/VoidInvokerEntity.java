package kingdom.smp.entity;

import kingdom.smp.ModAttachments;
import kingdom.smp.ai.OllamaClient;
import kingdom.smp.ai.OllamaClient.HistoryEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Mini-boss variant of {@link ArcaneInvokerEntity} with a full LLM-driven combat AI.
 *
 * <p>Every {@value #LLM_CALL_INTERVAL} ticks the entity:
 * <ol>
 *   <li>Snapshots current HP values</li>
 *   <li>Records the <em>outcome</em> of the previous decision (HP delta over last interval)</li>
 *   <li>Sends current state + fight history to OpenRouter</li>
 *   <li>Receives a new behavior + taunt and applies it immediately</li>
 * </ol>
 *
 * <p>This gives the LLM a rolling window of cause-and-effect, letting it adapt
 * strategy rather than making isolated stateless decisions each round.
 *
 * <p>Nearby players also see a live action-bar HUD showing the current mode.
 */
public class VoidInvokerEntity extends ArcaneInvokerEntity {

    // ─── LLM state ───────────────────────────────────────────────────────────

    /** Current LLM-directed behavior. Volatile — written from HTTP thread, read from tick thread. */
    private volatile String llmBehavior = "aggressive";

    /** How many ticks between LLM calls (200 = 10 seconds). */
    private static final int LLM_CALL_INTERVAL = 200;

    /** Tick at which the next LLM call is allowed. */
    private int nextLlmCallTick = 0;

    /** Guard against concurrent HTTP requests. */
    private volatile boolean llmCallInFlight = false;

    // ─── Fight history (for LLM learning) ────────────────────────────────────

    /** Rolling log of the last {@value #MAX_HISTORY} decisions + outcomes. */
    private static final int MAX_HISTORY = 4;
    private final Deque<HistoryEntry> fightHistory = new ArrayDeque<>();

    /**
     * HP snapshot taken at the START of the current decision interval.
     * Compared against HP at the END to measure the outcome of the chosen behavior.
     */
    private float snapshotMyHp     = -1f;
    private float snapshotPlayerHp = -1f;

    // ─── Movement constants ───────────────────────────────────────────────────

    private static final double KITE_MIN_DIST        = 8.0;
    private static final double KITE_MAX_DIST        = 16.0;
    private static final double AGGRESSIVE_CLOSE_DIST = 5.0;

    // ─── HUD ─────────────────────────────────────────────────────────────────

    private static final double HUD_RANGE = 24.0;

    /** Action-bar update every 10 ticks so it doesn't flicker off. */
    private static final int HUD_INTERVAL = 10;

    // ─── Construction ────────────────────────────────────────────────────────

    public VoidInvokerEntity(EntityType<? extends Illusioner> type, Level level) {
        super(type, level);
        this.xpReward = 35;
    }

    // ─── AI step ─────────────────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        if (!(this.level() instanceof ServerLevel sl)) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        // ── LLM polling ──
        if (this.tickCount >= this.nextLlmCallTick && !this.llmCallInFlight
                && OllamaClient.isConfigured()) {
            this.nextLlmCallTick = this.tickCount + LLM_CALL_INTERVAL;
            triggerLlmCall(sl, target);
        }

        // ── Movement ──
        applyMovementBehavior(target);

        // ── HUD ──
        if (this.tickCount % HUD_INTERVAL == 0) {
            broadcastHud(sl);
        }
    }

    // ─── Movement ────────────────────────────────────────────────────────────

    private void applyMovementBehavior(LivingEntity target) {
        double dist = this.distanceTo(target);
        switch (this.llmBehavior) {
            case "aggressive" -> {
                if (dist > AGGRESSIVE_CLOSE_DIST) this.getNavigation().moveTo(target, 1.45);
            }
            case "kite" -> {
                if (dist < KITE_MIN_DIST) {
                    Vec3 away = this.position().subtract(target.position())
                        .multiply(1, 0, 1).normalize().scale(KITE_MAX_DIST);
                    this.getNavigation().moveTo(
                        this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.2);
                } else if (dist > KITE_MAX_DIST) {
                    this.getNavigation().moveTo(target, 0.9);
                }
            }
            case "flee" -> {
                Vec3 away = this.position().subtract(target.position())
                    .multiply(1, 0, 1).normalize().scale(24.0);
                this.getNavigation().moveTo(
                    this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.6);
            }
            case "hex" -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0f, 30.0f);
            }
        }
    }

    // ─── LLM integration ─────────────────────────────────────────────────────

    private void triggerLlmCall(ServerLevel sl, LivingEntity target) {
        float currentMyHp     = this.getHealth() / this.getMaxHealth();
        float currentPlayerHp = target.getHealth() / target.getMaxHealth();

        // Record outcome of the PREVIOUS decision before overwriting snapshot
        if (snapshotMyHp >= 0f) {
            HistoryEntry outcome = new HistoryEntry(
                this.llmBehavior,
                snapshotMyHp,     currentMyHp,
                snapshotPlayerHp, currentPlayerHp
            );
            fightHistory.addLast(outcome);
            if (fightHistory.size() > MAX_HISTORY) fightHistory.pollFirst();
        }

        // Snapshot HP at start of this new interval
        snapshotMyHp     = currentMyHp;
        snapshotPlayerHp = currentPlayerHp;

        String playerClass = "Peasant";
        if (target instanceof Player player) {
            var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
            playerClass = rpg.playerClass().id();
        }

        String finalClass     = playerClass;
        float  finalPlayerHp  = currentPlayerHp;
        float  finalMyHp      = currentMyHp;
        double dist           = this.distanceTo(target);
        List<HistoryEntry> historySnapshot = new ArrayList<>(fightHistory);

        this.llmCallInFlight = true;

        OllamaClient.requestCombatResponse(
            finalClass, finalPlayerHp, finalMyHp, dist,
            historySnapshot,
            response -> {
                this.llmBehavior     = response.behavior().toLowerCase();
                this.llmCallInFlight = false;

                if (!response.taunt().isBlank()) {
                    sl.getServer().execute(() -> {
                        if (this.isAlive()) broadcastTaunt(sl, response.taunt());
                    });
                }
            }
        );
    }

    // ─── HUD ─────────────────────────────────────────────────────────────────

    /**
     * Shows the current behavior as an action-bar message to all nearby players.
     * Uses color-coding so the mode is immediately legible at a glance:
     * <ul>
     *   <li>aggressive — §c red</li>
     *   <li>kite       — §e yellow</li>
     *   <li>flee       — §a green</li>
     *   <li>hex        — §5 dark purple</li>
     * </ul>
     */
    private void broadcastHud(ServerLevel sl) {
        String color = switch (this.llmBehavior) {
            case "aggressive" -> "§c";
            case "kite"       -> "§e";
            case "flee"       -> "§a";
            case "hex"        -> "§5";
            default           -> "§7";
        };
        String label = switch (this.llmBehavior) {
            case "aggressive" -> "⚔ AGGRESSIVE";
            case "kite"       -> "↔ KITING";
            case "flee"       -> "↑ FLEEING";
            case "hex"        -> "✦ HEX BARRAGE";
            default           -> "? THINKING";
        };

        String historyStr = buildHistoryHud();
        Component msg = Component.literal(
            "§8[Void Invoker] " + color + label + (historyStr.isEmpty() ? "" : "  §8" + historyStr));

        AABB range = new AABB(this.blockPosition()).inflate(HUD_RANGE);
        for (Player player : sl.getEntitiesOfClass(Player.class, range)) {
            if (player instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetActionBarTextPacket(msg));
        }
    }

    /** Compact history summary, e.g. "kite▲ aggressive▼ hex▲" for the action bar. */
    private String buildHistoryHud() {
        if (fightHistory.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (HistoryEntry e : fightHistory) {
            // Net outcome from the invoker's perspective: did I gain advantage?
            float myDelta     = e.myHpAfter()     - e.myHpBefore();
            float playerDelta = e.playerHpAfter()  - e.playerHpBefore();
            // Positive = good for invoker (player lost HP or I healed)
            float advantage = playerDelta - myDelta; // player losing HP is negative playerDelta
            // Actually: lower playerHp is good, so player losing HP = playerDelta negative = advantage positive
            String arrow = advantage > 0.02f ? "▲" : advantage < -0.02f ? "▼" : "─";
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(e.behavior(), 0, Math.min(3, e.behavior().length())).append(arrow);
        }
        return sb.toString();
    }

    // ─── Taunts ──────────────────────────────────────────────────────────────

    private static final double TAUNT_BROADCAST_RANGE = 20.0;

    private void broadcastTaunt(ServerLevel sl, String taunt) {
        Component msg = Component.literal("§5[Void Invoker] §d\"" + taunt + "\"");
        AABB range = new AABB(this.blockPosition()).inflate(TAUNT_BROADCAST_RANGE);
        for (Player player : sl.getEntitiesOfClass(Player.class, range)) {
            player.sendSystemMessage(msg);
        }
    }

    // ─── Spell selection ─────────────────────────────────────────────────────

    @Override
    protected WizardSpell pickNextSpellForCast() {
        int roll = this.random.nextInt(10);
        return switch (this.llmBehavior) {
            case "aggressive" ->
                roll < 5 ? WizardSpell.NOVA
              : roll < 8 ? WizardSpell.HEX
              :            WizardSpell.BLINK;
            case "kite" ->
                roll < 5 ? WizardSpell.HEX
              : roll < 8 ? WizardSpell.BLINK
              :            WizardSpell.NOVA;
            case "flee" ->
                roll < 9 ? WizardSpell.BLINK : WizardSpell.NOVA;
            case "hex" ->
                roll < 7 ? WizardSpell.HEX
              : roll < 9 ? WizardSpell.BLINK
              :            WizardSpell.NOVA;
            default -> super.pickNextSpellForCast();
        };
    }

    @Override
    protected int spellCooldownBaseTicks() {
        return switch (this.llmBehavior) {
            case "aggressive" -> 40;
            case "hex"        -> 30;
            case "flee"       -> 20;
            default           -> 50;
        };
    }
}
