# [Recipe Name] — [key dimensions]

**Status:** v0 untested | v1 in-game-validated | deprecated
**Provenance:** claude-proposal | reference-image | reference-public | user-built
**Source:** [URL or "Joshua, hand-built 2026-05-15"]
**License:** [if external — copy from source]
**Best for:** [great halls, gatehouses, ruined buildings, etc.]
**Reads as:** [ecclesiastical, defensive, residential, agricultural, etc.]
**Pair with:** [other recipe names from the library]

## Description

One paragraph: what this is, when it works, what feeling it conveys.

## Reference image

If applicable. Embed via:
```
![alt](../_images/<source>/<image>.png)
```

## Block list

- Nx <block_id>: purpose
- ...

## Layout

A small ASCII or block-by-block diagram showing the spatial pattern.

```
y+5    . ^ .
y+4    ^ G ^
...
```

## Python snippet

```python
def recipe_name(g, anchor_x, anchor_y, anchor_z, facing="north", ...):
    """Place this recipe with its lower-left corner at (anchor_x, anchor_y, anchor_z)."""
    # exact block placement using setb() and palette ids
    ...
```

## Iteration history

- v0 (YYYY-MM-DD): initial proposal
- v1 (YYYY-MM-DD): what changed and why

## See also

- [related_recipe.md](../category/related_recipe.md) — when to use this instead
