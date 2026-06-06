package kingdom.smp.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Applies player-owned locks to vanilla chests, shelves, and armor stands and
 * rejects every player interaction that could change or remove a locked target.
 */
public final class LockProtectionHandler {
    private LockProtectionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof ShelfBlock)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.is(ModItems.LOCK.get())) {
            if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                applyBlockLock(serverPlayer, event.getLevel(), event.getPos(), state, held);
            }
            consume(event);
            return;
        }

        if (!event.getLevel().isClientSide()
                && hasForeignOwner(blockInteractionTargets(event.getLevel(), event.getPos(), state), player.getUUID())) {
            player.sendSystemMessage(Component.literal("This is locked by another player."));
            consume(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorStandInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleArmorStandInteraction(event, event.getTarget(), event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorStandInteract(PlayerInteractEvent.EntityInteract event) {
        handleArmorStandInteraction(event, event.getTarget(), event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (isOwnedByAnother(blockEntity, event.getPlayer().getUUID())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(Component.literal("This is locked by another player."));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackArmorStand(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ArmorStand armorStand)) return;
        if (isOwnedByAnother(armorStand, event.getEntity().getUUID())) {
            event.setCanceled(true);
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().sendSystemMessage(Component.literal("This armor stand is locked by another player."));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorStandDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand) || !isLocked(armorStand)) return;
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player) || !isOwnedBy(armorStand, player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> isLocked(event.getLevel().getBlockEntity(pos)));
        event.getAffectedEntities().removeIf(entity -> entity instanceof ArmorStand && isLocked(entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPistonMove(PistonEvent.Pre event) {
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        if (resolver.getToPush().stream().anyMatch(pos -> isLocked(event.getLevel().getBlockEntity(pos)))
                || resolver.getToDestroy().stream().anyMatch(pos -> isLocked(event.getLevel().getBlockEntity(pos)))) {
            event.setCanceled(true);
        }
    }

    public static boolean isLocked(BlockEntity blockEntity) {
        return blockEntity != null && !owner(blockEntity).isEmpty();
    }

    private static boolean isLocked(Entity entity) {
        return !owner(entity).isEmpty();
    }

    private static void applyBlockLock(
            ServerPlayer player, Level level, BlockPos pos, BlockState state, ItemStack lockStack) {
        Set<BlockEntity> targets = blockInteractionTargets(level, pos, state);
        if (targets.isEmpty()) return;

        UUID playerId = player.getUUID();
        if (hasForeignOwner(targets, playerId)) {
            player.sendSystemMessage(Component.literal("This is already locked by another player."));
            return;
        }
        if (targets.stream().allMatch(target -> isOwnedBy(target, playerId))) {
            player.sendSystemMessage(Component.literal("This is already locked to you."));
            return;
        }

        String owner = playerId.toString();
        targets.forEach(target -> target.setData(ModAttachments.LOCK_OWNER.get(), owner));
        spendLock(player, lockStack);
        player.sendSystemMessage(Component.literal(
            state.getBlock() instanceof ChestBlock ? "Chest locked to you." : "Shelf locked to you."));
    }

    private static void handleArmorStandInteraction(
            PlayerInteractEvent event, Entity target, InteractionHand hand) {
        if (!(target instanceof ArmorStand armorStand)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.LOCK.get())) {
            if (!player.level().isClientSide()) {
                if (isOwnedByAnother(armorStand, player.getUUID())) {
                    player.sendSystemMessage(Component.literal("This armor stand is already locked by another player."));
                } else if (isOwnedBy(armorStand, player.getUUID())) {
                    player.sendSystemMessage(Component.literal("This armor stand is already locked to you."));
                } else {
                    armorStand.setData(ModAttachments.LOCK_OWNER.get(), player.getUUID().toString());
                    spendLock(player, held);
                    player.sendSystemMessage(Component.literal("Armor stand locked to you."));
                }
            }
            cancelEntityInteraction(event);
            return;
        }

        if (!player.level().isClientSide() && isOwnedByAnother(armorStand, player.getUUID())) {
            player.sendSystemMessage(Component.literal("This armor stand is locked by another player."));
            cancelEntityInteraction(event);
        }
    }

    private static Set<BlockEntity> blockInteractionTargets(Level level, BlockPos pos, BlockState state) {
        Set<BlockEntity> targets = new LinkedHashSet<>();
        addBlockEntity(level, pos, targets);

        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            addBlockEntity(level, ChestBlock.getConnectedBlockPos(pos, state), targets);
        } else if (state.getBlock() instanceof ShelfBlock shelf && state.getValue(ShelfBlock.POWERED)) {
            for (BlockPos connectedPos : shelf.getAllBlocksConnectedTo(level, pos)) {
                addBlockEntity(level, connectedPos, targets);
            }
        }
        return targets;
    }

    private static void addBlockEntity(Level level, BlockPos pos, Set<BlockEntity> targets) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) targets.add(blockEntity);
    }

    private static boolean hasForeignOwner(Set<BlockEntity> targets, UUID playerId) {
        return targets.stream().anyMatch(target -> isOwnedByAnother(target, playerId));
    }

    private static boolean isOwnedByAnother(BlockEntity target, UUID playerId) {
        return target != null && !owner(target).isEmpty() && !isOwnedBy(target, playerId);
    }

    private static boolean isOwnedByAnother(Entity target, UUID playerId) {
        return !owner(target).isEmpty() && !isOwnedBy(target, playerId);
    }

    private static boolean isOwnedBy(BlockEntity target, UUID playerId) {
        return owner(target).equals(playerId.toString());
    }

    private static boolean isOwnedBy(Entity target, UUID playerId) {
        return owner(target).equals(playerId.toString());
    }

    private static String owner(BlockEntity target) {
        return target.getData(ModAttachments.LOCK_OWNER.get());
    }

    private static String owner(Entity target) {
        return target.getData(ModAttachments.LOCK_OWNER.get());
    }

    private static void spendLock(Player player, ItemStack lockStack) {
        if (!player.hasInfiniteMaterials()) lockStack.shrink(1);
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void cancelEntityInteraction(PlayerInteractEvent event) {
        if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
            specific.setCanceled(true);
            specific.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
            entityInteract.setCanceled(true);
            entityInteract.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
