#!/usr/bin/env bash
set -u

FAIL=0

pass() {
    echo "[PASS] $1"
}

fail() {
    echo "[FAIL] $1"
    FAIL=$((FAIL + 1))
}

echo "=== Keep-Alive Connection Reuse ==="

RESPONSE=$(
    (
        printf "GET /static/test.html HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: keep-alive\r\n"
        printf "\r\n"

        sleep 0.2

        printf "POST /uploads HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: keep-alive\r\n"
        printf "Content-Length: 11\r\n"
        printf "\r\n"
        printf "test upload"

        sleep 0.2

        printf "GET / HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: close\r\n"
        printf "\r\n"
    ) | nc localhost 8080
)

RESPONSE_COUNT=$(echo "$RESPONSE" | grep -c "HTTP/1.1")
if [ "$RESPONSE_COUNT" -eq 3 ]; then
    pass "received 3 responses on one TCP connection"
else
    fail "expected 3 responses, got $RESPONSE_COUNT"
fi

if echo "$RESPONSE" | grep -qi "Connection: keep-alive"; then
    pass "keep-alive header present"
else
    fail "keep-alive header missing"
fi

if echo "$RESPONSE" | grep -qi "Connection: close"; then
    pass "connection close header present for final request"
else
    fail "connection close header missing for final request"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
