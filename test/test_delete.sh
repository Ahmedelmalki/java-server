#!/bin/bash
# Test: DELETE method

echo "=== Test: DELETE Method ==="

# First create a file - use mkdir -p to ensure directory exists
echo "Creating test file..."
mkdir -p www/uploads  # ADD THIS LINE
echo "test content" > www/uploads/test_delete.txt

if [ -f "www/uploads/test_delete.txt" ]; then
    echo "✓ Test file created"
else
    echo "✗ Failed to create test file"
    echo "Current directory: $(pwd)"  # ADD THIS for debugging
    echo "Trying to create in: $(pwd)/www/uploads/"  # ADD THIS
    exit 1
fi

# Delete the file
echo "Sending DELETE request..."
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X DELETE \
  http://localhost:8080/uploads/test_delete.txt)

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASS: DELETE returned 200"
    
    # Verify file is gone
    if [ ! -f "www/uploads/test_delete.txt" ]; then
        echo "✓ PASS: File deleted successfully"
    else
        echo "✗ FAIL: File still exists"
    fi
else
    echo "✗ FAIL: Expected 200, got $HTTP_CODE"
    echo "$RESPONSE"
fi

echo ""