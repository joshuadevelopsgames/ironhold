package kingdom.smp.item;

import kingdom.smp.ModParticles;
import kingdom.smp.client.entity.BattleHammerRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
 * Battle Hammer — a netherite war hammer. Hold right-click to charge "forge power"
 * (the inner rings glow orange), then release for a ground slam: an AoE shockwave
 * that damages and launches nearby enemies airborne, scaling with charge. No fall
 * required (unlike the Mace) — the charge replaces the smash buildup.
 */
public class BattleHammerItem extends Item implements GeoItem {

    public static final int MIN_CHARGE_TICKS = 8;
    public static final int FULL_CHARGE_TICKS = 30;
    private static final int COOLDOWN_TICKS = 30;

    /** Forge-power crit-combo: each consecutive critical hit adds one level (1..MAX),
     *  the glow stage tracks the level, and the level scales the ground-slam power.
     *  The charge is held indefinitely (across weapon swaps, downtime, plain non-crit
     *  hits, etc.) and only resets when a swing fully misses — whiffing into the air. */
    public static final int MAX_FORGE_CHARGE = 8;

    private static final RawAnimation IDLE =
        RawAnimation.begin().thenLoop("animation.battle_hammer.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BattleHammerItem(Properties props) {
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
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(),
            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4F, 0.6F);
        // Wind-up + slam are driven by the renderer (charge fraction + post-release cooldown),
        // not GeckoLib triggers (one-shot item triggers proved unreliable in this version).
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        // Must hold through the wind-up animation to commit a smash.
        int charge = getUseDuration(stack, entity) - timeLeft;
        if (charge < MIN_CHARGE_TICKS) return false;

        // Smash power comes from the forge-power crit combo, NOT how long you held.
        // No combo built up -> nothing to unleash (release fizzles, no cooldown).
        ForgeCharge fc = stack.getOrDefault(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE);
        if (fc.level() <= 0) return false;

        float frac = Math.min(fc.level() / (float) MAX_FORGE_CHARGE, 1.0f);

        if (!level.isClientSide()) {
            groundSlam(player, (ServerLevel) level, frac);
            stack.hurtAndBreak(1, player, player.getUsedItemHand());
            // Spend the charge: the glow drops back to nothing after the slam.
            stack.set(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE);
            // Recoil: pop the slammer slightly off the ground so the blow has real weight.
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.32 + 0.22 * frac, 0.0));
            player.hurtMarked = true;
            player.fallDistance = 0.0;
        }
        // Earth-smash audio (both sides: caster hears it locally, others via broadcast).
        // The heavy ground crack rides over the crunch of the block underfoot caving in,
        // with a subtle forge clang. Lower pitch on bigger slams reads as a heavier blow.
        float x = (float) player.getX(), y = (float) player.getY(), z = (float) player.getZ();
        net.minecraft.world.level.block.state.BlockState floor =
            level.getBlockState(player.blockPosition().below());
        level.playSound(player, x, y, z,
            SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.1F, 0.95F - 0.30F * frac);
        if (!floor.isAir()) {
            level.playSound(player, x, y, z,
                floor.getSoundType().getBreakSound(), SoundSource.PLAYERS,
                1.2F + 0.4F * frac, 0.55F + 0.15F * frac);
        }
        level.playSound(player, x, y, z,
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 0.55F + 0.2F * frac);
        // Mjolnir-style thunder: a sharp electric crack at the point of impact + the Channeling
        // trident's thunderclap, with a rolling rumble tail on the bigger slams, so a full-charge
        // hit lands like a lightning strike.
        level.playSound(player, x, y, z,
            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F + 0.5F * frac, 1.1F - 0.2F * frac);
        level.playSound(player, x, y, z,
            SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F + 0.6F * frac, 0.9F + 0.2F * frac);
        if (frac > 0.5f) {
            level.playSound(player, x, y, z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F + 0.5F * frac, 0.7F);
        }
        // Ominous wither-spawn boom layered over the strike for an "unleashed" feel.
        level.playSound(player, x, y, z,
            SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F + 0.5F * frac, 0.9F + 0.2F * frac);
        player.swing(player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return true;
    }

    /**
     * Bumps the forge-power crit combo by one (capped at {@link #MAX_FORGE_CHARGE}). Called
     * from {@link BattleHammerCombatHandler} when the holder lands a critical hit on a living
     * target. The combo never times out and survives plain non-crit hits — it only resets via
     * {@link #resetCharge} when a swing fully misses. Returns the new level so the caller can
     * play scaled feedback.
     */
    public static int addCritCharge(ItemStack stack, ServerLevel level) {
        ForgeCharge cur = stack.getOrDefault(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE);
        int next = Math.min(cur.level() + 1, MAX_FORGE_CHARGE);
        stack.set(IronholdItemComponents.FORGE_CHARGE.get(), new ForgeCharge(next, level.getGameTime()));
        return next;
    }

    /** Breaks the combo (back to level 0). Called when a swing fully misses (whiffs into the
     *  air). No-ops on stacks that aren't a charged hammer, so it's safe to call on any held
     *  item. */
    public static void resetCharge(ItemStack stack) {
        if (stack.getOrDefault(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE).level() > 0) {
            stack.set(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE);
        }
    }

    /** True if this stack is a Battle Hammer currently carrying forge charge (level &gt; 0). */
    public static boolean hasForgeCharge(ItemStack stack) {
        return stack.getItem() instanceof BattleHammerItem
            && stack.getOrDefault(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE).level() > 0;
    }

    private static void groundSlam(Player player, ServerLevel level, float frac) {
        double cx = player.getX(), cy = player.getY(), cz = player.getZ();
        // Radius scales hard with charge: a small ~3-block pop at level 1 up to a sweeping
        // 7-block blast at full charge, so the biggest combo clearly has the biggest reach.
        double radius = 2.5 + 4.5 * frac;
        float damage = 6.0f + 12.0f * frac;
        double kbH = 0.6 + 0.7 * frac;
        double kbUp = 0.8 + 0.8 * frac;

        AABB area = new AABB(cx - radius, cy - 2, cz - radius, cx + radius, cy + 2, cz + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            if (target.distanceToSqr(cx, target.getY(), cz) > radius * radius) continue;

            target.hurtServer(level, level.damageSources().playerAttack(player), damage);

            // Launch airborne + outward.
            Vec3 push = new Vec3(target.getX() - cx, 0, target.getZ() - cz);
            push = push.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : push.normalize();
            target.setDeltaMovement(target.getDeltaMovement()
                .add(push.x * kbH, kbUp, push.z * kbH));
            target.hurtMarked = true; // force velocity sync to clients

            // Forge sparks on each struck enemy.
            level.sendParticles(ModParticles.IRON_SPARK.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                10, 0.25, 0.3, 0.25, 0.06);
        }

        // Insta-erode TRMT terrain in a TIGHT zone right under the hammer (much smaller than
        // the full knockback blast): grass/dirt stamp to worn path, sand sinks, plants shatter.
        double erodeRadius = 1.3 + 0.9 * frac; // ~1.3 at level 1 up to ~2.2 at full charge
        int ir = (int) Math.ceil(erodeRadius);
        int bx0 = (int) Math.floor(cx), by0 = (int) Math.floor(cy), bz0 = (int) Math.floor(cz);
        double er2 = erodeRadius * erodeRadius;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz > er2) continue;
                for (int dy = 1; dy >= -2; dy--) {
                    milkucha.trmt.erosion.SlamErosion.forceErode(level,
                        new net.minecraft.core.BlockPos(bx0 + dx, by0 + dy, bz0 + dz));
                }
            }
        }

        // --- Impact VFX: smashing the earth --------------------------------
        // Ground-shatter is the centerpiece: chunks of the block underfoot erupt upward, and
        // a ring of debris is flung outward along the ground like a crack splitting the earth.
        net.minecraft.world.level.block.state.BlockState ground =
            level.getBlockState(player.blockPosition().below());
        if (!ground.isAir()) {
            var debris = new net.minecraft.core.particles.BlockParticleOption(
                net.minecraft.core.particles.ParticleTypes.BLOCK, ground);
            // Upward eruption at the point of impact.
            level.sendParticles(debris, cx, cy + 0.2, cz,
                45 + (int) (85 * frac), radius * 0.3, 0.15, radius * 0.3, 0.45);
            // Outward-cracking ring of debris (count 0 => the offset vector is the velocity).
            int cracks = 30 + (int) (44 * frac);
            double crackSpeed = 0.5 + 0.6 * frac;
            for (int i = 0; i < cracks; i++) {
                double a = (i / (double) cracks) * Math.PI * 2;
                double dirX = Math.cos(a), dirZ = Math.sin(a);
                level.sendParticles(debris,
                    cx + dirX * 0.8, cy + 0.15, cz + dirZ * 0.8,
                    0, dirX, 0.28, dirZ, crackSpeed);
            }
        }

        // Impact sparks: a scattered fountain bursting UP and OUT from the point of impact
        // (not a ring) — like sparks thrown off when the hammer head bites into the ground.
        // Origin is the hammer-head landing spot — a step in front of the player along their
        // horizontal facing — so the sparks fly off the impact rather than up through the
        // player's own body. count 0 => the (vx,vy,vz) offset is the particle's velocity.
        Vec3 facing = player.getLookAngle();
        double facingLen = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
        double fx = facingLen < 1.0e-4 ? 0.0 : facing.x / facingLen;
        double fz = facingLen < 1.0e-4 ? 0.0 : facing.z / facingLen;
        double impactX = cx + fx * 1.1;
        double impactZ = cz + fz * 1.1;
        var rng = level.getRandom();
        int sparks = 30 + (int) (70 * frac);
        for (int i = 0; i < sparks; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double horiz = 0.10 + rng.nextDouble() * (0.45 + 0.65 * frac);
            double up = 0.25 + rng.nextDouble() * (0.55 + 0.55 * frac);
            level.sendParticles(ModParticles.IRON_SPARK.get(),
                impactX, cy + 0.15, impactZ, 0,
                Math.cos(a) * horiz, up, Math.sin(a) * horiz, 1.0);
        }

        // Tight ring of wind-charge gusts hugging the hit zone (not way out at the blast edge),
        // so the gale reads as kicked up right where the hammer struck.
        double gustRadius = 1.2 + 0.5 * frac;
        int gusts = 8 + (int) (8 * frac);
        for (int i = 0; i < gusts; i++) {
            double a = (i / (double) gusts) * Math.PI * 2;
            level.sendParticles(ParticleTypes.GUST,
                cx + Math.cos(a) * gustRadius, cy + 0.3, cz + Math.sin(a) * gustRadius,
                1, 0.05, 0.05, 0.05, 0.0);
        }
        // Full-charge slams kick up a big expanding gust ring at the center.
        if (frac > 0.6f) {
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, cx, cy + 0.2, cz, 1, 0, 0, 0, 0);
        }

        // A rising dust plume from the crater + a hot forge flare. No explosion poof.
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            cx, cy + 0.25, cz, 22 + (int) (40 * frac), 0.6, 0.25, 0.6, 0.07);
        level.sendParticles(ModParticles.IRON_SPARK_FLARE.get(),
            impactX, cy + 0.3, impactZ, 40 + (int) (50 * frac), 0.5, 0.35, 0.5, 0.22);

        // Real terrain chunks erupt from the ground — the centerpiece of the earth-smash.
        launchTerrainChunks(player, level, frac, radius);
    }

    /**
     * Erupts short-lived, visual-only terrain chunks from the hammer's hit zone (the block
     * the slammer is standing on) and flings them up and outward so they radiate from the
     * point of impact rather than popping up at scattered spots. Outward speed scales with
     * charge so a bigger slam throws them across its wider blast.
     */
    private static void launchTerrainChunks(Player player, ServerLevel level, float frac, double radius) {
        var rng = level.getRandom();
        double cx = player.getX(), cz = player.getZ();

        // Surface block at the impact point — the source the chunks are "torn" from.
        net.minecraft.core.BlockPos centerGround = null;
        for (int dy = 0; dy >= -2; dy--) {
            net.minecraft.core.BlockPos check = net.minecraft.core.BlockPos.containing(cx, player.getY() + dy, cz);
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(check);
            if (!st.isAir()
                    && st.getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL
                    && level.getBlockState(check.above()).isAir()) {
                centerGround = check;
                break;
            }
        }
        if (centerGround == null) return;
        net.minecraft.world.level.block.state.BlockState centerState = level.getBlockState(centerGround);
        double surfaceY = centerGround.getY() + 1.0;

        int count = 9 + (int) (19 * frac); // ~9 at level 1, up to ~28 at full charge
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;

            // Source texture: a block right in the hit zone (falls back to the center block).
            double srcDist = rng.nextDouble() * 1.3;
            net.minecraft.core.BlockPos src =
                net.minecraft.core.BlockPos.containing(cx + Math.cos(a) * srcDist, surfaceY - 0.5, cz + Math.sin(a) * srcDist);
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(src);
            if (st.isAir() || st.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
                st = centerState;
            }

            // Spawn at the impact zone and fling outward + up; outward carries them across the blast.
            double spawnDist = rng.nextDouble() * 0.7;
            double sx = cx + Math.cos(a) * spawnDist;
            double sz = cz + Math.sin(a) * spawnDist;
            double outward = 0.18 + rng.nextDouble() * (0.20 + 0.32 * frac);
            double up = 0.38 + rng.nextDouble() * (0.30 + 0.45 * frac);
            Vec3 vel = new Vec3(Math.cos(a) * outward, up, Math.sin(a) * outward);
            // ~1s of flight + ~0.5s resting on the ground before vanishing.
            kingdom.smp.entity.SlamDebrisEntity.spawn(level, sx, surfaceY, sz, st, vel, 24 + rng.nextInt(8));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BattleHammerItem>("idle", 0,
            state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BattleHammerRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new BattleHammerRenderer();
                return renderer;
            }
        });
    }
}
