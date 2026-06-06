package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On moon side/ceiling faces the collision box is reoriented (tall along a horizontal
 * axis), but vanilla still believes the player is 0.6x1.8 in Y. {@code updatePlayerPose}
 * therefore keeps re-checking stand-vs-crouch headroom against that Y-tall phantom and
 * rapidly toggles the pose — the avatar "jumps and crouches." On faces we pin the pose to
 * STANDING instead of letting vanilla run: that stops the toggle AND nails the eye height to
 * the standing value. (Merely cancelling froze whatever pose you arrived in — enter the moon
 * mid-crouch/-swim and the eye stayed at that smaller height the whole time on walls, sitting
 * lower than the box expects and easier to bury.)
 */
@Mixin(Player.class)
public abstract class PlayerGravityPoseMixin {

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void ironhold$freezePoseOnFaces(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)
                && GravityHelper.getGravityDirection(self) != Direction.DOWN) {
            self.setPose(Pose.STANDING);
            ci.cancel();
        }
    }
}
