# Material storytelling

Material choice tells the building's history. A discerning eye reads the
age and circumstance of a castle from its stone before reading its shape.

## The four "eras" every castle has

| Era | What was built | Materials | Why |
|-----|----------------|-----------|-----|
| **Founding** (oldest) | Original keep / motte, a single tower | Cobbled deepslate, cobblestone, mossy variants, the crudest stone available. Iron-bound oak doors. Heavy timber roof, no glass. | This was a frightened lord 200 years ago. He built for survival, not beauty. |
| **Curtain** (middle) | Surrounding wall, gatehouse, well, first chapel | Deepslate bricks (or stone bricks). Some dressed stone. First small glass. | A more confident era. The lord could now spare workers for stone-shaping. |
| **Refinement** (newer) | Solar additions, chapel expansion, banquet hall, gardens | Polished deepslate, polished blackstone, chiseled blocks, glass panes, finer wood (dark oak vs the founders' birch/oak). | A wealthy period. The lord wanted to live well, not just survive. |
| **Modern** (newest) | Barbican, drum towers (replacing square ones), spires | Polished blackstone bricks, chiseled accents, lots of glass, complex roof shapes. | The newest fashion. A descendant rebuilt the entry to impress visitors. |

## How to render this in MC

### Layered material functions

Have separate "stone_X" helpers for each era. The block-mix proportions
differ:

```python
def stone_founding():     # oldest, cruder
    r = random.random()
    if r < 0.55: return CDS         # cobbled deepslate
    if r < 0.75: return MOSSY       # mossy cobble
    if r < 0.88: return DSB_CRACK   # cracked
    return DSB

def stone_curtain():      # middle
    r = random.random()
    if r < 0.55: return DSB
    if r < 0.75: return CDS
    if r < 0.90: return DSB_CRACK
    return PDS

def stone_refinement():   # newer
    r = random.random()
    if r < 0.55: return PDS
    if r < 0.75: return DSB
    if r < 0.85: return CHIS_DS
    if r < 0.95: return DSB_TILE
    return PBS

def stone_modern():       # newest
    r = random.random()
    if r < 0.55: return PBSB
    if r < 0.75: return PDS
    if r < 0.88: return CHIS_PBSB
    if r < 0.95: return CHIS_DS
    return DSB_TILE
```

Use the founding mix on the keep walls (especially lower courses).
Curtain mix on the curtain walls. Refinement on the solar/chapel/upper
keep. Modern on the barbican.

### Visible boundaries

Where two eras meet, leave a slightly visible seam — a column of cruder
stone next to a column of polished. Real masonry has these joins;
flawless transitions look CG-generated.

### Wear and weathering

Distribute cracked / mossy variants by *exposure*:
- More cracked on south-facing walls (sun damage)
- More mossy on north-facing walls (damp)
- Lower courses always crustier than upper
- Tops of merlons more weathered (rain)

This is a 5% effect — too much and it just looks ruined. A 1-in-20 chance
of "weathered" at a given perimeter cell, weighted toward the right
exposure direction.

## Anti-pattern

dark_castle_v2 uses one global `stone_dressed()` mix for everything except
the keep (which uses `stone_old`). That's only 2 eras visible, evenly
distributed. A keen viewer can't tell which part was built first or last.

## Apply this rule

A new generator should pick the building's era plan at the start:
- Date the founding (oldest part)
- List 2–4 additions with eras
- Render each in the appropriate mix
- Tag which walls belong to which era in code comments
