# Prompts — Stepper buttons (4 states)

Four 12×12 sprites packed left-to-right at `y = 16`. The screen
draws the `-` and `+` glyph in white text on top, so leave the center
visually clean.

---

## STEPPER_IDLE — `(0, 16, 12, 12)`

> Pixel art Minecraft GUI button, 12×12, raised stone face. Top and
> left edges: 1-pixel highlight (`#BFBFBF`). Bottom and right edges:
> 1-pixel shadow (`#3F3F3F`). Body: flat `#5C5C5C`. 1-pixel
> diagonal specular highlight in the top-left interior corner
> (`#E6E6E6`, 1 pixel). 1px black outline on the full silhouette.
> Reads as a chunky pressable stone tile.

---

## STEPPER_HOVER — `(12, 16, 12, 12)`

> Same as STEPPER_IDLE, but the body is one shade lighter
> (`#8E8E8E` instead of `#5C5C5C`) and the specular highlight is
> brighter and 2 pixels wide. The change must be obvious at a glance
> but the silhouette is identical to IDLE.

---

## STEPPER_PRESSED — `(24, 16, 12, 12)`

> Pixel art Minecraft GUI button, 12×12, **inset / pressed** stone
> face. Bevel reversed: top and left edges = 1-pixel shadow
> (`#3F3F3F`), bottom and right edges = 1-pixel highlight
> (`#BFBFBF`). Body: `#3F3F3F`. The button visually sits 1 pixel
> deeper than IDLE. No specular highlight.

---

## STEPPER_DISABLED — `(36, 16, 12, 12)`

> Same as IDLE, but the body and bevel are desaturated to a uniform
> `#3F3F3F` interior with `#5C5C5C` highlights and no specular. The
> button should read as "you can't press this" — clearly muted vs.
> the other three states.

---

## Acceptance test

Place all four states in a horizontal strip and squint at 1× zoom:
- IDLE → HOVER: visible brightness pop
- IDLE → PRESSED: visible bevel reversal (looks pushed in)
- IDLE → DISABLED: clearly muted, almost grey-on-grey

If two states look the same, redo. The screen relies on these reading
distinctly to communicate interactivity.
