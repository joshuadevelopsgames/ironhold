# Prompts — Decree icons

Two 16×16 icons at row A. The third slot in this row (`ICON_SKULL` at
`(112, 0)`) is **reserved/unused** — the unrest section was removed,
so leave that slot transparent.

---

## ICON_WAX_SEAL — `(128, 0, 16, 16)`

> Pixel art wax seal medallion, Minecraft GUI style, 16×16, face-on
> view. Round red wax disc 12 px in diameter (1px outline in
> `#8B1F1F` instead of black — the seal is so warm-toned that black
> outline reads wrong). Body: `#8B1F1F`. Two diagonal "drip" pixels
> at the bottom-left and bottom-right edges (suggesting molten wax
> just set). Crown imprint embossed in the center: a 5×3 crown
> silhouette in `#5C4A2B` (parchment_edge, used as a "darker red"
> that's still in palette). Single specular highlight: 2×1 in
> `#FFCC44` (gold) on the upper-left.

State: 1. Used as the small inline seal icon next to the decree text.

> **Outline rule exception:** wax seals get a `#8B1F1F` outline
> instead of `#2A2A2A`. Document this in your sprite metadata.

---

## ICON_DECREE_STAMP — `(144, 0, 16, 16)`

> Pixel art rubber-stamp tool, Minecraft GUI style, 16×16, 3/4 view.
> Wooden handle on top (vertical, 4px wide, 6px tall, body `#5C4A2B`,
> 1-pixel highlight `#C8B07A` on the left side). Brass band around
> the middle (1px thick, `#FFCC44`). Stamp base at the bottom: a
> wider 8×3 rectangle in dark `#3F3F3F` with a single `#8B1F1F`
> wax-red drop visible on the underside (suggesting the stamp has
> just been used). 1px black outline. Reads as "official seal stamp".

State: 1 (the slot button gets idle/hover BG from `renderSlotButton`,
the stamp icon itself is single-state).
