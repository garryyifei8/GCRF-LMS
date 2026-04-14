#!/bin/bash

# GCRF - Quick Integration Test (Auth + Reader Services Only)
# 测试 Gateway, Auth, Reader 服务

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    echo -e "${YELLOW}[$(($TOTAL_TESTS + 1))] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

print_failure() {
    echo -e "${RED}❌ $1${NC}"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

print_header "GCRF Phase 1 Quick Test"
echo "Date: $(date)"
echo ""

# Check services
print_header "1. Service Status Check"
echo "Gateway (8080):"
lsof -i :8080 | grep -q LISTEN && echo "✅ Running" || echo "❌ Not Running"

echo "Auth (8081):"
lsof -i :8081 | grep -q LISTEN && echo "✅ Running" || echo "❌ Not Running"

echo "Reader (8084):"
lsof -i :8084 | grep -q LISTEN && echo "✅ Running" || echo "❌ Not Running"

sleep 2

# Test 1: Login
print_header "2. Auth Service Tests"
print_test "Login test"

LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username": "admin", "password": "admin123"}' 2>&1)

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"\(.*\)"/\1/' || echo "")

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    print_success "Login successful, token obtained"
else
    print_failure "Login failed - Response: $LOGIN_RESPONSE"
    echo "尝试直接访问 Auth Service..."
    LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8081/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin", "password": "admin123"}')
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"\(.*\)"/\1/' || echo "")
    if [ -n "$TOKEN" ]; then
        print_success "Direct auth service login successful"
    fi
fi

# Test 2: Reader List (if we have token)
if [ -n "$TOKEN" ]; then
    print_header "3. Reader Service Tests"
    print_test "Get reader list"

    READER_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "http://localhost:8080/api/v1/readers?pageNum=1&pageSize=20" 2>&1)

    if echo "$READER_RESPONSE" | grep -q '"code":200'; then
        print_success "Get reader list successful"
    else
        print_failure "Get reader list failed - Response: $READER_RESPONSE"
    fi
fi

# Summary
print_header "4. Test Summary"
echo ""
echo "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc 2>/dev/null || echo "N/A")
    echo "Success Rate: ${SUCCESS_RATE}%"
fi

echo ""
if [ $FAILED_TESTS -eq 0 ] && [ $TOTAL_TESTS -gt 0 ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some tests failed or incomplete. Check logs for details.${NC}"
    exit 1
fi
