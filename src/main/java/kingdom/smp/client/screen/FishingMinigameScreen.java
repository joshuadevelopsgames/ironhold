package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.FishingMinigameResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Bite minigame — opens when the server reports a fish biting on this
 * player's bobber. Hold SPACE or right-mouse to raise the hook; release to
 * let it fall under gravity. Keep the hook overlapping the fish to drain
 * the fish's stamina meter. Drain it to 0 to catch; bail out via ESC/E
 * (the fish escapes — no catch).
 *
 * <p>The displayed item is the actual item the server pre-rolled for this
 * bite, so the player sees what they're fighting for.
 *
 * <p>Meter semantics: starts FULL (1.0) and counts down when the hook is
 * off the fish — so the player can see how much time they have left
 * before the fish escapes. Hook on fish refills the meter (capped at
 * full). The catch itself is awarded once you've kept the hook on the
 * fish for {@link #CATCH_TICKS} cumulative ticks. Empty meter = fish
 * escapes (lose).
 *
 * <p>Movement is integrated at the 20 Hz tick rate but rendered with
 * partial-tick interpolation so it doesn't feel stuttery at higher
 * framerates.
 */
public class FishingMinigameScreen extends Screen {

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int BAR_HEIGHT  = 168;
    private static final int BAR_WIDTH   = 40;
    private static final int METER_WIDTH = 12;
    private static final int METER_GAP   = 8;
    private static final int FRAME_PAD   = 3;
    private static final int FISH_SIZE   = 16;

    // ── Tuning (per-tick at 20 Hz) ───────────────────────────────────────
    private static final float HOOK_THRUST  = -1.4f;
    private static final float HOOK_GRAVITY = 0.85f;
    private static final float HOOK_DAMP    = 0.85f;
    /** How fast the meter refills while the hook is on the fish (capped at 1.0). */
    private static final float METER_FILL   = 0.012f;
    /** How fast the meter drains while the hook is off the fish — the "countdown". */
    private static final float METER_DRAIN  = 0.014f;
    /** Cumulative ticks the hook must be on the fish for the catch to land. */
    private static final int   CATCH_TICKS  = 80; // 4 seconds of on-fish time

    // ── Colors ───────────────────────────────────────────────────────────
    private static final int COL_BAR_BG_TOP  = 0xFF1B3A5C;
    private static final int COL_BAR_BG_BOT  = 0xFF050B18;
    private static final int COL_FRAME_OUTER = 0xFF6B4A28;
    private static final int COL_FRAME_INNER = 0xFFC9A66B;
    private static final int COL_FRAME_SHADE = 0xFF3A2614;
    private static final int COL_HOOK_ZONE   = 0x66FFE099;
    private static final int COL_HOOK_LINE   = 0xFFFFE7A8;
    private static final int COL_METER_BG    = 0xFF0E1318;
    private static final int COL_METER_FRAME = 0xFF5A4022;
    /** High meter = you're doing well (default, green). */
    private static final int COL_METER_HIGH  = 0xFF55DD66;
    private static final int COL_METER_MID   = 0xFFE7C347;
    private static final int COL_METER_LOW   = 0xFFE05050;
    private static final int COL_METER_TICK  = 0x66FFFFFF;
    // ── Vanilla container-panel palette ──────────────────────────────────
    private static final int COL_PANEL_BG     = 0xFFC6C6C6;
    private static final int COL_PANEL_HI     = 0xFFFFFFFF;
    private static final int COL_PANEL_LO     = 0xFF555555;
    private static final int COL_PANEL_BORDER = 0xFF000000;
    // ── Open-air label text (drawn over the world, with drop shadow) ─────
    private static final int COL_LABEL  = 0xFFFFE7B0;
    private static final int COL_HINT   = 0xFFE6E6E6;
    private static final int COL_HINT_2 = 0xFFB0B0B0;
    private static final int COL_HINT_3 = 0xFF9A9A9A;

    // ── Tutorial / label strings ─────────────────────────────────────────
    private static final String TITLE = "Reel it in!";
    private static final String HINT1 = "Hold [SPACE] or [Right Click] to rise";
    private static final String HINT2 = "Keep the hook on the fish";
    private static final String HINT3 = "[E] or [Esc] to give up";

    // ── Server-provided params ───────────────────────────────────────────
    private final int hookZoneHeight;
    private final int motionPattern;
    private final ItemStack fishStack;

    // ── Game state (current tick) ────────────────────────────────────────
    private float fishY;
    private float fishTarget;
    private int   fishRetargetIn = 0;
    private float hookY;
    private float hookVy;
    /** Starts full (1.0). Drains when hook is off-fish ("how much time you have left"). */
    private float meter = 1.0f;
    /** Cumulative on-fish ticks. Catch when this hits {@link #CATCH_TICKS}. */
    private int   catchProgress = 0;

    // ── State at end of previous tick — for render interpolation ─────────
    private float prevFishY;
    private float prevHookY;
    private float prevMeter;

    private int   ticks = 0;


    // ── Input state ──────────────────────────────────────────────────────
    private boolean spaceDown;
    private boolean mouseDown;

    private boolean resolved = false;

    private int barX, barY, meterX;

    public FishingMinigameScreen(int hookZoneHeight, int motionPattern, ItemStack catchPreview) {
        super(Component.literal("Fishing"));
        this.hookZoneHeight = Math.max(8, hookZoneHeight);
        this.motionPattern  = Math.max(0, Math.min(2, motionPattern));
        ItemStack preview = catchPreview == null ? ItemStack.EMPTY : catchPreview;
        if (preview.isEmpty()) {
            preview = switch (this.motionPattern) {
                case 1  -> new ItemStack(Items.SALMON);
                case 2  -> new ItemStack(Items.PUFFERFISH);
                default -> new ItemStack(Items.COD);
            };
        }
        this.fishStack = preview;
    }

    @Override
    protected void init() {
        super.init();
        int barMeterW = BAR_WIDTH + METER_GAP + METER_WIDTH;
        // Center the bar+meter widget on screen; tutorial text floats to its left.
        barX = (width - barMeterW) / 2;
        barY = (height - BAR_HEIGHT) / 2;
        meterX = barX + BAR_WIDTH + METER_GAP;

        fishY = BAR_HEIGHT * 0.5f;
        prevFishY = fishY;
        fishTarget = fishY;

        hookY = BAR_HEIGHT - hookZoneHeight / 2f - 4f;
        prevHookY = hookY;

        prevMeter = meter;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;  // but onClose treats it as a loss
    }

    @Override
    public void tick() {
        super.tick();
        if (resolved) return;
        ticks++;

        prevFishY = fishY;
        prevHookY = hookY;
        prevMeter = meter;

        // ── Fish motion ─────────────────────────────────────────────────
        switch (motionPattern) {
            case 1 -> tickFishJumpy();
            case 2 -> tickFishThrashing();
            default -> tickFishCalm();
        }

        // ── Hook physics ────────────────────────────────────────────────
        if (spaceDown || mouseDown) {
            hookVy += HOOK_THRUST;
        }
        hookVy += HOOK_GRAVITY;
        hookVy *= HOOK_DAMP;
        hookY  += hookVy;
        float half = hookZoneHeight / 2f;
        if (hookY < half) {
            hookY = half;
            hookVy = Math.max(0f, hookVy);
        } else if (hookY > BAR_HEIGHT - half) {
            hookY = BAR_HEIGHT - half;
            hookVy = Math.min(0f, hookVy);
        }

        // ── Meter (your time left) + catch progress ─────────────────────
        float zoneTop = hookY - half;
        float zoneBot = hookY + half;
        boolean onFish = fishY >= zoneTop && fishY <= zoneBot;
        if (onFish) {
            meter = Math.min(1f, meter + METER_FILL);
            catchProgress++;
        } else {
            meter -= METER_DRAIN;
        }

        if (catchProgress >= CATCH_TICKS) resolve(true);
        else if (meter <= 0f) resolve(false);
    }

    private void tickFishCalm() {
        if (fishRetargetIn-- <= 0) {
            fishTarget = randInBar();
            fishRetargetIn = 30 + randInt(20);
        }
        fishY += (fishTarget - fishY) * 0.05f;
    }

    private void tickFishJumpy() {
        if (fishRetargetIn-- <= 0) {
            fishTarget = randInBar();
            fishRetargetIn = 18 + randInt(24);
        }
        fishY += (fishTarget - fishY) * 0.12f;
    }

    private void tickFishThrashing() {
        if (fishRetargetIn-- <= 0) {
            fishTarget = randInBar();
            fishRetargetIn = 6 + randInt(10);
        }
        fishY += (fishTarget - fishY) * 0.30f;
    }

    private float randInBar() {
        return 10f + nextFloat() * (BAR_HEIGHT - 20f);
    }

    private float nextFloat() {
        var lvl = Minecraft.getInstance().level;
        return lvl != null ? lvl.getRandom().nextFloat() : (float) Math.random();
    }

    private int randInt(int bound) {
        var lvl = Minecraft.getInstance().level;
        return lvl != null ? lvl.getRandom().nextInt(bound) : (int) (Math.random() * bound);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) { spaceDown = true; return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE) { spaceDown = false; return true; }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mouseDown = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mouseDown = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        if (!resolved) resolve(false);
        else Minecraft.getInstance().setScreen(null);
    }

    private void resolve(boolean won) {
        if (resolved) return;
        resolved = true;
        ClientPayloads.sendToServer(new FishingMinigameResultPayload(won));
        Minecraft.getInstance().setScreen(null);
    }

    // ── Render ────────────────────────────────────────────────────────────

    /** Keep the world fully visible behind the minigame — no fade. */
    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // intentionally empty
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor gfx) {
        // intentionally empty
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor gfx) {
        // intentionally empty
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        float fY = Mth.lerp(partialTick, prevFishY, fishY);
        float hY = Mth.lerp(partialTick, prevHookY, hookY);
        float m  = Mth.lerp(partialTick, prevMeter, meter);

        int x0 = barX;
        int y0 = barY;
        int x1 = x0 + BAR_WIDTH;
        int y1 = y0 + BAR_HEIGHT;

        // Vanilla container-style panel — tight around just the bar + meter
        int panelX0 = x0 - 8;
        int panelY0 = y0 - 8;
        int panelX1 = meterX + METER_WIDTH + 8;
        int panelY1 = y1 + 8;
        drawVanillaPanel(gfx, panelX0, panelY0, panelX1, panelY1);

        // Wooden frame around the water bar
        gfx.fill(x0 - FRAME_PAD, y0 - FRAME_PAD, x1 + FRAME_PAD, y1 + FRAME_PAD, COL_FRAME_OUTER);
        gfx.fill(x0 - 1, y0 - 1, x1 + 1, y0,     COL_FRAME_INNER);
        gfx.fill(x0 - 1, y0,     x0,     y1,     COL_FRAME_INNER);
        gfx.fill(x0,     y1,     x1 + 1, y1 + 1, COL_FRAME_SHADE);
        gfx.fill(x1,     y0,     x1 + 1, y1,     COL_FRAME_SHADE);

        // Water gradient
        gfx.fillGradient(x0, y0, x1, y1, COL_BAR_BG_TOP, COL_BAR_BG_BOT);

        // Depth-cue waterlines
        for (int i = 1; i <= 3; i++) {
            int wy = y0 + BAR_HEIGHT * i / 4;
            gfx.fill(x0 + 2, wy, x1 - 2, wy + 1, 0x22FFFFFF);
        }

        // Hook zone
        float half = hookZoneHeight / 2f;
        int zoneTop = y0 + Math.round(hY - half);
        int zoneBot = y0 + Math.round(hY + half);
        gfx.fill(x0 + 1, zoneTop, x1 - 1, zoneBot, COL_HOOK_ZONE);
        gfx.fill(x0, zoneTop - 1, x1, zoneTop,     COL_HOOK_LINE);
        gfx.fill(x0, zoneBot,     x1, zoneBot + 1, COL_HOOK_LINE);

        // Fish item
        int fishCx = x0 + BAR_WIDTH / 2;
        int fishCy = y0 + Math.round(fY);
        gfx.item(fishStack, fishCx - FISH_SIZE / 2, fishCy - FISH_SIZE / 2);

        // Catch meter (fish stamina — drain to win)
        int mx0 = meterX;
        int mx1 = mx0 + METER_WIDTH;
        gfx.fill(mx0 - 2, y0 - 2, mx1 + 2, y1 + 2, COL_METER_FRAME);
        gfx.fill(mx0, y0, mx1, y1, COL_METER_BG);
        int fillPx = Math.max(0, Math.min(BAR_HEIGHT, Math.round(BAR_HEIGHT * m)));
        int meterColor = m > 0.50f ? COL_METER_HIGH
                       : m > 0.25f ? COL_METER_MID
                       :              COL_METER_LOW;
        gfx.fill(mx0, y1 - fillPx, mx1, y1, meterColor);
        for (int i = 1; i <= 3; i++) {
            int ty = y0 + BAR_HEIGHT * i / 4;
            gfx.fill(mx0, ty, mx1, ty + 1, COL_METER_TICK);
        }

        // Warning gold glow when the meter is about to run out
        if (m < 0.25f) {
            float pulse = (float) (Math.sin((ticks + partialTick) * 0.35) * 0.5 + 0.5);
            int alpha = 0x40 + (int) (0x90 * pulse);
            int glow = (alpha << 24) | 0xFFE070;
            int gx0 = x0 - FRAME_PAD - 1;
            int gy0 = y0 - FRAME_PAD - 1;
            int gx1 = mx1 + 3;
            int gy1 = y1 + FRAME_PAD + 1;
            gfx.fill(gx0, gy0, gx1, gy0 + 1, glow);
            gfx.fill(gx0, gy1 - 1, gx1, gy1, glow);
            gfx.fill(gx0, gy0, gx0 + 1, gy1, glow);
            gfx.fill(gx1 - 1, gy0, gx1, gy1, glow);
        }

        // Title — centered over the bar+meter, above the panel in open air
        int centerX = x0 + (BAR_WIDTH + METER_GAP + METER_WIDTH) / 2;
        int titleW = font.width(TITLE);
        gfx.text(font, TITLE, centerX - titleW / 2, y0 - font.lineHeight - 12, COL_LABEL, true);

        // Tutorial hints — left of the bar in open air, right-aligned against the
        // panel and wrapped to whatever horizontal space is left, vertically
        // centered on the bar.
        int textRight = panelX0 - 8;
        int maxTextW = Math.max(48, textRight - 8);
        int lh = font.lineHeight + 4;

        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<Integer> lineColors = new java.util.ArrayList<>();
        String[] hints = { HINT1, HINT2, HINT3 };
        int[] hintColors = { COL_HINT, COL_HINT_2, COL_HINT_3 };
        for (int i = 0; i < hints.length; i++) {
            for (String ln : wrapText(hints[i], maxTextW)) {
                lines.add(ln);
                lineColors.add(hintColors[i]);
            }
        }
        int blockH = (lines.size() - 1) * lh + font.lineHeight;
        int ty = y0 + (BAR_HEIGHT - blockH) / 2;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            gfx.text(font, ln, textRight - font.width(ln), ty + i * lh, lineColors.get(i), true);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Greedy word-wrap of a hint string to a pixel width using the active font. */
    private java.util.List<String> wrapText(String text, int maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && font.width(trial) > maxWidth) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** Draws the classic beveled gray Minecraft container panel. */
    private void drawVanillaPanel(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, COL_PANEL_BORDER); // outer black border
        gfx.fill(x0, y0, x1, y1, COL_PANEL_BG);                     // base fill
        gfx.fill(x0, y0, x1, y0 + 2, COL_PANEL_HI);                 // top highlight
        gfx.fill(x0, y0, x0 + 2, y1, COL_PANEL_HI);                 // left highlight
        gfx.fill(x0, y1 - 2, x1, y1, COL_PANEL_LO);                 // bottom shadow
        gfx.fill(x1 - 2, y0, x1, y1, COL_PANEL_LO);                 // right shadow
    }
}
