#!/usr/bin/env bash
# Pulls every .isc structure file off the Folium MC server into the local
# repo so Claude (or the user) can read and analyse them.
#
# The Pterodactyl SFTP user lands inside the server's container root, so
# the world's structures live at:
#     <world>/ironhold/structures/<name>.isc
#
# We mirror that into:
#     <repo>/structures/<name>.isc        (flat — name only)
#
# Usage:
#   bash scripts/fetch-structures.sh                       # SSH-key auth
#   SFTP_PASS='...' bash scripts/fetch-structures.sh       # password auth
#
# Optional overrides:
#   REMOTE_WORLD=world1     # default: "world"
#   LOCAL_DIR=./structures  # default: "<repo>/structures"

set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_DIR="${LOCAL_DIR:-$REPO_DIR/structures}"
mkdir -p "$LOCAL_DIR"

SFTP_HOST="${SFTP_HOST:-n-cal-3.folium.host}"
SFTP_PORT="${SFTP_PORT:-2022}"
SFTP_USER="${SFTP_USER:-hpbob3q5.9cc1bf35}"
REMOTE_WORLD="${REMOTE_WORLD:-world}"
REMOTE_DIR="${REMOTE_WORLD}/ironhold/structures"

echo "==> Mirroring $SFTP_USER@$SFTP_HOST:$REMOTE_DIR/ → $LOCAL_DIR/"

run_sftp() {
  if [[ -n "${SFTP_PASS:-}" ]]; then
    command -v expect >/dev/null || { echo "error: 'expect' not installed" >&2; exit 1; }
    expect <<EOF
      log_user 0
      set timeout 60
      spawn sftp -P $SFTP_PORT -o StrictHostKeyChecking=accept-new $SFTP_USER@$SFTP_HOST
      expect "assword:" { send "\$env(SFTP_PASS)\r" }
      expect "sftp>"
      send "lcd $LOCAL_DIR\r"
      expect "sftp>"
      send "cd $REMOTE_DIR\r"
      expect "sftp>"
      send "mget *.isc\r"
      set timeout 600
      expect "sftp>"
      send "bye\r"
      expect eof
EOF
  else
    sftp -P "$SFTP_PORT" -o StrictHostKeyChecking=accept-new "$SFTP_USER@$SFTP_HOST" <<EOF
lcd $LOCAL_DIR
cd $REMOTE_DIR
mget *.isc
bye
EOF
  fi
}

run_sftp

echo
echo "Local copies:"
ls -lh "$LOCAL_DIR"/*.isc 2>/dev/null || echo "  (no .isc files were fetched)"
