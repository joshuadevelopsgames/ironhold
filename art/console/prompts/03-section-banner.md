# Prompts — Section banners

## BANNER_RIBBON — atlas slot `(0, 76, 96, 24)`

> Pixel art heraldic banner ribbon, Minecraft GUI style, 96×20 sprite.
> Used as a section title plate above the TAXES, OVERVIEW, and TOLLS
> columns. A horizontal red (`#8B1F1F`) ribbon with a 1-pixel gold
> (`#FFCC44`) edge and small triangular notches cut into both ends
> (3-pixel deep notch). The body is flat red — no gradient. The center
> 60×16 region is visually clear (just red + gold edge) so the screen
> can draw the white-text section title on top. Tiny 2×2 gold
> heraldic dots at each end as accent. 1px black outline on the
> outermost silhouette.

State: 1. Size: 96×20. Used three times (one per column).

> **Important:** No text in the sprite. The screen draws "TAXES" /
> "OVERVIEW" / "TOLLS" on top at runtime.

---

## BURDEN_BAR_BG — atlas slot `(96, 76, 96, 24)`

> Pixel art recessed slot for a status bar, 96×20 sprite. A dark
> `#3F3F3F` rectangle inset 2 pixels from a thin iron-rim border —
> looks like an empty groove ready to hold a horizontal bar. The
> screen draws the four-segment traffic-light bar (green→yellow→
> orange→red) on top inside the inset region. Above the inset, a
> small 2-pixel iron tab on the top edge centered (decorative).
> Top edge highlight (#BFBFBF), bottom edge shadow (#3F3F3F) — classic
> inset bevel, like INNER_BOX but wider/shorter.

State: 1. Size: 96×20.
