package kingdom.smp.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the pieces of {@link LevelRenderer} the mirror reflection needs to build its own visible-section
 * set for the capture pass: the section storage, the two visible lists that {@code prepareChunkRenders}
 * and block-entity extraction read, and {@code compileSections} so never-yet-seen sections behind the
 * player get their meshes scheduled (vanilla only compiles sections the main camera can see).
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("viewArea")
    @Nullable ViewArea ironhold$getViewArea();

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> ironhold$getVisibleSections();

    @Accessor("nearbyVisibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> ironhold$getNearbyVisibleSections();

    @Invoker("compileSections")
    void ironhold$compileSections(Camera camera);
}
