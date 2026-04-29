# How to use these prompts

Each file in this folder is a self-contained brief for one sprite (or a
group of related sprites). Paste the prompt into your image model
(GPT image, Midjourney, Flux, etc.), generate, then run the result
through this cleanup pass:

1. Crop to the exact sprite size (16×16, 12×12, etc.).
2. Open in Aseprite.
3. *Sprite → Color Mode → Indexed*, palette = `palette.gpl`.
4. Hand-fix the outline (1px solid `#2A2A2A`, no gaps).
5. Hand-fix any anti-alias mush near edges.
6. Verify against `style-rules.md`.
7. Drop into the matching slot in `atlas-template.png`.

## Universal preamble

Prepend this paragraph to every prompt unless the sprite says otherwise:

> Pixel art icon, Minecraft-style, painted at exactly the target size
> (no upscaling). 1-pixel solid black outline (`#2A2A2A`). No
> anti-aliasing, no gradients, no soft shadows, no glow. Top-left light
> source. Use only these palette colors: `#2A2A2A #3F3F3F #5C5C5C
> #8E8E8E #BFBFBF #E6E6E6 #B8860B #FFCC44 #FFEE99 #C8B07A #5C4A2B
> #3A2D14 #55DD55 #CC3333 #8B1F1F #4477CC`. Transparent background.

The sprite-specific prompt below adds size, subject, and any
tile-ability or state constraints.

## Tip — generation strategy

Most image models still generate 256+ pixel images even when asked for
small ones. Prompt the *visual* in detail, generate at 512–1024px,
then aggressively downsample with **nearest-neighbor** to the target
size in Aseprite. Don't trust the model to produce a perfect 16×16
directly.
