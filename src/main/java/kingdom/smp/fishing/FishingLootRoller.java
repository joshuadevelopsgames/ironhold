package kingdom.smp.fishing;

import kingdom.smp.Ironhold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rolls the vanilla {@code minecraft:gameplay/fishing} loot table for a
 * given hook + player + rod context. Used by the bite minigame to
 * pre-determine the catch so the player sees the actual item they're
 * fighting for. The catch is replayed back into the
 * {@code ItemFishedEvent} at win-time so the displayed item matches the
 * awarded item.
 */
public final class FishingLootRoller {
    private FishingLootRoller() {}

    public static List<ItemStack> roll(FishingHook hook, ServerPlayer player, ItemStack rod) {
        try {
            ServerLevel sl = (ServerLevel) hook.level();
            // Match vanilla FishingHook.retrieve exactly — only ORIGIN, TOOL, and
            // THIS_ENTITY are allowed by LootContextParamSets.FISHING. Passing
            // anything else (e.g. ATTACKING_ENTITY) makes .create() throw.
            // Match vanilla retrieve()'s luck: hook.luck (Luck of the Sea bonus,
            // baked in at cast time) + player.getLuck().
            float luck = ((IFishingHookMinigame) hook).ironhold$getLuck() + player.getLuck();
            LootParams params = new LootParams.Builder(sl)
                    .withParameter(LootContextParams.ORIGIN, hook.position())
                    .withParameter(LootContextParams.TOOL, rod)
                    .withParameter(LootContextParams.THIS_ENTITY, hook)
                    .withLuck(luck)
                    .create(LootContextParamSets.FISHING);
            LootTable table = sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            List<ItemStack> out = new ArrayList<>();
            table.getRandomItems(params, out::add);
            return out;
        } catch (Throwable t) {
            // Pre-roll is best-effort — if it fails we just fall back to the motion
            // sprite for display and let vanilla retrieve roll the actual drops.
            Ironhold.LOGGER.warn("Fishing pre-roll failed; minigame will use fallback display", t);
            return Collections.emptyList();
        }
    }
}
