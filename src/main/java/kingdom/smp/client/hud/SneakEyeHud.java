package kingdom.smp.client.hud;

import kingdom.smp.Ironhold;
import kingdom.smp.client.ClientSneakDetectionState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Skyrim-style sneak-eye HUD. Renders one of four frames driven by the
 * server-computed detection state (see {@code SneakDetectionTracker}).
 *
 * <p>Frame meaning:
 * <ul>
 *   <li><b>closed</b> — fully hidden, nobody is watching you.</li>
 *   <li><b>almost-closed</b> — someone is watching you, but they're not in
 *       your view (behind or beside you).</li>
 *   <li><b>start-closing</b> — someone in your line of sight is watching
 *       you (face to face / mutual visibility).</li>
 *   <li><b>full-open</b> — a hostile mob has you targeted.</li>
 * </ul>
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Hidden completely until the player has held crouch for
 *       {@link #HOLD_THRESHOLD_TICKS} (3 s at 20 tps) without interruption.</li>
 *   <li>Fades in to full opacity over {@link #FADE_TICKS} (0.5 s).</li>
 *   <li>Uncrouching at any point resets the timer.</li>
 * </ol>
 *
 * <p>All four frames render inside the same outer canvas — sized to the
 * full-open aspect ratio — so the eye appears to grow / shrink in place
 * rather than the entire icon jumping height between states.
 */
public final class SneakEyeHud {

    private static final int HOLD_THRESHOLD_TICKS = 60;
    private static final int FADE_TICKS = 10;
    /** Cap on final opacity so the eye reads as a glassy overlay, like the crosshair. */
    private static final float MAX_OPACITY = 0.7f;

    /** Base on-screen width in GUI px at scale=1.0. */
    private static final int BASE_WIDTH = 200;

    private static final Identifier TEX_CLOSED        = id("textures/gui/sneak_eye/eye-closed.png");
    private static final Identifier TEX_ALMOST_CLOSED = id("textures/gui/sneak_eye/eye-almost-closed.png");
    private static final Identifier TEX_START_CLOSING = id("textures/gui/sneak_eye/eye-start-closing.png");
    private static final Identifier TEX_FULL_OPEN     = id("textures/gui/sneak_eye/eye-full-open.png");

    private static final int W_CLOSED = 724,        H_CLOSED = 20;
    private static final int W_ALMOST = 755,        H_ALMOST = 118;
    private static final int W_START  = 758,        H_START  = 244;
    private static final int W_FULL   = 742,        H_FULL   = 464;

    /** Player.tickCount when the current crouch began. -1 = not crouching. */
    private static int crouchStartTick = -1;

    private SneakEyeHud() {}

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            crouchStartTick = -1;
            return;
        }
        // Don't reset the timer just because a screen is on top — sneak is held across
        // GUIs by SneakHoldInputHandler, so the eye should pick right back up when the
        // screen closes. Just skip the actual render.
        if (mc.screen != null) {
            return;
        }
        if (!player.isCrouching()) {
            crouchStartTick = -1;
            return;
        }

        SneakEyeConfig.loadIfNeeded();

        if (crouchStartTick == -1) {
            crouchStartTick = player.tickCount;
        }

        float partial = delta.getGameTimeDeltaPartialTick(false);
        float heldTicks = (player.tickCount - crouchStartTick) + partial;
        if (heldTicks < HOLD_THRESHOLD_TICKS) return;

        float alpha = Math.min(1f, (heldTicks - HOLD_THRESHOLD_TICKS) / FADE_TICKS) * MAX_OPACITY;
        int alpha255 = Math.round(alpha * 255f);
        int color = (alpha255 << 24) | 0x00FFFFFF;

        byte state = ClientSneakDetectionState.get();
        Identifier tex;
        int texW, texH;
        switch (state) {
            case 0 -> { tex = TEX_CLOSED;        texW = W_CLOSED; texH = H_CLOSED; }
            case 1 -> { tex = TEX_ALMOST_CLOSED; texW = W_ALMOST; texH = H_ALMOST; }
            case 2 -> { tex = TEX_START_CLOSING; texW = W_START;  texH = H_START;  }
            case 3 -> { tex = TEX_FULL_OPEN;     texW = W_FULL;   texH = H_FULL;   }
            default -> { return; }
        }

        int drawW   = Math.max(1, Math.round(BASE_WIDTH * SneakEyeConfig.scale));
        int canvasH = Math.max(1, Math.round(drawW * (H_FULL / (float) W_FULL)));
        int frameH  = Math.max(1, Math.round(drawW * (texH / (float) texW)));

        int cx = gfx.guiWidth() / 2 + SneakEyeConfig.offsetX;
        int cy = gfx.guiHeight() / 2 + SneakEyeConfig.offsetY;
        int x = cx - drawW / 2;
        int y = cy - canvasH / 2 + (canvasH - frameH) / 2;

        gfx.blit(RenderPipelines.CROSSHAIR, tex,
            x, y, 0f, 0f, drawW, frameH, texW, texH, texW, texH, color);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, path);
    }
}
