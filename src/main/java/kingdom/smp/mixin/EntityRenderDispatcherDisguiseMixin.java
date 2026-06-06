package kingdom.smp.mixin;

import kingdom.smp.client.DisguiseClient;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Substitutes a disguised player's render with their chosen entity. When the dispatcher
 * extracts the render state for a player the client knows is disguised, we return the
 * disguise entity's render state instead, so the normal pipeline draws the disguise in the
 * player's place — a single, stable entity render (no re-entrant submit, no double passes).
 *
 * <p>The disguise entity's own state is extracted by re-calling {@code extractEntity}; since
 * that dummy is a mob (not a {@link Player}), this injection skips it — no recursion.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherDisguiseMixin {

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void ironhold$disguise(
            E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }
        EntityRenderDispatcher dispatcher = (EntityRenderDispatcher) (Object) this;
        EntityRenderState substitute = DisguiseClient.extractDisguise(dispatcher, player, partialTicks);
        if (substitute != null) {
            cir.setReturnValue(substitute);
        }
    }
}
