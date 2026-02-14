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

echo "=== Methods and Error Pages ==="

MISSING_PATH="/missing-resource-$(date +%s)"
HTTP_CODE=$(curl -s -o "$TMP_DIR/404.body" -w "%{http_code}" "http://localhost:8080$MISSING_PATH")
if [ "$HTTP_CODE" = "404" ]; then
    pass "missing resource returned 404"
else
    fail "missing resource expected 404, got $HTTP_CODE"
fi

if grep -q "<h1>404 Not Found</h1>" "$TMP_DIR/404.body"; then
    pass "custom 404 page is served"
else
    fail "custom 404 page content not detected"
fi

HTTP_CODE=$(curl -s -X POST -o "$TMP_DIR/405.body" -w "%{http_code}" \
    http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "405" ]; then
    pass "POST /static/test.html returned 405"
else
    fail "POST /static/test.html expected 405, got $HTTP_CODE"
fi

if grep -q "<h1>405 Method Not Allowed</h1>" "$TMP_DIR/405.body"; then
    pass "custom 405 page is served"
else
    fail "custom 405 page content not detected"
fi

HTTP_CODE=$(curl -s -X DELETE -o /dev/null -w "%{http_code}" \
    http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "405" ]; then
    pass "DELETE is blocked on route without DELETE permission"
else
    fail "DELETE /static/test.html expected 405, got $HTTP_CODE"
fi

HTTP_CODE=$(curl --path-as-is -s -o "$TMP_DIR/403.body" -w "%{http_code}" \
    http://localhost:8080/static/../config.json)
if [ "$HTTP_CODE" = "403" ]; then
    pass "path traversal attempt returned 403"
else
    fail "path traversal attempt expected 403, got $HTTP_CODE"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
