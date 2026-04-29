package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.KingEndermanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class KingEndermanRenderer extends MobRenderer<KingEndermanEntity, KingEndermanRenderState, KingEndermanModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/king_enderman.png");
    private static final Identifier GLOW_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/king_enderman_glow.png");
    private static final RenderType EYES = RenderTypes.eyes(GLOW_TEXTURE);

    private static final float BOSS_SCALE = 2.6F;

    public KingEndermanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KingEndermanModel(ctx.bakeLayer(KingEndermanModel.LAYER_LOCATION)), 1.4F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(KingEndermanRenderState state) {
        return TEXTURE;
    }

    @Override
    public KingEndermanRenderState createRenderState() {
        return new KingEndermanRenderState();
    }

    @Override
    public void extractRenderState(KingEndermanEntity entity, KingEndermanRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        float maxHp = entity.getMaxHealth();
        state.healthFraction = maxHp > 0 ? entity.getHealth() / maxHp : 1.0F;
        // Drive the model's enraged animation off the entity's authoritative phase flag,
        // not raw HP — phase 3 starts at 33%, not 50%.
        state.isEnraged = entity.isEnraged();
    }

    @Override
    protected void scale(KingEndermanRenderState state, PoseStack poseStack) {
        poseStack.scale(BOSS_SCALE, BOSS_SCALE, BOSS_SCALE);
    }

    /** Inflate the cull box well beyond the 3.0×5.5 hitbox — the model at 2.6× scale
     *  reaches roughly 18 blocks tall with arms swinging ~4 blocks past the hitbox edge.
     *  Without this, the boss disappears whenever its small hitbox exits the frustum. */
    @Override
    protected AABB getBoundingBoxForCulling(KingEndermanEntity entity) {
        return super.getBoundingBoxForCulling(entity).inflate(4.0, 8.0, 4.0);
    }
}
