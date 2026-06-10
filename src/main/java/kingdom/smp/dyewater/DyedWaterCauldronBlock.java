package kingdom.smp.dyewater;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A water cauldron that remembers a {@link DyeColor}. Visually identical to the vanilla water
 * cauldron except the contained water is tinted to {@link #COLOR} (see the client block tint source
 * in {@link DyedWaterClientEvents}). Fill levels reuse vanilla {@link LayeredCauldronBlock#LEVEL}.
 *
 * <p>Item interactions are driven externally by {@link DyedWaterInteractions} on the game bus, so
 * this block carries an empty interaction dispatcher.
 */
public class DyedWaterCauldronBlock extends LayeredCauldronBlock {

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public DyedWaterCauldronBlock(BlockBehaviour.Properties props) {
        super(Biome.Precipitation.NONE, new CauldronInteraction.Dispatcher(), props);
        registerDefaultState(stateDefinition.any()
            .setValue(LEVEL, 1)
            .setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds LEVEL
        builder.add(COLOR);
    }

    /** Dyed water never receives stalactite drips (keeps the colour stable). */
    @Override
    protected boolean canReceiveStalactiteDrip(net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }
}
