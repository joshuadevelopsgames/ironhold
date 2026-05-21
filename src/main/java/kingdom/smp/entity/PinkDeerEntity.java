package kingdom.smp.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pink Deer — a gentle, shy creature. Breeds with sweet berries.
 * Babies follow the nearest Mom Pink Deer when one is in range.
 */
public class PinkDeerEntity extends Animal {

    // ── Synched state ─────────────────────────────────────────────────────────

    static final EntityDataAccessor<Boolean> DATA_IS_GRAZING =
        SynchedEntityData.defineId(PinkDeerEntity.class, EntityDataSerializers.BOOLEAN);

    public PinkDeerEntity(EntityType<? extends PinkDeerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_GRAZING, false);
    }

    public boolean isGrazing() {
        return this.entityData.get(DATA_IS_GRAZING);
    }

    void setGrazing(boolean grazing) {
        this.entityData.set(DATA_IS_GRAZING, grazing);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.TEMPT_RANGE, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Custom bolt: picks flee targets 20 blocks away, re-picks every 5 ticks
        this.goalSelector.addGoal(1, new BoltGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        // Approach players who hold sweet berries
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(Items.SWEET_BERRIES), false));
        // Custom shyness: 14-block bubble, collapses to 5 when player sneaks
        this.goalSelector.addGoal(4, new DeerShyGoal(this, 16.0F, 5.0F));
        // Babies follow nearest mom, fallback to same-type parent
        this.goalSelector.addGoal(5, new FollowMomGoal());
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new DeerGrazeGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return kingdom.smp.ModEntities.PINK_DEER.get().create(level,
            net.minecraft.world.entity.EntitySpawnReason.BREEDING);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 1200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kingdom.smp.ModSounds.PINK_DEER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kingdom.smp.ModSounds.PINK_DEER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kingdom.smp.ModSounds.PINK_DEER_DEATH.get();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Shared static goals — used by both PinkDeerEntity and MomPinkDeerEntity
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Replaces vanilla PanicGoal. Picks flee destinations 20 blocks away from the
     * threat source and re-picks every 5 ticks so the mob never stops mid-flight.
     * Also handles fire / freezing like vanilla PanicGoal does.
     */
    static class BoltGoal extends Goal {

        private final PathfinderMob mob;
        private int runTicks;

        BoltGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.getLastHurtByMob() != null || mob.isOnFire() || mob.isFreezing();
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getLastHurtByMob() != null || mob.isOnFire() || mob.isFreezing();
        }

        @Override
        public void start() {
            runTicks = 0;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            runTicks++;
            // Aggressively re-pick a far destination every 5 ticks, or whenever
            // the current path finishes — no standing still mid-panic
            if (runTicks % 5 == 1 || mob.getNavigation().isDone()) {
                LivingEntity source = mob.getLastHurtByMob();
                Vec3 away = source != null ? source.position() : mob.position();
                Vec3 dest = DefaultRandomPos.getPosAway(mob, 20, 7, away);
                if (dest != null) {
                    mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 2.2);
                }
            }
        }
    }

    /**
     * Sneak-aware player avoidance. Large bubble (normalRange) when player is
     * upright; shrinks to sneakRange when player crouches. Berry-holders are ignored
     * entirely. Uses DefaultRandomPos.getPosAway() directly so flee paths are always
     * found — no silent failure like vanilla AvoidEntityGoal can have.
     * TPS-friendly: entity scan rate-limited to once per 10 ticks.
     * Deer always sprint away — they never walk from threats.
     */
    static class DeerShyGoal extends Goal {

        private final PathfinderMob mob;
        private final float normalRange;  // detection radius when player upright
        private final float sneakRange;   // detection radius when player crouching
        private @Nullable Player threat;
        private int scanCooldown;

        DeerShyGoal(PathfinderMob mob, float normalRange, float sneakRange) {
            this.mob = mob;
            this.normalRange = normalRange;
            this.sneakRange  = sneakRange;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean holdsBerriesInHands(Player p) {
            return p.getMainHandItem().is(Items.SWEET_BERRIES)
                || p.getOffhandItem().is(Items.SWEET_BERRIES);
        }

        @Override
        public boolean canUse() {
            return scan() != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (threat == null || !threat.isAlive() || holdsBerriesInHands(threat)) {
                return scan() != null;
            }
            float r = threat.isCrouching() ? sneakRange : normalRange;
            return mob.distanceToSqr(threat) < (r * r);
        }

        /** Rate-limited scan; returns nearest threatening player or null. */
        private @Nullable Player scan() {
            // 10-tick cooldown — twice as reactive as before, still TPS-friendly
            if (scanCooldown > 0) { scanCooldown--; return threat; }
            scanCooldown = 10;

            List<Player> nearby = mob.level().getEntitiesOfClass(
                Player.class,
                mob.getBoundingBox().inflate(normalRange, 4.0, normalRange),
                p -> !p.isCreative() && !p.isSpectator() && !holdsBerriesInHands(p));

            threat = null;
            double closestSq = Double.MAX_VALUE;
            for (Player p : nearby) {
                float r = p.isCrouching() ? sneakRange : normalRange;
                double dSq = mob.distanceToSqr(p);
                if (dSq < r * r && dSq < closestSq) {
                    closestSq = dSq;
                    threat = p;
                }
            }
            return threat;
        }

        @Override
        public void stop() {
            threat = null;
            scanCooldown = 0;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (threat == null) return;
            // Re-path every 10 ticks — smooth enough, avoids nav spam
            if (mob.tickCount % 10 == 0) {
                // Flee 20 blocks away so the deer gets genuinely far
                Vec3 dest = DefaultRandomPos.getPosAway(mob, 20, 7, threat.position());
                if (dest != null) {
                    // Always sprint — deer run, not walk, from threats.
                    // Push harder (2.0) when the player is within 8 blocks (64 sq).
                    double speed = mob.distanceToSqr(threat) < 64.0 ? 2.0 : 1.6;
                    mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Instance goal — occasional grazing animation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Occasionally lowers the deer's head to graze.  Runs only when the mob
     * is standing still (walkSpeed ≈ 0) and is on the ground.  Each graze
     * lasts 40–80 ticks (2–4 seconds) and cannot repeat for at least 600
     * ticks (~30 s) after finishing.
     * TPS-friendly: no entity scanning; just a tick counter + random check.
     */
    static class DeerGrazeGoal extends Goal {

        private final PinkDeerEntity mob;
        // Ticks remaining in the current graze session
        private int grazeTicks;
        // Minimum ticks before the next graze can start
        private int cooldown;

        DeerGrazeGoal(PinkDeerEntity mob) {
            this.mob = mob;
            // No MOVE flag — deer stands still; LOOK not claimed either so
            // LookAtPlayerGoal can still run during idle grazing.
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) { cooldown--; return false; }
            // Never start grazing while hurt / on fire / freezing
            if (mob.getLastHurtByMob() != null || mob.isOnFire() || mob.isFreezing()) return false;
            // Low random chance each tick — roughly once every ~35 s on average
            // (~1/700 per tick × 20 ticks/s = ~1/35 s, slightly randomised)
            if (mob.getRandom().nextInt(700) != 0) return false;
            // Only graze when standing still and on solid ground
            return mob.onGround() && mob.getDeltaMovement().horizontalDistanceSqr() < 0.01;
        }

        @Override
        public boolean canContinueToUse() {
            // Stop immediately if the deer is hit — canContinueToUse returning false
            // triggers stop(), which clears the isGrazing flag before BoltGoal takes over.
            if (mob.getLastHurtByMob() != null || mob.isOnFire() || mob.isFreezing()) return false;
            return grazeTicks > 0;
        }

        @Override
        public void start() {
            // Duration: 60–120 ticks (3–6 s) — long enough for the slow,
            // graceful neck arc to complete a full dip without repeated bobbing.
            grazeTicks = 60 + mob.getRandom().nextInt(61);
            mob.setGrazing(true);
        }

        @Override
        public void stop() {
            mob.setGrazing(false);
            // Cooldown: 600–900 ticks (~30–45 s) before grazing again
            cooldown = 600 + mob.getRandom().nextInt(301);
            grazeTicks = 0;
        }

        @Override
        public void tick() {
            grazeTicks--;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Instance goal — only for PinkDeerEntity babies following mom
    // ═══════════════════════════════════════════════════════════════════════════

    private class FollowMomGoal extends Goal {

        private @Nullable MomPinkDeerEntity momTarget = null;
        private int scanCooldown = 0;

        public FollowMomGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!PinkDeerEntity.this.isBaby()) return false;
            return findMom() != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!PinkDeerEntity.this.isBaby()) return false;
            if (momTarget == null || !momTarget.isAlive()) momTarget = findMom();
            return momTarget != null && PinkDeerEntity.this.distanceToSqr(momTarget) > 9.0;
        }

        private @Nullable MomPinkDeerEntity findMom() {
            if (scanCooldown > 0) {
                scanCooldown--;
                // Discard stale target immediately so canContinueToUse() returns false
                return (momTarget != null && momTarget.isAlive()) ? momTarget : null;
            }
            scanCooldown = 20;

            // Simple loop — no Stream/Comparator/Optional allocations
            momTarget = null;
            double closestSq = Double.MAX_VALUE;
            for (MomPinkDeerEntity m : PinkDeerEntity.this.level().getEntitiesOfClass(
                    MomPinkDeerEntity.class,
                    PinkDeerEntity.this.getBoundingBox().inflate(10.0, 4.0, 10.0))) {
                double dSq = PinkDeerEntity.this.distanceToSqr(m);
                if (dSq < closestSq) {
                    closestSq = dSq;
                    momTarget = m;
                }
            }
            return momTarget;
        }

        @Override
        public void stop() {
            momTarget = null;
            scanCooldown = 0;
            PinkDeerEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (momTarget == null) return;
            if (PinkDeerEntity.this.tickCount % 10 == 0) {
                boolean momFleeing = momTarget.getLastHurtByMob() != null;
                boolean lagging    = PinkDeerEntity.this.distanceToSqr(momTarget) > 49.0;
                double speed = (momFleeing || lagging) ? 1.6 : 1.1;
                PinkDeerEntity.this.getNavigation().moveTo(momTarget, speed);
            }
        }
    }
}
