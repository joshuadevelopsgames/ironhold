package kingdom.smp.fishing;

import kingdom.smp.Ironhold;
import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.gear.QualityScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    /**
     * Extra effective luck contributed by a full-power (100%) bait. Bait power
     * is a percentage, so a 50%-power butterfly adds {@code 0.5 * this}. Mirrors
     * Terraria, where bait power feeds fishing power and biases the catch toward
     * higher-tier results.
     */
    private static final float BAIT_LUCK_AT_FULL_POWER = 3.0f;

    /**
     * @param baitPowerPercent bait power as a percentage (0 = no bait); raises
     *                         effective luck and the chance to bump caught gear
     *                         up a quality tier, so stronger bait yields better catches
     * @param baitTheme        optional themed-loot key from the bait ({@code "nether"},
     *                         {@code "end"}, {@code "soul"}, …) or {@code null}; biases
     *                         the catch toward dimension-appropriate bonus loot
     */
    public static List<ItemStack> roll(FishingHook hook, ServerPlayer player, ItemStack rod,
                                       int baitPowerPercent, String baitTheme) {
        try {
            ServerLevel sl = (ServerLevel) hook.level();
            // Match vanilla FishingHook.retrieve exactly — only ORIGIN, TOOL, and
            // THIS_ENTITY are allowed by LootContextParamSets.FISHING. Passing
            // anything else (e.g. ATTACKING_ENTITY) makes .create() throw.
            // Match vanilla retrieve()'s luck: hook.luck (Luck of the Sea bonus,
            // baked in at cast time) + player.getLuck(), plus the bait bonus.
            float luck = ((IFishingHookMinigame) hook).ironhold$getLuck() + player.getLuck()
                    + (baitPowerPercent / 100.0f) * BAIT_LUCK_AT_FULL_POWER;
            LootParams params = new LootParams.Builder(sl)
                    .withParameter(LootContextParams.ORIGIN, hook.position())
                    .withParameter(LootContextParams.TOOL, rod)
                    .withParameter(LootContextParams.THIS_ENTITY, hook)
                    .withLuck(luck)
                    .create(LootContextParamSets.FISHING);
            LootTable table = sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            List<ItemStack> out = new ArrayList<>();
            table.getRandomItems(params, out::add);
            applyBaitQuality(out, player, baitPowerPercent);
            applyThemedBias(out, player, baitTheme);
            return out;
        } catch (Throwable t) {
            // Pre-roll is best-effort — if it fails we just fall back to the motion
            // sprite for display and let vanilla retrieve roll the actual drops.
            Ironhold.LOGGER.warn("Fishing pre-roll failed; minigame will use fallback display", t);
            return Collections.emptyList();
        }
    }

    /**
     * Gear-quality lever: for each quality-eligible catch, bait gives a
     * {@code power}% chance to bump it one {@link ItemQuality} tier toward MINT.
     * Stronger bait → reliably better gear, on top of the luck bonus.
     */
    private static void applyBaitQuality(List<ItemStack> drops, ServerPlayer player, int baitPowerPercent) {
        if (baitPowerPercent <= 0) return;
        RandomSource random = player.getRandom();
        for (ItemStack stack : drops) {
            if (!QualityScope.isEligible(stack)) continue;
            if (random.nextInt(100) < baitPowerPercent) {
                ItemQuality current = GearComponents.getQuality(stack);
                int next = Math.min(ItemQuality.MINT.ordinal(), current.ordinal() + 1);
                GearComponents.setQuality(stack, ItemQuality.byId(next));
            }
        }
    }

    /**
     * Themed-loot lever: a dimensional bait (Nether/End/Soul butterflies) has a
     * chance to add a small dimension-appropriate bonus catch. Starting point —
     * tune the chance and swap for dedicated themed loot tables as the
     * dimensional butterflies are finalized in Phase 3.
     */
    private static void applyThemedBias(List<ItemStack> drops, ServerPlayer player, String baitTheme) {
        if (baitTheme == null || drops.isEmpty()) return;
        if (player.getRandom().nextInt(100) >= 20) return; // ~20% bonus-catch chance
        ItemStack bonus = switch (baitTheme) {
            case "nether" -> new ItemStack(Items.MAGMA_CREAM);
            case "end" -> new ItemStack(Items.CHORUS_FRUIT);
            case "soul" -> new ItemStack(Items.GHAST_TEAR);
            default -> ItemStack.EMPTY;
        };
        if (!bonus.isEmpty()) drops.add(bonus);
    }
}
