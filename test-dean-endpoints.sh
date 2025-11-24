#!/bin/bash

# Dean Dashboard API Endpoint Testing Script
# This script tests all the Dean dashboard endpoints to verify they're working

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080/api"

# Check if token is provided
if [ -z "$1" ]; then
    echo -e "${YELLOW}Usage: $0 <JWT_TOKEN>${NC}"
    echo ""
    echo "To get your JWT token:"
    echo "1. Open browser DevTools (F12)"
    echo "2. Go to Console tab"
    echo "3. Type: localStorage.getItem('token')"
    echo "4. Copy the token (without quotes)"
    echo ""
    echo "Example:"
    echo "  $0 eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    exit 1
fi

TOKEN="$1"

echo -e "${YELLOW}Testing Dean Dashboard API Endpoints${NC}"
echo "========================================"
echo ""

# Function to test an endpoint
test_endpoint() {
    local name="$1"
    local endpoint="$2"
    local method="${3:-GET}"
    
    echo -n "Testing $name... "
    
    response=$(curl -s -w "\n%{http_code}" \
        -X "$method" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        "$BASE_URL$endpoint")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        echo -e "${GREEN}✓ OK${NC} (HTTP $http_code)"
        # Pretty print JSON if jq is available
        if command -v jq &> /dev/null; then
            echo "$body" | jq -C '.' 2>/dev/null || echo "$body"
        else
            echo "$body"
        fi
    else
        echo -e "${RED}✗ FAILED${NC} (HTTP $http_code)"
        echo "$body"
    fi
    echo ""
}

# Test endpoints
echo -e "${YELLOW}1. Academic Years${NC}"
test_endpoint "Get Academic Years" "/deanship/academic-years"

echo -e "${YELLOW}2. Departments${NC}"
test_endpoint "Get Departments" "/deanship/departments"

echo -e "${YELLOW}3. Professors${NC}"
test_endpoint "Get All Professors" "/deanship/professors"

echo -e "${YELLOW}4. Courses${NC}"
test_endpoint "Get All Courses" "/deanship/courses"

echo -e "${YELLOW}5. Course Assignments (requires semesterId)${NC}"
echo "Note: This endpoint requires a semesterId parameter"
echo "Example: /deanship/course-assignments?semesterId=1"
echo ""

echo -e "${YELLOW}6. Reports (requires semesterId)${NC}"
echo "Note: This endpoint requires a semesterId parameter"
echo "Example: /deanship/reports/system-wide?semesterId=1"
echo ""

echo "========================================"
echo -e "${GREEN}Testing Complete!${NC}"
echo ""
echo "If you see 401 errors, your token may have expired."
echo "If you see 403 errors, your user may not have DEANSHIP role."
echo "If you see 404 errors, check that the backend is running."
