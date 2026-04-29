#!/usr/bin/env bash
# install.sh — wire up the Ironhold Blockbench bridge end-to-end.
#
# 1. Symlinks the Blockbench plugin into Blockbench's plugin directory.
# 2. Installs Python dependencies for the MCP server (into the user site).
# 3. Prints the snippet to add to your Claude Code MCP config.
#
# Re-running is safe — the symlink is recreated and pip is idempotent.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PLUGIN_SRC="$REPO_ROOT/tools/blockbench-plugin/ironhold_bridge.js"
SERVER_PY="$REPO_ROOT/tools/bb-mcp/server.py"

case "$(uname -s)" in
  Darwin)  PLUGIN_DIR="$HOME/Library/Application Support/Blockbench/plugins" ;;
  Linux)   PLUGIN_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/Blockbench/plugins" ;;
  MINGW*|CYGWIN*|MSYS*) PLUGIN_DIR="$APPDATA/Blockbench/plugins" ;;
  *) echo "unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac

# 1. Symlink the plugin
mkdir -p "$PLUGIN_DIR"
LINK="$PLUGIN_DIR/ironhold_bridge.js"
if [ -L "$LINK" ] || [ -f "$LINK" ]; then rm -f "$LINK"; fi
ln -s "$PLUGIN_SRC" "$LINK"
echo "[1/3] Plugin file: $LINK -> $PLUGIN_SRC
       (Blockbench requires loading via Plugin Manager — see README)"

# 2. Python deps
echo "[2/3] Installing Python deps..."
python3 -m pip install --user --quiet -r "$REPO_ROOT/tools/bb-mcp/requirements.txt"
echo "      ok"

# 3. Print MCP config snippet
cat <<EOF

[3/3] Add this to your Claude Code MCP config (e.g. ~/.claude/settings.json
      or this project's .claude/settings.local.json):

  {
    "mcpServers": {
      "blockbench": {
        "command": "python3",
        "args": ["$SERVER_PY"]
      }
    }
  }

Restart Claude Code so it picks up the new MCP server, then in Blockbench:
  - Restart Blockbench (so it loads the plugin)
  - The bridge auto-starts on launch (File → Tools → Ironhold Bridge: Toggle
    to stop/start)
  - Test from Claude with: bb_ping  → expect {ok: true, ...}

EOF
