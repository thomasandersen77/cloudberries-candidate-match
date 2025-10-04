#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$ROOT_DIR/.run/pids"

stop_one() {
  local name="$1"; shift
  local pid_file="$1"; shift
  if [[ -f "$pid_file" ]]; then
    local pid
    pid=$(cat "$pid_file" || echo "")
    if [[ -n "$pid" ]]; then
      if kill -0 "$pid" 2>/dev/null; then
        echo "[dev-stop] Stopping $name (PID $pid)..."
        kill "$pid" || true
        # Give it a moment, then force if needed
        sleep 1
        if kill -0 "$pid" 2>/dev/null; then
          echo "[dev-stop] Forcing $name (PID $pid)"
          kill -9 "$pid" || true
        fi
      else
        echo "[dev-stop] $name not running (stale PID $pid)"
      fi
    fi
    rm -f "$pid_file"
  else
    echo "[dev-stop] PID file not found for $name ($pid_file)"
  fi
}

stop_one "OpenAPI watcher" "$PID_DIR/watch-openapi.pid"
stop_one "Backend" "$PID_DIR/backend.pid"
stop_one "Frontend" "$PID_DIR/frontend.pid"

echo "[dev-stop] Done."