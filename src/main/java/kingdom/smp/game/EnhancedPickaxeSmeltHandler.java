package kingdom.smp.game;

import kingdom.smp.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Handles Enhanced Pickaxe smelting-on-mine behavior.
 * - When an ore is mined with an Enhanced Pickaxe, the smelted result is dropped
 * - Example: mining Iron Ore drops Iron Ingot instead of the ore block
 * - Manually maps ores to their smelted results
 *
 * Register this class in Ironhold.java with:
 * NeoForge.EVENT_BUS.register(kingdom.smp.game.EnhancedPickaxeSmeltHandler.class);
 */
public final class EnhancedPickaxeSmeltHandler {
    private EnhancedPickaxeSmeltHandler() {}

    @SubscribeEvent
    public static void onOreMinedWithEnhancedPickaxe(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        ItemStack tool = event.getPlayer().getMainHandItem();

        // Check if the tool is an Enhanced Pickaxe
        if (!tool.is(ModItems.ENHANCED_PICKAXE.asItem())) return;

        BlockState state = event.getState();
        ItemStack smeltedResult = getSmelterResult(state);

        if (!smeltedResult.isEmpty()) {
            // Drop the smelted result instead of the ore block
            var level = (net.minecraft.server.level.ServerLevel) event.getLevel();
            net.minecraft.world.level.block.Block.popResource(level, event.getPos(), smeltedResult);
            // Experience is handled by the mined block's drops naturally
        }
    }

    /**
     * Returns the smelted result for an ore block, or an empty stack if no smelting recipe exists.
     */
    private static ItemStack getSmelterResult(BlockState state) {
        // Map vanilla ores to their smelted results
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
            return new ItemStack(Items.IRON_INGOT);
        } else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) {
            return new ItemStack(Items.GOLD_INGOT);
        } else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
            return new ItemStack(Items.COPPER_INGOT);
        } else if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
            return new ItemStack(Items.LAPIS_LAZULI, 4);
        } else if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return new ItemStack(Items.REDSTONE, 3);
        } else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            return new ItemStack(Items.DIAMOND);
        } else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
            return new ItemStack(Items.EMERALD);
        }
        return ItemStack.EMPTY;
    }
}
