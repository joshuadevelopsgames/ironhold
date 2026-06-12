#!/usr/bin/env python3
"""Generate block/item assets for the statue blocks (one set per variant).

Per variant this emits:
  - blockstates/<v>_statue.json          (particle-only shared model)
  - items/<v>_statue.json                (client item definition)
  - models/item/<v>_statue.json          (flat icon model)
  - textures/item/<v>_statue.png         (16x16 icon: head+hat front of the skin, 2x)
  - data/.../loot_table/blocks/<v>_statue.json (drops itself)
plus the shared models/block/statue.json (break-particle texture only; all
visible geometry is drawn by StatueBlockRenderer).

Run after adding a statue variant:
    python3 scripts/gen_statue_assets.py
"""

import json
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/ironhold")
DATA = os.path.join(ROOT, "src/main/resources/data/ironhold")

VARIANTS = [
    "kangarude",
    "haalina",
    "facelaces",
    "red_raichu",
    "twohrd",
    "arcatheone",
    "cheakie",
]


def write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as out:
        json.dump(payload, out, indent=2)
        out.write("\n")


def make_icon(variant):
    """16x16 inventory icon: the statue skin's head front with hat overlay, 2x."""
    skin = Image.open(
        os.path.join(ASSETS, f"textures/entity/statue/{variant}.png")
    ).convert("RGBA")
    head = skin.crop((8, 8, 16, 16))
    hat = skin.crop((40, 8, 48, 16))
    head.alpha_composite(hat)
    icon = head.resize((16, 16), Image.NEAREST)
    path = os.path.join(ASSETS, f"textures/item/{variant}_statue.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    icon.save(path)


def main():
    write_json(
        os.path.join(ASSETS, "models/block/statue.json"),
        {"textures": {"particle": "minecraft:block/stone"}},
    )

    for variant in VARIANTS:
        name = f"{variant}_statue"
        write_json(
            os.path.join(ASSETS, f"blockstates/{name}.json"),
            {"variants": {"": {"model": "ironhold:block/statue"}}},
        )
        write_json(
            os.path.join(ASSETS, f"items/{name}.json"),
            {"model": {"type": "minecraft:model", "model": f"ironhold:item/{name}"}},
        )
        write_json(
            os.path.join(ASSETS, f"models/item/{name}.json"),
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"ironhold:item/{name}"},
            },
        )
        write_json(
            os.path.join(DATA, f"loot_table/blocks/{name}.json"),
            {
                "type": "minecraft:block",
                "pools": [
                    {
                        "rolls": 1,
                        "entries": [{"type": "minecraft:item", "name": f"ironhold:{name}"}],
                        "conditions": [{"condition": "minecraft:survives_explosion"}],
                    }
                ],
            },
        )
        make_icon(variant)
        print(f"  {name}: blockstate, item def, item model, loot, icon")


if __name__ == "__main__":
    main()
