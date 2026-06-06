package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Telegraphed melee for the {@link StoneGolemEntity}. Unlike vanilla {@code MeleeAttackGoal}
 * (instant hit), every swing runs <b>windup → strike → recovery</b> so the player can read and
 * dodge it. Damage is dealt only on the single "active" frame, as an area-of-effect:
 * <ul>
 *   <li><b>Slam</b> — single big target: radial AoE, heavy damage, launch up + back.</li>
 *   <li><b>Sweep</b> — 2+ clustered targets: 180° frontal arc, lighter damage, horizontal knockback.</li>
 * </ul>
 * The matching GeckoLib animation ("slam"/"sweep") is triggered at windup start; strike timings line
 * up with each animation's impact frame.
 */
public final class StoneGolemAttackGoal extends Goal {

    private enum Phase { APPROACH, WINDUP, RECOVER }
    private enum Attack { SLAM, SWEEP }

    private static final double REACH = 7.5; // scaled for the 2× golem (Attributes.SCALE = 2.0)
    // Strike/end frames (ticks) tuned to the animation impact frames.
    private static final int SLAM_STRIKE = 14, SLAM_END = 28, SLAM_COOLDOWN = 18;
    private static final int SWEEP_STRIKE = 10, SWEEP_END = 20, SWEEP_COOLDOWN = 14;

    private final StoneGolemEntity golem;
    private Phase phase = Phase.APPROACH;
    private Attack attack = Attack.SLAM;
    private int timer;
    private int cooldown;
    private boolean struck;

    public StoneGolemAttackGoal(StoneGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = golem.getTarget();
        return t != null && t.isAlive() && !golem.isStaggered() && golem.isAwake() && !golem.isWaking();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || phase != Phase.APPROACH; // finish an in-progress swing
    }

    @Override
    public void stop() {
        phase = Phase.APPROACH;
        struck = false;
        golem.setCharge(0f);
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
        LivingEntity target = golem.getTarget();
        if (target != null) {
            golem.getLookControl().setLookAt(target, 30f, 30f);
        }

        if (golem.isStaggered()) {
            golem.getNavigation().stop();
            return;
        }

        switch (phase) {
            case APPROACH -> {
                if (target == null) {
                    return;
                }
                double dist = golem.distanceTo(target);
                if (dist <= REACH && cooldown <= 0 && golem.getSensing().hasLineOfSight(target)) {
                    int near = golem.level().getEntitiesOfClass(LivingEntity.class,
                        golem.getBoundingBox().inflate(REACH),
                        e -> e != golem && !(e instanceof StoneGolemEntity) && e.isAlive()
                            && !golem.isAlliedTo(e)).size();
                    attack = near >= 2 ? Attack.SWEEP : Attack.SLAM;
                    phase = Phase.WINDUP;
                    timer = 0;
                    struck = false;
                    golem.getNavigation().stop();
                    golem.triggerAnim("action", attack == Attack.SLAM ? "slam" : "sweep");
                } else {
                    golem.getNavigation().moveTo(target, 1.0);
                }
            }
            case WINDUP -> {
                golem.getNavigation().stop();
                if (target != null) {
                    faceHard(target);
                }
                timer++;
                int strikeAt = attack == Attack.SLAM ? SLAM_STRIKE : SWEEP_STRIKE;
                int endAt = attack == Attack.SLAM ? SLAM_END : SWEEP_END;
                // Gather the inner light through the wind-up — the eye glow swells, peaking at the strike.
                golem.setCharge(Math.min(1f, timer / (float) strikeAt));
                if (timer >= strikeAt && !struck) {
                    struck = true;
                    doStrike();
                }
                if (timer >= endAt) {
                    cooldown = attack == Attack.SLAM ? SLAM_COOLDOWN : SWEEP_COOLDOWN;
                    if (golem.isEnraged()) {
                        cooldown = cooldown * 3 / 5; // enraged: ~40% faster
                    }
                    phase = Phase.RECOVER;
                }
            }
            case RECOVER -> {
                golem.setCharge(0f); // release: the glow drops back to its resting heartbeat
                phase = Phase.APPROACH;
            }
        }
    }

    private void faceHard(LivingEntity t) {
        double dx = t.getX() - golem.getX();
        double dz = t.getZ() - golem.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
        golem.setYRot(yaw);
        golem.yBodyRot = yaw;
        golem.yHeadRot = yaw;
    }

    private void doStrike() {
        if (!(golem.level() instanceof ServerLevel sl)) {
            return;
        }
        float base = (float) golem.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 facing = Vec3.directionFromRotation(0f, golem.getYRot());

        if (attack == Attack.SLAM) {
            // Readable, dodgeable AoE: an expanding shockwave radiates from the strike point and the
            // damage lands as the wavefront passes (handled in StoneGolemEntity), not instantly.
            golem.spawnShockwave(golem.position().add(facing.scale(3.0)), 8.0, base);
        } else {
            // Sweep stays an instant 180° frontal arc — a fast, lighter follow-up.
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, golem.getBoundingBox().inflate(8.0))) {
                if (e == golem || e instanceof StoneGolemEntity || golem.isAlliedTo(e)) {
                    continue;
                }
                Vec3 to = e.position().subtract(golem.position());
                if (facing.x * to.x + facing.z * to.z < 0) {
                    continue; // behind the golem — outside the frontal arc
                }
                e.hurtServer(sl, sl.damageSources().mobAttack(golem), base * 0.7f);
                e.knockback(0.85, golem.getX() - e.getX(), golem.getZ() - e.getZ());
            }
        }
        Vec3 impact = golem.position().add(facing.scale(2.2));
        sl.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.1, impact.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.1, impact.z, 24, 1.2, 0.1, 1.2, 0.02);
        sl.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.4f, 0.55f);
    }
}
