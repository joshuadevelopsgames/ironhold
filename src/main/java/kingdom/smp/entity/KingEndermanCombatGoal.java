package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.EnumSet;

/**
 * Move-picker AI for King Enderman.
 *
 * <p>Each tick, if no move is "in progress", picks a move based on phase, distance,
 * and per-move cooldowns, then runs it as a multi-tick state machine. Between moves
 * the boss does a short blink-reposition for tempo (the ender-king feel).
 *
 * <p>Per-move audio comes in three beats: <b>telegraph</b> (start, lets the player
 * read the wind-up), <b>execute</b> (peak of the move), <b>impact</b> (the payoff).
 */
public class KingEndermanCombatGoal extends Goal {

    public enum Move { NONE, MELEE, EYE_VOLLEY, TELESMASH, BLINK }

    private final KingEndermanEntity king;

    private LivingEntity target;
    private Move activeMove = Move.NONE;
    private int moveTick;

    private int cdMelee;
    private int cdVolley;
    private int cdTelesmash;
    private int cdBlink;
    private int globalCd;

    /** Position cached at telesmash teleport-up — used for the impact-zone telegraph ring. */
    private Vec3 telesmashTarget = Vec3.ZERO;

    public KingEndermanCombatGoal(KingEndermanEntity king) {
        this.king = king;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (king.isInPylonShield()) return false;
        target = king.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (king.isInPylonShield()) return false;
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        king.getNavigation().moveTo(target, 1.0);
    }

    @Override
    public void stop() {
        activeMove = Move.NONE;
        moveTick = 0;
        king.setTelesmashCharging(false);
        king.setTelesmashFalling(false);
        king.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;
        king.getLookControl().setLookAt(target, 30F, 30F);

        if (cdMelee > 0)     cdMelee--;
        if (cdVolley > 0)    cdVolley--;
        if (cdTelesmash > 0) cdTelesmash--;
        if (cdBlink > 0)     cdBlink--;
        if (globalCd > 0)    globalCd--;

        if (activeMove != Move.NONE) {
            tickActiveMove();
            return;
        }

        double distSq = king.distanceToSqr(target);
        Move next = pickMove(distSq);
        if (next != Move.NONE) startMove(next);
        else maintainEngage(distSq);
    }

    // -------- Move selection -----------------------------------------------

    private Move pickMove(double distSq) {
        boolean rage = king.getPhase() == KingEndermanEntity.Phase.PHASE_3;
        boolean phase2plus = king.getPhase() != KingEndermanEntity.Phase.PHASE_1;

        // During the global cooldown, only fast actions are allowed.
        if (globalCd > 0) {
            if (cdBlink == 0 && distSq > 16.0 && distSq < 144.0) return Move.BLINK;
            if (cdMelee == 0 && distSq < 16.0)                    return Move.MELEE;
            return Move.NONE;
        }

        // 25% chance per tick to throw in a blink for tempo even when other moves are up.
        if (cdBlink == 0 && distSq < 256.0 && king.getRandom().nextFloat() < 0.04F) {
            return Move.BLINK;
        }

        if (distSq > 100.0) {
            if (cdVolley == 0)                            return Move.EYE_VOLLEY;
            if (phase2plus && cdTelesmash == 0)           return Move.TELESMASH;
            if (cdBlink == 0)                             return Move.BLINK;
            return Move.NONE;
        }

        if (distSq > 25.0) {
            if (phase2plus && cdTelesmash == 0
                && king.getRandom().nextFloat() < 0.55F)  return Move.TELESMASH;
            if (cdVolley == 0)                             return Move.EYE_VOLLEY;
            if (rage && phase2plus && cdTelesmash == 0)    return Move.TELESMASH;
            return Move.NONE;
        }

        if (cdMelee == 0)                                                  return Move.MELEE;
        if (rage && cdVolley == 0 && king.getRandom().nextFloat() < 0.35F) return Move.EYE_VOLLEY;
        return Move.NONE;
    }

    private void maintainEngage(double distSq) {
        if (distSq > 4.0) king.getNavigation().moveTo(target, 1.0);
        else              king.getNavigation().stop();
    }

    // -------- Move start ---------------------------------------------------

    private void startMove(Move move) {
        activeMove = move;
        moveTick = 0;
        switch (move) {
            case MELEE -> {
                cdMelee  = rageScale(20);
                globalCd = 4;
                playKing(SoundEvents.ENDERMAN_AMBIENT, 1.5F, 0.55F);  // telegraph: low growl
            }
            case EYE_VOLLEY -> {
                cdVolley = rageScale(80);
                globalCd = 22;
                playKing(SoundEvents.ENDERMAN_STARE, 2.0F, 0.7F);     // telegraph: stare hum
            }
            case TELESMASH -> {
                cdTelesmash = rageScale(140);
                globalCd    = 30;
                king.setTelesmashCharging(true);
                playKing(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 0.5F); // telegraph: deep charge
            }
            case BLINK -> {
                cdBlink  = rageScale(40);
                globalCd = 3;
            }
            default -> { /* no-op */ }
        }
    }

    private int rageScale(int base) {
        return king.getPhase() == KingEndermanEntity.Phase.PHASE_3
            ? Math.max(8, (int) (base * 0.55F))
            : base;
    }

    // -------- Move execution -----------------------------------------------

    private void tickActiveMove() {
        moveTick++;
        switch (activeMove) {
            case MELEE      -> tickMelee();
            case EYE_VOLLEY -> tickEyeVolley();
            case TELESMASH  -> tickTelesmash();
            case BLINK      -> tickBlink();
            default         -> finishMove();
        }
    }

    private void finishMove() {
        activeMove = Move.NONE;
        moveTick = 0;
        king.setTelesmashCharging(false);
        king.setTelesmashFalling(false);
    }

    // ---- MELEE: 2-arm sweep, wind-up tick 4, hit tick 8, recover to 14
    private void tickMelee() {
        if (moveTick == 4) {
            // Whoosh telegraph
            playKing(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.5F);
        }
        if (moveTick == 8) {
            if (king.distanceToSqr(target) < 16.0 && target.isAlive()) {
                if (king.level() instanceof ServerLevel sl) {
                    DamageSource src = sl.damageSources().mobAttack(king);
                    target.hurtServer(sl, src,
                        (float) king.getAttributeValue(Attributes.ATTACK_DAMAGE));
                }
                king.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                playKing(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6F, 0.6F);
            }
        }
        if (moveTick >= 14) finishMove();
    }

    // ---- EYE VOLLEY: charge particles 0..15, fire at 16, recover to 30
    private void tickEyeVolley() {
        if (king.level() instanceof ServerLevel sl) {
            // Charge particles building at the king's chest height during wind-up.
            if (moveTick > 0 && moveTick <= 15 && moveTick % 2 == 0) {
                Vec3 origin = king.position().add(0, king.getEyeHeight() * 0.7, 0);
                sl.sendParticles(ParticleTypes.PORTAL,
                    origin.x, origin.y, origin.z, 4, 0.6, 0.4, 0.6, 0.0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    origin.x, origin.y, origin.z, 2, 0.3, 0.2, 0.3, 0.05);
            }
        }
        if (moveTick == 8)  playKing(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.7F);   // mid-charge ping
        if (moveTick == 16) {
            int count = (king.getPhase() == KingEndermanEntity.Phase.PHASE_3) ? 6 : 4;
            fireEyeVolley(count);
            playKing(SoundEvents.WITHER_SHOOT, 1.4F, 0.9F);                            // execute
            playKing(SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.8F);
        }
        if (moveTick >= 30) finishMove();
    }

    private void fireEyeVolley(int count) {
        if (!(king.level() instanceof ServerLevel sl)) return;
        Vec3 origin = king.position().add(0, king.getEyeHeight() * 0.7, 0);
        Vec3 toTarget = target.getEyePosition().subtract(origin).normalize();
        Vec3 right = toTarget.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);

        float spread = 0.45F;
        for (int i = 0; i < count; i++) {
            float offset = ((float) i / Math.max(1, count - 1) - 0.5F) * 2F;
            Vec3 dir = toTarget.add(right.scale(offset * spread)).normalize();
            dir = dir.add(0, ((i % 2 == 0) ? 0.05 : -0.05), 0).normalize();
            KingEnderEyeEntity eye = new KingEnderEyeEntity(king, dir, sl);
            Vec3 spawn = origin.add(toTarget.scale(1.5));
            eye.setPos(spawn.x, spawn.y, spawn.z);
            sl.addFreshEntity(eye);
        }
    }

    // ---- TELESMASH: charge 0..16, TP-up + impact ring telegraph 16..18, fall to land,
    //                 then impact and recover.
    private void tickTelesmash() {
        if (king.level() instanceof ServerLevel sl) {
            // Charge particles at king's feet during wind-up.
            if (moveTick > 0 && moveTick <= 15 && moveTick % 2 == 0) {
                Vec3 p = king.position();
                sl.sendParticles(ParticleTypes.PORTAL,
                    p.x, p.y + 0.5, p.z, 6, 1.2, 0.6, 1.2, 0.05);
            }
            // After teleport: paint a ground ring at the predicted impact spot every 4 ticks.
            if (moveTick > 16 && !king.onGround() && moveTick % 4 == 0) {
                drawImpactRing(sl, telesmashTarget, 5.0);
            }
        }
        if (moveTick == 8)  playKing(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.6F);   // mid-charge boom

        if (moveTick == 16) {
            // Teleport above predicted target position.
            Vec3 predicted = target.position().add(target.getDeltaMovement().scale(8));
            int yTop = king.level().getMaxY();
            int targetY = (int) Math.min(predicted.y + 14, yTop - 2);
            telesmashTarget = new Vec3(predicted.x, predicted.y, predicted.z);
            king.teleportTo(predicted.x, targetY, predicted.z);
            king.setDeltaMovement(0, -0.1, 0);
            king.fallDistance = 0;
            king.setTelesmashFalling(true);
            playKing(SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.5F);
            playKing(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2F, 0.5F);
            if (king.level() instanceof ServerLevel sl) {
                Vec3 p = king.position();
                sl.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 80, 1.2, 1.5, 1.2, 0.6);
            }
        }
        if (moveTick > 18 && king.onGround()) {
            telesmashImpact();
            finishMove();
            return;
        }
        if (moveTick >= 80) {     // safety bail (mid-air platform / void)
            telesmashImpact();
            finishMove();
        }
    }

    private void drawImpactRing(ServerLevel sl, Vec3 center, double radius) {
        int steps = 36;
        // Drop the y down to ground level under the predicted spot.
        double groundY = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            (int) center.x, (int) center.z);
        for (int i = 0; i < steps; i++) {
            double a = (i / (double) steps) * Math.PI * 2;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, groundY + 0.1, z, 1, 0, 0.05, 0, 0.0);
        }
    }

    private void telesmashImpact() {
        if (!(king.level() instanceof ServerLevel sl)) return;
        king.setTelesmashFalling(false);
        Vec3 center = king.position();
        float damage = 18F;
        if (king.getPhase() == KingEndermanEntity.Phase.PHASE_3) damage += 8F;
        double radiusSq = 25.0;
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class,
                king.getBoundingBox().inflate(8, 4, 8))) {
            if (entity == king) continue;
            if (entity.distanceToSqr(center) > radiusSq) continue;
            entity.hurtServer(sl, sl.damageSources().mobAttack(king), damage);
            Vec3 push = entity.position().subtract(center).normalize();
            entity.push(push.x * 1.4, 0.7, push.z * 1.4);
        }
        // Ring of dust + central explosion particle.
        for (int i = 0; i < 64; i++) {
            double angle = (i / 64.0) * Math.PI * 2;
            double r = 5.0;
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x + Mth.cos((float) angle) * r, center.y + 0.1,
                center.z + Mth.sin((float) angle) * r,
                1, 0, 0.05, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 4, 1.5, 0.5, 1.5, 0);
        // Impact audio: lightning + warden boom + generic explosion stack.
        playKing(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.6F, 0.6F);
        playKing(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.7F);
        playKing(SoundEvents.GENERIC_EXPLODE.value(), 2.2F, 0.5F);
    }

    // ---- BLINK: instant short reposition. Ender-king tempo glue between specials.
    private void tickBlink() {
        if (moveTick == 1) {
            doBlink();
        }
        if (moveTick >= 4) finishMove();
    }

    private void doBlink() {
        if (!(king.level() instanceof ServerLevel sl)) return;
        // Pick a fresh angle: between 4–7 blocks from target, on a random side.
        Vec3 toKing = king.position().subtract(target.position());
        double yaw = Math.atan2(toKing.z, toKing.x);
        double newYaw = yaw + (king.getRandom().nextFloat() - 0.5F) * Math.PI * 1.2; // ±~110°
        double dist = 4.5 + king.getRandom().nextDouble() * 2.5;
        double tx = target.getX() + Math.cos(newYaw) * dist;
        double tz = target.getZ() + Math.sin(newYaw) * dist;
        double ty = target.getY();
        // Origin particles
        Vec3 origin = king.position();
        sl.sendParticles(ParticleTypes.PORTAL, origin.x, origin.y + 1, origin.z, 30, 0.5, 1.2, 0.5, 0.3);
        king.teleportTo(tx, ty, tz);
        sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1, tz, 30, 0.5, 1.2, 0.5, 0.3);
        playKing(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.85F);
    }

    // -------- Audio helper --------------------------------------------------

    private void playKing(SoundEvent event, float volume, float pitch) {
        king.level().playSound(null, king.getX(), king.getY(), king.getZ(), event,
            king.getSoundSource(), volume, pitch);
    }
}
