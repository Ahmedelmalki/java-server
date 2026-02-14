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

echo "=== Bad Request Handling and Recovery ==="

printf "BROKEN_REQUEST\r\n\r\n" | nc localhost 8080 > "$TMP_DIR/bad1.raw"
if grep -q "400 Bad Request" "$TMP_DIR/bad1.raw"; then
    pass "malformed request line returned 400"
else
    fail "malformed request line did not return 400"
fi

(
    printf "POST /uploads HTTP/1.1\r\n"
    printf "Host: localhost\r\n"
    printf "Content-Length: not-a-number\r\n"
    printf "\r\n"
) | nc localhost 8080 > "$TMP_DIR/bad2.raw"
if grep -q "400 Bad Request" "$TMP_DIR/bad2.raw"; then
    pass "invalid Content-Length returned 400"
else
    fail "invalid Content-Length did not return 400"
fi

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "server still handles valid request after bad requests"
else
    fail "server did not recover after bad requests (got $HTTP_CODE)"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
