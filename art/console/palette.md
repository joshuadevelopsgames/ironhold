# Locked Palette — 16 colors

Sampled from the King's Console reference mockup. Every sprite **must use
only these colors**. No off-palette pixels. Enforce by loading
`palette.gpl` in Aseprite under *Sprite → Color Mode → Indexed*.

| # | Hex | Role | Where used |
|---|-----|------|------------|
| 0 | `#2A2A2A` | shadow / outline | All icon outlines, deepest shadows |
| 1 | `#3F3F3F` | dark_mid | Recessed slots, button shadow |
| 2 | `#5C5C5C` | stone_dark | Frame inner edge, dark bevel |
| 3 | `#8E8E8E` | stone_mid | Main stone frame body |
| 4 | `#BFBFBF` | stone_light | Stone highlights, light bevel |
| 5 | `#E6E6E6` | text / metal_high | Title plate, brightest highlight |
| 6 | `#B8860B` | gold_dark | Coin shadow, gold trim shadow |
| 7 | `#FFCC44` | gold_mid | Coins, title text, banner accents |
| 8 | `#FFEE99` | gold_light | Specular highlight on gold |
| 9 | `#C8B07A` | parchment | Decree paper, scrolls |
| 10 | `#5C4A2B` | parchment_edge | Parchment shadow, ink |
| 11 | `#3A2D14` | parchment_text | Decree body text color |
| 12 | `#55DD55` | ok_green | "FAIR" status, ok bar segment, toggle on |
| 13 | `#CC3333` | bad_red | Unrest %, bad bar segment, X button |
| 14 | `#8B1F1F` | wax_red | Decree wax seal body |
| 15 | `#4477CC` | ally_blue | Ally heraldry banner color |

## Notes

- **No anti-aliasing.** Hard edges only. Subpixel-blended pixels are not
  in the palette and are rejected.
- **Two-tone shading max** for icons under 16×16 (one highlight + one
  shadow from the table). Larger panels can use 3 tones.
- **Status colors are reserved** — don't use `ok_green`/`bad_red` outside
  status indicators.
- **Frame bevel rule:** stone uses 4 → 3 → 2 (light → mid → dark) on a
  top-left light source. Never reverse.
