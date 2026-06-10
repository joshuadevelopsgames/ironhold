package kingdom.smp.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kingdom.smp.ModAttachments;
import kingdom.smp.item.BattleHammerItem;
import kingdom.smp.rpg.ability.AbilityCooldowns;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Server-side footwork layer for the two tap-key defensive actions (no stamina, no sustained block):
 * <ul>
 *   <li><b>Parry</b> — opens a short timed window; a hit landing inside it is negated, the attacker is
 *       staggered, parried projectiles are reflected, and the player's ability cooldowns are refunded.</li>
 *   <li><b>Dodge</b> — a short omnidirectional hop with <i>perfect-dodge</i> i-frames: invulnerable only
 *       if a hit lands within the first few ticks; a late dodge just repositions.</li>
 * </ul>
 *
 * Cooldowns live in the {@link AbilityCooldowns} attachment (reset on death, synced) under reserved ids.
 * The active windows are sub-second and live in transient server-only maps. Registered to the game event
 * bus in {@code Ironhold}.
 *
 * <p>Spec: {@code specs/fantasia-ports/01-combat-actions.md}. No camera shake (per design).
 */
public final class FootworkHandler {
    private FootworkHandler() {}

    /** Reserved cooldown ids in the shared {@link AbilityCooldowns} map. */
    public static final String PARRY_CD = "ironhold:parry";
    public static final String DODGE_CD = "ironhold:dodge";

    // ── Tunables (ticks; v1 targets — see spec §3/§4) ───────────────────────────
    private static final int PARRY_WINDOW_DEFAULT = 5;
    private static final int PARRY_WINDOW_HAMMER = 4; // tighter window, bigger stagger
    // Asymmetric cost makes parry a COMMITMENT, not a spammable shield:
    private static final int PARRY_WHIFF_COOLDOWN = 24;  // every press → ~1.2s lockout (blind spam is punished)
    private static final int PARRY_SUCCESS_COOLDOWN = 8;  // a clean parry shortens it → ~0.4s (good timing flows)
    private static final int PARRY_STAGGER_TICKS = 30;
    private static final int PARRY_STAGGER_TICKS_HAMMER = 50;
    private static final int COOLDOWN_REFUND = 40;    // shaved off ability cooldowns on a clean parry
    private static final int DODGE_IFRAME = 5;        // perfect-dodge window
    private static final int DODGE_COOLDOWN = 30;     // ~1.5s flat
    private static final double DODGE_POWER = 0.55;   // hop strength
    private static final double DODGE_HOP_Y = 0.18;

    /** player UUID → game-tick the parry window closes (transient, server-only). */
    private static final Map<UUID, Long> PARRY_WINDOW = new ConcurrentHashMap<>();
    /** player UUID → game-tick the dodge i-frame window closes (transient, server-only). */
    private static final Map<UUID, Long> DODGE_IFRAME_UNTIL = new ConcurrentHashMap<>();

    // ── Entry points (called from ModNetworking handlers) ───────────────────────

    /** Open the parry window if off cooldown. */
    public static void tryParry(ServerPlayer player) {
        long now = player.level().getGameTime();
        AbilityCooldowns cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get());
        if (cds.isOnCooldown(PARRY_CD, now)) return;

        boolean hammer = player.getMainHandItem().getItem() instanceof BattleHammerItem;
        int window = hammer ? PARRY_WINDOW_HAMMER : PARRY_WINDOW_DEFAULT;
        PARRY_WINDOW.put(player.getUUID(), now + window);
        // Commit: every press starts the long WHIFF lockout up front. A successful parry shortens it
        // (see onParrySuccess); a whiff leaves the player locked out and exposed to follow-up hits.
        player.setData(ModAttachments.ABILITY_COOLDOWNS.get(),
            cds.withCooldown(PARRY_CD, now + PARRY_WHIFF_COOLDOWN));

        player.level().playSound(null, player.blockPosition(),
            SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.4F, 1.7F);
    }

    /** Perform a short directional hop if off cooldown, arming the perfect-dodge window. */
    public static void tryDodge(ServerPlayer player, boolean fwd, boolean back, boolean left, boolean right) {
        long now = player.level().getGameTime();
        AbilityCooldowns cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get());
        if (cds.isOnCooldown(DODGE_CD, now)) return;

        Vec3 dir = hopDirection(player, fwd, back, left, right);
        Vec3 cur = player.getDeltaMovement();
        player.setDeltaMovement(dir.x * DODGE_POWER, Math.max(cur.y, DODGE_HOP_Y), dir.z * DODGE_POWER);
        player.hurtMarked = true;     // force-sync the velocity to the client
        player.fallDistance = 0;      // hop shouldn't accrue fall damage

        DODGE_IFRAME_UNTIL.put(player.getUUID(), now + DODGE_IFRAME);
        player.setData(ModAttachments.ABILITY_COOLDOWNS.get(), cds.withCooldown(DODGE_CD, now + DODGE_COOLDOWN));

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                6, 0.2, 0.0, 0.2, 0.01);
        }
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.7F);
    }

    // ── Damage hook ─────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        UUID id = player.getUUID();

        // Perfect-dodge i-frames: full negate if a hit lands inside the window.
        Long iframe = DODGE_IFRAME_UNTIL.get(id);
        if (iframe != null && now <= iframe) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            DODGE_IFRAME_UNTIL.remove(id);
            onPerfectDodge(player);
            return;
        }

        // Parry: a melee or projectile hit inside the window is negated + punished.
        Long window = PARRY_WINDOW.get(id);
        if (window != null && now <= window) {
            DamageSource src = event.getSource();
            Entity direct = src.getDirectEntity();
            boolean parryable = direct instanceof LivingEntity || direct instanceof Projectile;
            if (parryable) {
                event.setAmount(0.0F);
                event.setCanceled(true);
                PARRY_WINDOW.remove(id); // consume on success
                onParrySuccess(player, src);
            }
        }
    }

    // ── Effects ─────────────────────────────────────────────────────────────────

    private static void onParrySuccess(ServerPlayer player, DamageSource src) {
        long now = player.level().getGameTime();
        boolean hammer = player.getMainHandItem().getItem() instanceof BattleHammerItem;

        // Refund ability cooldowns AND replace the long whiff lockout (set on press) with a short
        // recovery — so a well-timed parry flows, while a blind/early press still eats the full
        // ~1.2s whiff cooldown. This is what makes parry tactical instead of spammable.
        AbilityCooldowns cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get())
            .withAllReducedBy(COOLDOWN_REFUND, now)
            .withCooldown(PARRY_CD, now + PARRY_SUCCESS_COOLDOWN);
        player.setData(ModAttachments.ABILITY_COOLDOWNS.get(), cds);

        Entity direct = src.getDirectEntity();
        Entity shooter = src.getEntity();

        // Reflect a parried projectile back at its source (or straight back).
        if (direct instanceof Projectile proj) {
            Vec3 back = proj.getDeltaMovement().scale(-1.0);
            double speed = Math.max(back.length(), 0.6);
            if (shooter != null) {
                back = shooter.getEyePosition().subtract(proj.position()).normalize().scale(speed);
            }
            proj.setDeltaMovement(back);
            proj.hurtMarked = true; // force the velocity change to sync to clients
            proj.setOwner(player); // the parrier now owns the deflected shot
        }

        // Stagger the attacker: slow + kill momentum.
        if (shooter instanceof LivingEntity le) {
            int stagger = hammer ? PARRY_STAGGER_TICKS_HAMMER : PARRY_STAGGER_TICKS;
            le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stagger, 2));
            le.setDeltaMovement(le.getDeltaMovement().scale(0.1));
            le.hurtMarked = true;
        }

        // Feedback — particle ring + clink. No camera shake (per design).
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(),
                14, 0.4, 0.4, 0.4, 0.2);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.3, 0.3, 0.3, 0.1);
        }
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35F, 1.9F);
    }

    private static void onPerfectDodge(ServerPlayer player) {
        // Small reward for precise timing (mirrors the timed parry).
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0));
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                8, 0.3, 0.0, 0.3, 0.02);
        }
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 2.0F);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Hop direction from movement input, relative to facing. Neutral input = backhop. */
    private static Vec3 hopDirection(ServerPlayer player, boolean fwd, boolean back, boolean left, boolean right) {
        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 strafeRight = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 v = Vec3.ZERO;
        if (fwd) v = v.add(forward);
        if (back) v = v.subtract(forward);
        if (right) v = v.add(strafeRight);
        if (left) v = v.subtract(strafeRight);
        if (v.lengthSqr() < 1.0E-4) {
            v = forward.scale(-1.0); // no directional input → hop backward
        }
        return v.normalize();
    }
}
