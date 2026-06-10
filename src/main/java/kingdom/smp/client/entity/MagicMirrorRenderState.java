package kingdom.smp.client.entity;

import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/** Carries the magic mirror's glowing overlay (drawn by {@link MagicMirrorRenderer}) on top of {@link MirrorRenderState}. */
public class MagicMirrorRenderState extends MirrorRenderState {
    /** Player username — always shown above the head (null only before the first valid extract). */
    public @Nullable FormattedCharSequence name;
    /** Fade for the username (a gentle one-time fade-in, then steady). */
    public float nameAlpha;

    /** Stat lines, cycled through one at a time (per-glyph RGB lives in the styles). */
    public List<FormattedCharSequence> statLines = List.of();
    /** Index into {@link #statLines} of the line currently being shown. */
    public int activeLine;
    /** 0..1 fade for the active stat line: ramps up, holds, then ramps down before the next line. */
    public float statAlpha;

    /** Horizontal anchor in pane-local blocks (viewer-right from centre); clamped per-line when drawn. */
    public float anchorX;
    /** Local Y (blocks, up from centre) of the username row's centre. */
    public float nameCenterY;
    /** Local Y (blocks, up from centre) of the active stat row's centre. */
    public float statCenterY;
    /** Half the pane width in blocks — used to keep each line inside the glass. */
    public float halfW;
}
