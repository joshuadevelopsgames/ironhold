package kingdom.smp.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes {@link Camera}'s protected positioning setters (and the detached flag) so the mirror
 *  can drive a second camera through the vanilla extract pipeline. */
@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setPosition")
    void ironhold$setPosition(Vec3 position);

    @Invoker("setRotation")
    void ironhold$setRotation(float yRot, float xRot);

    /** Detached = third-person-style; makes the dispatcher render the local player. */
    @Accessor("detached")
    void ironhold$setDetached(boolean detached);
}
