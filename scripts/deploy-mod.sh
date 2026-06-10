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
# Resolve the exact current version. Picking the newest mtime is unsafe because an old
# jar can become "newest" after copying, touching, or restoring build outputs.
if [[ -z "${JAR_PATH:-}" ]]; then
  MOD_VERSION="$(sed -n 's/^mod_version=//p' "$REPO_DIR/gradle.properties" | tail -n1)"
  JAR_PATH="$REPO_DIR/build/libs/ironhold-$MOD_VERSION.jar"
fi

SFTP_HOST="${SFTP_HOST:-n-cal-3.folium.host}"
SFTP_PORT="${SFTP_PORT:-2022}"
SFTP_USER="${SFTP_USER:-hpbob3q5.9cc1bf35}"
REMOTE_DIR="${REMOTE_DIR:-mods}"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "error: jar not found at $JAR_PATH — run ./gradlew build first" >&2
  exit 1
fi

# Fail before deployment if the archive is corrupt or lacks the declared mod entrypoint.
if ! unzip -tq "$JAR_PATH" >/dev/null; then
  echo "error: invalid or corrupt jar: $JAR_PATH" >&2
  exit 1
fi
# Capture the entry listing once, then match against it. Piping `unzip -Z1`
# straight into `grep -Fxq` lets grep close the pipe on its first match, which
# can hand unzip a SIGPIPE (exit 141); under `set -o pipefail` that spuriously
# fails the check even though the entry is present (Ironhold.class is entry ~6
# of several thousand, so the match — and the pipe close — happen immediately).
jar_entries="$(unzip -Z1 "$JAR_PATH")"
for required_entry in kingdom/smp/Ironhold.class META-INF/neoforge.mods.toml; do
  if ! grep -Fxq "$required_entry" <<<"$jar_entries"; then
    echo "error: jar is missing $required_entry: $JAR_PATH" >&2
    exit 1
  fi
done

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
  cmp -s "$JAR_PATH" "$LOCAL_MODS_DIR/$(basename "$JAR_PATH")" || {
    echo "error: local installed jar does not match build output" >&2
    exit 1
  }
  echo "    copied to $LOCAL_MODS_DIR/$(basename "$JAR_PATH")"
fi

echo
echo "Done. Now restart the MC server from the Folium panel."
