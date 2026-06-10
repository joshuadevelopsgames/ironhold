package kingdom.smp.alcohol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModItems;
import kingdom.smp.item.IronholdItemComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Server-authoritative intoxication, blackout body control, and recovery.
 *
 * <p>The destructive blackout events are deliberately capped: at most five
 * coins and one stackable rare item can be lost per blackout.
 */
public final class AlcoholService {
    private AlcoholService() {}

    public static final int MAX_LOAD = 100;
    public static final int BLACKOUT_DURATION_TICKS = 20 * 12;
    public static final long BLACKOUT_COOLDOWN_TICKS = 24_000L;
    public static final boolean ALLOW_COIN_SPENDING = true;
    public static final boolean ALLOW_RARE_ITEM_CONSUMPTION = true;
    public static final float COIN_SPEND_CHANCE = 0.40F;
    public static final float RARE_ITEM_CONSUMPTION_CHANCE = 0.08F;

    public static void drink(ServerPlayer player, AlcoholicDrinkItem.Drink drink) {
        long now = player.level().getGameTime();
        AlcoholState state = player.getData(ModAttachments.ALCOHOL.get());
        if (state.blackedOut()) return;

        int nextLoad = Math.min(MAX_LOAD, state.load() + drink.dose());
        state = state.withLoad(nextLoad, now);
        player.setData(ModAttachments.ALCOHOL.get(), state);
        player.connection.send(new ClientboundSetActionBarTextPacket(
            Component.literal(intoxicationLabel(nextLoad)).withStyle(intoxicationColor(nextLoad))));

        if (nextLoad >= MAX_LOAD && now >= state.blackoutCooldownUntil()) {
            beginBlackout(player, state, now);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    public static boolean isBlackedOut(ServerPlayer player) {
        return player.getData(ModAttachments.ALCOHOL.get()).blackedOut();
    }

    private static void tick(ServerPlayer player) {
        AlcoholState state = player.getData(ModAttachments.ALCOHOL.get());
        if (state.blackedOut()) {
            tickBlackout(player, state);
            return;
        }
        if (state.load() <= 0) return;

        if (player.tickCount % 40 == 0) {
            applyThresholdEffects(player, state.load());
        }
        if (player.tickCount % 200 == 0) {
            player.setData(ModAttachments.ALCOHOL.get(),
                state.withLoad(Math.max(0, state.load() - 1), state.lastDrinkTick()));
        }
    }

    private static void applyThresholdEffects(ServerPlayer player, int load) {
        if (load >= 90) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
        } else if (load >= 65) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0, false, true));
        } else if (load >= 40) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 0, false, true));
        }
    }

    private static void beginBlackout(ServerPlayer player, AlcoholState state, long now) {
        player.stopUsingItem();
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
            BLACKOUT_DURATION_TICKS + 60, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
            BLACKOUT_DURATION_TICKS + 60, 4, false, false));
        player.sendSystemMessage(Component.literal("Everything goes dark...")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 0.8F, 0.65F);
        player.setData(ModAttachments.ALCOHOL.get(),
            state.startBlackout(BLACKOUT_DURATION_TICKS, now + BLACKOUT_COOLDOWN_TICKS));
    }

    private static void tickBlackout(ServerPlayer player, AlcoholState state) {
        player.stopUsingItem();
        player.setSprinting(false);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 4, false, false));

        int elapsed = BLACKOUT_DURATION_TICKS - state.blackoutTicksRemaining();
        int stage = Math.min(5, elapsed / 40);
        String recap = state.recap();
        if (stage > state.blackoutStage()) {
            recap = performStage(player, stage, recap);
        }

        int remaining = state.blackoutTicksRemaining() - 1;
        if (remaining <= 0) {
            finishBlackout(player, state.advanceBlackout(0, stage, recap));
        } else {
            player.setData(ModAttachments.ALCOHOL.get(),
                state.advanceBlackout(remaining, stage, recap));
        }
    }

    private static String performStage(ServerPlayer player, int stage, String recap) {
        if (stage == 1 && ALLOW_COIN_SPENDING
                && player.getRandom().nextFloat() < COIN_SPEND_CHANCE) {
            int spent = spendCoins(player, 1 + player.getRandom().nextInt(5));
            if (spent > 0) {
                return appendRecap(recap, "bought a round for " + spent + " coin"
                    + (spent == 1 ? "" : "s"));
            }
        }
        if (stage == 3 && ALLOW_RARE_ITEM_CONSUMPTION
                && player.getRandom().nextFloat() < RARE_ITEM_CONSUMPTION_CHANCE) {
            String consumed = consumeRareItem(player);
            if (consumed != null) {
                return appendRecap(recap, "somehow consumed " + consumed);
            }
        }

        return switch (player.getRandom().nextInt(6)) {
            case 0 -> {
                player.setYRot(player.getYRot() + 90 + player.getRandom().nextInt(181));
                player.swing(InteractionHand.MAIN_HAND, true);
                yield appendRecap(recap, "challenged an empty corner to a duel");
            }
            case 1 -> {
                safeStagger(player);
                yield appendRecap(recap, "staggered off in a very confident direction");
            }
            case 2 -> {
                player.swing(InteractionHand.OFF_HAND, true);
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.5F, 0.8F);
                yield appendRecap(recap, "raised a toast to nobody in particular");
            }
            case 3 -> {
                if (player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.35, 0.0));
                    player.hurtMarked = true;
                }
                yield appendRecap(recap, "attempted a celebratory dance");
            }
            case 4 -> {
                player.sendSystemMessage(Component.literal("*incoherent tavern singing*")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                yield appendRecap(recap, "sang half of a song and invented the rest");
            }
            default -> {
                player.setYRot(player.getYRot() - 120);
                safeStagger(player);
                yield appendRecap(recap, "argued with a chair and lost");
            }
        };
    }

    private static void safeStagger(ServerPlayer player) {
        ServerLevel level = player.level();
        float angle = player.getYRot() + player.getRandom().nextFloat() * 120.0F - 60.0F;
        double radians = Math.toRadians(angle);
        double dx = -Math.sin(radians) * 0.75;
        double dz = Math.cos(radians) * 0.75;
        BlockPos target = BlockPos.containing(player.getX() + dx, player.getY(), player.getZ() + dz);
        BlockState feet = level.getBlockState(target);
        BlockState head = level.getBlockState(target.above());
        BlockPos belowPos = target.below();
        BlockState below = level.getBlockState(belowPos);
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return;
        if (feet.is(BlockTags.FIRE) || head.is(BlockTags.FIRE)) return;
        if (!below.isFaceSturdy(level, belowPos, Direction.UP)) return;
        if (!level.noCollision(player, player.getBoundingBox().move(dx, 0, dz))) return;

        player.teleportTo(level, player.getX() + dx, player.getY(), player.getZ() + dz,
            Set.of(), angle, player.getXRot(), false);
    }

    private static int spendCoins(ServerPlayer player, int wanted) {
        int remaining = wanted;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ModItems.GOLD_COIN.get())) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ModItems.COIN_PURSE.get())) continue;
            int balance = stack.getOrDefault(IronholdItemComponents.COIN_BALANCE.get(), 0);
            int take = Math.min(remaining, balance);
            if (take > 0) {
                stack.set(IronholdItemComponents.COIN_BALANCE.get(), balance - take);
                remaining -= take;
            }
        }
        int spent = wanted - remaining;
        if (spent > 0) {
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        return spent;
    }

    private static @Nullable String consumeRareItem(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<Integer> candidates = new ArrayList<>();
        int mainSlots = Math.min(36, inventory.getContainerSize());
        for (int i = 0; i < mainSlots; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) continue;
            if (stack.is(ModItems.GOLD_COIN.get()) || stack.getItem() instanceof AlcoholicDrinkItem) continue;
            Rarity rarity = stack.getRarity();
            if (rarity == Rarity.RARE || rarity == Rarity.EPIC) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return null;
        ItemStack selected = inventory.getItem(candidates.get(player.getRandom().nextInt(candidates.size())));
        String name = selected.getHoverName().getString();
        selected.shrink(1);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8F, 0.75F);
        return name;
    }

    private static void finishBlackout(ServerPlayer player, AlcoholState state) {
        RecoveryPoint recovery = findRecoveryPoint(player);
        player.teleportTo(recovery.level(), recovery.pos().getX() + 0.5,
            recovery.pos().getY(), recovery.pos().getZ() + 0.5,
            Set.of(), player.getYRot(), 0.0F, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.RESISTANCE);
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 20, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45 * 20, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 45 * 20, 0));
        player.setData(ModAttachments.ALCOHOL.get(), state.finishBlackout(15));

        String recap = state.recap().isBlank()
            ? "You remember absolutely nothing useful."
            : "You remember you " + state.recap() + ".";
        player.sendSystemMessage(Component.literal("You wake up with a splitting headache.")
            .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(recap)
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static RecoveryPoint findRecoveryPoint(ServerPlayer player) {
        ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
        if (respawn != null) {
            LevelData.RespawnData data = respawn.respawnData();
            ServerLevel level = player.level().getServer().getLevel(data.dimension());
            if (level != null) {
                BlockPos beside = safeBesideBed(level, data.pos());
                if (beside != null) return new RecoveryPoint(level, beside);
            }
        }

        BlockPos nearbyBed = findNearbyBed(player.level(), player.blockPosition(), 16);
        if (nearbyBed != null) {
            BlockPos beside = safeBesideBed(player.level(), nearbyBed);
            if (beside != null) return new RecoveryPoint(player.level(), beside);
        }

        LevelData.RespawnData worldSpawn = player.level().getServer().getRespawnData();
        ServerLevel level = player.level().getServer().getLevel(worldSpawn.dimension());
        if (level == null) level = player.level().getServer().overworld();
        BlockPos pos = worldSpawn.pos();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new RecoveryPoint(level, new BlockPos(pos.getX(), top, pos.getZ()));
    }

    private static @Nullable BlockPos findNearbyBed(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)) continue;
            if (state.getValue(BedBlock.PART) != BedPart.HEAD || state.getValue(BedBlock.OCCUPIED)) continue;
            double distance = pos.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private static @Nullable BlockPos safeBesideBed(ServerLevel level, BlockPos bedPos) {
        BlockState bed = level.getBlockState(bedPos);
        if (!(bed.getBlock() instanceof BedBlock)) return null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = bedPos.relative(direction);
            BlockPos below = candidate.below();
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(candidate).getFluidState().isEmpty()) {
                return candidate;
            }
        }
        return bedPos.above();
    }

    private static String appendRecap(String recap, String entry) {
        if (recap.isBlank()) return entry;
        if (recap.length() > 240) return recap;
        return recap + ", then " + entry;
    }

    private static String intoxicationLabel(int load) {
        if (load >= 90) return "Wasted";
        if (load >= 65) return "Drunk";
        if (load >= 40) return "Tipsy";
        if (load >= 20) return "Merry";
        return "Sober";
    }

    private static ChatFormatting intoxicationColor(int load) {
        if (load >= 90) return ChatFormatting.DARK_RED;
        if (load >= 65) return ChatFormatting.RED;
        if (load >= 40) return ChatFormatting.YELLOW;
        if (load >= 20) return ChatFormatting.GOLD;
        return ChatFormatting.GRAY;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isBlackedOut(player)) {
            event.setCanceled(true);
        }
    }

    private record RecoveryPoint(ServerLevel level, BlockPos pos) {}
}
