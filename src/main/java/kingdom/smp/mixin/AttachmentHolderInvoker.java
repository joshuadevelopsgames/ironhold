package kingdom.smp.mixin;

import java.util.Map;

import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link AttachmentHolder#getAttachmentMap()} so {@link InventoryMenuMixin} can install
 * a default {@link kingdom.smp.accessory.AccessoryInventory} without calling {@code getData},
 * which would sync while {@link net.minecraft.server.level.ServerPlayer#connection} is still null
 * during player construction.
 */
@Mixin(AttachmentHolder.class)
public interface AttachmentHolderInvoker {

    @Invoker("getAttachmentMap")
    Map<AttachmentType<?>, Object> ironhold$getAttachmentMap();
}
