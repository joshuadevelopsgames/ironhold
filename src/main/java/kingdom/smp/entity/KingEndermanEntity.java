package kingdom.smp.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * King Enderman — endgame raid boss.
 *
 * <p>Three combat phases gated by HP milestones, with a "pylon shield" interlude
 * triggered as the boss crosses each milestone:
 * <pre>
 *   100% ─────── 66% ──── 33% ──── 0%
 *       PHASE_1   PHASE_2   PHASE_3
 *               △         △
 *               │         │
 *           pylon       pylon       (shielded; 4 end crystals must be destroyed)
 * </pre>
 *
 * <p>Combat moves are driven by {@link KingEndermanCombatGoal} — a move-picker
 * AI that fires melee sweeps, 4-arm pearl volleys, and predictive telesmashes
 * based on phase + distance + cooldowns.
 *
 * <p>Reactive behaviors layered on the entity itself:
 * <ul>
 *   <li><b>Punisher teleport</b>: hit by a projectile from outside line of sight → blink behind attacker.</li>
 *   <li><b>Projectile mitigation</b>: incoming projectile damage scaled to 0.55× (encourages melee).</li>
 *   <li><b>Pylon shield</b>: blocks all damage while shielded; 4 end crystals tether the king's invulnerability.</li>
 *   <li><b>Crystal-driven heal</b>: while shielded, regenerates HP per surviving crystal — kill them fast or he claws back the fight.</li>
 * </ul>
 *
 * <p>Extends {@link Monster} (not {@link EnderMan}) so vanilla teleport-on-hit
 * behavior is replaced with our targeted punisher TP.
 */
public class KingEndermanEntity extends Monster {

    public enum Phase { PHASE_1, PHASE_2, PHASE_3, PYLON_SHIELD }

    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
        SynchedEntityData.defineId(KingEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_PHASE =
        SynchedEntityData.defineId(KingEndermanEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_TELESMASH_CHARGING =
        SynchedEntityData.defineId(KingEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TELESMASH_FALLING =
        SynchedEntityData.defineId(KingEndermanEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float MILESTONE_2 = 0.66F;
    private static final float MILESTONE_3 = 0.33F;
    private static final int   PYLON_CRYSTAL_COUNT = 4;
    private static final double PYLON_RADIUS = 6.0;

    /** ticks after entity creation; we don't tick the millisecond clock so this stays deterministic. */
    private final ServerBossEvent bossBar;

    private final List<UUID> pylonCrystalIds = new ArrayList<>();
    private float pylonShieldHp;            // HP at shield engagement; HP snaps to this on activation
    private boolean milestone2Triggered;
    private boolean milestone3Triggered;

    /** Counts down between boss-music plays. Reset to 0 when no players are
     *  in the bossbar so music starts immediately on the first audience join. */
    private int musicCycleTicks;

    public KingEndermanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            UUID.randomUUID(),
            net.minecraft.network.chat.Component.literal("King Enderman"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 250;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 600.0)
            .add(Attributes.ATTACK_DAMAGE, 22.0)
            .add(Attributes.ATTACK_KNOCKBACK, 2.5)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.ARMOR_TOUGHNESS, 6.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ENRAGED, false);
        builder.define(DATA_PHASE, (byte) Phase.PHASE_1.ordinal());
        builder.define(DATA_TELESMASH_CHARGING, false);
        builder.define(DATA_TELESMASH_FALLING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KingEndermanCombatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // -------- Phase / state accessors --------------------------------------

    public Phase getPhase() {
        return Phase.values()[this.entityData.get(DATA_PHASE)];
    }

    private void setPhase(Phase phase) {
        this.entityData.set(DATA_PHASE, (byte) phase.ordinal());
    }

    public boolean isInPylonShield() {
        return getPhase() == Phase.PYLON_SHIELD;
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    private void setEnraged(boolean enraged) {
        this.entityData.set(DATA_ENRAGED, enraged);
    }

    public boolean isTelesmashCharging() {
        return this.entityData.get(DATA_TELESMASH_CHARGING);
    }

    public void setTelesmashCharging(boolean v) {
        this.entityData.set(DATA_TELESMASH_CHARGING, v);
    }

    public boolean isTelesmashFalling() {
        return this.entityData.get(DATA_TELESMASH_FALLING);
    }

    public void setTelesmashFalling(boolean v) {
        this.entityData.set(DATA_TELESMASH_FALLING, v);
    }

    // -------- Tick ---------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        // Visual flag for the model: enraged once we're in phase 3.
        boolean shouldEnrage = getPhase() == Phase.PHASE_3;
        if (shouldEnrage != isEnraged()) {
            setEnraged(shouldEnrage);
            if (shouldEnrage) {
                // Phase-3 entry: big stacked audio cue.
                this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.5F);
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5F, 0.8F);
                this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.7F);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30);
            }
        }

        bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        // Combat ambient: occasional growls, plus a heartbeat in phase 3 for menace.
        tickCombatAmbient();
        // Boss music — replays on a cycle for as long as the bossbar has an audience.
        tickBossMusic();

        // HP-milestone phase transitions.
        float hpFraction = this.getHealth() / this.getMaxHealth();
        if (!milestone2Triggered && hpFraction <= MILESTONE_2) {
            milestone2Triggered = true;
            beginPylonShield(MILESTONE_2);
        } else if (!milestone3Triggered && hpFraction <= MILESTONE_3) {
            milestone3Triggered = true;
            beginPylonShield(MILESTONE_3);
        }

        if (isInPylonShield()) tickPylonShield();

        // Telesmash falling — accelerate downward fall for dramatic crash.
        if (isTelesmashFalling()) {
            Vec3 v = this.getDeltaMovement();
            if (v.y > -1.6) {
                this.setDeltaMovement(v.x * 0.95, v.y - 0.18, v.z * 0.95);
            }
            this.fallDistance = 0;
        }
    }

    // -------- Pylon shield mechanic ----------------------------------------

    private void beginPylonShield(float lockFraction) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        setPhase(Phase.PYLON_SHIELD);
        pylonShieldHp = this.getMaxHealth() * lockFraction;
        this.setHealth(pylonShieldHp);
        this.getNavigation().stop();
        this.setTarget(null);
        // Big phase-cue stack: scream + wither spawn + thunder for impact.
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.6F);
        this.playSound(SoundEvents.WITHER_SPAWN, 2.8F, 0.6F);
        this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);

        // Spawn 4 end crystals around the king at cardinal positions.
        pylonCrystalIds.clear();
        Vec3 center = this.position();
        for (int i = 0; i < PYLON_CRYSTAL_COUNT; i++) {
            double angle = i * (Math.PI * 2 / PYLON_CRYSTAL_COUNT);
            double dx = Math.cos(angle) * PYLON_RADIUS;
            double dz = Math.sin(angle) * PYLON_RADIUS;
            EndCrystal crystal = new EndCrystal(sl, center.x + dx, center.y + 1, center.z + dz);
            crystal.setShowBottom(true);
            crystal.setBeamTarget(this.blockPosition());
            sl.addFreshEntity(crystal);
            pylonCrystalIds.add(crystal.getUUID());
            sl.sendParticles(ParticleTypes.PORTAL,
                center.x + dx, center.y + 1, center.z + dz, 30, 0.5, 0.5, 0.5, 0.4);

            // Spawn 1 endermite next to each crystal so the player has something to fight
            // during the shield phase — keeps tempo, prevents "stand and wait" downtime.
            Endermite mite = EntityType.ENDERMITE.create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (mite != null) {
                mite.setPos(center.x + dx, center.y + 1, center.z + dz);
                mite.setYRot(this.random.nextFloat() * 360F);
                sl.addFreshEntity(mite);
            }
        }
    }

    private void tickPylonShield() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // Count surviving crystals — they tether the king's healing.
        int alive = 0;
        for (UUID id : pylonCrystalIds) {
            Entity e = sl.getEntity(id);
            if (e instanceof EndCrystal c && c.isAlive() && !c.isRemoved()) alive++;
        }

        if (alive == 0) {
            endPylonShield();
            return;
        }

        // Heal proportional to surviving crystals — 0.6 HP per crystal per second.
        // 4 crystals = 2.4 HP/sec, dropping as the player kills them. Caps at max HP.
        if (this.tickCount % 20 == 0) {
            float healAmount = 0.6F * alive;
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(healAmount);
                // Sparkle to show the heal is happening (so the player understands the urgency).
                sl.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + 4.5, this.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
            }
        }

        // Periodic shield crackle visuals.
        if (this.tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY() + 2, this.getZ(), 6, 1.5, 1.5, 1.5, 0.05);
        }
    }

    private void endPylonShield() {
        // Resume in the next phase down.
        Phase next = milestone3Triggered ? Phase.PHASE_3 : Phase.PHASE_2;
        setPhase(next);
        pylonCrystalIds.clear();
        this.playSound(SoundEvents.WITHER_BREAK_BLOCK, 3.0F, 0.5F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 2, this.getZ(), 6, 1.5, 1.5, 1.5, 0);
        }
    }

    // -------- Boss music --------------------------------------------------

    /** Plays the vanilla Ender Dragon track to every player tracking the bossbar.
     *  Loops on a ~5.7-minute cycle so the music doesn't peter out mid-fight.
     *  Uses {@link SoundSource#MUSIC} so it respects each player's music volume slider. */
    private void tickBossMusic() {
        var audience = bossBar.getPlayers();
        if (audience.isEmpty()) {
            // Reset so a fresh entry into the bossbar starts music immediately.
            musicCycleTicks = 0;
            return;
        }
        if (musicCycleTicks > 0) {
            musicCycleTicks--;
            return;
        }
        for (ServerPlayer p : audience) {
            if (p.level() instanceof ServerLevel sl) {
                sl.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.MUSIC_DRAGON.value(), SoundSource.MUSIC, 1.5F, 1.0F);
            }
        }
        musicCycleTicks = 6800;  // ~5.7 minutes; dragon track is ~6 minutes, leave a tiny gap.
    }

    // -------- Combat ambient (growls + heartbeat) -------------------------

    private void tickCombatAmbient() {
        if (this.getTarget() == null) return;
        // Random low growls every ~6s, more frequent when enraged.
        int growlPeriod = isEnraged() ? 80 : 140;
        if (this.tickCount % growlPeriod == 0 && this.random.nextFloat() < 0.6F) {
            this.playSound(SoundEvents.ENDERMAN_AMBIENT, 1.4F, 0.45F + this.random.nextFloat() * 0.15F);
        }
        // Phase-3 only: warden heartbeat every 1.6s for menace.
        if (isEnraged() && this.tickCount % 32 == 0) {
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 0.7F);
        }
    }

    // -------- Damage handler ----------------------------------------------

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        if (effect.is(net.minecraft.world.effect.MobEffects.POISON)) return false;
        if (effect.is(net.minecraft.world.effect.MobEffects.WITHER)) return false;
        return super.canBeAffected(effect);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Pylon shield: zero out incoming damage entirely.
        if (isInPylonShield()) {
            // Visual feedback so the player understands the boss is currently shielded.
            level.sendParticles(ParticleTypes.ENCHANT,
                this.getX(), this.getY() + 2, this.getZ(), 8, 0.6, 1.0, 0.6, 0.5);
            return false;
        }

        // Projectile mitigation (encourages melee engagement).
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount *= 0.55F;
            tryPunisherTeleport(level, source);
        }
        return super.hurtServer(level, source, amount);
    }

    /** If the projectile attacker is outside line of sight, blink behind them. */
    private void tryPunisherTeleport(ServerLevel level, DamageSource source) {
        if (getPhase() == Phase.PHASE_1) return;          // unlocks at phase 2
        Entity direct = source.getDirectEntity();         // the projectile
        if (!(direct instanceof Projectile)) return;
        Entity attacker = source.getEntity();             // shooter
        if (!(attacker instanceof LivingEntity living)) return;
        if (this.random.nextFloat() > 0.45F) return;      // 45% chance, don't be oppressive

        if (!hasLineOfSight(level, living)) {
            // Teleport behind the attacker, 2 blocks away, facing them.
            Vec3 attackerLook = living.getLookAngle().normalize();
            Vec3 behind = living.position().subtract(attackerLook.scale(3.0));
            this.teleportTo(behind.x, behind.y, behind.z);
            this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, living.getEyePosition());
            this.setTarget(living);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.5F);
            level.sendParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY() + 1, this.getZ(), 40, 0.5, 1.0, 0.5, 0.4);
        }
    }

    private boolean hasLineOfSight(ServerLevel level, LivingEntity other) {
        Vec3 from = this.getEyePosition();
        Vec3 to = other.getEyePosition();
        HitResult hit = level.clip(new ClipContext(from, to,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS
            || hit.getType() == HitResult.Type.ENTITY;
    }

    // -------- Boss bar lifecycle ------------------------------------------

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

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }
}
