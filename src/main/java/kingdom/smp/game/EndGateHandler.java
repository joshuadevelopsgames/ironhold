package kingdom.smp.game;

import java.util.List;

import kingdom.smp.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Boss-gated End (Phase 6 ⑱, core): you can't seat an Eye of Ender in an End Portal Frame until you've
 * defeated the kingdom's bosses. Reuses the {@code BOSS_ARTIFACTS_EARNED} tracking from the boss-accessory
 * system. Registered to the game bus in {@code Ironhold}.
 *
 * <p>Deferred (need content/assets): boss-dropped eye items, boss-arena structures, and the story
 * dimension. Spec: {@code specs/fantasia-ports/13-end-arenas-dimension.md}.
 */
public final class EndGateHandler {
    private EndGateHandler() {}

    /** Bosses that must be beaten to open the End (2 now; grows as bosses ship). */
    private static final List<String> REQUIRED_BOSSES = List.of("king_enderman", "stone_golem");

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.END_PORTAL_FRAME) || state.getValue(EndPortalFrameBlock.HAS_EYE)) {
            return; // not an empty frame
        }

        var earned = player.getData(ModAttachments.BOSS_ARTIFACTS_EARNED.get());
        for (String boss : REQUIRED_BOSSES) {
            if (!earned.has(boss)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "✦ The End resists you. Defeat the kingdom's bosses before the eyes will seat.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
                return;
            }
        }
    }
}
