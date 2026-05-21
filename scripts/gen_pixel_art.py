#!/usr/bin/env python3
"""Generate pixel-art assets via gpt-image-2 + deterministic post-pipeline.

Pipeline:
  prompt -> gpt-image-2 (high-res) -> chroma-key (optional)
        -> area+nearest downscale -> palette quantize -> validate -> save

API key is loaded from $OPENAI_API_KEY, then from
~/.config/ironhold/openai.env (mode 600).

Examples:
  python3 scripts/gen_pixel_art.py \\
      --preset minecraft_item_32 \\
      --prompt "rusty iron key with a small red gem in the bow" \\
      --out scratch/iron_key.png

  python3 scripts/gen_pixel_art.py \\
      --preset minecraft_block_16 \\
      --prompt "mossy dark stone bricks, slightly damp" \\
      --out scratch/dark_brick.png --keep-source
"""

from __future__ import annotations

import argparse
import base64
import io
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

from PIL import Image
from openai import OpenAI


# ---------- Palettes ---------------------------------------------------------

# Broadly Minecraft-flavored 16-color palette. Tweak per asset family.
MINECRAFT_ITEM_PALETTE: list[tuple[int, int, int]] = [
    (0,   0,   0),
    (60,  40,  30),
    (110, 70,  40),
    (160, 110, 60),
    (210, 170, 110),
    (90,  90,  90),
    (140, 140, 140),
    (200, 200, 200),
    (240, 240, 240),
    (120, 30,  30),
    (200, 50,  40),
    (220, 140, 40),
    (240, 200, 60),
    (60,  100, 50),
    (40,  80,  130),
    (90,  50,  120),
]

PALETTES: dict[str, list[tuple[int, int, int]]] = {
    "minecraft_item": MINECRAFT_ITEM_PALETTE,
}


# ---------- Presets ----------------------------------------------------------

@dataclass(frozen=True)
class Preset:
    final_size: tuple[int, int]
    gen_size: tuple[int, int]          # must be multiple of 16; integer multiple of final_size
    palette_name: str
    max_colors: int
    alpha_mode: str                    # "binary" | "none"
    chroma_key_hex: Optional[str]
    style_block: str


PRESETS: dict[str, Preset] = {
    "minecraft_item_32": Preset(
        final_size=(32, 32),
        gen_size=(1024, 1024),         # 32x scale
        palette_name="minecraft_item",
        max_colors=16,
        alpha_mode="binary",
        chroma_key_hex="ff00ff",
        style_block=(
            "Style: true pixel art, vanilla Minecraft inspired, hard pixel edges, "
            "flat colors, no gradients, no glow, no soft shadows, no anti-aliasing. "
            "Composition: single centered subject, readable at 32x32 pixels. "
            "Background: pure solid magenta #ff00ff filling the entire canvas behind the subject. "
            "Do not use magenta or pink anywhere on the subject itself."
        ),
    ),

    "minecraft_item_64": Preset(
        final_size=(64, 64),
        gen_size=(1024, 1024),         # 16x scale
        palette_name="minecraft_item",
        max_colors=24,
        alpha_mode="binary",
        chroma_key_hex="ff00ff",
        style_block=(
            "Style: detailed pixel art in Minecraft style, hard pixel edges, "
            "flat colors with optional 1-pixel shading. No gradients, no glow, no soft shadows. "
            "Composition: single centered subject, readable at 64x64 pixels. "
            "Background: pure solid magenta #ff00ff filling the entire canvas behind the subject. "
            "Do not use magenta or pink anywhere on the subject itself."
        ),
    ),

    "minecraft_block_16": Preset(
        final_size=(16, 16),
        gen_size=(1024, 1024),         # 64x scale — expect rough output; cleanup likely
        palette_name="minecraft_item",
        max_colors=12,
        alpha_mode="none",
        chroma_key_hex=None,
        style_block=(
            "Style: true pixel art, vanilla Minecraft block face texture, "
            "tileable seamlessly on all four edges. Hard pixel edges, flat colors, "
            "no gradients, no glow, no shadows, no anti-aliasing. "
            "Composition: a square material surface, no centered subject, no border. "
            "Fully opaque, no transparent pixels."
        ),
    ),

    "minecraft_gui_256": Preset(
        final_size=(256, 256),
        gen_size=(2048, 2048),         # 8x scale
        palette_name="minecraft_item",
        max_colors=32,
        alpha_mode="binary",
        chroma_key_hex="ff00ff",
        style_block=(
            "Style: medieval RPG GUI panel, painted-pixel-art look, hard pixel edges, "
            "limited palette. No gradients, no glow, no soft shadows. "
            "Composition: a single decorative panel filling most of the canvas. "
            "Background outside the panel: pure solid magenta #ff00ff. "
            "Do not use magenta or pink anywhere on the panel itself."
        ),
    ),
}


# ---------- Key loading ------------------------------------------------------

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
    raise RuntimeError(
        "OPENAI_API_KEY not found in environment or "
        f"{env_path}"
    )


# ---------- Generation -------------------------------------------------------

def _build_prompt(user_prompt: str, preset: Preset) -> str:
    fw, fh = preset.final_size
    gw, gh = preset.gen_size
    sx, sy = gw // fw, gh // fh
    return (
        f"{user_prompt}\n\n"
        f"Target: a {fw}x{fh} pixel-art asset for a Minecraft NeoForge mod.\n"
        f"Render at {gw}x{gh}, treating each {sx}x{sy} block of source pixels "
        f"as exactly one logical pixel.\n"
        f"Every visual feature must occupy a whole number of logical pixels. "
        f"No feature smaller than one logical pixel. "
        f"No anti-aliased edges between logical pixels.\n"
        f"Limit distinct colors to at most {preset.max_colors}.\n\n"
        f"{preset.style_block}"
    )


def generate_source(client: OpenAI, user_prompt: str, preset: Preset,
                    quality: str) -> Image.Image:
    prompt = _build_prompt(user_prompt, preset)
    w, h = preset.gen_size
    resp = client.images.generate(
        model="gpt-image-2",
        prompt=prompt,
        size=f"{w}x{h}",
        quality=quality,
        n=1,
    )
    b64 = resp.data[0].b64_json
    if b64 is None:
        raise RuntimeError("API returned no b64_json image data")
    return Image.open(io.BytesIO(base64.b64decode(b64))).convert("RGBA")


# ---------- Post-pipeline ----------------------------------------------------

def chroma_key_to_alpha(img: Image.Image, hex_color: str,
                         tol: int = 28) -> Image.Image:
    target = tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    tr, tg, tb = target
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            if abs(r - tr) <= tol and abs(g - tg) <= tol and abs(b - tb) <= tol:
                px[x, y] = (0, 0, 0, 0)
    return rgba


def downscale(img: Image.Image, size: tuple[int, int]) -> Image.Image:
    """Two-step: area-average to 2x target, then nearest to target.

    Area averaging on the first step suppresses sub-pixel noise from the AI
    output. The final nearest-neighbor step locks everything to the target
    pixel grid with no anti-aliasing.
    """
    intermediate = (size[0] * 2, size[1] * 2)
    stepped = img.resize(intermediate, Image.Resampling.BOX)
    return stepped.resize(size, Image.Resampling.NEAREST)


def quantize_to_palette(img: Image.Image, palette: list[tuple[int, int, int]],
                         alpha_mode: str) -> Image.Image:
    rgba = img.convert("RGBA")
    alpha = rgba.getchannel("A")
    rgb = rgba.convert("RGB")

    pal_img = Image.new("P", (1, 1))
    flat: list[int] = []
    for r, g, b in palette:
        flat.extend([r, g, b])
    flat += [0, 0, 0] * (256 - len(palette))
    pal_img.putpalette(flat)

    indexed = rgb.quantize(palette=pal_img, dither=Image.Dither.NONE)
    out = indexed.convert("RGBA")

    if alpha_mode == "binary":
        binary = alpha.point(lambda a: 255 if a >= 128 else 0)
        out.putalpha(binary)
    elif alpha_mode == "none":
        out.putalpha(255)
    else:
        out.putalpha(alpha)
    return out


def validate(img: Image.Image, preset: Preset) -> list[str]:
    issues: list[str] = []
    if img.size != preset.final_size:
        issues.append(f"size {img.size} != expected {preset.final_size}")
    rgba = img.convert("RGBA")
    alpha_values = set(rgba.getchannel("A").get_flattened_data())
    if preset.alpha_mode == "binary" and not alpha_values.issubset({0, 255}):
        issues.append("non-binary alpha values present")
    if preset.alpha_mode == "none" and alpha_values != {255}:
        issues.append("expected fully opaque but found transparent pixels")
    colors = rgba.convert("RGB").getcolors(maxcolors=1 << 20) or []
    if len(colors) > preset.max_colors:
        issues.append(f"{len(colors)} distinct colors > max {preset.max_colors}")
    return issues


# ---------- Main -------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(
        description="Generate pixel-art assets via gpt-image-2.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("--prompt", required=True)
    ap.add_argument("--preset", required=True, choices=sorted(PRESETS))
    ap.add_argument("--out", required=True, help="Final PNG output path.")
    ap.add_argument("--quality", default="medium",
                    choices=["low", "medium", "high", "auto"])
    ap.add_argument("--keep-source", action="store_true",
                    help="Also save the raw high-res generation as <out>.source.png.")
    args = ap.parse_args()

    preset = PRESETS[args.preset]
    palette = PALETTES[preset.palette_name]

    client = OpenAI(api_key=load_api_key())
    print(f"[gen] preset={args.preset} quality={args.quality}")
    print(f"[gen] prompt={args.prompt!r}")
    src = generate_source(client, args.prompt, preset, args.quality)

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    if args.keep_source:
        src_path = out_path.with_name(out_path.stem + ".source.png")
        src.save(src_path)
        print(f"[gen] kept source -> {src_path}")

    work = src
    if preset.chroma_key_hex:
        work = chroma_key_to_alpha(work, preset.chroma_key_hex)

    small = downscale(work, preset.final_size)
    final = quantize_to_palette(small, palette, preset.alpha_mode)
    final.save(out_path)
    print(f"[gen] saved final -> {out_path}")

    issues = validate(final, preset)
    if issues:
        print("[gen] WARNING — validation issues:")
        for i in issues:
            print(f"  - {i}")
        return 2
    print("[gen] validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
