package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.client.VillagerDialogueCache;
import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.entity.VillagerProfession;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for Kingdom Villagers using the vanilla villager model.
 * Uses VillagerRenderState directly (not a subclass) so that VillagerProfessionLayer's
 * generic bounds are satisfied by VillagerModel's type parameters.
 */
public class KingdomVillagerRenderer
    extends AgeableMobRenderer<KingdomVillagerEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier VILLAGER_BASE_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    // Transient state for the current render pass — set in extractRenderState, read in submit
    private int lastEntityId;
    private boolean lastIsTalker;

    public KingdomVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            0.5F);
        this.addLayer(new VillagerProfessionLayer<>(
            this, ctx.getResourceManager(), "villager",
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER))));
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return VILLAGER_BASE_TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(KingdomVillagerEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        VillagerProfession prof = entity.getProfession();
        lastEntityId = entity.getId();
        lastIsTalker = prof.canTalk();

        // Initialize villagerData (our entity isn't a vanilla Villager so super doesn't set it)
        ResourceKey<net.minecraft.world.entity.npc.villager.VillagerProfession> mappedProf = mapProfession(prof);
        if (state.villagerData == null) {
            var registryAccess = entity.registryAccess();
            var typeRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_TYPE);
            var profRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
            state.villagerData = new VillagerData(
                typeRegistry.getOrThrow(net.minecraft.world.entity.npc.villager.VillagerType.PLAINS),
                profRegistry.getOrThrow(mappedProf),
                1);
        } else {
            state.villagerData = state.villagerData.withProfession(
                entity.registryAccess(), mappedProf);
        }
    }

    @Override
    public void submit(VillagerRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        // Check dialogue cache for this entity
        VillagerDialogueCache.DialogueEntry dialogue = VillagerDialogueCache.getDialogue(lastEntityId);
        if (dialogue != null) {
            Component text = Component.literal(
                (lastIsTalker ? "\u00A7b" : "\u00A7e") + dialogue.dialogue());
            poseStack.pushPose();
            poseStack.translate(0.0, 0.3, 0.0);
            collector.submitNameTag(
                poseStack, Vec3.ZERO,
                0x80000000,   // semi-transparent black bg
                text, true,
                0xF000F0,     // full brightness
                64.0 * 64.0,  // render distance
                camera);
            poseStack.popPose();
            return; // dialogue takes priority over emotes
        }

        // Check emote cache
        VillagerDialogueCache.EmoteEntry emote = VillagerDialogueCache.getEmote(lastEntityId);
        if (emote != null) {
            String icon = VillagerDialogueCache.emoteIcon(
                VillagerDialogueCache.emoteFromOrdinal(emote.emoteOrdinal()));
            Component emoteText = Component.literal(icon);
            poseStack.pushPose();
            poseStack.translate(0.0, 0.3, 0.0);
            collector.submitNameTag(
                poseStack, Vec3.ZERO,
                0x40000000,   // lighter bg for emotes
                emoteText, true,
                0xF000F0,
                48.0 * 48.0,
                camera);
            poseStack.popPose();
        }
    }

    private static ResourceKey<net.minecraft.world.entity.npc.villager.VillagerProfession> mapProfession(
            VillagerProfession prof) {
        return switch (prof) {
            case BLACKSMITH -> net.minecraft.world.entity.npc.villager.VillagerProfession.ARMORER;
            case FARMER     -> net.minecraft.world.entity.npc.villager.VillagerProfession.FARMER;
            case GUARD      -> net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH;
            case MERCHANT   -> net.minecraft.world.entity.npc.villager.VillagerProfession.MASON;
            case ALCHEMIST  -> net.minecraft.world.entity.npc.villager.VillagerProfession.CLERIC;
            case WIZARD     -> net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN;
            case PRIEST     -> net.minecraft.world.entity.npc.villager.VillagerProfession.CLERIC;
            case LIBRARIAN  -> net.minecraft.world.entity.npc.villager.VillagerProfession.CARTOGRAPHER;
            case BARD       -> net.minecraft.world.entity.npc.villager.VillagerProfession.NITWIT;
            // ENDER renders via a dedicated EnderVillagerEntity + EnderVillagerRenderer; this branch
            // is unreachable but the switch must be exhaustive over all enum values.
            case ENDER      -> net.minecraft.world.entity.npc.villager.VillagerProfession.NONE;
        };
    }
}
