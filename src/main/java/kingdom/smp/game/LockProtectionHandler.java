package kingdom.smp.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModItems;
import kingdom.smp.item.LockKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Applies player-owned locks to vanilla chests, shelves, armor stands, and doors and
 * rejects every player interaction that could change or remove a locked target. Chests, shelves,
 * and stands store their owner on a block-entity / entity attachment; doors have no block entity,
 * so their lock lives in {@link LockedDoorData} instead.
 */
public final class LockProtectionHandler {
    private LockProtectionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() instanceof DoorBlock) {
            handleDoorInteraction(event, state);
            return;
        }
        if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof ShelfBlock)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        Set<BlockEntity> targets = blockInteractionTargets(event.getLevel(), event.getPos(), state);
        if (held.is(ModItems.LOCK.get())) {
            if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                applyBlockLock(serverPlayer, event.getLevel(), event.getPos(), state, held);
            }
            consume(event);
            return;
        }

        if (!event.getLevel().isClientSide()) {
            if (held.is(ModItems.KEY.get()) && allLockedTargetsOwnedBy(targets, player.getUUID())) {
                bindKey(player, held, targets, event.getLevel(),
                    event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5);
                consume(event);
                return;
            }

            if (hasForeignOwner(targets, player.getUUID())) {
                if (canAccessAll(targets, player)) {
                    playUnlockSound(event.getLevel(),
                        event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5);
                } else {
                    player.sendSystemMessage(Component.literal("This is locked by another player."));
                    consume(event);
                }
            }
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
        UUID breakerId = event.getPlayer().getUUID();

        BlockState state = event.getState();
        if (state.getBlock() instanceof DoorBlock && event.getLevel() instanceof ServerLevel level) {
            long key = doorLowerPos(event.getPos(), state).asLong();
            String owner = LockedDoorData.get(level).owner(key);
            if (owner.isEmpty()) return;
            if (owner.equals(breakerId.toString())) {
                LockedDoorData.get(level).unlock(key); // owner is removing it — drop the lock so the spot isn't stale
            } else {
                event.setCanceled(true);
                event.setNotifyClient(true);
                event.getPlayer().sendSystemMessage(Component.literal("This door is locked by another player."));
            }
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (isOwnedByAnother(blockEntity, breakerId)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(Component.literal("This is locked by another player."));
        }
    }

    /**
     * Doors carry no block entity, so their lock lives in {@link LockedDoorData}. A fresh door must
     * never inherit a stale lock left by a previous door at this spot (e.g. one destroyed by means
     * that didn't clear its entry), so any leftover entry is dropped on placement.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDoorPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().getBlock() instanceof DoorBlock
                && event.getLevel() instanceof ServerLevel level) {
            LockedDoorData.get(level).unlock(doorLowerPos(event.getPos(), event.getPlacedBlock()).asLong());
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
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(pos -> isLockedDoor(level, pos));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPistonMove(PistonEvent.Pre event) {
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        boolean blocked = resolver.getToPush().stream().anyMatch(pos -> isLocked(event.getLevel().getBlockEntity(pos)))
                || resolver.getToDestroy().stream().anyMatch(pos -> isLocked(event.getLevel().getBlockEntity(pos)));
        if (!blocked && event.getLevel() instanceof ServerLevel level) {
            blocked = resolver.getToPush().stream().anyMatch(pos -> isLockedDoor(level, pos))
                    || resolver.getToDestroy().stream().anyMatch(pos -> isLockedDoor(level, pos));
        }
        if (blocked) event.setCanceled(true);
    }

    public static boolean isLocked(BlockEntity blockEntity) {
        return blockEntity != null && !owner(blockEntity).isEmpty();
    }

    public static boolean isLocked(Entity entity) {
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
        String keyId = UUID.randomUUID().toString();
        targets.forEach(target -> {
            target.setData(ModAttachments.LOCK_OWNER.get(), owner);
            target.setData(ModAttachments.LOCK_KEY_ID.get(), keyId);
        });
        spendLock(player, lockStack);
        playLockSound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        player.sendSystemMessage(Component.literal(
            state.getBlock() instanceof ChestBlock ? "Chest locked to you." : "Shelf locked to you."));
    }

    // ── Doors ──────────────────────────────────────────────────────────────
    // Doors have no block entity, so ownership lives in LockedDoorData (per dimension) instead of an
    // attachment. The flow mirrors chests/shelves: lock with a LOCK, the owner opens freely and can
    // bind a KEY to share access, key-holders get in too, everyone else is turned away.

    private static void handleDoorInteraction(PlayerInteractEvent.RightClickBlock event, BlockState state) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // Locking: swallow on both sides (the client knows it holds the lock) so it can't mispredict opening.
        if (held.is(ModItems.LOCK.get())) {
            if (event.getLevel() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                applyDoorLock(serverPlayer, level, doorLowerPos(event.getPos(), state), held);
            }
            consume(event);
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos lowerPos = doorLowerPos(event.getPos(), state);
        long key = lowerPos.asLong();
        LockedDoorData data = LockedDoorData.get(level);
        String owner = data.owner(key);
        if (owner.isEmpty()) return; // unlocked → vanilla open/close

        double x = lowerPos.getX() + 0.5, y = lowerPos.getY() + 1.0, z = lowerPos.getZ() + 0.5;
        boolean isOwner = owner.equals(player.getUUID().toString());

        // Owner binding a key so they can hand out access to others.
        if (isOwner && held.is(ModItems.KEY.get())) {
            LockKeyItem.bind(held, data.keyId(key));
            playUnlockSound(level, x, y, z);
            player.sendSystemMessage(Component.literal("Key bound. Give it to someone you trust."));
            consume(event);
            return;
        }

        if (isOwner) return; // owner → vanilla open/close

        // Non-owner: allowed only with a matching key. Consume is server-only, so the client briefly
        // mispredicts opening and the server's block update sets it straight — same as chests.
        if (hasMatchingKey(player, data.keyId(key))) {
            playUnlockSound(level, x, y, z);
        } else {
            player.sendSystemMessage(Component.literal("This door is locked by another player."));
            consume(event);
        }
    }

    private static void applyDoorLock(ServerPlayer player, ServerLevel level, BlockPos lowerPos, ItemStack lockStack) {
        LockedDoorData data = LockedDoorData.get(level);
        long key = lowerPos.asLong();
        String existing = data.owner(key);
        String playerId = player.getUUID().toString();
        if (!existing.isEmpty()) {
            player.sendSystemMessage(Component.literal(existing.equals(playerId)
                ? "This door is already locked to you."
                : "This door is already locked by another player."));
            return;
        }
        data.lock(key, playerId, UUID.randomUUID().toString());
        spendLock(player, lockStack);
        playLockSound(level, lowerPos.getX() + 0.5, lowerPos.getY() + 1.0, lowerPos.getZ() + 0.5);
        player.sendSystemMessage(Component.literal("Door locked to you."));
    }

    /** A door's lock is keyed on its lower half, so both halves resolve to the same entry. */
    private static BlockPos doorLowerPos(BlockPos pos, BlockState state) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    private static boolean isLockedDoor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock
            && LockedDoorData.get(level).isLocked(doorLowerPos(pos, state).asLong());
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
                    armorStand.setData(ModAttachments.LOCK_KEY_ID.get(), UUID.randomUUID().toString());
                    // A locked stand defies gravity so it can't be knocked into a gap or shot off its
                    // perch — it stays exactly where its owner placed it. Persists via the NoGravity flag.
                    armorStand.setNoGravity(true);
                    spendLock(player, held);
                    playLockSound(armorStand.level(), armorStand.getX(), armorStand.getY(), armorStand.getZ());
                    player.sendSystemMessage(Component.literal("Armor stand locked to you."));
                }
            }
            cancelEntityInteraction(event);
            return;
        }

        if (!player.level().isClientSide()) {
            if (held.is(ModItems.KEY.get()) && isOwnedBy(armorStand, player.getUUID())) {
                bindKey(player, held, Set.of(), armorStand.level(),
                    armorStand.getX(), armorStand.getY(), armorStand.getZ(), armorStand);
                cancelEntityInteraction(event);
                return;
            }

            if (isOwnedByAnother(armorStand, player.getUUID())) {
                if (hasMatchingKey(player, keyId(armorStand))) {
                    playUnlockSound(armorStand.level(), armorStand.getX(), armorStand.getY(), armorStand.getZ());
                } else {
                    player.sendSystemMessage(Component.literal("This armor stand is locked by another player."));
                    cancelEntityInteraction(event);
                }
            }
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

    private static boolean allLockedTargetsOwnedBy(Set<BlockEntity> targets, UUID playerId) {
        return !targets.isEmpty()
            && targets.stream().allMatch(target -> isLocked(target) && isOwnedBy(target, playerId));
    }

    private static boolean canAccessAll(Set<BlockEntity> targets, Player player) {
        return targets.stream().allMatch(target ->
            !isLocked(target)
                || isOwnedBy(target, player.getUUID())
                || hasMatchingKey(player, keyId(target)));
    }

    private static boolean hasMatchingKey(Player player, String keyId) {
        return !keyId.isEmpty() && player.getInventory().contains(stack -> LockKeyItem.matches(stack, keyId));
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

    private static String keyId(BlockEntity target) {
        return target.getData(ModAttachments.LOCK_KEY_ID.get());
    }

    private static String keyId(Entity target) {
        return target.getData(ModAttachments.LOCK_KEY_ID.get());
    }

    private static void bindKey(
            Player player,
            ItemStack keyStack,
            Set<BlockEntity> targets,
            Level level,
            double x,
            double y,
            double z) {
        bindKey(player, keyStack, targets, level, x, y, z, null);
    }

    private static void bindKey(
            Player player,
            ItemStack keyStack,
            Set<BlockEntity> targets,
            Level level,
            double x,
            double y,
            double z,
            Entity entityTarget) {
        String keyId = entityTarget != null
            ? keyId(entityTarget)
            : targets.stream().map(LockProtectionHandler::keyId).filter(id -> !id.isEmpty()).findFirst().orElse("");
        if (keyId.isEmpty()) keyId = UUID.randomUUID().toString();

        final String boundKeyId = keyId;
        if (entityTarget != null) {
            entityTarget.setData(ModAttachments.LOCK_KEY_ID.get(), boundKeyId);
        } else {
            targets.forEach(target -> target.setData(ModAttachments.LOCK_KEY_ID.get(), boundKeyId));
        }
        LockKeyItem.bind(keyStack, boundKeyId);
        playUnlockSound(level, x, y, z);
        player.sendSystemMessage(Component.literal("Key bound. Give it to someone you trust."));
    }

    private static void spendLock(Player player, ItemStack lockStack) {
        if (!player.hasInfiniteMaterials()) lockStack.shrink(1);
    }

    /** Mechanical click that confirms a target has just been locked. Called server-side; null source so the actor hears it too. */
    private static void playLockSound(Level level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static void playUnlockSound(Level level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.55F, 1.35F);
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
