package kingdom.smp.game;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Applies Minecraft attribute modifiers derived from a class's 0–100 design stats.
 * <p>
 * Conversion from design values to attribute modifiers:
 * <ul>
 *   <li>Health 50 = baseline (20 HP). Each point above/below adds/removes 0.4 HP (±20 HP range).</li>
 *   <li>Defense 50 = baseline (0 armor). Each point above adds 0.16 armor toughness (max +8).</li>
 *   <li>Attack Damage 50 = baseline (+0). Each point above/below adds/removes 0.1 damage (±5 range).</li>
 *   <li>Attack Speed 50 = baseline. Each point above/below adds/removes 0.4% attack speed.</li>
 *   <li>Speed 50 = baseline. Each point above/below adds/removes 0.6% movement speed.</li>
 *   <li>Luck — applied as Minecraft's luck attribute (affects loot tables). Scaled 0–2.</li>
 *   <li>Mana — not an MC attribute; handled by the ability/mana system separately.</li>
 * </ul>
 * Per-tier scaling: every 5 class levels adds a small bonus to all stats.
 */
public final class ClassStatHandler {
    private static final Identifier HEALTH_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_max_health");
    private static final Identifier ATTACK_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_attack_damage");
    private static final Identifier MOVE_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_movement_speed");
    private static final Identifier ATK_SPEED_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_attack_speed");
    private static final Identifier ARMOR_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_armor_toughness");
    private static final Identifier LUCK_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_luck");

    /** Per-tier (every 5 class levels) bonus multiplier. */
    private static final double TIER_HEALTH = 2.0;
    private static final double TIER_ATTACK = 0.25;
    private static final double TIER_MOVE = 0.01;

    private ClassStatHandler() {}

    /**
     * Players whose class stats need re-application this tick. The per-player-
     * tick handler in IronholdGameEvents calls {@link #apply} every tick; we
     * only do the expensive attribute-modifier mutations when this set
     * contains the player. Mark dirty whenever class, level, or any stat
     * affecting modifier inputs changes.
     */
    private static final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    /** Mark this player as needing class stats re-applied on the next tick. */
    public static void markDirty(java.util.UUID playerId) {
        dirty.add(playerId);
    }

    public static void markDirty(ServerPlayer player) {
        dirty.add(player.getUUID());
    }

    /**
     * Called by the per-player-tick handler. Cheap if the player isn't dirty —
     * only does the attribute mutations when class/level changed since the
     * last apply. Mutations call {@link #apply} directly instead.
     */
    public static void applyIfDirty(ServerPlayer player, PlayerKingdomRpgData rpg) {
        if (!dirty.remove(player.getUUID())) return;
        apply(player, rpg);
    }

    public static void apply(ServerPlayer player, PlayerKingdomRpgData rpg) {
        // Always-apply path. Called explicitly from class/level mutation
        // sites (login, level up, promotion). The per-tick handler calls
        // applyIfDirty, which short-circuits unless the player has been
        // marked dirty. apply() also clears the dirty flag for symmetry.
        dirty.remove(player.getUUID());

        PlayerClass c = rpg.playerClass();
        int levelTier = RpgProgression.classTier(rpg.classLevel());

        // ── Health: baseline 20 HP, ±0.4 per point from 50 ────────
        applyMod(player, Attributes.MAX_HEALTH, HEALTH_MOD,
            (c.statHealth() - 50) * 0.4 + TIER_HEALTH * levelTier,
            AttributeModifier.Operation.ADD_VALUE);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));

        // ── Attack Damage: ±0.1 per point from 50 (floor at -0.5 so fists always work) ─
        applyMod(player, Attributes.ATTACK_DAMAGE, ATTACK_MOD,
            Math.max(-0.5, (c.statAttackDamage() - 50) * 0.1 + TIER_ATTACK * levelTier),
            AttributeModifier.Operation.ADD_VALUE);

        // ── Movement Speed: ±0.6% per point from 50 ──────────────
        applyMod(player, Attributes.MOVEMENT_SPEED, MOVE_MOD,
            (c.statSpeed() - 50) * 0.006 + TIER_MOVE * levelTier,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // ── Attack Speed: ±0.4% per point from 50 ────────────────
        applyMod(player, Attributes.ATTACK_SPEED, ATK_SPEED_MOD,
            (c.statAttackSpeed() - 50) * 0.004,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // ── Armor Toughness (Defense): +0.16 per point above 50 ──
        applyMod(player, Attributes.ARMOR_TOUGHNESS, ARMOR_MOD,
            Math.max(0, (c.statDefense() - 50) * 0.16),
            AttributeModifier.Operation.ADD_VALUE);

        // ── Luck: scaled 0–2 from 0–100 ──────────────────────────
        applyMod(player, Attributes.LUCK, LUCK_MOD,
            c.statLuck() * 0.02,
            AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyMod(ServerPlayer player,
                                  net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                  Identifier id, double value,
                                  AttributeModifier.Operation op) {
        var inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0.0) {
            inst.addTransientModifier(new AttributeModifier(id, value, op));
        }
    }
}
