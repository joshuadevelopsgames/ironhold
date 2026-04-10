package kingdom.smp;

import kingdom.smp.command.IronholdCommands;
import kingdom.smp.game.ClassStatHandler;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.RpgXpBarSync;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class IronholdGameEvents {
    private IronholdGameEvents() {}

    /** Sync interval: send RPG data packet every N ticks (not every tick). */
    private static final int SYNC_INTERVAL = 10;


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        IronholdCommands.register(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTravelDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
        KingdomWorldData data = overworld.getDataStorage().computeIfAbsent(KingdomWorldData.TYPE);
        ResourceKey<Level> dest = event.getDimension();
        if (dimensionIdEquals(dest, Level.NETHER) && !data.isNetherUnlocked()) {
            event.setCanceled(true);
            player.sendSystemMessage(
                Component.literal(
                    "The Nether is sealed until a kingdom reaches "
                        + KingdomWorldData.NETHER_UNLOCK_XP
                        + " pooled Class XP."));
        } else if (dimensionIdEquals(dest, Level.END) && !data.isEndUnlocked()) {
            event.setCanceled(true);
            player.sendSystemMessage(
                Component.literal(
                    "The End is sealed until a kingdom reaches "
                        + KingdomWorldData.END_UNLOCK_XP
                        + " pooled Class XP."));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        ClassStatHandler.apply(player, rpg);
        RpgXpBarSync.sync(player, rpg);
        // Immediately sync full RPG state to the client for HUD rendering
        ModNetworking.syncToClient(player);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        ClassStatHandler.apply(player, rpg);
        EncumbranceHandler.tick(player, ModAttachments.PLAYER_RPG.get());
        RpgXpBarSync.sync(player, rpg);

        // Periodic RPG data sync to client (every SYNC_INTERVAL ticks)
        if (player.tickCount % SYNC_INTERVAL == 0) {
            ModNetworking.syncToClient(player);
        }

    }

    /** ~12% chance to slip a piece of fool's gold into the catch. */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity().level().getRandom().nextFloat() < 0.12f) {
            event.getDrops().add(new ItemStack(Ironhold.FOOLS_GOLD.get()));
        }
    }

    /** Match destination dimension even if {@link ResourceKey} identity differs between loaders. */
    private static boolean dimensionIdEquals(ResourceKey<Level> a, ResourceKey<Level> b) {
        return a.identifier().equals(b.identifier());
    }
}
