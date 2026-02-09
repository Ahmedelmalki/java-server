#!/bin/bash
# Test: HTTP Keep-Alive (Connection reuse)

echo "=== Test: Keep-Alive ==="

echo "Sending 3 pipelined requests on same connection..."

RESPONSE=$(
    (
        # Request 1: GET with keep-alive
        printf "GET /static/test.html HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: keep-alive\r\n"
        printf "\r\n"
        
        sleep 0.2
        
        # Request 2: POST with keep-alive
        printf "POST /uploads HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: keep-alive\r\n"
        printf "Content-Length: 11\r\n"
        printf "\r\n"
        printf "test upload"
        
        sleep 0.2
        
        # Request 3: GET with close
        printf "GET / HTTP/1.1\r\n"
        printf "Host: localhost\r\n"
        printf "Connection: close\r\n"
        printf "\r\n"
    ) | nc localhost 8080
)

# Count how many HTTP responses we got
RESPONSE_COUNT=$(echo "$RESPONSE" | grep -c "HTTP/1.1")

if [ "$RESPONSE_COUNT" -eq 3 ]; then
    echo "✓ PASS: Got 3 responses on same connection"
    
    # Check if responses have keep-alive headers
    if echo "$RESPONSE" | grep -q "Connection: keep-alive"; then
        echo "✓ PASS: Keep-Alive header present"
    else
        echo "⚠ WARNING: No Keep-Alive header (might still work)"
    fi
else
    echo "✗ FAIL: Expected 3 responses, got $RESPONSE_COUNT"
    echo "Response:"
    echo "$RESPONSE"
fi

echo ""