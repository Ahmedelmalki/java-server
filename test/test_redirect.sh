#!/bin/bash
# Test: HTTP redirect

echo "=== Test: Redirect ==="

echo "Testing redirect from /redirect to /..."
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}\nREDIRECT:%{redirect_url}" \
  -o /dev/null \
  http://localhost:8080/redirect)

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
REDIRECT_URL=$(echo "$RESPONSE" | grep "REDIRECT:" | cut -d: -f2-)

if [ "$HTTP_CODE" = "301" ]; then
    echo "✓ PASS: Got 301 redirect"
    echo "  Redirect URL: $REDIRECT_URL"
else
    echo "✗ FAIL: Expected 301, got $HTTP_CODE"
fi

echo ""