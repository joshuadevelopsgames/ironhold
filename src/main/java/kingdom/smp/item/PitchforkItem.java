package kingdom.smp.item;

import kingdom.smp.client.entity.PitchforkRenderer;
import kingdom.smp.entity.ThrownPitchforkEntity;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.Ironhold;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Pitchfork — a throwable spear weapon, like a trident.
 *
 * <p>Hold right-click to wind up (SPEAR pose), release to throw.
 * The thrown pitchfork sticks in entities/ground and can be walked over to retrieve.
 * Minimum charge: 10 ticks. Full power reached at 20 ticks.
 */
public class PitchforkItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int MIN_CHARGE_TICKS = 10;

    public PitchforkItem(Properties props) {
        super(props);
    }

    // ── Use / charge / throw ──────────────────────────────────────────────────

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** Start charging — hold the pitchfork out like a spear. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Release to throw. Removes the pitchfork from inventory (returned on pickup). */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int chargeTicks = getUseDuration(stack, entity) - timeLeft;
        if (chargeTicks < MIN_CHARGE_TICKS) return false;

        if (!level.isClientSide()) {
            // Power scales 0→1 over the first 20 ticks of charge
            float power = Math.min(1.0F, chargeTicks / 20.0F);

            ThrownPitchforkEntity thrown = new ThrownPitchforkEntity(player, level, stack);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, power * 2.5F, 1.0F);
            level.addFreshEntity(thrown);

            level.playSound(null, player.blockPosition(),
                    SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

            // Remove from inventory so it exists only as the entity (returned on pickup)
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(stack);
            }
        }
        return true;
    }

    // ── Hay bale instant break ─────────────────────────────────────────────────

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Blocks.HAY_BLOCK)) {
            return 100.0F; // Instant break
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(Blocks.HAY_BLOCK) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.is(Blocks.HAY_BLOCK) && !level.isClientSide()) {
            // Remove the hay bale and drop 9 wheat (crafting recipe equivalent)
            level.destroyBlock(pos, false);
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(Items.WHEAT, 9));
            level.addFreshEntity(drop);
            // Minimal durability cost
            if (miner instanceof Player player && !player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, (ServerLevel) level, player, item -> {});
            }
            return true;
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Throw")
            .withStyle(ChatFormatting.RED));
        tooltip.accept(Component.literal("A farmer's best friend... and worst enemy")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static model — no animations needed for the held item
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PitchforkRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new PitchforkRenderer();
                return renderer;
            }
        });
    }
}
