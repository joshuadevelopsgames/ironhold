package kingdom.smp.entity;

import kingdom.smp.block.GuillotineBlock;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Invisible entity that players mount when placed in the guillotine.
 * Forces the rider into a swimming (prone) pose so they appear to kneel
 * with their head through the lunette.
 */
public class GuillotineSeatEntity extends Entity {

    public GuillotineSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        // Server-only: cleanup when empty or block broken
        if (!level().isClientSide()) {
            if (getPassengers().isEmpty()) {
                discard();
                return;
            }
            if (!(level().getBlockState(blockPosition()).getBlock() instanceof GuillotineBlock)) {
                ejectPassengers();
                discard();
                return;
            }
        }
        // Both client AND server: lock pose and rotation every tick
        // Running on client is essential so other players see the locked rotation
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                living.setPose(Pose.SWIMMING);
                float yaw = this.getYRot();
                living.setYRot(yaw);
                living.yRotO = yaw;
                living.setYBodyRot(yaw);
                living.yBodyRotO = yaw;
                living.setYHeadRot(yaw);
                living.yHeadRotO = yaw;
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // Position the rider so their head sits in the lunette hole.
        // The seat is spawned at the head-hole center (~0.8 blocks above the guillotine block floor).
        // In swimming pose the player is horizontal; offset Y so the head aligns with the hole.
        return new Vec3(0.0, -0.35, 0.0);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        if (passenger instanceof LivingEntity living) {
            // Lock both body and head yaw — player can only look up/down (pitch)
            living.setYBodyRot(this.getYRot());
            living.setYHeadRot(this.getYRot());
            living.setYRot(this.getYRot());
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // Restore normal pose on dismount
        if (passenger instanceof LivingEntity living) {
            living.setPose(Pose.STANDING);
        }
    }
}
