package kingdom.smp.disguise;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.SyncDisguisePayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side authority for player disguises (Master of Disguise tome). Sets/clears the
 * {@link ModAttachments#DISGUISE} attachment and pushes the change to all tracking clients
 * (and the disguised player) via {@link SyncDisguisePayload}.
 */
public final class DisguiseManager {
    private DisguiseManager() {}

    public static void setDisguise(ServerPlayer player, EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        player.setData(ModAttachments.DISGUISE.get(), DisguiseState.of(id));
        broadcast(player);
    }

    /** Drops the disguise if one is active. No-op (and no packet) when already undisguised. */
    public static void clear(ServerPlayer player) {
        if (!player.getData(ModAttachments.DISGUISE.get()).active()) {
            return;
        }
        player.setData(ModAttachments.DISGUISE.get(), DisguiseState.NONE);
        broadcast(player);
    }

    public static boolean isDisguised(ServerPlayer player) {
        return player.getData(ModAttachments.DISGUISE.get()).active();
    }

    /** Push this player's current disguise state to every tracking client and to the player. */
    public static void broadcast(ServerPlayer player) {
        DisguiseState state = player.getData(ModAttachments.DISGUISE.get());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new SyncDisguisePayload(player.getUUID(), state.entityTypeId()));
    }

    /** Send {@code target}'s disguise (if any) to a single viewer that just started tracking it. */
    public static void sendTo(ServerPlayer target, ServerPlayer viewer) {
        DisguiseState state = target.getData(ModAttachments.DISGUISE.get());
        if (state.active()) {
            PacketDistributor.sendToPlayer(viewer,
                    new SyncDisguisePayload(target.getUUID(), state.entityTypeId()));
        }
    }
}
