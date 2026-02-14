#!/usr/bin/env bash
set -u

FAIL=0
TMP_DIR=$(mktemp -d)
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

pass() {
    echo "[PASS] $1"
}

fail() {
    echo "[FAIL] $1"
    FAIL=$((FAIL + 1))
}

echo "=== Redirect ==="

HTTP_CODE=$(curl -s -o /dev/null -D "$TMP_DIR/redirect.hdr" -w "%{http_code}" \
    http://localhost:8080/redirect)
LOCATION=$(grep -i '^Location:' "$TMP_DIR/redirect.hdr" | tail -n 1 | cut -d' ' -f2- | tr -d '\r')

if [ "$HTTP_CODE" = "301" ]; then
    pass "GET /redirect returned 301"
else
    fail "GET /redirect expected 301, got $HTTP_CODE"
fi

if [ "$LOCATION" = "http://localhost:8080/" ]; then
    pass "redirect location is correct"
else
    fail "expected Location http://localhost:8080/, got '$LOCATION'"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
