# Prompts — Frame chrome (9-slice)

Three 9-slice tilesets that wrap the screen and its inner panels.

---

## OUTER_FRAME — atlas slot `(0, 44, 32, 32)`

> Pixel art Minecraft GUI panel border, 9-slice tileset. Heavy iron-bound
> stone, weathered castle wall — flat stone face with dark mortar lines,
> bordered top and bottom by a 2-pixel iron strap with riveted bolts at
> each corner. Painted as a 24×24 sprite divided into nine 8×8 tiles:
> top-left corner with a riveted iron bracket, top-middle that tiles
> horizontally (continuous iron strap over stone), top-right corner
> mirroring the left, middle-left edge that tiles vertically (vertical
> iron rail), middle-middle plain stone fill that tiles both ways,
> middle-right edge mirroring left, then the three bottom tiles
> mirroring the top. Must read as a hand-painted Minecraft GUI border
> at 1× zoom. 1px black outline on the outermost edge only.
> Palette: stone tones (#5C5C5C, #8E8E8E, #BFBFBF) for the body,
> iron bands in (#3F3F3F, #2A2A2A) with rivets in #5C5C5C.

State: 1 (idle). Size: 24×24 9-slice (use top-left 24×24 of the slot).

---

## INNER_BOX — atlas slot `(32, 44, 32, 32)`

> Pixel art recessed slot frame, 9-slice tileset, 24×24 with 8×8
> tiles. Lighter iron rim than OUTER_FRAME — a thin 1-pixel iron edge
> with a 1-pixel inner highlight, framing a recessed dark panel
> (`#1F1F1F` fill). Top edge bright (#BFBFBF), bottom edge dark
> (#3F3F3F) — classic inset bevel. Used for tax, toll, and overview
> sub-panels. Must tile cleanly when stretched horizontally and
> vertically.

State: 1. Size: 24×24 9-slice.

---

## PARCHMENT — atlas slot `(128, 44, 64, 32)`

> Pixel art aged parchment paper, 9-slice tileset, 24×24 with 8×8
> tiles. Warm tan (`#C8B07A`) base with a 1-pixel darker brown
> (`#5C4A2B`) border that frays slightly at the corners (1-2 pixels
> displaced). Subtle 2-pixel darker fiber speckles scattered on the
> middle fill, low density (~3-4 specks per 8×8 tile, never adjacent).
> No drop shadow — Minecraft GUIs don't shadow. The middle tile must
> repeat horizontally without visible seams. The slot is 64 wide so
> you can paint two copies of the middle tile side-by-side and verify
> the seam in-place.

State: 1. Size: 24×24 9-slice (paint duplicate mid-strip in the
remaining 24×8 to verify tile-ability).

---

## Acceptance for 9-slice sets

After painting, in Aseprite:
1. Slice each into 9 cells (Slice tool).
2. Use *Edit → Patterns* to tile a 64×64 area from each cell.
3. Visible seam at any cell boundary = redo that edge.
