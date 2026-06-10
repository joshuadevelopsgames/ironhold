package kingdom.smp.dyewater;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

import kingdom.smp.mixin.RegisterFluidModelsEventAccessor;

/**
 * Client-side rendering for dyed water. Coloured water is real {@code Fluids.WATER}, so we don't register
 * any fluid models; instead we swap the tint source on vanilla's water model so any cell we've stamped with
 * a colour (see {@link DyedWaterlog}) renders that colour, while ordinary water keeps its biome colour. The
 * dyed cauldron's water layer is tinted separately from its {@code COLOR} blockstate.
 */
public final class DyedWaterClientEvents {
    private DyedWaterClientEvents() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(DyedWaterClientEvents::onOverrideWaterModel);
        modBus.addListener(DyedWaterClientEvents::onRegisterBlockTints);
    }

    /**
     * Reuses vanilla's already-baked water models verbatim (sprites + render layer), swapping only the tint
     * source. Vanilla models are seeded before this event fires (see {@code ClientHooks.gatherFluidModels}),
     * so overwriting the map entry is safe at any priority.
     */
    private static void onOverrideWaterModel(RegisterFluidModelsEvent event) {
        Map<Fluid, FluidModel> models = ((RegisterFluidModelsEventAccessor) (Object) event).ironhold$getModels();
        retintWater(models, Fluids.WATER);
        retintWater(models, Fluids.FLOWING_WATER);
    }

    private static void retintWater(Map<Fluid, FluidModel> models, Fluid fluid) {
        FluidModel vanilla = models.get(fluid);
        if (vanilla == null) return; // defensive — vanilla is always seeded before this runs
        models.put(fluid, new FluidModel(vanilla.layer(), vanilla.stillMaterial(),
            vanilla.flowingMaterial(), vanilla.overlayMaterial(), new DyedWaterTint()));
    }

    /** Vanilla water tint, except cells we've stamped with a dye colour (see {@link DyedWaterlog}). */
    private static final class DyedWaterTint implements FluidTintSource {
        private final FluidTintSource base = FluidTintSources.water();

        @Override
        public int color(FluidState state) {
            return base.color(state);
        }

        @Override
        public int colorInWorld(FluidState fluid, BlockState state, BlockAndTintGetter getter, BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                DyeColor color = DyedWaterlog.get(mc.level, pos);
                if (color != null) {
                    return 0xFF000000 | DyedWater.rgb(color);
                }
            }
            return base.colorInWorld(fluid, state, getter, pos);
        }
    }

    private static void onRegisterBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new CauldronWaterTint()), DyedWater.CAULDRON.get());
    }

    /** Tints the dyed cauldron's water (tintindex 0) to its stored {@link DyeColor}. */
    private static final class CauldronWaterTint implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return 0xFF000000 | (state.getValue(DyedWaterCauldronBlock.COLOR).getTextureDiffuseColor() & 0xFFFFFF);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of(DyedWaterCauldronBlock.COLOR);
        }
    }
}
