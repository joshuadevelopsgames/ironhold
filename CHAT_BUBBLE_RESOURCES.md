# Chat above heads (not global chat) — options

## 1. Chat Bubbles – NeoForge (jvitor)

- **CurseForge:** https://www.curseforge.com/minecraft/mc-mods/chat-bubbles-neoforge  
- **What it does:** Puts **player** chat in bubbles over **players** (RP / SMP). **Client + server** both need the mod.  
- **Mob lines:** The page describes **player** messages and `/chatbubbles` styling — **not** documented as “any mob says a line.” Treat **mob dialogue as unconfirmed** until you test or ask the author.  
- **Versions:** Listed for **1.21.1** NeoForge; check a **1.21.11** build or try the latest jar on your version.

## 2. Notable Bubble Text (NBT) — Mrbysco *(mob-friendly)*

- **GitHub (MIT):** https://github.com/mrbysco/NotableBubbleText  
- **CurseForge:** https://www.curseforge.com/minecraft/mc-mods/nbt  
- **What it does:** Bubbles for **player chat**, plus **`/bubbletext <author> <message>`** for entities that carry a **string tag** matching `author` (default key **`BubbleOwner`**; configurable).  
- **Example (older README used ForgeData):** On NeoForge **1.21** use the mod’s current docs / in-game for the correct persistent-data path (often NeoForge’s data attachment style may differ from `{ForgeData:{...}}` — verify in **config + source** for your MC version).  
- **Good fit for Kingdom SMP:** Spawn your mob with a **unique `BubbleOwner` id**, then show lines via **`/bubbletext`** or by **calling the same logic from your mod** later (soft dependency / copy pattern).

## 3. Rolling your own (kingdom20)

- Render short-lived **billboard text** in **`RenderNameTagEvent`** or entity **render layer** (client-only), driven by **synced attachment** or **entity data**. No extra mod; more work, full control.

## Practical pick

- **Players only, pretty UI, per-player colors:** try **Chat Bubbles – NeoForge**.  
- **Mobs must speak over their heads:** **Notable Bubble Text** is the one with **documented mob + author tag** support; **verify 1.21.11** on Modrinth/Curse files.

Cursor rule: `.cursor/rules/chat-bubble-resources.mdc`.

## kingdom20 patrol mobs + Notable Bubble Text

Rare patrol buffs set **`BubbleOwner`** on the entity to **`getUUID().toString()`** (same as vanilla UUID string). If your NBT mod expects a different key/path on NeoForge 1.21, change `KingdomPatrolSpawnHandler` or the mod config.
