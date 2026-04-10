Ironhold Shader Compatibility — Bliss / Complementary / BSL
===========================================================

To get colored (purple) light from Ironhold blocks when using
Bliss Shader or other shader packs with colored lighting support:

1. Open your shader pack's "shaders/block.properties" file.

2. Find the line for crying_obsidian (purple light) — it looks like:
     block.XXX=crying_obsidian

3. Add Ironhold's purple-emitting blocks to that same line:
     block.XXX=crying_obsidian ironhold:bat_flower ironhold:ebony_crying_obsidian

   This makes those blocks emit the same purple light as crying obsidian.

4. Save and reload shaders (F3+R in-game).

Note: The arcane scepter's held-item glow uses NeoForge's
AuxiliaryLightManager (vanilla white light). Colored held-item
light requires shader-side support for entity light emission,
which most shader packs do not yet support.
