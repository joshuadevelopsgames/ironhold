package kingdom.smp.dyewater;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import kingdom.smp.Ironhold;

/**
 * 16-colour "dyed water". Coloured water <em>is</em> ordinary {@link Fluids#WATER}; the colour is stored
 * per block position in {@link DyedWaterlog} and applied as a render tint. Because it is genuinely water,
 * every vanilla water behaviour — waterlogging (stairs and all), lava → cobble/obsidian, fishing, sponges,
 * swimming, infinite sources, freezing — works with no special-casing.
 *
 * <p>A {@link DyedWaterBucketItem} is just a water bucket that also remembers which colour to stamp at the
 * spot it pours. Scooping coloured water back gives the matching bucket (see {@code LiquidBlockDyedPickupMixin}).
 */
public final class DyedWater {
    private DyedWater() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ironhold.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ironhold.MODID);

    /** The colour-remembering water cauldron (one block, COLOR is a blockstate property). */
    public static final DeferredBlock<DyedWaterCauldronBlock> CAULDRON = BLOCKS.register(
        "dyed_water_cauldron",
        id -> new DyedWaterCauldronBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
                .setId(ResourceKey.create(Registries.BLOCK, id))));

    /** Per-colour water buckets, keyed by {@link DyeColor}. */
    public static final Map<DyeColor, DeferredItem<DyedWaterBucketItem>> BUCKET = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            BUCKET.put(color, ITEMS.registerItem("dyed_water_bucket_" + color.getName(),
                p -> new DyedWaterBucketItem(color, p.craftRemainder(Items.BUCKET).stacksTo(1))));
        }
    }

    /** RGB (0xRRGGBB) tint for a colour, derived from the vanilla dye texture colour. */
    public static int rgb(DyeColor color) {
        return color.getTextureDiffuseColor() & 0xFFFFFF;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(DyedWater::addToCreativeTab);
    }

    private static void addToCreativeTab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (DyeColor color : DyeColor.values()) {
                event.accept(BUCKET.get(color).get());
            }
        }
    }

    /**
     * A water bucket that stamps its {@link #color} at the spot it pours. Placing/waterlogging is vanilla
     * {@link BucketItem} behaviour (content is {@link Fluids#WATER}); we only record the colour afterwards.
     */
    public static final class DyedWaterBucketItem extends BucketItem {
        public final DyeColor color;

        public DyedWaterBucketItem(DyeColor color, Item.Properties props) {
            super(Fluids.WATER, props);
            this.color = color;
        }

        @Override
        public boolean emptyContents(LivingEntity user, Level level, BlockPos pos, BlockHitResult hitResult) {
            boolean placed = super.emptyContents(user, level, pos, hitResult);
            // Only stamp where water actually ended up (super recurses to an adjacent pos when the
            // clicked block can't hold the fluid).
            if (placed && !level.isClientSide() && level.getFluidState(pos).getType() == Fluids.WATER) {
                DyedWaterlog.set(level, pos, color);
            }
            return placed;
        }
    }
}
