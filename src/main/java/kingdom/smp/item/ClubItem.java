package kingdom.smp.item;

import kingdom.smp.client.entity.ClubRenderer;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Club family — crude bludgeons rendered with a GeckoLib 3D model. Heavy, slow-swinging
 * melee weapons that knock foes back (the blunt "bludgeon" feel); their weight lives
 * entirely in the attribute modifiers set in {@code ModItems}. No special right-click ability.
 *
 * <p>One class drives every variant (plain / spiked / ribbed): {@code geoName} selects the
 * GeckoLib model + texture ({@code geometry.<geoName>}, {@code textures/item/<geoName>.png})
 * and {@code flavor} is the italic tooltip line. Each registration in {@code ModItems} is a
 * distinct item instance, so each gets its own renderer pointed at its own model.</p>
 */
public class ClubItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String geoName;
    private final String flavor;

    public ClubItem(Properties props, String geoName, String flavor) {
        super(props);
        this.geoName = geoName;
        this.flavor = flavor;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (flavor != null && !flavor.isEmpty()) {
            tooltip.accept(Component.literal(flavor).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static model — no animations needed for the held item.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ClubRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new ClubRenderer(geoName);
                return renderer;
            }
        });
    }
}
