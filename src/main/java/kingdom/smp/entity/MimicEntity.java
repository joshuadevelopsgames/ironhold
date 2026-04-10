package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Mimic — a chest-shaped monster that lies dormant, then springs to life
 * and hops toward the player like a slime when provoked.
 *
 * <p>Only awakens on right-click (trying to "open" it) or taking damage.
 * While dormant, snaps to the block grid and faces the nearest player
 * at 90° increments like a placed chest.
 */
public class MimicEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_AWAKENED =
        SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ticks since awakening — used for the lid-open animation. */
    private int awakeTicks = 0;

    /** Ticks for the "springing open" animation before the mimic starts moving. */
    private static final int WAKE_ANIMATION_TICKS = 15;

    /** Whether the entity has been snapped to the grid on first tick. */
    private boolean snappedToGrid = false;

    /** Tracks previous ground state for landing sound. */
    private boolean wasOnGround = false;

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.moveControl = new MimicMoveControl(this);
    }

    @Override
    public @org.jspecify.annotations.Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level, DifficultyInstance difficulty,
        EntitySpawnReason reason, @org.jspecify.annotations.Nullable SpawnGroupData spawnData
    ) {
        // Snap to block grid immediately on spawn
        snapToGrid();
        snappedToGrid = true;
        snapRotationTowardPlayer();
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.84)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKENED, false);
    }

    @Override
    protected void registerGoals() {
        // Slime-style goals: hop toward target, random direction when idle
        this.goalSelector.addGoal(1, new MimicAttackGoal(this));
        this.goalSelector.addGoal(3, new MimicRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new MimicKeepHoppingGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── State ────────────────────────────────────────────────────────────────

    public boolean isAwakened() {
        return this.entityData.get(DATA_AWAKENED);
    }

    private void awaken(Player player) {
        if (isAwakened()) return;
        this.entityData.set(DATA_AWAKENED, true);
        this.awakeTicks = 0;
        this.setTarget(player);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CHEST_OPEN,
                SoundSource.HOSTILE, 1.0F, 0.6F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                SoundSource.HOSTILE, 0.7F, 1.4F);
        }
    }

    public int getAwakeTicks() {
        return awakeTicks;
    }

    // ── Grid snapping ───────────────────────────────────────────────────────

    private void snapToGrid() {
        double sx = Math.floor(this.getX()) + 0.5;
        double sy = Math.floor(this.getY());
        double sz = Math.floor(this.getZ()) + 0.5;
        this.setPos(sx, sy, sz);
        this.xo = sx;
        this.yo = sy;
        this.zo = sz;
    }

    private void snapRotationTowardPlayer() {
        Player nearest = this.level().getNearestPlayer(this, 24.0);
        if (nearest == null) return;

        double dx = nearest.getX() - this.getX();
        double dz = nearest.getZ() - this.getZ();
        float rawAngle = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float snapped = Math.round(rawAngle / 90.0F) * 90.0F;

        this.setYRot(snapped);
        this.yRotO = snapped;
        this.yHeadRot = snapped;
        this.yBodyRot = snapped;
    }

    // ── Interaction — right-clicking the "chest" wakes it ────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAwakened()) {
            awaken(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ── Contact damage (like slime) ─────────────────────────────────────────

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (isAwakened() && awakeTicks > WAKE_ANIMATION_TICKS) {
            dealContactDamage(player);
        }
    }

    private void dealContactDamage(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel
            && this.isAlive()
            && this.isWithinMeleeAttackRange(target)
            && this.hasLineOfSight(target)) {
            DamageSource source = this.damageSources().mobAttack(this);
            if (target.hurtServer(serverLevel, source, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                this.playSound(SoundEvents.WOOD_BREAK, 1.0F, 0.8F);
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
            }
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void aiStep() {
        if (!isAwakened()) {
            if (!snappedToGrid) {
                snapToGrid();
                snappedToGrid = true;
            }

            this.setDeltaMovement(Vec3.ZERO);
            this.setXRot(0);

            if (this.tickCount % 20 == 0 && !this.level().isClientSide()) {
                snapRotationTowardPlayer();
            } else {
                this.setYRot(this.yRotO);
                this.yHeadRot = this.yRotO;
                this.yBodyRot = this.yRotO;
            }

            return; // Skip normal AI — only wakes on interact or damage
        }

        // ── Awakened ─────────────────────────────────────────────────────────
        awakeTicks++;

        if (awakeTicks <= WAKE_ANIMATION_TICKS) {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide() && awakeTicks == 1) {
                for (int i = 0; i < 12; i++) {
                    double dx = (this.random.nextDouble() - 0.5) * 0.8;
                    double dy = this.random.nextDouble() * 0.6;
                    double dz = (this.random.nextDouble() - 0.5) * 0.8;
                    this.level().addParticle(ParticleTypes.POOF,
                        this.getX() + dx, this.getY() + dy, this.getZ() + dz,
                        0, 0.05, 0);
                }
            }
            if (awakeTicks == WAKE_ANIMATION_TICKS && !this.level().isClientSide()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.42, 0));
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                    SoundSource.HOSTILE, 0.6F, 1.5F);
            }
            return;
        }

        super.aiStep();
    }

    @Override
    public void jumpFromGround() {
        Vec3 vel = this.getDeltaMovement();
        this.setDeltaMovement(vel.x, this.getJumpPower(), vel.z);
        this.needsSync = true;
    }

    @Override
    public void tick() {
        super.tick();
        // Play wood thud on landing (like slime's landing sound)
        if (isAwakened() && this.onGround() && !this.wasOnGround) {
            this.playSound(SoundEvents.WOOD_PLACE, 0.8F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }
        this.wasOnGround = this.onGround();
    }

    // ── Loot — 20% chance to drop a Mimic Key ────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (killedByPlayer && this.random.nextFloat() < 0.20F) {
            this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(kingdom.smp.Ironhold.MIMIC_KEY.get()));
        }
    }

    // ── Hitbox — expand projectile pick radius to fully cover the model ────

    @Override
    public float getPickRadius() {
        // Extra margin so arrows/projectiles don't slip past the sides
        return 0.15F;
    }

    // ── Dormant mimics shouldn't make ambient sounds ─────────────────────────

    @Override
    public int getAmbientSoundInterval() {
        return isAwakened() ? super.getAmbientSoundInterval() : Integer.MAX_VALUE;
    }

    @Override
    public boolean isCustomNameVisible() {
        return isAwakened() && super.isCustomNameVisible();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isAwakened() && source.getEntity() instanceof Player player) {
            awaken(player);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return isAwakened();
    }

    // ── Collision — dormant mimics should be solid like a chest block ────────

    @Override
    public boolean isPickable() {
        // Makes the entity act as a solid body that blocks player movement
        return this.isAlive();
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity entity) {
        // Dormant mimics are solid like a chest block
        return !isAwakened() && this.isAlive();
    }

    // ── Save / Load ─────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Awakened", isAwakened());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.snappedToGrid = false;
        if (input.getBooleanOr("Awakened", false)) {
            this.entityData.set(DATA_AWAKENED, true);
            this.awakeTicks = WAKE_ANIMATION_TICKS + 1;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Slime-style movement: MoveControl + Goals
    // ═══════════════════════════════════════════════════════════════════════

    /** Slime-like MoveControl that hops instead of walking. */
    private static class MimicMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final MimicEntity mimic;
        private boolean aggressive;

        MimicMoveControl(MimicEntity mimic) {
            super(mimic);
            this.mimic = mimic;
            this.yRot = 180.0F * mimic.getYRot() / (float) Math.PI;
        }

        void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.aggressive = aggressive;
        }

        void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            if (!mimic.isAwakened() || mimic.awakeTicks <= WAKE_ANIMATION_TICKS) return;

            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = MoveControl.Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = mimic.random.nextInt(20) + 10;
                        if (this.aggressive) {
                            this.jumpDelay /= 3;
                        }
                        mimic.getJumpControl().jump();
                    } else {
                        this.mob.xxa = 0.0F;
                        this.mob.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }

    /** Hop toward attack target (like SlimeAttackGoal). */
    private static class MimicAttackGoal extends Goal {
        private final MimicEntity mimic;
        private int tiredTimer;

        MimicAttackGoal(MimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mimic.isAwakened() || mimic.awakeTicks <= WAKE_ANIMATION_TICKS) return false;
            LivingEntity target = mimic.getTarget();
            return target != null && mimic.canAttack(target);
        }

        @Override
        public void start() {
            this.tiredTimer = reducedTickDelay(300);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mimic.getTarget();
            return target != null && mimic.canAttack(target) && --this.tiredTimer > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = mimic.getTarget();
            if (target != null) {
                mimic.lookAt(target, 10.0F, 10.0F);
            }
            if (mimic.getMoveControl() instanceof MimicMoveControl mc) {
                mc.setDirection(mimic.getYRot(), true);
            }
        }
    }

    /** Random direction when idle (like SlimeRandomDirectionGoal). */
    private static class MimicRandomDirectionGoal extends Goal {
        private final MimicEntity mimic;
        private float chosenDegrees;
        private int nextRandomizeTime;

        MimicRandomDirectionGoal(MimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mimic.isAwakened()
                && mimic.awakeTicks > WAKE_ANIMATION_TICKS
                && mimic.getTarget() == null
                && mimic.onGround();
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + mimic.getRandom().nextInt(60));
                this.chosenDegrees = mimic.getRandom().nextInt(360);
            }
            if (mimic.getMoveControl() instanceof MimicMoveControl mc) {
                mc.setDirection(this.chosenDegrees, false);
            }
        }
    }

    /** Keep hopping (like SlimeKeepOnJumpingGoal). */
    private static class MimicKeepHoppingGoal extends Goal {
        private final MimicEntity mimic;

        MimicKeepHoppingGoal(MimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mimic.isAwakened()
                && mimic.awakeTicks > WAKE_ANIMATION_TICKS
                && !mimic.isPassenger();
        }

        @Override
        public void tick() {
            if (mimic.getMoveControl() instanceof MimicMoveControl mc) {
                mc.setWantedMovement(1.0);
            }
        }
    }
}
