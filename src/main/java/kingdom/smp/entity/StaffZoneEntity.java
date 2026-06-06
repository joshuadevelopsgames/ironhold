package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class StaffZoneEntity extends Entity {
    private static final EntityDataAccessor<Integer> ZONE_TYPE = SynchedEntityData.defineId(StaffZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(StaffZoneEntity.class, EntityDataSerializers.FLOAT);
    
    // 0 = Solar (Burn), 1 = Lunar (Gravity)
    private int maxAge = 100;
    
    public StaffZoneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ZONE_TYPE, 0);
        builder.define(RADIUS, 3.0f);
    }

    public void setZoneType(int type) { this.entityData.set(ZONE_TYPE, type); }
    public int getZoneType() { return this.entityData.get(ZONE_TYPE); }
    public void setRadius(float r) { this.entityData.set(RADIUS, r); }
    public float getRadius() { return this.entityData.get(RADIUS); }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.tickCount > maxAge) {
                this.discard();
                return;
            }
            int type = getZoneType();
            float r = getRadius();
            ServerLevel sl = (ServerLevel) this.level();
            Vec3 pos = this.position();
            
            if (type == 0) { // Solar: Supernova Lingering Zone
                if (this.tickCount % 5 == 0) {
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), e -> e.isAlive())) {
                        if (e.distanceToSqr(pos) <= r * r) {
                            e.igniteForSeconds(2);
                            e.hurtServer(sl, sl.damageSources().inFire(), 2.0f);
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 5, r * 0.5, 0.1, r * 0.5, 0.02);
                if (this.tickCount % 4 == 0) {
                    sl.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.1, pos.z, 1, r * 0.5, 0.1, r * 0.5, 0.0);
                }
            } else if (type == 1) { // Lunar: Gravity Well
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), e -> e.isAlive())) {
                    if (e.distanceToSqr(pos) <= r * r) {
                        Vec3 center = pos.add(0, 1.0, 0);
                        Vec3 diff = center.subtract(e.position());
                        if (diff.lengthSqr() > 0.5) {
                            e.setDeltaMovement(e.getDeltaMovement().add(diff.normalize().scale(0.06)));
                            e.hurtMarked = true;
                        } else {
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.15, 0));
                            e.hurtMarked = true;
                        }
                        if (this.tickCount % 10 == 0) {
                            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1, false, false, false));
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 10, r * 0.5, 0.5, r * 0.5, -0.1);
            }
        }
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        // Can add state saving later if needed
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        // Can add state saving later if needed
    }

    @Override public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
}
