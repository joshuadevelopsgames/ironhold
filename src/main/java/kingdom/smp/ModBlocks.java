package kingdom.smp;

import kingdom.smp.block.BlueVinesBlock;
import kingdom.smp.block.BlueVinesPlantBlock;
import kingdom.smp.block.EbonyLeavesBlock;
import kingdom.smp.block.FoolsGoldOreBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block and block-entity registrations, split out of {@link Ironhold}.
 *  Block-item registrations stay in Ironhold and reference these holders. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ironhold.MODID);

    public static final DeferredBlock<Block> TANZANITE_ORE = BLOCKS.register(
        "tanzanite_ore",
        id -> new DropExperienceBlock(
            UniformInt.of(4, 9),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .requiresCorrectToolForDrops()
                .strength(52.0f, 1200.0f)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // ── Fool's gold ore ───────────────────────────────────────────────────────
    // Worldgen: same y-range as gold ore (-64..32, trapezoid), count 3 per chunk
    // (vanilla gold uses count 4 → ~75% which matches the "2/3 frequency" intent).
    // Drops fool's gold item (silk touch drops the block); requires iron pickaxe or better.
    public static final DeferredBlock<Block> FOOLS_GOLD_ORE = BLOCKS.register(
        "fools_gold_ore",
        id -> new FoolsGoldOreBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(3.0f, 3.0f)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Deepslate variant — generated in the deepslate layer (see fools_gold_ore configured feature).
    // Drops fool's gold item (silk touch drops the block); harder, like vanilla deepslate gold ore.
    // Requires iron pickaxe or better.
    public static final DeferredBlock<Block> DEEPSLATE_FOOLS_GOLD_ORE = BLOCKS.register(
        "deepslate_fools_gold_ore",
        id -> new FoolsGoldOreBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(4.5f, 3.0f)
                .sound(SoundType.DEEPSLATE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<Block> EBONY_LOG = BLOCKS.register(
        "ebony_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<Block> EBONY_PLANKS = BLOCKS.register(
        "ebony_planks",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_EBONY_LOG = BLOCKS.register(
        "stripped_ebony_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> EBONY_SLAB = BLOCKS.register(
        "ebony_slab",
        id -> new net.minecraft.world.level.block.SlabBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> EBONY_STAIRS = BLOCKS.register(
        "ebony_stairs",
        id -> new net.minecraft.world.level.block.StairBlock(
            EBONY_PLANKS.get().defaultBlockState(),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.FenceBlock> EBONY_FENCE = BLOCKS.register(
        "ebony_fence",
        id -> new net.minecraft.world.level.block.FenceBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.FenceGateBlock> EBONY_FENCE_GATE = BLOCKS.register(
        "ebony_fence_gate",
        id -> new net.minecraft.world.level.block.FenceGateBlock(
            net.minecraft.world.level.block.state.properties.WoodType.DARK_OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.DoorBlock> EBONY_DOOR = BLOCKS.register(
        "ebony_door",
        id -> new net.minecraft.world.level.block.DoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.TrapDoorBlock> EBONY_TRAPDOOR = BLOCKS.register(
        "ebony_trapdoor",
        id -> new net.minecraft.world.level.block.TrapDoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.ButtonBlock> EBONY_BUTTON = BLOCKS.register(
        "ebony_button",
        id -> new net.minecraft.world.level.block.ButtonBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            30,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.PressurePlateBlock> EBONY_PRESSURE_PLATE = BLOCKS.register(
        "ebony_pressure_plate",
        id -> new net.minecraft.world.level.block.PressurePlateBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // ── Chinese Cedar wood (pink heartwood, tan birch-style bark) ─────────────
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> CHINESE_CEDAR_LOG = BLOCKS.register(
        "chinese_cedar_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<Block> CHINESE_CEDAR_PLANKS = BLOCKS.register(
        "chinese_cedar_planks",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_CHINESE_CEDAR_LOG = BLOCKS.register(
        "stripped_chinese_cedar_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Stripped wood — all six faces show the stripped-log side grain.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_CHINESE_CEDAR_WOOD = BLOCKS.register(
        "stripped_chinese_cedar_wood",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> CHINESE_CEDAR_SLAB = BLOCKS.register(
        "chinese_cedar_slab",
        id -> new net.minecraft.world.level.block.SlabBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> CHINESE_CEDAR_STAIRS = BLOCKS.register(
        "chinese_cedar_stairs",
        id -> new net.minecraft.world.level.block.StairBlock(
            CHINESE_CEDAR_PLANKS.get().defaultBlockState(),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.FenceBlock> CHINESE_CEDAR_FENCE = BLOCKS.register(
        "chinese_cedar_fence",
        id -> new net.minecraft.world.level.block.FenceBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.FenceGateBlock> CHINESE_CEDAR_FENCE_GATE = BLOCKS.register(
        "chinese_cedar_fence_gate",
        id -> new net.minecraft.world.level.block.FenceGateBlock(
            net.minecraft.world.level.block.state.properties.WoodType.DARK_OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.DoorBlock> CHINESE_CEDAR_DOOR = BLOCKS.register(
        "chinese_cedar_door",
        id -> new net.minecraft.world.level.block.DoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.TrapDoorBlock> CHINESE_CEDAR_TRAPDOOR = BLOCKS.register(
        "chinese_cedar_trapdoor",
        id -> new net.minecraft.world.level.block.TrapDoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.ButtonBlock> CHINESE_CEDAR_BUTTON = BLOCKS.register(
        "chinese_cedar_button",
        id -> new net.minecraft.world.level.block.ButtonBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            30,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<net.minecraft.world.level.block.PressurePlateBlock> CHINESE_CEDAR_PRESSURE_PLATE = BLOCKS.register(
        "chinese_cedar_pressure_plate",
        id -> new net.minecraft.world.level.block.PressurePlateBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Ebony leaves — generated by ebony trees in the Ebonwood Hollow biome.
    // Fixed-colour texture (dark ashen grey-green); does NOT use biome tint.
    public static final DeferredBlock<EbonyLeavesBlock> EBONY_LEAVES = BLOCKS.register(
        "ebony_leaves",
        id -> new EbonyLeavesBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.2f)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((s, l, p, e) -> false)
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Bat Flower — tall two-block dark flower native to Ebonwood Hollow.
    public static final DeferredBlock<TallFlowerBlock> BAT_FLOWER = BLOCKS.register(
        "bat_flower",
        id -> new TallFlowerBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .noCollision()
                .strength(0.0f)
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Orange Glowshrooms — flowerbed-style (1-4) glowing mushrooms native to Ebonwood Hollow.
    public static final DeferredBlock<FlowerBedBlock> ORANGE_GLOWSHROOMS = BLOCKS.register(
        "orange_glowshrooms",
        id -> new FlowerBedBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollision()
                .strength(0.0f)
                .sound(SoundType.GRASS)
                .lightLevel(state -> 2)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        ) {}
    );

    // Blue Glowshrooms — flowerbed-style (1-4) glowing mushrooms that grow on the Moon's surface.
    public static final DeferredBlock<FlowerBedBlock> BLUE_GLOWSHROOMS = BLOCKS.register(
        "blue_glowshrooms",
        id -> new FlowerBedBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .noCollision()
                .strength(0.0f)
                .sound(SoundType.GRASS)
                .lightLevel(state -> 2)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        ) {}
    );

    // Ebonwood Grass — grass block with mud sides/bottom, generated on biome surface.
    public static final DeferredBlock<net.minecraft.world.level.block.GrassBlock> EBONWOOD_GRASS = BLOCKS.register(
        "ebonwood_grass",
        id -> new net.minecraft.world.level.block.GrassBlock(
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Black Sand — replaces sand patches near water in the Ebonwood Hollow biome.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> BLACK_SAND = BLOCKS.register(
        "black_sand",
        id -> new net.minecraft.world.level.block.ColoredFallingBlock(
            new net.minecraft.util.ColorRGBA(0x08070EFF),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.SAND)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Dark Gravel — replaces gravel patches near water in the Ebonwood Hollow biome.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> DARK_GRAVEL = BLOCKS.register(
        "dark_gravel",
        id -> new net.minecraft.world.level.block.ColoredFallingBlock(
            new net.minecraft.util.ColorRGBA(0x1E1E26FF),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.6f)
                .sound(SoundType.GRAVEL)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // Blue Vines — upward-growing vine native to Ebonwood Hollow, deep-blue variant.
    public static final DeferredBlock<BlueVinesBlock> BLUE_VINES = BLOCKS.register(
        "blue_vines",
        id -> new BlueVinesBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.WEEPING_VINES)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<BlueVinesPlantBlock> BLUE_VINES_PLANT = BLOCKS.register(
        "blue_vines_plant",
        id -> new BlueVinesPlantBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .noCollision()
                .instabreak()
                .sound(SoundType.WEEPING_VINES)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // ── Moon Dimension Blocks ───────────────────────────────────────────────────
    public static final DeferredBlock<Block> MOON_STONE = BLOCKS.register(
        "moon_stone",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(1.5f, 6.0f)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<kingdom.smp.block.MoonDustBlock> MOON_DUST = BLOCKS.register(
        "moon_dust",
        id -> new kingdom.smp.block.MoonDustBlock(
            new net.minecraft.util.ColorRGBA(0x8C8C91FF), // Silver/grey tint
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(0.5f)
                .sound(SoundType.SAND)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredBlock<kingdom.smp.block.MoonPortalBlock> MOON_PORTAL = BLOCKS.register(
        "moon_portal",
        id -> new kingdom.smp.block.MoonPortalBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .noCollision()
                .randomTicks()
                .strength(-1.0F)
                .sound(SoundType.GLASS)
                .lightLevel((state) -> 11)
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    /** Light-blue mushroom — render-prop only (no BlockItem). Supplies the baked
     *  block model for the Moonshroom's back-mushrooms, recoloured from vanilla red. */
    public static final DeferredBlock<Block> MOON_MUSHROOM = BLOCKS.register(
        "moon_mushroom",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .noCollision()
                .instabreak()
                .noOcclusion()
                .sound(SoundType.GRASS)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    // ── Guillotine blocks (wood variants) ───────────────────────────────────────
    private static DeferredBlock<Block> guillotineBlock(String name) {
        return BLOCKS.register(name,
            id -> new kingdom.smp.block.GuillotineBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            )
        );
    }

    public static final DeferredBlock<Block> GUILLOTINE_OAK_BLOCK      = guillotineBlock("guillotine_oak");

    public static final DeferredBlock<Block> GUILLOTINE_SPRUCE_BLOCK   = guillotineBlock("guillotine_spruce");

    public static final DeferredBlock<Block> GUILLOTINE_BIRCH_BLOCK    = guillotineBlock("guillotine_birch");

    public static final DeferredBlock<Block> GUILLOTINE_JUNGLE_BLOCK   = guillotineBlock("guillotine_jungle");

    public static final DeferredBlock<Block> GUILLOTINE_ACACIA_BLOCK   = guillotineBlock("guillotine_acacia");

    public static final DeferredBlock<Block> GUILLOTINE_DARK_OAK_BLOCK = guillotineBlock("guillotine_dark_oak");

    public static final DeferredBlock<Block> GUILLOTINE_MANGROVE_BLOCK = guillotineBlock("guillotine_mangrove");

    public static final DeferredBlock<Block> GUILLOTINE_CHERRY_BLOCK   = guillotineBlock("guillotine_cherry");

    public static final DeferredBlock<Block> GUILLOTINE_CRIMSON_BLOCK  = guillotineBlock("guillotine_crimson");

    public static final DeferredBlock<Block> GUILLOTINE_WARPED_BLOCK   = guillotineBlock("guillotine_warped");

    public static final DeferredBlock<Block> GUILLOTINE_EBONY_BLOCK   = guillotineBlock("guillotine_ebony");

    public static final DeferredBlock<Block> GUILLOTINE_CHINESE_CEDAR_BLOCK = guillotineBlock("guillotine_chinese_cedar");

    // Block entity type — shared by all guillotine variants
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ironhold.MODID);

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.GuillotineBlockEntity>> GUILLOTINE_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("guillotine",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.GuillotineBlockEntity::new,
                GUILLOTINE_OAK_BLOCK.get(), GUILLOTINE_SPRUCE_BLOCK.get(), GUILLOTINE_BIRCH_BLOCK.get(),
                GUILLOTINE_JUNGLE_BLOCK.get(), GUILLOTINE_ACACIA_BLOCK.get(), GUILLOTINE_DARK_OAK_BLOCK.get(),
                GUILLOTINE_MANGROVE_BLOCK.get(), GUILLOTINE_CHERRY_BLOCK.get(), GUILLOTINE_CRIMSON_BLOCK.get(),
                GUILLOTINE_WARPED_BLOCK.get(), GUILLOTINE_EBONY_BLOCK.get(),
                GUILLOTINE_CHINESE_CEDAR_BLOCK.get()
            )
        );

    // ── Chorus Wardheart (force-field shield generator) ───────────────────────
    public static final DeferredBlock<kingdom.smp.block.wardheart.WardheartBlock> WARDHEART_BLOCK =
        BLOCKS.register("wardheart",
            id -> new kingdom.smp.block.wardheart.WardheartBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.5f, 1200.0f)   // tough, blast-resistant
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 9)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.wardheart.WardheartBlockEntity>>
            WARDHEART_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("wardheart",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.wardheart.WardheartBlockEntity::new,
                WARDHEART_BLOCK.get()));

    // ── Class Stone (Tier 1 class-selection pedestal) ─────────────────────────
    /**
     * Pedestal that opens the Tier 1 class-selection screen on right-click.
     * Visually it's a 12-tall slab with four rotating items hovering above —
     * sword, bow, arcane scepter, enchanted book — driven by
     * {@link kingdom.smp.client.block.ClassStoneRenderer}.
     */
    public static final DeferredBlock<kingdom.smp.block.ClassStoneBlock> CLASS_STONE_BLOCK =
        BLOCKS.register("class_stone",
            id -> new kingdom.smp.block.ClassStoneBlock(
                // Bedrock-style indestructibility: -1.0 hardness = no break progress
                // ever in survival; 3,600,000 blast resistance = creeper / wither / TNT
                // and even end crystals can't touch it. Creative players still break
                // instantly because creative bypasses the destroy-progress check.
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(s -> 7)
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.ClassStoneBlockEntity>>
            CLASS_STONE_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("class_stone",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.ClassStoneBlockEntity::new,
                CLASS_STONE_BLOCK.get()));

    // ── Ender Shrine (totem-fueled sanctuary revive) ──────────────────────────
    public static final DeferredBlock<kingdom.smp.block.EnderShrineBlock> ENDER_SHRINE_BLOCK =
        BLOCKS.register("ender_shrine",
            id -> new kingdom.smp.block.EnderShrineBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0f, 1200.0f)   // tough, blast-resistant base
                    .sound(SoundType.STONE)
                    .lightLevel(s -> 7)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.EnderShrineBlockEntity>>
            ENDER_SHRINE_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("ender_shrine",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.EnderShrineBlockEntity::new,
                ENDER_SHRINE_BLOCK.get()));

    // ── Tripwire rack ─────────────────────────────────────────────────────────
    // Vanilla tripwire hooks double as a wall rack that can hold a single tool /
    // item. The EntityBlock interface is grafted onto TripWireHookBlock by
    // {@link kingdom.smp.mixin.TripWireHookEntityBlockMixin}, so this BE type is
    // bound to the vanilla block rather than one of ours.
    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.TripwireRackBlockEntity>>
            TRIPWIRE_RACK_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("tripwire_rack",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.TripwireRackBlockEntity::new,
                net.minecraft.world.level.block.Blocks.TRIPWIRE_HOOK));

    // ── Magma Crust ───────────────────────────────────────────────────────────
    // Temporary, self-reverting solid laid over lava by the Magma Boots so the
    // wearer can walk across it (Frost-Walker style). Glows, safe to stand on,
    // no BlockItem (world-only). See {@link kingdom.smp.block.MagmaCrustBlock}.
    public static final DeferredBlock<kingdom.smp.block.MagmaCrustBlock> MAGMA_CRUST =
        BLOCKS.register("magma_crust",
            id -> new kingdom.smp.block.MagmaCrustBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f)
                    .sound(SoundType.BASALT)
                    .lightLevel(s -> 7)
                    .noLootTable()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    // ── Butterfly Terrarium ───────────────────────────────────────────────────
    // Placeable glass jar that displays up to 3 captured butterflies. Right-click
    // with a butterfly to add, empty-hand to take one back; breaking keeps the
    // butterflies inside (BUTTERFLY_JAR_CONTENTS component). See ButterflyJarBlock.
    public static final DeferredBlock<kingdom.smp.block.ButterflyJarBlock> BUTTERFLY_TERRARIUM =
        BLOCKS.register("butterfly_terrarium",
            id -> new kingdom.smp.block.ButterflyJarBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isViewBlocking((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.ButterflyJarBlockEntity>>
            BUTTERFLY_TERRARIUM_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("butterfly_terrarium",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.ButterflyJarBlockEntity::new,
                BUTTERFLY_TERRARIUM.get()));

    // ── Chalice ───────────────────────────────────────────────────────────────
    // Placeable golden goblet you can pour any liquid into (water/lava/milk/powder
    // snow buckets, honey bottles, any potion). The held liquid renders inside the
    // cup via ChaliceRenderer; right-click empty-hand tips it out. See ChaliceBlock.
    public static final DeferredBlock<kingdom.smp.block.ChaliceBlock> CHALICE =
        BLOCKS.register("chalice",
            id -> new kingdom.smp.block.ChaliceBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isViewBlocking((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            ));

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.ChaliceBlockEntity>>
            CHALICE_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("chalice",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.ChaliceBlockEntity::new,
                CHALICE.get()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
