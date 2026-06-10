package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import kingdom.smp.ModBlocks;
import kingdom.smp.ModItems;
import kingdom.smp.entity.ButterflySpecies;
import kingdom.smp.item.ButterflyItem;
import kingdom.smp.item.IronholdItemComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/**
 * Placeable glass jar that displays up to {@link ButterflyJarBlockEntity#MAX_BUTTERFLIES}
 * captured butterflies.
 *
 * <ul>
 *   <li>Right-click with a captured butterfly ({@link ButterflyItem}) to add it.</li>
 *   <li>Right-click with an empty hand to take the last one back out.</li>
 *   <li>Breaking it drops a jar that still holds its butterflies (stored on the
 *       {@code BUTTERFLY_JAR_CONTENTS} data component); re-placing restores them.</li>
 * </ul>
 */
public class ButterflyJarBlock extends Block implements EntityBlock {

    public static final MapCodec<ButterflyJarBlock> CODEC = simpleCodec(ButterflyJarBlock::new);

    /** Matches the model bounds (x5-11, y0-11, z5-11). */
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 11, 11);

    public ButterflyJarBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ButterflyJarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Only a captured butterfly goes in here; anything else falls through to normal behaviour.
        if (!(stack.getItem() instanceof ButterflyItem butterfly)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof ButterflyJarBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (be.isFull()) {
            // Jar's full — consume the click so we don't trigger the item's own use, but do nothing.
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            be.addButterfly(butterfly.species());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ButterflyJarBlockEntity be) || be.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ButterflySpecies removed = be.removeLast();
            if (removed != null) {
                ItemStack give = ModItems.butterflyFor(removed);
                if (!player.addItem(give)) {
                    player.drop(give, false);
                }
                level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 0.7f, 1.0f);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Restore butterflies stored on the placed item's component. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        List<String> ids = stack.get(IronholdItemComponents.BUTTERFLY_JAR_CONTENTS.get());
        if (ids != null && !ids.isEmpty() && level.getBlockEntity(pos) instanceof ButterflyJarBlockEntity be) {
            be.setFromIds(ids);
        }
    }

    /** Middle-click pick keeps the contained butterflies. */
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof ButterflyJarBlockEntity be && !be.isEmpty()) {
            stack.set(IronholdItemComponents.BUTTERFLY_JAR_CONTENTS.get(), be.contentIds());
        }
        return stack;
    }

    /**
     * When the jar is broken, stamp the dropped jar with its contents so the
     * butterflies are still inside. The loot table drops a plain jar; we just
     * attach the component to that drop here (the BE is still available on this event).
     */
    @SubscribeEvent
    public static void onJarBroken(BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof ButterflyJarBlockEntity be) || be.isEmpty()) {
            return;
        }
        List<String> ids = be.contentIds();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack s = drop.getItem();
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() == ModBlocks.BUTTERFLY_TERRARIUM.get()) {
                s.set(IronholdItemComponents.BUTTERFLY_JAR_CONTENTS.get(), ids);
                return;
            }
        }
    }
}
