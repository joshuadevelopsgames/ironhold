package kingdom.smp.client;

import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Carry-weight readout beside container GUIs.
 *
 * <p><b>Stubbed out.</b> {@link #renderAfterScreen} is intentionally a no-op right now —
 * the weight overlay is not in use. The previous implementation (weight text +
 * encumbrance coloring, anchored to the bottom-right of the GUI) lives in git history;
 * restore it here when the weight UI is wanted again.
 */
public final class InventoryWeightOverlay {
    private InventoryWeightOverlay() {}

    /** No-op stub — the carry-weight overlay is currently disabled. */
    public static void renderAfterScreen(ScreenEvent.Render.Post event) {
        // intentionally empty
    }
}
