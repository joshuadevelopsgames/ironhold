# Prompts — Title bar plates

## TITLE_BAR — atlas slot `(64, 44, 64, 32)`

> Pixel art gilded metal title plate, Minecraft GUI style, 9-slice
> tileset with 8×8 tiles totaling 24×24, plus a horizontally-tileable
> middle. Dark stone bracket caps on left and right (deeper than the
> outer frame) with a single gold rivet centered in each cap. Middle
> tile: a flat dark recessed band (`#3F3F3F` background) with a thin
> gold underline (1px, `#FFCC44`) running horizontally — this is where
> the screen will draw "THE KING'S CONSOLE" text on top. The middle
> must tile horizontally with no seam. Cap pieces are fixed (don't
> need to tile). 1px black outline on the outer edge of caps only.

State: 1. Size: 24×24 9-slice + 24w tileable mid (paint the mid twice
to verify tile-ability).

---

## KING_CONSOLE_PLATE — atlas slot `(32, 100, 128, 32)`

> Pixel art ornate gilded plaque, Minecraft style, 128×24 sprite (use
> top-left 128×24 of the 128×32 slot). A horizontal banner shape with
> peaked end caps, edged in gold (`#FFCC44`) over a dark `#3F3F3F`
> base. Subtle scrollwork at each end: a 4-pixel curl in `#FFEE99`
> highlight. The center is a flat dark recess where text will be
> drawn at runtime — leave it visually empty (no pre-drawn letters).
> Two small gold heraldic shields flank the recess (4×6 pixels each,
> 8 px from each end), each with a tiny crown silhouette in
> `#FFEE99`. 1px black outline on the entire silhouette.

State: 1. Size: 128×24. Single sprite — not 9-slice.

> **Important:** Do not paint any text into this sprite. The screen
> draws "THE KING'S CONSOLE" on top at runtime, so any pre-drawn text
> would double-up.
