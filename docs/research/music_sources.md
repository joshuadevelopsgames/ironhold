# Royalty-free music sources for the reactive music engine + bard instruments

Researched 2026-06-10. Goal: tracks we can **redistribute inside the mod jar** (so licensing must
allow embedding, not just streaming), covering the three reactive-engine buckets
(`kingdom.smp.music` — exploration / escalation / PvP-climax) plus note samples for bard
instruments (spec `specs/fantasia-ports/11-bard-instruments.md`).

## License ground rules

| License | Embed in jar? | Attribution? | Notes |
|---|---|---|---|
| CC0 / public domain | ✅ | none | safest — prefer this |
| CC-BY 4.0 | ✅ | required | add a `CREDITS.md` in the jar + curseforge/modrinth page |
| Pixabay Content License | ✅ as part of the mod | none | OK embedded in a larger work; may NOT ship as standalone audio files — a mod jar qualifies as embedded, but a separately-downloadable "music pack" zip would not |
| CC-BY-NC | ⚠️ avoid | — | non-commercial only; murky if the SMP ever takes donations |

## Recommended sources by engine bucket

### 1. Ambient atmospheric (exploration default)
- **OpenGameArt CC0 music** — filter Music + CC0: https://opengameart.org/content/cc0-music-0
  and https://opengameart.org/content/cc0-fantasy-music-sounds . Quality varies; best for sparse
  drones/textures that sit under gameplay.
- **Fantasy Music Mega Pack (Blacis, itch.io, CC0)** — 100+ fantasy/cinematic/ambience tracks:
  https://blacis.itch.io/royalty-free-music-megapack . Big single grab; curate hard.
- **Fantasy Game Music Tracks (kmontesdev, CC0)** — 7 mystical/ethereal tracks:
  https://kmontesdev.itch.io/7-fantasy-music-tracks

### 2. Medieval folk / tavern (villages, taverns; also bard-adjacent)
- **Pixabay medieval/tavern search** — https://pixabay.com/music/search/medieval/ and
  https://pixabay.com/music/search/tavern%20music/ . No attribution, embeddable
  (license summary: https://pixabay.com/service/license-summary/).
- **Kevin MacLeod / incompetech (CC-BY)** — the classic; medieval collection:
  https://incompetech.com/music/royalty-free/index.html?collection=003 . CC-BY → credit line.
- **Free-Stock-Music medieval (mostly CC-BY)** — https://www.free-stock-music.com/search.php?keyword=medieval

### 3. Dark orchestral fantasy (combat escalation / PvP climax / boss)
- **Blacis Mega Pack (CC0, above)** — has cinematic/battle material; first stop.
- **itch.io tag sweep** — CC0+music: https://itch.io/game-assets/free/tag-cc0/tag-music and
  dark-fantasy+music: https://itch.io/game-assets/free/tag-dark-fantasy/tag-music
  (check each pack's license individually; itch packs are usually CC0 or CC-BY).
- **OpenGameArt** battle themes filtered CC0/CC-BY.

### 4. Bard instrument note samples (the .ogg-per-note kind)
- **VSCO 2 Community Edition — CC0, ~3 GB orchestral samples** —
  https://versilian-studios.com/vsco-community/ + https://github.com/sgossner/VSCO-2-CE .
  Explicitly: no attribution, no restrictions on redistribution; designed as a building block
  for game audio. Has harp, harpsichord (lute-ish), recorder, percussion → exactly what
  bard instruments need (one .ogg per pitch, vanilla note-block style).
- **VCSL (same author, CC0)** — https://github.com/sgossner/VCSL — additional instruments.

## Suggested plan

1. Pull VSCO 2 CE harp/recorder/harpsichord notes → bard instruments unblocked (CC0, zero credits).
2. Curate ~12 tracks: 4 ambient (CC0), 4 folk/tavern (Pixabay/incompetech), 4 combat-escalation
   (CC0 packs) → normalize loudness, encode mono-ish low-bitrate .ogg to keep the jar small.
3. Wire into `ReactiveSongbook` buckets; ship `CREDITS.md` listing every track + license
   (required for CC-BY, good citizenship for the rest).
4. Keep per-track provenance in this file as tracks are adopted.
