# Prompts — Toggle button frames (4 states)

Four 60×16 sprites at `y = 28`. The screen draws the label text and
the green checkmark on top — leave the interior visually clear in the
left ~50px and reserve a darker 8×8 pad in the right side for the
checkmark.

The toggle is shaped like a heraldic shield strip: rounded ends with a
thin metallic band along the top.

---

## TOGGLE_OFF_IDLE — `(0, 28, 60, 16)`

> Pixel art toggle button frame, Minecraft GUI style, 60×16.
> Heraldic shield-strip silhouette: flat 60×14 rectangle with the top
> two corner pixels and bottom two corner pixels chamfered (1 pixel
> diagonal). Body: dark stone `#3F3F3F`. Top edge: 1-pixel highlight
> `#5C5C5C`. Bottom edge: 1-pixel shadow `#2A2A2A`. Left side
> 50×14 is plain (text drawn on top). Right side: a slightly inset
> 10×10 panel (1px darker `#2A2A2A` on top/left, 1px lighter
> `#5C5C5C` on bottom/right) — this is where the checkmark goes when
> "on" is reached. 1px black outline on full silhouette.

---

## TOGGLE_OFF_HOVER — `(60, 28, 60, 16)`

> Same as TOGGLE_OFF_IDLE but the body is one shade lighter (`#5C5C5C`)
> and the highlight band is brighter (`#8E8E8E`). Frame must read as
> the same shape — only the body color changes.

---

## TOGGLE_ON_IDLE — `(120, 28, 60, 16)`

> Same silhouette as TOGGLE_OFF_IDLE, but with green heraldic
> framing: outline becomes `#55DD55` instead of black, body stays
> `#3F3F3F`, top-edge highlight is `#55DD55`. The right-side 10×10
> inset panel becomes a darker green (`#2D5C2D`) so a `#55DD55`
> checkmark drawn on top will read clearly. 1px black outline still
> appears around the very outermost edge under the green highlight
> (so it doesn't bleed into adjacent UI).

---

## TOGGLE_ON_HOVER — `(180, 28, 60, 16)`

> Same as TOGGLE_ON_IDLE but body is `#5C5C5C` and the green outline
> brightens to a slightly more saturated `#88FF88`. Reads as a
> "ready to click off" state.

---

## Reserved checkmark zone

The right-most 10×10 of every toggle is **for the runtime checkmark**.
If you decorate that zone with anything more than a flat darker panel
the checkmark will conflict. Keep it muted.
