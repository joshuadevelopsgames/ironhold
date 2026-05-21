package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Slime Pet — a tiny floating player-head companion that hops along behind its owner and
 * bites hostile mobs for half a heart, leaving them {@link kingdom.smp.effect.SlimedEffect Slimed}
 * (stuck + slowed) for 3 seconds.
 *
 * <p>Two flavours, one class — the {@link #variant()} is read straight off the registered
 * {@link EntityType} ({@code 0} = Je11ie, {@code 1} = Cheakie), so the client and server agree
 * without any synced data. The renderer uses the variant to pick which player's live skin to
 * wear on the head.
 */
public class SlimePetEntity extends TamableAnimal {

    /** How long the bite keeps a target Slimed. */
    private static final int SLIMED_TICKS = 60; // 3 seconds

    private final int variant;
    private boolean wasOnGround = false;
    private int biteCooldown = 0;

    public SlimePetEntity(EntityType<? extends SlimePetEntity> type, Level level) {
        super(type, level);
        this.variant = type == Ironhold.SLIME_PET_CHEAKIE.get() ? 1 : 0;
        this.moveControl = new SlimePetMoveControl(this);
    }

    /** 0 = Je11ie, 1 = Cheakie. */
    public int variant() {
        return this.variant;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0) // as tough as an iron golem
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 1.0) // half a heart
            .add(Attributes.FOLLOW_RANGE, 18.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new SlimePetAttackGoal(this));
        this.goalSelector.addGoal(3, new SlimePetFollowOwnerGoal(this));
        this.goalSelector.addGoal(4, new SlimePetRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new SlimePetKeepHoppingGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Tame pets hunt nearby hostiles; untamed ones just sit there harmlessly.
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 5, true, false,
            (entity, level) -> this.isTame()));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // Never bite a fellow slime pet — they're friends.
        if (target instanceof SlimePetEntity) return false;
        boolean ok = super.doHurtTarget(level, target);
        if (ok && target instanceof LivingEntity victim && kingdom.smp.ModEffects.SLIMED_EFFECT != null) {
            victim.addEffect(new MobEffectInstance(kingdom.smp.ModEffects.SLIMED_EFFECT, SLIMED_TICKS, 0, false, true, true), this);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.SLIME_SQUISH, this.getSoundSource(), 0.6F, 1.4F);
        }
        return ok;
    }

    // ── Slime-style hop physics ───────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (this.biteCooldown > 0) this.biteCooldown--;
        if (this.onGround() && !this.wasOnGround) {
            this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.4F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 1.6F);
        }
        this.wasOnGround = this.onGround();
    }

    @Override
    public void jumpFromGround() {
        // Full jump power so the hop clears a whole block.
        Vec3 vel = this.getDeltaMovement();
        this.setDeltaMovement(vel.x, this.getJumpPower(), vel.z);
        this.needsSync = true;
    }

    // Stateless & ephemeral: never written to chunk NBT. The Pink Slime Ball re-spawns the
    // companion each second, so a pet left behind (death, teleport) simply vanishes on unload
    // instead of being saved and reloading as a duplicate.
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** When a slime pet is killed, its Pink Slime Ball accessory shatters. */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.getOwner() instanceof ServerPlayer owner) {
            kingdom.smp.item.PinkSlimeBallItem.breakBall(owner);
        }
        super.die(source);
    }

    // ── Tame on interact (these are gifted pets, so taming is instant) ────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isTame()) {
            if (!this.level().isClientSide()) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }
        // Owner toggles sit with an empty hand.
        if (this.isOwnedBy(player) && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Slime-style hop movement (mirrors BabyMimicEntity)
    // ═══════════════════════════════════════════════════════════════════════

    private static class SlimePetMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final SlimePetEntity pet;
        private boolean eager;

        SlimePetMoveControl(SlimePetEntity pet) {
            super(pet);
            this.pet = pet;
            this.yRot = 180.0F * pet.getYRot() / (float) Math.PI;
        }

        void setDirection(float yRot, boolean eager) {
            this.yRot = yRot;
            this.eager = eager;
        }

        void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
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
                        this.jumpDelay = pet.random.nextInt(20) + 10;
                        if (this.eager) {
                            this.jumpDelay /= 3;
                        }
                        pet.getJumpControl().jump();
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

    /** Hop toward the current target and bite it on contact. */
    private static class SlimePetAttackGoal extends Goal {
        private final SlimePetEntity pet;

        SlimePetAttackGoal(SlimePetEntity pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = pet.getTarget();
            return !pet.isOrderedToSit() && target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = pet.getTarget();
            if (target == null) return;
            pet.lookAt(target, 10.0F, 10.0F);
            if (pet.getMoveControl() instanceof SlimePetMoveControl mc) {
                mc.setDirection(pet.getYRot(), true);
                mc.setWantedMovement(2.5);
            }
            double reach = pet.getBbWidth() * 1.5 + target.getBbWidth();
            if (pet.biteCooldown <= 0 && pet.distanceToSqr(target) <= reach * reach) {
                if (pet.level() instanceof ServerLevel sl) {
                    pet.doHurtTarget(sl, target);
                    pet.biteCooldown = 20;
                }
            }
        }
    }

    /** Follow owner by hopping toward them. */
    private static class SlimePetFollowOwnerGoal extends Goal {
        private final SlimePetEntity pet;

        SlimePetFollowOwnerGoal(SlimePetEntity pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!pet.isTame() || pet.isOrderedToSit() || pet.getTarget() != null) return false;
            LivingEntity owner = pet.getOwner();
            return owner != null && pet.distanceToSqr(owner) > 25.0;
        }

        @Override
        public boolean canContinueToUse() {
            if (!pet.isTame() || pet.isOrderedToSit() || pet.getTarget() != null) return false;
            LivingEntity owner = pet.getOwner();
            return owner != null && pet.distanceToSqr(owner) > 4.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity owner = pet.getOwner();
            if (owner == null) return;
            pet.lookAt(owner, 10.0F, 10.0F);
            if (pet.distanceToSqr(owner) > 144.0) {
                pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return;
            }
            if (pet.getMoveControl() instanceof SlimePetMoveControl mc) {
                mc.setDirection(pet.getYRot(), true);
                mc.setWantedMovement(2.5);
            }
        }
    }

    /** Pick a random hop direction when idle. */
    private static class SlimePetRandomDirectionGoal extends Goal {
        private final SlimePetEntity pet;
        private float chosenDegrees;
        private int nextRandomizeTime;

        SlimePetRandomDirectionGoal(SlimePetEntity pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return pet.getTarget() == null && pet.onGround()
                && (!pet.isTame() || pet.isOrderedToSit() || pet.getOwner() == null
                    || pet.distanceToSqr(pet.getOwner()) <= 25.0);
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + pet.getRandom().nextInt(60));
                this.chosenDegrees = pet.getRandom().nextInt(360);
            }
            if (pet.getMoveControl() instanceof SlimePetMoveControl mc) {
                mc.setDirection(this.chosenDegrees, false);
            }
        }
    }

    /** Keep hopping. */
    private static class SlimePetKeepHoppingGoal extends Goal {
        private final SlimePetEntity pet;

        SlimePetKeepHoppingGoal(SlimePetEntity pet) {
            this.pet = pet;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !pet.isPassenger() && (!pet.isTame() || !pet.isOrderedToSit());
        }

        @Override
        public void tick() {
            if (pet.getMoveControl() instanceof SlimePetMoveControl mc) {
                mc.setWantedMovement(1.0);
            }
        }
    }
}
