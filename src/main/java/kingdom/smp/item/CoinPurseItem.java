package kingdom.smp.item;

import java.util.function.Consumer;

import kingdom.smp.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Coin Purse — banks loose {@link ModItems#GOLD_COIN} into a stored balance.
 * <ul>
 *   <li><b>Right-click in the world:</b> deposit all loose coins.</li>
 *   <li><b>Right-click + hold the purse in your inventory:</b> withdraw coins onto the cursor; the
 *       longer you hold, the faster they come (1 → 5 → 20 → 50 per pulse). Driven client-side by
 *       {@link kingdom.smp.client.CoinPurseHoldHandler} → {@link #withdraw}.</li>
 * </ul>
 * Spec: {@code specs/fantasia-ports/05-coin-purse.md}.
 */
public class CoinPurseItem extends Item {

    /** Max coins per stack (matches {@code GOLD_COIN.stacksTo}). */
    public static final int COIN_STACK = 100;

    public CoinPurseItem(Properties props) {
        super(props.stacksTo(1));
    }

    // ── World right-click: deposit all loose coins ─────────────────────────────
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        deposit(player, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    /**
     * Server-side: withdraw up to {@code amount} coins from the purse in the open menu's {@code slotIndex}
     * onto the player's cursor (the carried stack). Called from the held-right-click client loop.
     */
    public static void withdraw(ServerPlayer player, int slotIndex, int amount) {
        if (amount <= 0) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.getSlot(slotIndex);
        ItemStack purse = slot.getItem();
        if (!(purse.getItem() instanceof CoinPurseItem)) {
            return;
        }
        int balance = getBalance(purse);
        if (balance <= 0) {
            return;
        }
        ItemStack carried = menu.getCarried();
        int take;
        if (carried.isEmpty()) {
            take = Math.min(amount, Math.min(balance, COIN_STACK));
            if (take <= 0) return;
            setBalance(purse, balance - take);
            menu.setCarried(new ItemStack(ModItems.GOLD_COIN.get(), take));
        } else if (carried.is(ModItems.GOLD_COIN.get())) {
            int room = COIN_STACK - carried.getCount();
            take = Math.min(amount, Math.min(balance, room));
            if (take <= 0) return;
            setBalance(purse, balance - take);
            carried.grow(take);
        } else {
            return; // cursor holds something else
        }
        slot.setChanged();
        menu.broadcastChanges();
        clink(player);
    }

    private static void deposit(Player player, ItemStack purse) {
        Inventory inv = player.getInventory();
        int gained = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(ModItems.GOLD_COIN.get())) {
                gained += s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (gained <= 0) {
            actionBar(player, Component.literal("No loose coins to deposit.").withStyle(ChatFormatting.GRAY));
            return;
        }
        int balance = getBalance(purse) + gained;
        setBalance(purse, balance);
        clink(player);
        actionBar(player, Component.literal("◉ Deposited " + gained + " — purse holds " + balance)
            .withStyle(ChatFormatting.GOLD));
    }

    public static int getBalance(ItemStack purse) {
        return purse.getOrDefault(IronholdItemComponents.COIN_BALANCE.get(), 0);
    }

    private static void setBalance(ItemStack purse, int value) {
        purse.set(IronholdItemComponents.COIN_BALANCE.get(), Math.max(0, value));
    }

    private static void clink(Player player) {
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }

    private static void actionBar(Player player, Component msg) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(msg));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("◉ " + getBalance(stack) + " coins").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.literal("Right-click (world): deposit all").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Hold right-click in inventory: withdraw").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, ctx, display, tooltip, flag);
    }
}
