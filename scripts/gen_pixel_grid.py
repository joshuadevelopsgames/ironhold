#!/usr/bin/env python3
"""Generate pixel-art textures by having an LLM paint-by-numbers on a grid.

Palette and rules are derived empirically from actual vanilla Minecraft item
textures (leather_boots, iron_boots, iron_sword, stick, apple, bread, etc.).
The LLM output IS the final pixel art — no image generation, no downsampling.

Usage:
  python3 scripts/gen_pixel_grid.py --size 16 \\
      --prompt "iron key with a small red gem in the bow" \\
      --out scratch/iron_key_grid.png --upscale 16
"""

from __future__ import annotations

import argparse
import base64
import json
import mimetypes
import os
import sys
from pathlib import Path

from PIL import Image
from openai import OpenAI


# Vanilla-aligned palette. Lowercase = darker shade, uppercase = lighter shade
# of the same hue family. NO PURE BLACK — vanilla never uses (0,0,0).
PALETTE: dict[str, tuple[int, int, int, int]] = {
    "_": (0,   0,   0,   0),     # transparent
    # Warm wood / leather ramp — vanilla stick + bread tones
    "w": (40,  30,  11,  255),   # darkest warm — used as outline for warm subjects
    "W": (73,  54,  21,  255),   # dark warm
    "x": (101, 67,  33,  255),   # mid-dark warm
    "X": (145, 105, 65,  255),   # mid warm
    "h": (180, 140, 95,  255),   # light warm
    "H": (220, 180, 130, 255),   # highlight tan
    # Cool gray / iron ramp — vanilla iron_sword + iron_boots tones
    "s": (24,  24,  24,  255),   # darkest cool — outline for cool subjects (NOT black)
    "S": (53,  53,  53,  255),   # dark gray
    "g": (107, 107, 107, 255),   # mid gray
    "G": (167, 167, 167, 255),   # light gray
    "i": (215, 215, 215, 255),   # near-white
    # Accents (use sparingly — 2+ pixels min)
    "y": (220, 150, 20,  255),   # gold dark
    "Y": (250, 220, 80,  255),   # gold light
    "r": (180, 30,  30,  255),   # red (apple, gem)
    "b": (50,  150, 200, 255),   # cyan / diamond
}

PALETTE_LEGEND = """
_ = transparent

WARM RAMP (wood, leather, anything brown — pick darkest = w as the OUTLINE):
  w = #281e0b  darkest warm (outline shade)
  W = #493615  dark wood
  x = #654321  mid-dark wood
  X = #916941  mid wood
  h = #b48c5f  light wood
  H = #dcb482  highlight tan

COOL RAMP (iron, stone, white materials — pick darkest = s as the OUTLINE):
  s = #181818  darkest cool (outline shade — NOT pure black)
  S = #353535  dark gray
  g = #6b6b6b  mid gray
  G = #a7a7a7  light gray
  i = #d7d7d7  near-white / highlight

ACCENTS (≥2 pixels each; never as the main material):
  y = #dc9614  gold dark
  Y = #fadc50  gold light
  r = #b41e1e  red (gem, apple, blood)
  b = #3296c8  cyan / diamond
""".strip()


# Few-shot examples decoded from actual vanilla textures.
FEW_SHOT_EXAMPLES = """
EXAMPLE 1 — leather_boots (vanilla pattern: PAIR of small boots, upper half only,
outline in the material's mid-shade `g`, fills only ~20% of canvas):
{
  "plan": "Two small leather boots side-by-side in the upper half; warm-leather
  ramp with mid-gray outline; legs occupy ~4 cols each, ~7 rows tall.",
  "grid": [
    "________________",
    "________________",
    "________________",
    "____ggg__ggg____",
    "___giiS__giiS___",
    "___giiS__giGS___",
    "___giGS__gGGS___",
    "___gGGS__gGGS___",
    "___gGGS__gGGS___",
    "___GG______GG___",
    "________________",
    "________________",
    "________________",
    "________________",
    "________________",
    "________________"
  ]
}

EXAMPLE 2 — iron_sword (vanilla pattern: DIAGONAL axis top-left to bottom-right,
1-pixel-wide blade, dark outline = material's darkest shade `s` not pure black,
fills only ~30% of canvas, leaves generous transparent margins):
{
  "plan": "Diagonal sword from upper-right tip to lower-left hilt; iron-gray
  ramp s/S/G/i; warm wood handle x/X with w outline; pommel one cell.",
  "grid": [
    "_____________SSS",
    "____________Siis",
    "___________SiGis",
    "__________SiGis_",
    "_________SiGis__",
    "________SiGis___",
    "__SS___SiGis____",
    "__SgS_SiGis_____",
    "___SGsiGis______",
    "___SGGgis_______",
    "____SgSs________",
    "___WxsSSs_______",
    "__WXw_ssSs______",
    "SSxw____ss______",
    "Sgs_____________",
    "sss_____________"
  ]
}
""".strip()


def build_prompt(user_prompt: str, n: int) -> str:
    return f"""You are generating an item texture for vanilla Minecraft Java Edition.
The grid IS the final asset. No downsampling. Every cell counts.

PALETTE
=======
{PALETTE_LEGEND}

RULES — derived from analyzing actual vanilla 1.21 item textures
================================================================
1. NO PURE BLACK. Vanilla never uses RGB(0,0,0). For warm subjects (wood,
   leather, gold), the darkest shade is `w` (#281e0b). For cool subjects
   (iron, stone, white), the darkest shade is `s` (#181818). Use the dark
   shade as the silhouette outline.

2. OUTLINE = MATERIAL'S DARKEST OR MID-DARK SHADE, not a universal black.
   Vanilla outlines use the same hue family as the object. Iron boots
   outline with `S`, leather boots outline with `g`, swords outline with
   `s`/`S`. Pick the outline shade that fits the subject's material.

3. SPARSE FILLS — leave generous transparent margins.
   Vanilla items occupy only 20–50% of the canvas:
     • leather_boots: 50/256 cells (~20%)
     • iron_boots: 88/256 cells (~34%)
     • stick: 37/256 cells (~14%)
     • feather: 65/256 cells (~25%)
   Aim for ~30% fill. Tiny subjects centered in lots of empty space read
   better than crammed-full canvases.

4. 3 SHADES PER MATERIAL — max 4. Use the ramp consistently:
     • Warm material: pick from w / W / x / X / h / H
     • Cool material: pick from s / S / g / G / i
   Don't mix more than 4 shades from one ramp on one object.

5. SHADING DIRECTION: light from upper-left.
   Lighter shades on upper-left of forms, darker on lower-right.

6. DIAGONAL AXIS FOR TOOLS — vanilla swords, picks, axes, sticks, fishing
   rods all run from upper-right tip to lower-left handle. 1-pixel-wide
   blade/shaft. Diagonal stairs of pixels, not horizontal/vertical.

7. ARMOR ITEMS (boots / chestplate / helmet) — render as a PAIR (boots) or
   SYMMETRIC silhouette (helmet, chestplate). Boots are two small ~4-wide
   shapes side-by-side in the upper half only, separated by 2 transparent
   columns. Do NOT render boots as one big single boot filling the canvas.

8. ACCENTS — ≥2 cells each. A single accent pixel (a buckle, a gem) reads
   as noise, not as a feature. If the accent fits in 1 cell, omit it.

9. NO ANTI-ALIASING. Each cell is one solid palette character.

SUBJECT
=======
{user_prompt}

{FEW_SHOT_EXAMPLES}

OUTPUT FORMAT
=============
Return ONLY valid JSON in this exact shape:
{{
  "plan": "<one short sentence: silhouette + material/ramp choice + which rules apply>",
  "grid": [
    "<row 0, exactly {n} characters>",
    "<row 1, exactly {n} characters>",
    ...
  ]
}}
The "grid" array must contain exactly {n} strings, each exactly {n} chars long,
each char one of the palette keys. No prose outside the JSON."""


def load_api_key() -> str:
    if (k := os.environ.get("OPENAI_API_KEY")):
        return k
    env_path = Path.home() / ".config" / "ironhold" / "openai.env"
    if env_path.exists():
        for raw in env_path.read_text().splitlines():
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("export "):
                line = line[len("export "):]
            if "=" in line:
                k, v = line.split("=", 1)
                if k.strip() == "OPENAI_API_KEY":
                    return v.strip().strip("'\"")
    raise RuntimeError("OPENAI_API_KEY not found")


def encode_image_data_url(path: Path) -> str:
    mime = mimetypes.guess_type(path.name)[0] or "image/png"
    return f"data:{mime};base64,{base64.b64encode(path.read_bytes()).decode()}"


def build_initial_content(prompt_text: str, reference: Path | None
                          ) -> str | list[dict]:
    if reference is None:
        return prompt_text
    vision_note = (
        "\n\nA REFERENCE IMAGE is attached below. Treat it as visual ground "
        "truth for silhouette, pose, color emphasis, and which features matter. "
        "Translate it into vanilla Minecraft style per the rules above — "
        "do not copy it pixel-for-pixel; map its colors onto the vanilla "
        "palette ramps and its forms onto the sparse vanilla composition. "
        "Pick the warm or cool ramp based on the dominant hue in the reference."
    )
    return [
        {"type": "text", "text": prompt_text + vision_note},
        {"type": "image_url", "image_url": {"url": encode_image_data_url(reference)}},
    ]


def call_model(client: OpenAI, messages: list[dict], model: str) -> dict:
    resp = client.chat.completions.create(
        model=model,
        messages=messages,
        response_format={"type": "json_object"},
    )
    text = resp.choices[0].message.content
    if not text:
        raise RuntimeError("empty response from model")
    return json.loads(text)


def validate_grid(grid: list[str], n: int) -> list[str]:
    issues: list[str] = []
    if len(grid) != n:
        issues.append(f"expected {n} rows, got {len(grid)}")
    valid_chars = set(PALETTE.keys())
    for i, row in enumerate(grid[:n]):
        if len(row) != n:
            issues.append(f"row {i} length {len(row)} != {n}")
        bad = sorted(set(row) - valid_chars)
        if bad:
            issues.append(f"row {i} invalid chars: {bad}")
    return issues


def normalize_grid(grid: list[str], n: int) -> list[str]:
    fixed: list[str] = []
    for row in grid[:n]:
        if len(row) < n:
            row = row + "_" * (n - len(row))
        elif len(row) > n:
            row = row[:n]
        fixed.append(row)
    while len(fixed) < n:
        fixed.append("_" * n)
    return fixed


def render_grid(grid: list[str], n: int) -> Image.Image:
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    for y, row in enumerate(grid[:n]):
        for x, ch in enumerate(row[:n]):
            img.putpixel((x, y), PALETTE.get(ch, (0, 0, 0, 0)))
    return img


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--prompt", required=True)
    ap.add_argument("--size", type=int, default=16, choices=[16, 32, 64])
    ap.add_argument("--out", required=True)
    ap.add_argument("--model", default="gpt-5.5")
    ap.add_argument("--upscale", type=int, default=0,
                    help="If >0, also save a copy upscaled by this factor.")
    ap.add_argument("--max-retries", type=int, default=2)
    ap.add_argument("--reference", type=Path, default=None,
                    help="Optional reference image (jpg/png/webp). When given, "
                         "the model uses it as visual ground truth for silhouette "
                         "and colors, then translates into vanilla pixel-art style.")
    args = ap.parse_args()

    if args.reference is not None and not args.reference.exists():
        print(f"[grid] reference not found: {args.reference}", file=sys.stderr)
        return 1

    client = OpenAI(api_key=load_api_key())
    prompt = build_prompt(args.prompt, args.size)
    print(f"[grid] size={args.size} model={args.model}"
          + (f" reference={args.reference}" if args.reference else ""))
    print(f"[grid] prompt={args.prompt!r}")

    initial_content = build_initial_content(prompt, args.reference)
    messages = [{"role": "user", "content": initial_content}]
    data = call_model(client, messages, args.model)
    plan = data.get("plan", "")
    grid = data.get("grid", [])
    print(f"[grid] plan: {plan}")

    issues = validate_grid(grid, args.size)
    attempts = 0
    while issues and attempts < args.max_retries:
        attempts += 1
        print(f"[grid] retry {attempts}/{args.max_retries} — {issues}")
        messages.append({"role": "assistant", "content": json.dumps(data)})
        messages.append({"role": "user", "content": (
            "Your grid has these problems: "
            + "; ".join(issues)
            + f". Return the corrected JSON with exactly {args.size} rows, "
            f"each exactly {args.size} characters long, using only palette keys."
        )})
        data = call_model(client, messages, args.model)
        plan = data.get("plan", plan)
        grid = data.get("grid", [])
        issues = validate_grid(grid, args.size)

    if issues:
        print("[grid] WARNING — unresolved issues:")
        for i in issues:
            print(f"  - {i}")

    grid = normalize_grid(grid, args.size)
    img = render_grid(grid, args.size)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)
    print(f"[grid] saved -> {out_path}")

    if args.upscale > 0:
        big = img.resize(
            (args.size * args.upscale, args.size * args.upscale),
            Image.Resampling.NEAREST,
        )
        big_path = out_path.with_name(f"{out_path.stem}.x{args.upscale}.png")
        big.save(big_path)
        print(f"[grid] saved upscaled -> {big_path}")

    return 0 if not issues else 2


if __name__ == "__main__":
    sys.exit(main())
