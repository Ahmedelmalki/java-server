#!/bin/bash
# Test: Request body size limit enforcement (413 Payload Too Large)
# Config: primary-server has clientMaxBodySize: 10485760 (10MB)

echo "=== Test: Body Size Limit (413) ==="

# Create 12MB file (exceeds 10MB limit)
echo "Generating 12MB payload..."
dd if=/dev/zero of=large_payload.dat bs=1M count=12 status=none

echo "Sending 12MB POST to /uploads (expects 413)..."
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST \
  --data-binary @large_payload.dat \
  http://localhost:8080/uploads)

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "413" ]; then
    echo "✓ PASS: Got 413 Payload Too Large"
else
    echo "✗ FAIL: Expected 413, got $HTTP_CODE"
    echo "$RESPONSE"
fi

# Cleanup
rm large_payload.dat

echo ""