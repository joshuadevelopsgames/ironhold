package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Pure visual entity representing a magic beam from origin to its spawn position.
 * Positioned at the END (impact point); origin (caster tip) stored in synced data.
 * Lives for maxLife ticks then discards automatically.
 */
public class SpellBeamEntity extends Entity {

    private static final EntityDataAccessor<Float> ORIGIN_X =
        SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Y =
        SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Z =
        SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BEAM_COLOR =
        SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFE =
        SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.INT);

    public SpellBeamEntity(EntityType<? extends SpellBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SpellBeamEntity create(Level level,
            double endX, double endY, double endZ,
            double originX, double originY, double originZ,
            int color, int maxLife) {
        SpellBeamEntity beam = new SpellBeamEntity(kingdom.smp.ModEntities.SPELL_BEAM.get(), level);
        beam.setPos(endX, endY, endZ);
        beam.entityData.set(ORIGIN_X, (float) originX);
        beam.entityData.set(ORIGIN_Y, (float) originY);
        beam.entityData.set(ORIGIN_Z, (float) originZ);
        beam.entityData.set(BEAM_COLOR, color);
        beam.entityData.set(MAX_LIFE, maxLife);
        return beam;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORIGIN_X, 0.0f);
        builder.define(ORIGIN_Y, 0.0f);
        builder.define(ORIGIN_Z, 0.0f);
        builder.define(BEAM_COLOR, 0x8844FF);
        builder.define(MAX_LIFE, 15);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.entityData.get(MAX_LIFE)) {
            this.discard();
        }
    }

    public float getOriginX() { return this.entityData.get(ORIGIN_X); }
    public float getOriginY() { return this.entityData.get(ORIGIN_Y); }
    public float getOriginZ() { return this.entityData.get(ORIGIN_Z); }
    public int getBeamColor() { return this.entityData.get(BEAM_COLOR); }
    public int getMaxLife()   { return this.entityData.get(MAX_LIFE); }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
