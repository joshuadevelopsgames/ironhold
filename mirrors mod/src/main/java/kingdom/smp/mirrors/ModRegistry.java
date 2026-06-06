package kingdom.smp.mirrors;

import kingdom.smp.mirrors.entity.MirrorEntity;
import kingdom.smp.mirrors.item.MirrorItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry for the mirror item and entity type, plus its creative-tab placement. */
public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mirrors.MODID);
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Mirrors.MODID);

    /** Wall-hung mirror — places like a painting (auto-fits the largest shape the wall allows). */
    public static final DeferredItem<Item> MIRROR = ITEMS.registerItem(
        "mirror",
        MirrorItem::new,
        props -> props.stacksTo(16));

    /** Wall-hung mirror — painting-like decoration entity; reflection rendered client-side. */
    public static final DeferredHolder<EntityType<?>, EntityType<MirrorEntity>> MIRROR_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "mirror",
            MirrorEntity::new,
            MobCategory.MISC,
            b -> b.sized(MirrorEntity.DEFAULT_WIDTH, MirrorEntity.DEFAULT_HEIGHT)
                  .eyeHeight(0.0F)
                  .clientTrackingRange(10)
                  .updateInterval(Integer.MAX_VALUE));

    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(MIRROR.get());
        }
    }
}
