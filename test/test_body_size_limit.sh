#!/usr/bin/env bash
set -u

FAIL=0
TMP_DIR=$(mktemp -d)
PAYLOAD_FILE="$TMP_DIR/large_payload.dat"

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

echo "=== Body Size Limit (413) ==="

dd if=/dev/zero of="$PAYLOAD_FILE" bs=1M count=12 status=none

HTTP_CODE=$(curl -s -o "$TMP_DIR/413.body" -w "%{http_code}" -X POST \
    --data-binary @"$PAYLOAD_FILE" \
    http://localhost:8080/uploads)

if [ "$HTTP_CODE" = "413" ]; then
    pass "oversized upload returned 413"
else
    fail "oversized upload expected 413, got $HTTP_CODE"
fi

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "server still responds after 413"
else
    fail "server did not recover cleanly after 413 (got $HTTP_CODE)"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
