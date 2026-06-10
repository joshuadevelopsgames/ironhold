package kingdom.smp.enchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kingdom.smp.Ironhold;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Soulbound enchantment behaviour (the enchantment itself is data-driven JSON; this is its effect).
 *
 * <p>On player death (when keep-inventory is off): every item enchanted with {@code ironhold:soulbound}
 * is pulled out of the dying inventory <em>before</em> vanilla drops run, given a small durability hit
 * that can never break it, stashed, and handed back on respawn. So the player chooses per-item what
 * survives death, and protection isn't free.
 *
 * <p>Restored consistently regardless of how the player comes back (normal respawn, Ender Shrine, or a
 * vanilla totem pop — all route through {@link PlayerEvent.PlayerRespawnEvent}).
 *
 * <p><b>v1 caveat:</b> the stash is an in-memory map; if the server restarts while the player sits on
 * the death screen, the stashed items are lost. Acceptable for the near-instant common case; a
 * persisted-NBT stash is the hardening follow-up. Registered to the game bus in {@code Ironhold}.
 *
 * <p>Spec: {@code specs/fantasia-ports/04-soulbound-enchant.md}.
 */
public final class SoulboundDeathHandler {
    private SoulboundDeathHandler() {}

    public static final ResourceKey<Enchantment> SOULBOUND =
        ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "soulbound"));

    /** Durability hit per soulbound save, as a fraction of the item's max (scales across gear). Clamped
     *  so the item never fully breaks. v1: 30% — ~3 deaths heavily wears an item without ever breaking it. */
    private static final float WEAR_FRACTION = 0.30F;

    /** player UUID → items pulled from the dying inventory, restored on respawn. */
    private static final Map<UUID, List<ItemStack>> STASH = new HashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // If the death was already cancelled (e.g. an Ender Shrine revive, which runs first), the player
        // keeps their whole inventory — don't stash anything, or it would be lost (no respawn fires).
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // keep-inventory keeps everything anyway — nothing to protect.
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }

        Holder<Enchantment> soulbound;
        try {
            soulbound = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(SOULBOUND);
        } catch (RuntimeException notLoaded) {
            return; // enchantment datapack not present
        }

        List<ItemStack> kept = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (EnchantmentHelper.getItemEnchantmentLevel(soulbound, s) > 0) {
                applyWear(s);
                kept.add(s.copy());
                inv.setItem(i, ItemStack.EMPTY); // remove so vanilla won't drop it on death
            }
        }
        if (!kept.isEmpty()) {
            STASH.put(player.getUUID(), kept);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> kept = STASH.remove(player.getUUID());
        if (kept == null) {
            return;
        }
        for (ItemStack s : kept) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
    }

    /** Small fixed durability cost, clamped to leave the item at ≥1 durability (never breaks). */
    private static void applyWear(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }
        int max = stack.getMaxDamage();
        int hit = Math.max(1, Math.round(max * WEAR_FRACTION));
        int next = Math.min(stack.getDamageValue() + hit, max - 1); // clamp: always leaves ≥1 durability
        stack.setDamageValue(Math.max(0, next));
    }
}
