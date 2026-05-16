package kingdom.smp.item;

import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Wizard Stick — entry-level wand that shoots a curving fire stream.
 *
 * <p>Power scales with the holder's {@link PlayerClass}:
 * <ul>
 *   <li>T0 — non-mage: pitiful sputter, mostly cosmetic</li>
 *   <li>T1 — Mage Apprentice: short curving stream, light damage</li>
 *   <li>T2 — Wizard: longer stream, real damage, small impact splash</li>
 *   <li>T3 — Elementalist: long flat stream, splash + lingering fire</li>
 *   <li>T4 — Sorcerer Supreme / Arcane Knight / Arcane Ranger / Divine Mage:
 *       triple-fan stream, big damage, big splash. The actually-strong one.</li>
 * </ul>
 */
public class WizardStickItem extends Item {

    public WizardStickItem(Properties props) {
        super(props);
    }

    /** Per-tier tuning. */
    private record Tuning(
        double initialSpeed,
        double gravity,
        double drag,
        int    maxSteps,
        float  damage,
        int    fireTicks,
        int    cooldownTicks,
        int    streamCount,
        double splashRadius,
        float  pitch
    ) {}

    private static Tuning tuningFor(PlayerClass pc) {
        // Tier-4 mage path (and the hybrid classes that include Sorcerer Supreme).
        if (pc == PlayerClass.SORCERER_SUPREME
            || pc == PlayerClass.ARCANE_KNIGHT
            || pc == PlayerClass.ARCANE_RANGER
            || pc == PlayerClass.DIVINE_MAGE) {
            return new Tuning(1.15, 0.035, 0.985, 42, 7.0f, 100,  8, 3, 4.0, 0.7f);
        }
        if (pc == PlayerClass.ELEMENTALIST) {
            return new Tuning(1.00, 0.040, 0.975, 34, 5.0f,  80, 12, 1, 3.0, 0.9f);
        }
        if (pc == PlayerClass.WIZARD) {
            return new Tuning(0.85, 0.045, 0.965, 28, 3.5f,  60, 16, 1, 2.0, 1.1f);
        }
        if (pc == PlayerClass.MAGE_APPRENTICE) {
            return new Tuning(0.70, 0.050, 0.950, 22, 2.0f,  40, 22, 1, 0.0, 1.4f);
        }
        // Anything else (Peasant, Knight, Archer, ...): the stick mostly sputters.
        return     new Tuning(0.55, 0.060, 0.930, 18, 1.0f,  20, 30, 1, 0.0, 1.8f);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.ironhold.wizard_stick").withStyle(ChatFormatting.GOLD);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        PlayerKingdomRpgData rpg = serverPlayer.getData(ModAttachments.PLAYER_RPG.get());
        Tuning t = tuningFor(rpg.playerClass());

        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();

        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, t.pitch);

        // Streams: 1 dead-center; 3 means a small fan ±5° on the yaw axis.
        if (t.streamCount == 1) {
            fireStream(serverLevel, serverPlayer, eye, look, t);
        } else {
            // Build perpendicular axis so the spread is horizontal regardless of pitch.
            Vec3 horizontal = new Vec3(-look.z, 0, look.x).normalize();
            double offsetDeg = 5.0;
            fireStream(serverLevel, serverPlayer, eye, look, t);
            fireStream(serverLevel, serverPlayer, eye, rotateAround(look, horizontal,  offsetDeg), t);
            fireStream(serverLevel, serverPlayer, eye, rotateAround(look, horizontal, -offsetDeg), t);
        }

        user.getCooldowns().addCooldown(stack, t.cooldownTicks);
        return InteractionResult.SUCCESS;
    }

    /** Rodrigues rotation of {@code v} around unit-axis {@code axis} by {@code degrees}. */
    private static Vec3 rotateAround(Vec3 v, Vec3 axis, double degrees) {
        double r = Math.toRadians(degrees);
        double c = Math.cos(r);
        double s = Math.sin(r);
        Vec3 cross = axis.cross(v);
        double dot = axis.dot(v);
        return v.scale(c).add(cross.scale(s)).add(axis.scale(dot * (1 - c)));
    }

    private static void fireStream(ServerLevel level, ServerPlayer caster,
                                   Vec3 eye, Vec3 dir, Tuning t) {
        Vec3 pos = eye;
        Vec3 vel = dir.scale(t.initialSpeed);

        LivingEntity hitEntity = null;
        Vec3 impactPos = null;

        for (int i = 0; i < t.maxSteps; i++) {
            Vec3 nextPos = pos.add(vel);

            BlockHitResult blockHit = level.clip(new ClipContext(
                pos, nextPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                caster
            ));
            Vec3 segmentEnd = blockHit.getType() == HitResult.Type.MISS ? nextPos : blockHit.getLocation();

            LivingEntity entityHit = findEntityOnSegment(level, pos, segmentEnd, caster);
            if (entityHit != null) {
                hitEntity = entityHit;
                impactPos = entityHit.position().add(0, entityHit.getBbHeight() / 2, 0);
                break;
            }

            spawnTrail(level, pos, segmentEnd);

            if (blockHit.getType() != HitResult.Type.MISS) {
                impactPos = blockHit.getLocation();
                break;
            }

            pos = nextPos;
            vel = vel.add(0, -t.gravity, 0).scale(t.drag);
        }

        // Direct hit
        if (hitEntity != null) {
            hitEntity.setRemainingFireTicks(t.fireTicks);
            hitEntity.hurt(level.damageSources().playerAttack(caster), t.damage);
        }

        // Splash — only meaningful at Wizard tier and above
        if (impactPos != null && t.splashRadius > 0) {
            applySplash(level, caster, impactPos, t, hitEntity);
            spawnImpactParticles(level, impactPos, t.splashRadius);
        } else if (impactPos != null) {
            // Tiny puff for low tiers
            level.sendParticles(ParticleTypes.SMALL_FLAME,
                impactPos.x, impactPos.y, impactPos.z, 4, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private static void applySplash(ServerLevel level, ServerPlayer caster, Vec3 center,
                                     Tuning t, LivingEntity directHit) {
        double r = t.splashRadius;
        AABB box = new AABB(center.subtract(r, r, r), center.add(r, r, r));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
            le -> le != caster && le != directHit && le.isAlive())) {
            // Damage falloff with distance
            double dist = Math.sqrt(e.distanceToSqr(center.x, center.y, center.z));
            float falloff = (float) Math.max(0.25, 1.0 - dist / r);
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), (int) (t.fireTicks * 0.6)));
            e.hurt(level.damageSources().playerAttack(caster), t.damage * 0.6f * falloff);
        }
    }

    private static void spawnTrail(ServerLevel world, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        int steps = Math.max(2, (int) (dist * 5));
        Vec3 step = dist == 0 ? Vec3.ZERO : delta.scale(1.0 / steps);
        for (int i = 0; i < steps; i++) {
            Vec3 p = from.add(step.scale(i));
            world.sendParticles(ParticleTypes.SMALL_FLAME,
                p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static void spawnImpactParticles(ServerLevel world, Vec3 center, double radius) {
        int count = (int) (12 + radius * 6);
        for (int i = 0; i < count; i++) {
            double a1 = world.getRandom().nextDouble() * Math.PI * 2;
            double a2 = world.getRandom().nextDouble() * Math.PI;
            double rr = world.getRandom().nextDouble() * radius;
            double x = center.x + rr * Math.sin(a2) * Math.cos(a1);
            double y = center.y + rr * Math.sin(a2) * Math.sin(a1);
            double z = center.z + rr * Math.cos(a2);
            world.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private static LivingEntity findEntityOnSegment(ServerLevel world, Vec3 start, Vec3 end, Entity caster) {
        AABB box = new AABB(start, end).inflate(0.45);
        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box,
            le -> le != caster && le.isAlive())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.45).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < closestDistSqr) {
                    closestDistSqr = d;
                    closest = e;
                }
            }
        }
        return closest;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                 Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("A frail wand. Power grows with your wizard rank.")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
