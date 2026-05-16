package kingdom.smp.rtf.feature.template.placement;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import kingdom.smp.rtf.feature.template.BlockUtils;
import kingdom.smp.rtf.feature.template.decorator.TreeContext;
import kingdom.smp.rtf.feature.template.template.Dimensions;

public record TreePlacement() implements TemplatePlacement<TreeContext> {
    public static final MapCodec<TreePlacement> CODEC = MapCodec.unit(TreePlacement::new);

    @Override
    public boolean canPlaceAt(LevelAccessor world, BlockPos pos, Dimensions dimensions) {
        return BlockUtils.isSoil(world, pos.below()) && BlockUtils.isClearOverhead(world, pos, dimensions.getSizeY(), BlockUtils::canTreeReplace);
    }

    @Override
    public boolean canReplaceAt(LevelAccessor world, BlockPos pos) {
        return BlockUtils.canTreeReplace(world, pos);
    }

    @Override
    public TreeContext createContext() {
        return new TreeContext();
    }

    @Override
    public MapCodec<TreePlacement> codec() {
        return CODEC;
    }
}
