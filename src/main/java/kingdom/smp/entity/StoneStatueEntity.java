package kingdom.smp.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A humanoid stone statue — a frozen, AI-less decoration rendered with the
 * stonified player model (see {@link kingdom.smp.client.entity.StoneStatueRenderer}).
 * It never wanders, looks, or animates ({@code setNoAi(true)}) and defies gravity
 * ({@code setNoGravity(true)}), so it floats exactly where it's placed.
 *
 * <p>It is immune to all survival/environmental damage (attacks, fire, lava,
 * fall, explosions) via the invulnerable flag — but a <b>creative-mode</b> player
 * can still break it, because vanilla's {@code isInvulnerableTo} deliberately
 * lets creative-player damage through. ({@code /kill} also removes one.)
 */
public class StoneStatueEntity extends PathfinderMob {

    public StoneStatueEntity(EntityType<? extends StoneStatueEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();   // a placed statue is permanent — never despawns
        this.setNoAi(true);              // frozen: no pathfinding, no looking
        this.setNoGravity(true);         // defies gravity — floats where placed
        this.setInvulnerable(true);      // immune to survival/environmental damage; creative can still break it
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)   // unmovable when struck
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    /** A statue has no behavior. */
    @Override
    protected void registerGoals() {}

    private boolean gridSnapped = false;

    /**
     * On the first server tick after spawn/placement, snap to the block grid:
     * centre on the block column (x/z to .5) and align vertically to the nearest
     * block line. Also turn to face the nearest player — when placed via spawn
     * egg that's whoever just used it, so a statue always greets its placer
     * instead of spawning at a random yaw. Covers both spawn-egg placement and
     * {@code /summon}. {@code setOldPosAndRot} avoids a one-frame visual slide.
     */
    @Override
    public void tick() {
        super.tick();
        if (!gridSnapped && !this.level().isClientSide()) {
            gridSnapped = true;
            double sx = Math.floor(this.getX()) + 0.5;
            double sz = Math.floor(this.getZ()) + 0.5;
            double sy = Math.round(this.getY());
            float yaw = this.getYRot();
            Player placer = this.level().getNearestPlayer(this, 16.0);
            if (placer != null) {
                double dx = placer.getX() - sx;
                double dz = placer.getZ() - sz;
                yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            }
            this.snapTo(sx, sy, sz, yaw, 0.0F);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
            this.setOldPosAndRot();
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Placed by hand — must outlive distance-based despawn. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Other entities can't shove a stone statue around. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Fully damage-resistant in survival and to the environment (attacks, fire,
     * lava, fall, explosions, mobs) — those all return early with no damage and
     * therefore no red hurt-flash. ONLY a creative-mode player hit (or a
     * {@code /kill}-style {@code BYPASSES_INVULNERABILITY} source) gets through,
     * so a creative player can still break it.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.isCreativePlayer() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }
}
