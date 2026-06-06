package kingdom.smp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Drives the tripwire-hook-as-rack behaviour (storage lives in
 * {@link TripwireRackBlockEntity}, the visual in {@code TripwireRackRenderer}):
 *
 * <ul>
 *   <li>Right-click an empty hook with an item → hangs one of that item.</li>
 *   <li>Right-click an occupied hook (empty hand or not) → takes the item back.</li>
 *   <li>Sneak-right-click an occupied hook → cycles how it hangs (down → left →
 *       up → right). Sneaking on an empty hook falls through so you can still
 *       place blocks against it.</li>
 *   <li>Breaking the hook drops the hung item ({@link BlockDropsEvent}, which
 *       covers survival mining, explosions and pistons).</li>
 * </ul>
 *
 * Registered on the NeoForge game event bus from {@code Ironhold}.
 */
public final class TripwireRackHandler {
    private TripwireRackHandler() {}

    @SubscribeEvent
    public static void onRightClickHook(PlayerInteractEvent.RightClickBlock event) {
        // One hand only — otherwise an empty off-hand fires a second interaction.
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.TRIPWIRE_HOOK)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TripwireRackBlockEntity rack)) return;

        Player player = event.getEntity();
        boolean sneaking = player.isSecondaryUseActive();
        boolean occupied = !rack.getItem().isEmpty();

        // Sneak + occupied: rotate the hanging direction.
        if (sneaking) {
            if (!occupied) return; // empty hook: let vanilla handle (e.g. place a block)
            if (!level.isClientSide()) {
                rack.cycleOrientation();
                playSound(level, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM);
            }
            consume(event);
            return;
        }

        // Plain click on an occupied hook: take the item back.
        if (occupied) {
            if (!level.isClientSide()) {
                ItemStack taken = rack.removeItem();
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                playSound(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
            }
            consume(event);
            return;
        }

        // Plain click on an empty hook with an item in hand: hang one.
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty()) {
            if (!level.isClientSide()) {
                rack.setItem(held.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(level, pos, SoundEvents.ITEM_FRAME_ADD_ITEM);
            }
            consume(event);
        }
    }

    /** Drop the hung item when the hook is destroyed (mining, explosion, piston). */
    @SubscribeEvent
    public static void onHookBroken(BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof TripwireRackBlockEntity rack)) return;
        ItemStack item = rack.getItem();
        if (item.isEmpty()) return;
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(),
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.copy()));
    }

    private static void playSound(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.9f, 1.0f);
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
