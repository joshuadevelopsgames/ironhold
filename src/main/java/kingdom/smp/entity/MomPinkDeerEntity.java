package kingdom.smp.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Mom Pink Deer — a larger, protective variant of the Pink Deer.
 * She never breeds. She is silent except when hurt (low pitched cry).
 * On natural spawn she has a 60% chance to be accompanied by a baby deer.
 * When threats approach nearby babies she adopts a warning posture — slowly
 * advancing and staring — but never attacks.
 */
public class MomPinkDeerEntity extends PinkDeerEntity {

    public MomPinkDeerEntity(EntityType<? extends MomPinkDeerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.21)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.TEMPT_RANGE, 10.0);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // Custom goal set for mom — no BreedGoal, no FollowParentGoal.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Custom bolt: far flee destinations, re-picks every 5 ticks, no stopping mid-flight
        this.goalSelector.addGoal(1, new PinkDeerEntity.BoltGoal(this));
        // When a threat is near her babies, approach and stare (overrides shyness)
        this.goalSelector.addGoal(2, new ProtectBabiesGoal());
        // Approach players who offer sweet berries
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(Items.SWEET_BERRIES), false));
        // Mom is more alert — 16-block bubble, 6 when player sneaks
        this.goalSelector.addGoal(4, new PinkDeerEntity.DeerShyGoal(this, 18.0F, 7.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new PinkDeerEntity.DeerGrazeGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Mom never initiates breeding — she's permanently a mom. */
    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    // ── Spawn companion baby ──────────────────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);

        // 60% chance on natural/world-gen spawns to bring a baby along
        if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)
                && level instanceof ServerLevel serverLevel
                && level.getRandom().nextFloat() < 0.6F) {
            spawnBabyCompanion(serverLevel);
        }
        return result;
    }

    private void spawnBabyCompanion(ServerLevel serverLevel) {
        // 1/1000 shot at a rare baby, otherwise a normal pink deer baby
        EntityType<? extends PinkDeerEntity> babyType =
            serverLevel.getRandom().nextInt(1000) == 0
                ? kingdom.smp.Ironhold.RARE_PINK_DEER.get()
                : kingdom.smp.Ironhold.PINK_DEER.get();

        AgeableMob baby = babyType.create(serverLevel, EntitySpawnReason.NATURAL);
        if (baby == null) return;

        baby.setBaby(true);
        double ox = (serverLevel.getRandom().nextDouble() - 0.5) * 3.0;
        double oz = (serverLevel.getRandom().nextDouble() - 0.5) * 3.0;
        baby.setPos(this.getX() + ox, this.getY(), this.getZ() + oz);
        baby.setYRot(this.getYRot());
        serverLevel.addFreshEntity(baby);
    }

    // ── Offspring ─────────────────────────────────────────────────────────────

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        // canMate() always returns false so this is only a safety fallback
        return kingdom.smp.Ironhold.PINK_DEER.get().create(level, EntitySpawnReason.BREEDING);
    }

    // ── Sounds — silent except when hurt ─────────────────────────────────────

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kingdom.smp.Ironhold.PINK_DEER_MOM_HURT.get();
    }

    // ── Inner goal: warning posture when threats approach babies ──────────────

    private class ProtectBabiesGoal extends Goal {

        private @Nullable LivingEntity threat = null;
        // Only run the expensive entity scan every 20 ticks (~1 second)
        private int scanCooldown = 0;
        // Tracks whether we've already issued a nav stop to avoid repeated calls
        private boolean holdingGround = false;

        public ProtectBabiesGoal() {
            // MOVE + LOOK flags: GoalSelector won't run other nav/look goals in parallel,
            // eliminating redundant path requests and look-control fights each tick.
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return scanForThreat();
        }

        @Override
        public boolean canContinueToUse() {
            // Cheap check first — distanceToSqr avoids sqrt; compare against 20² = 400
            if (threat == null || !threat.isAlive()
                    || MomPinkDeerEntity.this.distanceToSqr(threat) >= 400.0) {
                return scanForThreat();
            }
            return true;
        }

        /**
         * Entity scan rate-limited to once every 20 ticks.
         * Uses exactly 2 entity searches regardless of baby count:
         *   1. Cheap baby check within 10 blocks of mom.
         *   2. If babies present, single threat search within 17 blocks of mom
         *      (= 10 baby-range + 7 threat-range — covers all reachable threats).
         * Previous design did N+1 searches (1 baby search + 1 per baby).
         */
        private boolean scanForThreat() {
            if (scanCooldown > 0) {
                scanCooldown--;
                return threat != null && threat.isAlive();
            }
            scanCooldown = 20;

            // 1. Is any baby nearby? Early-exit avoids the second search entirely.
            boolean hasBabies = !MomPinkDeerEntity.this.level().getEntitiesOfClass(
                PinkDeerEntity.class,
                MomPinkDeerEntity.this.getBoundingBox().inflate(10.0, 4.0, 10.0),
                AgeableMob::isBaby).isEmpty();

            if (!hasBabies) {
                threat = null;
                return false;
            }

            // 2. Single broad threat search: baby ≤10 from mom + threat ≤7 from baby = ≤17 from mom.
            List<LivingEntity> threats = MomPinkDeerEntity.this.level().getEntitiesOfClass(
                LivingEntity.class,
                MomPinkDeerEntity.this.getBoundingBox().inflate(17.0, 5.0, 17.0),
                e -> e != MomPinkDeerEntity.this
                    && !(e instanceof PinkDeerEntity)
                    && e.isAlive()
                    && (e instanceof Monster
                        || (e instanceof Player p && !p.isCreative() && !p.isSpectator())));

            threat = threats.isEmpty() ? null : threats.get(0);
            return threat != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            // true so LookControl.setLookAt is refreshed every tick for smooth tracking
            return true;
        }

        @Override
        public void start() {
            holdingGround = false;
        }

        @Override
        public void stop() {
            threat = null;
            scanCooldown = 0;
            holdingGround = false;
            MomPinkDeerEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (threat == null) return;

            // setLookAt is cheap — just stores target coords, no pathfinding
            MomPinkDeerEntity.this.getLookControl().setLookAt(threat, 30F, 30F);

            // distanceToSqr avoids sqrt; compare against 4.5² = 20.25
            if (MomPinkDeerEntity.this.distanceToSqr(threat) > 20.25) {
                holdingGround = false;
                // Recalculate nav path every 10 ticks — smooth enough for a slow advance
                if (MomPinkDeerEntity.this.tickCount % 10 == 0) {
                    MomPinkDeerEntity.this.getNavigation().moveTo(threat, 0.85);
                }
            } else if (!holdingGround) {
                // Only issue stop() once on transition to hold-ground state
                holdingGround = true;
                MomPinkDeerEntity.this.getNavigation().stop();
            }
        }
    }
}
