package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.ZombieRenderState;

/** Carries filcher-specific flags needed by {@link FilcherModel#setupAnim}. */
public class FilcherRenderState extends ZombieRenderState {
    /** True while the filcher is in stalk mode (targeting a player, empty-handed). */
    public boolean isStalking;
    /** True while the filcher is showing off its loot to peers. */
    public boolean isShowingOff;
    /** True while this filcher is the pack king (wearing a crown). */
    public boolean isKing;
}
