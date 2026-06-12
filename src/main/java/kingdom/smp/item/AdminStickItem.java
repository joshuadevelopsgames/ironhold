package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Admin Stick — gamemaster-only moderation tool that deletes whatever is in the crosshair.
 *
 * <p>Right-click raytraces up to {@value #RANGE} blocks: an entity hit is {@link Entity#discard()}ed
 * (works on persistent entities — baby mimics, NPCs, bosses), otherwise the targeted block is set to
 * air with no drops. Sneak-right-click targets fluid source blocks instead of passing through them.
 * Left-clicking an entity deletes it too (and never deals damage), which is the way to remove mobs
 * whose right-click opens a dialogue or trade screen.
 *
 * <p>Players are never deletable. Permission is resolved through {@code ServerPlayer.permissions()},
 * so both vanilla ops and Ironhold-granted gamemasters can use it; for anyone else it is inert.
 */
public class AdminStickItem extends Item {

    private static final double RANGE = 64.0;
    private static final int COOLDOWN_TICKS = 4;

    public AdminStickItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!isAdmin(serverPlayer)) {
            serverPlayer.sendOverlayMessage(
                Component.translatable("item.ironhold.admin_stick.no_permission").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        Entity target = findTargetedEntity(serverPlayer);
        if (target != null) {
            deleteEntity(serverLevel, serverPlayer, target);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
            return InteractionResult.SUCCESS_SERVER;
        }

        // No entity in the way — fall through to the block (or fluid source, when sneaking).
        Vec3 eye = serverPlayer.getEyePosition();
        Vec3 end = eye.add(serverPlayer.getLookAngle().scale(RANGE));
        ClipContext.Fluid fluidMode = serverPlayer.isShiftKeyDown()
            ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
        BlockHitResult hit = serverLevel.clip(
            new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluidMode, serverPlayer));
        if (hit.getType() == HitResult.Type.BLOCK && deleteBlock(serverLevel, serverPlayer, hit.getBlockPos())) {
            player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** Right-clicking directly on a mob within reach — same delete, no trade/dialogue fall-through. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isAdmin(serverPlayer)) {
            return InteractionResult.PASS;
        }
        deleteEntity(serverPlayer.level(), serverPlayer, target);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Left-click delete; returning true also means the stick never deals attack damage. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player instanceof ServerPlayer serverPlayer && isAdmin(serverPlayer)) {
            deleteEntity(serverPlayer.level(), serverPlayer, entity);
        }
        return true;
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static void deleteEntity(ServerLevel level, ServerPlayer admin, Entity target) {
        if (target instanceof EnderDragonPart part) {
            target = part.parentMob;
        }
        if (target instanceof Player) {
            admin.sendOverlayMessage(
                Component.translatable("item.ironhold.admin_stick.no_players").withStyle(ChatFormatting.RED));
            return;
        }
        Component name = target.getDisplayName();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            16, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.02);
        level.sendParticles(ParticleTypes.POOF,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            8, target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3, 0.01);
        level.playSound(null, target.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.5F);
        target.discard();
        admin.sendOverlayMessage(
            Component.translatable("item.ironhold.admin_stick.deleted_entity", name)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static boolean deleteBlock(ServerLevel level, ServerPlayer admin, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        Component name = state.getBlock().getName();
        level.levelEvent(2001, pos, Block.getId(state)); // break particles + sound, no drops
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        admin.sendOverlayMessage(
            Component.translatable("item.ironhold.admin_stick.deleted_block", name)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /**
     * Crosshair raytrace for ANY entity — no pickable/living filter, so item frames, dropped items,
     * armor stands, projectiles and persistent mobs are all valid targets. Blocked by walls.
     */
    private static Entity findTargetedEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Don't reach through walls: clamp the entity search to the first block hit.
        BlockHitResult blockHit = player.level().clip(
            new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double reach = blockHit.getType() == HitResult.Type.BLOCK
            ? Math.sqrt(blockHit.getLocation().distanceToSqr(eye)) : RANGE;
        Vec3 end = eye.add(look.scale(reach));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, end, searchBox,
            e -> !e.isSpectator() && e != player && !(e instanceof Player),
            reach * reach);
        return hit == null ? null : hit.getEntity();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.ironhold.admin_stick.tooltip.use").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.ironhold.admin_stick.tooltip.attack").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.ironhold.admin_stick.tooltip.sneak").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.ironhold.admin_stick.tooltip.gate").withStyle(ChatFormatting.DARK_GRAY));
    }
}
