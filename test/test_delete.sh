#!/usr/bin/env bash
set -u

FAIL=0
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPLOAD_DIR="$REPO_ROOT/www/uploads"
mkdir -p "$UPLOAD_DIR"
TEST_FILE="$UPLOAD_DIR/test_delete_$$.txt"
BASENAME=$(basename "$TEST_FILE")

cleanup() {
    rm -f "$TEST_FILE"
}
trap cleanup EXIT

pass() {
    echo "[PASS] $1"
}

fail() {
    echo "[FAIL] $1"
    FAIL=$((FAIL + 1))
}

echo "=== DELETE Method ==="

echo "test content" > "$TEST_FILE"
if [ -f "$TEST_FILE" ]; then
    pass "created test file for deletion"
else
    fail "failed to create test file"
fi

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "http://localhost:8080/uploads/$BASENAME")
if [ "$HTTP_CODE" = "200" ]; then
    pass "DELETE returned 200"
else
    fail "DELETE expected 200, got $HTTP_CODE"
fi

if [ ! -f "$TEST_FILE" ]; then
    pass "target file was removed"
else
    fail "target file still exists after DELETE"
fi

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "http://localhost:8080/uploads/$BASENAME")
if [ "$HTTP_CODE" = "404" ]; then
    pass "deleting missing file returns 404"
else
    fail "second DELETE expected 404, got $HTTP_CODE"
fi

if [ "$FAIL" -eq 0 ]; then
    exit 0
fi
exit 1
