package kingdom.smp.entity;

import kingdom.smp.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Short-lived, visual-only chunk of terrain flung up by the Battle Hammer's ground slam.
 * Renders as whatever block it represents (see {@code SlamDebrisRenderer}), obeys gravity +
 * collision, and discards itself after a moment. It never places or drops a block, so it
 * can't grief terrain — purely juice. Inspired by Expanded-Combat's ground-slam falling
 * blocks, ported to the 1.26 render-state pipeline (reuses FallingBlockRenderState).
 */
public class SlamDebrisEntity extends Entity {

    private static final EntityDataAccessor<BlockState> BLOCK_STATE =
        SynchedEntityData.defineId(SlamDebrisEntity.class, EntityDataSerializers.BLOCK_STATE);

    private int life = 16;
    private int age = 0;

    public SlamDebrisEntity(EntityType<? extends SlamDebrisEntity> type, Level level) {
        super(type, level);
    }

    /** Server-side spawn helper: a debris chunk of {@code state} launched with {@code velocity}. */
    public static void spawn(ServerLevel level, double x, double y, double z,
                             BlockState state, Vec3 velocity, int life) {
        SlamDebrisEntity e = new SlamDebrisEntity(ModEntities.SLAM_DEBRIS.get(), level);
        e.setPos(x, y, z);
        e.setDeltaMovement(velocity);
        e.entityData.set(BLOCK_STATE, state);
        e.life = life;
        level.addFreshEntity(e);
    }

    public BlockState getBlockStateData() {
        return this.entityData.get(BLOCK_STATE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    @Override
    public void tick() {
        age++;
        if (getBlockStateData().isAir()) {
            discard();
            return;
        }
        Vec3 v = getDeltaMovement();
        if (!isNoGravity()) v = v.add(0.0, -0.045, 0.0);
        setDeltaMovement(v);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.96));
        // Server decides when it's spent. Landed chunks now rest in place until their
        // lifetime runs out (rather than vanishing the instant they touch ground).
        if (!level().isClientSide() && age >= life) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
