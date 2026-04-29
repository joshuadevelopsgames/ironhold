package kingdom.smp.client.screen;

import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;

/**
 * UV constants for the King's Console sprite atlas.
 * <p>
 * Source of truth is {@code art/console/atlas-spec.md}. If you move a
 * slot, update both that file and this one — the artist relies on the
 * spec, the screen relies on this class.
 * <p>
 * Atlas image: {@code assets/ironhold/textures/gui/console/kings_console.png}
 * — drop the painted PNG there and flip {@link KingsConsoleScreen#USE_ATLAS}
 * to {@code true}.
 */
public final class ConsoleAtlas {
    private ConsoleAtlas() {}

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/console/kings_console.png");

    /** Standalone hand-painted wax seal texture (not in the atlas — kept full-res). */
    public static final Identifier WAX_SEAL_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/console/wax_seal.png");
    public static final int WAX_SEAL_NATIVE = 64;

    public static final int ATLAS_W = 256;
    public static final int ATLAS_H = 256;

    /** Single sprite slot — UV origin and size on the atlas. */
    public record Slot(int u, int v, int w, int h) {}

    // ── Row A — 16×16 icons ────────────────────────────────────────────────
    public static final Slot ICON_COIN          = new Slot(  0, 0, 16, 16);
    public static final Slot ICON_GOLD_INGOT    = new Slot( 16, 0, 16, 16);
    public static final Slot ICON_EMERALD       = new Slot( 32, 0, 16, 16);
    public static final Slot ICON_LAND          = new Slot( 48, 0, 16, 16);
    public static final Slot ICON_CROWN         = new Slot( 64, 0, 16, 16);
    public static final Slot ICON_GATE          = new Slot( 80, 0, 16, 16);
    public static final Slot ICON_PORTAL        = new Slot( 96, 0, 16, 16);
    /** Reserved (was unrest skull, removed from layout). */
    public static final Slot ICON_RESERVED_A    = new Slot(112, 0, 16, 16);
    public static final Slot ICON_WAX_SEAL      = new Slot(128, 0, 16, 16);
    public static final Slot ICON_DECREE_STAMP  = new Slot(144, 0, 16, 16);

    // ── Row B — 12×12 stepper button states ────────────────────────────────
    public static final Slot STEPPER_IDLE       = new Slot( 0, 16, 12, 12);
    public static final Slot STEPPER_HOVER      = new Slot(12, 16, 12, 12);
    public static final Slot STEPPER_PRESSED    = new Slot(24, 16, 12, 12);
    public static final Slot STEPPER_DISABLED   = new Slot(36, 16, 12, 12);

    // ── Row C — 60×16 toggle button frames ─────────────────────────────────
    public static final Slot TOGGLE_OFF_IDLE    = new Slot(  0, 28, 60, 16);
    public static final Slot TOGGLE_OFF_HOVER   = new Slot( 60, 28, 60, 16);
    public static final Slot TOGGLE_ON_IDLE     = new Slot(120, 28, 60, 16);
    public static final Slot TOGGLE_ON_HOVER    = new Slot(180, 28, 60, 16);

    // ── Row D — 9-slice chrome (the painter fills only the top-left
    //          24×24 of each 32×32 slot; the rest is reserved for variants) ─
    public static final Slot OUTER_FRAME        = new Slot(  0, 44, 24, 24);
    public static final Slot INNER_BOX          = new Slot( 32, 44, 24, 24);
    /** 9-slice spans 24×24 of the 64-wide slot; the painter doubles the
     *  middle strip to verify horizontal tile-ability in-place. */
    public static final Slot TITLE_BAR          = new Slot( 64, 44, 24, 24);
    public static final Slot PARCHMENT          = new Slot(128, 44, 24, 24);

    // ── Row E — Banner ribbons (96×20 on a 96×24 slot) ─────────────────────
    public static final Slot BANNER_RIBBON      = new Slot(  0, 76, 96, 20);
    public static final Slot BURDEN_BAR_BG      = new Slot( 96, 76, 96, 20);

    // ── Row F — Decorative (variable size) ─────────────────────────────────
    public static final Slot WAX_SEAL_DECREE    = new Slot(  0, 100,  32, 32);
    /** Title plate is 128×24 on a 128×32 slot. */
    public static final Slot KING_CONSOLE_PLATE = new Slot( 32, 100, 128, 24);

    // ── 9-slice tile size (corners) ────────────────────────────────────────
    /**
     * Most chrome 9-slices use 8-pixel corners. {@link #OUTER_FRAME} uses
     * 12-pixel corners since the outer frame is heavier — check the spec
     * before assuming.
     */
    public static final int CORNER_8  = 8;
    public static final int CORNER_12 = 12;
}
