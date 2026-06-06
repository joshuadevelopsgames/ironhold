package kingdom.smp.effect;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slimed — the goop a Slime Pet leaves on whatever it bites.
 *
 * <p>For its short duration it makes movement sticky: a movement-speed attribute modifier
 * handles ordinary walking, and the tick handler damps momentum plus clips upward jump
 * velocity into a weak hop instead of stacking hidden vanilla effects.
 */
public class SlimedEffect extends MobEffect {

    /** Hot-pink ooze. */
    private static final int PINK = 0xFF6EC7;
    private static final DustParticleOptions OOZE = new DustParticleOptions(0xFF000000 | PINK, 1.2F);
    private static final Identifier SLIMED_MOVEMENT =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "slimed_movement");
    private static final double WALK_STEP_DISTANCE_SQR = 0.58 * 0.58;
    private static final Map<UUID, StepSoundState> STEP_SOUNDS = new ConcurrentHashMap<>();

    public SlimedEffect() {
        super(MobEffectCategory.HARMFUL, PINK);
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            SLIMED_MOVEMENT,
            -0.55,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        double horizontalDamp = entity.onGround() ? 0.74 : 0.88;
        double y = motion.y;
        if (y > 0.12) {
            y = 0.12;
        }
        entity.setDeltaMovement(motion.x * horizontalDamp, y, motion.z * horizontalDamp);

        if (level.getGameTime() % 4 == 0) {
            double dx = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double dy = level.getRandom().nextDouble() * entity.getBbHeight();
            double dz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            level.sendParticles(OOZE,
                entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                2, 0.0, 0.02, 0.0, 0.0);
        }
        return true;
    }

    public static void tickPlayerStepSound(ServerLevel level, Player player) {
        if (!player.hasEffect(kingdom.smp.ModEffects.SLIMED_EFFECT)) {
            STEP_SOUNDS.remove(player.getUUID());
            return;
        }
        StepSoundState state = STEP_SOUNDS.computeIfAbsent(player.getUUID(),
            ignored -> new StepSoundState(player.getX(), player.getZ(), 0.0));
        if (!player.onGround() || player.isPassenger()) {
            state.lastX = player.getX();
            state.lastZ = player.getZ();
            state.distanceSqr = 0.0;
            return;
        }
        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        state.lastX = player.getX();
        state.lastZ = player.getZ();
        double movedSqr = dx * dx + dz * dz;
        if (movedSqr < 1.0e-5) {
            return;
        }
        state.distanceSqr += movedSqr;
        if (state.distanceSqr >= WALK_STEP_DISTANCE_SQR) {
            state.distanceSqr = 0.0;
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.35F,
                0.85F + level.getRandom().nextFloat() * 0.25F);
        }
    }

    private static final class StepSoundState {
        double lastX;
        double lastZ;
        double distanceSqr;

        StepSoundState(double lastX, double lastZ, double distanceSqr) {
            this.lastX = lastX;
            this.lastZ = lastZ;
            this.distanceSqr = distanceSqr;
        }
    }
}
