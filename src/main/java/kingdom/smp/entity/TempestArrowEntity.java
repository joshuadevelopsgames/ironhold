package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import kingdom.smp.Ironhold;

/**
 * Arrow that can summon lightning on impact. Tempest arrows always strike; shots from the
 * Tempest Bow (with normal ammo) strike with a 1/3 chance. Tempest-arrow ammo uses a custom
 * particle trail on the client.
 */
public class TempestArrowEntity extends AbstractArrow {
    private static final String TAG_ALWAYS = "TempestAlways";
    private static final String TAG_BOW = "TempestBow";

    /** From {@link kingdom.smp.item.TempestArrowItem} — lightning every hit. */
    private boolean alwaysLightning;
    /** Fired using {@link kingdom.smp.item.TempestBowItem} with normal arrows — 1/3 lightning. */
    private boolean tempestBow;

    public TempestArrowEntity(EntityType<? extends TempestArrowEntity> type, Level level) {
        super(type, level);
    }

    public TempestArrowEntity(
        LivingEntity shooter,
        Level level,
        ItemStack ammo,
        ItemStack weapon,
        boolean alwaysLightning,
        boolean tempestBow
    ) {
        super(kingdom.smp.ModEntities.TEMPEST_ARROW_ENTITY.get(), shooter, level, ammo, weapon);
        this.alwaysLightning = alwaysLightning;
        this.tempestBow = tempestBow;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && !this.isInGround() && this.tickCount % 2 == 0 && this.alwaysLightning) {
            Vec3 p = this.position();
            this.level()
                .addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    p.x,
                    p.y,
                    p.z,
                    (this.random.nextDouble() - 0.5) * 0.08,
                    (this.random.nextDouble() - 0.5) * 0.08,
                    (this.random.nextDouble() - 0.5) * 0.08);
            this.level().addParticle(ParticleTypes.FIREWORK, p.x, p.y, p.z, 0.0, 0.0, 0.0);
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
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        boolean strike;
        if (this.alwaysLightning) {
            strike = true;
        } else if (this.tempestBow) {
            strike = sl.getRandom().nextInt(3) == 0;
        } else {
            strike = false;
        }
        if (!strike) {
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
        out.putBoolean(TAG_ALWAYS, this.alwaysLightning);
        out.putBoolean(TAG_BOW, this.tempestBow);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.alwaysLightning = in.getBooleanOr(TAG_ALWAYS, false);
        this.tempestBow = in.getBooleanOr(TAG_BOW, false);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return this.alwaysLightning
            ? new ItemStack(Ironhold.TEMPEST_ARROW.get())
            : new ItemStack(Items.ARROW);
    }
}
