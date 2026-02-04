#!/bin/bash
# Master test runner for Java HTTP Server

echo "======================================"
echo "  Java HTTP Server Test Suite"
echo "======================================"
echo ""

# Check if server is running
if ! nc -z localhost 8080 2>/dev/null; then
    echo "ERROR: Server not running on port 8080"
    echo "Start server with: mvn exec:java"
    exit 1
fi

echo "✓ Server is running on port 8080"
echo ""

# Make all test scripts executable
chmod +x test_*.sh

# Run all tests
PASS=0
FAIL=0

run_test() {
    if ./"$1"; then
        ((PASS++))
    else
        ((FAIL++))
    fi
}

run_test "test_static_files.sh"
run_test "test_redirect.sh"
run_test "test_delete.sh"
run_test "test_cgi.sh"
run_test "test_chunked_fragmented.sh"
run_test "test_binary_chunked.sh"
run_test "test_body_size_limit.sh"

echo "======================================"
echo "  Test Summary"
echo "======================================"
echo "PASSED: $PASS"
echo "FAILED: $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "✓ All tests passed!"
    exit 0
else
    echo "✗ Some tests failed"
    exit 1
fi