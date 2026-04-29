package kingdom.smp.item;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ArcaneOrbEntity;
import kingdom.smp.entity.SpellBeamEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.client.entity.ArcaneScepterRenderer;
import java.util.function.Consumer;

/**
 * Arcane Scepter — a wizard weapon that shoots arcane orbs.
 *
 * <p>Quick release (under 30 ticks): fires a straight orb.
 * <p>Full charge (30+ ticks): fires a homing orb at the targeted entity.
 * While charging, the targeted entity gets purple particle highlights.
 */
public class ArcaneScepterItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int MIN_CHARGE_TICKS = 10;
    private static final int FULL_CHARGE_TICKS = 30;
    private static final int COOLDOWN_TICKS = 20;
    private static final double TARGET_RANGE = 32.0;

    /** The last entity the crosshair was on during a full charge. Stored per-player on server. */
    private static final java.util.Map<java.util.UUID, Integer> lockedTargets = new java.util.HashMap<>();

    public ArcaneScepterItem(Properties props) {
        super(props);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.3F, 2.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int charged = getUseDuration(stack, entity) - remainingUseDuration;
        if (charged < 5) return;

        if (level.isClientSide()) {
            Vec3 look = entity.getLookAngle();
            float yawRad = (float) Math.toRadians(entity.getYRot());
            double rx = -Math.cos(yawRad) * 0.35;
            double rz = -Math.sin(yawRad) * 0.35;
            Vec3 tip = entity.position()
                .add(rx, entity.getBbHeight() * 0.75, rz)
                .add(look.scale(0.5));

            // Staff tip particles
            if (charged % 3 == 0) {
                level.addParticle(ParticleTypes.WITCH,
                    tip.x + (Math.random() - 0.5) * 0.2,
                    tip.y + Math.random() * 0.2,
                    tip.z + (Math.random() - 0.5) * 0.2,
                    0, 0.02, 0);
            }
            if (charged > 12 && charged % 5 == 0) {
                level.addParticle(ParticleTypes.ENCHANT,
                    tip.x + (Math.random() - 0.5) * 0.3,
                    tip.y + Math.random() * 0.3,
                    tip.z + (Math.random() - 0.5) * 0.3,
                    0, -0.3, 0);
            }

            // Target highlighting — show purple particles on the entity we're aiming at
            if (charged >= FULL_CHARGE_TICKS) {
                Entity target = findTarget(entity);
                if (target != null) {
                    // Purple particles orbiting the target
                    double angle = entity.tickCount * 0.15;
                    double r = target.getBbWidth() * 0.8 + 0.3;
                    for (int i = 0; i < 3; i++) {
                        double a = angle + i * (Math.PI * 2 / 3);
                        double px = target.getX() + Math.cos(a) * r;
                        double py = target.getY() + target.getBbHeight() * 0.5;
                        double pz = target.getZ() + Math.sin(a) * r;
                        level.addParticle(ParticleTypes.WITCH, px, py, pz, 0, 0.02, 0);
                    }
                    // Glow at target's feet
                    if (charged % 4 == 0) {
                        level.addParticle(ParticleTypes.REVERSE_PORTAL,
                            target.getX() + (Math.random() - 0.5) * 0.5,
                            target.getY() + 0.1,
                            target.getZ() + (Math.random() - 0.5) * 0.5,
                            0, 0.05, 0);
                    }
                }
            }
        }

        // Server side: lock target while fully charged and crosshair is on an entity
        if (!level.isClientSide() && charged >= FULL_CHARGE_TICKS) {
            Entity target = findTarget(entity);
            if (target != null) {
                lockedTargets.put(entity.getUUID(), target.getId());
            }
        }

        // Charging hum
        if (!level.isClientSide() && charged % 10 == 0 && charged <= 30) {
            level.playSound(null, entity.blockPosition(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.15F, 1.8F + charged * 0.02F);
        }
        // Full charge sound cue
        if (!level.isClientSide() && charged == FULL_CHARGE_TICKS) {
            level.playSound(null, entity.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 2.0F);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int chargeTime = this.getUseDuration(stack, entity) - timeLeft;
        if (chargeTime < MIN_CHARGE_TICKS) {
            // Quick tap: instant beam attack
            if (!level.isClientSide()) {
                fireBeam(player, (ServerLevel) level, stack);
            }
            player.swing(player.getUsedItemHand());
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS / 2);
            return true;
        }

        if (!level.isClientSide()) {
            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = player.getEyePosition().add(look.scale(1.5));

            // Retrieve the locked-on target from charging
            Entity homingTarget = null;
            if (chargeTime >= FULL_CHARGE_TICKS) {
                Integer targetId = lockedTargets.remove(player.getUUID());
                if (targetId != null) {
                    Entity e = level.getEntity(targetId);
                    if (e != null && e.isAlive()) {
                        homingTarget = e;
                    }
                }
            } else {
                lockedTargets.remove(player.getUUID());
            }

            ArcaneOrbEntity orb = new ArcaneOrbEntity(
                player, homingTarget, look.scale(0.5), level);
            orb.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            level.addFreshEntity(orb);

            if (homingTarget != null) {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 0.8F, 1.5F);
            } else {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.2F);
            }

            stack.hurtAndBreak(1, player, player.getUsedItemHand());
        }

        player.swing(player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return true;
    }

    private static void fireBeam(Player player, ServerLevel level, ItemStack stack) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look    = player.getLookAngle();
        double range = 32.0;
        Vec3 endPos  = eyePos.add(look.scale(range));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player, eyePos, endPos, searchBox,
            e -> !e.isSpectator() && e.isPickable() && e != player
                && (e instanceof LivingEntity
                    || e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal
                    || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart),
            range * range);

        Vec3 impactPos;
        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part) {
                target = part.parentMob;
            }
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), 5.0f);
            impactPos = entityHit.getLocation();
            level.playSound(null, target.blockPosition(),
                SoundEvents.ILLUSIONER_HURT, SoundSource.PLAYERS, 0.8F, 1.6F);
        } else {
            BlockHitResult blockHit = level.clip(
                new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            impactPos = blockHit.getLocation();
            level.playSound(null, player.blockPosition(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, 1.4F);
        }

        Vec3 origin = eyePos.add(look.scale(0.8));
        SpellBeamEntity beam = SpellBeamEntity.create(level,
            impactPos.x, impactPos.y, impactPos.z,
            origin.x, origin.y, origin.z,
            0x8844FF, 15);
        level.addFreshEntity(beam);

        stack.hurtAndBreak(1, player, player.getUsedItemHand());
    }

    /**
     * Raycast from the entity's eyes to find a targetable entity under the crosshair.
     * Supports LivingEntity (mobs, players, bosses) and EndCrystal.
     */
    private static Entity findTarget(LivingEntity user) {
        Vec3 eyePos = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eyePos.add(look.scale(TARGET_RANGE));

        AABB searchBox = user.getBoundingBox().expandTowards(look.scale(TARGET_RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            user, eyePos, end, searchBox,
            e -> !e.isSpectator() && e.isPickable() && e != user
                && (e instanceof LivingEntity
                    || e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal
                    || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart),
            TARGET_RANGE * TARGET_RANGE);

        if (hit == null) return null;
        Entity target = hit.getEntity();
        // EnderDragonPart → resolve to the actual dragon
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part) {
            return part.parentMob;
        }
        return target;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ArcaneScepterRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new ArcaneScepterRenderer();
                return renderer;
            }
        });
    }
}
