package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.VillagerRenderState;

/**
 * Extended render state for Kingdom Villagers, carrying dialogue and emote data
 * from the entity to the render layers.
 */
public class KingdomVillagerRenderState extends VillagerRenderState {
    public String activeDialogue;
    public String activeEmoteIcon;
    public String professionId;
    public boolean isTalker;
}
