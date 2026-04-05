package kingdom.smp.mixin;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import kingdom.smp.Ironhold;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Injects Ebonwood Hollow into the overworld MultiNoise biome source.
 * <p>
 * Targets {@code OverworldBiomeBuilder.addBiomes(Consumer)} at RETURN.
 * This method is called from {@code MultiNoiseBiomeSourceParameterList.Preset.generateOverworldBiomes},
 * which is used both for building the actual parameter list AND for {@code usedBiomes()}
 * (the method the bootstrap validator checks). So injecting here ensures the biome is
 * present in both paths, preventing the "Unreferenced key" validation crash.
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "addBiomes", at = @At("RETURN"))
    private void ironhold$addEbonwoodHollow(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes,
            CallbackInfo ci) {
        LOGGER.debug("[Ironhold] Injecting ebonwood_hollow into overworld biome parameters");
        biomes.accept(Pair.of(
            Climate.parameters(
                Climate.Parameter.span(-0.45f, -0.15f),   // temperature — cool (taiga band)
                Climate.Parameter.span(-0.10f,  0.30f),   // humidity — moderate
                Climate.Parameter.span( 0.30f,  1.00f),   // continentalness — inland
                Climate.Parameter.span(-0.78f,  0.05f),   // erosion — hilly to flat
                Climate.Parameter.point(0.0f),              // depth — surface layer
                Climate.Parameter.span( 0.05f,  0.40f),   // weirdness — mid band (common)
                0.0f                                        // offset — same priority as vanilla biomes
            ),
            Ironhold.EBONWOOD_HOLLOW
        ));
    }
}
