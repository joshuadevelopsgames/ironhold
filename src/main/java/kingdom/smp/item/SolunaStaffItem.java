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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
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

    private static final int MAX_CHARGE_TICKS = 50; // ~2.5 seconds for full power
    private static final int OVERCHARGE_TICKS = 160; // 8 seconds for portal
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

        // Passives
        if (!level.isClientSide()) {
            boolean night = isNight(level);
            if (night) {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, true));
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 0, false, false, true));
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

    private void generateMoonPortal(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        look = new Vec3(look.x, 0, look.z).normalize();
        net.minecraft.core.BlockPos center = player.blockPosition().offset((int)(look.x * 5), 0, (int)(look.z * 5));
        
        while (level.isEmptyBlock(center) && center.getY() > level.getMinY()) {
            center = center.below();
        }
        center = center.above(); // Bottom center of the portal
        
        boolean isXAxis = Math.abs(look.x) > Math.abs(look.z);
        net.minecraft.core.Direction.Axis axis = isXAxis ? net.minecraft.core.Direction.Axis.Z : net.minecraft.core.Direction.Axis.X;

        net.minecraft.world.level.block.state.BlockState frame = net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState portal = kingdom.smp.ModBlocks.MOON_PORTAL.get().defaultBlockState()
                .setValue(kingdom.smp.block.MoonPortalBlock.AXIS, axis);

        for (int w = -1; w <= 2; w++) {
            for (int h = -1; h <= 3; h++) {
                net.minecraft.core.BlockPos pos = isXAxis ? center.offset(0, h, w) : center.offset(w, h, 0);
                if (w == -1 || w == 2 || h == -1 || h == 3) {
                    level.setBlockAndUpdate(pos, frame);
                } else {
                    level.setBlockAndUpdate(pos, portal);
                }
            }
        }
        
        level.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        
        // Massive particle burst
        level.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, 
            100, 1.0, 1.5, 1.0, 0.2);
    }

    private void grantShootForTheMoonAchievement(ServerLevel level, Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.advancements.AdvancementHolder advancement = level.getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath(Ironhold.MODID, "shoot_for_the_moon"));
            if (advancement != null) {
                net.minecraft.advancements.AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
                if (!progress.isDone()) {
                    for (String criterion : progress.getRemainingCriteria()) {
                        serverPlayer.getAdvancements().award(advancement, criterion);
                    }
                }
            }
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

            // Charge power: 0.0 (instant) → 1.0 at MAX_CHARGE (~2.5 seconds, capped)
            float power = Math.min(1.0F, (float) chargeTime / MAX_CHARGE_TICKS);

            if (night && power >= 1.0F) {
                long t = level.getOverworldClockTime() % 24000;
                double theta = ((t - 12000) / 12000.0) * Math.PI;
                Vec3 moonDir = new Vec3(Math.cos(theta), Math.sin(theta), 0).normalize();
                
                if (moonDir.dot(look.normalize()) > 0.85) {
                    generateMoonPortal((ServerLevel) level, player);
                    stack.hurtAndBreak(5, player, player.getUsedItemHand());
                    grantShootForTheMoonAchievement((ServerLevel) level, player);
                    player.swing(player.getUsedItemHand());
                    player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS * 2);
                    return true;
                }
            }

            // Launch speed: 1.0 (instant tap) → 4.5 (full charge, bolt speed)
            double launchSpeed = 1.0 + power * 3.5;

            if (night) {
                LunarOrbEntity orb = new LunarOrbEntity(player, look.scale(launchSpeed), level, power);
                orb.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                level.addFreshEntity(orb);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F + power * 0.5F);
            } else {
                SolarOrbEntity orb = new SolarOrbEntity(player, look.scale(launchSpeed), level, power);
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

    // ── Inventory tick (sets custom model data for dimension-aware icon) ────

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        float mode = isNight(level) ? 1.0F : 0.0F;
        CustomModelData current = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        Float existing = current.getFloat(0);
        if (existing == null || existing != mode) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(mode), List.of(), List.of(), List.of()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Nether → always sun, End → always moon, Overworld → time-based. */
    static boolean isNight(Level level) {
        if (level.dimension() == Level.NETHER) return false;
        if (level.dimension() == Level.END) return true;
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
