#!/usr/bin/env bash
# Upload the /admin vanilla datapack to the Folium server via SFTP.
#
# Drops `admin_datapack/` into the server's `world/datapacks/` directory. A
# plain recursive put overwrites existing files in place; orphaned function
# files left over from a previous layout are harmless (nothing references them).
#
# Auth (same as deploy-mod.sh):
#   1. SSH key if your Folium panel has your public key.
#   2. Password via SFTP_PASS (uses `expect` so it never hits CLI args).
#
# Usage:
#   ( source ~/.config/ironhold/folium.env && export SFTP_PASS && bash scripts/deploy-datapack.sh )
#
# After upload, run `/reload` in-game (or restart) and `/datapack list` to confirm
# `file/admin_datapack` is enabled. Then `/function admin:toggle`.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DATAPACK_NAME="${DATAPACK_NAME:-admin_datapack}"
LOCAL_DATAPACK="$REPO_DIR/$DATAPACK_NAME"

SFTP_HOST="${SFTP_HOST:-n-cal-3.folium.host}"
SFTP_PORT="${SFTP_PORT:-2022}"
SFTP_USER="${SFTP_USER:-hpbob3q5.9cc1bf35}"
# World datapacks live under <level-name>/datapacks — "world" is the default level-name.
REMOTE_DATAPACKS="${REMOTE_DATAPACKS:-world/datapacks}"

if [[ ! -f "$LOCAL_DATAPACK/pack.mcmeta" ]]; then
  echo "error: $LOCAL_DATAPACK/pack.mcmeta not found — run scripts/gen_admin_datapack.py first" >&2
  exit 1
fi

echo "==> Uploading $DATAPACK_NAME → $SFTP_USER@$SFTP_HOST:$REMOTE_DATAPACKS/$DATAPACK_NAME/"

if [[ -n "${SFTP_PASS:-}" ]]; then
  command -v expect >/dev/null || { echo "error: 'expect' not installed" >&2; exit 1; }
  expect <<EOF
    log_user 0
    set timeout 30
    set pass \$env(SFTP_PASS)
    spawn sftp -P $SFTP_PORT -o StrictHostKeyChecking=accept-new $SFTP_USER@$SFTP_HOST
    expect {
      "assword:" { send "\$pass\r" }
      timeout    { puts "timeout waiting for password prompt"; exit 1 }
    }
    expect {
      "sftp>" { }
      timeout { puts "timeout after auth — wrong password?"; exit 1 }
    }
    send "lcd $REPO_DIR\r"
    expect "sftp>"
    send "cd $REMOTE_DATAPACKS\r"
    expect "sftp>"
    send "put -r $DATAPACK_NAME\r"
    set timeout 600
    expect {
      "sftp>" { }
      timeout { puts "timeout during upload"; exit 1 }
    }
    log_user 1
    send "ls $DATAPACK_NAME\r"
    expect "sftp>"
    send "ls $DATAPACK_NAME/data/admin/function\r"
    expect "sftp>"
    log_user 0
    send "bye\r"
    expect eof
EOF
  echo "    upload complete"
else
  sftp -P "$SFTP_PORT" -o StrictHostKeyChecking=accept-new "$SFTP_USER@$SFTP_HOST" <<EOF
lcd $REPO_DIR
cd $REMOTE_DATAPACKS
put -r $DATAPACK_NAME
ls $DATAPACK_NAME
bye
EOF
fi

echo
echo "Done. In-game (as op): run  /reload  then  /datapack list  to confirm,"
echo "then  /function admin:toggle"
