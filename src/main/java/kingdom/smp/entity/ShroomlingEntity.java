package kingdom.smp.entity;

import kingdom.smp.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shroomling — gentle, glow-loving cave mushroom. Passive: flees danger,
 * is drawn to glow berries, tames and breeds on glow berries. Periodically
 * releases faint glow spores and gives a little hop when startled or trailing
 * a target. Immune to the Darkness effect.
 */
public class ShroomlingEntity extends TamableAnimal {

    private static final float TAME_CHANCE = 1.0F / 3.0F;
    /** Chance a naturally-spawned Shroomling is the rare glowing-orange variant. */
    private static final float ORANGE_CHANCE = 0.12F;

    private static final EntityDataAccessor<Boolean> DATA_ORANGE =
            SynchedEntityData.defineId(ShroomlingEntity.class, EntityDataSerializers.BOOLEAN);
    /** True once a thief has lifted this Shroomling's cap — it renders bare-headed thereafter. */
    private static final EntityDataAccessor<Boolean> DATA_CAPLESS =
            SynchedEntityData.defineId(ShroomlingEntity.class, EntityDataSerializers.BOOLEAN);

    private int hopCooldown;
    private int sporeCooldown;

    public ShroomlingEntity(EntityType<? extends ShroomlingEntity> type, Level level) {
        super(type, level);
        this.hopCooldown = level.getRandom().nextInt(20) + 10;
        this.sporeCooldown = level.getRandom().nextInt(60) + 40;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ORANGE, false);
        builder.define(DATA_CAPLESS, false);
    }

    /** True for the rare glowing-orange variant (same model, recoloured texture). */
    public boolean isOrange() {
        return this.entityData.get(DATA_ORANGE);
    }

    public void setOrange(boolean orange) {
        this.entityData.set(DATA_ORANGE, orange);
    }

    /** True once this Shroomling's cap has been pickpocketed — hides the cap on the model. */
    public boolean isCapless() {
        return this.entityData.get(DATA_CAPLESS);
    }

    public void setCapless(boolean capless) {
        this.entityData.set(DATA_CAPLESS, capless);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (level.getRandom().nextFloat() < ORANGE_CHANCE) {
            setOrange(true);
        }
        return result;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.TEMPT_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.4, 1.6));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.2, stack -> stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.1, 8.0F, 3.0F));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        ShroomlingEntity child = kingdom.smp.ModEntities.SHROOMLING.get().create(level, EntitySpawnReason.BREEDING);
        if (child != null) {
            if (this.isTame()) {
                child.setOwnerReference(this.getOwnerReference());
                child.setTame(true, true);
            }
            // Inherit colour from a random parent, with a small mutation chance.
            boolean orange = (partner instanceof ShroomlingEntity other && level.getRandom().nextBoolean())
                    ? other.isOrange()
                    : this.isOrange();
            if (level.getRandom().nextFloat() < 0.08F) {
                orange = !orange;
            }
            child.setOrange(orange);
        }
        return child;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean glowBerry = held.is(Items.GLOW_BERRIES);

        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (glowBerry && this.getHealth() < this.getMaxHealth()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(2.0F);
                    sparkleAt(this.position().add(0, 0.4, 0), 6);
                    return InteractionResult.SUCCESS;
                }
                if (glowBerry && (this.isBaby() || this.canFallInLove())) {
                    // Animal handles love-mode / baby growth + item consumption.
                    return super.mobInteract(player, hand);
                }
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        if (glowBerry) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            sparkleAt(this.position().add(0, 0.4, 0), 8);
            if (this.random.nextFloat() < TAME_CHANCE) {
                this.tame(player);
                this.navigation.stop();
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        // Spore Hop — small hop when fleeing or trailing a target.
        boolean active = this.isPanicking() || this.getNavigation().isInProgress();
        if (active && this.onGround() && !this.isOrderedToSit()
                && this.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
            if (--this.hopCooldown <= 0) {
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x * 1.15, 0.34, v.z * 1.15);
                this.hopCooldown = 12 + this.random.nextInt(8);
            }
        }

        // Glow Spores — faint periodic spore puff.
        if (--this.sporeCooldown <= 0) {
            this.sporeCooldown = 70 + this.random.nextInt(70);
            emitSpores();
        }
    }

    private void emitSpores() {
        if (!(this.level() instanceof ServerLevel server)) return;
        Vec3 c = this.position().add(0, this.getBbHeight() * 0.75, 0);
        ParticleOptions spores = this.isOrange()
                ? ModParticles.ORANGE_SHROOMLING_SPORE.get()
                : ModParticles.SHROOMLING_SPORE.get();
        server.sendParticles(spores, c.x, c.y, c.z, 9, 0.3, 0.22, 0.3, 0.025);
        server.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, this.isOrange() ? 3 : 2, 0.22, 0.16, 0.22, 0.0);
    }

    private void sparkleAt(Vec3 pos, int count) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0);
            server.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, count * 2, 0.3, 0.3, 0.3, 0.2);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().is(MobEffects.DARKNESS)) return false;
        return super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && !this.hasCustomName();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kingdom.smp.ModSounds.SHROOMLING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kingdom.smp.ModSounds.SHROOMLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kingdom.smp.ModSounds.SHROOMLING_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 1400;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Orange", isOrange());
        out.putBoolean("Capless", isCapless());
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setOrange(in.getBooleanOr("Orange", false));
        setCapless(in.getBooleanOr("Capless", false));
    }

    /** Cave creature: spawns on solid ground at any light level (not just sunlit grass). */
    public static boolean checkShroomlingSpawnRules(EntityType<ShroomlingEntity> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason == EntitySpawnReason.SPAWNER) return true;
        return level.getBlockState(pos.below()).canOcclude();
    }
}
