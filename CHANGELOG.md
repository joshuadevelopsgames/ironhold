# Changelog

All notable changes to Ironhold. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [1.64.1 → 1.82.3] — 2026-05-25

> Consolidated across the range. Intermediate patch versions (1.65.x–1.82.x) were
> shipped as build artifacts but not individually tracked in git, so this entry is a
> single diff of jar `1.64.1` → `1.82.3` plus the in-progress guillotine work.

### Added

- **Moon dimension** — a custom Moon dimension with its own dimension type, reached
  via a new **Moon Portal** block; adds **Moon Dust** and **Moon Stone** blocks and a
  *"Shoot for the Moon"* advancement.
- **Quest system** — full quest framework (`quest/`): definitions, objectives, rewards,
  progress tracking, boss-bar display, and per-world saved data.
- **Reactive music engine** — adaptive in-game music system (`music/`) with **61 streamed
  tracks** (`sounds/music/`), including PvP-escalation behavior.
- **Vampire Bat** — new flying mob (entity + renderer + texture), with biome-modifier
  spawns that replace vanilla bats.
- **Mirror** — placeable reflective Mirror item/entity (custom renderer + `mirror_surface`
  texture + crafting recipe).
- **Combat/bleeding effects** — Bleeding, Stifled Bleeding, and Kill Burst status effects;
  a **Bandage** item to stop bleeding; new combat particles (Iron Spark, Plague) and a
  `ModParticles` registry.
- **Enhanced Pickaxe** — pickaxe with auto-smelt on mined drops.
- **Fool's Gold Ore** — new ore block (+ deepslate variant) with smelting.
- **Emote system** — client emote support (`client/emote/`) with a *Point* emote keybind
  and networking payload.
- **Staff Zone** entity (area-effect support entity for staff items).
- **14 new mixins** — camera gravity, mirror hand/reflection rendering, chat screen,
  server-side permissions, player-list announcements, and more.

### Changed

- **Guillotine — falling blade + execution mechanic:**
  - Added the angled blade to the model and textured it across **all 11 wood variants**.
  - **Sneak + right-click** drops the blade; plain right-click still seats a player.
  - The blade falls with **gravity-accurate acceleration** (`easeInQuad`, i.e. d = ½·a·t²)
    and **slices all the way to the base** of the frame, with an impact rebound.
  - **Guaranteed insta-kill** of whoever is locked in — uses `genericKill` damage
    (`BYPASSES_INVULNERABILITY`), so it ignores totems, armor, invuln frames, and creative.
    Drops the victim's head (player skull with their skin, or matching mob skull) plus
    blood particles and an impact thud.
  - The blade **stays down** until reset; **sneak + right-click** resets it (instant
    teleport back up). Also triggerable by a **redstone** rising edge.

### Removed

- Old `arcane_scepter_e.png` item texture.
