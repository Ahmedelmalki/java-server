#!/bin/bash
# Test: Binary file upload via chunked encoding (tests binary safety)

echo "=== Test: Binary Upload (Chunked) ==="

# Create a small binary file with random data
echo "Generating 1KB random binary file..."
dd if=/dev/urandom of=test_binary.dat bs=1K count=1 status=none

ORIGINAL_SIZE=$(stat -f%z test_binary.dat 2>/dev/null || stat -c%s test_binary.dat)
echo "Original size: $ORIGINAL_SIZE bytes"

echo "Uploading via chunked encoding to /uploads..."
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST \
  -H "Transfer-Encoding: chunked" \
  --data-binary @test_binary.dat \
  http://localhost:8080/uploads)

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
FILENAME=$(echo "$RESPONSE" | grep -o 'upload_[0-9]*\.bin' | head -1)

if [ "$HTTP_CODE" = "201" ] && [ -n "$FILENAME" ]; then
    echo "✓ PASS: Upload succeeded"
    echo "  Created: $FILENAME"
    
    # Verify file size
    if [ -f "www/uploads/$FILENAME" ]; then
        UPLOADED_SIZE=$(stat -f%z "www/uploads/$FILENAME" 2>/dev/null || stat -c%s "www/uploads/$FILENAME")
        if [ "$ORIGINAL_SIZE" = "$UPLOADED_SIZE" ]; then
            echo "✓ PASS: File size matches ($UPLOADED_SIZE bytes)"
        else
            echo "✗ FAIL: Size mismatch (original: $ORIGINAL_SIZE, uploaded: $UPLOADED_SIZE)"
        fi
    fi
else
    echo "✗ FAIL: Upload failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi

# Cleanup
rm test_binary.dat

echo ""