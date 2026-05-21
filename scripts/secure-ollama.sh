#!/usr/bin/env bash
# Lock down Ollama on the Hetzner box behind Caddy with bearer-token auth.
#
#   - installs Caddy (idempotent)
#   - generates a strong random bearer token (or accepts one via OLLAMA_TOKEN env)
#   - writes a Caddyfile that listens on :11435 and reverse-proxies authenticated
#     requests to 127.0.0.1:11434 (Ollama, which stays bound to localhost)
#   - opens the UFW rule for port 11435
#   - restarts Caddy and smoke-tests
#
# Usage: bash scripts/secure-ollama.sh
#
# Idempotent. Re-running ROTATES the token unless OLLAMA_TOKEN is preset.

set -euo pipefail

HETZNER_HOST="${HETZNER_HOST:-204.168.218.71}"
HETZNER_USER="${HETZNER_USER:-root}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519_hetzner}"
PROXY_PORT="${PROXY_PORT:-11435}"

if [[ ! -f "$SSH_KEY" ]]; then
  echo "error: SSH key not found at $SSH_KEY" >&2
  exit 1
fi

# 64 hex chars (~256 bits of entropy). Generate locally so the token never lives
# on the remote side outside the Caddyfile.
if [[ -z "${OLLAMA_TOKEN:-}" ]]; then
  OLLAMA_TOKEN="$(openssl rand -hex 32)"
fi

SSH=(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$HETZNER_USER@$HETZNER_HOST")

echo "==> Installing Caddy (skipped if already present)..."
"${SSH[@]}" 'export DEBIAN_FRONTEND=noninteractive
# Clean up any half-written repo file from previous failed runs.
rm -f /etc/apt/sources.list.d/caddy-stable.list
command -v caddy >/dev/null || (
  apt-get install -y debian-keyring debian-archive-keyring apt-transport-https curl gnupg >/dev/null
  curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/gpg.key \
    | gpg --dearmor --yes -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  # The cloudsmith file already includes the signed-by clause; do NOT transform it.
  curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt \
    > /etc/apt/sources.list.d/caddy-stable.list
  apt-get update -qq
  apt-get install -y caddy >/dev/null
)'

echo "==> Writing Caddyfile (proxy :$PROXY_PORT -> 127.0.0.1:11434 with bearer auth)..."
"${SSH[@]}" "cat > /etc/caddy/Caddyfile <<'CADDY'
{
    auto_https off
    admin off
}

:$PROXY_PORT {
    @authorized header Authorization \"Bearer $OLLAMA_TOKEN\"
    handle @authorized {
        reverse_proxy 127.0.0.1:11434
    }
    respond \"unauthorized\" 401
}
CADDY"

echo "==> Opening firewall for port $PROXY_PORT..."
"${SSH[@]}" "
  if command -v ufw >/dev/null && ufw status | grep -q 'Status: active'; then
    ufw allow $PROXY_PORT/tcp >/dev/null
  fi
"

echo "==> Restarting Caddy..."
"${SSH[@]}" 'systemctl enable --now caddy && systemctl restart caddy'

echo "==> Smoke test (should be 401 without token, 200 with token)..."
sleep 2
UNAUTH=$(curl -s -o /dev/null -w '%{http_code}' "http://$HETZNER_HOST:$PROXY_PORT/api/tags" || echo "000")
AUTH=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer $OLLAMA_TOKEN" \
  "http://$HETZNER_HOST:$PROXY_PORT/api/tags" || echo "000")
echo "    no-token  -> $UNAUTH (want 401)"
echo "    with-token -> $AUTH (want 200)"

if [[ "$UNAUTH" != "401" || "$AUTH" != "200" ]]; then
  echo "warn: smoke test didn't return the expected codes — check Caddy logs:" >&2
  echo "      ssh root@$HETZNER_HOST journalctl -u caddy -n 50" >&2
fi

cat <<EOF

================================================================
Ollama is now reachable at:

  URL:   http://$HETZNER_HOST:$PROXY_PORT
  Token: $OLLAMA_TOKEN

In the Folium panel (Startup -> Variables, or wherever env vars
live), set BOTH of these on the MC server:

  KANGARUDE_OLLAMA_URL=http://$HETZNER_HOST:$PROXY_PORT
  KANGARUDE_OLLAMA_TOKEN=$OLLAMA_TOKEN

(Also keep ELEVENLABS_API_KEY and ELEVENLABS_VOICE_ID in the
panel env if you haven't already.)

Then restart the MC server. Token is shown ONCE — copy it now.
================================================================
EOF
