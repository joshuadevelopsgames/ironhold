package kingdom.smp.entity;

import kingdom.smp.game.AnkhShieldBarrier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import java.util.UUID;

/**
 * Illusioner-based hostile wizard with three spells, a ranged magic bolt,
 * and stages of invulnerability that intensify as health drops.
 * <p>
 * Each major spell favors one particle type for visual clarity:
 * <ul>
 *   <li><b>Blink</b> — {@code REVERSE_PORTAL} — teleport + AoE burst at origin</li>
 *   <li><b>Nova</b> — {@code END_ROD} — expanding shockwave rings + pillar</li>
 *   <li><b>Hex</b> — {@code SOUL_FIRE_FLAME} — converging cage + curse projectile</li>
 *   <li><b>Resize</b> (uncommon) — dual {@code DRAGON_BREATH} corkscrew swirls — random {@link Attributes#SCALE} 0.3–2.0</li>
 * </ul>
 */
public class ArcaneWizardEntity extends Illusioner {
    protected static final ParticleOptions DRAGON_BREATH_SWIRL =
        PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 0.75F);

    private int arcaneInvulnTicks;
    private @Nullable WizardSpell pendingSpell;
    private @Nullable UUID linkedWizardUuid;
    private boolean canDuplicate = true;
    private boolean suppressLinkedSyncDamage;
    private int lastDouseTick = -200;
    private int fireWindowStartTick = -200;

    public ArcaneWizardEntity(EntityType<? extends Illusioner> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    /**
     * Evoker-style poses: crossed arms when idle (matches {@code evil_evoker.png} UVs). Vanilla illusioner
     * would use bow pose when aggressive even without a bow.
     */
    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isCastingSpell()) {
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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return result;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> {
            String n = g.getClass().getName();
            return n.contains("IllusionerBlindnessSpellGoal")
                || g instanceof RangedBowAttackGoal;
        });
        this.goalSelector.addGoal(4, new ArcaneSpellGoal());
        this.goalSelector.addGoal(5, new RangedAttackGoal(this, 1.0, 40, 15.0F));
    }

    /** Magic bolt — fires a slow projectile between spell cooldowns via {@link RangedAttackGoal}. */
    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 from = this.position().add(0, 1.4, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = to.subtract(from);
        ArcaneBoltEntity bolt = new ArcaneBoltEntity(this, dir, sl);
        bolt.setPos(from);
        sl.addFreshEntity(bolt);
        burst(sl, from, ParticleTypes.ENCHANT, 4, 0.1, 0.1, 0.1, 0.02);
        this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.4F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean fromPlayer = source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player;
        if (fromPlayer && this.canDuplicate) {
            this.spawnLinkedDuplicate(level);
        }
        if (this.arcaneInvulnTicks > 0) return false;
        if (source.getEntity() instanceof ArcaneWizardEntity || source.getDirectEntity() instanceof ArcaneWizardEntity) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            if (this.tickCount - this.fireWindowStartTick > 20) {
                this.fireWindowStartTick = this.tickCount;
            } else {
                return false;
            }
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !this.suppressLinkedSyncDamage) {
            ArcaneWizardEntity linked = this.getLinkedWizard(level);
            if (linked != null && linked.isAlive()) {
                linked.suppressLinkedSyncDamage = true;
                try {
                    if (this.isAlive()) {
                        linked.setHealth(Math.min(linked.getHealth(), this.getHealth()));
                    } else {
                        linked.hurtServer(level, source, Float.MAX_VALUE);
                    }
                } finally {
                    linked.suppressLinkedSyncDamage = false;
                }
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel sl) {
            ArcaneWizardEntity linked = this.getLinkedWizard(sl);
            if (linked != null && linked.isAlive() && this.isAlive()) {
                float shared = Math.min(this.getHealth(), linked.getHealth());
                if (Math.abs(this.getHealth() - shared) > 0.001F) {
                    this.setHealth(shared);
                }
                if (Math.abs(linked.getHealth() - shared) > 0.001F) {
                    linked.setHealth(shared);
                }
            }

            if (this.isOnFire()) {
                this.clearFire();
                if (this.tickCount - this.lastDouseTick > 20) {
                    this.lastDouseTick = this.tickCount;
                    this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.9F, 1.0F);
                }
            }
        }
        if (this.arcaneInvulnTicks > 0) {
            this.arcaneInvulnTicks--;
            if (this.level() instanceof ServerLevel sl) {
                Vec3 pos = this.position().add(0, 1.0, 0);
                double a = this.tickCount * 0.5;
                for (int i = 0; i < 3; i++) {
                    double angle = a + i * Math.PI * 2.0 / 3.0;
                    double bob = Math.sin(this.tickCount * 0.3 + i) * 0.25;
                    sl.sendParticles(ParticleTypes.END_ROD,
                        pos.x + Math.cos(angle) * 0.6, pos.y + bob,
                        pos.z + Math.sin(angle) * 0.6,
                        1, 0, 0, 0, 0);
                }
            }
        }
    }

    void grantArcaneInvulnerability(int ticks) {
        this.arcaneInvulnTicks = Math.max(this.arcaneInvulnTicks, ticks);
    }

    private @Nullable ArcaneWizardEntity getLinkedWizard(ServerLevel level) {
        if (this.linkedWizardUuid == null) return null;
        var entity = level.getEntity(this.linkedWizardUuid);
        return entity instanceof ArcaneWizardEntity linked ? linked : null;
    }

    private boolean isLinkedDuplicateOf(ArcaneWizardEntity other) {
        return (this.linkedWizardUuid != null && this.linkedWizardUuid.equals(other.getUUID()))
            || (other.linkedWizardUuid != null && other.linkedWizardUuid.equals(this.getUUID()));
    }

    private void spawnLinkedDuplicate(ServerLevel sl) {
        if (!this.canDuplicate) return;
        ArcaneWizardEntity clone = (ArcaneWizardEntity) this.getType().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (clone == null) return;

        Vec3 base = this.position();
        double angle = this.getYRot() * (Math.PI / 180.0) + (Math.PI / 2.0);
        Vec3 offset = new Vec3(Math.cos(angle) * 1.8, 0.0, Math.sin(angle) * 1.8);
        clone.setPos(base.x + offset.x, base.y, base.z + offset.z);
        clone.setYRot(this.getYRot());
        clone.setXRot(this.getXRot());
        clone.setHealth(this.getHealth());
        var scaleAttr = this.getAttribute(Attributes.SCALE);
        var cloneScale = clone.getAttribute(Attributes.SCALE);
        if (scaleAttr != null && cloneScale != null) {
            cloneScale.setBaseValue(scaleAttr.getBaseValue());
        }
        clone.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        clone.canDuplicate = false;
        this.canDuplicate = false;
        clone.linkedWizardUuid = this.getUUID();
        this.linkedWizardUuid = clone.getUUID();
        clone.setTarget(this.getTarget());

        sl.addFreshEntity(clone);
        sphereSurface(sl, clone.position().add(0, 1, 0), ParticleTypes.END_ROD, 24, 1.2, 0.1);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof ArcaneWizardEntity) {
            return false;
        }
        return super.canAttack(target);
    }

    protected enum WizardSpell { BLINK, NOVA, HEX, RESIZE }

    /** Weighted pick: RESIZE is rarer than Blink/Nova/Hex. */
    protected WizardSpell pickNextSpellForCast() {
        int roll = this.random.nextInt(24);
        if (roll < 3) {
            return WizardSpell.RESIZE;
        }
        return WizardSpell.values()[this.random.nextInt(3)];
    }

    /** After cast completes, next spell no sooner than {@code base + random(0..randExclusive)} ticks. */
    protected int spellCooldownBaseTicks() {
        return 80;
    }

    protected int spellCooldownRandomExclusive() {
        return 60;
    }

    /** Sets {@link Attributes#SCALE} on this wizard and linked duplicate, if any. */
    protected void setArcaneScale(float scale) {
        var inst = this.getAttribute(Attributes.SCALE);
        if (inst != null) {
            inst.setBaseValue(scale);
        }
        if (this.level() instanceof ServerLevel sl) {
            ArcaneWizardEntity linked = this.getLinkedWizard(sl);
            if (linked != null && linked.isAlive()) {
                var li = linked.getAttribute(Attributes.SCALE);
                if (li != null) {
                    li.setBaseValue(scale);
                }
            }
        }
    }

    // ── Spell implementations (override in subclasses e.g. {@link ArcaneInvokerEntity}) ──

    protected void spellWarmupBlink(ServerLevel sl, float t) {
        Vec3 base = this.position();
        double yaw = this.tickCount * 0.35;
        double radius = 1.0 - t * 0.6;

        for (int i = 0; i < 8; i++) {
            double angle = yaw + i * Math.PI / 4.0;
            double y = base.y + (i / 8.0) * 2.2;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                base.x + Math.cos(angle) * radius, y,
                base.z + Math.sin(angle) * radius,
                1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                base.x + Math.cos(angle + Math.PI) * radius, y,
                base.z + Math.sin(angle + Math.PI) * radius,
                1, 0, 0, 0, 0);
        }
        double pulse = 0.8 + Math.sin(this.tickCount * 0.5) * 0.2;
        circle(sl, base.add(0, 0.05, 0), ParticleTypes.REVERSE_PORTAL, 10, pulse);
    }

    protected void spellWarmupNova(ServerLevel sl, float t) {
        Vec3 base = this.position();

        circle(sl, base.add(0, 0.1, 0), ParticleTypes.END_ROD,
            (int) (8 + t * 16), 0.5 + t * 3.5);

        double colH = t * 3.0;
        for (int i = 0; i < 4; i++) {
            sl.sendParticles(ParticleTypes.END_ROD,
                base.x, base.y + (i / 4.0) * colH, base.z,
                1, 0.05, 0, 0.05, 0);
        }

        double speed = 0.4 + t * 0.8;
        for (int i = 0; i < 3; i++) {
            double angle = this.tickCount * speed + i * Math.PI * 2.0 / 3.0;
            sl.sendParticles(ParticleTypes.END_ROD,
                base.x + Math.cos(angle) * 0.8, base.y + 1.0,
                base.z + Math.sin(angle) * 0.8,
                1, 0, 0, 0, 0);
        }
    }

    protected void spellWarmupHex(ServerLevel sl, float t) {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 wp = this.position().add(0, 1.2, 0);
        Vec3 tpEff = AnkhShieldBarrier.clipSegmentEnd(wp, tp, sl);

        double spread = 2.0 * (1.0 - t);
        for (int i = 0; i < 4; i++) {
            double ox = (this.random.nextDouble() - 0.5) * spread;
            double oy = (this.random.nextDouble() - 0.5) * spread;
            double oz = (this.random.nextDouble() - 0.5) * spread;
            Vec3 from = tpEff.add(ox, oy, oz);
            Vec3 dir = tpEff.subtract(from).normalize();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                from.x, from.y, from.z,
                0, dir.x, dir.y, dir.z, 0.06);
        }

        double spiralR = 1.5 * (1.0 - t * 0.7);
        double a = this.tickCount * 0.6;
        for (int i = 0; i < 4; i++) {
            double angle = a + i * Math.PI / 2.0;
            double y = tpEff.y - 0.5 + (i / 4.0) * target.getBbHeight();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                tpEff.x + Math.cos(angle) * spiralR, y,
                tpEff.z + Math.sin(angle) * spiralR,
                1, 0, 0, 0, 0);
        }

        beam(sl, wp, tpEff, ParticleTypes.SOUL_FIRE_FLAME, 6 + (int) (t * 6));
    }

    protected void spellWarmupResize(ServerLevel sl, float t) {
        Vec3 base = this.position().add(0, 0.2, 0);
        double spin = this.tickCount * 0.45;
        double height = 0.6 + t * 1.5;
        double maxR = 0.25 + t * 0.85;
        double turns = 1.8 + t * 2.2;
        int pts = 14 + (int) (t * 14);
        resizeVortexSwirl(sl, base, DRAGON_BREATH_SWIRL, height, maxR, turns, pts, spin);
    }

    protected void spellCastBlink(ServerLevel sl, LivingEntity target) {
        float hp = this.getHealth() / this.getMaxHealth();
        this.grantArcaneInvulnerability(hp < 0.34F ? 28 : hp < 0.67F ? 22 : 18);
        Vec3 here = this.position();

        float dmg = 6.0F + (1.0F - hp) * 4.0F;
        double r = 3.5;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(here.x - r, here.y - 1, here.z - r,
                         here.x + r, here.y + 3, here.z + r))) {
            if (!e.is(this) && e.isAlive() && this.canAttack(e)) {
                e.hurtServer(sl, sl.damageSources().indirectMagic(this, this), dmg);
            }
        }

        this.blinkDepartureEffects(sl, here);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);

        Vec3 away = here.subtract(target.position()).normalize()
            .scale(6.0 + this.random.nextDouble() * 4.0);
        double tx = here.x + away.x;
        double tz = here.z + away.z;
        int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Mth.floor(tx), Mth.floor(tz));
        this.teleportTo(tx, gy, tz);

        Vec3 there = this.position();
        this.blinkTrailEffects(sl, here, there);
        this.blinkArrivalEffects(sl, there);
    }

    /** Subclasses (e.g. invoker) may replace with custom blink visuals. */
    protected void blinkDepartureEffects(ServerLevel sl, Vec3 here) {
        sphereSurface(sl, here.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 35, 1.5, -0.15);
        circle(sl, here.add(0, 0.1, 0), ParticleTypes.REVERSE_PORTAL, 20, 1.4);
    }

    /** Short-lived path hint between old and new position; default none. */
    protected void blinkTrailEffects(ServerLevel sl, Vec3 from, Vec3 to) {
    }

    protected void blinkArrivalEffects(ServerLevel sl, Vec3 there) {
        sphereSurface(sl, there.add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.3, 0.2);
        doubleHelix(sl, there, ParticleTypes.PORTAL, 0.6, 2.2, 2.0, 18);
    }

    protected void spellCastNova(ServerLevel sl) {
        float hp = this.getHealth() / this.getMaxHealth();
        this.grantArcaneInvulnerability(hp < 0.34F ? 16 : hp < 0.67F ? 12 : 8);
        Vec3 p = this.position();

        circle(sl, p.add(0, 0.2, 0), ParticleTypes.END_ROD, 36, 3.0);
        circle(sl, p.add(0, 0.6, 0), ParticleTypes.END_ROD, 28, 4.0);
        circle(sl, p.add(0, 1.0, 0), ParticleTypes.END_ROD, 22, 5.0);
        for (int i = 0; i < 20; i++) {
            sl.sendParticles(ParticleTypes.END_ROD,
                p.x, p.y + (i / 20.0) * 4.0, p.z,
                1, 0.06, 0, 0.06, 0);
        }
        sphereSurface(sl, p.add(0, 1, 0), ParticleTypes.END_ROD, 20, 2.5, 0.18);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.8F);

        float dmg = 8.0F + (1.0F - hp) * 4.0F;
        double rad = 5.0;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(p.x - rad, p.y - 1, p.z - rad,
                         p.x + rad, p.y + 3, p.z + rad))) {
            if (!e.is(this) && e.isAlive() && this.canAttack(e)) {
                e.hurtServer(sl, sl.damageSources().indirectMagic(this, this), dmg);
            }
        }
    }

    protected void spellCastHex(ServerLevel sl, LivingEntity target) {
        float hp = this.getHealth() / this.getMaxHealth();
        Vec3 from = this.position().add(0, 1.35, 0);
        Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = tp.subtract(from);
        HexBoltEntity hexBolt = new HexBoltEntity(this, dir, sl);
        hexBolt.setPos(from);
        sl.addFreshEntity(hexBolt);

        burst(sl, from, ParticleTypes.SOUL_FIRE_FLAME, 10, 0.25, 0.25, 0.25, 0.02);
        circle(sl, from.add(0, -0.2, 0), ParticleTypes.SOUL_FIRE_FLAME, 10, 0.6);
        this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.6F);

        if (hp < 0.5F) this.grantArcaneInvulnerability(10);
    }

    protected void spellCastResize(ServerLevel sl) {
        float s = 0.3F + this.random.nextFloat() * 1.7F;
        this.setArcaneScale(s);
        Vec3 base = this.position().add(0, 0.15, 0);
        double spin = this.tickCount * 0.35 + this.random.nextDouble() * Math.PI * 2;
        double height = 1.2 + s * 1.2;
        double maxR = 0.55 + s * 0.55;
        resizeVortexSwirl(sl, base, DRAGON_BREATH_SWIRL, height, maxR, 4.5, 44, spin);
        this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.7F + s * 0.15F);
    }

    // ─────────────────── Spell Goal ───────────────────

    private final class ArcaneSpellGoal extends Goal {
        private int warmup;
        private int totalWarmup;
        private int nextCastTick;

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public boolean canUse() {
            LivingEntity target = ArcaneWizardEntity.this.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (ArcaneWizardEntity.this.isCastingSpell()) return false;
            if (ArcaneWizardEntity.this.distanceToSqr(target) > 256.0) return false;
            // No casting through walls — require an unobstructed line of sight.
            if (!ArcaneWizardEntity.this.getSensing().hasLineOfSight(target)) return false;
            return ArcaneWizardEntity.this.tickCount >= this.nextCastTick;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = ArcaneWizardEntity.this.getTarget();
            return target != null && target.isAlive() && this.warmup > 0;
        }

        @Override
        public void start() {
            ArcaneWizardEntity w = ArcaneWizardEntity.this;
            w.pendingSpell = w.pickNextSpellForCast();
            this.totalWarmup = 20;
            this.warmup = this.totalWarmup;
            w.spellCastingTickCount = this.totalWarmup;
            this.nextCastTick = w.tickCount + w.spellCooldownBaseTicks()
                + w.random.nextInt(w.spellCooldownRandomExclusive());
            w.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 1.0F);
            w.setIsCastingSpell(mapSpell(w.pendingSpell));
        }

        @Override
        public void stop() {
            ArcaneWizardEntity.this.spellCastingTickCount = 0;
            ArcaneWizardEntity.this.pendingSpell = null;
        }

        @Override
        public void tick() {
            this.warmup--;
            if (this.warmup > 0) {
                if (ArcaneWizardEntity.this.level() instanceof ServerLevel sl) {
                    float t = 1.0F - (float) this.warmup / this.totalWarmup;
                    WizardSpell spell = ArcaneWizardEntity.this.pendingSpell;
                    if (spell != null) {
                        switch (spell) {
                            case BLINK -> ArcaneWizardEntity.this.spellWarmupBlink(sl, t);
                            case NOVA  -> ArcaneWizardEntity.this.spellWarmupNova(sl, t);
                            case HEX   -> ArcaneWizardEntity.this.spellWarmupHex(sl, t);
                            case RESIZE -> ArcaneWizardEntity.this.spellWarmupResize(sl, t);
                        }
                    }
                }
            } else if (this.warmup == 0) {
                castFinal();
                ArcaneWizardEntity.this.playSound(
                    ArcaneWizardEntity.this.getCastingSoundEvent(), 1.0F, 1.0F);
            }
        }

        private void castFinal() {
            ArcaneWizardEntity w = ArcaneWizardEntity.this;
            LivingEntity target = w.getTarget();
            WizardSpell spell = w.pendingSpell;
            w.pendingSpell = null;
            if (target == null || spell == null) return;
            if (!(w.level() instanceof ServerLevel sl)) return;
            switch (spell) {
                case BLINK -> w.spellCastBlink(sl, target);
                case NOVA  -> w.spellCastNova(sl);
                case HEX   -> w.spellCastHex(sl, target);
                case RESIZE -> w.spellCastResize(sl);
            }
        }

        private static SpellcasterIllager.IllagerSpell mapSpell(@Nullable WizardSpell s) {
            if (s == null) return SpellcasterIllager.IllagerSpell.NONE;
            return switch (s) {
                case BLINK -> SpellcasterIllager.IllagerSpell.DISAPPEAR;
                case NOVA  -> SpellcasterIllager.IllagerSpell.FANGS;
                case HEX   -> SpellcasterIllager.IllagerSpell.BLINDNESS;
                case RESIZE -> SpellcasterIllager.IllagerSpell.SUMMON_VEX;
            };
        }
    }

    // ─────────────────── Particle geometry ───────────────────

    protected static void burst(ServerLevel sl, Vec3 pos, ParticleOptions p,
                              int n, double dx, double dy, double dz, double speed) {
        sl.sendParticles(p, pos.x, pos.y, pos.z, n, dx, dy, dz, speed);
    }

    protected static void circle(ServerLevel sl, Vec3 c, ParticleOptions p,
                               int points, double radius) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points;
            sl.sendParticles(p,
                c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius,
                1, 0.02, 0.02, 0.02, 0);
        }
    }

    protected static void sphereSurface(ServerLevel sl, Vec3 c, ParticleOptions p,
                                       int count, double radius, double outSpeed) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            double theta = Math.random() * Math.PI * 2;
            double sx = Math.sin(phi) * Math.cos(theta);
            double sy = Math.cos(phi);
            double sz = Math.sin(phi) * Math.sin(theta);
            sl.sendParticles(p,
                c.x + sx * radius, c.y + sy * radius, c.z + sz * radius,
                0, sx, sy, sz, outSpeed);
        }
    }

    protected static void doubleHelix(ServerLevel sl, Vec3 base, ParticleOptions p,
                                     double radius, double height, double turns, int points) {
        for (int i = 0; i < points; i++) {
            double frac = (double) i / points;
            double angle = frac * turns * Math.PI * 2;
            double y = base.y + frac * height;
            sl.sendParticles(p,
                base.x + Math.cos(angle) * radius, y,
                base.z + Math.sin(angle) * radius,
                1, 0, 0, 0, 0);
            sl.sendParticles(p,
                base.x + Math.cos(angle + Math.PI) * radius, y,
                base.z + Math.sin(angle + Math.PI) * radius,
                1, 0, 0, 0, 0);
        }
    }

    protected static void beam(ServerLevel sl, Vec3 from, Vec3 to,
                             ParticleOptions p, int points) {
        Vec3 end = AnkhShieldBarrier.clipSegmentEnd(from, to, sl);
        if (end.distanceToSqr(to) > 2.5e-5) {
            sl.sendParticles(ParticleTypes.ENCHANT, end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.02);
        }
        Vec3 dir = end.subtract(from);
        int n = Math.max(0, points);
        for (int i = 0; i <= n; i++) {
            double frac = n == 0 ? 0.0 : (double) i / n;
            sl.sendParticles(p,
                from.x + dir.x * frac, from.y + dir.y * frac, from.z + dir.z * frac,
                1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Two opposite corkscrews around Y — reads as a vertical magic vortex. */
    protected static void resizeVortexSwirl(ServerLevel sl, Vec3 base, ParticleOptions p,
            double height, double maxRadius, double fullTurns, int points, double spin) {
        int n = Math.max(2, points);
        for (int i = 0; i < n; i++) {
            double frac = (double) i / (n - 1);
            double y = frac * height;
            double r = maxRadius * (0.12 + 0.88 * Math.sqrt(frac));
            for (int sign : new int[] { -1, 1 }) {
                double angle = spin + sign * frac * fullTurns * Math.PI * 2;
                sl.sendParticles(p,
                    base.x + Math.cos(angle) * r, base.y + y,
                    base.z + Math.sin(angle) * r,
                    1, 0.012, 0.025, 0.012, 0);
            }
        }
    }
}
