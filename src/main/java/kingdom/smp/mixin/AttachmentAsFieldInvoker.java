package kingdom.smp.mixin;

import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the package-private {@code AsField.getExposedHolder()} so we can resolve the owning chunk. */
@Mixin(AttachmentHolder.AsField.class)
public interface AttachmentAsFieldInvoker {

    @Invoker("getExposedHolder")
    IAttachmentHolder ironhold$getExposedHolder();
}
