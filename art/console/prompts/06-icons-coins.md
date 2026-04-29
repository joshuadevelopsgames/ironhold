# Prompts — Currency icons

Three 16×16 icons at row A (y=0). Treasury and tax-icon use.

---

## ICON_COIN — `(0, 0, 16, 16)`

> Pixel art gold coin, Minecraft GUI item style, 16×16, face-on view.
> Round shape (1px black outline forming a circle). Body: `#FFCC44`.
> Crown imprint embossed on the face — a small 5×3 crown silhouette
> in `#B8860B` (darker gold) centered. Specular highlight: 2-pixel
> diagonal in the upper-left interior in `#FFEE99`. Bottom-right
> interior: 1-pixel shadow ring in `#B8860B`. Reads as a single
> gold coin from a pile.

State: 1. Reused as the "currency" icon throughout the screen.

---

## ICON_GOLD_INGOT — `(16, 0, 16, 16)`

> Pixel art gold ingot, Minecraft style, 16×16, top-down 3/4 view. A
> rectangular bar 12×6, centered horizontally with a slight inward
> taper at the bottom (parallelogram-like). Body: `#FFCC44`. Top
> face: 2-pixel `#FFEE99` highlight strip. Bottom edge: 2-pixel
> `#B8860B` shadow. Side facets: 1-pixel `#B8860B` on the right.
> 1px black outline. Vanilla Minecraft gold-ingot energy but
> not literally pixel-copying.

State: 1.

---

## ICON_EMERALD — `(32, 0, 16, 16)`

> Pixel art emerald gem, Minecraft style, 16×16. Hexagonal cut crystal
> shape (1px black outline). Body: `#22DD66` style green — but only
> palette greens are allowed, so use `#55DD55` as the body and
> `#3F3F3F` as the deep facet shadow. Top facet: 2-pixel `#88FF88`
> wedge highlight. Vertical edge crease in body splits left/right
> shading. Reads as a single uncut emerald.

State: 1.

> **Palette note:** if you only have `#55DD55` and `#3F3F3F` to work
> with for green, that's fine — the icon must be readable with two
> palette greens. If a richer emerald is needed, treat darker tones
> as low-saturation `#5C5C5C` outlines around a `#55DD55` core.

---

## Acceptance

Lay all three at 1× scale next to a vanilla Minecraft gold-ingot icon
and emerald icon. They should read as the same family — same outline
weight, same highlight density. If they look "shinier" or "softer"
than vanilla, they're wrong.
