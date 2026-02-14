#!/usr/bin/env bash
set -u

echo "=== Stress Test (Siege) ==="
echo "Target: 99.5% availability"
echo

if ! command -v siege >/dev/null 2>&1; then
    echo "ERROR: siege is not installed"
    echo "Install with: brew install siege (macOS) or apt install siege (Linux)"
    exit 1
fi

echo "Running siege stress test (30 seconds, 10 concurrent users)..."
siege -b -c 10 -t 30s http://localhost:8080/ 2>&1 | tee siege_results.txt

AVAILABILITY=$(grep "Availability:" siege_results.txt | awk '{print $2}' | tr -d '%')
if [ -z "$AVAILABILITY" ]; then
    echo "FAIL: could not parse availability from siege output"
    rm -f siege_results.txt
    exit 1
fi

echo
echo "======================================"
awk -v a="$AVAILABILITY" 'BEGIN {
    if (a + 0 >= 99.5) {
        printf "PASS: Availability %s%% (target: 99.5%%)\n", a;
        exit 0;
    }
    printf "FAIL: Availability %s%% (target: 99.5%%)\n", a;
    exit 1;
}'
STATUS=$?
echo "======================================"

rm -f siege_results.txt
exit "$STATUS"
