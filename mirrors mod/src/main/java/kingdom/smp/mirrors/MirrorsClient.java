package kingdom.smp.mirrors;

import kingdom.smp.mirrors.client.MirrorCamDebugCommand;
import kingdom.smp.mirrors.client.MirrorReflectionEvents;
import kingdom.smp.mirrors.client.entity.MirrorRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only init: registers the mirror entity renderer and the per-frame reflection-capture hooks. */
@Mod(value = Mirrors.MODID, dist = Dist.CLIENT)
public class MirrorsClient {
    public MirrorsClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(MirrorsClient::registerEntityRenderers);
        NeoForge.EVENT_BUS.register(MirrorReflectionEvents.class);
        NeoForge.EVENT_BUS.register(MirrorCamDebugCommand.class);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.MIRROR_ENTITY.get(), MirrorRenderer::new);
    }
}
