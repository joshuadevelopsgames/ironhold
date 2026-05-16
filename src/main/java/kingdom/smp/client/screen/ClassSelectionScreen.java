package kingdom.smp.client.screen;

import com.mojang.authlib.GameProfile;
import kingdom.smp.Ironhold;
import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.ClassChoicePayload;
import kingdom.smp.net.ClientRpgData;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Class selection / promotion screen. Tier 1 paints four banner cards with
 * live 3D character previews (Knight, Ranger, Wizard, Cleric). Higher tiers
 * fall back to the same banner skeleton with a generic locked or unlocked
 * banner texture until per-class art is produced.
 *
 * Render order per card (back to front):
 *   1. Banner backdrop PNG
 *   2. Class-color glow PNG (only when hovered/selected)
 *   3. 3D entity (RemotePlayer with class loadout)
 *   4. Animated 2D particle overlay
 *   5. Class title text
 */
public class ClassSelectionScreen extends Screen {

    private static final int CARD_GAP = 12;
    private static final int MARGIN = 24;
    private static final int CARD_HEIGHT = 368;
    private static final int CARD_WIDTH = 220;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 34;
    private static final int MAX_COLS = 4;

    // Source texture dimensions for the four banner PNGs (uniform).
    private static final int BANNER_TEX_W = 216;
    private static final int BANNER_TEX_H = 361;
    // Glow placeholder dimensions (regenerated to match card+padding).
    private static final int GLOW_TEX_W = 244;
    private static final int GLOW_TEX_H = 392;

    private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(
        Ironhold.MODID, "textures/gui/class_select/banner_glow.png");
    private static final Identifier LOCKED_BANNER = Identifier.fromNamespaceAndPath(
        Ironhold.MODID, "textures/gui/class_select/banner_locked.png");

    private int selected = -1;
    private double scrollY = 0;

    private final @Nullable Screen returnTo;
    private List<ClassEntry> entries = List.of();
    private final Map<PlayerClass, RemotePlayer> previews = new EnumMap<>(PlayerClass.class);

    public ClassSelectionScreen() {
        this(null);
    }

    public ClassSelectionScreen(@Nullable Screen returnTo) {
        super(Component.literal("Choose Your Path"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        super.init();
        entries = buildEntries();
        selected = -1;
        scrollY = 0;
        buildPreviews();

        addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            if (selected >= 0 && selected < entries.size() && entries.get(selected).unlocked) {
                ClientPayloads.sendToServer(new ClassChoicePayload(entries.get(selected).pc.ordinal()));
                onClose();
            }
        }).bounds(width / 2 - 50, height - FOOTER_HEIGHT + 4, 100, 20).build());
    }

    private void buildPreviews() {
        previews.clear();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        GameProfile profile = mc.player.getGameProfile();
        for (ClassEntry e : entries) {
            if (!e.unlocked) continue;
            ClassGear.Loadout gear = ClassGear.forClass(e.pc);
            RemotePlayer preview = new RemotePlayer(level, profile);
            preview.setCustomNameVisible(false);
            preview.setSilent(true);
            preview.setItemSlot(EquipmentSlot.HEAD,    gear.head());
            preview.setItemSlot(EquipmentSlot.CHEST,   gear.chest());
            preview.setItemSlot(EquipmentSlot.LEGS,    gear.legs());
            preview.setItemSlot(EquipmentSlot.FEET,    gear.feet());
            preview.setItemSlot(EquipmentSlot.MAINHAND, gear.main());
            preview.setItemSlot(EquipmentSlot.OFFHAND,  gear.off());
            previews.put(e.pc, preview);
        }
    }

    // ── Entry building — only classes on the player's path ───────────────────

    private List<ClassEntry> buildEntries() {
        PlayerClass current = ClientRpgData.playerClass();
        Set<PlayerClass> completed = ClientRpgData.completedClassSet();
        int targetTier = current.tier() + 1;

        Set<PlayerClass> withCurrent = new HashSet<>(completed);
        withCurrent.add(current);

        List<ClassEntry> result = new ArrayList<>();
        for (PlayerClass pc : PlayerClass.values()) {
            if (pc.tier() != targetTier) continue;
            if (!isOnPlayerPath(pc, withCurrent)) continue;

            boolean unlocked = pc.canUnlock(withCurrent);
            String reason = unlocked ? null : buildLockedReason(pc, withCurrent);
            result.add(new ClassEntry(pc, unlocked, reason));
        }
        return result;
    }

    private static boolean isOnPlayerPath(PlayerClass candidate, Set<PlayerClass> completedWithCurrent) {
        List<PlayerClass> prereqs = candidate.prerequisites();
        if (prereqs.isEmpty()) return true;
        for (PlayerClass req : prereqs) {
            if (completedWithCurrent.contains(req)) return true;
        }
        return false;
    }

    private static String buildLockedReason(PlayerClass pc, Set<PlayerClass> completed) {
        List<PlayerClass> prereqs = pc.prerequisites();
        List<String> missing = new ArrayList<>();
        for (PlayerClass req : prereqs) {
            if (!completed.contains(req)) {
                missing.add(req.id());
            }
        }
        if (missing.isEmpty()) return "Locked";
        return "Also requires: " + String.join(" + ", missing);
    }

    // ── Grid layout ──────────────────────────────────────────────────────────

    private int cols() {
        return Math.min(entries.size(), MAX_COLS);
    }

    private int rows() {
        int c = cols();
        if (c == 0) return 0;
        return (entries.size() + c - 1) / c;
    }

    private int cardWidth() {
        int c = cols();
        if (c == 0) return CARD_WIDTH;
        int wBudget = (width - MARGIN * 2 - CARD_GAP * (c - 1)) / c;
        int hBudget = Math.max(80, height - HEADER_HEIGHT - FOOTER_HEIGHT - 16);
        int wFromHeight = (hBudget * BANNER_TEX_W) / BANNER_TEX_H;
        return Math.min(CARD_WIDTH, Math.min(wBudget, wFromHeight));
    }

    private int cardHeight() {
        return (cardWidth() * BANNER_TEX_H) / BANNER_TEX_W;
    }

    private int itemsInRow(int row) {
        int c = cols();
        int remaining = entries.size() - row * c;
        return Math.min(remaining, c);
    }

    private int cardX(int col, int rowCount) {
        int cw = cardWidth();
        int totalWidth = rowCount * cw + (rowCount - 1) * CARD_GAP;
        int rowStartX = (width - totalWidth) / 2;
        return rowStartX + col * (cw + CARD_GAP);
    }

    private int cardY(int row) {
        return HEADER_HEIGHT + row * (cardHeight() + CARD_GAP) - (int) scrollY;
    }

    private int totalGridHeight() {
        int r = rows();
        return r * cardHeight() + (r - 1) * CARD_GAP;
    }

    private int viewportHeight() {
        return height - HEADER_HEIGHT - FOOTER_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, totalGridHeight() - viewportHeight());
    }

    // ── Scrolling ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY -= scrollY * 20;
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScroll()));
        return true;
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Full-screen dim behind everything else this stratum draws
        gfx.fill(0, 0, width, height, 0xCC0A0A14);

        PlayerClass current = ClientRpgData.playerClass();
        int targetTier = current.tier() + 1;

        String titleText = current == PlayerClass.PEASANT
            ? "Choose Your Path"
            : "Promotion — Tier " + targetTier;
        gfx.centeredText(font, Component.literal(titleText)
                        .withStyle(Style.EMPTY.withBold(true)),
                width / 2, 12, 0xFFFFDD55);

        int promoLevel = RpgProgression.promotionLevelForTier(current.tier());
        String subtitle = current == PlayerClass.PEASANT
            ? "Select a class to begin your journey."
            : "You have mastered " + current.id() + " at Level " + promoLevel + ".";
        gfx.centeredText(font, subtitle, width / 2, 26, 0xFFAAAAAA);

        gfx.enableScissor(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT);

        int cw = cardWidth();
        int c = cols();
        ClassEntry hovered = null;

        for (int i = 0; i < entries.size(); i++) {
            int row = i / c;
            int col = i % c;
            int rowItems = itemsInRow(row);
            int cx = cardX(col, rowItems);
            int cy = cardY(row);

            int ch = cardHeight();
            if (cy + ch < HEADER_HEIGHT || cy > height - FOOTER_HEIGHT) continue;

            boolean isHovered = mouseX >= cx && mouseX < cx + cw
                    && mouseY >= cy && mouseY < cy + ch
                    && mouseY >= HEADER_HEIGHT && mouseY < height - FOOTER_HEIGHT;
            if (isHovered) hovered = entries.get(i);
            drawClassCard(gfx, entries.get(i), cx, cy, cw, isHovered, i == selected,
                          (float) mouseX, (float) mouseY);
        }

        gfx.disableScissor();

        if (maxScroll() > 0) {
            drawScrollbar(gfx);
        }

        if (hovered != null) {
            queueTooltip(gfx, hovered, mouseX, mouseY);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx) {
        int trackX = width - 6;
        int trackTop = HEADER_HEIGHT;
        int trackH = viewportHeight();

        gfx.fill(trackX, trackTop, trackX + 4, trackTop + trackH, 0x44FFFFFF);

        int totalH = totalGridHeight();
        int thumbH = Math.max(10, trackH * viewportHeight() / totalH);
        int thumbY = trackTop + (int) ((trackH - thumbH) * scrollY / maxScroll());
        gfx.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xAAFFFFFF);
    }

    // ── Click ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            if (my >= HEADER_HEIGHT && my < height - FOOTER_HEIGHT) {
                int cw = cardWidth();
                int c = cols();
                for (int i = 0; i < entries.size(); i++) {
                    int row = i / c;
                    int col = i % c;
                    int rowItems = itemsInRow(row);
                    int cx = cardX(col, rowItems);
                    int cy = cardY(row);
                    if (mx >= cx && mx < cx + cw
                            && my >= cy && my < cy + cardHeight()) {
                        if (entries.get(i).unlocked) {
                            selected = i;
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    @Override
    public void onClose() {
        previews.clear();
        Minecraft.getInstance().setScreen(returnTo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Card drawing ─────────────────────────────────────────────────────────

    private void drawClassCard(GuiGraphicsExtractor gfx, ClassEntry entry, int x, int y,
                               int cw, boolean hovered, boolean selected,
                               float mouseX, float mouseY) {
        PlayerClass pc = entry.pc;
        int color = entry.unlocked ? classColor(pc) : 0xFF666666;

        int ch = cardHeight();

        // 1. Banner backdrop — scale source to (cw × ch), aspect preserved by cardHeight()
        Identifier banner = entry.unlocked ? bannerFor(pc) : LOCKED_BANNER;
        gfx.blit(RenderPipelines.GUI_TEXTURED, banner,
                 x, y, 0f, 0f, cw, ch,
                 BANNER_TEX_W, BANNER_TEX_H, BANNER_TEX_W, BANNER_TEX_H);

        // 2. Class-color glow when interactive — scale glow PNG to card+padding
        if (entry.unlocked && (hovered || selected)) {
            int alpha = selected ? 0xFF : 0xAA;
            int tint = (alpha << 24) | (color & 0x00FFFFFF);
            int gw = cw + 24;
            int gh = ch + 24;
            gfx.blit(RenderPipelines.GUI_TEXTURED, GLOW_TEXTURE,
                     x - 12, y - 12, 0f, 0f, gw, gh,
                     GLOW_TEX_W, GLOW_TEX_H, GLOW_TEX_W, GLOW_TEX_H, tint);
        }

        // 3. 3D character preview
        RemotePlayer preview = previews.get(pc);
        if (entry.unlocked && preview != null) {
            int cx = x + cw / 2;
            int scale = Math.max(35, Math.min(70, ch / 6));
            int floor = y + ch - 8;                       // feet near bottom of banner
            // Box top: full entity height (1.8) + headroom (0.15) so the head crown
            // doesn't clip when the avatar tilts to follow the cursor. Name tag still
            // renders ~0.5 blocks above the head, comfortably outside this box, so the
            // scissor in renderEntityInInventory still hides it.
            int top = floor - (int) (scale * 1.95f);
            // Only the hovered avatar tracks the mouse; others face forward.
            float emx = hovered ? mouseX : cx;
            float emy = hovered ? mouseY : (top + floor) * 0.5f;
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                gfx, cx - 52, top, cx + 52, floor,
                scale, 0.0625F, emx, emy, preview);
        }

        // 4. Hover sparkles — fantasy-magical accent that only fires when this card is hovered
        if (entry.unlocked && hovered) drawHoverSparkles(gfx, pc, x, y, cw, ch, color);

        // 5. Lock badge — only thing rendered on top of the banner art now
        if (!entry.unlocked) {
            String locked = "❌ LOCKED";
            int lw = font.width(locked);
            gfx.text(font, locked, x + cw / 2 - lw / 2, y + 50, 0xFFFF5555, false);
        }
    }

    private static Identifier bannerFor(PlayerClass pc) {
        String family = bannerFamily(pc);
        if (family == null) return LOCKED_BANNER;
        return Identifier.fromNamespaceAndPath(Ironhold.MODID,
            "textures/gui/class_select/banner_" + family + ".png");
    }

    /** Maps each class to one of the four iconic banners, or null for tiers
     *  without dedicated art (those fall back to the locked banner). */
    private static @Nullable String bannerFamily(PlayerClass pc) {
        return switch (pc) {
            case SQUIRE, KNIGHT          -> "knight";
            case ARCHER, RANGER          -> "ranger";
            case MAGE_APPRENTICE, WIZARD -> "wizard";
            case MEDIC, CLERIC           -> "cleric";
            default                      -> null;
        };
    }

    // ── Hover sparkles ───────────────────────────────────────────────────────
    // Fantasy-magical accent: small 4-point stars rise from the bottom of the
    // banner, twinkle in size, fade in/out, and tint to the class color. Only
    // drawn while the card is hovered.

    private static final Identifier SPARKLE_TEX = Identifier.fromNamespaceAndPath(
        Ironhold.MODID, "textures/gui/class_select/particles/sparkle.png");
    private static final int SPARKLE_COUNT = 22;

    /** Splittable-mix-style hash → uniform float in [0,1) from an int seed. */
    private static float hash01(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return ((h & 0x00FFFFFF) / (float) 0x01000000);
    }

    private void drawHoverSparkles(GuiGraphicsExtractor gfx, PlayerClass pc,
                                   int x, int y, int cw, int ch, int classColor) {
        long now = System.currentTimeMillis();
        int travel = ch - 30;
        if (travel <= 0) return;

        for (int i = 0; i < SPARKLE_COUNT; i++) {
            // Per-particle randomized parameters — different hash seeds avoid
            // any visible diagonal/grid pattern.
            float rX        = hash01(i * 7 + 11);
            float rLife     = hash01(i * 13 + 23);     // lifetime variance
            float rPhase    = hash01(i * 17 + 41);     // start phase
            float rSwayAmp  = hash01(i * 19 + 53);     // sway amplitude
            float rSwayFreq = hash01(i * 23 + 61);     // sway frequency
            float rPulse    = hash01(i * 29 + 71);     // size pulse phase

            long lifetime = 1200L + (long) (rLife * 1800L);          // 1.2s..3.0s
            long phaseOff = (long) (rPhase * lifetime);
            float t = ((now + phaseOff) % lifetime) / (float) lifetime;  // 0..1

            float baseX = 6f + rX * (cw - 12f);
            float swayAmp = 2f + rSwayAmp * 5f;
            float swayPhase = (now / (40f + rSwayFreq * 80f)) + rPhase * 6.28f;
            float sway = (float) Math.sin(swayPhase) * swayAmp;
            int px = x + (int) (baseX + sway);

            // Eased rise so particles don't all move at constant speed.
            float eased = t * t * (3f - 2f * t);
            int py = y + ch - 12 - (int) (travel * eased);

            float fade;
            if (t < 0.2f) fade = t / 0.2f;
            else if (t > 0.8f) fade = (1f - t) / 0.2f;
            else fade = 1f;

            float pulse = 0.55f + 0.45f * (float) Math.sin(now / 110f + rPulse * 6.28f);
            int size = Math.max(3, (int) (6 + rLife * 6f) * (int) Math.max(1, pulse * 2) / 2);
            size = Math.max(3, Math.min(13, size));

            int alpha = Math.max(0, Math.min(255, (int) (235 * fade)));
            int tint = (alpha << 24) | (classColor & 0x00FFFFFF);

            gfx.blit(RenderPipelines.GUI_TEXTURED, SPARKLE_TEX,
                     px - size / 2, py - size / 2,
                     0f, 0f, size, size,
                     16, 16, 16, 16, tint);
        }
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    private void queueTooltip(GuiGraphicsExtractor gfx, ClassEntry entry, int mouseX, int mouseY) {
        PlayerClass pc = entry.pc;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(pc.id())
            .withStyle(Style.EMPTY.withBold(true).withColor(classColor(pc))));
        lines.add(Component.literal(pc.role() + " — Tier " + pc.tier())
            .withStyle(Style.EMPTY.withColor(0xFF888888)));

        if (entry.unlocked) {
            lines.add(Component.literal("HP " + pc.statHealth()
                + "  ATK " + pc.statAttackDamage()
                + "  DEF " + pc.statDefense())
                .withStyle(Style.EMPTY.withColor(0xFF55FF55)));
            lines.add(Component.literal("MANA " + pc.statMana()
                + "  SPD " + pc.statSpeed()
                + "  LUCK " + pc.statLuck())
                .withStyle(Style.EMPTY.withColor(0xFF55CCFF)));
            lines.add(Component.literal("ATK SPD " + pc.statAttackSpeed()
                + "  CARRY " + pc.maxCarryWeight())
                .withStyle(Style.EMPTY.withColor(0xFFFFDD55)));
            List<PlayerClass> prereqs = pc.prerequisites();
            if (!prereqs.isEmpty()) {
                StringBuilder sb = new StringBuilder("✓ From: ");
                for (int i = 0; i < prereqs.size(); i++) {
                    if (i > 0) sb.append(" + ");
                    sb.append(prereqs.get(i).id());
                }
                lines.add(Component.literal(sb.toString())
                    .withStyle(Style.EMPTY.withColor(0xFF55AA55)));
            }
        } else if (entry.lockReason != null) {
            lines.add(Component.literal(entry.lockReason)
                .withStyle(Style.EMPTY.withColor(0xFFAA6666)));
        }

        gfx.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
    }

    // ── Class color helper ───────────────────────────────────────────────────

    private static int classColor(PlayerClass pc) {
        String family = bannerFamily(pc);
        if (family != null) {
            return switch (family) {
                case "knight" -> 0xFFCC4444;
                case "ranger" -> 0xFF55AA55;
                case "wizard" -> 0xFF7755FF;
                case "cleric" -> 0xFFFFDD55;
                default       -> 0xFFFFFFFF;
            };
        }
        return pc == PlayerClass.PEASANT ? 0xFF888888 : 0xFFFFFFFF;
    }

    private record ClassEntry(PlayerClass pc, boolean unlocked, @Nullable String lockReason) {}
}
