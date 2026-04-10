package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Siren — a humanoid ocean mob that floats near the surface,
 * singing to lure players. Players within range get slowly pulled toward
 * the siren. Once close, it attacks with melee.
 * Drowns (takes damage) when outside of water.
 */
public class SirenEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_SINGING =
        SynchedEntityData.defineId(SirenEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double LURE_RANGE = 20.0;
    private static final double LURE_STRENGTH = 0.055;
    private int singCooldown = 0;
    private int outOfWaterTicks = 0;

    public SirenEntity(EntityType<? extends SirenEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
        this.xpReward = 15;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SINGING, false);
    }

    public boolean isSinging() {
        return this.entityData.get(DATA_SINGING);
    }

    public void setSinging(boolean singing) {
        this.entityData.set(DATA_SINGING, singing);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 35.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 28.0)
            .add(Attributes.ARMOR, 4.0);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // ── Goals ────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SirenLureGoal(this));
        this.goalSelector.addGoal(2, new SirenApproachGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8, 20));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (singCooldown > 0) singCooldown--;

        // Drown when out of water
        if (!this.level().isClientSide()) {
            if (!this.isInWater()) {
                outOfWaterTicks++;
                if (outOfWaterTicks > 40) { // 2 seconds grace period
                    this.hurt(this.damageSources().drown(), 2.0F);
                }
            } else {
                outOfWaterTicks = 0;
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel sl) {
            // Lure nearby players when singing
            if (isSinging()) {
                List<Player> nearby = sl.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(LURE_RANGE),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator());

                for (Player player : nearby) {
                    double dist = this.distanceTo(player);
                    if (dist > 3.0 && dist < LURE_RANGE) {
                        Vec3 pull = this.position().subtract(player.position()).normalize().scale(LURE_STRENGTH);
                        player.setDeltaMovement(player.getDeltaMovement().add(pull));
                        player.hurtMarked = true;

                        if (this.tickCount % 20 == 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
                        }
                    }
                }

                // Singing particles
                if (this.tickCount % 4 == 0) {
                    sl.sendParticles(ParticleTypes.NOTE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.5,
                        this.getEyeY() + 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.5,
                        1, 0, 0.1, 0, 0);
                    sl.sendParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getEyeY(), this.getZ(),
                        3, 0.5, 0.3, 0.5, 0.1);
                }
            }
        }

        // Client-side ambient particles
        if (this.level().isClientSide() && this.isInWater() && this.tickCount % 8 == 0) {
            this.level().addParticle(ParticleTypes.BUBBLE,
                this.getRandomX(0.5), this.getY() + 0.8, this.getRandomZ(0.5),
                0, 0.05, 0);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isInWater()) {
            this.moveRelative(0.04F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public int getAirSupply() {
        // Never show bubble bar — siren breathes water, not air
        return this.getMaxAirSupply();
    }

    // ── Stop luring when hit ────────────────────────────────────────────────

    @Override
    protected void actuallyHurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        super.actuallyHurt(level, source, amount);
        // Cancel singing and go on cooldown so it fights instead of luring
        if (isSinging()) {
            setSinging(false);
        }
        singCooldown = 100; // 5 seconds before it can sing again
    }

    // ── Sounds ───────────────────────────────────────────────────────────────

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    // ── Approach Goal — swim toward player ───────────────────────────────────

    private static class SirenApproachGoal extends Goal {
        private final SirenEntity siren;
        private Player target;

        SirenApproachGoal(SirenEntity siren) {
            this.siren = siren;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            target = siren.level().getNearestPlayer(siren, LURE_RANGE);
            return target != null && siren.isInWater() && siren.distanceTo(target) > 3.0;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && siren.isInWater() && siren.distanceTo(target) > 2.0;
        }

        @Override
        public void tick() {
            if (target == null) return;
            // Face toward the player
            siren.getLookControl().setLookAt(target);
            double dx = target.getX() - siren.getX();
            double dz = target.getZ() - siren.getZ();
            float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            siren.setYRot(targetYaw);
            siren.yBodyRot = targetYaw;

            // Swim toward the player
            Vec3 dir = target.position().subtract(siren.position()).normalize();
            double speed = 0.035;
            siren.setDeltaMovement(siren.getDeltaMovement().add(dir.x * speed, dir.y * speed * 0.5, dir.z * speed));
        }

        @Override
        public void stop() {
            target = null;
        }
    }

    // ── Lure Goal ────────────────────────────────────────────────────────────

    private static class SirenLureGoal extends Goal {
        private final SirenEntity siren;
        private int singDuration;

        SirenLureGoal(SirenEntity siren) {
            this.siren = siren;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (siren.singCooldown > 0) return false;
            Player nearest = siren.level().getNearestPlayer(siren, LURE_RANGE);
            return nearest != null && siren.distanceTo(nearest) > 5.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player nearest = siren.level().getNearestPlayer(siren, LURE_RANGE);
            return nearest != null && singDuration < 100 && siren.distanceTo(nearest) > 3.0;
        }

        @Override
        public void start() {
            siren.setSinging(true);
            singDuration = 0;
            siren.level().playSound(null, siren.blockPosition(),
                SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, siren.getSoundSource(), 1.5F, 0.6F);
        }

        @Override
        public void tick() {
            singDuration++;
            Player nearest = siren.level().getNearestPlayer(siren, LURE_RANGE);
            if (nearest != null) {
                siren.getLookControl().setLookAt(nearest);
            }
            if (singDuration % 30 == 0) {
                siren.level().playSound(null, siren.blockPosition(),
                    SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, siren.getSoundSource(), 1.2F, 0.5F + siren.random.nextFloat() * 0.3F);
            }
        }

        @Override
        public void stop() {
            siren.setSinging(false);
            siren.singCooldown = 60;
        }
    }
}
