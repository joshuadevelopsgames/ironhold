#!/usr/bin/env python3
"""
Generate Ironhold item textures following the minecraft-item-texturing skill.

Produces 12 PNGs at 16x16, top-left lit, with shadow-tone outlines and
3-5 color palettes per material. Designs reference:
 - Terraria: band_of_regeneration, cloud_in_a_bottle, hermes_boots, shield_ankh
 - Vanilla Minecraft conventions: steel_ingot, pitchfork, armor_polish
 - D&D/fantasy: tempest_arrow, filcher_crown, wraiths_sigil
 - Original: raw_tanzanite, magic_minecart

All glyphs in the 16x16 grids map to RGBA tuples via a per-item palette.
"." is transparent. Rows must be exactly 16 characters.
"""

from PIL import Image
from pathlib import Path

TEX_DIR = Path(
    "/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/item"
)


def rgba(hex_str, a=255):
    h = hex_str.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


TRANSPARENT = (0, 0, 0, 0)

# ── Material palettes (copied from minecraft-item-texturing skill) ──────────

GOLD = dict(o=rgba("#6a5018"), s=rgba("#9a7820"), b=rgba("#d4a840"),
            h=rgba("#f0d060"), g=rgba("#fff0a0"))
IRON = dict(o=rgba("#4a4a4a"), s=rgba("#6a6a6a"), b=rgba("#a0a0a0"),
            h=rgba("#d0d0d0"), g=rgba("#f0f0f0"))
STEEL = dict(o=rgba("#2a2e36"), s=rgba("#4a525c"), b=rgba("#88909c"),
             h=rgba("#c0c8d4"), g=rgba("#e8ecf0"))
LEATHER_BROWN = dict(o=rgba("#3a2010"), s=rgba("#5a3420"),
                     b=rgba("#8a5438"), h=rgba("#a06e48"))
LEATHER_TAN = dict(o=rgba("#5a3820"), s=rgba("#806040"),
                   b=rgba("#b89068"), h=rgba("#d8b890"))
RUBY = dict(o=rgba("#5a0818"), s=rgba("#8a1824"), b=rgba("#d03040"),
            h=rgba("#f06878"), g=rgba("#ffb0c0"))
AMETHYST = dict(o=rgba("#301848"), s=rgba("#4a2870"), b=rgba("#7850b0"),
                h=rgba("#a078d0"), g=rgba("#d0a8f0"))
TANZANITE = dict(o=rgba("#281050"), s=rgba("#402080"), b=rgba("#6038b8"),
                 h=rgba("#8868d8"), g=rgba("#c0a8f0"))
WOOD = dict(o=rgba("#4a2a10"), s=rgba("#6a3e1c"), b=rgba("#9a6838"),
            h=rgba("#b88854"))
WOOD_DARK = dict(o=rgba("#2a1808"), s=rgba("#3e2414"), b=rgba("#5a3824"),
                 h=rgba("#7a5040"))
CLOUD = dict(o=rgba("#808898"), s=rgba("#b0b8c4"), b=rgba("#d8dee6"),
             h=rgba("#f0f4f8"))
GLASS_OUTLINE = rgba("#7a8898")
GLASS_SHADOW = rgba("#a4b0c0", a=200)
GLASS_BODY = rgba("#c8d4e0", a=110)
GLASS_SHINE = rgba("#f0f8ff", a=220)
CORK = dict(o=rgba("#4a2a10"), s=rgba("#7a5020"), b=rgba("#a07840"))
POLISH = dict(o=rgba("#5a3810"), s=rgba("#8a5820"), b=rgba("#c49040"),
              h=rgba("#e8c060"), g=rgba("#fff0a0"))
WIND = dict(o=rgba("#3a5070"), s=rgba("#5878a0"), b=rgba("#88a8c8"),
            h=rgba("#c0d8e8"))
WRAITH = dict(o=rgba("#0a0818"), s=rgba("#1c1830"), b=rgba("#3a3060"),
              h=rgba("#6858a0"), g=rgba("#b0a0e0"))
WHITE_FEATHER = dict(o=rgba("#9a9a9a"), b=rgba("#e8e8e8"), h=rgba("#ffffff"))


def from_grid(grid, palette):
    """Build a 16x16 RGBA image from an ASCII grid + glyph→color dict."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    for y, row in enumerate(grid):
        assert len(row) == 16, f"row {y} has len {len(row)}: {row!r}"
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            color = palette[ch]
            img.putpixel((x, y), color)
    return img


def save(img, name):
    out = TEX_DIR / f"{name}.png"
    img.save(out)
    print(f"wrote {out}")


# ── Item designs ────────────────────────────────────────────────────────────


def band_of_regeneration():
    # Gold ring w/ ruby cabochon — HOLLOW center so it reads as a band
    # Viewed roughly front-on with slight tilt
    p = dict(
        O=GOLD["o"], S=GOLD["s"], B=GOLD["b"], H=GOLD["h"], G=GOLD["g"],
        r=RUBY["o"], d=RUBY["s"], R=RUBY["b"], y=RUBY["h"], x=RUBY["g"],
    )
    grid = [
        "................",
        "................",
        ".......r........",  # gem tip
        "......ryR.......",  # gem: highlight top, glint
        ".....rxRRRr.....",
        ".....rRRRRr.....",
        "....rRRdRdRr....",  # facet pixels
        "...OOrrrrrOO....",  # gem seats on ring top
        "..OGBB...BBSO...",  # ring — hollow starts
        ".OBB.......BBO..",  # outer band + empty middle
        ".OB.........BO..",
        ".OBS.......SBO..",
        "..OSSSS.SSSSO...",  # ring bottom
        "...OOSSSSSOO....",
        "....OOOOOOO.....",
        "................",
    ]
    return from_grid(grid, p)


def steel_ingot():
    # Trapezoidal ingot, cooler & sharper than vanilla iron
    p = dict(
        O=STEEL["o"], S=STEEL["s"], B=STEEL["b"], H=STEEL["h"], G=STEEL["g"],
    )
    grid = [
        "................",
        "................",
        "................",
        "................",
        ".....OOOOOO.....",
        "....OGHBBBSO....",  # top: glint + highlight dominate
        "...OHBBBBBBSO...",
        "...OBBBBBBBBO...",
        "...OBBBBBBBBO...",
        "...OSBBBBBBSO...",
        "....OSSBBSSO....",
        ".....OOOOOO.....",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_grid(grid, p)


def cloud_in_a_bottle():
    # Vanilla-bottle silhouette with a cloud puff inside
    p = dict(
        o=GLASS_OUTLINE, s=GLASS_SHADOW, b=GLASS_BODY, i=GLASS_SHINE,
        C=CORK["o"], K=CORK["s"], k=CORK["b"],
        X=CLOUD["o"], Y=CLOUD["s"], W=CLOUD["b"], V=CLOUD["h"],
    )
    grid = [
        "................",
        "......CCC.......",  # cork top
        "......CkC.......",
        "......CKC.......",
        ".....ooooo......",  # bottle neck
        ".....obbbo......",
        "....obbbbbo.....",  # bottle shoulder
        "...obWVWbbbo....",  # cloud starts
        "..obWVWVWbbbo...",
        "..obVWVWVWbbo...",
        "..obWVWVWYbio...",  # shine pixel on right
        "..obWWYWYWbbo...",
        "..obXYYYYYbso...",
        "...obsYYYbso....",
        "....ossssso.....",
        ".....ooooo......",
    ]
    return from_grid(grid, p)


def hermes_boots():
    # Two short boots, tan leather, tiny white wing on outside of each
    p = dict(
        O=LEATHER_TAN["o"], S=LEATHER_TAN["s"], B=LEATHER_TAN["b"], H=LEATHER_TAN["h"],
        o=WHITE_FEATHER["o"], w=WHITE_FEATHER["b"], f=WHITE_FEATHER["h"],
        G=GOLD["s"],
    )
    grid = [
        "................",
        "................",
        "................",
        "..OOO.....OOO...",  # boot tops
        ".OHBBO...OHBBO..",
        "OHBBBBo.OHBBBBo.",  # wings start to poke out
        "OHBBBBwOHBBBBwo.",
        "OHBBBBfwHBBBBfw.",
        "OSBBBBGoSBBBBGo.",  # gold trim anklet
        "OSBBBBBOSBBBBBO.",
        "OSBBBBBOSBBBBBO.",
        ".OSBBBBOOSBBBBO.",
        "..OBBBBOOBBBBO..",  # toe
        "..OSBBBOOSBBBO..",
        "...OOOO..OOOO...",  # sole
        "................",
    ]
    return from_grid(grid, p)


def tempest_arrow():
    # Diagonal arrow, iron head upper-right, fletching lower-left, wind wisps
    p = dict(
        O=IRON["o"], S=IRON["s"], B=IRON["b"], H=IRON["h"], G=IRON["g"],
        w=WIND["o"], x=WIND["s"], y=WIND["b"], z=WIND["h"],
        F=WIND["s"],  # fletching reuses wind-blue palette
        f=WIND["b"], F2=WIND["h"],
        W=WOOD_DARK["o"], D=WOOD_DARK["s"], d=WOOD_DARK["b"],
    )
    grid = [
        "................",
        "...........OO...",  # arrowhead tip
        "..........OBHO..",
        ".........OBGBO..",  # glint on head
        ".z......OSBBO...",
        "..zy...OSBBO....",  # wind wisp left + arrow shaft
        "...yzOSBBO......",
        "...WdDO.........",  # shaft wood
        "..WDddW.........",
        ".FDdDW..........",
        "ffFD............",  # fletching
        "zfFF............",
        "yzf.............",
        "wy..............",
        "................",
        "................",
    ]
    return from_grid(grid, p)


def magic_minecart():
    # Dark minecart silhouette (3/4 view) with purple rune glow on rim
    p = dict(
        O=IRON["o"], S=IRON["s"], B=IRON["b"], H=IRON["h"],
        W=WOOD_DARK["o"], D=WOOD_DARK["s"], d=WOOD_DARK["b"],
        m=MAGIC_PURPLE_OR_LIT("o"), n=MAGIC_PURPLE_OR_LIT("s"),
        M=MAGIC_PURPLE_OR_LIT("b"), L=MAGIC_PURPLE_OR_LIT("h"),
        g=MAGIC_PURPLE_OR_LIT("g"),
    )
    # (alias handling below)
    grid = [
        "................",
        "................",
        "...g............",
        "...Mm...........",  # glow spark
        "..OMMOOOOOMMO...",  # rim with magic glow at corners
        ".OHBBBBBBBBBHO..",  # inside of cart (iron)
        ".OBDddddddddBO..",  # wood floor
        ".OBDddddddddBO..",
        ".OSBWWWWWWWWBO..",
        "..OOSSSSSSSSOO..",
        "...OOO....OOO...",  # wheels
        "..OSBBO..OSBBO..",
        "..OBBBO..OBBBO..",
        "..OSBSO..OSBSO..",
        "...OOO....OOO...",
        "................",
    ]
    return from_grid(grid, p)


def pitchfork():
    # Farm pitchfork — 3 iron tines top-right, wood haft diagonal to bottom-left
    p = dict(
        O=IRON["o"], S=IRON["s"], B=IRON["b"], H=IRON["h"], G=IRON["g"],
        W=WOOD["o"], D=WOOD["s"], d=WOOD["b"], l=WOOD["h"],
    )
    grid = [
        "........O.O.O...",  # 3 tine tips
        "........O.O.O...",
        "........OBOBO...",
        "........OBOBO...",
        "........OHBHO...",  # tine bases + glint
        ".........OGO....",
        "........OBBO....",  # ferrule
        ".......ODDWO....",  # haft top (wood)
        "......OWlDW.....",
        ".....OWdDW......",
        "....OWdDW.......",
        "...OWlDW........",
        "..OWdDW.........",
        ".OWdDW..........",
        "OWdDW...........",  # haft bottom
        "OWDW............",
    ]
    return from_grid(grid, p)


def shield_ankh():
    # Round-top shield, tan leather w/ iron rim, gold ankh symbol centered
    # 14x14 silhouette fills most of 16x16
    p = dict(
        O=IRON["o"], S=IRON["s"], I=IRON["b"], H=IRON["h"],
        L=LEATHER_TAN["o"], l=LEATHER_TAN["s"], b=LEATHER_TAN["b"], h=LEATHER_TAN["h"],
        G=GOLD["o"], g=GOLD["s"], A=GOLD["b"], a=GOLD["h"], k=GOLD["g"],
    )
    grid = [
        "................",
        "...OOOOOOOOOO...",  # shield top rim
        "..OHIIIIIIIIHO..",
        "..OIhbbbbbbblO..",
        ".OIhbbGAAAgbbIO.",  # ankh loop top
        ".OIbbGAkAAgbblO.",
        ".OIbbbGAAGbbblO.",  # loop bottom
        ".OIbbgGAGgbbblO.",
        ".OIbbbbAbbbbblO.",  # ankh stem + crossbar
        ".OIbbGgAgGbbblO.",
        ".OIbbbbAbbbbblO.",
        "..OIbbbbAbbbblO.",
        "..OIlbbbbbbblIO.",
        "...OIllbbllIO...",  # bottom curve
        "....OOllllOO....",
        ".....OOOOOO.....",
    ]
    return from_grid(grid, p)


# raw_tanzanite: kept as the original hand-drawn PNG (not regenerated)
# wraiths_sigil: kept as the original hand-drawn PNG (not regenerated)


def filcher_crown():
    # Small asymmetric crown — 5 tines of varying heights, gold palette
    p = dict(
        O=GOLD["o"], S=GOLD["s"], B=GOLD["b"], H=GOLD["h"], G=GOLD["g"],
        r=RUBY["o"], R=RUBY["b"], y=RUBY["h"],
    )
    grid = [
        "................",
        "................",
        "..O....O........",  # tine tops (3 visible)
        "..OB...OB...O...",  # tine tops with highlight
        "..OBO.OBO..OBO..",
        "..OBOrOBOrOOBO..",  # small rubies at base of tines
        ".OBHByOBByOHBO..",  # highlight row, asymmetric tine
        "OHBBBBBBBBBBBSO.",  # crown band top
        "OBGBBBBBBBBBBBO.",  # glint one side
        "OSBBBBBBBBBBBSO.",
        "OSSBBBBBBBBBBSO.",
        ".OSSSSSSSSSSSO..",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
        "................",
    ]
    return from_grid(grid, p)


def armor_polish():
    # Glass bottle with amber polish liquid, cloth wrap on neck, single glint
    p = dict(
        o=GLASS_OUTLINE, s=GLASS_SHADOW, b=GLASS_BODY, i=GLASS_SHINE,
        C=CORK["o"], k=CORK["b"],
        L=LEATHER_BROWN["o"], l=LEATHER_BROWN["s"], m=LEATHER_BROWN["b"],
        P=POLISH["o"], Q=POLISH["s"], R=POLISH["b"], H=POLISH["h"], G=POLISH["g"],
    )
    grid = [
        "................",
        "......CCC.......",
        "......CkC.......",
        ".....LmLmL......",  # cloth wrap
        ".....LmmmL......",
        ".....ooooo......",
        "....obibbbo.....",
        "...obbbbbibo....",  # shine pixel on glass
        "..obHRRRRRbo....",
        "..obRGRRRRRbo...",  # polish highlight + glint
        "..obRRRRRRRbo...",
        "..obQRRRRRQbo...",
        "..obQQRRRQQbo...",
        "...obQQQQQbo....",
        "....osssssso....",
        ".....ooooo......",
    ]
    return from_grid(grid, p)


def mimic_key():
    # Ornate brass key — circular bow top-right, shaft diagonal, 2 teeth bot-left
    p = dict(
        O=GOLD["o"], S=GOLD["s"], B=GOLD["b"], H=GOLD["h"], G=GOLD["g"],
        r=RUBY["o"], R=RUBY["b"], y=RUBY["h"],
    )
    grid = [
        "................",
        "...........OOO..",  # bow outer top
        "..........OHBBO.",  # bow with highlight
        ".........OBrRrBO",  # tiny ruby in bow
        ".........OHyRHBO",  # bow highlight + glint
        ".........OBrRrBO",
        "..........OSBBO.",
        "...........OOSO.",
        "..........OBO...",  # shaft begins
        ".........OBO....",
        "........OBO.....",
        ".......OBO......",
        "......OBO.......",
        ".....OBBOO......",  # top tooth
        "....OBBOOO......",
        "...OOOOO........",  # second tooth + stem end
    ]
    return from_grid(grid, p)


# Alias for the magic minecart palette (defined after declaration uses it)
def MAGIC_PURPLE_OR_LIT(k):
    table = dict(o=rgba("#3a2048"), s=rgba("#5a3070"), b=rgba("#8860b0"),
                 h=rgba("#b090e0"), g=rgba("#e8c8ff"))
    return table[k]


# ── Main ────────────────────────────────────────────────────────────────────


ITEMS = {
    "band_of_regeneration": band_of_regeneration,
    "steel_ingot": steel_ingot,
    "cloud_in_a_bottle": cloud_in_a_bottle,
    "hermes_boots": hermes_boots,
    "tempest_arrow": tempest_arrow,
    "magic_minecart": magic_minecart,
    "pitchfork": pitchfork,
    "shield_ankh": shield_ankh,
    "mimic_key": mimic_key,
    "filcher_crown": filcher_crown,
    "armor_polish": armor_polish,
    # raw_tanzanite, wraiths_sigil — reverted to originals, not regenerated
}


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    for name, fn in ITEMS.items():
        img = fn()
        save(img, name)
    print(f"\nGenerated {len(ITEMS)} textures.")


if __name__ == "__main__":
    main()
