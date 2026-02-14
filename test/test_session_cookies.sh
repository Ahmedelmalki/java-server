#!/usr/bin/env bash
set -u

FAIL=0
TMP_DIR=$(mktemp -d)
COOKIE_JAR="$TMP_DIR/cookies.txt"

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

echo "=== Session and Cookies ==="

HTTP_CODE=$(curl -s -o /dev/null -D "$TMP_DIR/first.hdr" -c "$COOKIE_JAR" -w "%{http_code}" \
    http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "first request returned 200"
else
    fail "first request expected 200, got $HTTP_CODE"
fi

if grep -qi '^Set-Cookie: JSESSIONID=' "$TMP_DIR/first.hdr"; then
    pass "first response sets JSESSIONID cookie"
else
    fail "first response did not set JSESSIONID cookie"
fi

HTTP_CODE=$(curl -s -o /dev/null -D "$TMP_DIR/second.hdr" -b "$COOKIE_JAR" -c "$COOKIE_JAR" -w "%{http_code}" \
    http://localhost:8080/static/test.html)
if [ "$HTTP_CODE" = "200" ]; then
    pass "second request with cookie returned 200"
else
    fail "second request expected 200, got $HTTP_CODE"
fi

if grep -qi '^Set-Cookie: JSESSIONID=' "$TMP_DIR/second.hdr"; then
    fail "second response should not issue a new JSESSIONID"
else
    pass "second response reused existing JSESSIONID"
fi

HTTP_CODE=$(curl -s -o "$TMP_DIR/session.body" -b "$COOKIE_JAR" -w "%{http_code}" \
    http://localhost:8080/api/session.py)
if [ "$HTTP_CODE" = "200" ]; then
    pass "CGI session endpoint returned 200"
else
    fail "CGI session endpoint expected 200, got $HTTP_CODE"
fi

if grep -q '"session_id": null' "$TMP_DIR/session.body"; then
    fail "CGI did not receive session cookie"
else
    pass "CGI received session cookie"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
