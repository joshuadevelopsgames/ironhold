package kingdom.smp.item;

import kingdom.smp.entity.MirrorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Places a {@link MirrorEntity} on the clicked wall face exactly like a painting: it picks the
 * largest pane shape (by area) that fits the available space, falling back through smaller shapes.
 */
public class MirrorItem extends Item {
    // Candidate shapes {width, height} in blocks, ordered largest-area first.
    private static final int[][] SHAPES = {
        {4, 4}, {4, 3}, {3, 4}, {3, 3}, {4, 2}, {2, 4},
        {3, 2}, {2, 3}, {2, 2}, {2, 1}, {1, 2}, {1, 1}
    };

    public MirrorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isVertical()) {
            return InteractionResult.FAIL;
        }
        BlockPos placePos = context.getClickedPos().relative(clickedFace);
        Player player = context.getPlayer();
        ItemStack itemInHand = context.getItemInHand();
        if (player != null && !player.mayUseItemAt(placePos, clickedFace, itemInHand)) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        // Largest shape that fits wins (SHAPES is ordered by descending area).
        MirrorEntity chosen = null;
        for (int[] shape : SHAPES) {
            MirrorEntity candidate = createMirror(level, placePos, clickedFace, shape[0], shape[1]);
            EntityType.<MirrorEntity>createDefaultStackConfig(level, itemInHand, player).accept(candidate);
            if (candidate.survives()) {
                chosen = candidate;
                break;
            }
        }
        if (chosen == null) {
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide()) {
            chosen.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, chosen.position());
            level.addFreshEntity(chosen);
        }
        itemInHand.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Builds the entity this item places. Overridden by {@link MagicMirrorItem}. */
    protected MirrorEntity createMirror(Level level, BlockPos pos, Direction face, int width, int height) {
        return new MirrorEntity(level, pos, face, width, height);
    }
}
