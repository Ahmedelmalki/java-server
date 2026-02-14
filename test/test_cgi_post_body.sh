#!/usr/bin/env bash
set -u

FAIL=0
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP_DIR=$(mktemp -d)
CGI_ECHO_SCRIPT="$REPO_ROOT/www/api/cgi-bin/echo_post.py"

cleanup() {
    rm -f "$CGI_ECHO_SCRIPT"
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

echo "=== CGI POST (Chunked and Unchunked) ==="

cat > "$CGI_ECHO_SCRIPT" <<'PYEOF'
#!/usr/bin/env python3
import sys

body = sys.stdin.buffer.read()
print("Content-Type: text/plain")
print()
sys.stdout.write(body.decode("utf-8", errors="replace"))
PYEOF

chmod +x "$CGI_ECHO_SCRIPT"

UNCHUNKED_PAYLOAD="alpha=123&beta=xyz"
HTTP_CODE=$(curl -s -o "$TMP_DIR/unchunked.body" -w "%{http_code}" \
    -X POST -H "Content-Type: text/plain" --data "$UNCHUNKED_PAYLOAD" \
    http://localhost:8080/api/echo_post.py)
if [ "$HTTP_CODE" = "200" ]; then
    pass "unchunked CGI POST returned 200"
else
    fail "unchunked CGI POST expected 200, got $HTTP_CODE"
fi

if grep -q "$UNCHUNKED_PAYLOAD" "$TMP_DIR/unchunked.body"; then
    pass "unchunked CGI POST body reached script"
else
    fail "unchunked CGI POST body did not reach script"
fi

(
    printf "POST /api/echo_post.py HTTP/1.1\r\n"
    printf "Host: localhost\r\n"
    printf "Transfer-Encoding: chunked\r\n"
    printf "Content-Type: text/plain\r\n"
    printf "\r\n"
    printf "6\r\n"
    printf "chunk-\r\n"
    printf "4\r\n"
    printf "test\r\n"
    printf "0\r\n"
    printf "\r\n"
) | nc localhost 8080 > "$TMP_DIR/chunked.raw"

if grep -q "HTTP/1.1 200" "$TMP_DIR/chunked.raw"; then
    pass "chunked CGI POST returned 200"
else
    fail "chunked CGI POST did not return 200"
fi

if grep -q "chunk-test" "$TMP_DIR/chunked.raw"; then
    pass "chunked CGI POST body reached script"
else
    fail "chunked CGI POST body did not reach script"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
