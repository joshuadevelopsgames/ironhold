package milkucha.trmt;

import milkucha.trmt.block.ErodedDirtBlock;
import milkucha.trmt.block.ErodedGrassBlock;
import milkucha.trmt.block.ErodedSandBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge port of upstream {@code TRMTBlocks}. Upstream's direct
 * {@code Registry.register(BuiltInRegistries.BLOCK, ...)} pattern doesn't
 * work on NeoForge (modded registries are sealed before {@code @Mod} static
 * init runs), so we route through {@link DeferredRegister}.
 *
 * <p>The {@code public static Block ERODED_X} fields keep the upstream
 * field shape — every mixin and erosion-package reference works unchanged.
 * They are populated by {@link #resolve()} during {@code FMLCommonSetupEvent},
 * after all block registrations have fired.
 */
public final class TRMTBlocks {

    private static final DeferredRegister.Blocks REG =
            DeferredRegister.createBlocks(TRMT.MOD_ID);

    private static final DeferredBlock<ErodedDirtBlock> H_DIRT = REG.register(
            "eroded_dirt",
            id -> new ErodedDirtBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                            .randomTicks()
                            .setId(ResourceKey.create(Registries.BLOCK, id))));

    private static final DeferredBlock<ErodedDirtBlock> H_COARSE_DIRT = REG.register(
            "eroded_coarse_dirt",
            id -> new ErodedDirtBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.COARSE_DIRT)
                            .randomTicks()
                            .setId(ResourceKey.create(Registries.BLOCK, id))));

    /** Exposed for client color-handler registration (called before {@link #resolve()} runs). */
    public static final DeferredBlock<ErodedGrassBlock> H_GRASS = REG.register(
            "eroded_grass_block",
            id -> new ErodedGrassBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                            .mapColor(MapColor.DIRT)
                            .randomTicks()
                            .setId(ResourceKey.create(Registries.BLOCK, id))));

    private static final DeferredBlock<ErodedSandBlock> H_SAND = REG.register(
            "eroded_sand",
            id -> new ErodedSandBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
                            .mapColor(MapColor.TERRACOTTA_YELLOW)
                            .noOcclusion()
                            .randomTicks()
                            .setId(ResourceKey.create(Registries.BLOCK, id))));

    /** Populated by {@link #resolve()} once the BLOCK registry is built. */
    public static Block ERODED_DIRT;
    public static Block ERODED_COARSE_DIRT;
    public static Block ERODED_GRASS_BLOCK;
    public static Block ERODED_SAND;

    private TRMTBlocks() {}

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }

    /** Wire holder values into the legacy {@code public static Block} fields. */
    public static void resolve() {
        ERODED_DIRT = H_DIRT.value();
        ERODED_COARSE_DIRT = H_COARSE_DIRT.value();
        ERODED_GRASS_BLOCK = H_GRASS.value();
        ERODED_SAND = H_SAND.value();
    }

    static void touch() {}
}
