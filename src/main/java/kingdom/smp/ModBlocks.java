package kingdom.smp;

import kingdom.smp.block.BlueVinesBlock;
import kingdom.smp.block.BlueVinesPlantBlock;
import kingdom.smp.block.EbonyLeavesBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
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
    // Drops itself; mineable with any pickaxe (no tool tier requirement).
    public static final DeferredBlock<Block> FOOLS_GOLD_ORE = BLOCKS.register(
        "fools_gold_ore",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(3.0f, 3.0f)
                .sound(SoundType.STONE)
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
                GUILLOTINE_WARPED_BLOCK.get(), GUILLOTINE_EBONY_BLOCK.get()
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

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
