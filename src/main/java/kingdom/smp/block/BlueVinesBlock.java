package kingdom.smp.block;

import kingdom.smp.Ironhold;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TwistingVinesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Blue Vines — upward-growing vine native to the Ebonwood Hollow biome.
 * Visually identical in shape to vanilla Twisting Vines, repainted in deep blue.
 *
 * Overrides getBodyBlock() so the growing mechanic places BlueVinesPlantBlock
 * below the tip instead of the vanilla plant. No codec() override needed —
 * TwistingVinesBlock already provides a concrete codec(), and block identity
 * in the world is determined by the registry name, not the codec type.
 */
public class BlueVinesBlock extends TwistingVinesBlock {

    public BlueVinesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return kingdom.smp.ModBlocks.BLUE_VINES_PLANT.get();
    }
}
