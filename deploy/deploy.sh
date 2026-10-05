#!/usr/bin/env bash
#
# deploy.sh — manual, isolated VPS deployment for the Shared Shopping List
# backend (Stage 8, AC29 / AC31 / AC32; build step 11).
#
# Design goals (Architecture notes "VPS deployment design", AGENTS.md):
#   * Fully isolated from Hermes: separate app directory, separate Compose
#     project name, mounted SQLite volume, no reused Hermes ports/service files,
#     no committed .env.
#   * Manual only — no CI deploy in the MVP (R3). This script performs
#     pull -> build -> restart -> smoke check and proves Hermes is untouched
#     (OQ-9 baseline/verify).
#
# It is NOT executed automatically and there is no CI workflow that calls it.
#
# Usage (run on the VPS host):
#   ./deploy/deploy.sh [command]
#
# Commands:
#   deploy     (default) baseline -> git update -> build -> up -> smoke -> verify
#   baseline   capture the Hermes "before" snapshot
#   verify     capture the Hermes "after" snapshot and diff against the baseline
#   rollback   redeploy the previously recorded git revision
#   status     show the app's Compose containers + recent logs
#   logs       follow the backend container logs
#
# Configuration (environment overrides):
#   APP_DIR          default /home/hermes/apps/shared-shopping-list
#   COMPOSE_PROJECT  default shared-shopping-list
#   API_PORT         default 8000
#   GIT_REF          default main
#   HEALTH_URL       default http://127.0.0.1:${API_PORT}
#   BASELINE_FILE    default ${APP_DIR}/deploy/.hermes-baseline.txt
set -euo pipefail

APP_DIR="${APP_DIR:-/home/hermes/apps/shared-shopping-list}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-shared-shopping-list}"
API_PORT="${API_PORT:-8000}"
GIT_REF="${GIT_REF:-main}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${API_PORT}}"
BASELINE_FILE="${BASELINE_FILE:-${APP_DIR}/deploy/.hermes-baseline.txt}"
PREV_REV_FILE="${APP_DIR}/deploy/.previous-revision"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SMOKE_CHECK="${REPO_DIR}/deploy/smoke-check.sh"

log()  { printf '\n==> %s\n' "$*"; }
warn() { printf 'warn: %s\n' "$*" >&2; }
die()  { printf 'error: %s\n' "$*" >&2; exit 1; }

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

compose() {
    docker compose -p "$COMPOSE_PROJECT" --project-directory "$APP_DIR" "$@"
}

# ---------------------------------------------------------------------------
# OQ-9 — Hermes-untouched evidence.
#
# Captures a *Hermes-relevant* snapshot: Docker containers, running systemd
# services, and listening TCP ports — deliberately EXCLUDING this app's own
# Compose project and its API port. Two captures (before deploy / after deploy)
# must therefore be byte-identical; any diff proves Hermes was disturbed.
#
# The same commands are documented in deploy/README.md so they are repeatable
# by hand and usable as the paired AC29/AC33 evidence.
# ---------------------------------------------------------------------------
hermes_snapshot() {
    echo "# docker containers (excluding Compose project ${COMPOSE_PROJECT})"
    docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' \
        | grep -v -- "$COMPOSE_PROJECT" || true
    echo "# running systemd services"
    if command -v systemctl >/dev/null 2>&1; then
        systemctl list-units --type=service --state=running --no-pager --no-legend \
            | awk '{print $1}' | sort -u || true
    else
        echo "(systemctl unavailable)"
    fi
    echo "# listening TCP ports (excluding this app on :${API_PORT})"
    if command -v ss >/dev/null 2>&1; then
        ss -ltnH | awk '{print $4}' | grep -Ev "[:.]${API_PORT}\$" | sort -u || true
    elif command -v netstat >/dev/null 2>&1; then
        netstat -an 2>/dev/null | grep -i 'listen' | awk '{print $4}' \
            | grep -Ev "[:.]${API_PORT}\$" | sort -u || true
    else
        echo "(ss/netstat unavailable)"
    fi
    echo "# timestamp-independent: statuses only, no PIDs/uptime"
}

cmd_baseline() {
    require_cmd docker
    mkdir -p "$APP_DIR" "$(dirname "$BASELINE_FILE")"
    hermes_snapshot > "$BASELINE_FILE"
    log "Hermes baseline written to $BASELINE_FILE"
    printf '    %s lines captured\n' "$(wc -l < "$BASELINE_FILE" | tr -d ' ')"
}

cmd_verify() {
    [ -f "$BASELINE_FILE" ] || die "no baseline at $BASELINE_FILE — run '$0 baseline' first"
    _current="$(mktemp)"
    # shellcheck disable=SC2064
    trap "rm -f '$_current'" EXIT HUP INT TERM
    hermes_snapshot > "$_current"
    log "Hermes before/after comparison"
    if diff -u "$BASELINE_FILE" "$_current"; then
        printf 'Hermes untouched: baseline and current snapshot are identical.\n'
    else
        warn "Hermes snapshot CHANGED between baseline and now (see diff above)."
        warn "Investigate before considering the deploy safe."
        return 1
    fi
}

smoke_with_retry() {
    [ -x "$SMOKE_CHECK" ] || die "smoke script not executable: $SMOKE_CHECK"
    _attempt=1
    _max="${SMOKE_ATTEMPTS:-15}"
    while [ "$_attempt" -le "$_max" ]; do
        if "$SMOKE_CHECK" "$HEALTH_URL"; then
            return 0
        fi
        printf '    attempt %s/%s not ready yet, retrying in 2s...\n' "$_attempt" "$_max"
        _attempt=$((_attempt + 1))
        sleep 2
    done
    die "smoke check failed after ${_max} attempts against ${HEALTH_URL}"
}

cmd_deploy() {
    require_cmd docker
    require_cmd git
    require_cmd curl

    [ -d "$APP_DIR" ] || die "app dir not found: $APP_DIR (create it and clone the repo first)"
    [ -f "$APP_DIR/.env" ] || die "missing $APP_DIR/.env — copy .env.example to .env on the host (never committed)"

    cd "$APP_DIR"

    # Record the revision we are leaving, so rollback is one command (AC32).
    _prev="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
    echo "$_prev" > "$PREV_REV_FILE"
    log "previous revision recorded: $_prev (rollback target)"

    # Capture Hermes *before* state if we do not already have a baseline.
    if [ ! -f "$BASELINE_FILE" ]; then
        cmd_baseline
    else
        log "reusing existing Hermes baseline at $BASELINE_FILE"
    fi

    log "updating working tree to '$GIT_REF'"
    git fetch --prune origin
    if git show-ref --verify --quiet "refs/heads/$GIT_REF"; then
        git checkout "$GIT_REF"
        git pull --ff-only origin "$GIT_REF"
    else
        git checkout --detach "origin/$GIT_REF"
    fi
    _now="$(git rev-parse --short HEAD)"
    log "deploying revision $_now"

    log "building image (Compose project: $COMPOSE_PROJECT)"
    compose build --pull

    log "restarting the isolated app (no impact on Hermes services)"
    compose up -d --remove-orphans

    log "smoke check: $HEALTH_URL"
    smoke_with_retry

    log "deployed revision: $_now"
    log "Hermes-untouched verification"
    cmd_verify

    log "deploy complete"
    printf 'Inspect logs with: %s logs\n' "$0"
}

cmd_rollback() {
    require_cmd docker
    require_cmd git
    [ -f "$PREV_REV_FILE" ] || die "no previous revision recorded at $PREV_REV_FILE"
    _target="$(cat "$PREV_REV_FILE")"
    [ -n "$_target" ] && [ "$_target" != "unknown" ] || die "recorded revision is not usable: $_target"

    cd "$APP_DIR"
    log "rolling back to previous revision $_target"
    git checkout --detach "$_target"
    compose build --pull
    compose up -d --remove-orphans
    smoke_with_retry
    log "rollback complete at revision $_target"
    warn "After a successful rollback, record the new 'previous' revision for the next deploy."
}

cmd_status() {
    compose ps
    log "recent backend logs (tail 50)"
    compose logs --tail=50
}

cmd_logs() {
    compose logs -f
}

main() {
    case "${1:-deploy}" in
        deploy)   cmd_deploy ;;
        baseline) cmd_baseline ;;
        verify)   cmd_verify ;;
        rollback) cmd_rollback ;;
        status)   cmd_status ;;
        logs)     cmd_logs ;;
        *) die "unknown command: $1 (use deploy|baseline|verify|rollback|status|logs)" ;;
    esac
}

main "$@"
