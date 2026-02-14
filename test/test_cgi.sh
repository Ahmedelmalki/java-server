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

echo "=== CGI Execution ==="

HTTP_CODE=$(curl -s -o "$TMP_DIR/cgi.body" -D "$TMP_DIR/cgi.hdr" -w "%{http_code}" \
    http://localhost:8080/api/hello.py)
if [ "$HTTP_CODE" = "200" ]; then
    pass "GET /api/hello.py returned 200"
else
    fail "GET /api/hello.py expected 200, got $HTTP_CODE"
fi

if grep -qi '^Content-Type: text/plain' "$TMP_DIR/cgi.hdr"; then
    pass "CGI returned text/plain content type"
else
    fail "CGI did not return expected Content-Type"
fi

if grep -q "Hello from Python CGI!" "$TMP_DIR/cgi.body"; then
    pass "CGI body content matches expected output"
else
    fail "CGI output missing expected text"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
