#!/usr/bin/env bash
set -u

FAIL=0
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPLOAD_DIR="$REPO_ROOT/www/uploads"
TMP_DIR=$(mktemp -d)
SOURCE_FILE="$TMP_DIR/source_upload.txt"

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

echo "=== Multipart Upload Integrity ==="

printf "multipart-payload-%s\nline-two\n" "$(date +%s)" > "$SOURCE_FILE"

HTTP_CODE=$(curl -s -o "$TMP_DIR/upload.body" -w "%{http_code}" \
    -X POST -F "file=@$SOURCE_FILE;filename=fixture.txt" \
    http://localhost:8080/uploads)

if [ "$HTTP_CODE" = "201" ]; then
    pass "multipart upload returned 201"
else
    fail "multipart upload expected 201, got $HTTP_CODE"
fi

UPLOADED_FILE=$(grep -o 'upload_[0-9]\+\.[A-Za-z0-9]\+' "$TMP_DIR/upload.body" | head -n 1)
if [ -n "$UPLOADED_FILE" ]; then
    pass "uploaded filename detected in response"
else
    fail "could not parse uploaded filename from response"
fi

if [ -n "${UPLOADED_FILE:-}" ]; then
    HTTP_CODE=$(curl -s -o "$TMP_DIR/downloaded.txt" -w "%{http_code}" \
        "http://localhost:8080/uploads/$UPLOADED_FILE")

    if [ "$HTTP_CODE" = "200" ]; then
        pass "uploaded file can be downloaded"
    else
        fail "downloaded uploaded file expected 200, got $HTTP_CODE"
    fi

    if cmp -s "$SOURCE_FILE" "$TMP_DIR/downloaded.txt"; then
        pass "downloaded file matches original multipart content"
    else
        fail "downloaded file differs from original multipart content"
    fi
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
