#!/bin/bash
# Test: CGI script execution

echo "=== Test: CGI Execution ==="

echo "Testing Python CGI at /api/hello.py..."
RESPONSE=$(curl -s http://localhost:8080/api/hello.py)

if echo "$RESPONSE" | grep -q "Hello"; then
    echo "✓ PASS: Python CGI executed"
    echo "  Response: $RESPONSE"
else
    echo "✗ FAIL: CGI execution failed"
    echo "  Response: $RESPONSE"
fi

echo ""