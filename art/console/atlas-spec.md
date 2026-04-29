# Atlas Spec — `kings_console.png`

Single PNG, **256 × 256 RGBA**, transparent background.

Coordinate origin: top-left = `(0, 0)`. All UVs are inclusive of the
top-left and exclusive of the bottom-right (standard Minecraft atlas
convention).

## Row A — Icons (y = 0..15)

Ten 16×16 item-style icons, packed left-to-right.

| Slot | UV `(u, v)` | Size | Sprite | Prompt |
|------|-------------|------|--------|--------|
| `ICON_COIN` | (0, 0) | 16×16 | Gold coin (face on) | [06](prompts/06-icons-coins.md) |
| `ICON_GOLD_INGOT` | (16, 0) | 16×16 | Gold ingot, top-down | [06](prompts/06-icons-coins.md) |
| `ICON_EMERALD` | (32, 0) | 16×16 | Emerald gem | [06](prompts/06-icons-coins.md) |
| `ICON_LAND` | (48, 0) | 16×16 | Grass-block face | [07](prompts/07-icons-tax.md) |
| `ICON_CROWN` | (64, 0) | 16×16 | Royal crown | [07](prompts/07-icons-tax.md) |
| `ICON_GATE` | (80, 0) | 16×16 | Iron portcullis | [07](prompts/07-icons-tax.md) |
| `ICON_PORTAL` | (96, 0) | 16×16 | Ender ring | [07](prompts/07-icons-tax.md) |
| ~~`ICON_SKULL`~~ | (112, 0) | 16×16 | **RESERVED** — was unrest skull, removed from screen. Leave transparent. | — |
| `ICON_WAX_SEAL` | (128, 0) | 16×16 | Wax seal w/ crown imprint | [08](prompts/08-icons-misc.md) |
| `ICON_DECREE_STAMP` | (144, 0) | 16×16 | Brass stamp on red base | [08](prompts/08-icons-misc.md) |

## Row B — Stepper buttons (y = 16..27)

Four 12×12 button states, packed left-to-right.

| Slot | UV | Size | State |
|------|----|------|-------|
| `STEPPER_IDLE` | (0, 16) | 12×12 | Idle (raised stone) |
| `STEPPER_HOVER` | (12, 16) | 12×12 | Hover (brighter highlight) |
| `STEPPER_PRESSED` | (24, 16) | 12×12 | Pressed (inset, 1px shift) |
| `STEPPER_DISABLED` | (36, 16) | 12×12 | Disabled (desaturated) |

**Note:** the `-` and `+` glyphs are drawn by the screen on top in white.
Don't paint them into the sprite.

## Row C — Toggle button frames (y = 28..43)

Four 60×16 frame states. The label text is drawn by the screen on top.

| Slot | UV | Size | State |
|------|----|------|-------|
| `TOGGLE_OFF_IDLE` | (0, 28) | 60×16 | Off, no hover |
| `TOGGLE_OFF_HOVER` | (60, 28) | 60×16 | Off, hovered |
| `TOGGLE_ON_IDLE` | (120, 28) | 60×16 | On (green frame), no hover |
| `TOGGLE_ON_HOVER` | (180, 28) | 60×16 | On, hovered |

**Note:** the checkmark for "on" state is drawn at runtime in the right
4 px of the toggle. Reserve a roughly 8×8 darker region in the right
side of `TOGGLE_ON_*` so the checkmark contrasts.

## Row D — Panel chrome 9-slices (y = 44..75)

Four 9-slice tilesets, each laid out in a 32×32 region:

```
[ 0 ]                 [ 12 ]   [ 16 ]                [ 20 ]
+----+----+----+      +----+----+----+
| TL | TM | TR |      | TL | TM | TR |
+----+----+----+      +----+----+----+
| ML | MM | MR |      | ML | MM | MR |
+----+----+----+      +----+----+----+
| BL | BM | BR |      | BL | BM | BR |
+----+----+----+      +----+----+----+
```

Each tile inside the 9-slice is 8×8 except for `OUTER_FRAME` which uses
12×12 corners (so its full slot is wider). For 8×8-corner sets, the
9-slice fills 24×24 of the 32×32 slot.

| Slot | UV | Size | Tile | Notes |
|------|----|------|------|-------|
| `OUTER_FRAME` | (0, 44) | 32×32 | 12×12 corners | Heavy iron-bound stone frame around the whole panel |
| `INNER_BOX` | (32, 44) | 32×32 | 8×8 corners | Recessed slot frame for tax/toll boxes |
| `TITLE_BAR` | (64, 44) | 64×32 | 8×32 caps + tileable mid | Gilded metal title plaque |
| `PARCHMENT` | (128, 44) | 64×32 | 8×8 corners | Aged paper background for Royal Decree |

Use the *first* 24×24 of each 32×32 slot for the actual 9-slice.
The remaining strip is reserved for variants (don't paint it).

For `TITLE_BAR` and `PARCHMENT` the slot is 64 wide so tile-ability of
the middle strip can be tested in-place (paint two copies side-by-side
and verify the seam).

## Row E — Section banner ribbons (y = 76..99)

| Slot | UV | Size | Sprite |
|------|----|------|--------|
| `BANNER_RIBBON` | (0, 76) | 96×24 | Red-on-gold scroll plate, used for "TAXES" / "OVERVIEW" / "TOLLS" |
| `BURDEN_BAR_BG` | (96, 76) | 96×24 | Recessed slot for the Total Burden traffic bar (decorative frame; bar drawn on top) |

`BANNER_RIBBON` is **not** 9-slice — it's a fixed 96-wide plate. The
section title text is drawn at runtime, centered.

## Row F — Decorative (y = 100..131)

| Slot | UV | Size | Sprite |
|------|----|------|--------|
| `WAX_SEAL_DECREE` | (0, 100) | 32×32 | Large wax seal w/ blue ribbon, painted at fixed position next to decree |
| `KING_CONSOLE_PLATE` | (32, 100) | 128×32 | Gilded title plate with embossed "THE KING'S CONSOLE" — *no text*, just the metal — text drawn at runtime |

## Reserved / unused

- `(160, 100)..(255, 131)` — empty, reserved for future decorations
  (candle, scroll piles, etc.)
- `(0, 132)..(255, 255)` — empty, reserved (used to be where the realm
  map would go before it was scoped out)

## Code reference

UV constants live in `kingdom.smp.client.screen.ConsoleAtlas`. The
screen calls `gfx.blit(ConsoleAtlas.TEXTURE, x, y, U, V, W, H, 256, 256)`
with these exact UVs. Don't move slots around without updating that file.
