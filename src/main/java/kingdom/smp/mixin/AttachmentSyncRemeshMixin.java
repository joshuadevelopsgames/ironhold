package kingdom.smp.mixin;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import kingdom.smp.dyewater.DyedWaterlog;

/**
 * Re-meshes a chunk when its dyed-water colour data arrives on the client (see {@link DyedWaterlog}).
 *
 * <p>The colour lives in a chunk attachment, not the block state, so the client doesn't re-render water
 * just because the colour changed — the water mesh was already built (often before the colour packet
 * landed) and would stay the plain biome colour. Both the initial chunk-send sync and mid-game updates
 * funnel through {@code AttachmentSync.receiveSyncedDataAttachments}, so marking the chunk's sections dirty
 * here makes the water re-tint as soon as the colour is known.
 */
@Mixin(AttachmentSync.class)
public class AttachmentSyncRemeshMixin {

    @Inject(method = "receiveSyncedDataAttachments(Lnet/neoforged/neoforge/attachment/AttachmentHolder;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;[B)V",
            at = @At("TAIL"))
    private static void ironhold$remeshOnWaterColor(AttachmentHolder holder, RegistryAccess registryAccess,
                                                    List<AttachmentType<?>> types, byte[] data, CallbackInfo ci) {
        if (!types.contains(DyedWaterlog.COLORS.get())) return;
        if (!(holder instanceof AttachmentHolder.AsField field)) return;
        IAttachmentHolder exposed = ((AttachmentAsFieldInvoker) (Object) field).ironhold$getExposedHolder();
        if (!(exposed instanceof LevelChunk chunk)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) return;
        ChunkPos cp = chunk.getPos();
        Level level = chunk.getLevel();
        mc.levelRenderer.setSectionRangeDirty(cp.x(), level.getMinSectionY(), cp.z(),
                                              cp.x(), level.getMaxSectionY(), cp.z());
    }
}
