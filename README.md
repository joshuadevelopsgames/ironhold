# Kingdom 2.0 (NeoForge mod)

**Minecraft `1.21.11`** · **NeoForge** (`gradle.properties` → `neo_version`) · **Java 21**

Playable / testable dev runs:

```bash
./gradlew runClient    # client with mod loaded
./gradlew runServer    # dedicated test server (then connect via localhost)
./gradlew build        # output: build/libs/kingdom20-<version>.jar
```

Planning docs live in **`../Kingdom SMP 2.0/`** (markdown). This folder is the **implementation**;  
**Minecraft 26.1** remains a future port when mod support matures.

### Implemented from the markdowns (stubs)

1. **Player RPG data** — `PlayerKingdomRpgData` attachment (kingdom index 0–3, class, class level + XP bar), **persists**, **copies on death**, and **syncs to the client** (HUD / inventory weight cap match the server).  
2. **Kingdom pooled XP + gates** — `KingdomWorldData` on the **Overworld**; **Nether/End travel blocked** until any kingdom’s pool hits thresholds (`KingdomWorldData.NETHER_UNLOCK_XP` / `END_UNLOCK_XP`).  
3. **Encumbrance** — tag-based **carry weight** vs class max; **movement speed** penalty + **Slowness** when over cap; **HUD + inventory** show weight (no hotbar spam).  
4. **Class stats** — Per-class **max health, attack damage, move speed** (Knight slower, Ranger/Rogue faster per class GUI) and **Rogue attack speed**; **every 5 class levels** adds a tier: +1 heart max, +0.25 attack, +1% move (stacks with class move bonus). Encumbrance still applies its own speed penalty when over carry.  
5. **XP bar** — vanilla bar is **driven by class level + class XP** each tick (not the same as vanilla mob XP totals). **XP to next level** = `40 + classLevel * 26` (softer than ×30).  
6. **Class XP from combat** — `ClassXpKillRewards`: **monsters** scale with max health (capped); **bosses** (dragon / wither / warden / elder guardian) large flat XP; **other mobs** (animals, ambient, water) **1–2** XP; **villagers & armor stands** give none. **PVP** grants XP by **killer’s class** (Rogue highest); **Rogue** gets **+40** when the kill is from **behind** (rear arc). **Class-favored targets** (`data/kingdom20/tags/entity_type/*_favored.json`) grant **~+30%** class XP (not bosses / not PVP). All use `RpgProgressionActions` (kingdom pool + sync).  
7. **Patrol spawns** — `KingdomPatrolSpawnHandler`: ~**4%** of eligible overworld **natural-style** spawns (see `patrol_buff_candidates` tag) get **+6 max HP**, **iron sword**, a **kingdom-style name**, and **`BubbleOwner`** = entity UUID (for **Notable Bubble Text** `/bubbletext`, if you add that mod).

**Cheats / OP:** `/k2` (requires gamemaster permission — e.g. cheats on singleplayer).

| Command | Purpose |
|--------|---------|
| `/k2 whoami` | Show kingdom, class, level, XP into level |
| `/k2 class set <peasant\|knight\|…>` | Set class |
| `/k2 kingdom set <0-3>` | Set kingdom index |
| `/k2 classxp add <amount> [players]` | Add class XP (also adds same amount to that player’s kingdom pool) |
| `/k2 classxp remove <amount>` | Remove class XP (can de-level; floors at L1) |
| `/k2 kingdomxp add <0-3> <amount>` | Add pooled kingdom XP only |
| `/k2 gates` | Show pool totals and Nether/End lock state |
| `/k2 weight` | Show computed carry weight vs class cap (debug) |
| `/k2 classgui` / `kingdomgui` / `profile` | Open selection / profile screens (syncs RPG packet first) |

**Quick test:** `/k2 gates` → `/k2 kingdomxp add 0 500` → Nether portal should work; open inventory (**E**) — **Weight: current/max** appears to the **right** of the panel; bottom-right HUD shows the same when no menu is open (class + XP bar stacked above it).

---

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
