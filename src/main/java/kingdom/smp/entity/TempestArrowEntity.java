package kingdom.smp.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Tempest Arrow. It flies and damages exactly like a vanilla arrow, but always carries a
 * light-blue electric look with a constant spark trail. When fired from the
 * {@link kingdom.smp.item.TempestBowItem} it additionally summons a lightning bolt wherever it
 * lands; fired from any other bow it behaves like a normal arrow (no lightning).
 */
public class TempestArrowEntity extends AbstractArrow {
    private static final String TAG_LIGHTNING = "TempestLightning";

    /** Electric-blue core of the trail, matching the arrow's texture. */
    private static final DustParticleOptions ARC_DUST = new DustParticleOptions(0x3FA9FF, 1.0F);

    /** Summon a guaranteed lightning bolt on impact (set when fired from the Tempest Bow). */
    private boolean lightning;

    public TempestArrowEntity(EntityType<? extends TempestArrowEntity> type, Level level) {
        super(type, level);
    }

    public TempestArrowEntity(LivingEntity shooter, Level level, ItemStack ammo, ItemStack weapon, boolean lightning) {
        super(kingdom.smp.ModEntities.TEMPEST_ARROW_ENTITY.get(), shooter, level, ammo, weapon);
        this.lightning = lightning;
    }

    @Override
    public void tick() {
        // Vanilla draws a white crit-bubble trail in AbstractArrow#tick for crit arrows (every
        // full-draw shot). Suppress it on the client only — our electric arc replaces it — while
        // leaving the crit flag (and its server-side damage bonus) untouched, so the arrow still
        // behaves like a normal arrow.
        boolean restoreCrit = false;
        if (this.level().isClientSide() && this.isCritArrow()) {
            this.setCritArrow(false);
            restoreCrit = true;
        }
        super.tick();
        if (restoreCrit) {
            this.setCritArrow(true);
        }

        // Crackling electric arc while in flight — the arrow always "feels like electricity".
        if (this.level().isClientSide() && !this.isInGround()) {
            spawnArcTrail();
        }
    }

    /**
     * Lays a continuous, jagged electric arc along the segment the arrow travelled this tick: a
     * glowing electric-blue dust core jittered off-axis (so it reads as a lightning zig-zag rather
     * than a straight line) with sparks darting outward like a discharge.
     */
    private void spawnArcTrail() {
        RandomSource r = this.random;
        int steps = 3;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, this.xo, this.getX());
            double y = Mth.lerp(t, this.yo, this.getY());
            double z = Mth.lerp(t, this.zo, this.getZ());
            this.level()
                .addParticle(
                    ARC_DUST,
                    x + (r.nextDouble() - 0.5) * 0.1,
                    y + (r.nextDouble() - 0.5) * 0.1,
                    z + (r.nextDouble() - 0.5) * 0.1,
                    0.0,
                    0.0,
                    0.0);
            if (r.nextInt(2) == 0) {
                this.level()
                    .addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        x,
                        y,
                        z,
                        (r.nextDouble() - 0.5) * 0.22,
                        (r.nextDouble() - 0.5) * 0.22,
                        (r.nextDouble() - 0.5) * 0.22);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        tryLightning(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        tryLightning(result.getLocation());
    }

    private void tryLightning(Vec3 hitPos) {
        if (!this.lightning || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
        bolt.setPos(hitPos);
        if (this.getOwner() instanceof ServerPlayer sp) {
            bolt.setCause(sp);
        }
        sl.addFreshEntity(bolt);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean(TAG_LIGHTNING, this.lightning);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.lightning = in.getBooleanOr(TAG_LIGHTNING, false);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(kingdom.smp.ModItems.TEMPEST_ARROW.get());
    }
}
