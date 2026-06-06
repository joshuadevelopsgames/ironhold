package kingdom.smp.moon;

import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The moon's black sky drives a near, black fog that shrinks visibility to a few
 * blocks ("so dark, can only see 5 blocks"). Push the fog far out so the moon reads
 * as open space with the starfield behind it instead of a tiny dark bubble.
 */
@EventBusSubscriber(modid = "ironhold", value = Dist.CLIENT)
public final class MoonFogHandler {

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Entity camEntity = event.getCamera().entity();
        if (camEntity == null) return;
        if (!camEntity.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)) return;

        // Effectively disable the close fog: start it far and end it well past view range.
        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), 1024.0f));
    }
}
