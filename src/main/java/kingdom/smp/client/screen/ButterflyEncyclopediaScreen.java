package kingdom.smp.client.screen;

import kingdom.smp.ModAttachments;
import kingdom.smp.entity.ButterflyDex;
import kingdom.smp.entity.ButterflySpecies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Butterfly Encyclopedia — a paginated two-page field guide. Each page shows two species
 * (left page / right page); undiscovered species are masked as a black silhouette with a
 * "?" and "???" text until the player catches one. Clean-room reimplementation of the
 * Luminous Butterflies encyclopedia: data-driven over {@link ButterflySpecies} rather than
 * twelve hand-built pages, reading discovery from the synced {@link ButterflyDex}.
 *
 * <p>Visuals: a generated parchment book texture, crisp 64px specimen portraits, and a
 * rarity-coloured frame per entry (Epic = purple).
 */
public class ButterflyEncyclopediaScreen extends Screen {

    private static final int BOOK_W = 340;
    private static final int BOOK_H = 196;
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int PORTRAIT = 64;

    // The book background texture includes a 4px cover border beyond the page area.
    private static final Identifier BOOK_TEX =
        Identifier.fromNamespaceAndPath("ironhold", "textures/gui/encyclopedia_book.png");
    private static final int BOOK_TEX_W = 348;
    private static final int BOOK_TEX_H = 204;

    // Ink palette (parchment-readable).
    private static final int COL_INK        = 0xFF3A2A12;
    private static final int COL_INK_MUTED  = 0xFF7A6A4A;
    private static final int COL_MAT        = 0xFFF4ECD2; // light mat behind a portrait
    private static final int COL_FRAME_UNK  = 0xFF8A7A55; // muted frame for undiscovered
    private static final int COL_SILHOUETTE = 0xFF1B1410; // near-black tint for unknown

    private final int totalPages;
    private int page;

    private @Nullable Button prevButton;
    private @Nullable Button nextButton;

    public ButterflyEncyclopediaScreen() {
        super(Component.literal("Butterfly Encyclopedia"));
        int species = ButterflySpecies.values().length;
        this.totalPages = (species + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE;
    }

    /** Open the encyclopedia (client-side only). */
    public static void open() {
        Minecraft.getInstance().setScreen(new ButterflyEncyclopediaScreen());
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - BOOK_W) / 2;
        int top = (height - BOOK_H) / 2;
        int by = top + BOOK_H - 26;

        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
            .bounds(left + 16, by, 20, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
            .bounds(left + BOOK_W - 36, by, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(left + BOOK_W / 2 - 30, by, 60, 20).build());
        updateButtons();
    }

    private void turn(int delta) {
        page = Math.max(0, Math.min(totalPages - 1, page + delta));
        updateButtons();
    }

    private void updateButtons() {
        if (prevButton != null) prevButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < totalPages - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        var player = Minecraft.getInstance().player;
        ButterflyDex dex = player == null ? ButterflyDex.EMPTY : player.getData(ModAttachments.BUTTERFLY_DEX.get());

        int left = (width - BOOK_W) / 2;
        int top = (height - BOOK_H) / 2;
        int spineX = left + BOOK_W / 2;

        // Book background (texture includes the 4px violet cover border).
        gfx.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEX, left - 4, top - 4, 0f, 0f,
            BOOK_TEX_W, BOOK_TEX_H, BOOK_TEX_W, BOOK_TEX_H, BOOK_TEX_W, BOOK_TEX_H);

        // Header: title + collection progress.
        String title = "Butterfly Encyclopedia";
        gfx.text(font, Component.literal(title).withStyle(Style.EMPTY.withBold(true)),
            spineX - font.width(title) / 2, top + 13, COL_INK, false);
        String progress = dex.count() + " / " + ButterflySpecies.values().length + " discovered";
        gfx.text(font, progress, spineX - font.width(progress) / 2, top + 25, COL_INK_MUTED, false);

        // Two entries: left page, right page.
        int contentTop = top + 40;
        int leftCenter = left + 86;
        int rightCenter = left + 254;
        int firstIndex = page * ENTRIES_PER_PAGE;
        drawEntry(gfx, leftCenter, contentTop, firstIndex, dex);
        drawEntry(gfx, rightCenter, contentTop, firstIndex + 1, dex);

        // Footer: page indicator.
        String pageLabel = "Page " + (page + 1) + " / " + totalPages;
        gfx.text(font, pageLabel, spineX - font.width(pageLabel) / 2, top + BOOK_H - 34, COL_INK_MUTED, false);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Draw one species entry centred on {@code centerX}; masked if undiscovered. */
    private void drawEntry(GuiGraphicsExtractor gfx, int centerX, int top, int index, ButterflyDex dex) {
        ButterflySpecies[] all = ButterflySpecies.values();
        if (index >= all.length) {
            // Spare slot on the final page — mirror the reference's "Coming Soon".
            String soon = "Coming Soon";
            gfx.text(font, Component.literal(soon).withStyle(Style.EMPTY.withItalic(true)),
                centerX - font.width(soon) / 2, top + PORTRAIT / 2 + 10, COL_INK_MUTED, false);
            return;
        }
        ButterflySpecies species = all[index];
        boolean known = dex.has(species);

        int px = centerX - PORTRAIT / 2;
        int py = top;

        // Rarity-coloured frame + light mat behind the portrait.
        int frameColor = known ? (species.rarity().color() | 0xFF000000) : COL_FRAME_UNK;
        gfx.fill(px - 4, py - 4, px + PORTRAIT + 4, py + PORTRAIT + 4, frameColor);
        gfx.fill(px - 2, py - 2, px + PORTRAIT + 2, py + PORTRAIT + 2, COL_MAT);

        Identifier portrait = Identifier.fromNamespaceAndPath(
            "ironhold", "textures/gui/butterfly_portrait/" + species.id() + ".png");
        if (known) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, portrait, px, py, 0f, 0f,
                PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
        } else {
            gfx.blit(RenderPipelines.GUI_TEXTURED, portrait, px, py, 0f, 0f,
                PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT, COL_SILHOUETTE);
            gfx.text(font, Component.literal("?").withStyle(Style.EMPTY.withBold(true)),
                centerX - font.width("?") / 2, py + PORTRAIT / 2 - 4, 0xFFEFE6CB, true);
        }

        int ty = py + PORTRAIT + 8;

        // Name.
        String name = known ? species.displayName() : "???";
        gfx.text(font, Component.literal(name).withStyle(Style.EMPTY.withBold(true)),
            centerX - font.width(name) / 2, ty, COL_INK, false);
        ty += 13;

        // Rarity (coloured when known).
        if (known) {
            ButterflySpecies.Rarity r = species.rarity();
            field(gfx, centerX, ty, "Rarity: ", r.label(), r.color() | 0xFF000000);
        } else {
            field(gfx, centerX, ty, "Rarity: ", "???", COL_INK_MUTED);
        }
        ty += 11;

        field(gfx, centerX, ty, "Location: ", known ? species.location() : "???", COL_INK);
        ty += 11;

        field(gfx, centerX, ty, "Likes: ", known ? species.likes() : "???", COL_INK);
    }

    /** Draw a "Label: value" line centred on {@code centerX}, label muted, value coloured. */
    private void field(GuiGraphicsExtractor gfx, int centerX, int y, String label, String value, int valueColor) {
        int total = font.width(label) + font.width(value);
        int x = centerX - total / 2;
        gfx.text(font, label, x, y, COL_INK_MUTED, false);
        gfx.text(font, value, x + font.width(label), y, valueColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
