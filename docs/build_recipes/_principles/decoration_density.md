# Decoration density

How many "ornamental" blocks per room. Target ranges that distinguish
under-decorated (looks blocky and empty) from over-decorated (looks busy
and amateur).

## Definitions

- **Structural block**: walls, floors, ceilings, doors, full-block trim
- **Furnishing block**: tables, beds, thrones, chests, barrels, lecterns
- **Atmospheric block**: candles, banners, paintings, cobwebs, item frames,
  flower pots, signs, carpets, lanterns
- **Density** = (furnishing + atmospheric) / floor area in blocks²

## Target ranges by room type

| Room | Density (blocks per 10 m²) | Example for a 10×10 room |
|------|---------------------------|--------------------------|
| Empty/utility (corridor, undercroft) | 1–3 | 1 lantern + 1 chest |
| Storage (larder, undercroft) | 5–10 | barrels stacks, hanging meats |
| Living (solar, kitchen, guard) | 8–15 | bed, table, chairs, hearth, banner, candles |
| Ceremonial (great hall, throne) | 12–20 | throne+dais, banquet table+chairs, banners, chandeliers, fireplace |
| Sacred (chapel, crypt) | 10–18 | altar, candles, pews, lectern, tapestries |

## Anti-patterns

**Sparse**: a 16×16 great hall with one banner. The space dwarfs the
furnishing. Tells the player "nothing happens here."

**Cluttered**: a 6×6 chapel with 14 candles, 6 banners, an altar, three
pews, two lecterns, and a hanging chandelier. Each item competes for
attention; the space loses sanctity.

## Lighting density (separate from decoration)

Match the room's mood:

| Mood | Light source count per 10 m² | Mix |
|------|------------------------------|-----|
| Bright ceremonial | 2–3 | normal lanterns/chandeliers, white candles |
| Warm domestic | 1–2 | torches, fireplace, normal candles |
| Sacred | 1 | a single hanging lantern + altar candles |
| Dread | 0.5 | soul lanterns only, gaps of darkness |
| Dungeon | 0.25 | one torch per cell, dark gaps between |

## Apply this rule

In any generator function that builds a room, count furnishings against
density target before finishing. If under, add more thematic items from
the room's recipe palette. If over, remove the least-essential.
