# Lighting dramaturgy

Light is a narrative tool, not just a visibility utility. Each Minecraft
light source signals something different. Mixing them indiscriminately
washes out the story.

## Vocabulary

| Light source | Reads as | Use for |
|--------------|----------|---------|
| Torch (yellow) | Common, working, mortal | Stable, smithy, guardroom, peasant hall |
| Wall torch | Same as torch, attached | Corridor sconces, courtyard markers |
| Lantern (yellow, hanging) | Wealth, refinement, indoors | Great hall, solar, dining, lord's chamber |
| Lantern (yellow, floor) | Working area, durable | Smithy, gatehouse, dock |
| Soul torch (blue) | Death, dread, otherworldly | Crypt, prison, dark ritual chamber |
| Soul lantern (hanging) | Solemn, sanctified, ominous | Chapel above an altar, sentry path, gate of a dark castle |
| Candle (white) | Sacred, private, intimate | Altar, bedside, lord's writing desk |
| Candle (red) | Ritual, sacrifice, mourning | Crypt, dark altar, vigil |
| Candle (black) | Heresy, hidden cult | Wither altar, forbidden chamber |
| Magma block | Forge, fire pit, infernal | Smithy, central hearth, dread threshold |
| Fire (on magma/netherrack) | Active flame, life of building | Hearth, brazier, beacon |
| End rod | Magical, alien, royal | Throne room above the throne only, magical sanctum |
| Glowstone (yellow) | Sun-like, divine | Above an altar dome, sun motif |
| Lava (carefully) | Volcanic, hostile | Dungeon, lava-moat, depths |

## Rules

### One primary, one accent

Each room picks ONE primary light source for ambient glow, and one accent
light for moments of emphasis. A great hall = lanterns (primary) +
candles on the table (accent). The chapel = soul lantern (primary, single)
+ altar candles (accent). The crypt = soul torches (primary, sparse) +
red candles on sarcophagi (accent).

### Never mix soul fire and normal fire in the same space

Soul fire is otherworldly. Normal fire is mortal. A room reads as one or
the other. Mixing them tells the player "the designer didn't pick."

### Light density follows mood

See `decoration_density.md` for the light-density table.

### Shadow is content

Don't light every cell. A great hall with deliberate dim corners reads as
moody and inhabited. Wall-to-wall lanterns reads as a museum.

### Light reveals the important block

If there's a throne in the great hall, the *one* hanging lantern over it
is the only light source within 6 blocks. Everything else is fireplace
glow or table candles. The throne is lit; the path to it is implied.

## Anti-pattern

The dark_castle_v2 mixes soul lanterns, torches, normal lanterns, candles,
fire, magma blocks, and a wither skull as light sources within the same
building. The keep has soul lanterns in the great hall AND the chapel AND
the solar, plus chain lanterns AND candles in the chapel, plus fire in
the fireplace AND magma. The intent reads as "dark and atmospheric" but
the execution is "everything at once."

## Apply this rule

In any generator, decide each room's primary + accent light at the start
and don't mix outside that pair. The DesignLanguage class from
`design_language.md` should include `primary_light` and `accent_light`
slots.
