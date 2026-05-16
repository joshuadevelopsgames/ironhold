package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.client.KangarudeSkins;
import kingdom.smp.client.VillagerDialogueCache;
import kingdom.smp.entity.KangarudeEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Client renderer for {@link KangarudeEntity}. Uses the vanilla player model
 * with the live skin for the configured username (see {@link KangarudeSkinCache}),
 * and overlays the active dialogue line via {@link VillagerDialogueCache}.
 */
public class KangarudeRenderer
    extends HumanoidMobRenderer<KangarudeEntity, AvatarRenderState, PlayerModel> {

    private static final Identifier SKIN_TEXTURE = KangarudeSkins.KANGARUDE_TEXTURE;
    private static final Identifier KANGABRINE_TEXTURE = KangarudeSkins.KANGABRINE_TEXTURE;

    // Transient state for the current pass, set in extractRenderState and read in submit.
    private int lastEntityId;
    private boolean lastKangabrineMode;

    public KangarudeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(KangarudeEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // PlayerModel reads state.skin to pick texture + slim/wide arms; without
        // this it falls back to the default skin instead of our bundled PNG.
        boolean kangabrine = entity.isKangabrineMode();
        state.skin = kangabrine ? KangarudeSkins.KANGABRINE_SKIN : KangarudeSkins.KANGARUDE_SKIN;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        lastEntityId = entity.getId();
        lastKangabrineMode = kangabrine;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return lastKangabrineMode ? KANGABRINE_TEXTURE : SKIN_TEXTURE;
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        VillagerDialogueCache.DialogueEntry dialogue = VillagerDialogueCache.getDialogue(lastEntityId);
        if (dialogue == null) return;

        Component text = Component.literal("§e" + dialogue.dialogue());
        poseStack.pushPose();
        poseStack.translate(0.0, 0.3, 0.0);
        collector.submitNameTag(
            poseStack, Vec3.ZERO,
            0x80000000,    // semi-transparent black bg
            text, true,
            0xF000F0,      // full brightness
            64.0 * 64.0,   // render distance
            camera);
        poseStack.popPose();
    }
}
