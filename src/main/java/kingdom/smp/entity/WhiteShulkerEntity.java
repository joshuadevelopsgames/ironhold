package kingdom.smp.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * White Shulker — pacifist healer/priest caste. Does not attack. Every
 * {@link #HEAL_INTERVAL_TICKS} it opens a {@link #HEAL_WINDOW_TICKS}-long
 * heal pulse: cleanse + Resistance up front, then heal-per-tick + heal-beam
 * particles every tick of the window so the support is visible the whole
 * time it's happening, not as a one-frame flash.
 *
 * Teleports cluster the White into the bulk: if a vanilla on-hit or
 * lost-attachment teleport launches it away from a group, it reverts to
 * stay anchored. Solo Whites teleport per vanilla and try to gravitate
 * toward a buddy after landing.
 */
public class WhiteShulkerEntity extends Shulker {

    private static final int HEAL_INTERVAL_TICKS = 60;
    private static final int HEAL_WINDOW_TICKS = 20;
    private static final double HEAL_RADIUS = 8.0;
    /** Total = HEAL_PER_TICK * HEAL_WINDOW_TICKS = 2.0 HP per pulse. */
    private static final float HEAL_PER_TICK = 0.1F;
    private static final double TELEPORT_DETECT_THRESHOLD_SQ = 4.0;
    private static final double TELEPORT_REDIRECT_SEARCH_RADIUS = 24.0;
    /** Other shulkers within this radius of the pre-teleport position count as a "bulk". */
    private static final double BULK_RADIUS = 16.0;

    private int healPhase;
    private final List<LivingEntity> currentHealTargets = new ArrayList<>();

    public WhiteShulkerEntity(EntityType<? extends Shulker> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    /**
     * Copy the wall-attachment direction from the vanilla shulker we're
     * replacing, so the variant doesn't snap to a default face mid-spawn
     * and end up facing the wrong way (or floating away from the wall).
     */
    public void inheritAttachStateFrom(Shulker source) {
        this.entityData.set(DATA_ATTACH_FACE_ID, source.getAttachFace());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.ARMOR, 8.0);
    }

    @Override
    public void tick() {
        double oldX = getX();
        double oldY = getY();
        double oldZ = getZ();

        super.tick();

        if (level().isClientSide() || !(level() instanceof ServerLevel server)) {
            return;
        }

        double dx = getX() - oldX;
        double dy = getY() - oldY;
        double dz = getZ() - oldZ;
        if (dx * dx + dy * dy + dz * dz > TELEPORT_DETECT_THRESHOLD_SQ) {
            handleTeleport(server, oldX, oldY, oldZ);
        }

        if (healPhase > 0) {
            tickHealWindow(server);
        } else if (tickCount % HEAL_INTERVAL_TICKS == 0) {
            startHealWindow(server);
        }
    }

    private void startHealWindow(ServerLevel server) {
        AABB area = getBoundingBox().inflate(HEAL_RADIUS);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != this && e.isAlive() && isEndAlly(e));
        if (targets.isEmpty()) {
            return;
        }

        currentHealTargets.clear();
        currentHealTargets.addAll(targets);
        healPhase = HEAL_WINDOW_TICKS;

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, HEAL_WINDOW_TICKS, 0, true, false));
            cleanseHarmfulEffects(target);
        }

        server.sendParticles(ParticleTypes.HEART,
            getX(), getY() + 0.8, getZ(),
            6, 0.6, 0.4, 0.6, 0.0);
    }

    private void tickHealWindow(ServerLevel server) {
        healPhase--;
        for (LivingEntity target : currentHealTargets) {
            if (target == null || !target.isAlive()) continue;
            target.heal(HEAL_PER_TICK);
            drawHealBeam(server, target);
        }
        if (healPhase <= 0) {
            currentHealTargets.clear();
        }
    }

    private static void cleanseHarmfulEffects(LivingEntity target) {
        List<net.minecraft.core.Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().value().getCategory()
                == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                harmful.add(effect.getEffect());
            }
        }
        for (var effect : harmful) {
            target.removeEffect(effect);
        }
    }

    private void drawHealBeam(ServerLevel server, LivingEntity ally) {
        double sx = getX();
        double sy = getY() + 0.5;
        double sz = getZ();
        double tx = ally.getX();
        double ty = ally.getY() + ally.getBbHeight() * 0.5;
        double tz = ally.getZ();
        double length = Math.sqrt((tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) + (tz - sz) * (tz - sz));
        int steps = Math.max(2, (int) Math.ceil(length * 3));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = sx + (tx - sx) * t;
            double py = sy + (ty - sy) * t;
            double pz = sz + (tz - sz) * t;
            server.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Vanilla teleport just fired (we detected a position jump). If the White
     * was already in a bulk, refuse it and snap back to anchor with the group.
     * If it was alone, allow the vanilla teleport but try to gravitate toward
     * any buddy near the new position so the loner finds a group over time.
     */
    private void handleTeleport(ServerLevel server, double oldX, double oldY, double oldZ) {
        AABB origin = new AABB(
            oldX - BULK_RADIUS, oldY - BULK_RADIUS, oldZ - BULK_RADIUS,
            oldX + BULK_RADIUS, oldY + BULK_RADIUS, oldZ + BULK_RADIUS);
        boolean hadBuddiesAtOrigin = !server.getEntitiesOfClass(Shulker.class, origin,
            e -> e != this && e.isAlive()).isEmpty();
        if (hadBuddiesAtOrigin) {
            randomTeleport(oldX, oldY, oldZ, false);
        } else {
            redirectToNearbyShulker(server);
        }
    }

    private void redirectToNearbyShulker(ServerLevel server) {
        AABB area = getBoundingBox().inflate(TELEPORT_REDIRECT_SEARCH_RADIUS);
        List<Shulker> nearby = server.getEntitiesOfClass(Shulker.class, area,
            e -> e != this && e.isAlive());
        if (nearby.isEmpty()) {
            return;
        }
        Shulker anchor = nearby.get(getRandom().nextInt(nearby.size()));
        for (int attempt = 0; attempt < 8; attempt++) {
            double offX = (getRandom().nextDouble() - 0.5) * 6.0;
            double offY = (getRandom().nextDouble() - 0.5) * 4.0;
            double offZ = (getRandom().nextDouble() - 0.5) * 6.0;
            if (randomTeleport(
                anchor.getX() + offX,
                anchor.getY() + offY,
                anchor.getZ() + offZ,
                false)) {
                return;
            }
        }
    }

    private static boolean isEndAlly(LivingEntity entity) {
        return entity instanceof Shulker
            || entity instanceof EnderMan
            || entity instanceof Endermite
            || entity instanceof EnderDragon;
    }
}
