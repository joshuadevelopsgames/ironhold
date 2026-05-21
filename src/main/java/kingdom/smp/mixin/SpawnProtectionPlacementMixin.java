package kingdom.smp.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps spawn protection's anti-grief teeth: even though {@link SpawnProtectionInteractMixin} now
 * lets right-clicks reach {@code useItemOn}, world-modifying item uses (placing blocks, buckets,
 * flint &amp; steel, hoes, …) must still be refused inside the spawn-protection radius. The block's
 * own interaction (door/button/container) already returns earlier in {@code useItemOn}, so this
 * only intercepts the trailing {@code itemStack.useOn} placement step. Ops and out-of-radius
 * positions are unaffected because {@code isUnderSpawnProtection} handles those.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class SpawnProtectionPlacementMixin {

    @Redirect(
        method = "useItemOn",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult ironhold$blockPlacementInSpawn(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null && level instanceof ServerLevel serverLevel
                && serverLevel.getServer().isUnderSpawnProtection(serverLevel, context.getClickedPos(), player)) {
            return InteractionResult.PASS;
        }
        return stack.useOn(context);
    }
}
