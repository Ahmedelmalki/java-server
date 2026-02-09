#!/bin/bash
# Test: Static file serving and directory listing

echo "=== Test: Static Files & Autoindex ==="

# Test 1: Serve static file
echo "Testing static file /static/test.html..."
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8080/static/test.html)
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASS: Static file served"
else
    echo "✗ FAIL: Expected 200, got $HTTP_CODE"
fi

# Test 2: Directory listing (autoindex enabled on /static)
echo "Testing directory listing /static/..."
RESPONSE=$(curl -s http://localhost:8080/static/)

if echo "$RESPONSE" | grep -q "Index of"; then
    echo "✓ PASS: Directory listing works"
else
    echo "✗ FAIL: Directory listing failed"
fi

echo ""