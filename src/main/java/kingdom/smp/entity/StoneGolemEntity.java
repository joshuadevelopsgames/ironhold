package kingdom.smp.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Stone Golem — a hulking, slow, heavily-armoured boss that wields the
 * {@link kingdom.smp.item.BattleHammerItem battle hammer} two-handed and fights with
 * telegraphed slams and sweeps ({@link StoneGolemAttackGoal}).
 *
 * <p>Rendered via GeckoLib on a jointed rig (elbows / knees / waist). Two animation controllers:
 * a {@code movement} controller (idle/walk, speed-synced to ground velocity) and an {@code action}
 * controller that plays triggered one-shots (slam, sweep, stagger) over the top. A poise meter turns
 * accumulated burst damage into a {@code stagger} stun — the boss's punish window.
 */
public class StoneGolemEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("slam");
    private static final RawAnimation SWEEP = RawAnimation.begin().thenPlay("sweep");
    private static final RawAnimation STAGGER = RawAnimation.begin().thenPlay("stagger");
    private static final RawAnimation FIDGET_TAP = RawAnimation.begin().thenPlay("fidget_tap");
    private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("activate");

    private static final double WAKE_RANGE = 12.0;   // a player this close wakes the dormant statue
    private static final int ACTIVATE_TICKS = 32;    // rooted while the wake-up animation plays (~1.6s)
    private static final float ENRAGE_FRAC = 0.4f;   // enrages below 40% health

    private static final float POISE_MAX = 36f;      // burst damage needed to stagger
    private static final int STAGGER_TICKS = 32;     // ~1.6s, matches the stagger animation
    private static final int STAGGER_IMMUNE = 120;   // can't be re-staggered for 6s after

    /** Attack wind-up charge 0..1, synced to clients to drive the eye-glow swell + strike flare. */
    private static final EntityDataAccessor<Float> CHARGE =
        SynchedEntityData.defineId(StoneGolemEntity.class, EntityDataSerializers.FLOAT);
    /** Whether the golem has woken from its dormant statue state (synced — drives AI, eye-glow, bar). */
    private static final EntityDataAccessor<Boolean> AWAKE =
        SynchedEntityData.defineId(StoneGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
        Component.translatable("entity.ironhold.stone_golem"),
        BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    private boolean enraged;
    private int wakeLock; // ticks remaining of the wake-up animation, during which it can't act

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Dev slow-mo multiplier for the action (attack) controller; 1.0 = normal. */
    public static volatile float DEBUG_ACTION_SPEED = 1.0f;

    private int staggerTicks;
    private int staggerImmuneTicks;
    private float poise;
    private int fidgetCooldown = 120; // ticks until the next bored hammer-tap when standing idle
    private final List<Shockwave> shockwaves = new ArrayList<>(); // active slam shockwaves (server)

    public StoneGolemEntity(EntityType<? extends StoneGolemEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.LAVA, 8.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE, 0f);
        builder.define(AWAKE, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Awake", isAwake());
        out.putBoolean("Enraged", enraged);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(AWAKE, in.getBooleanOr("Awake", false));
        this.enraged = in.getBooleanOr("Enraged", false);
        if (enraged) {
            bossBar.setColor(BossEvent.BossBarColor.RED);
        }
    }

    public boolean isAwake() {
        return this.entityData.get(AWAKE);
    }

    public boolean isEnraged() {
        return enraged;
    }

    public boolean isWaking() {
        return wakeLock > 0;
    }

    /** Wind-up charge 0..1 (synced). 0 = idle, ramps to 1 across an attack wind-up, flares on strike. */
    public float getCharge() {
        return this.entityData.get(CHARGE);
    }

    public void setCharge(float value) {
        this.entityData.set(CHARGE, Mth.clamp(value, 0f, 1f));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.STEP_HEIGHT, 1.5)
                // A true giant: SCALE doubles the rendered model AND the hitbox / eye height / step
                // height. Attack reach, AoE radii and FX offsets are scaled to match in code below.
                .add(Attributes.SCALE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new StoneGolemAttackGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Mob.class, 8, true, false,
                (t, lvl) -> t instanceof AbstractVillager && !(t instanceof Creeper)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BATTLE_HAMMER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    /**
     * Heavy head: the golem swivels its stone head only so far (45° vs the vanilla 75°) before the
     * whole body has to heave around to follow — read as ponderous mass rather than an owl's neck.
     * The body-follow itself is vanilla ({@link net.minecraft.world.entity.Mob#clampHeadRotationToBody()}
     * rotates {@code yBodyRot} once the head hits this limit); the rendered head turn comes from the
     * {@code query.head_y_rotation}/{@code query.head_x_rotation} molang on the {@code head} bone.
     */
    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    // ── Poise / stagger ───────────────────────────────────────────────────────

    public boolean isStaggered() {
        return staggerTicks > 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isAwake() && this.isAlive()) {
            wake(); // striking the statue wakes it
        }
        boolean took = super.hurtServer(level, source, amount);
        if (took && amount > 0 && !isStaggered() && staggerImmuneTicks <= 0 && this.isAlive()) {
            poise += amount;
            if (poise >= POISE_MAX) {
                enterStagger();
            }
        }
        return took;
    }

    private void enterStagger() {
        poise = 0f;
        staggerTicks = STAGGER_TICKS;
        staggerImmuneTicks = STAGGER_IMMUNE;
        setCharge(0f); // a stagger interrupts any wind-up; kill the glow swell
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        this.triggerAnim("action", "stagger");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        bossBar.setProgress(Mth.clamp(this.getHealth() / Math.max(1f, this.getMaxHealth()), 0f, 1f));
        bossBar.setVisible(isAwake());

        if (!isAwake()) {
            // Dormant statue: holds still until a player strays too close (or strikes it).
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.setTarget(null);
            Player near = this.level().getNearestPlayer(this, WAKE_RANGE);
            if (near != null && near.isAlive() && !near.isCreative() && !near.isSpectator()
                    && this.hasLineOfSight(near)) {
                wake();
            }
            return;
        }

        if (wakeLock > 0) { // mid wake-up animation: rooted, not yet attacking
            wakeLock--;
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }
        if (staggerImmuneTicks > 0) {
            staggerImmuneTicks--;
        }
        if (staggerTicks > 0) {
            staggerTicks--;
            this.getNavigation().stop();
        } else if (poise > 0f) {
            poise = Math.max(0f, poise - 0.25f); // slow poise regen between hits
        }
        tickFidget();
        if (!shockwaves.isEmpty() && this.level() instanceof ServerLevel sl) {
            tickShockwaves(sl);
        }
        if (!enraged && this.getHealth() < ENRAGE_FRAC * this.getMaxHealth()) {
            enrage();
        }
    }

    /** Statue → boss: flare the eyes, shudder to life, and lock briefly while the wake-up plays. */
    private void wake() {
        this.entityData.set(AWAKE, true);
        this.wakeLock = ACTIVATE_TICKS;
        this.triggerAnim("action", "activate");
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.7f, 0.4f);
            sl.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 2.0f, 0.35f);
            sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.3, this.getZ(),
                    50, 1.8, 0.5, 1.8, 0.06);
        }
    }

    /** Below 40% health: faster, redder, and it roars. */
    private void enrage() {
        enraged = true;
        bossBar.setColor(BossEvent.BossBarColor.RED);
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.35);
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.6f);
            sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 3.0, this.getZ(),
                    40, 1.4, 1.2, 1.4, 0.06);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    // ── Slam shockwave ─────────────────────────────────────────────────────────
    // An expanding ground wave that resolves the giant's slam AoE over time: damage lands as the
    // wavefront sweeps over each target (once), so the hit is readable and dodgeable — the Mowzie's
    // way of doing big AoE — instead of an instant radius. Driven server-side from the golem's tick.

    /** Launch an expanding shockwave centred at {@code center}; damage lands as the wavefront passes. */
    public void spawnShockwave(Vec3 center, double maxRadius, float damage) {
        shockwaves.add(new Shockwave(center, maxRadius, maxRadius / 13.0, damage)); // ~0.65s to full
    }

    private void tickShockwaves(ServerLevel sl) {
        BlockParticleOption stone =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        Iterator<Shockwave> it = shockwaves.iterator();
        while (it.hasNext()) {
            Shockwave sw = it.next();
            double prev = sw.radius;
            sw.radius = Math.min(sw.maxRadius, sw.radius + sw.growth);
            // Damage everything the wavefront swept over this tick, once each, knocked up-and-out.
            AABB band = new AABB(sw.center.x - sw.radius - 1, sw.center.y - 3, sw.center.z - sw.radius - 1,
                    sw.center.x + sw.radius + 1, sw.center.y + 4, sw.center.z + sw.radius + 1);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, band)) {
                if (e == this || e instanceof StoneGolemEntity || this.isAlliedTo(e) || sw.hit.contains(e.getId())) {
                    continue;
                }
                double dx = e.getX() - sw.center.x, dz = e.getZ() - sw.center.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= prev && d <= sw.radius && Math.abs(e.getY() - sw.center.y) < 3.5) {
                    sw.hit.add(e.getId());
                    e.hurtServer(sl, this.damageSources().mobAttack(this), sw.damage);
                    e.knockback(1.1, sw.center.x - e.getX(), sw.center.z - e.getZ()); // outward
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.55, 0));          // launch up
                }
            }
            // Erupting ring of stone dust at the wavefront.
            int points = Mth.clamp((int) (sw.radius * 3.5), 14, 30);
            for (int i = 0; i < points; i++) {
                double a = (i / (double) points) * Math.PI * 2;
                double px = sw.center.x + Math.cos(a) * sw.radius;
                double pz = sw.center.z + Math.sin(a) * sw.radius;
                sl.sendParticles(stone, px, sw.center.y + 0.1, pz, 2, 0.15, 0.1, 0.15, 0.08);
                if ((i & 3) == 0) {
                    sl.sendParticles(ParticleTypes.POOF, px, sw.center.y + 0.2, pz, 1, 0.1, 0.05, 0.1, 0.02);
                }
            }
            if (sw.radius >= sw.maxRadius) {
                it.remove();
            }
        }
    }

    private static final class Shockwave {
        final Vec3 center;
        final double maxRadius;
        final double growth;
        final float damage;
        double radius;
        final java.util.Set<Integer> hit = new java.util.HashSet<>();

        Shockwave(Vec3 center, double maxRadius, double growth, float damage) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.growth = growth;
            this.damage = damage;
        }
    }

    /** When standing around out of combat, the golem gets bored and taps the hammer on its offhand. */
    private void tickFidget() {
        boolean idle = staggerTicks <= 0 && getCharge() <= 0f && getTarget() == null
                && this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() < 1.0e-4;
        if (!idle) {
            fidgetCooldown = Math.max(fidgetCooldown, 40); // re-arm a short delay after each disturbance
            return;
        }
        if (--fidgetCooldown <= 0) {
            this.triggerAnim("fidget", "fidget_tap");
            fidgetCooldown = 160 + this.getRandom().nextInt(220); // next bored tap in ~8–19s
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, this::movementPredicate)
                .setSoundKeyframeHandler(e -> onSound(e.animatable(), e.keyframeData().getSound()))
                .setParticleKeyframeHandler(e -> onParticle(e.animatable(), e.keyframeData().getEffect())));
        controllers.add(new AnimationController<StoneGolemEntity>("action", 3, state -> {
                    // Dev slow-mo so attack poses can be studied frame-by-frame (1.0 = normal).
                    state.controller().setAnimationSpeed(DEBUG_ACTION_SPEED);
                    return PlayState.STOP;
                })
                .triggerableAnim("slam", SLAM)
                .triggerableAnim("sweep", SWEEP)
                .triggerableAnim("stagger", STAGGER)
                .triggerableAnim("activate", ACTIVATE)
                .setSoundKeyframeHandler(e -> onSound(e.animatable(), e.keyframeData().getSound()))
                .setParticleKeyframeHandler(e -> onParticle(e.animatable(), e.keyframeData().getEffect())));
        // Idle "fidget" layer: occasional bored hammer-tap, played over the across-body idle hold.
        controllers.add(new AnimationController<StoneGolemEntity>("fidget", 5, state -> PlayState.STOP)
                .triggerableAnim("fidget_tap", FIDGET_TAP)
                .setSoundKeyframeHandler(e -> onSound(e.animatable(), e.keyframeData().getSound())));
    }

    // ── Animation-synced footstep / impact FX (client-side keyframe handlers) ──
    // Driven by sound_effects/particle_effects keyframes baked into the animation JSON,
    // so the dust and footfalls land exactly on the foot-plant and impact frames.

    private static void onSound(StoneGolemEntity golem, String id) {
        Level level = golem.level();
        if (!level.isClientSide()) {
            return;
        }
        double x = golem.getX(), y = golem.getY(), z = golem.getZ();
        switch (id) {
            case "step" -> level.playLocalSound(x, y, z, SoundEvents.IRON_GOLEM_STEP,
                    SoundSource.HOSTILE, 0.7f, 0.62f, false);
            case "windup" -> level.playLocalSound(x, y, z, SoundEvents.IRON_GOLEM_STEP,
                    SoundSource.HOSTILE, 0.55f, 0.42f, false);
            case "stagger" -> level.playLocalSound(x, y, z, SoundEvents.STONE_BREAK,
                    SoundSource.HOSTILE, 0.9f, 0.55f, false);
            case "tap" -> level.playLocalSound(x, y, z, SoundEvents.STONE_HIT,
                    SoundSource.HOSTILE, 0.5f, 0.85f, false);
            default -> { }
        }
    }

    private void onParticle(StoneGolemEntity golem, String id) {
        Level level = golem.level();
        if (!level.isClientSide()) {
            return;
        }
        Vec3 fwd = Vec3.directionFromRotation(0f, golem.getYRot());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        switch (id) {
            case "dust_left" -> footDust(level, golem.position().add(right.scale(-1.4)).add(fwd.scale(0.4)));
            case "dust_right" -> footDust(level, golem.position().add(right.scale(1.4)).add(fwd.scale(0.4)));
            case "slam_impact" -> impactDust(level, golem.position().add(fwd.scale(4.0)), 40, 2.8);
            case "sweep_impact" -> impactDust(level, golem.position().add(fwd.scale(3.2)), 28, 3.6);
            default -> { }
        }
    }

    private static void footDust(Level level, Vec3 at) {
        BlockParticleOption stone =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        var rng = level.getRandom();
        for (int i = 0; i < 12; i++) {
            double ox = (rng.nextDouble() - 0.5) * 1.6;
            double oz = (rng.nextDouble() - 0.5) * 1.6;
            level.addParticle(stone, at.x + ox, at.y + 0.05, at.z + oz,
                    ox * 0.4, 0.05 + rng.nextDouble() * 0.06, oz * 0.4);
        }
        level.addParticle(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, 0, 0.02, 0);
    }

    private static void impactDust(Level level, Vec3 at, int count, double spread) {
        BlockParticleOption stone =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        var rng = level.getRandom();
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double r = rng.nextDouble() * spread;
            double vx = Math.cos(a) * (0.12 + rng.nextDouble() * 0.18);
            double vz = Math.sin(a) * (0.12 + rng.nextDouble() * 0.18);
            level.addParticle(stone, at.x + Math.cos(a) * r * 0.3, at.y + 0.1, at.z + Math.sin(a) * r * 0.3,
                    vx, 0.06 + rng.nextDouble() * 0.12, vz);
        }
    }

    private PlayState movementPredicate(AnimationTest<StoneGolemEntity> test) {
        if (!isAwake()) {
            test.controller().setAnimationSpeed(1.0);
            test.controller().setAnimation(DORMANT);
            return PlayState.CONTINUE;
        }
        if (test.isMoving()) {
            // Sync leg cadence to actual ground speed so the feet don't slide.
            double speed = this.getDeltaMovement().horizontalDistance();
            test.controller().setAnimationSpeed(Mth.clamp(speed * 7.0, 0.55, 1.8));
            test.controller().setAnimation(WALK);
        } else {
            test.controller().setAnimationSpeed(1.0);
            test.controller().setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
