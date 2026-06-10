package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import kingdom.smp.entity.MagicMirrorEntity;
import kingdom.smp.entity.MirrorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the magic mirror like a {@link MirrorRenderer} (frame + live reflection) and then draws
 * glowing text on the glass, anchored to the player's reflection:
 * <ul>
 *   <li>the player's username, always shown just above their head;</li>
 *   <li>their stats, off to the player's right — revealed one line at a time (each fades in, holds
 *       ~3s, then fades out before the next), cycling continuously.</li>
 * </ul>
 *
 * <p>Every line is kept fully inside the pane (auto-shrunk + clamped) and glows via full-bright glyphs
 * with a uniform 1px halo (the Font outline) — not a scaled copy, so there is no doubling.
 *
 * <p>{@code shouldRender} (inherited) skips the pane during reflection captures, so the text is drawn
 * only in the main pass — it overlays the front of the glass and never appears inside the reflection.
 */
public class MagicMirrorRenderer extends MirrorRenderer {
    // Per-line cycle: ramp the glyphs in, hold them, then ramp out before advancing to the next line.
    private static final long FADE_IN_MS = 600L;
    private static final long HOLD_MS = 3000L;
    private static final long FADE_OUT_MS = 600L;
    private static final long LINE_CYCLE_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    /** Text size: ~0.18 block per line at full size (shrinks automatically to fit narrow mirrors). */
    private static final float TEXT_SCALE = 0.02F;
    /** Text fill colour for every line. Warm yellow; swap to red {@code 0xFF3B3B} if preferred. */
    private static final int TEXT_RGB = 0xFFD21E;
    /** Crisp black 1px border around the glyphs (the Font outline, no doubling). */
    private static final int BORDER_RGB = 0x000000;
    /** Keep text this far (blocks) inside the pane edge so it never touches the frame. */
    private static final float EDGE_MARGIN = 0.12F;
    /** Nudge the text just off the glass so it sits in front of the reflection. */
    private static final float FRONT_LIFT = 0.02F;
    /** Where the stat line's left edge sits, in blocks to the player's right of their reflection. */
    private static final float STAT_RIGHT_OFFSET = 0.45F;

    private final Font font;

    public MagicMirrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
    }

    @Override
    public MirrorRenderState createRenderState() {
        return new MagicMirrorRenderState();
    }

    @Override
    public void extractRenderState(MirrorEntity entity, MirrorRenderState stateIn, float partialTicks) {
        super.extractRenderState(entity, stateIn, partialTicks);
        MagicMirrorRenderState state = (MagicMirrorRenderState) stateIn;
        MagicMirrorEntity mirror = (MagicMirrorEntity) entity;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            state.name = null;
            state.statLines = List.of();
            return;
        }

        long now = Util.getMillis();
        if (mirror.clientFadeStartMillis == 0L) {
            mirror.clientFadeStartMillis = now;
        }
        long elapsed = now - mirror.clientFadeStartMillis;

        // Username: always visible, with a gentle one-time fade-in on first sight.
        state.name = buildName(player);
        state.nameAlpha = Mth.clamp(elapsed / (float) FADE_IN_MS, 0.0F, 1.0F);

        // Stats: cycle one line at a time (fade in -> hold -> fade out -> next).
        state.statLines = buildStatLines(player);
        int n = state.statLines.size();
        if (n > 0) {
            long t = elapsed % (LINE_CYCLE_MS * n);
            state.activeLine = (int) (t / LINE_CYCLE_MS);
            long within = t % LINE_CYCLE_MS;
            float a;
            if (within < FADE_IN_MS) {
                a = within / (float) FADE_IN_MS;
            } else if (within < FADE_IN_MS + HOLD_MS) {
                a = 1.0F;
            } else {
                a = 1.0F - (within - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
            }
            state.statAlpha = Mth.clamp(a, 0.0F, 1.0F);
        }

        // Anchor on the player's reflection (see submit() for the frame). +X runs along the wall
        // (counter-clockwise), +Y is world up; horizontal clamping is per-line (it depends on width).
        Vec3 center = mirror.position();
        var dir = mirror.getDirection();
        var ccw = dir.getCounterClockWise();
        Vec3 paneRight = new Vec3(ccw.getStepX(), ccw.getStepY(), ccw.getStepZ());
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 rel = player.position().subtract(center);
        float halfH = mirror.getHeightBlocks() / 2.0F;
        state.halfW = mirror.getWidthBlocks() / 2.0F;
        state.anchorX = (float) rel.dot(paneRight);
        float headY = (float) rel.dot(up) + player.getBbHeight();

        float lineH = font.lineHeight * TEXT_SCALE;
        state.nameCenterY = clampRow(headY + 0.12F + lineH / 2.0F, lineH, halfH); // just above the head
        state.statCenterY = clampRow(headY - 0.20F, lineH, halfH);                // beside the head/shoulder
    }

    @Override
    public void submit(MirrorRenderState stateIn, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(stateIn, poseStack, collector, camera); // frame + live reflection
        MagicMirrorRenderState state = (MagicMirrorRenderState) stateIn;
        if (state.name == null) {
            return;
        }

        float frontZ = MirrorEntity.DEPTH / 2.0F + FRONT_LIFT;
        poseStack.pushPose();
        // Vanilla flat-sign text frame: +Z is the pane's outward normal, +X runs along the wall
        // (counter-clockwise), +Y is world up. This is the orientation that reads correctly and faces
        // the viewer (the TEXT pipeline back-face culls).
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.direction.toYRot()));

        // Persistent glowing username, centred above the head.
        drawGlowLine(poseStack, collector, state.name, state.anchorX, state.nameCenterY, state.halfW,
            frontZ, state.nameAlpha, false);

        // One stat line at a time, off to the player's right, fading in and out.
        if (!state.statLines.isEmpty()) {
            int idx = Mth.clamp(state.activeLine, 0, state.statLines.size() - 1);
            drawGlowLine(poseStack, collector, state.statLines.get(idx), state.anchorX + STAT_RIGHT_OFFSET,
                state.statCenterY, state.halfW, frontZ, state.statAlpha, true);
        }

        poseStack.popPose();
    }

    /**
     * Draws one line: full-bright glyphs with a uniform black 1px border (the Font outline).
     * The line is uniformly shrunk and clamped so it stays inside the pane. {@code leftAlign} puts the
     * left edge at {@code anchorX} (used for the right-side stats); otherwise it is centred on it.
     */
    private void drawGlowLine(PoseStack ps, SubmitNodeCollector col, FormattedCharSequence line,
                              float anchorX, float centerY, float halfW, float frontZ, float alpha,
                              boolean leftAlign) {
        if (alpha <= 0.0F) {
            return;
        }
        int widthPx = font.width(line);
        if (widthPx <= 0) {
            return;
        }
        float avail = 2.0F * halfW - 2.0F * EDGE_MARGIN;
        if (avail <= 0.0F) {
            return;
        }
        float scale = Math.min(TEXT_SCALE, avail / widthPx); // shrink to fit width
        float worldW = widthPx * scale;
        float leftMin = -halfW + EDGE_MARGIN;
        float leftMax = halfW - EDGE_MARGIN - worldW;
        float desiredLeft = leftAlign ? anchorX : anchorX - worldW / 2.0F;
        float leftX = leftMax <= leftMin ? leftMin : Mth.clamp(desiredLeft, leftMin, leftMax);

        int a = Mth.ceil(alpha * 255.0F);
        int color = (a << 24) | 0x00FFFFFF; // alpha only; each glyph keeps its style RGB (TEXT_RGB)
        int outline = (a << 24) | BORDER_RGB;
        ps.pushPose();
        ps.translate(leftX, centerY, frontZ);
        ps.scale(scale, -scale, scale);
        col.submitText(ps, 0, -font.lineHeight / 2.0F, line, false, Font.DisplayMode.POLYGON_OFFSET,
            LightCoordsUtil.FULL_BRIGHT, color, 0, outline);
        ps.popPose();
    }

    /** Clamp a row's centre Y so the whole line stays inside the pane (with the edge margin). */
    private static float clampRow(float centerY, float lineH, float halfH) {
        float lo = -halfH + EDGE_MARGIN + lineH / 2.0F;
        float hi = halfH - EDGE_MARGIN - lineH / 2.0F;
        return hi <= lo ? 0.0F : Mth.clamp(centerY, lo, hi);
    }

    private static FormattedCharSequence buildName(LocalPlayer p) {
        return p.getDisplayName().copy().withStyle(s -> s.withColor(TEXT_RGB).withBold(true)).getVisualOrderText();
    }

    private static List<FormattedCharSequence> buildStatLines(LocalPlayer p) {
        List<Component> lines = new ArrayList<>();
        lines.add(stat("Health", String.format("%.0f / %.0f", p.getHealth(), p.getMaxHealth())));
        lines.add(stat("Armor", Integer.toString(p.getArmorValue())));
        lines.add(stat("Attack", String.format("%.1f", p.getAttributeValue(Attributes.ATTACK_DAMAGE))));
        lines.add(stat("Level", Integer.toString(p.experienceLevel)));
        lines.add(stat("Food", Integer.toString(p.getFoodData().getFoodLevel())));

        List<FormattedCharSequence> out = new ArrayList<>(lines.size());
        for (Component c : lines) {
            out.add(c.getVisualOrderText());
        }
        return out;
    }

    private static Component stat(String label, String value) {
        return Component.literal(label + "  " + value).withStyle(s -> s.withColor(TEXT_RGB));
    }
}
