package kingdom.smp.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Mini Dragon — a tiny tameable blue ender dragon pet.
 * Always airborne, follows its owner, and perches on their shoulder.
 */
public class MiniDragonEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> DATA_PERCHING =
        SynchedEntityData.defineId(MiniDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private final DragonFlightHistory flightHistory = new DragonFlightHistory();

    /** Ticks until the dragon may spontaneously decide to perch again. */
    private int perchCooldown;

    public MiniDragonEntity(EntityType<? extends MiniDragonEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PERCHING, false);
    }

    public boolean isPerching() {
        return this.entityData.get(DATA_PERCHING);
    }

    public void setPerching(boolean perching) {
        this.entityData.set(DATA_PERCHING, perching);
    }

    public DragonFlightHistory getFlightHistory() {
        return this.flightHistory;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FLYING_SPEED, 0.4)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PerchOnOwnerGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.2, 6.0F, 2.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    // ── Tick ────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // ── Face the direction of travel (no flying backwards) ──────────
        if (!this.isPerching()) {
            Vec3 move = this.getDeltaMovement();
            double horizSpeedSq = move.x * move.x + move.z * move.z;
            if (horizSpeedSq > 0.0001) {
                // Target yaw from movement vector
                float targetYaw = (float) (Math.toDegrees(Math.atan2(-move.x, move.z)));
                // Smooth turn — lerp toward target so it twirls naturally
                float currentYaw = this.getYRot();
                float diff = targetYaw - currentYaw;
                // Wrap to [-180, 180]
                while (diff > 180.0F) diff -= 360.0F;
                while (diff < -180.0F) diff += 360.0F;
                float turnSpeed = 12.0F; // degrees per tick max turn
                diff = Math.max(-turnSpeed, Math.min(turnSpeed, diff));
                float newYaw = currentYaw + diff;
                this.setYRot(newYaw);
                this.yBodyRot = newYaw;
                this.yHeadRot = newYaw;
            }
        }

        this.flightHistory.record(this.getY(), this.getYRot());

        if (this.perchCooldown > 0) this.perchCooldown--;

        if (this.isPerching()) {
            LivingEntity owner = this.getOwner();
            if (owner == null || !owner.isAlive() || this.distanceToSqr(owner) > 100) {
                this.setPerching(false);
                return;
            }
            // Snap to the owner's right shoulder
            float yaw = (float) Math.toRadians(owner.yBodyRot);
            double sx = owner.getX() - Math.cos(yaw) * 0.4;
            double sy = owner.getY() + 1.5;
            double sz = owner.getZ() - Math.sin(yaw) * 0.4;
            this.setPos(sx, sy, sz);
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(owner.yBodyRot);
            this.yBodyRot = owner.yBodyRot;
            this.yHeadRot = owner.yHeadRot;
            this.navigation.stop();
        } else {
            // ── Stay airborne — never touch the ground ──────────────────
            Vec3 vel = this.getDeltaMovement();

            // Find the ground level below us
            net.minecraft.core.BlockPos.MutableBlockPos probe = new net.minecraft.core.BlockPos.MutableBlockPos(
                (int) Math.floor(this.getX()), (int) Math.floor(this.getY()), (int) Math.floor(this.getZ()));
            for (int i = 0; i < 8; i++) {
                if (!this.level().getBlockState(probe).isAir()) break;
                probe.move(net.minecraft.core.Direction.DOWN);
            }
            double groundY = probe.getY() + 1; // top of the ground block
            double minFlyHeight = groundY + 2.5; // hover at least 2.5 blocks above ground

            // Always maintain minimum height — force upward if too low
            if (this.getY() < minFlyHeight) {
                double urgency = (minFlyHeight - this.getY()) * 0.15; // stronger push the lower we are
                this.setDeltaMovement(vel.x, Math.max(vel.y, urgency), vel.z);
                vel = this.getDeltaMovement();
            }

            // Also stay at or above owner's eye level
            LivingEntity owner = this.getOwner();
            if (owner != null && this.getY() < owner.getEyeY()) {
                double lift = (owner.getEyeY() - this.getY()) * 0.1;
                this.setDeltaMovement(vel.x, Math.max(vel.y, lift), vel.z);
            }

            // Prevent downward drift when idle — always float
            if (this.getDeltaMovement().y < -0.02) {
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x, v.y * 0.7, v.z);
            }

            // ── Idle drift — gentle circling so it never hovers in place ──
            Vec3 cur = this.getDeltaMovement();
            double horizSpeed = Math.sqrt(cur.x * cur.x + cur.z * cur.z);
            if (horizSpeed < 0.03) {
                // Lazy circle: direction slowly rotates over time
                float driftAngle = (float) Math.toRadians(this.tickCount * 1.8);
                double driftSpeed = 0.025 + Math.sin(this.tickCount * 0.05) * 0.008;
                double dx = -Math.sin(driftAngle) * driftSpeed;
                double dz = Math.cos(driftAngle) * driftSpeed;
                // Gentle vertical bob
                double dy = Math.sin(this.tickCount * 0.07) * 0.008;
                this.setDeltaMovement(cur.x + dx, cur.y + dy, cur.z + dz);
            }
        }
    }

    // ── Flying — never take fall damage ─────────────────────────────────

    @Override
    protected void checkFallDamage(double yDiff, boolean onGround,
                                    net.minecraft.world.level.block.state.BlockState state,
                                    net.minecraft.core.BlockPos pos) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    // ── Interaction — toggle perch ──────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isTame()) {
            if (!this.level().isClientSide()) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 0.3F, 2.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isOwnedBy(player)) {
            if (!this.level().isClientSide()) {
                boolean perch = !this.isPerching();
                this.setPerching(perch);
                this.navigation.stop();
                this.setTarget(null);
                if (!perch) {
                    this.perchCooldown = 200; // Don't auto-perch again for 10 seconds
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // ── Sounds ──────────────────────────────────────────────────────────

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.15F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // ── Misc ────────────────────────────────────────────────────────────

    @Override
    public boolean isFood(ItemStack stack) { return false; }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) { return null; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
        return !this.isPerching() && super.canBeCollidedWith(other);
    }

    @Override
    public boolean isColliding(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // No block collision while perching — prevents pushing the owner
        return !this.isPerching() && super.isColliding(pos, state);
    }

    @Override
    protected void pushEntities() {
        // Never push other entities (especially the owner while perching)
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }

    // ════════════════════════════════════════════════════════════════════
    // Perch goal — spontaneously perch when hovering near the owner
    // ════════════════════════════════════════════════════════════════════

    private static class PerchOnOwnerGoal extends Goal {
        private final MiniDragonEntity dragon;
        private int perchTicks;

        PerchOnOwnerGoal(MiniDragonEntity dragon) {
            this.dragon = dragon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!dragon.isTame() || dragon.isPerching() || dragon.perchCooldown > 0) return false;
            LivingEntity owner = dragon.getOwner();
            if (owner == null) return false;
            // Only perch when close and owner is relatively still
            double distSq = dragon.distanceToSqr(owner);
            boolean ownerStill = owner.getDeltaMovement().horizontalDistanceSqr() < 0.005;
            return distSq < 16.0 && ownerStill && dragon.random.nextInt(200) == 0;
        }

        @Override
        public void start() {
            dragon.setPerching(true);
            this.perchTicks = 200 + dragon.random.nextInt(400); // Perch for 10-30 seconds
        }

        @Override
        public boolean canContinueToUse() {
            if (!dragon.isPerching()) return false;
            LivingEntity owner = dragon.getOwner();
            if (owner == null || !owner.isAlive()) return false;
            // Hop off if owner starts sprinting or we've perched long enough
            boolean ownerSprinting = owner instanceof Player p && p.isSprinting();
            return --this.perchTicks > 0 && !ownerSprinting;
        }

        @Override
        public void stop() {
            dragon.setPerching(false);
            dragon.perchCooldown = 400; // Wait 20 seconds before perching again
        }
    }
}
