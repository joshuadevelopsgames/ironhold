package kingdom.smp.item;

import kingdom.smp.Ironhold;
import kingdom.smp.client.entity.SolunaStaffModel;
import kingdom.smp.client.entity.SolunaStaffRenderer;
import kingdom.smp.entity.LunarOrbEntity;
import kingdom.smp.entity.SolarOrbEntity;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Soluna Staff — a wizard-class weapon that shifts between sun and moon forms
 * based on the time of day.
 *
 * <p>During the day the staff fires golden solar orbs that ignite targets.
 * <p>At night it fires silver lunar orbs that slow and levitate targets.
 * <p>The orb spin animation speeds up while charging and returns to normal on release.
 */
public class SolunaStaffItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int MAX_CHARGE_TICKS = 100; // ~5 seconds for full power
    private static final int COOLDOWN_TICKS = 25;
    private static final String CONTROLLER_NAME = "spin_controller";

    public SolunaStaffItem(Properties props) {
        super(props);
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Wizard").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.literal("Shifts between sun and moon with the sky")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // ── Use mechanics (same flow as Arcane Scepter) ──────────────────────────

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            boolean night = isNight(level);
            level.playSound(null, player.blockPosition(),
                night ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.4F, night ? 1.2F : 2.0F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int charged = getUseDuration(stack, entity) - remainingUseDuration;
        if (charged < 3) return;

        // Speed up the spin animation while charging
        if (level.isClientSide()) {
            float speedMult = Math.min(1.0F + charged * 0.04F, 5.0F);
            setSpinSpeed(stack, speedMult);
        }

        // Charging particles at the staff tip
        if (level.isClientSide()) {
            boolean night = isNight(level);
            Vec3 look = entity.getLookAngle();
            float yawRad = (float) Math.toRadians(entity.getYRot());
            double rx = -Math.cos(yawRad) * 0.35;
            double rz = -Math.sin(yawRad) * 0.35;
            Vec3 tip = entity.position()
                .add(rx, entity.getBbHeight() * 0.75, rz)
                .add(look.scale(0.5));

            if (charged % 3 == 0) {
                level.addParticle(
                    night ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                    tip.x + (Math.random() - 0.5) * 0.2,
                    tip.y + Math.random() * 0.2,
                    tip.z + (Math.random() - 0.5) * 0.2,
                    0, 0.02, 0);
            }
            if (charged > 10 && charged % 4 == 0) {
                level.addParticle(
                    night ? ParticleTypes.END_ROD : ParticleTypes.LAVA,
                    tip.x + (Math.random() - 0.5) * 0.3,
                    tip.y + Math.random() * 0.3,
                    tip.z + (Math.random() - 0.5) * 0.3,
                    0, night ? -0.02 : 0.02, 0);
            }
        }

        // Charging hum
        if (!level.isClientSide() && charged % 10 == 0 && charged <= 30) {
            boolean night = isNight(level);
            level.playSound(null, entity.blockPosition(),
                night ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS, 0.15F, 1.8F + charged * 0.02F);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int chargeTime = this.getUseDuration(stack, entity) - timeLeft;

        // Reset animation speed on release
        if (level.isClientSide()) {
            setSpinSpeed(stack, 1.0F);
        }

        if (!level.isClientSide()) {
            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = player.getEyePosition().add(look.scale(1.5));
            boolean night = isNight(level);

            // Charge power: 0.0 (instant) → 1.0 at MAX_CHARGE (~5 seconds, capped)
            float power = Math.min(1.0F, (float) chargeTime / MAX_CHARGE_TICKS);
            // Launch speed: 0.15 (instant tap) → 3.0 (full charge, arrow speed)
            double launchSpeed = 0.15 + power * 2.85;

            if (night) {
                LunarOrbEntity orb = new LunarOrbEntity(player, look.scale(launchSpeed), level);
                orb.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                level.addFreshEntity(orb);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F + power * 0.5F);
            } else {
                SolarOrbEntity orb = new SolarOrbEntity(player, look.scale(launchSpeed), level);
                orb.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                level.addFreshEntity(orb);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.0F + power * 0.6F);
            }

            stack.hurtAndBreak(1, player, player.getUsedItemHand());
        }

        player.swing(player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isNight(Level level) {
        long t = level.getOverworldClockTime() % 24000;
        return t >= 12000;
    }

    /** Adjusts the GeckoLib spin animation speed on the client. */
    private void setSpinSpeed(ItemStack stack, float speed) {
        long id = GeoItem.getId(stack);
        var manager = this.cache.getManagerForId(id);
        var controller = manager.getAnimationControllers().get(CONTROLLER_NAME);
        if (controller != null) {
            controller.setAnimationSpeed(speed);
        }
    }

    // ── GeckoLib ─────────────────────────────────────────────────────────────

    private static final RawAnimation SPIN =
        RawAnimation.begin().thenLoop("animation.soluna_staff.spin");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SolunaStaffItem>(CONTROLLER_NAME, 0,
            state -> state.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SolunaStaffRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new SolunaStaffRenderer();
                return renderer;
            }
        });
    }
}
