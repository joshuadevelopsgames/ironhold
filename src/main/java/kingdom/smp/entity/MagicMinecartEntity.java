package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * Rideable cart that moves from rider input packets, lays {@link Blocks#RAIL},
 * and uses powered rails for slopes (high max speed + low friction).
 */
public class MagicMinecartEntity extends AbstractMinecart {
    private static final double ACCEL = 0.08;
    private static final double MAX_SPEED = 0.55;
    private static final double SPRINT_MAX = 0.80;
    private static final double H_FRICTION = 0.96;
    private static final int INPUT_TIMEOUT_TICKS = 10;
    private static final int TURN_COOLDOWN_TICKS = 6;

    private float serverForwardInput;
    private float serverStrafeInput;
    private boolean serverJumpInput;
    private boolean serverSprintInput;
    private int serverInputAgeTicks = INPUT_TIMEOUT_TICKS + 1;

    private Direction heading = Direction.NORTH;
    private double forwardSpeed = 0.0;
    private int turnCooldown = 0;
    private double smoothY = Double.NaN;
    private double logicalGroundY = Double.NaN;

    public MagicMinecartEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 interactionLocation) {
        if (!player.isSecondaryUseActive() && !this.isVehicle() && (this.level().isClientSide() || player.startRiding(this))) {
            if (!this.level().isClientSide()) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected Item getDropItem() {
        return kingdom.smp.ModItems.MAGIC_MINECART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(kingdom.smp.ModItems.MAGIC_MINECART_ITEM.get());
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // Custom movement in magicServerTick
    }

    public void updateServerInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint) {
        this.serverForwardInput = forward == backward ? 0.0F : (forward ? 1.0F : -1.0F);
        this.serverStrafeInput = left == right ? 0.0F : (left ? 1.0F : -1.0F);
        this.serverJumpInput = jump;
        this.serverSprintInput = sprint;
        this.serverInputAgeTicks = 0;
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.getHurtTime() > 0) this.setHurtTime(this.getHurtTime() - 1);
            if (this.getDamage() > 0.0F) this.setDamage(this.getDamage() - 1.0F);
            this.checkBelowWorld();
            this.handlePortal();
            this.magicServerTick(serverLevel);
            this.firstTick = false;
        } else {
            super.tick();
        }
    }

    private void magicServerTick(ServerLevel level) {
        this.serverInputAgeTicks++;
        if (this.serverInputAgeTicks > INPUT_TIMEOUT_TICKS) {
            this.serverForwardInput = 0.0F;
            this.serverStrafeInput = 0.0F;
            this.serverJumpInput = false;
            this.serverSprintInput = false;
        }

        Entity rider = this.getFirstPassenger();
        double max = this.serverSprintInput ? SPRINT_MAX : MAX_SPEED;

        if (rider instanceof ServerPlayer sp && this.serverForwardInput > 0 && Math.abs(this.forwardSpeed) < 0.01) {
            this.heading = Direction.fromYRot(sp.getYRot());
        }

        if (this.turnCooldown > 0) this.turnCooldown--;
        if (this.serverStrafeInput != 0 && this.turnCooldown <= 0 && Math.abs(this.forwardSpeed) > 0.02) {
            Direction oldHeading = this.heading;
            if (this.serverStrafeInput > 0) {
                this.heading = this.heading.getCounterClockWise();
            } else {
                this.heading = this.heading.getClockWise();
            }
            double groundY = Double.isNaN(this.logicalGroundY) ? Math.floor(this.getY()) : this.logicalGroundY;
            BlockPos turnPos = BlockPos.containing(this.getX(), groundY, this.getZ());
            this.placeCurvedRail(level, turnPos, oldHeading, this.heading);
            this.turnCooldown = TURN_COOLDOWN_TICKS;
        }

        if (this.serverForwardInput > 0) {
            double accel = ACCEL * (this.serverSprintInput ? 1.5 : 1.0);
            this.forwardSpeed += accel;
        } else if (this.serverForwardInput < 0) {
            this.forwardSpeed -= ACCEL;
        }
        this.forwardSpeed = Mth.clamp(this.forwardSpeed, -max * 0.75, max);
        this.forwardSpeed *= H_FRICTION;
        if (Math.abs(this.forwardSpeed) < 1.0E-4) this.forwardSpeed = 0.0;

        double motX = this.heading.getStepX() * this.forwardSpeed;
        double motZ = this.heading.getStepZ() * this.forwardSpeed;
        this.setDeltaMovement(motX, 0.0, motZ);

        if (Double.isNaN(this.logicalGroundY)) {
            this.logicalGroundY = Math.floor(this.getY());
        }

        BlockPos cartBlock = BlockPos.containing(this.getX(), this.logicalGroundY, this.getZ());

        BlockPos aheadRailPos = cartBlock.relative(this.heading);
        BlockState aheadAtRail = level.getBlockState(aheadRailPos);
        BlockState aheadBelow = level.getBlockState(aheadRailPos.below());
        boolean aheadIsSolid = !aheadAtRail.isAir() && !aheadAtRail.canBeReplaced() && !aheadAtRail.is(BlockTags.RAILS);
        boolean aheadHasGround = aheadBelow.isFaceSturdy(level, aheadRailPos.below(), Direction.UP);
        boolean aheadIsDropoff = !aheadHasGround
                && level.getBlockState(aheadRailPos.below().below()).isFaceSturdy(level, aheadRailPos.below().below(), Direction.UP);

        boolean rampAtCart = false;
        if (Math.abs(this.forwardSpeed) > 0.01) {
            if (aheadIsSolid) {
                this.placePoweredAscendingRail(level, cartBlock, this.heading);
                rampAtCart = true;
                BlockPos topPos = aheadRailPos.above();
                this.placeStraightRailNoSupport(level, topPos, this.heading);
            } else if (aheadIsDropoff) {
                this.placePoweredAscendingRail(level, aheadRailPos, this.heading.getOpposite());
                BlockPos lowerPos = aheadRailPos.below();
                this.placeStraightRailNoSupport(level, lowerPos, this.heading);
            } else if (aheadHasGround) {
                this.placeStraightRailNoSupport(level, aheadRailPos, this.heading);
            }
        }

        if (!rampAtCart) {
            this.placeStraightRailNoSupport(level, cartBlock, this.heading);
        }

        double nextX = this.getX() + motX;
        double nextZ = this.getZ() + motZ;

        BlockPos nextBlock = BlockPos.containing(nextX, this.logicalGroundY, nextZ);
        BlockState nextAtRail = level.getBlockState(nextBlock);
        boolean nextIsSolid = !nextAtRail.isAir() && !nextAtRail.canBeReplaced() && !nextAtRail.is(BlockTags.RAILS);
        BlockState nextBelow = level.getBlockState(nextBlock.below());

        if (nextIsSolid) {
            BlockPos oneAbove = nextBlock.above();
            BlockPos twoAbove = oneAbove.above();
            boolean headroom = isPassable(level, oneAbove) && isPassable(level, twoAbove);
            if (headroom) {
                this.logicalGroundY += 1.0;
            } else {
                this.forwardSpeed = 0.0;
                nextX = this.getX();
                nextZ = this.getZ();
            }
        } else if (!nextBelow.isFaceSturdy(level, nextBlock.below(), Direction.UP)) {
            for (int drop = 2; drop <= 16; drop++) {
                BlockPos checkPos = nextBlock.offset(0, -drop, 0);
                if (level.getBlockState(checkPos).isFaceSturdy(level, checkPos, Direction.UP)) {
                    this.logicalGroundY = checkPos.getY() + 1.0;
                    break;
                }
            }
        } else {
            BlockPos head = nextBlock.above();
            if (!isPassable(level, head)) {
                this.forwardSpeed = 0.0;
                nextX = this.getX();
                nextZ = this.getZ();
            }
        }

        double targetY = this.logicalGroundY + 0.0625;
        if (Double.isNaN(this.smoothY)) this.smoothY = targetY;
        double yDiff = targetY - this.smoothY;
        if (Math.abs(yDiff) <= 0.26) {
            this.smoothY = targetY;
        } else {
            this.smoothY += Math.signum(yDiff) * 0.25;
        }
        this.setPos(nextX, this.smoothY, nextZ);

        float targetYRot = this.heading.toYRot();
        this.setYRot(Mth.approachDegrees(this.getYRot(), targetYRot, 25.0F));
    }

    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.RAILS);
    }

    private boolean hasNaturalSupport(ServerLevel level, BlockPos railPos) {
        BlockState existing = level.getBlockState(railPos);
        if (!existing.canBeReplaced() && !existing.is(BlockTags.RAILS)) return false;
        return level.getBlockState(railPos.below()).isFaceSturdy(level, railPos.below(), Direction.UP);
    }

    private void placeStraightRail(ServerLevel level, BlockPos railPos, Direction dir) {
        if (level.getBlockState(railPos).is(BlockTags.RAILS)) return;
        if (!hasNaturalSupport(level, railPos)) return;

        RailShape shape = (dir == Direction.NORTH || dir == Direction.SOUTH)
                        ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        level.setBlock(railPos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), Block.UPDATE_ALL);
    }

    private void placeStraightRailNoSupport(ServerLevel level, BlockPos railPos, Direction dir) {
        BlockState existing = level.getBlockState(railPos);
        if (existing.is(BlockTags.RAILS)) return;
        if (!existing.isAir() && !existing.canBeReplaced()) return;
        if (!level.getBlockState(railPos.below()).isFaceSturdy(level, railPos.below(), Direction.UP)) return;

        RailShape shape = (dir == Direction.NORTH || dir == Direction.SOUTH)
                        ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        level.setBlock(railPos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), Block.UPDATE_ALL);
    }

    private void placePoweredAscendingRail(ServerLevel level, BlockPos railPos, Direction ascendToward) {
        BlockState existing = level.getBlockState(railPos);
        if (!existing.isAir() && !existing.canBeReplaced() && !existing.is(BlockTags.RAILS)) return;
        if (!level.getBlockState(railPos.below()).isFaceSturdy(level, railPos.below(), Direction.UP)) return;

        RailShape shape = switch (ascendToward) {
            case NORTH -> RailShape.ASCENDING_NORTH;
            case SOUTH -> RailShape.ASCENDING_SOUTH;
            case EAST  -> RailShape.ASCENDING_EAST;
            case WEST  -> RailShape.ASCENDING_WEST;
            default    -> RailShape.NORTH_SOUTH;
        };
        level.setBlock(railPos, Blocks.POWERED_RAIL.defaultBlockState()
                .setValue(PoweredRailBlock.SHAPE, shape)
                .setValue(PoweredRailBlock.POWERED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private void placeCurvedRail(ServerLevel level, BlockPos railPos, Direction from, Direction to) {
        if (!hasNaturalSupport(level, railPos)) return;

        RailShape curve = getCurveShape(from, to);
        level.setBlock(railPos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, curve), Block.UPDATE_ALL);

        BlockPos aheadPos = railPos.relative(to);
        this.placeStraightRail(level, aheadPos, to);
    }

    private static RailShape getCurveShape(Direction from, Direction to) {
        Direction incoming = from.getOpposite();
        if (incoming == Direction.SOUTH && to == Direction.EAST) return RailShape.SOUTH_EAST;
        if (incoming == Direction.SOUTH && to == Direction.WEST) return RailShape.SOUTH_WEST;
        if (incoming == Direction.NORTH && to == Direction.EAST) return RailShape.NORTH_EAST;
        if (incoming == Direction.NORTH && to == Direction.WEST) return RailShape.NORTH_WEST;
        if (incoming == Direction.WEST && to == Direction.NORTH) return RailShape.NORTH_WEST;
        if (incoming == Direction.WEST && to == Direction.SOUTH) return RailShape.SOUTH_WEST;
        if (incoming == Direction.EAST && to == Direction.NORTH) return RailShape.NORTH_EAST;
        if (incoming == Direction.EAST && to == Direction.SOUTH) return RailShape.SOUTH_EAST;
        return RailShape.NORTH_SOUTH;
    }
}
