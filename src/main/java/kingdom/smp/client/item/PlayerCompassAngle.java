package kingdom.smp.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kingdom.smp.item.IronholdItemComponents;
import kingdom.smp.item.PlayerTrackerTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Range-select item-model property that drives the player-compass needle.
 *
 * Mirrors vanilla {@code CompassAngleState}: when there's a valid same-dimension
 * target we wobble toward it; when there isn't (no binding, or target is in a
 * different dimension) we spin randomly via the no-target wobbler.
 */
public class PlayerCompassAngle extends NeedleDirectionHelper implements RangeSelectItemModelProperty {

    public static final MapCodec<PlayerCompassAngle> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.BOOL.optionalFieldOf("wobble", true).forGetter(PlayerCompassAngle::wobbleField))
            .apply(i, PlayerCompassAngle::new));

    private final NeedleDirectionHelper.Wobbler trackedWobbler;
    private final NeedleDirectionHelper.Wobbler spinWobbler;
    private final RandomSource random = RandomSource.create();

    public PlayerCompassAngle(boolean wobble) {
        super(wobble);
        this.trackedWobbler = this.newWobbler(0.8F);
        this.spinWobbler = this.newWobbler(0.8F);
    }

    private boolean wobbleField() {
        return this.wobble();
    }

    @Override
    protected float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner) {
        PlayerTrackerTarget data = stack.get(IronholdItemComponents.PLAYER_TRACKER.get());
        GlobalPos target = data == null ? null : data.lastKnownPos().orElse(null);
        long gameTime = level.getGameTime();
        if (target == null || target.dimension() != owner.level().dimension()) {
            return spin(seed, gameTime);
        }
        if (target.pos().distToCenterSqr(owner.position()) < 1.0E-5F) {
            return spin(seed, gameTime);
        }
        return pointAt(owner, gameTime, target.pos());
    }

    @Override
    public MapCodec<PlayerCompassAngle> type() {
        return MAP_CODEC;
    }

    private float spin(int seed, long gameTime) {
        if (this.spinWobbler.shouldUpdate(gameTime)) {
            this.spinWobbler.update(gameTime, this.random.nextFloat());
        }
        float target = this.spinWobbler.rotation() + hash(seed) / 2.1474836E9F;
        return Mth.positiveModulo(target, 1.0F);
    }

    private float pointAt(ItemOwner owner, long gameTime, BlockPos targetPos) {
        float angleToTarget = (float) angleFromOwnerTo(owner, targetPos);
        float ownerYRot = Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
        float rotation;
        if (owner.asLivingEntity() instanceof Player p && p.isLocalPlayer() && p.level().tickRateManager().runsNormally()) {
            if (this.trackedWobbler.shouldUpdate(gameTime)) {
                this.trackedWobbler.update(gameTime, 0.5F - (ownerYRot - 0.25F));
            }
            rotation = angleToTarget + this.trackedWobbler.rotation();
        } else {
            rotation = 0.5F - (ownerYRot - 0.25F - angleToTarget);
        }
        return Mth.positiveModulo(rotation, 1.0F);
    }

    private static double angleFromOwnerTo(ItemOwner owner, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        Vec3 here = owner.position();
        return Math.atan2(target.z() - here.z(), target.x() - here.x()) / (Math.PI * 2);
    }

    private static int hash(int input) {
        return input * 1327217883;
    }
}
