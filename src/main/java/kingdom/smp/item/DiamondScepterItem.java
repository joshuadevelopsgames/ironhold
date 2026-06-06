package kingdom.smp.item;

import kingdom.smp.ModParticles;
import kingdom.smp.client.entity.DiamondScepterRenderer;
import kingdom.smp.entity.SpellBeamEntity;
import kingdom.smp.game.BeamReflection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/**
 * Diamond Scepter — a crystalline variant of the Arcane Scepter.
 *
 * <p>Right-click fires a piercing diamond beam ("Crystal Lance") that damages the
 * target, chills it (Slowness), marks it (Glowing), and shatters into a frost burst
 * that briefly slows nearby foes.
 */
public class DiamondScepterItem extends Item implements GeoItem {

    private static final int COOLDOWN_TICKS = 18;
    private static final int MIN_CHARGE_TICKS = 8;
    private static final int FULL_CHARGE_TICKS = 40;
    private static final double RANGE = 32.0;
    private static final int CRYSTAL_COLOR = 0x55FFFF;

    private static final RawAnimation FLOAT_SPIN =
        RawAnimation.begin().thenLoop("animation.diamond_scepter.float_spin");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DiamondScepterItem(Properties props) {
        super(props);
    }

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
        // Begin charging — the light-blue force-field renders client-side while
        // the player holds the item (driven by isUsingItem in the render layer).
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.3F, 2.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int charge = getUseDuration(stack, entity) - timeLeft;
        if (charge < MIN_CHARGE_TICKS) return false;

        float frac = Math.min(
            (charge - MIN_CHARGE_TICKS) / (float) (FULL_CHARGE_TICKS - MIN_CHARGE_TICKS), 1.0f);

        if (!level.isClientSide()) {
            fireCrystalLance(player, (ServerLevel) level, stack, frac);
            stack.hurtAndBreak(1, player, player.getUsedItemHand());
        }
        // Launch sound on BOTH sides: passing the player as the "except" entity means the
        // caster hears it locally with no latency (client) while others hear it broadcast
        // (server). Pitch rises with charge. This is the clear "fire" cue distinct from the
        // charge hum.
        float firePitch = 1.1F + 0.3F * frac;
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, firePitch);
        player.swing(player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return true;
    }

    private static void fireCrystalLance(Player player, ServerLevel level, ItemStack stack, float chargeFrac) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Trace the lance, bouncing off any mirrors so players can bank crystal shots around corners.
        BeamReflection.Result trace = BeamReflection.trace(level, player, eyePos, look, RANGE,
            e -> !e.isSpectator() && e.isPickable() && e != player && e instanceof LivingEntity);

        EntityHitResult entityHit = trace.entityHit();
        if (entityHit != null) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            float damage = 4.0f + 6.0f * chargeFrac;
            int slownessAmp = chargeFrac >= 0.99f ? 2 : 1;
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, slownessAmp, false, true, true));
            // Freeze: powder-snow frostbite (shiver, blue overlay, frost damage), duration scales with charge.
            int freezeTicks = target.getTicksRequiredToFreeze() + (int) (100 + 160 * chargeFrac);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), freezeTicks));
            // Icy crackle on a frozen target.
            level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 1.1F);
        }
        Vec3 impactPos = trace.impact();

        // One visual beam and crystal trail per straight leg. The first leg starts at the staff
        // tip; reflected legs start exactly where the previous leg met the mirror.
        java.util.List<BeamReflection.Segment> segments = trace.segments();
        for (int i = 0; i < segments.size(); i++) {
            BeamReflection.Segment seg = segments.get(i);
            Vec3 origin = i == 0 ? eyePos.add(look.scale(0.8)) : seg.start();
            SpellBeamEntity beam = SpellBeamEntity.create(level,
                seg.end().x, seg.end().y, seg.end().z,
                origin.x, origin.y, origin.z,
                CRYSTAL_COLOR, 12);
            level.addFreshEntity(beam);
            sendCrystalTrail(level, origin, seg.end());
        }

        // Frost glint + chime at each mirror bounce so the redirect reads clearly.
        for (Vec3 bounce : trace.bouncePoints()) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                bounce.x, bounce.y, bounce.z, 10, 0.15, 0.15, 0.15, 0.03);
            level.playSound(null, BlockPos.containing(bounce),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7F, 1.5F);
        }

        // Frost-shatter burst at the impact point
        level.sendParticles(ParticleTypes.SNOWFLAKE,
            impactPos.x, impactPos.y, impactPos.z, 18, 0.25, 0.25, 0.25, 0.04);
        level.sendParticles(ModParticles.DIAMOND_SCEPTER_SPARK.get(),
            impactPos.x, impactPos.y, impactPos.z, 24, 0.32, 0.32, 0.32, 0.08);

        // AoE chill: briefly slow other living entities near the impact (radius grows with charge)
        AABB blast = new AABB(impactPos, impactPos).inflate(2.0 + 1.5 * chargeFrac);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, blast,
                e -> e != player && e.isAlive())) {
            nearby.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true, true));
        }

        // Crystal shimmer at the impact point itself (spatial), for both block & entity hits.
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    private static void sendCrystalTrail(ServerLevel level, Vec3 origin, Vec3 impactPos) {
        Vec3 beam = impactPos.subtract(origin);
        double length = beam.length();
        if (length < 0.001) return;

        Vec3 direction = beam.normalize();
        int steps = Math.min(48, Math.max(8, (int) (length * 2.0)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = origin.add(beam.scale(t));
            double swirl = i * 0.75;
            double sideX = Math.cos(swirl) * 0.03;
            double sideY = Math.sin(swirl) * 0.03;
            level.sendParticles(ModParticles.DIAMOND_SCEPTER_SPARK.get(),
                p.x + sideX,
                p.y + sideY,
                p.z,
                1,
                direction.x * 0.02,
                direction.y * 0.02,
                direction.z * 0.02,
                0.01);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DiamondScepterItem>("float_spin", 0,
            state -> state.setAndContinue(FLOAT_SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DiamondScepterRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new DiamondScepterRenderer();
                return renderer;
            }
        });
    }
}
