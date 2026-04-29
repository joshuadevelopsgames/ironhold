# bb-mcp — Blockbench bridge for Claude Code

Lets Claude (the CLI) drive Blockbench programmatically. Same architecture as
the popular [`blender-mcp`](https://github.com/ahujasid/blender-mcp): a plugin
inside the editor exposes a localhost JSON-RPC server, an MCP server proxies
between that and Claude.

```
Claude Code  ──MCP/stdio──▶  server.py  ──HTTP localhost:31173──▶  Blockbench plugin
```

## Install

```bash
./tools/bb-mcp/install.sh
```

The script:
1. Symlinks `tools/blockbench-plugin/ironhold_bridge.js` into Blockbench's
   plugins directory (macOS / Linux / Windows-Git-Bash all handled).
2. `pip install --user` the deps for the MCP server (`mcp`, `httpx`).
3. Prints the JSON snippet to add to your Claude Code MCP config.

After running it, **restart Blockbench** (so it loads the plugin) and
**restart Claude Code** (so it picks up the new MCP server).

## Usage

In Claude:

```
bb_ping                   → confirm bridge is up
bb_status                 → see what's currently open
bb_open_project(path=...) → load a .bbmodel or .geo.json
bb_list_outliner          → tree of cubes + groups
bb_add_cube(...)          → create geometry
bb_set_texture_pixel(...) → paint a single pixel
bb_screenshot(path=...)   → visual verification
```

## Tool reference

### Connectivity

| Tool | Returns |
|---|---|
| `bb_ping` | `{ok, version, blockbench}` |
| `bb_status` | `{project_open, project_path, project_name, dirty, cube_count, group_count, texture_count, selected_uuids}` |

### Project

| Tool | Args |
|---|---|
| `bb_open_project` | `path` (abs) |
| `bb_save_project` | — (project must already have save_path) |
| `bb_save_project_as` | `path` (abs) |
| `bb_export_geo_json` | `path` (abs) — Bedrock format |

### Outliner / cubes / groups

| Tool | Args |
|---|---|
| `bb_list_outliner` | — |
| `bb_get_element` | `uuid_or_name` |
| `bb_add_cube` | `name`, `origin[3]`, `size[3]`, `uv[2]?`, `parent?`, `mirror?`, `rotation?`, `pivot?` |
| `bb_update_cube` | `uuid_or_name`, plus any subset of cube fields |
| `bb_delete_element` | `uuid_or_name` |
| `bb_add_group` | `name`, `pivot?`, `rotation?`, `parent?` |

### Textures

| Tool | Args |
|---|---|
| `bb_list_textures` | — |
| `bb_load_texture_from_file` | `path` (abs), `name?` |
| `bb_set_texture_pixel` | `texture` (uuid/name), `x`, `y`, `rgba[4]` 0-255 |
| `bb_set_texture_rect` | `texture`, `x`, `y`, `width`, `height`, `rgba[4]` |
| `bb_save_texture` | `texture`, `path` (abs) |

### Visual verification

| Tool | Args |
|---|---|
| `bb_screenshot` | `path` (abs), `view?` (front/back/left/right/top/bottom), `width?`, `height?` |

## Coordinates

Blockbench uses **Bedrock convention** — Y up, entity feet at world y=0.
Our `PiglinVillagerModel.java` uses Mojang's Java convention (Y inverted).
The translation rule (already documented in `tools/geo_to_java.py`):

- `PartPose.offset (root)` = `(pivot_x, 24 - bedrock_y, pivot_z)`
- `addBox y` = `bone_pivot_y_bedrock - cube_origin_y - cube_h`

So when adding cubes via `bb_add_cube`, use Bedrock-style world coords, not
Java-style negative Y.

## Security

The plugin binds **127.0.0.1 only** with no authentication. That's fine for a
local dev tool — anyone with shell access on your box can already do worse.
Don't expose port 31173 to the network.

To temporarily disable: File → Tools → **Ironhold Bridge: Toggle**.

## Troubleshooting

- `Cannot reach Blockbench bridge at http://127.0.0.1:31173/rpc`
  → Blockbench isn't running, or the plugin didn't load, or the bridge was
    toggled off. Open Blockbench → File → Tools → Ironhold Bridge: Toggle.
- Plugin not appearing in the plugin list
  → Did `install.sh` run with the right HOME? Check the symlink with
    `ls -la ~/Library/Application\ Support/Blockbench/plugins/` (macOS).
- `bb_open_project` returns "Blockbench could not read file"
  → Path needs to be absolute; check the file extension is recognized
    (.bbmodel, .geo.json, .json with project format).
- Tools like `bb_add_cube` / `bb_set_texture_pixel` succeed but the model
  shows nothing
  → No texture is bound to the project. Use `bb_load_texture_from_file`
    first, or open a `.bbmodel` that already has textures.
