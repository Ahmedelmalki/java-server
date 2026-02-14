#!/usr/bin/env bash
set -u

FAIL=0
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPLOAD_DIR="$REPO_ROOT/www/uploads"
TMP_DIR=$(mktemp -d)
ORIGINAL_FILE="$TMP_DIR/test_binary.dat"
DOWNLOADED_FILE="$TMP_DIR/downloaded_binary.dat"

cleanup() {
    if [ -n "${UPLOADED_FILE:-}" ] && [ -f "$UPLOAD_DIR/$UPLOADED_FILE" ]; then
        rm -f "$UPLOAD_DIR/$UPLOADED_FILE"
    fi
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

echo "=== Binary Upload (Chunked) ==="

dd if=/dev/urandom of="$ORIGINAL_FILE" bs=1K count=1 status=none

HTTP_CODE=$(curl -s -o "$TMP_DIR/upload.body" -w "%{http_code}" \
    -X POST -H "Transfer-Encoding: chunked" \
    --data-binary @"$ORIGINAL_FILE" \
    http://localhost:8080/uploads)

if [ "$HTTP_CODE" = "201" ]; then
    pass "chunked binary upload returned 201"
else
    fail "chunked binary upload expected 201, got $HTTP_CODE"
fi

UPLOADED_FILE=$(grep -o 'upload_[0-9]\+\.bin' "$TMP_DIR/upload.body" | head -n 1)
if [ -n "$UPLOADED_FILE" ]; then
    pass "uploaded binary filename detected"
else
    fail "could not parse uploaded binary filename"
fi

if [ -n "${UPLOADED_FILE:-}" ]; then
    HTTP_CODE=$(curl -s -o "$DOWNLOADED_FILE" -w "%{http_code}" \
        "http://localhost:8080/uploads/$UPLOADED_FILE")

    if [ "$HTTP_CODE" = "200" ]; then
        pass "uploaded binary can be downloaded"
    else
        fail "downloading uploaded binary expected 200, got $HTTP_CODE"
    fi

    if cmp -s "$ORIGINAL_FILE" "$DOWNLOADED_FILE"; then
        pass "downloaded binary exactly matches original"
    else
        fail "downloaded binary does not match original"
    fi
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
