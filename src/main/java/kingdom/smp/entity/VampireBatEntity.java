package kingdom.smp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class VampireBatEntity extends Bat implements Enemy {

    private int panicTicks = 0;
    private LivingEntity currentTarget = null;
    private BlockPos targetPosition;

    public VampireBatEntity(EntityType<? extends Bat> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1.0);
    }

    public static boolean checkVampireBatSpawnRules(EntityType<VampireBatEntity> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason spawnType, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (pos.getY() >= level.getSeaLevel()) return false;
        return level.getMaxLocalRawBrightness(pos) <= 4 && Bat.checkBatSpawnRules(EntityType.BAT, level, spawnType, pos, random);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Custom hunting AI (targeting, biting, damage, sounds) is server-side only.
        // aiStep runs on the client too, where level() is a ClientLevel — the
        // hurtServer(ServerLevel) cast below would otherwise crash with a
        // ClassCastException. Movement is synced to clients normally.
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        if (this.isResting()) {
            this.currentTarget = null;
            // Wake up if it's too bright or player is too close
            if (this.level().getMaxLocalRawBrightness(this.blockPosition()) > 7) {
                this.setResting(false);
                this.panicTicks = 100; // 5 seconds of panic
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BAT_TAKEOFF, this.getSoundSource(), 1.0F, 1.0F);
            } else if (this.random.nextInt(20) == 0) {
                // Occasionally check for targets
                this.currentTarget = findTarget();
                if (this.currentTarget != null) {
                    this.setResting(false);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BAT_TAKEOFF, this.getSoundSource(), 1.0F, 1.0F);
                }
            }
        } else {
            // Wake up logic
            if (this.panicTicks > 0) {
                this.panicTicks--;
                if (this.tickCount % 5 == 0) {
                    List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.0));
                    for (Player p : players) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                    }
                }
                this.flyErratic();
                return;
            }

            // Normal hunting logic
            if (this.currentTarget != null && this.currentTarget.isAlive() && this.currentTarget.distanceToSqr(this) < 400) {
                // Fly towards target
                Vec3 dir = new Vec3(this.currentTarget.getX() - this.getX(),
                                    this.currentTarget.getY() + (this.currentTarget.getBbHeight() / 2.0) - this.getY(),
                                    this.currentTarget.getZ() - this.getZ()).normalize();
                
                this.setDeltaMovement(this.getDeltaMovement().add(dir.scale(0.05)));

                // Bite
                if (this.getBoundingBox().inflate(0.5).intersects(this.currentTarget.getBoundingBox())) {
                    if (this.tickCount % 20 == 0) { // attack cooldown
                        this.currentTarget.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), this.damageSources().mobAttack(this), 1.0F);
                        if (kingdom.smp.ModEffects.BLEEDING_EFFECT != null) {
                            // Apply Bleeding (infinite effectively, wait 100 days)
                            this.currentTarget.addEffect(new MobEffectInstance(kingdom.smp.ModEffects.BLEEDING_EFFECT, 20 * 60 * 60 * 24, 0, false, true, true));
                        }
                    }
                }
                this.setYRot((float)(Mth.atan2(this.getDeltaMovement().z, this.getDeltaMovement().x) * (double)(180F / (float)Math.PI)) - 90.0F);
            } else {
                // Look for new target or roost
                if (this.random.nextInt(20) == 0) {
                    this.currentTarget = findTarget();
                }
                
                if (this.currentTarget == null) {
                    if (this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 7) {
                        // Try to roost
                        if (this.random.nextInt(20) == 0) {
                            BlockPos above = this.blockPosition().above();
                            if (this.level().getBlockState(above).isRedstoneConductor(this.level(), above)) {
                                this.setResting(true);
                                return;
                            }
                        }
                    }
                    this.flyErratic();
                }
            }
        }
    }

    private LivingEntity findTarget() {
        AABB aabb = this.getBoundingBox().inflate(16.0);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, aabb, p -> !p.isSpectator() && !p.isCreative());
        if (!players.isEmpty()) return players.get(this.random.nextInt(players.size()));

        List<Animal> animals = this.level().getEntitiesOfClass(Animal.class, aabb);
        if (!animals.isEmpty()) return animals.get(this.random.nextInt(animals.size()));

        return null;
    }

    private void flyErratic() {
        if (this.targetPosition == null || this.targetPosition.distSqr(this.blockPosition()) < 2 || this.random.nextInt(30) == 0) {
            this.targetPosition = this.blockPosition().offset(this.random.nextInt(7) - 3, this.random.nextInt(6) - 2, this.random.nextInt(7) - 3);
        }
        double dx = this.targetPosition.getX() + 0.5D - this.getX();
        double dy = this.targetPosition.getY() + 0.1D - this.getY();
        double dz = this.targetPosition.getZ() + 0.5D - this.getZ();
        Vec3 vel = this.getDeltaMovement();
        Vec3 vel2 = vel.add((Math.signum(dx) * 0.5D - vel.x) * 0.1D, (Math.signum(dy) * 0.7D - vel.y) * 0.1D, (Math.signum(dz) * 0.5D - vel.z) * 0.1D);
        this.setDeltaMovement(vel2);
        this.setYRot((float)(Mth.atan2(vel2.z, vel2.x) * (double)(180F / (float)Math.PI)) - 90.0F);
    }
}
