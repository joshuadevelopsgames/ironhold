package kingdom.smp.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Places a vanilla {@link ItemFrame} flagged invisible: the frame border is hidden by the
 * vanilla item-frame renderer whenever {@code isInvisible()} is true, so only the framed item
 * shows. Placement mirrors {@code ItemFrameItem}/{@code HangingEntityItem#useOn} (same
 * {@code mayPlace} rules), but skips the {@code entity_data} plumbing and just calls
 * {@link ItemFrame#setInvisible} directly. The invisible flag is persisted to the entity's NBT
 * by vanilla, so it survives reloads; breaking the frame drops this item back rather than a
 * plain frame — see {@code ItemFrameInvisibleDropMixin}.
 */
public class InvisibleItemFrameItem extends Item {
    public InvisibleItemFrameItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().relative(face);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();

        // ItemFrameItem#mayPlace: any face, inside build height, where the player may edit.
        if (player != null && (!level.isInsideBuildHeight(placePos)
                || !player.mayUseItemAt(placePos, face, stack))) {
            return InteractionResult.FAIL;
        }

        ItemFrame frame = new ItemFrame(level, placePos, face);
        frame.setInvisible(true);
        if (!frame.survives()) {
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide()) {
            frame.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, frame.position());
            level.addFreshEntity(frame);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
