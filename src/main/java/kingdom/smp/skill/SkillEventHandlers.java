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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
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
 *   <li>Blacksmithing — anvil repair / fatigue accumulation hooks</li>
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
    public static void onBlockBreakGate(BlockEvent.BreakEvent event) {
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
    public static void onBlockBreakVeinbreaker(BlockEvent.BreakEvent event) {
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

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        int chance = SkillEffects.extraDropChancePercent(player, Profession.FISHING);
        if (chance <= 0) return;
        if (player.level().getRandom().nextInt(100) >= chance) return;

        var drops = event.getDrops();
        if (drops.isEmpty()) return;
        ItemStack copy = drops.get(0).copy();
        copy.setCount(1);
        drops.add(copy);
    }

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
