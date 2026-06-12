#!/usr/bin/env bash
# Generate the NPC voice lines for the Ironhold website using the same
# ElevenLabs voices the mod itself uses (voice ids from the entity classes).
#
# Usage:
#   ELEVENLABS_API_KEY=sk_... ./generate_npc_voice_lines.sh
#
# Output: website/assets/voices/<slug>.mp3 — the site picks them up
# automatically; cards with no file fall back to "voice in casting".
set -euo pipefail

if [[ -z "${ELEVENLABS_API_KEY:-}" ]]; then
  echo "ELEVENLABS_API_KEY is not set." >&2
  exit 1
fi

OUT="$(cd "$(dirname "$0")/.." && pwd)/assets/voices"
mkdir -p "$OUT"

MODEL="eleven_multilingual_v2"   # highest quality — these are baked once

# slug|voice_id|line  (voice ids mirror src/main/java/kingdom/smp/entity/*.java)
LINES=(
  'halric|HAvvFKatz0uu0Fv55Riy|The seal holds because I hold it. Prove your kingdom worthy.'
  'kangarude|tdlj9WjgHdDTMKoAvBYQ|Oh look. A peasant. Tracked mud AND disappointment in here.'
  'mira|flHkNRp1BlvT73UL6gyz|Sit. Drink. Tell me which knight is lying about the dragon.'
  'roselind|wa4sQVgbDDzUDEzJwch3|Discipline wins wars. Heroes just die louder.'
  'eilan|0lp4RIz96WD1RUtvEu3Q|History repeats. I simply take attendance.'
  'dunstan|Vs5CmVCVJwW4odQS2pVf|The mountain pays in ore, or in funerals. Mind your bracing.'
  'bram|9exXVJADqBPPYLM4OGWi|Every ballad is true, friend. Some just have not happened yet.'
  'watcher|TsHrPyMlNFuIYnbODF01|You are not on my list yet. Keep it that way — I hate revisions.'
  'beren|nTMUXLFSfbWmdKKy7nDC|I have buried three kings and one bad idea. You smell like the second.'
  'hesta|NwyAvGnfbFoNNEi4UuTq|Chew this. Do not ask what it is. Asking makes it work less.'
  'pippa|ocZQ262SsZb9RIxcQBOj|Psst. For a coin I will tell you which guard sleeps standing up.'
  'wren|Df0A8fHl2LOO7kDNIlpg|The light keeps what it loves. Try to be worth keeping.'
)

for entry in "${LINES[@]}"; do
  IFS='|' read -r slug voice line <<<"$entry"
  echo "→ $slug"
  curl -sf -X POST \
    "https://api.elevenlabs.io/v1/text-to-speech/${voice}?output_format=mp3_44100_128" \
    -H "xi-api-key: ${ELEVENLABS_API_KEY}" \
    -H "Content-Type: application/json" \
    -d "$(printf '{"text":"%s","model_id":"%s","voice_settings":{"stability":0.45,"similarity_boost":0.8,"style":0.35}}' "$line" "$MODEL")" \
    -o "${OUT}/${slug}.mp3" \
    || { echo "  FAILED for $slug (check voice id / key / credits)"; rm -f "${OUT}/${slug}.mp3"; }
done

echo "Done. $(ls "$OUT" | wc -l | tr -d ' ') voice lines in $OUT"
