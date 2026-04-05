package kingdom.smp.entity;

import kingdom.smp.effect.*;
import kingdom.smp.entity.goal.MageRetreatGoal;
import kingdom.smp.entity.goal.SpellCastGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arcane Mage — spell-casting hostile using the same illager model/texture pipeline as
 * {@link ArcaneInvokerEntity} (illusioner geometry + {@code evil_evoker.png}).
 */
public class ArcaneMageEntity extends Illusioner {

    private static final EntityDataAccessor<Boolean> DATA_CASTING =
        SynchedEntityData.defineId(ArcaneMageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDED =
        SynchedEntityData.defineId(ArcaneMageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BARRIER_ACTIVE =
        SynchedEntityData.defineId(ArcaneMageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPELL_ID =
        SynchedEntityData.defineId(ArcaneMageEntity.class, EntityDataSerializers.INT);

    public enum SpellType {
        ARCANE_BARRAGE(0, 30, 60, "cast_barrage"),
        VOID_RIFT(1, 20, 160, "cast_rift"),
        SOUL_SHIELD(2, 15, 300, "cast_shield"),
        TELEPORT(3, 10, 100, "teleport");

        public final int id;
        public final int castTicks;
        public final int cooldownTicks;
        public final String animationName;

        SpellType(int id, int castTicks, int cooldownTicks, String animationName) {
            this.id = id;
            this.castTicks = castTicks;
            this.cooldownTicks = cooldownTicks;
            this.animationName = animationName;
        }
    }

    private final Map<SpellType, Integer> spellCooldowns = new HashMap<>();
    private int castAnimTick = 0;
    private SpellType activeSpell = null;

    private int shieldTicksRemaining = 0;
    private static final int SHIELD_DURATION = 100;

    private Vec3 riftTargetPos = null;
    private int riftTicksRemaining = 0;

    private int barrierVisualTick = 0;
    private int auraTick = 0;

    public ArcaneMageEntity(EntityType<? extends Illusioner> type, Level level) {
        super(type, level);
        for (SpellType spell : SpellType.values()) {
            spellCooldowns.put(spell, 0);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Illusioner.createAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING, false);
        builder.define(DATA_SHIELDED, false);
        builder.define(DATA_BARRIER_ACTIVE, false);
        builder.define(DATA_SPELL_ID, -1);
    }

    public boolean isCasting() {
        return this.entityData.get(DATA_CASTING);
    }

    public boolean isShielded() {
        return this.entityData.get(DATA_SHIELDED);
    }

    public boolean isBarrierActive() {
        return this.entityData.get(DATA_BARRIER_ACTIVE);
    }

    public int getSpellId() {
        return this.entityData.get(DATA_SPELL_ID);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> {
            String n = g.getClass().getName();
            return n.contains("IllusionerBlindnessSpellGoal") || g instanceof RangedBowAttackGoal;
        });
        this.goalSelector.addGoal(1, new SpellCastGoal(this, 16.0));
        this.goalSelector.addGoal(2, new MageRetreatGoal(this, 8.0, 1.0));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Crossed arms when idle; spell pose while casting (same UV intent as {@code evil_evoker.png}). */
    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isCasting()) {
            return AbstractIllager.IllagerArmPose.SPELLCASTING;
        }
        if (this.isCelebrating()) {
            return AbstractIllager.IllagerArmPose.CELEBRATING;
        }
        if (this.swinging) {
            return AbstractIllager.IllagerArmPose.ATTACKING;
        }
        return AbstractIllager.IllagerArmPose.CROSSED;
    }

    public boolean canUseSpell(SpellType spell) {
        return spellCooldowns.getOrDefault(spell, 0) <= 0;
    }

    public void startCasting(SpellType spell) {
        this.activeSpell = spell;
        this.castAnimTick = 0;
        this.entityData.set(DATA_CASTING, true);
        this.entityData.set(DATA_SPELL_ID, spell.id);
    }

    public void finishCasting() {
        if (activeSpell != null) {
            spellCooldowns.put(activeSpell, activeSpell.cooldownTicks);
        }
        this.activeSpell = null;
        this.castAnimTick = 0;
        this.entityData.set(DATA_CASTING, false);
        this.entityData.set(DATA_SPELL_ID, -1);
    }

    public void executeSpell(SpellType spell, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || target == null) return;

        switch (spell) {
            case ARCANE_BARRAGE -> executeArcaneBarrage(serverLevel, target);
            case VOID_RIFT -> executeVoidRift(serverLevel, target.position());
            case SOUL_SHIELD -> executeSoulShield(serverLevel);
            case TELEPORT -> executeTeleport(serverLevel, target);
        }
    }

    private void executeArcaneBarrage(ServerLevel level, LivingEntity target) {
        Vec3 castPos = this.position().add(0, 1.5, 0);
        ArcaneBarrageEffect.spawn(level, castPos, this, 15);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurtServer(level, level.damageSources().magic(), damage * 0.8f);

        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.8F);
    }

    private void executeVoidRift(ServerLevel level, Vec3 targetPos) {
        VoidRiftEffect.spawn(level, targetPos, this, 0);
        this.riftTargetPos = targetPos;
        this.riftTicksRemaining = 80;
        this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 0.5F);
    }

    private void executeSoulShield(ServerLevel level) {
        this.shieldTicksRemaining = SHIELD_DURATION;
        this.entityData.set(DATA_SHIELDED, true);
        SoulShieldEffect.spawn(level, this.position(), this, 0);
        this.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.2F);
    }

    private void executeTeleport(ServerLevel level, LivingEntity target) {
        Vec3 awayDir = this.position().subtract(target.position()).normalize();
        double dist = 8.0 + this.random.nextDouble() * 4.0;

        double newX = this.getX() + awayDir.x * dist;
        double newZ = this.getZ() + awayDir.z * dist;
        double newY = this.level()
            .getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(newX, this.getY(), newZ))
            .getY();

        TeleportBurstEffect.spawn(level, this.position().add(0, 1, 0), this, 0);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);

        this.teleportTo(newX, newY, newZ);

        TeleportBurstEffect.spawn(level, this.position().add(0, 1, 0), this, 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isShielded()) {
            amount *= 0.5f;
        }

        if (isBarrierActive() && source.getDirectEntity() instanceof Projectile projectile) {
            deflectProjectile(projectile);
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    private void deflectProjectile(Projectile projectile) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Vec3 motion = projectile.getDeltaMovement();
        projectile.setDeltaMovement(motion.scale(-0.6));
        projectile.setOwner(null);

        Vec3 hitPos = projectile.position();
        serverLevel.sendParticles(
            new DustParticleOptions(ARGB.colorFromFloat(1f, 0.3f, 0.6f, 1f), 1.5f),
            hitPos.x, hitPos.y, hitPos.z, 12, 0.3, 0.3, 0.3, 0.1);
        serverLevel.sendParticles(
            ParticleTypes.ENCHANTED_HIT,
            hitPos.x, hitPos.y, hitPos.z, 8, 0.2, 0.2, 0.2, 0.05);

        this.playSound(SoundEvents.SHIELD_BLOCK.value(), 1.2F, 1.5F);
    }

    @Override
    public void tick() {
        super.tick();

        for (SpellType spell : SpellType.values()) {
            int cd = spellCooldowns.getOrDefault(spell, 0);
            if (cd > 0) spellCooldowns.put(spell, cd - 1);
        }

        if (isCasting()) castAnimTick++;

        if (shieldTicksRemaining > 0) {
            shieldTicksRemaining--;
            if (this.level() instanceof ServerLevel sl) {
                SoulShieldEffect.spawn(sl, this.position(), this, SHIELD_DURATION - shieldTicksRemaining);
            }
            if (shieldTicksRemaining <= 0) {
                this.entityData.set(DATA_SHIELDED, false);
            }
        }

        if (riftTicksRemaining > 0 && riftTargetPos != null) {
            riftTicksRemaining--;
            if (this.level() instanceof ServerLevel sl) {
                VoidRiftEffect.spawn(sl, riftTargetPos, this, 80 - riftTicksRemaining);

                if (riftTicksRemaining < 60 && riftTicksRemaining % 10 == 0) {
                    AABB riftArea = new AABB(
                        riftTargetPos.x - 2.5, riftTargetPos.y - 1, riftTargetPos.z - 2.5,
                        riftTargetPos.x + 2.5, riftTargetPos.y + 4, riftTargetPos.z + 2.5);
                    List<LivingEntity> victims =
                        this.level().getEntitiesOfClass(LivingEntity.class, riftArea, e -> e != this);
                    for (LivingEntity victim : victims) {
                        victim.hurtServer(sl, sl.damageSources().magic(), 4.0f);
                        Vec3 pull = riftTargetPos.subtract(victim.position()).normalize().scale(0.15);
                        victim.push(pull.x, 0.05, pull.z);
                    }
                }
            }
            if (riftTicksRemaining <= 0) riftTargetPos = null;
        }

        boolean shouldBarrier = this.getHealth() < this.getMaxHealth() * 0.5f;
        if (shouldBarrier != isBarrierActive()) {
            this.entityData.set(DATA_BARRIER_ACTIVE, shouldBarrier);
            if (shouldBarrier && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                    new DustParticleOptions(ARGB.colorFromFloat(1f, 0.3f, 0.6f, 1f), 2.0f),
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    30, 1.5, 1.5, 1.5, 0.05);
                this.playSound(SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1.5F, 0.6F);
            }
        }

        if (isBarrierActive() && this.level() instanceof ServerLevel sl) {
            barrierVisualTick++;
            ProjectileBarrierEffect.spawn(sl, this.position(), this, barrierVisualTick);
        }

        if (this.level() instanceof ServerLevel sl) {
            auraTick++;
            PassiveAuraEffect.spawn(sl, this.position(), this, auraTick);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("ShieldTicks", shieldTicksRemaining);
        out.putInt("RiftTicks", riftTicksRemaining);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.shieldTicksRemaining = in.getIntOr("ShieldTicks", 0);
        this.riftTicksRemaining = in.getIntOr("RiftTicks", 0);
        if (shieldTicksRemaining > 0) this.entityData.set(DATA_SHIELDED, true);
    }
}
