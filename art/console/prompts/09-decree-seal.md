# Prompts — Decree wax seal (large)

The big medallion that sits next to the Royal Decree text. Shown on
the painted parchment panel, replacing the runtime-drawn honeycomb.

---

## WAX_SEAL_DECREE — `(0, 100, 32, 32)`

> Pixel art ornate wax seal with ribbon, Minecraft GUI style, 32×32,
> face-on. A round wax medallion 22 px in diameter centered horizontally,
> hanging slightly above center with two ribbons trailing down to the
> bottom of the sprite.
>
> Medallion body: `#8B1F1F` (wax red). 1-pixel `#5C4A2B` outline
> (warm dark instead of black — wax seals read wrong with pure black).
> Crown imprint embossed in the center: a 7×5 crown silhouette
> stamped into the wax, drawn in `#5C4A2B` (parchment_edge as the
> "deeper red"). Two 1×2 specular highlights on the upper-left of
> the medallion in `#FFCC44` to suggest the wax surface is glossy.
> A single 1×1 `#FFEE99` brightest specular on top.
>
> Two ribbons hang from behind the medallion, one to each side at
> ~30° outward angles. Ribbon body: `#4477CC` (ally_blue) for the
> visual contrast against the red wax. Each ribbon is 3 px wide and
> tapers to a forked end (V-cut) at the bottom of the sprite. 1-pixel
> darker `#2A2A4A`-style shadow stripe down the right edge of each
> ribbon (use the closest available palette color — likely
> `#3F3F3F`).
>
> The bottom 8 px of the sprite is the ribbon tails extending past
> the medallion. Background: transparent.

State: 1. Single sprite, no hover state.

## Acceptance

- The wax seal must read as warm/red, the ribbon as cool/blue. Color
  contrast is the whole point.
- The crown imprint must be visible at 1× zoom — squint test: the
  silhouette of a crown should be obvious without scaling.
- No drop shadow. The medallion sits flat on the parchment; depth
  comes from the embossed crown only.
