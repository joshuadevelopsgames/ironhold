package kingdom.smp.item;

import java.util.function.Consumer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.client.entity.HalricStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Warden Halric's staff — ceremonial weapon of the gatekeeper of Wayfarer's Hollow.
 * Held contexts render through GeckoLib so the chain can sway (and eventually
 * react to player motion via a procedural rope sim).
 */
public class HalricStaffItem extends Item implements GeoItem {

    private static final String CONTROLLER_NAME = "chain_controller";
    private static final RawAnimation IDLE =
        RawAnimation.begin().thenLoop("animation.halric_staff.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HalricStaffItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Halric's Staff").withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Warden of Wayfarer's Hollow")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // ── GeckoLib ─────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HalricStaffItem>(CONTROLLER_NAME, 0,
            state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HalricStaffRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new HalricStaffRenderer();
                return renderer;
            }
        });
    }
}
