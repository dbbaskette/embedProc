#!/bin/bash

# Test script for embedProc Processing API endpoints
# Run this when the application is running in cloud profile

BASE_URL="http://localhost:8080/api/processing"

echo "=== Testing embedProc Processing API Endpoints ==="
echo ""

# Test 1: Get current state
echo "1. Getting current processing state:"
curl -s -X GET "$BASE_URL/state" | jq '.'
echo ""
echo ""

# Test 2: Stop processing
echo "2. Stopping processing:"
curl -s -X POST "$BASE_URL/stop" | jq '.'
echo ""
echo ""

# Test 3: Get state after stop
echo "3. Getting state after stop:"
curl -s -X GET "$BASE_URL/state" | jq '.'
echo ""
echo ""

# Test 4: Start processing
echo "4. Starting processing:"
curl -s -X POST "$BASE_URL/start" | jq '.'
echo ""
echo ""

# Test 5: Get state after start
echo "5. Getting state after start:"
curl -s -X GET "$BASE_URL/state" | jq '.'
echo ""
echo ""

# Test 6: Toggle processing (should stop it)
echo "6. Toggling processing (should stop):"
curl -s -X POST "$BASE_URL/toggle" | jq '.'
echo ""
echo ""

# Test 7: Toggle again (should start it)
echo "7. Toggling processing again (should start):"
curl -s -X POST "$BASE_URL/toggle" | jq '.'
echo ""
echo ""

# Test 8: Final state check
echo "8. Final processing state:"
curl -s -X GET "$BASE_URL/state" | jq '.'
echo ""

echo "=== API Testing Complete ==="
