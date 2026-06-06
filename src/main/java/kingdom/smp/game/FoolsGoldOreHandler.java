package kingdom.smp.game;

import kingdom.smp.ModBlocks;
import kingdom.smp.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Handles fool's gold ore block drops.
 * - Drops fool's gold item (not the ore block) when mined without silk touch
 * - Drops the ore block when mined with silk touch
 *
 * Register this class in Ironhold.java with:
 * NeoForge.EVENT_BUS.register(kingdom.smp.game.FoolsGoldOreHandler.class);
 */
public final class FoolsGoldOreHandler {
    private FoolsGoldOreHandler() {}

    @SubscribeEvent
    public static void onFoolsGoldOreMined(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getState();
        boolean isFoolsGoldOre = state.is(ModBlocks.FOOLS_GOLD_ORE.get())
                              || state.is(ModBlocks.DEEPSLATE_FOOLS_GOLD_ORE.get());

        if (!isFoolsGoldOre) return;

        ItemStack tool = event.getPlayer().getMainHandItem();

        // Check for silk touch enchantment
        // In NeoForge 1.26, getAllEnchantments() requires a RegistryLookup<Enchantment>
        var registry = event.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var enchantments = tool.getAllEnchantments(registry);
        boolean hasSilkTouch = false;

        // Check the enchantments map for SILK_TOUCH
        if (enchantments != null && !enchantments.isEmpty()) {
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey() != null && entry.getValue() > 0) {
                    // Check if this is SILK_TOUCH by comparing the enchantment's string representation
                    var enchantmentId = entry.getKey().toString();
                    if (enchantmentId.contains("silk_touch")) {
                        hasSilkTouch = true;
                        break;
                    }
                }
            }
        }

        // If no silk touch, drop fool's gold item instead of the ore block
        // Note: The ore block itself won't drop due to requiresCorrectToolForDrops() being set
        if (!hasSilkTouch) {
            var level = (net.minecraft.server.level.ServerLevel) event.getLevel();
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.FOOLS_GOLD.get()));
        }
    }
}
