package kingdom.smp.block;

import kingdom.smp.Ironhold;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.TwistingVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Blue Vines Plant — the non-growing body segment placed below a BlueVinesBlock tip.
 *
 * Overrides getHeadBlock() so the block validates its connection to BlueVinesBlock
 * rather than vanilla TwistingVinesBlock. Return type must be GrowingPlantHeadBlock
 * (the abstract supertype of all growing plant head blocks) as declared in
 * GrowingPlantBodyBlock.
 */
public class BlueVinesPlantBlock extends TwistingVinesPlantBlock {

    public BlueVinesPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return kingdom.smp.ModBlocks.BLUE_VINES.get();
    }
}
