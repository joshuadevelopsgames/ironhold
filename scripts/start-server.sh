#!/usr/bin/env bash
# Daily-runtime launcher for the Ironhold MC server.
#
#   - sources Life Agent/.env so ELEVENLABS_API_KEY is in the env
#   - opens an SSH tunnel: localhost:11434 -> Hetzner localhost:11434
#     (Ollama on Hetzner is bound to localhost only; the tunnel is the
#     only way in and is authenticated by the SSH key)
#   - launches the MC server via Gradle's runServer
#   - tears the tunnel down when the server exits
#
# Usage: bash scripts/start-server.sh

set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIFE_ENV="${LIFE_ENV:-$HOME/Life Agent/.env}"

HETZNER_HOST="${HETZNER_HOST:-204.168.218.71}"
HETZNER_USER="${HETZNER_USER:-root}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519_hetzner}"
LOCAL_PORT="${LOCAL_PORT:-11434}"
REMOTE_PORT="${REMOTE_PORT:-11434}"

# ── Load secrets from Life Agent/.env without leaking other vars ─────────
if [[ -f "$LIFE_ENV" ]]; then
  while IFS='=' read -r key val; do
    case "$key" in
      ELEVENLABS_API_KEY|ELEVENLABS_VOICE_ID)
        # Strip surrounding quotes if present
        val="${val%\"}"; val="${val#\"}"
        val="${val%\'}"; val="${val#\'}"
        export "$key=$val"
        ;;
    esac
  done < "$LIFE_ENV"
else
  echo "warn: $LIFE_ENV not found — ElevenLabs voice will be silent." >&2
fi

if [[ -z "${ELEVENLABS_API_KEY:-}" ]]; then
  echo "warn: ELEVENLABS_API_KEY not set — Kangarude will use chat bubble only." >&2
fi

# ── Open the SSH tunnel for Ollama (auto-reconnects on drop) ─────────────
# Uses autossh if available (auto-reconnects on network blips). Falls back
# to a plain ssh process inside a respawn loop if autossh isn't installed.
TUNNEL_PID=""
SSH_OPTS=(
  -i "$SSH_KEY"
  -o StrictHostKeyChecking=accept-new
  -o ServerAliveInterval=30        # ping the server every 30s
  -o ServerAliveCountMax=3         # 3 missed pings (~90s) = kill the connection
  -o ExitOnForwardFailure=yes      # don't keep a half-dead connection alive
  -N
  -L "$LOCAL_PORT:127.0.0.1:$REMOTE_PORT"
  "$HETZNER_USER@$HETZNER_HOST"
)

if lsof -iTCP:"$LOCAL_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "info: localhost:$LOCAL_PORT already bound — assuming an existing tunnel/Ollama is up."
elif command -v autossh >/dev/null; then
  echo "==> Opening autossh tunnel: localhost:$LOCAL_PORT -> $HETZNER_USER@$HETZNER_HOST:$REMOTE_PORT"
  AUTOSSH_GATETIME=0 autossh -M 0 "${SSH_OPTS[@]}" &
  TUNNEL_PID=$!
else
  echo "==> autossh not installed — using bash respawn loop (install via 'brew install autossh' for a quieter setup)"
  (
    until ! ps -p $$ >/dev/null; do
      ssh "${SSH_OPTS[@]}"
      ec=$?
      [[ $ec -eq 0 ]] && break          # clean exit (we asked it to stop)
      echo "warn: tunnel dropped (exit $ec) — reconnecting in 5s..." >&2
      sleep 5
    done
  ) &
  TUNNEL_PID=$!
fi

if [[ -n "$TUNNEL_PID" ]]; then
  # Wait briefly for the forward to bind, then verify.
  for _ in 1 2 3 4 5; do
    sleep 1
    lsof -iTCP:"$LOCAL_PORT" -sTCP:LISTEN >/dev/null 2>&1 && break
  done
  if ! lsof -iTCP:"$LOCAL_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "error: SSH tunnel failed to bind localhost:$LOCAL_PORT after 5s" >&2
    kill "$TUNNEL_PID" 2>/dev/null || true
    exit 1
  fi
fi

cleanup() {
  if [[ -n "$TUNNEL_PID" ]]; then
    echo "==> Closing SSH tunnel (pid $TUNNEL_PID)"
    # Kill the whole process group so any child ssh dies with the wrapper.
    kill -- "-$TUNNEL_PID" 2>/dev/null || kill "$TUNNEL_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

# ── Launch the server ────────────────────────────────────────────────────
# Pass WATCH=1 to auto-restart the MC server if it crashes (Ctrl-C still exits).
cd "$REPO_DIR"
echo "==> Starting MC server (Ctrl-C to stop)..."

run_server_once() {
  ./gradlew runServer
}

if [[ "${WATCH:-0}" = "1" ]]; then
  while true; do
    run_server_once
    ec=$?
    if [[ $ec -eq 130 ]]; then break; fi   # SIGINT — user wanted out
    echo "warn: server exited with $ec — restarting in 5s (Ctrl-C to abort)..." >&2
    sleep 5
  done
else
  run_server_once
fi
