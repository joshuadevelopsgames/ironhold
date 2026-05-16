package kingdom.smp.rtf.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import kingdom.smp.rtf.data.preset.settings.Preset;

public final class PresetDimensionTypes {

    public static void bootstrap(Preset preset, BootstrapContext<DimensionType> ctx) {
        DimensionTypes.bootstrap(ctx);
    }
}
