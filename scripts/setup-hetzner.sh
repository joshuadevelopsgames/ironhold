#!/usr/bin/env bash
# One-time provisioning of the Hetzner box for Kangarude's Ollama brain.
# Installs Ollama, picks a Gemma model based on whether a GPU is present,
# binds Ollama to localhost only (so we reach it via SSH tunnel), and
# enables it as a systemd service.
#
# Usage: bash scripts/setup-hetzner.sh
#
# Idempotent — safe to re-run.

set -euo pipefail

HETZNER_HOST="${HETZNER_HOST:-204.168.218.71}"
HETZNER_USER="${HETZNER_USER:-root}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519_hetzner}"

if [[ ! -f "$SSH_KEY" ]]; then
  echo "error: SSH key not found at $SSH_KEY" >&2
  exit 1
fi

SSH=(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$HETZNER_USER@$HETZNER_HOST")

echo "==> Probing $HETZNER_USER@$HETZNER_HOST for GPU presence..."
if "${SSH[@]}" 'command -v nvidia-smi >/dev/null && nvidia-smi -L >/dev/null 2>&1'; then
  HAS_GPU=1
  MODEL="gemma3:12b"
  echo "    GPU detected — using $MODEL."
else
  HAS_GPU=0
  MODEL="gemma3:4b"
  echo "    No GPU detected — using $MODEL (CPU inference)."
fi

echo "==> Installing Ollama (skipped if already present)..."
"${SSH[@]}" 'command -v ollama >/dev/null || curl -fsSL https://ollama.com/install.sh | sh'

echo "==> Configuring systemd override to bind Ollama to 127.0.0.1 only..."
"${SSH[@]}" 'mkdir -p /etc/systemd/system/ollama.service.d && \
  cat > /etc/systemd/system/ollama.service.d/override.conf <<EOF
[Service]
Environment="OLLAMA_HOST=127.0.0.1:11434"
EOF
  systemctl daemon-reload && systemctl enable --now ollama && systemctl restart ollama'

echo "==> Pulling $MODEL (this may take a while on first run)..."
"${SSH[@]}" "ollama pull $MODEL"

echo "==> Smoke test..."
"${SSH[@]}" "curl -fsS http://127.0.0.1:11434/api/tags | head -c 200 && echo"

cat <<EOF

Hetzner setup complete.
  Model: $MODEL
  Listening on: 127.0.0.1:11434 (private to the Hetzner box)
  GPU mode: $([ "$HAS_GPU" = "1" ] && echo on || echo off)

Next: run scripts/start-server.sh to open the SSH tunnel and launch the MC server.
EOF
