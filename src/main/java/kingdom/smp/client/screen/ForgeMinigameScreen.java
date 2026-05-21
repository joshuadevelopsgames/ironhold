package kingdom.smp.client.screen;

import kingdom.smp.blacksmithing.BlacksmithingMinigameManager;
import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.ForgeMinigameResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Blacksmithing forge minigame — opens when the server reports a sneak-click
 * on an anvil while holding reforgeable gear. A striker sweeps back and forth
 * across the anvil face; tap SPACE / click to bring the hammer down. Land the
 * strike inside the glowing sweet zone to advance the forge; nail the bright
 * inner core for a <i>perfect</i> strike that lifts the final quality. The
 * sweet zone jumps to a new spot after every strike.
 *
 * <p>Land {@value BlacksmithingMinigameManager#TARGET_STRIKES} good strikes
 * before you rack up too many flaws or the metal cools, and the gear is
 * forged. The number of <i>perfect</i> strikes decides how high the quality
 * climbs (4+ → Mint). Botch it and the metal drops a tier and takes damage;
 * give up via ESC/E and nothing happens.
 *
 * <p>Difficulty (sweet-zone width, striker speed, allowed flaws, cooling
 * time) is derived from the player's Blacksmithing rank ordinal sent by the
 * server. Movement is integrated at 20 Hz and rendered with partial-tick
 * interpolation so it stays smooth above 20 fps.
 */
public class ForgeMinigameScreen extends Screen {

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int BAR_WIDTH  = 200;
    private static final int BAR_HEIGHT = 22;
    private static final int FRAME_PAD  = 3;
    private static final int ITEM_SIZE  = 16;

    private static final int TARGET_STRIKES = BlacksmithingMinigameManager.TARGET_STRIKES;

    // ── Colors ───────────────────────────────────────────────────────────
    private static final int COL_BAR_BG_TOP  = 0xFF2A2018;
    private static final int COL_BAR_BG_BOT  = 0xFF0C0805;
    private static final int COL_FRAME_OUTER = 0xFF6B4A28;
    private static final int COL_FRAME_INNER = 0xFFC9A66B;
    private static final int COL_FRAME_SHADE = 0xFF3A2614;
    private static final int COL_ZONE        = 0x885FCF66;
    private static final int COL_ZONE_EDGE   = 0xFF7FE08A;
    private static final int COL_PERFECT     = 0xCCFFE070;
    private static final int COL_PERFECT_EDGE= 0xFFFFF4B0;
    private static final int COL_STRIKER     = 0xFFFFF4D0;
    private static final int COL_PIP_EMPTY   = 0xFF1A1410;
    private static final int COL_PIP_GOOD    = 0xFFC0C7CF;
    private static final int COL_PIP_PERFECT = 0xFFFFE070;
    private static final int COL_FLAW        = 0xFFE05050;
    private static final int COL_LABEL       = 0xFFFFE7B0;
    private static final int COL_HINT        = 0xFFCCCCCC;

    // ── Vanilla container-panel palette ──────────────────────────────────
    private static final int COL_PANEL_BG     = 0xFFC6C6C6;
    private static final int COL_PANEL_HI     = 0xFFFFFFFF;
    private static final int COL_PANEL_LO     = 0xFF555555;
    private static final int COL_PANEL_BORDER = 0xFF000000;

    // ── Server-provided ──────────────────────────────────────────────────
    private final ItemStack gearStack;

    // ── Difficulty (derived from rank) ───────────────────────────────────
    private final float sweetHalf;
    private final float perfectHalf;
    private final float strikerSpeed;
    private final int   maxFlaws;
    private final int   coolTicksMax;

    // ── Game state (current tick) ────────────────────────────────────────
    private float strikerX;
    private int   strikerDir = 1;
    private float zoneCenter;
    private int   goodStrikes;
    private int   perfectStrikes;
    private int   flaws;
    private int   coolTicks;

    // ── Previous-tick state for render interpolation ─────────────────────
    private float prevStrikerX;

    private int   ticks = 0;
    private int   flashTicks = 0;     // brief border flash after a strike
    private int   flashColor = 0;
    private boolean resolved = false;

    private final Random rng = new Random();
    private int barX, barY;

    public ForgeMinigameScreen(ItemStack gearPreview, int rankOrdinal) {
        super(Component.literal("Forge"));
        this.gearStack = gearPreview == null ? ItemStack.EMPTY : gearPreview;

        // Difficulty table: index by rank ordinal (NOVICE..MASTER = 0..4);
        // rankOrdinal < 0 means no Blacksmithing rank — the unranked profile.
        int r = rankOrdinal; // -1 unranked, 0..4 ranks
        this.sweetHalf    = pick(r, 12, 16, 20, 25, 31, 38);
        this.perfectHalf  = pick(r, 4,  6,  7,  9,  11, 14);
        this.strikerSpeed = pick(r, 8.0f, 6.9f, 6.0f, 5.1f, 4.2f, 3.5f);
        this.maxFlaws     = (int) pick(r, 1, 2, 3, 3, 4, 5);
        this.coolTicksMax = (int) pick(r, 120, 140, 160, 180, 200, 240);
        this.coolTicks    = this.coolTicksMax;
    }

    /** values[0] = unranked, values[1..5] = NOVICE..MASTER. */
    private static float pick(int rankOrdinal, float unranked, float novice, float apprentice,
                              float journeyman, float expert, float master) {
        return switch (rankOrdinal) {
            case 0 -> novice;
            case 1 -> apprentice;
            case 2 -> journeyman;
            case 3 -> expert;
            case 4 -> master;
            default -> unranked;
        };
    }

    @Override
    protected void init() {
        super.init();
        barX = (width - BAR_WIDTH) / 2;
        barY = height / 2 - BAR_HEIGHT / 2;
        strikerX = 0f;
        prevStrikerX = 0f;
        repositionZone();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // onClose treats it as an abandon (no penalty)
    }

    @Override
    public void tick() {
        super.tick();
        if (resolved) return;
        ticks++;
        if (flashTicks > 0) flashTicks--;

        prevStrikerX = strikerX;

        // ── Striker sweep ───────────────────────────────────────────────
        strikerX += strikerSpeed * strikerDir;
        if (strikerX <= 0f) {
            strikerX = 0f;
            strikerDir = 1;
        } else if (strikerX >= BAR_WIDTH) {
            strikerX = BAR_WIDTH;
            strikerDir = -1;
        }

        // ── Metal cools over time ───────────────────────────────────────
        if (--coolTicks <= 0) {
            finish(false);
        }
    }

    private void repositionZone() {
        float lo = sweetHalf + 4f;
        float hi = BAR_WIDTH - sweetHalf - 4f;
        if (hi <= lo) {
            zoneCenter = BAR_WIDTH / 2f;
        } else {
            zoneCenter = lo + rng.nextFloat() * (hi - lo);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private void strike() {
        if (resolved) return;
        float d = Math.abs(strikerX - zoneCenter);
        if (d <= perfectHalf) {
            perfectStrikes++;
            goodStrikes++;
            flash(0xFFFFF4B0);
            playStrike(1.5f);
        } else if (d <= sweetHalf) {
            goodStrikes++;
            flash(0xFF7FE08A);
            playStrike(1.1f);
        } else {
            flaws++;
            flash(0xFFE05050);
            playStrike(0.7f);
        }
        repositionZone();

        int successful = goodStrikes; // perfect strikes also increment goodStrikes
        if (successful >= TARGET_STRIKES) {
            finish(true);
        } else if (flaws > maxFlaws) {
            finish(false);
        }
    }

    private void flash(int color) {
        flashTicks = 3;
        flashColor = color;
    }

    private void playStrike(float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, pitch, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            strike();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            strike();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (!resolved) {
            // Abandon: sentinel perfect=-1 tells the server to apply no penalty.
            resolved = true;
            ClientPayloads.sendToServer(new ForgeMinigameResultPayload(false, -1, 0));
            Minecraft.getInstance().setScreen(null);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void finish(boolean success) {
        if (resolved) return;
        resolved = true;
        ClientPayloads.sendToServer(new ForgeMinigameResultPayload(success, perfectStrikes, goodStrikes));
        Minecraft.getInstance().setScreen(null);
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // keep the world visible — no fade
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
        float sX = Mth.lerp(partialTick, prevStrikerX, strikerX);

        int x0 = barX;
        int y0 = barY;
        int x1 = x0 + BAR_WIDTH;
        int y1 = y0 + BAR_HEIGHT;

        // Gauge metrics — shared by the panel bounds and the widgets below.
        int pipSize = 8, pipGap = 4;
        int pipY = y0 - FRAME_PAD - 6 - pipSize;
        int heatY = y1 + FRAME_PAD + 4, heatH = 5;
        int itemRight = x1 + FRAME_PAD + 6 + ITEM_SIZE;

        // Vanilla container-style panel hugging the whole forge gauge
        drawVanillaPanel(gfx,
                x0 - FRAME_PAD - 6, pipY - 6,
                itemRight + 6,      heatY + heatH + 3);

        // Wooden frame around the anvil face
        gfx.fill(x0 - FRAME_PAD, y0 - FRAME_PAD, x1 + FRAME_PAD, y1 + FRAME_PAD, COL_FRAME_OUTER);
        gfx.fill(x0 - 1, y0 - 1, x1 + 1, y0,     COL_FRAME_INNER);
        gfx.fill(x0 - 1, y0,     x0,     y1,     COL_FRAME_INNER);
        gfx.fill(x0,     y1,     x1 + 1, y1 + 1, COL_FRAME_SHADE);
        gfx.fill(x1,     y0,     x1 + 1, y1,     COL_FRAME_SHADE);

        // Anvil-face gradient
        gfx.fillGradient(x0, y0, x1, y1, COL_BAR_BG_TOP, COL_BAR_BG_BOT);

        // Sweet zone (+ inner perfect core)
        int zc = x0 + Math.round(zoneCenter);
        int zsL = x0 + Math.round(zoneCenter - sweetHalf);
        int zsR = x0 + Math.round(zoneCenter + sweetHalf);
        gfx.fill(zsL, y0, zsR, y1, COL_ZONE);
        gfx.fill(zsL, y0, zsL + 1, y1, COL_ZONE_EDGE);
        gfx.fill(zsR - 1, y0, zsR, y1, COL_ZONE_EDGE);
        int zpL = x0 + Math.round(zoneCenter - perfectHalf);
        int zpR = x0 + Math.round(zoneCenter + perfectHalf);
        gfx.fill(zpL, y0, zpR, y1, COL_PERFECT);
        gfx.fill(zpL, y0, zpL + 1, y1, COL_PERFECT_EDGE);
        gfx.fill(zpR - 1, y0, zpR, y1, COL_PERFECT_EDGE);

        // Striker (the moving hammer position)
        int strikerPx = x0 + Math.round(sX);
        gfx.fill(strikerPx - 1, y0 - 2, strikerPx + 1, y1 + 2, COL_STRIKER);

        // Strike flash on the frame
        if (flashTicks > 0) {
            int a = 0x60 + (int) (0x70 * (flashTicks / 3f));
            int glow = (a << 24) | (flashColor & 0xFFFFFF);
            int gx0 = x0 - FRAME_PAD - 1, gy0 = y0 - FRAME_PAD - 1;
            int gx1 = x1 + FRAME_PAD + 1, gy1 = y1 + FRAME_PAD + 1;
            gfx.fill(gx0, gy0, gx1, gy0 + 1, glow);
            gfx.fill(gx0, gy1 - 1, gx1, gy1, glow);
            gfx.fill(gx0, gy0, gx0 + 1, gy1, glow);
            gfx.fill(gx1 - 1, gy0, gx1, gy1, glow);
        }

        // ── Progress pips (good strikes toward the target) ───────────────
        int pipsW = TARGET_STRIKES * pipSize + (TARGET_STRIKES - 1) * pipGap;
        int pipX = x0 + (BAR_WIDTH - pipsW) / 2;
        for (int i = 0; i < TARGET_STRIKES; i++) {
            int px = pipX + i * (pipSize + pipGap);
            int col;
            if (i < perfectStrikes)      col = COL_PIP_PERFECT;
            else if (i < goodStrikes)    col = COL_PIP_GOOD;
            else                         col = COL_PIP_EMPTY;
            gfx.fill(px, pipY, px + pipSize, pipY + pipSize, 0xFF3A2614);
            gfx.fill(px + 1, pipY + 1, px + pipSize - 1, pipY + pipSize - 1, col);
        }

        // ── Heat / cooling meter (depletes; warns when low) ──────────────
        float heat = Math.max(0f, Math.min(1f, coolTicks / (float) coolTicksMax));
        gfx.fill(x0 - 1, heatY - 1, x1 + 1, heatY + heatH + 1, COL_FRAME_SHADE);
        gfx.fill(x0, heatY, x1, heatY + heatH, 0xFF120A06);
        int heatW = Math.round(BAR_WIDTH * heat);
        int heatColor = heat > 0.5f ? 0xFFFF8A2B : heat > 0.25f ? 0xFFE0531F : 0xFF8A2B12;
        if (heat < 0.25f) {
            float pulse = (float) (Math.sin((ticks + partialTick) * 0.4) * 0.5 + 0.5);
            int g = 0x40 + (int) (0xB0 * pulse);
            heatColor = (0xFF << 24) | (g << 16) | 0x1010;
        }
        gfx.fill(x0, heatY, x0 + heatW, heatY + heatH, heatColor);

        // ── Flaw indicator ───────────────────────────────────────────────
        String flawStr = "Flaws: " + flaws + " / " + maxFlaws;
        gfx.text(font, flawStr, x0, heatY + heatH + 4, flaws >= maxFlaws ? COL_FLAW : 0xFF999999, true);

        // ── Item being forged ────────────────────────────────────────────
        gfx.item(gearStack, x1 + FRAME_PAD + 6, y0 + (BAR_HEIGHT - ITEM_SIZE) / 2);

        // ── Title + hints ────────────────────────────────────────────────
        int centerX = x0 + BAR_WIDTH / 2;
        String title = "Strike the sweet spot!";
        gfx.text(font, title, centerX - font.width(title) / 2, pipY - font.lineHeight - 6, COL_LABEL, true);

        String hint1 = "[SPACE] or [Click] to strike — gold core = perfect";
        String hint2 = "[E] or [Esc] to step away";
        gfx.text(font, hint1, centerX - font.width(hint1) / 2, heatY + heatH + 4 + font.lineHeight + 4, COL_HINT, true);
        gfx.text(font, hint2, centerX - font.width(hint2) / 2, heatY + heatH + 4 + (font.lineHeight + 4) + font.lineHeight + 2, 0xFF777777, true);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
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
