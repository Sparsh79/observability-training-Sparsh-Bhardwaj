#!/bin/bash

# Script to generate traffic to the Inventory Service
# This helps populate metrics and logs for Grafana visualization

echo "Starting traffic generation to Inventory Service..."
echo "Press Ctrl+C to stop"
echo ""

BASE_URL="http://localhost:8080/api/inventory/items"
VALID_ITEMS=("ITEM001" "ITEM002" "ITEM003" "ITEM004" "ITEM005")
INVALID_ITEMS=("INVALID1" "INVALID2" "NOTFOUND" "ITEM999")

counter=0

while true; do
    counter=$((counter + 1))

    # 70% valid requests, 30% invalid
    if [ $((RANDOM % 10)) -lt 7 ]; then
        # Valid item
        item=${VALID_ITEMS[$RANDOM % ${#VALID_ITEMS[@]}]}
        echo "[$counter] Requesting valid item: $item"
    else
        # Invalid item
        item=${INVALID_ITEMS[$RANDOM % ${#INVALID_ITEMS[@]}]}
        echo "[$counter] Requesting invalid item: $item (expecting error)"
    fi

    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/$item")
    echo "Response: $response"
    echo ""

    # Random sleep between 0.5 and 2 seconds
    sleep_time=$(echo "scale=1; $(($RANDOM % 16 + 5)) / 10" | bc)
    sleep $sleep_time
done
