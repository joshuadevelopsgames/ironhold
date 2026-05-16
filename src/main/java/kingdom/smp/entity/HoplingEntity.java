package kingdom.smp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hopling — small, rare End-dimension pet. Hops, teleports defensively,
 * tames after three feedings of popped chorus fruit. Zero combat damage.
 */
public class HoplingEntity extends TamableAnimal {

    public static final int TELEPORT_TOTAL_TICKS = 11;
    public static final int VANISH_TICK = 5;
    private static final int TAMING_FEED_COUNT = 3;
    private static final int FLEE_RADIUS = 8;
    private static final int FLEE_TELEPORT_RANGE = 12;

    private static final EntityDataAccessor<Integer> TELEPORT_TICKS =
            SynchedEntityData.defineId(HoplingEntity.class, EntityDataSerializers.INT);

    private int hopCooldown;
    private int chorusFedCount;
    private int idleTeleportCooldown;

    public HoplingEntity(EntityType<? extends HoplingEntity> type, Level level) {
        super(type, level);
        this.hopCooldown = level.getRandom().nextInt(20) + 10;
        this.idleTeleportCooldown = 600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.JUMP_STRENGTH, 0.55);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TELEPORT_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 8.0F, 3.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (held.is(Items.POPPED_CHORUS_FRUIT) && this.getHealth() < this.getMaxHealth()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(2.0F);
                    sparkleAt(this.position().add(0, 0.4, 0), 6);
                    return InteractionResult.SUCCESS;
                }
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        if (held.is(Items.POPPED_CHORUS_FRUIT)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            this.chorusFedCount++;
            sparkleAt(this.position().add(0, 0.4, 0), 8);

            if (this.chorusFedCount >= TAMING_FEED_COUNT) {
                if (this.random.nextFloat() < 0.5F) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.chorusFedCount = TAMING_FEED_COUNT - 1;
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        int ttl = this.entityData.get(TELEPORT_TICKS);
        if (ttl > 0) this.entityData.set(TELEPORT_TICKS, ttl - 1);

        if (this.onGround() && !this.isOrderedToSit()
                && this.getDeltaMovement().horizontalDistanceSqr() > 0.0001) {
            if (--this.hopCooldown <= 0) {
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x * 1.4, 0.42, v.z * 1.4);
                this.hopCooldown = 8 + this.random.nextInt(6);
            }
        }

        if (!this.isTame()) {
            Player nearest = this.level().getNearestPlayer(this, FLEE_RADIUS);
            if (nearest != null && nearest.distanceToSqr(this) < 9.0 && this.random.nextInt(20) == 0) {
                tryTeleportAwayFrom(nearest.position());
            }
        } else if (--this.idleTeleportCooldown <= 0) {
            this.idleTeleportCooldown = 800 + this.random.nextInt(400);
            if (this.random.nextInt(3) == 0 && !this.isOrderedToSit()) {
                tryShortTeleport(2);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean wasHurt = super.hurtServer(level, source, amount);
        if (wasHurt && this.isAlive() && !this.isTame()) {
            Entity attacker = source.getEntity();
            if (attacker != null) {
                tryTeleportAwayFrom(attacker.position());
            } else {
                tryShortTeleport(FLEE_TELEPORT_RANGE);
            }
        }
        return wasHurt;
    }

    private void tryTeleportAwayFrom(Vec3 threat) {
        Vec3 away = this.position().subtract(threat).normalize().scale(FLEE_TELEPORT_RANGE);
        Vec3 target = this.position().add(away.x, 0, away.z);
        teleportToWithFx(target.x, target.y, target.z);
    }

    private void tryShortTeleport(int radius) {
        RandomSource r = this.random;
        for (int i = 0; i < 8; i++) {
            double dx = (r.nextDouble() - 0.5) * 2 * radius;
            double dz = (r.nextDouble() - 0.5) * 2 * radius;
            double tx = this.getX() + dx;
            double tz = this.getZ() + dz;
            double ty = findGroundY(tx, this.getY(), tz);
            if (!Double.isNaN(ty)) {
                teleportToWithFx(tx, ty, tz);
                return;
            }
        }
    }

    private double findGroundY(double x, double startY, double z) {
        BlockPos start = BlockPos.containing(x, startY + 1, z);
        for (int dy = 0; dy < 8; dy++) {
            BlockPos p = start.below(dy);
            if (!this.level().getBlockState(p).isAir() && this.level().getBlockState(p.above()).isAir()) {
                return p.getY() + 1.0;
            }
        }
        return Double.NaN;
    }

    private void teleportToWithFx(double x, double y, double z) {
        if (!(this.level() instanceof ServerLevel server)) return;

        Vec3 origin = this.position();
        playTeleportRing(server, origin);
        server.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6F, 1.4F);

        this.entityData.set(TELEPORT_TICKS, TELEPORT_TOTAL_TICKS);
        this.snapTo(x, y, z, this.getYRot(), this.getXRot());
        this.navigation.stop();

        Vec3 dest = new Vec3(x, y, z);
        playTeleportRing(server, dest);
        server.playSound(null, dest.x, dest.y, dest.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6F, 1.6F);
    }

    private void playTeleportRing(ServerLevel server, Vec3 center) {
        final int ringPoints = 16;
        final double radius = 0.7;
        for (int i = 0; i < ringPoints; i++) {
            double a = (Math.PI * 2.0 * i) / ringPoints;
            double rx = center.x + Math.cos(a) * radius;
            double rz = center.z + Math.sin(a) * radius;
            server.sendParticles(ParticleTypes.END_ROD, rx, center.y + 0.05, rz, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.PORTAL, rx, center.y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        server.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.5, center.z,
                10, 0.3, 0.6, 0.3, 0.5);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.4, center.z,
                6, 0.2, 0.3, 0.2, 0.05);
    }

    private void sparkleAt(Vec3 pos, int count) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0);
            server.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, count * 2, 0.3, 0.3, 0.3, 0.4);
        }
    }

    public int getTeleportTicks() {
        return this.entityData.get(TELEPORT_TICKS);
    }

    public boolean isTeleporting() {
        return this.entityData.get(TELEPORT_TICKS) > 0;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && !this.hasCustomName();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMITE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMITE_DEATH;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("ChorusFed", this.chorusFedCount);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.chorusFedCount = in.getIntOr("ChorusFed", 0);
    }
}
