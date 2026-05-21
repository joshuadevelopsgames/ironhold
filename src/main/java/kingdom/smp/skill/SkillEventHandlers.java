package kingdom.smp.skill;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side event handlers that turn profession ranks into actual gameplay effects.
 *
 * Effects implemented in v1:
 * <ul>
 *   <li><b>Mining</b> — per-rank +5/10/20/30% chance for an ore-break to drop one duplicate
 *       item. Master replaces the chance with <i>Veinbreaker</i>: breaking an ore vein-mines
 *       adjacent same-block ores (max {@value #VEINBREAKER_MAX} blocks per chain).</li>
 *   <li><b>Farming</b> — per-rank +5/10/20/30/40% chance for a crop break to duplicate any drop.</li>
 *   <li><b>Fishing</b> — per-rank +5/10/20/30/40% chance for a fishing catch to be duplicated.</li>
 *   <li><b>Cooking</b> — per-rank +0.5/1.0/1.5/2.0/2.5 saturation bonus when finishing food consumption.</li>
 * </ul>
 *
 * Deferred (need other systems first):
 * <ul>
 *   <li>Alchemy — brewing-stand interception</li>
 *   <li>Enchanting — enchant-table integration</li>
 *   <li>Trading — villager-trade interception</li>
 * </ul>
 */
public final class SkillEventHandlers {
    private SkillEventHandlers() {}

    private static final int VEINBREAKER_MAX = 32;

    /** Per-player re-entry guard so a vein-break recursive chain doesn't restart itself. */
    private static final Set<UUID> activeVeinBreakers = new HashSet<>();

    /**
     * Mining gate — cancel breaking gated ore blocks if the player lacks the required rank.
     * Runs at HIGHEST priority so the cancellation happens before Veinbreaker / drop handlers fire.
     * Creative-mode players bypass the gate.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreakGate(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (player.getAbilities().instabuild) return;

        BlockState state = event.getState();
        ProfessionRank required = MiningGating.requiredRank(state);
        if (required == null) return;
        if (SkillEffects.hasAtLeast(player, Profession.MINING, required)) return;

        event.setCanceled(true);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(
                    Component.literal("Requires Mining: " + required.displayName())
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }
    }

    @SubscribeEvent
    public static void onBlockBreakVeinbreaker(BreakBlockEvent event) {
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp)) return;
        BlockState state = event.getState();
        if (!state.is(Tags.Blocks.ORES)) return;
        if (!SkillEffects.hasAtLeast(sp, Profession.MINING, ProfessionRank.MASTER)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        UUID id = sp.getUUID();
        if (!activeVeinBreakers.add(id)) return;
        try {
            veinBreak(level, event.getPos(), state.getBlock(), sp);
        } finally {
            activeVeinBreakers.remove(id);
        }
    }

    @SubscribeEvent
    public static void onOreOrCropDrops(BlockDropsEvent event) {
        Entity breakerEntity = event.getBreaker();
        if (!(breakerEntity instanceof Player player)) return;
        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        Profession profession;
        if (state.is(Tags.Blocks.ORES)) {
            // Master Mining gets Veinbreaker instead of bonus drops.
            if (SkillEffects.hasAtLeast(player, Profession.MINING, ProfessionRank.MASTER)) return;
            profession = Profession.MINING;
        } else if (state.is(BlockTags.CROPS)) {
            profession = Profession.FARMING;
        } else {
            return;
        }

        int chance = SkillEffects.extraDropChancePercent(player, profession);
        if (chance <= 0) return;
        if (level.getRandom().nextInt(100) >= chance) return;

        List<ItemEntity> drops = event.getDrops();
        if (drops.isEmpty()) return;
        ItemEntity sample = drops.get(0);
        ItemStack copy = sample.getItem().copy();
        copy.setCount(1);
        ItemEntity bonus = new ItemEntity(level,
                sample.getX(), sample.getY(), sample.getZ(), copy);
        bonus.setDefaultPickUpDelay();
        drops.add(bonus);
    }

    // Fishing catches are handled entirely by the bite-minigame pipeline:
    //   • FishingMinigameManager pre-rolls loot + folds in the bonus-drop perk
    //   • FishingHookRetrieveMixin substitutes those drops into retrieve()
    //   • FishingMinigameManager.resolve awards use-skill XP on win
    // (Older ItemFishedEvent handlers were removed — vanilla retrieve() spawns
    // drops directly without a reliably-mutable event in this version.)

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();
        if (!item.has(DataComponents.FOOD)) return;

        ProfessionRank rank = SkillEffects.rankFor(player, Profession.COOKING);
        if (rank == null) return;
        float bonus = (rank.order() + 1) * 0.5f; // Novice +0.5 ... Master +2.5

        FoodData food = player.getFoodData();
        // Saturation can't exceed current food level; clamp.
        float newSaturation = Math.min(food.getFoodLevel(), food.getSaturationLevel() + bonus);
        food.setSaturation(newSaturation);
    }

    // ── Blacksmithing — cheaper anvil XP cost ────────────────────────────────

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ProfessionRank rank = SkillEffects.rankFor(event.getPlayer(), Profession.BLACKSMITHING);
        if (rank == null) return;
        // Per-rank XP-cost multiplier: more skilled smiths pay less.
        float mult = switch (rank) {
            case NOVICE -> 0.90f;
            case APPRENTICE -> 0.80f;
            case JOURNEYMAN -> 0.60f;
            case EXPERT -> 0.40f;
            case MASTER -> 0.20f;
        };
        int reduced = Math.max(1, Math.round(event.getXpCost() * mult));
        event.setXpCost(reduced);
    }

    // ── Alchemy — longer-duration brewed potions ─────────────────────────────

    @SubscribeEvent
    public static void onPlayerBrewedPotion(PlayerBrewedPotionEvent event) {
        ProfessionRank rank = SkillEffects.rankFor(event.getEntity(), Profession.ALCHEMY);
        if (rank == null) return;
        ItemStack stack = event.getStack();
        PotionContents orig = stack.get(DataComponents.POTION_CONTENTS);
        if (orig == null) return;

        // Per-rank duration multiplier (applies to the base potion's effects and
        // any custom effects). Materializes base→custom so we can rewrite each
        // MobEffectInstance with a longer duration.
        float scale = switch (rank) {
            case NOVICE -> 1.20f;
            case APPRENTICE -> 1.40f;
            case JOURNEYMAN -> 1.70f;
            case EXPERT -> 2.00f;
            case MASTER -> 2.50f;
        };

        List<MobEffectInstance> rebuilt = new ArrayList<>();
        for (MobEffectInstance src : orig.getAllEffects()) {
            int newDuration = src.getDuration() < 0
                ? src.getDuration() // -1 / infinite — leave alone
                : Math.max(1, Math.round(src.getDuration() * scale));
            rebuilt.add(new MobEffectInstance(
                src.getEffect(), newDuration, src.getAmplifier(),
                src.isAmbient(), src.isVisible(), src.showIcon()));
        }
        if (rebuilt.isEmpty()) return;

        // Color is preserved so the bottle still looks right; base potion is
        // cleared so its (un-extended) effects don't double-apply.
        PotionContents next = new PotionContents(
            Optional.empty(),
            Optional.of(orig.getColor()),
            rebuilt,
            orig.customName());
        stack.set(DataComponents.POTION_CONTENTS, next);
    }

    // ── Enchanting — XP refund after every enchant ───────────────────────────

    @SubscribeEvent
    public static void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ProfessionRank rank = SkillEffects.rankFor(player, Profession.ENCHANTING);
        if (rank == null) return;
        int refundLevels = switch (rank) {
            case NOVICE -> 1;
            case APPRENTICE -> 2;
            case JOURNEYMAN -> 3;
            case EXPERT -> 5;
            case MASTER -> 7;
        };
        player.giveExperienceLevels(refundLevels);
        player.sendSystemMessage(Component.literal("+" + refundLevels + " levels refunded (Enchanting "
                + rank.displayName() + ")").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ── Trading — chance to double trade output ──────────────────────────────

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int chance = SkillEffects.extraDropChancePercent(player, Profession.TRADING);
        if (chance <= 0) return;
        if (player.getRandom().nextInt(100) >= chance) return;
        ItemStack bonus = event.getMerchantOffer().getResult().copy();
        if (bonus.isEmpty()) return;
        bonus.setCount(1);
        // Add to inventory or drop at player's feet if full.
        if (!player.getInventory().add(bonus)) {
            player.drop(bonus, false);
        }
    }

    private static void veinBreak(ServerLevel level, BlockPos start, Block targetBlock, ServerPlayer player) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int broken = 0;
        while (!queue.isEmpty() && broken < VEINBREAKER_MAX) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                if (broken >= VEINBREAKER_MAX) break;
                BlockPos adj = pos.relative(dir);
                if (!visited.add(adj)) continue;
                BlockState adjState = level.getBlockState(adj);
                if (adjState.getBlock() != targetBlock) continue;
                level.destroyBlock(adj, true, player);
                queue.add(adj);
                broken++;
            }
        }
    }
}
