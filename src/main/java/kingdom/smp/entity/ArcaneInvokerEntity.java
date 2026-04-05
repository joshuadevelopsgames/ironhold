package kingdom.smp.entity;

import kingdom.smp.game.AnkhShieldBarrier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/**
 * Mini boss variant: particle-heavy spell kit — homing orb barrage, binding rune, arcane nova,
 * and shadow-step teleport.
 */
public class ArcaneInvokerEntity extends ArcaneWizardEntity {

    private static final DustParticleOptions RUNE_GOLD = dust(255, 224, 90, 1.2F);
    private static final DustParticleOptions NOVA_SWEEP = dust(115, 52, 230, 1.4F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        int rgb = 0xFF000000 | (r << 16) | (g << 8) | b;
        return new DustParticleOptions(rgb, scale);
    }

    private int bindingRuneTicks;
    private double bindingRuneX;
    private double bindingRuneZ;
    private int novaResidualTicks;

    public ArcaneInvokerEntity(EntityType<? extends Illusioner> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    @Override
    protected WizardSpell pickNextSpellForCast() {
        int roll = this.random.nextInt(24);
        if (roll < 4) {
            return WizardSpell.RESIZE;
        }
        if (roll < 14) {
            return WizardSpell.HEX;
        }
        return this.random.nextBoolean() ? WizardSpell.BLINK : WizardSpell.NOVA;
    }

    @Override
    protected int spellCooldownBaseTicks() {
        return 55;
    }

    @Override
    protected int spellCooldownRandomExclusive() {
        return 45;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel sl)) return;

        if (this.novaResidualTicks > 0) {
            this.novaResidualTicks--;
            Vec3 p = this.position().add(0, 0.4, 0);
            if (this.novaResidualTicks % 3 == 0) {
                sl.sendParticles(ParticleTypes.WITCH,
                    p.x, p.y, p.z, 6, 0.9, 0.35, 0.9, 0.02);
            }
        }

        if (this.bindingRuneTicks <= 0) return;

        this.bindingRuneTicks--;
        double cx = this.bindingRuneX;
        double cz = this.bindingRuneZ;
        double radius = 3.5;
        double y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));

        double ang = this.tickCount * 0.08;
        for (int i = 0; i < 8; i++) {
            double a = ang + i * Math.PI * 2 / 8;
            double px = cx + Math.cos(a) * radius * 0.92;
            double pz = cz + Math.sin(a) * radius * 0.92;
            sl.sendParticles(ParticleTypes.ENCHANT,
                px, y + 0.05, pz, 1, 0, 0.04, 0, 0.01);
        }
        if (this.bindingRuneTicks % 12 == 0) {
            double bx = cx + (this.random.nextDouble() - 0.5) * radius * 1.8;
            double bz = cz + (this.random.nextDouble() - 0.5) * radius * 1.8;
            sl.sendParticles(ParticleTypes.FIREWORK, bx, y + 0.02, bz, 2, 0.08, 0, 0.08, 0.01);
        }

        AABB zone = new AABB(cx - radius, y - 0.5, cz - radius, cx + radius, y + 3.0, cz + radius);
        for (Player player : sl.getEntitiesOfClass(Player.class, zone, LivingEntity::isAlive)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 2));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 1));
            Vec3 pp = player.position().add(0, player.getBbHeight() * 0.5, 0);
            sl.sendParticles(ParticleTypes.WITCH, pp.x, pp.y, pp.z, 3, 0.35, 0.45, 0.35, 0.02);
        }
    }

    @Override
    protected void spellWarmupBlink(ServerLevel sl, float t) {
        Vec3 base = this.position().add(0, 1.0, 0);
        double rings = 10 + (int) (t * 18);
        for (int i = 0; i < rings; i++) {
            double a = this.tickCount * 0.4 + i * 0.55;
            double rad = 0.55 + t * 1.1;
            sl.sendParticles(ParticleTypes.PORTAL,
                base.x + Math.cos(a) * rad, base.y + Math.sin(this.tickCount * 0.12 + i * 0.2) * 0.2,
                base.z + Math.sin(a) * rad,
                1, 0.02, 0.02, 0.02, 0);
        }
        for (int i = 0; i < 3; i++) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                base.x + (this.random.nextDouble() - 0.5) * 0.5,
                base.y + 0.35 + this.random.nextDouble() * 0.4,
                base.z + (this.random.nextDouble() - 0.5) * 0.5,
                1, 0.06, 0.06, 0.06, 0.01);
        }
    }

    @Override
    protected void blinkDepartureEffects(ServerLevel sl, Vec3 here) {
    }

    @Override
    protected void blinkTrailEffects(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 mid = from.add(0, 1, 0);
        Vec3 end = to.add(0, 1, 0);
        beam(sl, mid, end, ParticleTypes.PORTAL, 10);
    }

    @Override
    protected void blinkArrivalEffects(ServerLevel sl, Vec3 there) {
        Vec3 c = there.add(0, 1, 0);
        sphereSurface(sl, c, ParticleTypes.PORTAL, 22, 0.55, 0.12);
    }

    @Override
    protected void spellWarmupNova(ServerLevel sl, float t) {
        Vec3 base = this.position().add(0, 0.15, 0);
        int portalCount = (int) (14 + t * 28);
        for (int i = 0; i < portalCount; i++) {
            double a = this.tickCount * 0.55 + i * 0.31;
            double rad = 0.4 + t * 2.2;
            sl.sendParticles(ParticleTypes.PORTAL,
                base.x + Math.cos(a) * rad, base.y + (i % 5) * 0.35 + t * 1.2,
                base.z + Math.sin(a) * rad,
                1, 0.04, 0.04, 0.04, 0);
        }
        for (int i = 0; i < 4 + (int) (t * 5); i++) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                base.x + (this.random.nextDouble() - 0.5) * (1.2 + t),
                base.y + 1.1 + this.random.nextDouble() * 0.5,
                base.z + (this.random.nextDouble() - 0.5) * (1.2 + t),
                1, 0.08, 0.08, 0.08, 0.015);
        }
    }

    @Override
    protected void spellCastNova(ServerLevel sl) {
        float hp = this.getHealth() / this.getMaxHealth();
        this.grantArcaneInvulnerability(hp < 0.34F ? 18 : hp < 0.67F ? 14 : 10);
        Vec3 p = this.position();

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1, p.z, 1, 0, 0, 0, 0);

        for (int ring = 0; ring < 4; ring++) {
            double r = 1.2 + ring * 1.35;
            circle(sl, p.add(0, 0.15 + ring * 0.25, 0), NOVA_SWEEP, 32 + ring * 6, r);
        }

        sphereSurface(sl, p.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 48, 4.2, 0.35);
        for (int i = 0; i < 36; i++) {
            double ang = (Math.PI * 2 * i) / 36;
            double sx = Math.cos(ang);
            double sz = Math.sin(ang);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                p.x, p.y + 0.2, p.z,
                0, sx * 0.45, 0.06, sz * 0.45, 0.12);
        }

        this.playSound(SoundEvents.WITHER_SHOOT, 0.45F, 1.2F);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.9F, 0.75F);

        float dmg = 9.0F + (1.0F - hp) * 4.5F;
        double rad = 5.5;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(p.x - rad, p.y - 1, p.z - rad, p.x + rad, p.y + 3, p.z + rad))) {
            if (!e.is(this) && e.isAlive() && this.canAttack(e)) {
                e.hurtServer(sl, sl.damageSources().indirectMagic(this, this), dmg);
                Vec3 knock = e.position().subtract(p).multiply(1, 0, 1);
                if (knock.lengthSqr() > 1.0e-4) {
                    knock = knock.normalize();
                    e.knockback(0.55, knock.x, knock.z);
                }
            }
        }

        this.novaResidualTicks = 48;
    }

    @Override
    protected void spellWarmupHex(ServerLevel sl, float t) {
        LivingEntity target = this.getTarget();
        if (target == null) return;
        Vec3 hand = this.position().add(0, 1.35, 0);
        Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 tpEff = AnkhShieldBarrier.clipSegmentEnd(hand, tp, sl);
        double gather = 2.8 * (1.0 - t);
        for (int i = 0; i < 10; i++) {
            double ox = (this.random.nextDouble() - 0.5) * gather;
            double oy = (this.random.nextDouble() - 0.5) * gather;
            double oz = (this.random.nextDouble() - 0.5) * gather;
            Vec3 at = tpEff.add(ox, oy, oz);
            Vec3 inward = hand.subtract(at).normalize();
            sl.sendParticles(ParticleTypes.ENCHANT,
                at.x, at.y, at.z,
                0, inward.x * 0.12, inward.y * 0.12, inward.z * 0.12, 0.02);
        }
        double swirl = this.tickCount * 0.7;
        for (int i = 0; i < 6; i++) {
            double ang = swirl + i * Math.PI / 3;
            double rad = 0.45 + (1.0 - t) * 0.9;
            sl.sendParticles(dust(128, 64, 242, 1.6F),
                hand.x + Math.cos(ang) * rad, hand.y, hand.z + Math.sin(ang) * rad,
                1, 0.02, 0.02, 0.02, 0);
        }
    }

    @Override
    protected void spellCastHex(ServerLevel sl, LivingEntity target) {
        float hp = this.getHealth() / this.getMaxHealth();
        Vec3 base = this.position().add(0, 1.35, 0);
        int orbs = 5;
        for (int i = 0; i < orbs; i++) {
            double ang = (Math.PI * 2 * i) / orbs + this.random.nextDouble() * 0.35;
            double pitch = (this.random.nextDouble() - 0.5) * 0.35;
            Vec3 dir = new Vec3(Math.cos(ang) * Math.cos(pitch), Math.sin(pitch), Math.sin(ang) * Math.cos(pitch));
            ArcaneOrbEntity orb = new ArcaneOrbEntity(this, target, dir, sl);
            orb.setPos(base.x, base.y, base.z);
            sl.addFreshEntity(orb);
        }
        burst(sl, base, ParticleTypes.ENCHANT, 20, 0.4, 0.25, 0.4, 0.03);
        sphereSurface(sl, base, ParticleTypes.END_ROD, 16, 0.9, 0.1);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.15F);
        if (hp < 0.5F) this.grantArcaneInvulnerability(10);
    }

    @Override
    protected void spellWarmupResize(ServerLevel sl, float t) {
        Vec3 base = this.position();
        double floorY = base.y + 0.05;
        double runeR = 2.8 + t * 0.8;
        int pts = 40 + (int) (t * 24);
        for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2 * i) / pts + this.tickCount * 0.04;
            double px = base.x + Math.cos(a) * runeR;
            double pz = base.z + Math.sin(a) * runeR;
            Vec3 rim = new Vec3(px, floorY, pz);
            Vec3 inward = base.subtract(rim).multiply(1, 0, 1).normalize();
            sl.sendParticles(ParticleTypes.ENCHANT,
                rim.x, rim.y, rim.z,
                0, inward.x * 0.1, 0.02, inward.z * 0.1, 0.018);
        }
        for (int i = 0; i < 16; i++) {
            double a = this.tickCount * 0.25 + i * 0.4;
            sl.sendParticles(RUNE_GOLD,
                base.x + Math.cos(a) * runeR * 0.95, floorY + 0.02,
                base.z + Math.sin(a) * runeR * 0.95,
                1, 0.02, 0, 0.02, 0);
        }
    }

    @Override
    protected void spellCastResize(ServerLevel sl) {
        Vec3 p = this.position();
        this.bindingRuneX = p.x;
        this.bindingRuneZ = p.z;
        this.bindingRuneTicks = 120;
        this.grantArcaneInvulnerability(12);
        double r = 3.5;
        for (int i = 0; i < 48; i++) {
            double a = (Math.PI * 2 * i) / 48;
            sl.sendParticles(RUNE_GOLD,
                p.x + Math.cos(a) * r, p.y + 0.08, p.z + Math.sin(a) * r,
                1, 0.03, 0.01, 0.03, 0);
        }
        sl.sendParticles(ParticleTypes.ENCHANT, p.x, p.y + 0.2, p.z, 40, 0.55, 0.08, 0.55, 0.04);
        this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F, 0.85F);
        this.playSound(SoundEvents.BEACON_ACTIVATE, 0.35F, 1.4F);
    }
}
