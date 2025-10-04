#!/usr/bin/env bash
set -euo pipefail

# Watch backend OpenAPI for changes and auto-copy to frontend, then regenerate types.
# macOS-friendly (uses `md5 -q`).

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_OPENAPI="$ROOT_DIR/candidate-match/openapi.yaml"
FRONTEND_DIR="$HOME/git/cloudberries-candidate-match-web"
FRONTEND_OPENAPI="$FRONTEND_DIR/openapi.yaml"

if [[ ! -f "$BACKEND_OPENAPI" ]]; then
  echo "[watch-openapi] ERROR: Backend OpenAPI not found at $BACKEND_OPENAPI" >&2
  exit 1
fi
if [[ ! -d "$FRONTEND_DIR" ]]; then
  echo "[watch-openapi] ERROR: Frontend dir not found at $FRONTEND_DIR" >&2
  exit 1
fi

md5_file() {
  md5 -q "$1"
}

sync_once() {
  echo "[watch-openapi] Syncing OpenAPI to frontend and regenerating types..."
  cp "$BACKEND_OPENAPI" "$FRONTEND_OPENAPI"
  (cd "$FRONTEND_DIR" && npm run gen:api --silent)
  echo "[watch-openapi] Sync complete at $(date '+%Y-%m-%d %H:%M:%S')"
}

# Initial sync
sync_once
prev_hash=$(md5_file "$BACKEND_OPENAPI")

echo "[watch-openapi] Watching $BACKEND_OPENAPI for changes..."
while true; do
  sleep 2
  curr_hash=$(md5_file "$BACKEND_OPENAPI") || curr_hash=""
  if [[ "$curr_hash" != "$prev_hash" ]]; then
    echo "[watch-openapi] Change detected in OpenAPI."
    sync_once || echo "[watch-openapi] WARNING: sync failed; will retry on next change." >&2
    prev_hash="$curr_hash"
  fi
done