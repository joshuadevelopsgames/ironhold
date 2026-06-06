package kingdom.smp.client.entity;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.client.StoneGolemHammerTuning;
import kingdom.smp.entity.StoneGolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * GeckoLib renderer for the {@link StoneGolemEntity}. The golem's main-hand battle hammer is rendered
 * at the {@code weapon_mount} bone (under the right forearm, so it tracks the elbow) via
 * {@link HeldHammerLayer}. A neutral display context ({@code NONE}) is used so the grip is controlled
 * entirely here, decoupled from the player's third-person hammer transform; {@link StoneGolemHammerTuning}
 * is applied to the item's PoseStack each render for live, recompile-free tuning of position / rotation /
 * scale.
 */
public class StoneGolemRenderer extends GeoEntityRenderer<StoneGolemEntity, LivingEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/stone_golem.png");
    private static final ItemDisplayContext CTX = ItemDisplayContext.NONE;

    public StoneGolemRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 2.2F; // 2× golem
        withRenderLayer(new HeldHammerLayer(ctx, this));
        withRenderLayer(new StoneGolemCrackLayer(this));
        withRenderLayer(new StoneGolemGlowLayer(this));
    }

    @Override
    public void extractRenderState(StoneGolemEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        float max = entity.getMaxHealth();
        float healthFrac = max > 0f ? entity.getHealth() / max : 1.0f;
        state.addGeckolibData(StoneGolemCrackLayer.HEALTH_FRACTION, healthFrac);
        state.addGeckolibData(StoneGolemGlowLayer.GLOW, glowPulse(entity, healthFrac, partialTick));
    }

    /**
     * The eye heartbeat: a slow calm pulse at full health that quickens (and flickers) as the golem
     * is broken down, then fades out as it dies. All inputs are client-available, so no extra sync.
     */
    private static float glowPulse(StoneGolemEntity entity, float healthFrac, float partialTick) {
        if (!entity.isAwake()) {
            return 0.12f;                                        // dormant — eyes a faint, faded ember
        }
        float secs = (entity.tickCount + partialTick) / 20f;
        float wounded = Mth.clamp(1f - healthFrac, 0f, 1f);
        float rate = 0.8f + wounded * 2.2f;                       // breaths/sec: calm → panicked
        float beat = 0.5f + 0.5f * Mth.sin(secs * rate * 6.2831855f);
        float flicker = wounded > 0.7f ? 0.12f * Mth.sin(secs * 37f) : 0f; // failing-heart stutter
        float glow = 0.45f + 0.55f * beat + flicker;              // ~0.45..1.0 while alive
        if (healthFrac < 0.4f) {
            glow = Math.max(glow, 0.78f);                         // enraged — eyes burn bright
        }
        // Wind-up swell: the gathering attack overrides the calm heartbeat and flares toward the strike.
        float charge = entity.getCharge();
        if (charge > 0f) {
            glow = Math.max(glow, 0.6f + 0.4f * charge);
        }
        if (entity.isDeadOrDying()) {
            float k = Mth.clamp(1f - entity.deathTime / 20f, 0f, 1f);
            glow *= k * k;                                         // the light leaves the stone
        }
        return Mth.clamp(glow, 0f, 1f);
    }

    private static class Model extends DefaultedEntityGeoModel<StoneGolemEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "stone_golem"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }
    }

    /** Renders the golem's main-hand item at the {@code weapon_mount} bone, with the live grip transform. */
    private static class HeldHammerLayer
            extends BlockAndItemGeoLayer<StoneGolemEntity, Void, LivingEntityRenderState> {

        HeldHammerLayer(EntityRendererProvider.Context ctx,
                        GeoRenderer<StoneGolemEntity, Void, LivingEntityRenderState> renderer) {
            super(ctx, renderer);
        }

        @Override
        public void addRenderData(StoneGolemEntity golem, Void related,
                                  LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(CONTENTS, getRelevantBones(golem, related, state, partialTick));
        }

        @Override
        protected List<RenderData> getRelevantBones(StoneGolemEntity golem, Void related,
                                                    LivingEntityRenderState state, float partialTick) {
            ItemStack stack = golem.getMainHandItem();
            if (stack.isEmpty()) {
                return List.of();
            }
            ItemStackRenderState itemState = new ItemStackRenderState();
            this.itemModelResolver.updateForLiving(itemState, stack, CTX, golem);
            return List.of(RenderData.item("weapon_mount", CTX, itemState));
        }

        /** Apply the live-tuned grip transform to the hammer at the bone (trans → scale → Z·Y·X rot). */
        @Override
        protected void submitItemStackRender(PoseStack ps, GeoBone bone, ItemStackRenderState itemState,
                                             ItemDisplayContext ctx, LivingEntityRenderState renderState,
                                             SubmitNodeCollector collector, int packedLight) {
            ps.translate(StoneGolemHammerTuning.posX, StoneGolemHammerTuning.posY, StoneGolemHammerTuning.posZ);
            float s = StoneGolemHammerTuning.scale;
            ps.scale(s, s, s);
            ps.mulPose(Axis.ZP.rotationDegrees(StoneGolemHammerTuning.rotZ));
            ps.mulPose(Axis.YP.rotationDegrees(StoneGolemHammerTuning.rotY));
            ps.mulPose(Axis.XP.rotationDegrees(StoneGolemHammerTuning.rotX));
            super.submitItemStackRender(ps, bone, itemState, ctx, renderState, collector, packedLight);
        }
    }
}
