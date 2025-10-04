#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
PID_DIR="$RUN_DIR/pids"
LOG_DIR="$RUN_DIR/logs"
FRONTEND_DIR="$HOME/git/cloudberries-candidate-match-web"

mkdir -p "$PID_DIR" "$LOG_DIR"

start_bg() {
  # $1: command, $2: log file, $3: pid file
  echo "[dev-start] Starting: $1"
  nohup bash -lc "$1" > "$2" 2>&1 &
  local pid=$!
  echo "$pid" > "$3"
  echo "[dev-start] PID $pid; logs: $2"
}

# Start OpenAPI watcher
start_bg "$ROOT_DIR/scripts/watch-openapi.sh" "$LOG_DIR/watch-openapi.log" "$PID_DIR/watch-openapi.pid"

# Start backend (Spring Boot local profile)
start_bg "cd '$ROOT_DIR' && mvn -q -pl candidate-match spring-boot:run -Dspring-boot.run.profiles=local" "$LOG_DIR/backend.log" "$PID_DIR/backend.pid"

# Start frontend (Vite dev server)
start_bg "npm --prefix '$FRONTEND_DIR' run dev" "$LOG_DIR/frontend.log" "$PID_DIR/frontend.pid"

# Summary
echo "[dev-start] All processes started. PID files in $PID_DIR"
echo "[dev-start] Backend: http://localhost:8080"
echo "[dev-start] Frontend: http://localhost:5173 (default Vite port)"
echo "[dev-start] OpenAPI watcher running; will copy and regenerate types on change."