#!/usr/bin/env bash
set -u

FAIL=0
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPLOAD_DIR="$REPO_ROOT/www/uploads"
TMP_DIR=$(mktemp -d)
RESPONSE_FILE="$TMP_DIR/chunked_response.txt"

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

echo "=== Chunked Transfer (Fragmented) ==="

(
    printf "POST /uploads HTTP/1.1\r\n"
    printf "Host: localhost\r\n"
    printf "Transfer-Encoding: chunked\r\n"
    printf "\r\n"

    printf "5\r\n"
    sleep 0.1
    printf "Hello\r\n"
    sleep 0.1

    printf "5\r\n"
    sleep 0.1
    printf "World\r\n"
    sleep 0.1

    printf "0\r\n"
    sleep 0.1
    printf "\r\n"
) | nc localhost 8080 > "$RESPONSE_FILE"

if grep -q "HTTP/1.1 201" "$RESPONSE_FILE" || grep -q "HTTP/1.1 200" "$RESPONSE_FILE"; then
    pass "fragmented chunked request returned success"
else
    fail "fragmented chunked request did not return success"
fi

UPLOADED_FILE=$(grep -o 'upload_[0-9]\+\.bin' "$RESPONSE_FILE" | head -n 1)
if [ -n "$UPLOADED_FILE" ]; then
    pass "uploaded filename detected in chunked response"
else
    fail "could not parse uploaded filename from chunked response"
fi

if [ -n "${UPLOADED_FILE:-}" ] && [ -f "$UPLOAD_DIR/$UPLOADED_FILE" ]; then
    CONTENT=$(cat "$UPLOAD_DIR/$UPLOADED_FILE")
    if [ "$CONTENT" = "HelloWorld" ]; then
        pass "uploaded chunked content matches expected payload"
    else
        fail "uploaded chunked content mismatch"
    fi
else
    fail "uploaded file from chunked request was not found on disk"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
