#!/usr/bin/env sh
#
# smoke-check.sh — post-deploy / local smoke check for the Shared Shopping List
# backend (Stage 8, AC30 / AC32 / HP-14).
#
# It asserts the *locked* MVP contract:
#   GET /health  -> HTTP 200 AND body exactly {"status":"ok"}
#   GET /lists   -> HTTP 200 AND a JSON array (e.g. [] or [{...}])
#
# The script is intentionally dependency-free (POSIX sh + curl only) so the same
# file runs:
#   * locally, against the Compose backend:  ./deploy/smoke-check.sh http://localhost:8000
#   * on the VPS, against the isolated app:  ./deploy/smoke-check.sh http://127.0.0.1:8000
#
# Usage:
#   ./deploy/smoke-check.sh [BASE_URL]
#
# BASE_URL resolution order: $1 > $BASE_URL > http://localhost:8000
# A trailing slash is tolerated. Exits non-zero and prints a diagnostic on any
# mismatch, so it can gate a manual deploy.
set -eu

BASE_URL="${1:-${BASE_URL:-http://localhost:8000}}"
BASE_URL="${BASE_URL%/}"
TIMEOUT="${SMOKE_TIMEOUT:-10}"

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

pass() {
    printf 'PASS: %s\n' "$1"
}

# http_get PATH VAR_BODY VAR_CODE
# Fetches `${BASE_URL}${PATH}`, stores the response body and HTTP status code in
# the two named variables (via eval). Uses a temp file so multi-line bodies are
# handled correctly. Dies on transport failure.
http_get() {
    _path="$1"
    _body_var="$2"
    _code_var="$3"
    _tmp="$(mktemp "${TMPDIR:-/tmp}/ssl-smoke.XXXXXX")"
    _code="$(curl --silent --show-error --location --max-time "$TIMEOUT" \
        --output "$_tmp" \
        --write-out '%{http_code}' \
        "${BASE_URL}${_path}")" || {
        rm -f "$_tmp"
        fail "GET ${_path}: request failed (is ${BASE_URL} reachable?)"
    }
    eval "$_body_var=\$(cat '$_tmp')"
    eval "$_code_var=\$_code"
    rm -f "$_tmp"
}

printf 'Smoke check against %s\n' "$BASE_URL"

# ---------------------------------------------------------------------------
# 1) GET /health -> HTTP 200 + body {"status":"ok"} (exact)
# ---------------------------------------------------------------------------
http_get /health health_body health_code
[ "$health_code" = "200" ] || fail "GET /health: expected HTTP 200, got ${health_code} (body: ${health_body})"
[ "$health_body" = '{"status":"ok"}' ] || fail "GET /health: expected body {\"status\":\"ok\"}, got: ${health_body}"
pass 'GET /health -> HTTP 200 {"status":"ok"}'

# ---------------------------------------------------------------------------
# 2) GET /lists -> HTTP 200 + JSON array
# ---------------------------------------------------------------------------
http_get /lists lists_body lists_code
[ "$lists_code" = "200" ] || fail "GET /lists: expected HTTP 200, got ${lists_code} (body: ${lists_body})"

# Structural JSON-array check without requiring python/jq: drop whitespace, then
# require the first and last characters to be [ and ].
lists_compact="$(printf '%s' "$lists_body" | tr -d '\n\r\t ')"
case "$lists_compact" in
    \[*\]) : ;;
    *) fail "GET /lists: expected a JSON array, got: ${lists_body}" ;;
esac
pass 'GET /lists -> HTTP 200 JSON array'

printf 'Smoke check OK: %s\n' "$BASE_URL"
