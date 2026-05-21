package kingdom.smp.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import kingdom.smp.fishing.FishingMinigameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Substitutes the bite-minigame's pre-rolled catch into vanilla's
 * {@code FishingHook.retrieve}. Vanilla rolls the fishing loot table and
 * spawns the resulting items directly (it does not route the drops through
 * a mod-cancellable event in a way we can reliably mutate). By redirecting
 * the {@code LootTable.getRandomItems} call we make {@code retrieve} spawn
 * exactly the items the player saw in the minigame, guaranteeing
 * display == catch. When there's no pending pre-roll (e.g. a creative test
 * cast that skipped the minigame) we fall through to the real roll.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookRetrieveMixin {

    @Redirect(method = "retrieve",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> ironhold$usePreRolledCatch(LootTable table, LootParams params) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.getPlayerOwner() instanceof ServerPlayer sp) {
            List<ItemStack> preRolled = FishingMinigameManager.takePreRolledForRetrieve(sp);
            if (preRolled != null && !preRolled.isEmpty()) {
                ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
                for (ItemStack s : preRolled) result.add(s.copy());
                return result;
            }
        }
        return table.getRandomItems(params);
    }
}
