package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Right-click a player to bind; the needle then points to their last-known position
 * while they're in the same dimension as you, and spins randomly otherwise.
 *
 * Server-side {@link #inventoryTick} refreshes the bound player's GlobalPos once a
 * second (only while they're online) and persists it on the stack. The client-side
 * {@code kingdom.smp.client.item.PlayerCompassAngle} reads that component to drive
 * the model frame.
 */
public class PlayerCompassItem extends Item {

    private static final int REFRESH_INTERVAL_TICKS = 20;

    public PlayerCompassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player tracked)) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        PlayerTrackerTarget data = new PlayerTrackerTarget(
                tracked.getUUID(),
                Optional.of(tracked.getName().getString()),
                Optional.of(GlobalPos.of(tracked.level().dimension(), tracked.blockPosition())));
        stack.set(IronholdItemComponents.PLAYER_TRACKER.get(), data);

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(
                    Component.literal("Compass now tracking " + tracked.getName().getString())
                            .withStyle(ChatFormatting.GOLD),
                    true);
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.6f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (level.getGameTime() % REFRESH_INTERVAL_TICKS != 0) return;
        PlayerTrackerTarget data = stack.get(IronholdItemComponents.PLAYER_TRACKER.get());
        if (data == null) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerPlayer tracked = server.getPlayerList().getPlayer(data.uuid());
        if (tracked == null) return;

        GlobalPos here = GlobalPos.of(tracked.level().dimension(), tracked.blockPosition());
        if (data.lastKnownPos().filter(p -> p.equals(here)).isPresent()) return;

        stack.set(IronholdItemComponents.PLAYER_TRACKER.get(), data.withPos(here));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        PlayerTrackerTarget data = stack.get(IronholdItemComponents.PLAYER_TRACKER.get());
        if (data == null) {
            tooltip.accept(Component.literal("Right-click a player to bind").withStyle(ChatFormatting.GRAY));
            return;
        }
        String displayName = data.name().orElseGet(() -> shortUuid(data.uuid()));
        tooltip.accept(Component.literal("Tracking: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(displayName).withStyle(ChatFormatting.AQUA)));
        data.lastKnownPos().ifPresent(p -> {
            BlockPos pos = p.pos();
            String dimId = p.dimension().identifier().toString();
            tooltip.accept(Component.literal("  Last seen: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.literal("  Dimension: " + dimId).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    private static String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.substring(0, Math.min(8, s.length()));
    }
}
