package kingdom.smp.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import kingdom.smp.ModItems;

/**
 * Fool's Gold ore block.
 * - Drops fool's gold item (not the ore block) when mined without silk touch
 * - Drops the ore block when mined with silk touch
 * - Tool tier requirement is enforced via tags (see data packs — requires iron pickaxe or better)
 * - Drop handling is done via FoolsGoldOreHandler event listener
 */
public class FoolsGoldOreBlock extends Block {
    public FoolsGoldOreBlock(BlockBehaviour.Properties properties) {
        super(properties.requiresCorrectToolForDrops());
    }

}
