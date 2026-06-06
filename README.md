# Ironhold

**Mod id `ironhold`** · Minecraft **26.1.2** · NeoForge **26.1.2.48-beta** · Java **21**

Ironhold is a large kingdom-themed RPG overhaul for Minecraft. It layers a class &
kingdom progression system, voiced AI NPCs, custom mobs and bosses, gear quality,
profession skill trees, seasons, fishing/blacksmithing minigames, built-in dynamic
lighting, and a custom terrain generator on top of vanilla survival.

It runs as a normal NeoForge mod — drop the jar in your `mods/` folder. Most features
work out of the box; the talking AI NPCs are the only part that needs API keys (see
[Configuration](#configuration)), and they degrade gracefully when no keys are set.

---

## What's in it

- **Kingdoms & classes** — pick a kingdom (0–3) and a class (Peasant, Knight, Ranger, Rogue, …). Class sets your max health, attack, and movement profile.
- **Class leveling** — earn class XP from combat (scaled by target, bonuses for bosses, rear-arc Rogue kills, and favored targets). Every 5 levels grants a stat tier. A reskinned XP bar tracks class level, not vanilla XP.
- **Kingdom gates** — the Nether and End stay sealed until a kingdom's *pooled* class XP crosses a threshold, so progression is a shared server goal.
- **Encumbrance** — items have weight; exceed your class carry cap and you get a movement penalty + Slowness. Current/max weight shows in the inventory and HUD.
- **Gear quality & condition** — tools/armor/weapons carry a quality tier (durability multiplier) and a wear condition; ores and ingots carry quality too.
- **Profession skill trees** — spend points across professions, unlock ranks and milestones.
- **Voiced AI NPCs** — characters like Kangarude hold real conversations: an LLM brain (via OpenRouter), ElevenLabs text-to-speech, and optional push-to-talk mic input (OpenAI Whisper). Talk by typing or by voice.
- **Custom content** — knights, mages, mimics, shulker castes, will-o'-wisps, possessed armor, a Void Invoker, plus seasons, dynamic lights ("Ironglow"), cooking, fishing & blacksmithing minigames, accessories, and structure scan/build tooling.

---

## Installing

**Requirements:** Minecraft `26.1.2` with NeoForge `26.1.2.48-beta` (or compatible), Java 21.

1. Install NeoForge for Minecraft 26.1.2.
2. Drop `ironhold-<version>.jar` into your `mods/` folder — on **both** the server and every client that connects.
3. Launch. On a dedicated server, the AI-NPC config file is generated on first run (see below).

---

## Commands

Ironhold registers only RPG / UI commands. Staff commands such as `/fly`, `/warp`, `/mute`, and `/whois` are **not** part of this mod — use the separate **AdminCommands** project if your server needs them (see that repo’s `README.md`).

| Command | Who | Purpose |
|---|---|---|
| `/menu` | Everyone | Open the main Ironhold menu |
| `/console` | Everyone | Open the King's Console screen |
| `/k2 …` | Gamemaster+ | RPG admin tree (class, kingdom, XP, levels, skills, gear, structures, spawn villagers, …) |

---

## Configuration

Ironhold's config is a **server-side** file (its API keys are never sent to clients). It's created on first run at:

```
<world>/serverconfig/ironhold-server.toml
```

- **Dedicated server:** `world/serverconfig/ironhold-server.toml`
- **Singleplayer:** `saves/<World Name>/serverconfig/ironhold-server.toml`

Because it's per-world, edit it while the server is stopped, then start up — or reload with vanilla `/reload`. You can also change RPG state live with the in-game `/k2` commands.

### AI NPCs need keys (everything else doesn't)

The talking NPCs use three optional services. Leave the keys blank and the NPCs simply stay quiet — the rest of the mod is unaffected.

| Service | What it powers | Get a key |
|---|---|---|
| **OpenRouter** | NPC "brains" (LLM replies) + Void Invoker taunts | <https://openrouter.ai> (has free models) |
| **ElevenLabs** | NPC text-to-speech voices | <https://elevenlabs.io> |
| **OpenAI** | Whisper speech-to-text for talking to NPCs by mic | <https://platform.openai.com> |

### Config options

| Key | Default | Purpose |
|---|---|---|
| `openrouterApiKey` | `""` | OpenRouter API key (NPC brains + Void Invoker) |
| `openrouterModel` | `mistralai/mistral-7b-instruct:free` | Model for Void Invoker taunts |
| `kangarudeOpenrouterModel` | `anthropic/claude-haiku-4.5` | Model for Kangarude (and voiced NPCs) |
| `kangarudeIdleTimeoutSeconds` | `25` | Seconds Kangarude waits for a reply before walking off (5–600) |
| `elevenlabsApiKey` | `""` | ElevenLabs API key (TTS) |
| `elevenlabsVoiceId` | `tdlj9WjgHdDTMKoAvBYQ` | ElevenLabs voice id |
| `elevenlabsModel` | `eleven_flash_v2_5` | TTS model (`eleven_flash_v2_5` fastest, `eleven_multilingual_v2` highest quality) |
| `openaiApiKey` | `""` | OpenAI API key (Whisper STT for mic input) |
| `openaiWhisperModel` | `whisper-1` | Whisper transcription model |
| `sttSilenceMs` | `1000` | Silence (ms) after speaking before audio is transcribed (200–5000) |

Example:

```toml
openrouterApiKey = "sk-or-..."
elevenlabsApiKey = "..."
openaiApiKey = "sk-..."
```

### Environment-variable overrides

For the secrets, an environment variable (if set and non-empty) **overrides** the config file — handy for keeping keys out of the world save. Supported:

```
OPENROUTER_API_KEY      ELEVENLABS_API_KEY      ELEVENLABS_VOICE_ID      OPENAI_API_KEY
```

---

## Building & deploying (developers)

```bash
./gradlew runClient    # launch a client with the mod loaded
./gradlew runServer    # launch a dedicated test server (connect via localhost)
./gradlew build        # output: build/libs/ironhold-<version>.jar
```

Deploy the freshly built jar to the live server (uploads via SFTP and mirrors a copy into the local Modrinth profile):

```bash
bash scripts/deploy-mod.sh                  # SSH-key auth
SFTP_PASS='…' bash scripts/deploy-mod.sh    # password auth
```

The script auto-discovers the newest `build/libs/ironhold-*.jar`, removes old `ironhold-*.jar` from the remote `mods/` dir, uploads, and reminds you to restart the server from the Folium panel. Bump `mod_version` in `gradle.properties` on every change.

---

## Resources

- NeoForged docs: <https://docs.neoforged.net/>
- NeoForged Discord: <https://discord.neoforged.net/>
- Mojang mapping license: <https://github.com/NeoForged/NeoForm/blob/main/Mojang.md>
