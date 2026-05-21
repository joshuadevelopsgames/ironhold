# Design language: pick three motifs and repeat them

The single biggest lever between "blocky scaffold" and "designed building" is
**motif reuse**. A real architect picks a small set of shape and ornament
ideas and repeats them at every scale of the building.

## The rule

For any structure, decide on:

1. **One arch profile** — used at the gate, at the windows, above the throne, framing the altar, on the bed canopy. Same shape, different sizes.
2. **One trim element** — a slab string-course, a stair lip, a chiseled-block band. Appears at every floor break, around every window, framing every door.
3. **One ornament accent** — a banner color, a chiseled-block crest, a particular candle arrangement. Punctuates important spots.

If a feature in the building doesn't echo at least one of those three, it
either gets adjusted to echo or it doesn't belong.

## How to apply in a generator

```python
class DesignLanguage:
    arch_profile = "pointed_3w"    # see windows/gothic_pointed_3w5h.md
    trim_band = DSB_SLAB_TOP        # slab top, deepslate brick
    accent = "black_banner"

# Then every helper function references self.design.arch_profile etc.
# A new window doesn't get a fresh shape — it instances the chosen profile.
```

## Anti-pattern

The dark_castle_v2 build had at least four different window shapes (pointed,
rectangular pane, single arrow loop, double slit), three different door
treatments (plain block, arched portal, double-door), and two unrelated trim
elements (slab string-course, no string-course). That's why it reads as
"functional but bland" — there's no recognizable signature.

## Test for any room

Before declaring a room done, ask: "If a stranger walked in and saw only this
room, could they tell what building it's part of?" If yes — by an arch, trim,
or accent that repeats elsewhere — the room belongs. If no, it's a separate
building you accidentally built inside the first one.
