package kingdom.smp.client.entity;

import com.mojang.math.Axis;
import kingdom.smp.client.HalricStaffTransformDebug;
import kingdom.smp.item.HalricStaffItem;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders Halric's Staff with the GeckoLib animated chain. The transforms here
 * mirror the held-context values tuned via {@code /halricstaffdebug} — those
 * stay applied to the GUI/ground/fixed contexts via the static fallback model,
 * but held contexts (first/third person) render through GeckoLib so the chain
 * can sway and (eventually) react to motion.
 */
public class HalricStaffRenderer extends GeoItemRenderer<HalricStaffItem> {

    public HalricStaffRenderer() {
        super(new HalricStaffModel());
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo,
                              SubmitNodeCollector collector) {
        super.preRenderPass(renderPassInfo, collector);

        // Set the gravity bias based on this render's display context BEFORE the
        // physics ticks — otherwise the bias lags one frame.
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);
        computeAndSetGravityBias(ctx);

        // Tick both physics systems each frame (deduped internally to once per
        // game tick). First-person uses HalricStaffChainPhysics (analytical
        // velocity-driven swing — works well there). Third-person uses
        // HalricStaffRopePhysics (world-space verlet rope — handles the
        // unknown arm-bone transform that broke analytical gravity).
        HalricStaffChainPhysics.tickIfDue();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            HalricStaffRopePhysics.tickIfDue(HalricStaffRopePhysics.defaultAnchorFor(player));
        }

        // Left-hand display is a mirror of right-hand display; mirror the chain
        // swing direction (Z) to match, so the chain still trails the player's
        // motion correctly instead of swinging into the staff.
        boolean isLeftHand =
            ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        float zSign = isLeftHand ? -1f : 1f;

        // First-person swing intensity scaling. Velocity-driven chain motion is
        // visually amplified in first-person (camera close to the staff), so we
        // dampen it by 60% to keep it feeling natural. Gravity rest pose is
        // unaffected — only the swing-around-rest portion gets scaled.
        boolean isFirstPerson =
            ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        float swingScale = isFirstPerson ? 0.4f : 1.0f;

        BakedGeoModel baked = renderPassInfo.model();
        for (int i = 0; i < HalricStaffChainPhysics.LINKS; i++) {
            final int idx = i;
            baked.getBone("chain_link_" + (i + 1)).ifPresent(bone -> {
                // GeckoLib lazily allocates frameSnapshot only for bones the
                // animation controller touches. Our animation is empty (physics
                // drives chain bones) so we have to initialize the snapshot here.
                if (bone.frameSnapshot == null) {
                    bone.frameSnapshot = BoneSnapshot.create(bone);
                }

                // Decompose theta into (rest bias + velocity swing), scale only
                // the swing, recombine. Bias is non-uniform across links —
                // only chain_link_1 carries gravity bias, others have 0 bias.
                float biasX = HalricStaffChainPhysics.getBiasXForLinkRadians(idx);
                float biasZ = HalricStaffChainPhysics.getBiasZForLinkRadians(idx);
                float thetaX = HalricStaffChainPhysics.getRotXRadians(idx);
                float thetaZ = HalricStaffChainPhysics.getRotZRadians(idx);
                float outX = biasX + swingScale * (thetaX - biasX);
                float outZ = biasZ + swingScale * (thetaZ - biasZ);

                bone.frameSnapshot.setRotation(outX, 0f, zSign * outZ);
            });
        }
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        var ps = renderPassInfo.poseStack();

        // Read the per-context transform live from HalricStaffTransformDebug so
        // /halricstaffdebug tuning takes effect immediately, and the print output
        // is already what the renderer uses (no double-apply via the mixin).
        // Convention: MC display block applies translate → rotate XYZ → scale,
        // with translation in 1/16-block units.
        String ctxKey = switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
            case THIRD_PERSON_LEFT_HAND  -> "thirdperson_lefthand";
            case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
            case FIRST_PERSON_LEFT_HAND  -> "firstperson_lefthand";
            default -> null;
        };
        if (ctxKey != null) {
            HalricStaffTransformDebug.Transform t = HalricStaffTransformDebug.forContext(ctxKey);
            if (t != null) {
                ps.translate(t.transX / 16F, t.transY / 16F, t.transZ / 16F);
                ps.mulPose(Axis.XP.rotationDegrees(t.rotX));
                ps.mulPose(Axis.YP.rotationDegrees(t.rotY));
                ps.mulPose(Axis.ZP.rotationDegrees(t.rotZ));
                ps.scale(t.scale, t.scale, t.scale);
            }
        }

        // Third-person: override chain bone rotations using the world-space
        // verlet rope. preRenderPass already set bones from the analytical
        // physics; we overwrite chain_link_1 with the verlet-derived
        // rotation here, after the display transforms have been applied to
        // the pose stack (so we have the correct model-to-view matrix).
        boolean isThirdPerson =
            ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (isThirdPerson) {
            applyVerletToBones(renderPassInfo);
        }
    }

    /**
     * Converts the verlet rope's world-space direction into a chain_link_1
     * rotation, using the actual pose-stack matrix to handle the unknown
     * arm-bone transform that breaks analytical Euler bias for third-person.
     */
    private void applyVerletToBones(RenderPassInfo<GeoRenderState> renderPassInfo) {
        if (HalricStaffRopePhysics.getParticleWorld(0).equals(Vec3.ZERO)) return;

        var ps = renderPassInfo.poseStack();

        // Rope's overall direction in WORLD space (anchor → tip).
        Vec3 anchor = HalricStaffRopePhysics.getParticleWorld(0);
        Vec3 tip    = HalricStaffRopePhysics.getParticleWorld(HalricStaffRopePhysics.LINKS - 1);
        Vec3 dirWorld = tip.subtract(anchor);
        double dlen = dirWorld.length();
        if (dlen < 1e-6) return;
        dirWorld = dirWorld.scale(1.0 / dlen);

        // Convert world direction → camera-relative via inverse camera rotation.
        // Camera.rotation() is local-to-world (camera-frame → world), so inverting
        // gives world → camera.
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf invCamRot = new Quaternionf(cam.rotation()).invert();
        Vector3f dir = new Vector3f((float) dirWorld.x, (float) dirWorld.y, (float) dirWorld.z);
        invCamRot.transform(dir);

        // Convert camera-relative → model-local via inverse pose-stack rotation.
        // ps.last().pose() at this point is model-to-camera (display transforms
        // applied), so its inverse maps the camera-frame direction back into
        // model-local — the parent frame of chain_link_1.
        Matrix3f poseRotInv = new Matrix3f(ps.last().pose()).invert();
        poseRotInv.transform(dir);

        // Decompose target direction into chain_link_1's Euler X+Z bone rotation.
        // GeckoLib applies bone rotation as Mojang's Z·Y·X-on-vertex convention,
        // so for setRotation(α, 0, β) the chain's local -Y maps to:
        //   R_Z(β)·R_Y(0)·R_X(α)·(0, -1, 0) = (cos α sin β, -cos α cos β, -sin α)
        // Decompose: α = asin(-dir.z); β = atan2(dir.x, -dir.y).
        float rotX = (float) Math.asin(Mth.clamp(-dir.z, -1f, 1f));
        float rotZ = (float) Math.atan2(dir.x, -dir.y);

        // Apply to chain_link_1; zero out the other links (rigid chain for now,
        // per-link bending is a follow-up).
        BakedGeoModel baked = renderPassInfo.model();
        baked.getBone("chain_link_1").ifPresent(bone -> {
            if (bone.frameSnapshot == null) bone.frameSnapshot = BoneSnapshot.create(bone);
            bone.frameSnapshot.setRotation(rotX, 0f, rotZ);
        });
        for (int i = 2; i <= HalricStaffRopePhysics.LINKS; i++) {
            final String boneName = "chain_link_" + i;
            baked.getBone(boneName).ifPresent(bone -> {
                if (bone.frameSnapshot == null) bone.frameSnapshot = BoneSnapshot.create(bone);
                bone.frameSnapshot.setRotation(0f, 0f, 0f);
            });
        }
    }

    /**
     * Computes the chain's gravity-bias rotation from the current display
     * context's rotation, so the chain hangs along world-down regardless of
     * how the staff is tilted in the player's hand.
     *
     * <p>Derivation: the chain at rest hangs along the model's local -Y axis.
     * After display rotation R, that local -Y points to a direction in the
     * rendering frame (camera-local for first-person, body-local for
     * third-person). To pull the chain to true world-down, we compute
     * world-down expressed in the rendering frame, then transform by R⁻¹ to
     * get the chain's target direction in model-local, then decompose to
     * Euler X+Z bends.
     *
     * <p><b>Per view:</b>
     * <ul>
     *   <li><b>Third-person:</b> the staff is body-attached. Body has no pitch
     *       in MC, so world-down in body-local is just {@code (0, -1, 0)}.
     *       Body yaw rotates around world-Y, symmetric for world-down — no
     *       contribution.</li>
     *   <li><b>First-person:</b> the staff is camera-attached. Camera pitch
     *       tilts the rendering frame, so world-down in camera-local is
     *       {@code (0, -cos(pitch), -sin(pitch))}. Camera yaw is irrelevant
     *       (world-Y symmetric).</li>
     * </ul>
     */
    private static void computeAndSetGravityBias(ItemDisplayContext ctx) {
        String ctxKey = switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
            case THIRD_PERSON_LEFT_HAND  -> "thirdperson_lefthand";
            case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
            case FIRST_PERSON_LEFT_HAND  -> "firstperson_lefthand";
            default -> null;
        };
        if (ctxKey == null) {
            HalricStaffChainPhysics.setGravityBiasDegrees(0f, 0f);
            return;
        }
        HalricStaffTransformDebug.Transform t = HalricStaffTransformDebug.forContext(ctxKey);
        if (t == null) {
            HalricStaffChainPhysics.setGravityBiasDegrees(0f, 0f);
            return;
        }

        // World-down expressed in the staff's rendering frame. For first-person:
        // empirically MC's hand-render frame has -X as the "forward" axis here
        // (the direction the staff is held), so pitch tilts world-down into a
        // negative X component:
        //   pitch +θ (look down) → world-down = (-sin θ, -cos θ, 0)
        // Looking down then makes the chain bend toward camera-forward.
        boolean isFirstPerson =
            ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        float worldDownX = 0f;
        float worldDownY = -1f;
        float worldDownZ = 0f;
        if (isFirstPerson) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                float pitchRad = player.getXRot() * Mth.DEG_TO_RAD;
                // Off-hand (left) display is a mirrored pose, so the pitch
                // contribution to "forward" world-down direction flips sign.
                float pitchSign = (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) ? 1f : -1f;
                // Looking up swings the chain way more than feels natural
                // (the lantern was hanging slightly behind to begin with, so
                // the backward lean compounds). Dampen the upward direction
                // (negative pitch) while keeping the downward swing at full.
                float pitchScale = pitchRad >= 0f ? 1.0f : 0.4f;
                worldDownX = pitchSign * pitchScale * (float) Math.sin(pitchRad);
                worldDownY = (float) -Math.cos(pitchRad);
            }
        }

        // Apply inverse display rotation to world-down to get the chain's
        // target direction in model-local space.
        // Inverse of R_X(rx)·R_Y(ry)·R_Z(rz) is R_Z(-rz)·R_Y(-ry)·R_X(-rx),
        // applied right-to-left to the vector.
        float rxRad = -t.rotX * Mth.DEG_TO_RAD;
        float ryRad = -t.rotY * Mth.DEG_TO_RAD;
        float rzRad = -t.rotZ * Mth.DEG_TO_RAD;

        // Step 1: v1 = R_X(-rxDisplay) · worldDown
        float cosX = (float) Math.cos(rxRad);
        float sinX = (float) Math.sin(rxRad);
        float v1x = worldDownX;
        float v1y = worldDownY * cosX - worldDownZ * sinX;
        float v1z = worldDownY * sinX + worldDownZ * cosX;

        // Step 2: v2 = R_Y(-ryDisplay) · v1
        float cosY = (float) Math.cos(ryRad);
        float sinY = (float) Math.sin(ryRad);
        float v2x = v1x * cosY + v1z * sinY;
        float v2y = v1y;
        float v2z = -v1x * sinY + v1z * cosY;

        // Step 3: v3 = R_Z(-rzDisplay) · v2
        float cosZ = (float) Math.cos(rzRad);
        float sinZ = (float) Math.sin(rzRad);
        float v3x = v2x * cosZ - v2y * sinZ;
        float v3y = v2x * sinZ + v2y * cosZ;
        float v3z = v2z;

        // Decompose v3 = (vx, vy, vz) into chain bone Euler angles
        // (R_X(α)·R_Z(β) applied to model-local -Y direction):
        //   R_X(α)·R_Z(β)·(0, -1, 0) = (sin β, -cos β · cos α, -cos β · sin α)
        // So β = asin(vx); α = atan2(-vz, -vy).
        float betaRad  = (float) Math.asin(Mth.clamp(v3x, -1f, 1f));
        float alphaRad = (float) Math.atan2(-v3z, -v3y);

        float xBiasDeg = alphaRad * Mth.RAD_TO_DEG;
        float zBiasDeg = betaRad  * Mth.RAD_TO_DEG;

        // Third-person rendering inserts an MC arm-bone transform between the
        // player body and the display rotation that our analytical derivation
        // doesn't model. Rather than fight it with sign-flips that never quite
        // converge, just zero out gravity bias for third-person — the chain
        // hangs along the staff's local -Y axis, which after the user's tuned
        // display rotation lands close enough to world-down for the rest pose.
        // Verlet-rope-in-world-space is the proper fix; flagged as Path B.
        if (!isFirstPerson) {
            xBiasDeg = 0f;
            zBiasDeg = 0f;
        }

        HalricStaffChainPhysics.setGravityBiasDegrees(xBiasDeg, zBiasDeg);
    }
}
