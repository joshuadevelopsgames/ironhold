package kingdom.smp.mixin;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jspecify.annotations.Nullable;

/** Exposes {@link ViewArea}'s protected by-node section lookup so the mirror cull can read loaded sections directly. */
@Mixin(ViewArea.class)
public interface ViewAreaInvoker {

    @Invoker("getRenderSection")
    SectionRenderDispatcher.@Nullable RenderSection ironhold$getRenderSection(long sectionNode);
}
