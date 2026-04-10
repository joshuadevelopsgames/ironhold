package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Stub carry-weight: tag-based stack weight + class max; applies transient move-speed penalty. */
public final class EncumbranceHandler {
    /** Multiplies final move speed: value v => factor (1 + v). */
    private static final Identifier ENCUMBRANCE_MUL =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "encumbrance_mul");
    /** Flat subtract from move speed after multipliers. */
    private static final Identifier ENCUMBRANCE_ADD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "encumbrance_add");

    /** Players whose weight needs recalculating. */
    private static final Set<UUID> dirty = new HashSet<>();

    /** Safety fallback: recompute every N ticks even if no event fired (catches edge cases). */
    private static final int FALLBACK_INTERVAL = 100;

    private EncumbranceHandler() {}

    // ── Event-driven dirty marking ───────────────────────────────────────────

    /** Mark a player's weight as needing recalculation. */
    public static void markDirty(ServerPlayer player) {
        dirty.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer sp) markDirty(sp);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) markDirty(sp);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) markDirty(sp);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) markDirty(sp);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) markDirty(sp);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) markDirty(sp);
    }

    public static int weightFor(ServerPlayer player) {
        return computeWeight(player);
    }

    /** Client-safe: works on any Player (local or server). */
    public static int weightForAnyPlayer(Player player) {
        return computeWeight(player);
    }

    public static void tick(ServerPlayer player, AttachmentType<PlayerKingdomRpgData> rpgKey) {
        boolean needsRecalc = dirty.remove(player.getUUID())
                || player.tickCount % FALLBACK_INTERVAL == 0;
        if (!needsRecalc) return;

        PlayerKingdomRpgData rpg = player.getData(rpgKey);
        int max = rpg.playerClass().maxCarryWeight();
        int weight = computeWeight(player);
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move == null) {
            return;
        }
        move.removeModifier(ENCUMBRANCE_MUL);
        move.removeModifier(ENCUMBRANCE_ADD);
        double ratio = max <= 0 ? 0.0 : (double) weight / (double) max;
        if (ratio > 1.0) {
            double over = ratio - 1.0;
            // Old curve was ~invisible just above cap; keep a floor slowdown so it’s obvious.
            double multPenalty = Mth.clamp(0.18 + over * 0.75, 0.18, 0.72);
            move.addTransientModifier(
                new AttributeModifier(ENCUMBRANCE_MUL, -multPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            double flatPenalty = Mth.clamp(0.018 + over * 0.05, 0.018, 0.09);
            if (flatPenalty > 0.0) {
                move.addTransientModifier(
                    new AttributeModifier(ENCUMBRANCE_ADD, -flatPenalty, AttributeModifier.Operation.ADD_VALUE));
            }
            int slowAmp = over >= 1.5 ? 2 : over >= 0.5 ? 1 : 0;
            player.addEffect(
                new MobEffectInstance(MobEffects.SLOWNESS, 45, slowAmp, false, false, true));
        }
        // Under cap: we stop applying; Slowness from this mod expires within a few seconds.
    }

    private static int computeWeight(Player player) {
        int w = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            w += stackWeight(stack);
        }
        w += stackWeight(player.getItemBySlot(EquipmentSlot.OFFHAND));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.BODY) {
                ItemStack stack = player.getItemBySlot(slot);
                w += stackWeight(stack);
            }
        }
        return w;
    }

    private static int stackWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int perItem;
        if (isArmor(stack)) {
            perItem = 25;
        } else if (isToolWeapon(stack)) {
            perItem = 15;
        } else if (isMetalOrOre(stack)) {
            perItem = 5;
        } else {
            // Normal items (food, wood, dirt, etc.) have no weight
            return 0;
        }
        return perItem * stack.getCount();
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR)
            || stack.is(ItemTags.CHEST_ARMOR)
            || stack.is(ItemTags.LEG_ARMOR)
            || stack.is(ItemTags.FOOT_ARMOR);
    }

    private static boolean isToolWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.AXES)
            || stack.is(ItemTags.PICKAXES)
            || stack.is(ItemTags.SHOVELS)
            || stack.is(ItemTags.HOES);
    }

    /** Metal ingots/nuggets/blocks and ore blocks. */
    private static boolean isMetalOrOre(ItemStack stack) {
        // Ore blocks
        if (stack.is(ItemTags.IRON_ORES) || stack.is(ItemTags.GOLD_ORES)
                || stack.is(ItemTags.COPPER_ORES) || stack.is(ItemTags.DIAMOND_ORES)
                || stack.is(ItemTags.EMERALD_ORES) || stack.is(ItemTags.LAPIS_ORES)
                || stack.is(ItemTags.REDSTONE_ORES)) {
            return true;
        }
        // Raw metals, ingots, nuggets, and metal blocks by registry name
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        return path.contains("iron_") || path.contains("gold_") || path.contains("copper_")
            || path.contains("netherite") || path.contains("_ingot") || path.contains("_nugget")
            || path.contains("raw_iron") || path.contains("raw_gold") || path.contains("raw_copper")
            || path.contains("chain") || path.contains("anvil")
            || path.contains("diamond") || path.contains("emerald")
            || path.contains("tanzanite")
            || path.contains("iron_block") || path.contains("gold_block")
            || path.contains("copper_block") || path.contains("netherite_block");
    }
}
