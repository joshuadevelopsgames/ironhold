package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import kingdom.smp.net.SyncVanityPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side handler that:
 * <ul>
 *   <li>Ticks every equipped accessory each game tick.</li>
 *   <li>Broadcasts vanity data when a player logs in or is first tracked.</li>
 * </ul>
 */
public final class AccessoryTickHandler {
    private AccessoryTickHandler() {}

    /** ~Speed I (+20% move speed); transient, re-applied each tick only while Hermes is in an accessory slot. */
    private static final Identifier HERMES_MOVE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "accessory_hermes_movement");

    // ── Accessory buff ticks ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        boolean hermesEquipped = false;
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Ironhold.HERMES_BOOTS.get())) {
                hermesEquipped = true;
            }
            if (!stack.isEmpty() && stack.getItem() instanceof AccessoryItem acc) {
                acc.onAccessoryTick(player, stack);
            }
        }

        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            move.removeModifier(HERMES_MOVE);
            if (hermesEquipped) {
                move.addTransientModifier(
                        new AttributeModifier(HERMES_MOVE, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        // Broadcast vanity to all tracking clients when a vanity slot changes
        if (inv.isVanityDirty()) {
            inv.clearVanityDirty();
            broadcastVanity(player);
        }
    }

    // ── Vanity broadcasting ───────────────────────────────────────────────────

    /** Send this player's vanity data to all tracking clients (and self). */
    public static void broadcastVanity(ServerPlayer player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        SyncVanityPayload payload = new SyncVanityPayload(
                player.getUUID(),
                inv.getItem(AccessoryInventory.VANITY_HEAD),
                inv.getItem(AccessoryInventory.VANITY_CHEST),
                inv.getItem(AccessoryInventory.VANITY_LEGS),
                inv.getItem(AccessoryInventory.VANITY_FEET));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    /** When a player logs in, broadcast their vanity to everyone. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            broadcastVanity(sp);
        }
    }

    /** When someone starts tracking a player, send that player's vanity. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target
                && event.getEntity() instanceof ServerPlayer tracker) {
            AccessoryInventory inv = target.getData(ModAttachments.ACCESSORY_INV.get());
            SyncVanityPayload payload = new SyncVanityPayload(
                    target.getUUID(),
                    inv.getItem(AccessoryInventory.VANITY_HEAD),
                    inv.getItem(AccessoryInventory.VANITY_CHEST),
                    inv.getItem(AccessoryInventory.VANITY_LEGS),
                    inv.getItem(AccessoryInventory.VANITY_FEET));
            PacketDistributor.sendToPlayer(tracker, payload);
        }
    }

    /** When a player respawns (death/end), keep vanity in sync. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            broadcastVanity(sp);
        }
    }
}
