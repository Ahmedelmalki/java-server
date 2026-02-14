#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================"
echo "  Java HTTP Server Test Suite"
echo "======================================"
echo

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERROR: required command '$1' is not installed"
        exit 1
    fi
}

require_cmd curl
require_cmd nc

if ! nc -z localhost 8080 2>/dev/null; then
    echo "ERROR: server is not running on localhost:8080"
    echo "Start server with: mvn exec:java"
    exit 1
fi

echo "Server is reachable on localhost:8080"
echo

chmod +x test_*.sh

TESTS=(
    test_static_files.sh
    test_redirect.sh
    test_virtual_hosts.sh
    test_methods_and_error_pages.sh
    test_delete.sh
    test_multipart_upload.sh
    test_binary_chunked.sh
    test_chunked_fragmented.sh
    test_body_size_limit.sh
    test_session_cookies.sh
    test_cgi.sh
    test_cgi_post_body.sh
    test_keepalive.sh
    test_bad_request_recovery.sh
)

PASS=0
FAIL=0

for test_script in "${TESTS[@]}"; do
    echo "---- $test_script ----"
    if ./$test_script; then
        PASS=$((PASS + 1))
    else
        FAIL=$((FAIL + 1))
    fi
    echo
done

echo "======================================"
echo "  Test Summary"
echo "======================================"
echo "PASSED: $PASS"
echo "FAILED: $FAIL"
echo

if [ "$FAIL" -eq 0 ]; then
    echo "All tests passed."
    exit 0
fi

echo "Some tests failed."
exit 1
