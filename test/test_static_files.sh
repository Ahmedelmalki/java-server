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

echo "=== Static Files, Autoindex, Default Index ==="

HTTP_CODE=$(curl -s -o "$TMP_DIR/static.body" -D "$TMP_DIR/static.hdr" -w "%{http_code}" \
    http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "GET /static/test.html returned 200"
else
    fail "GET /static/test.html expected 200, got $HTTP_CODE"
fi

if grep -qi "^Content-Type: text/html" "$TMP_DIR/static.hdr"; then
    pass "static response has text/html content type"
else
    fail "static response missing/incorrect Content-Type"
fi

if grep -q "testing satic files" "$TMP_DIR/static.body"; then
    pass "static response body matches expected file content"
else
    fail "static response body does not match expected content"
fi

HTTP_CODE=$(curl -s -o "$TMP_DIR/dir.body" -w "%{http_code}" \
    http://localhost:8080/static/)
if [ "$HTTP_CODE" = "200" ]; then
    pass "GET /static/ returned 200"
else
    fail "GET /static/ expected 200, got $HTTP_CODE"
fi

if grep -q "Index of" "$TMP_DIR/dir.body"; then
    pass "autoindex directory listing is enabled for /static/"
else
    fail "autoindex directory listing missing for /static/"
fi

HTTP_CODE=$(curl -s -o "$TMP_DIR/root.body" -w "%{http_code}" \
    http://localhost:8080/)
if [ "$HTTP_CODE" = "200" ]; then
    pass "GET / returned 200"
else
    fail "GET / expected 200, got $HTTP_CODE"
fi

if grep -q "<h1>index</h1>" "$TMP_DIR/root.body"; then
    pass "default index file is served for /"
else
    fail "default index file check failed for /"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
