package kingdom.smp.rpg.ability;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game-bus listeners that implement the Iron Ward "Marked" damage bonus, the Guardian's Vow
 * damage redirect, and the periodic maintenance ticks for both abilities (target re-assertion,
 * link distance check, link-particle visuals, auto-break HP threshold).
 */
public final class AbilityEffects {
    private AbilityEffects() {}

    /** How often (ticks) we re-assert taunt targets and redraw link particles. */
    private static final int MAINTENANCE_INTERVAL = 5;

    // ──────────────────────────────────────────────────────────────────────────
    // Damage event — order matters
    //   HIGH:    Guardian's Vow redirects part of the damage to the Knight before reductions
    //   NORMAL:  Iron Ward's Mark applies its +15% multiplier when an ally hits a marked mob
    // ──────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamageVow(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Player ally) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        GuardianVowData vow = ally.getData(ModAttachments.GUARDIAN_VOW.get());
        if (vow == null || vow.knightUUID() == null || vow.knightUUID().getMostSignificantBits() == 0L) {
            return;
        }
        long now = level.getGameTime();
        if (!vow.isActive(now)) {
            return;
        }
        // Redirected damage cannot itself trigger redirect, and self-damage on the ally is not redirected.
        DamageSource src = event.getSource();
        if (src.is(net.minecraft.world.damagesource.DamageTypes.FALL)
            || src.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
            || src.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
            || src.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
            || src.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
            || src.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
            return;
        }
        Player knight = level.getPlayerByUUID(vow.knightUUID());
        if (!(knight instanceof ServerPlayer sk) || !sk.isAlive()) {
            return;
        }
        // Linked ally PVPs the Knight → break instantly.
        if (src.getEntity() != null && src.getEntity().is(sk)) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            return;
        }
        if (!GuardiansVow.inRange(ally, sk)) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            return;
        }

        float incoming = event.getAmount();
        float redirectFrac = vow.pvpMode() ? GuardiansVow.REDIRECT_PVP : GuardiansVow.REDIRECT_PVE;
        float redirected = incoming * redirectFrac;
        if (redirected <= 0F) {
            return;
        }
        // Save the ally → Knight eats the share, mitigated by his own armor/resistance via hurt().
        event.setAmount(incoming - redirected);
        sk.hurtServer((ServerLevel) sk.level(), src, redirected);

        ((ServerLevel) sk.level()).sendParticles(ParticleTypes.CRIT,
            sk.getX(), sk.getY() + 1.0, sk.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        ((ServerLevel) ally.level()).sendParticles(ParticleTypes.ENCHANT,
            ally.getX(), ally.getY() + 1.0, ally.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
    }

    @SubscribeEvent
    public static void onIncomingDamageMark(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(victim instanceof Mob mob)) {
            return;
        }
        TauntMarkData mark = mob.getData(ModAttachments.TAUNT_MARK.get());
        if (mark == null || mark.kingdom() < 0) {
            return;
        }
        long now = level.getGameTime();
        if (!mark.isMarked(now)) {
            mob.removeData(ModAttachments.TAUNT_MARK.get());
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        int attackerKingdom;
        try {
            attackerKingdom = attacker.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped();
        } catch (Throwable t) {
            return;
        }
        if (attackerKingdom != mark.kingdom()) {
            return;
        }
        event.setAmount(event.getAmount() * IronWard.MARK_DAMAGE_MULT);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Server-tick maintenance
    // ──────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server == null) return;
        long tick = server.getTickCount();
        if (tick % MAINTENANCE_INTERVAL != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    /** Half-extent of the per-player scan box used to find marked mobs nearby (must enclose IronWard.RANGE plus drift). */
    private static final double MARK_SCAN_RADIUS = IronWard.RANGE + 8.0;

    private static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();

        // Iterate around each player rather than the whole world. The world-border AABB scans the
        // full vertical extent, which overflows the entity section storage's long-keyed range
        // and crashes the server. A bounded per-player AABB sidesteps that and is also cheaper.
        for (ServerPlayer player : level.players()) {
            net.minecraft.world.phys.AABB scan = player.getBoundingBox().inflate(MARK_SCAN_RADIUS);
            // Defensive: a player whose position has NaN/infinity (or has been
            // teleported far past the world border by a buggy ride/mount)
            // would build an AABB that overflows section-key encoding inside
            // EntitySectionStorage. Skip them rather than crash the server.
            if (!isFiniteAabb(scan)) continue;
            for (Mob mob : level.getEntitiesOfClass(Mob.class, scan)) {
                TauntMarkData mark = mob.getData(ModAttachments.TAUNT_MARK.get());
                if (mark == null || mark.kingdom() < 0) continue;
                if (!mark.isMarked(now)) {
                    mob.removeData(ModAttachments.TAUNT_MARK.get());
                    continue;
                }
                if (mark.isForcedTarget(now)) {
                    Player taunter = level.getPlayerByUUID(mark.tauntPlayer());
                    if (taunter != null && taunter.isAlive()
                        && mob.distanceToSqr(taunter) <= IronWard.RANGE * IronWard.RANGE * 4.0) {
                        if (mob.getTarget() != taunter) {
                            mob.setTarget((LivingEntity) taunter);
                        }
                    }
                }
                // Periodic visual so allies can see who is marked. Once per mob per maintenance tick
                // is enough; multi-player overlap just emits an extra particle, which is harmless.
                if (now % 10 == 0) {
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            // Maintain Guardian's Vow links — distance, autobreak, particle line.
            tickPlayerVow(player);
        }
    }

    /**
     * True iff every coordinate of the AABB is finite and within Minecraft's
     * representable section range. Catches NaN/infinity and the
     * "player teleported to ±2³¹ blocks" case that would otherwise overflow
     * section-key encoding in EntitySectionStorage.
     */
    private static boolean isFiniteAabb(net.minecraft.world.phys.AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
            && Math.abs(box.minX) < 30_000_000.0 && Math.abs(box.maxX) < 30_000_000.0
            && Math.abs(box.minZ) < 30_000_000.0 && Math.abs(box.maxZ) < 30_000_000.0;
    }

    /**
     * Maintain a player's incoming Guardian's Vow (if any) — checks expiry, link distance,
     * Knight HP threshold, and emits the silver chain particle line.
     */
    public static void tickPlayerVow(ServerPlayer ally) {
        ServerLevel level = (ServerLevel) ally.level();
        GuardianVowData vow = ally.getData(ModAttachments.GUARDIAN_VOW.get());
        if (vow == null || vow.knightUUID() == null || vow.knightUUID().getMostSignificantBits() == 0L) {
            return;
        }
        long now = level.getGameTime();
        if (!vow.isActive(now)) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            return;
        }
        Player knight = level.getPlayerByUUID(vow.knightUUID());
        if (!(knight instanceof ServerPlayer sk) || !sk.isAlive() || sk.level() != level) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            return;
        }
        if (!GuardiansVow.inRange(ally, sk)) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            return;
        }
        if (sk.getHealth() / Math.max(0.01F, sk.getMaxHealth()) < GuardiansVow.AUTOBREAK_HP_FRACTION) {
            ally.removeData(ModAttachments.GUARDIAN_VOW.get());
            level.playSound(null, sk.getX(), sk.getY(), sk.getZ(),
                net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
            return;
        }

        // Silver chain particle line — small set of points along the link, every maintenance tick.
        double dx = ally.getX() - sk.getX();
        double dy = (ally.getY() + 1.0) - (sk.getY() + 1.0);
        double dz = ally.getZ() - sk.getZ();
        int n = 6;
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            level.sendParticles(ParticleTypes.END_ROD,
                sk.getX() + dx * t, sk.getY() + 1.0 + dy * t, sk.getZ() + dz * t,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static String modid() { return Ironhold.MODID; }
}
