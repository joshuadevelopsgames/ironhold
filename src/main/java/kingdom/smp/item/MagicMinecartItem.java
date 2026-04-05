package kingdom.smp.item;

import kingdom.smp.entity.MagicMinecartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns a {@link MagicMinecartEntity} on top of a solid block (face clicked with the item).
 */
public class MagicMinecartItem extends Item {
    private final EntityType<MagicMinecartEntity> entityType;

    public MagicMinecartItem(EntityType<MagicMinecartEntity> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        if (!clickedState.isFaceSturdy(level, clicked, context.getClickedFace())) {
            return InteractionResult.PASS;
        }
        BlockPos spawnPos = clicked.relative(context.getClickedFace());
        BlockState above = level.getBlockState(spawnPos);
        BlockState above2 = level.getBlockState(spawnPos.above());
        if (!above.isAir() || !above2.isAir()) {
            return InteractionResult.FAIL;
        }

        Vec3 center = Vec3.atBottomCenterOf(spawnPos).add(0.0, 0.25, 0.0);
        MagicMinecartEntity cart = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (cart == null) {
            return InteractionResult.FAIL;
        }
        cart.setPos(center.x, center.y, center.z);
        if (context.getPlayer() != null) {
            cart.setYRot(context.getPlayer().getYRot());
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockPos railPos = spawnPos;
            BlockState railState = level.getBlockState(railPos);
            if ((railState.isAir() || railState.canBeReplaced())
                    && level.getBlockState(railPos.below()).isFaceSturdy(level, railPos.below(), Direction.UP)) {
                level.setBlock(railPos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH), Block.UPDATE_ALL);
            }
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, spawnPos, GameEvent.Context.of(context.getPlayer(), clickedState));
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
