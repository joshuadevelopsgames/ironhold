# Ironhold Bridge — Blockbench plugin

Single-file Blockbench plugin (`ironhold_bridge.js`) that opens a localhost
HTTP server inside Blockbench so external processes can drive the editor.

Used by [`tools/bb-mcp`](../bb-mcp/) to let Claude operate Blockbench.

## Manual install

If you didn't run `tools/bb-mcp/install.sh`, copy or symlink the plugin into
Blockbench's plugin directory:

| OS | Plugin directory |
|---|---|
| macOS | `~/Library/Application Support/Blockbench/plugins/` |
| Linux | `~/.config/Blockbench/plugins/` |
| Windows | `%APPDATA%\Blockbench\plugins\` |

```bash
ln -s "$(pwd)/ironhold_bridge.js" "$HOME/Library/Application Support/Blockbench/plugins/"
```

Restart Blockbench. The bridge auto-starts on launch and listens on
`127.0.0.1:31173`. Toggle from **File → Tools → Ironhold Bridge: Toggle**.

## Wire protocol

Every operation is `POST /rpc` with body:

```json
{ "method": "<name>", "params": { ... } }
```

Response is either `{"result": ...}` (200) or `{"error": "..."}` (4xx/5xx).

See [`tools/bb-mcp/server.py`](../bb-mcp/server.py) for the canonical list
of methods.

## Coordinate system

Blockbench / Bedrock convention: **Y up**, entity feet at world `y=0`. This
matches what `bb_add_cube` and `bb_update_cube` accept. If you're translating
between Mojang's Java-side `addBox` calls and Bedrock coords, the formula is
documented in [`tools/geo_to_java.py`](../geo_to_java.py).

## Security

127.0.0.1-only, no authentication. Local dev tool. Don't expose to the
network. Toggle the bridge off when not in use if that bothers you.
