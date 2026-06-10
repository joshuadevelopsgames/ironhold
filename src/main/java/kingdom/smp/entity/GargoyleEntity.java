package kingdom.smp.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Gargoyle — a hostile, winged stone creature that flies and swoops at the player like a
 * {@link net.minecraft.world.entity.monster.Vex Vex}, <em>except</em> it cannot pass through
 * walls. A vanilla Vex phases through blocks (it sets {@code noPhysics = true} around its tick);
 * this entity never touches {@code noPhysics}, so its collision stays solid. To still behave well
 * around terrain it uses a {@link FlyingMoveControl} + {@link FlyingPathNavigation} (the Bee/Allay
 * flight stack) so it pathfinds <em>around</em> obstacles, and only commits to a straight Vex-style
 * charge when it has a clear line of sight to its target.
 *
 * <p>Rendered via GeckoLib on the {@code gargoyle} rig (wings flap on the {@code fly} loop; a
 * triggered {@code charge} one-shot plays on each swoop).
 */
public class GargoyleEntity extends Monster implements GeoEntity {

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlay("charge");

    /** Whether the gargoyle is mid-swoop — synced so the client can bias the flap/charge pose. */
    private static final EntityDataAccessor<Boolean> CHARGING =
        SynchedEntityData.defineId(GargoyleEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GargoyleEntity(EntityType<? extends GargoyleEntity> type, Level level) {
        super(type, level);
        // Bee/Allay-style hovering flight control: never enables noPhysics, so the gargoyle
        // collides with blocks instead of phasing through them like a Vex.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0); // stone hide
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    public void setCharging(boolean charging) {
        this.entityData.set(CHARGING, charging);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new GargoyleAttackGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // Flight is driven entirely by the FlyingMoveControl; keep gravity off so the gargoyle
        // hovers instead of sinking when idle. (This mirrors the Vex, which also disables gravity —
        // the only thing we intentionally do NOT copy is the Vex's noPhysics wall-phasing.)
        this.setNoGravity(true);
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, this::movementPredicate));
        controllers.add(new AnimationController<GargoyleEntity>("action", 2, state -> PlayState.STOP)
            .triggerableAnim("charge", CHARGE));
    }

    private PlayState movementPredicate(AnimationTest<GargoyleEntity> test) {
        // A flying creature: wings beat whenever it's aloft or moving; only the rare grounded,
        // motionless moment falls back to the perched idle.
        if (this.isCharging() || test.isMoving() || !this.onGround()) {
            test.controller().setAnimation(FLY);
        } else {
            test.controller().setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ── Attack AI ───────────────────────────────────────────────────────────────
    // Vex-flavoured swoop: dash straight at the target when the line is clear, otherwise pathfind
    // around the wall to regain the angle. Because noPhysics is never set, a straight dash is still
    // physically blocked by any block in the way — so the gargoyle can't tunnel to its target.

    private static class GargoyleAttackGoal extends Goal {
        private final GargoyleEntity gargoyle;
        private int contactCooldown; // ticks between melee hits
        private int repathCooldown;  // throttle navigation re-pathing

        GargoyleAttackGoal(GargoyleEntity gargoyle) {
            this.gargoyle = gargoyle;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = gargoyle.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = gargoyle.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            gargoyle.setCharging(false);
            gargoyle.getNavigation().stop();
            gargoyle.setTarget(null);
        }

        @Override
        public void tick() {
            LivingEntity target = gargoyle.getTarget();
            if (target == null) {
                return;
            }
            gargoyle.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (contactCooldown > 0) contactCooldown--;
            if (repathCooldown > 0) repathCooldown--;

            // On contact, bite the target on a short cooldown.
            if (gargoyle.getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) {
                gargoyle.setCharging(false);
                if (contactCooldown <= 0) {
                    doHurt(target);
                    contactCooldown = 20;
                }
                return;
            }

            double distSq = gargoyle.distanceToSqr(target);
            boolean lineOfSight = gargoyle.hasLineOfSight(target);

            if (lineOfSight && distSq <= 18.0 * 18.0) {
                // Clear shot → commit to a straight Vex-style swoop. Solid collision still applies,
                // so a block edging into the path stops us harmlessly.
                if (!gargoyle.isCharging()) {
                    gargoyle.setCharging(true);
                    gargoyle.triggerAnim("action", "charge");
                    gargoyle.playSound(SoundEvents.VEX_CHARGE, 1.0F, 0.7F);
                }
                gargoyle.getNavigation().stop();
                Vec3 eye = target.getEyePosition();
                gargoyle.getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 1.4);
            } else {
                // Blocked or out of range → pathfind around the terrain to regain a clean angle.
                gargoyle.setCharging(false);
                if (repathCooldown <= 0) {
                    gargoyle.getNavigation().moveTo(target, 1.0);
                    repathCooldown = 10;
                }
            }
        }

        private void doHurt(LivingEntity target) {
            if (!(gargoyle.level() instanceof ServerLevel level)) {
                return;
            }
            float damage = (float) gargoyle.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (target.hurtServer(level, gargoyle.damageSources().mobAttack(gargoyle), damage)) {
                target.knockback(0.4, gargoyle.getX() - target.getX(), gargoyle.getZ() - target.getZ());
                gargoyle.playSound(SoundEvents.VEX_HURT, 1.0F, 1.2F);
            }
        }
    }
}
