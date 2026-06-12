package kingdom.smp.client;

import kingdom.smp.Ironhold;
import kingdom.smp.client.screen.VillagerDialogueScreen;
import kingdom.smp.client.screen.WardenDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Takes over nametag submission for every Ironhold entity (all our NPCs):
 *
 * <ul>
 *   <li><b>No see-through:</b> vanilla submits nametags with the see-through
 *       flag, so names glow through walls. We cancel that submission and
 *       re-submit depth-tested, so a name is only visible when the NPC is.</li>
 *   <li><b>Hidden during dialogue:</b> while a dialogue screen is open for an
 *       NPC, that NPC's nametag is suppressed entirely — both the copy the GUI
 *       portrait would render inside the dialogue box and the floating tag on
 *       the world entity behind it.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Ironhold.MODID, value = Dist.CLIENT)
public final class NpcNameTagHandler {
    private NpcNameTagHandler() {}

    @SubscribeEvent
    public static void onNameTagRender(RenderNameTagEvent.DoRender event) {
        EntityRenderState state = event.getEntityRenderState();
        EntityType<?> type = state.entityType;
        if (type == null || !Ironhold.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
            return;
        }

        event.setCanceled(true);
        if (isDialoguePartner(type)) {
            return; // in dialogue with this NPC — no tag anywhere
        }

        // Re-submit exactly what vanilla would have, minus the see-through flag.
        // (The int param is the text's Y offset in name-tag space — vanilla
        // passes 0 for the name line; the background colour is derived from
        // the player's text-background opacity option inside the submit.)
        event.getSubmitNodeCollector().submitNameTag(
            event.getPoseStack(),
            state.nameTagAttachment != null ? state.nameTagAttachment : Vec3.ZERO,
            0,
            event.getContent(),
            false,                       // depth-tested: never visible through walls
            state.lightCoords,
            state.distanceToCameraSq,
            event.getCameraRenderState());
    }

    /** True if a dialogue screen is currently open for an NPC of this type. */
    private static boolean isDialoguePartner(EntityType<?> type) {
        Minecraft mc = Minecraft.getInstance();
        int dialogueEntityId;
        if (mc.screen instanceof WardenDialogueScreen warden) {
            dialogueEntityId = warden.entityId();
        } else if (mc.screen instanceof VillagerDialogueScreen villager) {
            dialogueEntityId = villager.entityId();
        } else {
            return false;
        }
        Entity partner = mc.level != null ? mc.level.getEntity(dialogueEntityId) : null;
        return partner != null && partner.getType() == type;
    }
}
