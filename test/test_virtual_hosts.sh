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

echo "=== Virtual Host Routing ==="

HTTP_CODE=$(curl -s -H "Host: localhost" -o "$TMP_DIR/localhost.body" -w "%{http_code}" \
    http://127.0.0.1:8080/)
if [ "$HTTP_CODE" = "200" ]; then
    pass "Host localhost returned 200"
else
    fail "Host localhost expected 200, got $HTTP_CODE"
fi

if grep -q "<h1>index</h1>" "$TMP_DIR/localhost.body"; then
    pass "Host localhost resolves to primary server content"
else
    fail "Host localhost did not return primary server content"
fi

HTTP_CODE=$(curl -s -H "Host: mohsindev" -o "$TMP_DIR/mohsindev.body" -w "%{http_code}" \
    http://127.0.0.1:8080/)
if [ "$HTTP_CODE" = "200" ]; then
    pass "Host mohsindev returned 200"
else
    fail "Host mohsindev expected 200, got $HTTP_CODE"
fi

if grep -q "<h1>home</h1>" "$TMP_DIR/mohsindev.body"; then
    pass "Host mohsindev resolves to secondary server content"
else
    fail "Host mohsindev did not return secondary server content"
fi

HTTP_CODE=$(curl -s -H "Host: unknown-host" -o "$TMP_DIR/fallback.body" -w "%{http_code}" \
    http://127.0.0.1:8080/)
if [ "$HTTP_CODE" = "200" ]; then
    pass "unknown host returned 200"
else
    fail "unknown host expected 200, got $HTTP_CODE"
fi

if grep -q "<h1>index</h1>" "$TMP_DIR/fallback.body"; then
    pass "unknown host falls back to default server"
else
    fail "unknown host did not fall back to default server"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
