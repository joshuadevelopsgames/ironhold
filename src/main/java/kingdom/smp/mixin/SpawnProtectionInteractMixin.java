package kingdom.smp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets players use blocks (doors, buttons, levers, containers, …) inside the dedicated-server
 * spawn-protection radius. Vanilla {@code handleUseItemOn} short-circuits every right-click on a
 * block when {@code isUnderSpawnProtection} is true, which also stops harmless interactions like
 * opening a door. We neutralise the two gates guarding that path so the interaction runs; world
 * modification (placing blocks / using items on blocks) is still blocked separately by
 * {@link SpawnProtectionPlacementMixin}, and block breaking is untouched.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SpawnProtectionInteractMixin {

    @Redirect(
        method = "handleUseItemOn",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isUnderSpawnProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean ironhold$allowInteractInSpawn(MinecraftServer server, ServerLevel level, BlockPos pos, Player player) {
        // Don't block the interaction here; placement is still gated in useItemOn.
        return false;
    }

    @Redirect(
        method = "handleUseItemOn",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean ironhold$mayInteractIgnoringSpawn(ServerLevel level, Entity entity, BlockPos pos) {
        // Drop the spawn-protection term but keep the world-border check.
        return level.getWorldBorder().isWithinBounds(pos);
    }
}
