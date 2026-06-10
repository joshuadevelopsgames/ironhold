package kingdom.smp.dyewater;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Cauldron round-trip for dyed water, on the game bus (independent of the 1.26
 * {@code CauldronInteraction.Dispatcher}, which is no longer mod-mutable):
 *
 * <ul>
 *   <li><b>Dye + water cauldron</b> (vanilla or already-dyed) → a {@link DyedWaterCauldronBlock}
 *       of that colour, consuming one dye.</li>
 *   <li><b>Empty bucket + full dyed cauldron</b> → the matching coloured water bucket, emptying
 *       the cauldron back to vanilla.</li>
 * </ul>
 *
 * Placing the coloured bucket in the world (and re-scooping it) is handled for free by the
 * {@link net.minecraft.world.item.BucketItem} + {@code BaseFlowingFluid} machinery.
 */
public final class DyedWaterInteractions {
    private DyedWaterInteractions() {}

    /** Vanilla dye item → colour (1.26 dropped {@code DyeItem.getDyeColor()}). */
    private static final Map<Item, DyeColor> DYE_ITEMS = new IdentityHashMap<>();
    static {
        DYE_ITEMS.put(Items.WHITE_DYE,      DyeColor.WHITE);
        DYE_ITEMS.put(Items.ORANGE_DYE,     DyeColor.ORANGE);
        DYE_ITEMS.put(Items.MAGENTA_DYE,    DyeColor.MAGENTA);
        DYE_ITEMS.put(Items.LIGHT_BLUE_DYE, DyeColor.LIGHT_BLUE);
        DYE_ITEMS.put(Items.YELLOW_DYE,     DyeColor.YELLOW);
        DYE_ITEMS.put(Items.LIME_DYE,       DyeColor.LIME);
        DYE_ITEMS.put(Items.PINK_DYE,       DyeColor.PINK);
        DYE_ITEMS.put(Items.GRAY_DYE,       DyeColor.GRAY);
        DYE_ITEMS.put(Items.LIGHT_GRAY_DYE, DyeColor.LIGHT_GRAY);
        DYE_ITEMS.put(Items.CYAN_DYE,       DyeColor.CYAN);
        DYE_ITEMS.put(Items.PURPLE_DYE,     DyeColor.PURPLE);
        DYE_ITEMS.put(Items.BLUE_DYE,       DyeColor.BLUE);
        DYE_ITEMS.put(Items.BROWN_DYE,      DyeColor.BROWN);
        DYE_ITEMS.put(Items.GREEN_DYE,      DyeColor.GREEN);
        DYE_ITEMS.put(Items.RED_DYE,        DyeColor.RED);
        DYE_ITEMS.put(Items.BLACK_DYE,      DyeColor.BLACK);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack held = event.getItemStack();
        Player player = event.getEntity();

        boolean isVanillaWaterCauldron = state.is(Blocks.WATER_CAULDRON);
        boolean isDyedCauldron = state.is(DyedWater.CAULDRON.get());

        // ── Dye on a (vanilla or dyed) water cauldron → set/replace the colour ──────────────
        DyeColor dyeColor = DYE_ITEMS.get(held.getItem());
        if (dyeColor != null && (isVanillaWaterCauldron || isDyedCauldron)) {
            if (!level.isClientSide()) {
                int fill = state.getValue(LayeredCauldronBlock.LEVEL);
                BlockState dyed = DyedWater.CAULDRON.get().defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, fill)
                    .setValue(DyedWaterCauldronBlock.COLOR, dyeColor);
                level.setBlockAndUpdate(pos, dyed);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.4f);
                level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos);
            }
            finish(event);
            return;
        }

        // ── Empty bucket on a FULL dyed cauldron → coloured bucket + empty cauldron ─────────
        if (held.is(Items.BUCKET) && isDyedCauldron
                && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
            if (!level.isClientSide()) {
                DyeColor color = state.getValue(DyedWaterCauldronBlock.COLOR);
                ItemStack filled = new ItemStack(DyedWater.BUCKET.get(color).get());
                player.setItemInHand(event.getHand(),
                    ItemUtils.createFilledResult(held, player, filled));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, pos);
            }
            finish(event);
        }
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
