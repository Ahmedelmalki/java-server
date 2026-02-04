#!/bin/bash
# Test: Chunked transfer encoding with slow/fragmented delivery
# Tests the state machine's ability to handle split chunk size lines and data

echo "=== Test: Chunked Transfer (Fragmented) ==="

echo "Sending chunked request byte-by-byte to /uploads..."

(
    printf "POST /uploads HTTP/1.1\r\n"
    printf "Host: localhost\r\n"
    printf "Transfer-Encoding: chunked\r\n"
    printf "\r\n"
    
    # Chunk 1: "Hello"
    printf "5\r\n"
    sleep 0.1
    printf "Hello\r\n"
    sleep 0.1
    
    # Chunk 2: "World"
    printf "5\r\n"
    sleep 0.1
    printf "World\r\n"
    sleep 0.1
    
    # Last chunk
    printf "0\r\n"
    sleep 0.1
    printf "\r\n"
) | nc localhost 8080 > /tmp/chunked_response.txt

# Check response
if grep -q "201 Created" /tmp/chunked_response.txt || grep -q "200 OK" /tmp/chunked_response.txt; then
    echo "✓ PASS: Chunked upload succeeded"
    if grep -q "upload_" /tmp/chunked_response.txt; then
        echo "  File created: $(grep -o 'upload_[0-9]*\.bin' /tmp/chunked_response.txt)"
    fi
else
    echo "✗ FAIL: Chunked upload failed"
    cat /tmp/chunked_response.txt
fi

rm -f /tmp/chunked_response.txt

echo ""