#!/usr/bin/env bash
# Upload the built mod jar to Folium via SFTP.
#
# Removes any existing ironhold-*.jar in the remote mods/ dir before uploading
# the newest build, so old versions don't pile up (and duplicate mod IDs can't
# crash the server). Mirrors the local-Modrinth cleanup done at the bottom.
#
# Auth methods, in priority order:
#   1. SSH key — if you've added ~/.ssh/id_ed25519.pub to your Folium panel's
#      SSH keys, this works passwordless.
#   2. Password — set SFTP_PASS in the environment. Uses `expect` so the
#      password never appears in CLI args or process listings.
#
# Usage:
#   bash scripts/deploy-mod.sh                         # SSH-key auth
#   SFTP_PASS='...' bash scripts/deploy-mod.sh         # password auth
#
# After upload, restart the MC server from the Folium panel.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# Auto-discover the built jar so we don't have to update this when mod_version bumps.
# Picks the most recently modified ironhold-*.jar — works regardless of version suffix.
if [[ -z "${JAR_PATH:-}" ]]; then
  JAR_PATH="$(ls -t "$REPO_DIR/build/libs/"ironhold-*.jar 2>/dev/null | head -n1 || true)"
fi

SFTP_HOST="${SFTP_HOST:-n-cal-3.folium.host}"
SFTP_PORT="${SFTP_PORT:-2022}"
SFTP_USER="${SFTP_USER:-hpbob3q5.9cc1bf35}"
REMOTE_DIR="${REMOTE_DIR:-mods}"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "error: jar not found at $JAR_PATH — run ./gradlew build first" >&2
  exit 1
fi

echo "==> Removing old ironhold-*.jar and uploading $(basename "$JAR_PATH") to $SFTP_USER@$SFTP_HOST:$REMOTE_DIR/"

if [[ -n "${SFTP_PASS:-}" ]]; then
  command -v expect >/dev/null || { echo "error: 'expect' not installed" >&2; exit 1; }
  # log_user 0 suppresses sftp output (which includes the password prompt echo).
  expect <<EOF
    log_user 0
    set timeout 30
    spawn sftp -P $SFTP_PORT -o StrictHostKeyChecking=accept-new $SFTP_USER@$SFTP_HOST
    expect {
      "assword:" { send "\$env(SFTP_PASS)\r" }
      timeout    { puts "timeout waiting for password prompt"; exit 1 }
    }
    expect {
      "sftp>" { }
      timeout { puts "timeout after auth — wrong password?"; exit 1 }
    }
    send "cd $REMOTE_DIR\r"
    expect "sftp>"
    send "rm ironhold-*.jar\r"
    expect "sftp>"
    send "put \"$JAR_PATH\"\r"
    set timeout 600
    expect {
      "sftp>" { }
      timeout { puts "timeout during upload"; exit 1 }
    }
    send "bye\r"
    expect eof
EOF
  echo "    upload complete"
else
  # SSH-key auth path
  sftp -P "$SFTP_PORT" -o StrictHostKeyChecking=accept-new "$SFTP_USER@$SFTP_HOST" <<EOF
cd $REMOTE_DIR
-rm ironhold-*.jar
put $JAR_PATH
bye
EOF
fi

# Also drop a copy into the local Modrinth profile so the client picks up the
# same jar version. Override via LOCAL_MODS_DIR. Removes any existing
# ironhold-*.jar in that folder first so duplicate mod IDs don't crash the client.
LOCAL_MODS_DIR="${LOCAL_MODS_DIR:-$HOME/Library/Application Support/ModrinthApp/profiles/neoforge 26.1.1/mods}"
if [[ -d "$LOCAL_MODS_DIR" ]]; then
  find "$LOCAL_MODS_DIR" -maxdepth 1 -name 'ironhold-*.jar' -delete
  cp "$JAR_PATH" "$LOCAL_MODS_DIR/"
  echo "    copied to $LOCAL_MODS_DIR/$(basename "$JAR_PATH")"
fi

echo
echo "Done. Now restart the MC server from the Folium panel."
