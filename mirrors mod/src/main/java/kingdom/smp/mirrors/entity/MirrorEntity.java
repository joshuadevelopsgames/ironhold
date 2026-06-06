package kingdom.smp.mirrors.entity;

import kingdom.smp.mirrors.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A wall-hung mirror that places like a painting, in several shapes. Its {@code width}/{@code height}
 * (in blocks) are synced so the client can size both the pane and the reflection. The reflective
 * surface is rendered by {@code MirrorRenderer}; the planar reflection lives client-side.
 */
public class MirrorEntity extends HangingEntity {
    /** Total thickness of the pane (matches vanilla paintings); the glass sits flush to the wall. */
    public static final float DEPTH = 0.0625F;
    public static final int DEFAULT_WIDTH = 1;
    public static final int DEFAULT_HEIGHT = 2;

    private static final EntityDataAccessor<Integer> DATA_WIDTH =
        SynchedEntityData.defineId(MirrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEIGHT =
        SynchedEntityData.defineId(MirrorEntity.class, EntityDataSerializers.INT);

    public MirrorEntity(EntityType<? extends MirrorEntity> type, Level level) {
        super(type, level);
    }

    public MirrorEntity(Level level, BlockPos blockPos, Direction direction, int width, int height) {
        super(kingdom.smp.mirrors.ModRegistry.MIRROR_ENTITY.get(), level, blockPos);
        this.entityData.set(DATA_WIDTH, width);
        this.entityData.set(DATA_HEIGHT, height);
        this.setDirection(direction);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_WIDTH, DEFAULT_WIDTH);
        entityData.define(DATA_HEIGHT, DEFAULT_HEIGHT);
    }

    public int getWidthBlocks() {
        return this.entityData.get(DATA_WIDTH);
    }

    public int getHeightBlocks() {
        return this.entityData.get(DATA_HEIGHT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_WIDTH.equals(accessor) || DATA_HEIGHT.equals(accessor)) {
            this.recalculateBoundingBox();
        }
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        int width = getWidthBlocks();
        int height = getHeightBlocks();
        Vec3 attachedToWall = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        double horizontalOffset = offsetForSize(width);
        double verticalOffset = offsetForSize(height);
        Direction left = direction.getCounterClockWise();
        Vec3 position = attachedToWall.relative(left, horizontalOffset).relative(Direction.UP, verticalOffset);
        Direction.Axis axis = direction.getAxis();
        double xSize = axis == Direction.Axis.X ? DEPTH : width;
        double ySize = height;
        double zSize = axis == Direction.Axis.Z ? DEPTH : width;
        return AABB.ofSize(position, xSize, ySize, zSize);
    }

    private static double offsetForSize(int size) {
        return size % 2 == 0 ? 0.5 : 0.0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("facing", Direction.LEGACY_ID_CODEC_2D, this.getDirection());
        output.putInt("mirror_width", getWidthBlocks());
        output.putInt("mirror_height", getHeightBlocks());
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_WIDTH, input.getIntOr("mirror_width", DEFAULT_WIDTH));
        this.entityData.set(DATA_HEIGHT, input.getIntOr("mirror_height", DEFAULT_HEIGHT));
        Direction direction = input.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(input);
        this.setDirection(direction);
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (!(causedBy instanceof Player player && player.hasInfiniteMaterials())) {
                this.spawnAtLocation(level, ModRegistry.MIRROR.get());
            }
        }
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void snapTo(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, this.getDirection().get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModRegistry.MIRROR.get());
    }
}
