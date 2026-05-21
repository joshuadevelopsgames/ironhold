#!/usr/bin/env bash
# Uploads every local .isc file back to the Folium MC server so Claude's
# generated builds become available in-game via `/k2 struct build <name>`.
#
# Mirrors:
#     <repo>/structures/*.isc  →  <world>/ironhold/structures/
#
# Usage:
#   bash scripts/push-structures.sh                       # SSH-key auth
#   SFTP_PASS='...' bash scripts/push-structures.sh       # password auth
#
# Optional overrides:
#   REMOTE_WORLD=world1     # default: "world"
#   LOCAL_DIR=./structures  # default: "<repo>/structures"

set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_DIR="${LOCAL_DIR:-$REPO_DIR/structures}"

if ! ls "$LOCAL_DIR"/*.isc >/dev/null 2>&1; then
  echo "error: no .isc files in $LOCAL_DIR — nothing to upload" >&2
  exit 1
fi

SFTP_HOST="${SFTP_HOST:-n-cal-3.folium.host}"
SFTP_PORT="${SFTP_PORT:-2022}"
SFTP_USER="${SFTP_USER:-hpbob3q5.9cc1bf35}"
REMOTE_WORLD="${REMOTE_WORLD:-world}"
REMOTE_DIR="${REMOTE_WORLD}/ironhold/structures"

echo "==> Uploading $LOCAL_DIR/*.isc → $SFTP_USER@$SFTP_HOST:$REMOTE_DIR/"

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
    send "mput *.isc\r"
    set timeout 600
    expect "sftp>"
    send "bye\r"
    expect eof
EOF
else
  sftp -P "$SFTP_PORT" -o StrictHostKeyChecking=accept-new "$SFTP_USER@$SFTP_HOST" <<EOF
lcd $LOCAL_DIR
cd $REMOTE_DIR
mput *.isc
bye
EOF
fi

echo
echo "Done. Run /k2 struct list in-game to confirm."
