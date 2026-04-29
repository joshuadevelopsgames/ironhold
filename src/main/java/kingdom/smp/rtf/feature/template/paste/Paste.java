package kingdom.smp.rtf.feature.template.paste;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import kingdom.smp.rtf.feature.template.placement.TemplatePlacement;
import kingdom.smp.rtf.feature.template.template.TemplateContext;

public interface Paste {
    <T extends TemplateContext>	boolean apply(LevelAccessor world, T ctx, BlockPos origin, Mirror mirror, Rotation rotation, TemplatePlacement<T> placement, PasteConfig config);
}
