#!/bin/bash
# Stress test with siege (requirement: 99.5% availability)

echo "=== Stress Test (Siege) ==="
echo "Target: 99.5% availability"
echo ""

# Check if siege is installed
if ! command -v siege &> /dev/null; then
    echo "ERROR: siege not installed"
    echo "Install with: brew install siege (macOS) or apt install siege (Linux)"
    exit 1
fi

echo "Running siege stress test (30 seconds, 10 concurrent users)..."
siege -b -c 10 -t 30s http://localhost:8080/ 2>&1 | tee siege_results.txt

# Parse results
AVAILABILITY=$(grep "Availability:" siege_results.txt | awk '{print $2}' | sed 's/%//')

echo ""
echo "======================================"
if (( $(echo "$AVAILABILITY >= 99.5" | bc -l) )); then
    echo "✓ PASS: Availability ${AVAILABILITY}% (target: 99.5%)"
else
    echo "✗ FAIL: Availability ${AVAILABILITY}% (target: 99.5%)"
fi
echo "======================================"

rm siege_results.txt