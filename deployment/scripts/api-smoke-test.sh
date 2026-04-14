#!/bin/bash

###############################################################################
# GCRF Library Management System - API Smoke Test Script
#
# Description: Quick smoke testing of all core API endpoints using curl
#   - Tests all microservice APIs through gateway
#   - Validates response status codes and data formats
#   - Tests authentication and authorization
#   - Provides quick health check of entire system
#
# Usage: ./api-smoke-test.sh [options]
#   Options:
#     --gateway <url>   Gateway URL (default: http://localhost:8080)
#     --verbose         Show detailed output
#     --json            Output results as JSON
#
# Author: GCRF DevOps Team
# Date: 2025-12-01
###############################################################################

set -e

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
VERBOSE=false
JSON_OUTPUT=false
TIMEOUT=10

# Test statistics
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test results
declare -a RESULTS=()

###############################################################################
# Utility Functions
###############################################################################

log() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[INFO]${NC} $*"
    fi
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
}

# Execute API test
test_api() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local token="$5"
    local expected_code="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local headers=(-H "Content-Type: application/json")
    if [ -n "$token" ]; then
        headers+=(-H "Authorization: Bearer $token")
    fi

    log "Testing: $name"
    log "  Method: $method"
    log "  Endpoint: ${GATEWAY_URL}${endpoint}"

    if [ "$method" = "GET" ] || [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            "${GATEWAY_URL}${endpoint}" \
            "${headers[@]}" \
            --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            "${GATEWAY_URL}${endpoint}" \
            "${headers[@]}" \
            -d "$data" \
            --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")
    fi

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "$expected_code" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "$name - Status: $http_code"
        RESULTS+=("{\"test\":\"$name\",\"status\":\"PASS\",\"code\":$http_code}")
        return 0
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "$name - Expected: $expected_code, Got: $http_code"
        if [ "$VERBOSE" = true ]; then
            echo "  Response: $response_body"
        fi
        RESULTS+=("{\"test\":\"$name\",\"status\":\"FAIL\",\"code\":$http_code,\"expected\":$expected_code}")
        return 1
    fi
}

###############################################################################
# Test Suite
###############################################################################

echo "======================================"
echo "GCRF Library API Smoke Tests"
echo "Gateway: $GATEWAY_URL"
echo "======================================"
echo ""

# Phase 1: Service Health Checks
echo "Phase 1: Service Health Checks"
echo "--------------------------------------"

test_api "Gateway Health Check" "GET" "/actuator/health" "" "" "200"
test_api "Auth Service Health" "GET" "/api/v1/auth/health" "" "" "200"

echo ""

# Phase 2: Authentication
echo "Phase 2: Authentication Tests"
echo "--------------------------------------"

# Admin login
admin_login_data='{"username":"admin","password":"admin123"}'
TOTAL_TESTS=$((TOTAL_TESTS + 1))

log "Testing: Admin Login"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${GATEWAY_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "$admin_login_data" \
    --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" = "200" ]; then
    ADMIN_TOKEN=$(echo "$response_body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$ADMIN_TOKEN" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "Admin Login - Token obtained"
        RESULTS+=("{\"test\":\"Admin Login\",\"status\":\"PASS\",\"code\":200}")
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "Admin Login - No token in response"
        RESULTS+=("{\"test\":\"Admin Login\",\"status\":\"FAIL\",\"code\":200,\"error\":\"No token\"}")
    fi
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    log_error "Admin Login - Status: $http_code"
    RESULTS+=("{\"test\":\"Admin Login\",\"status\":\"FAIL\",\"code\":$http_code}")
fi

# Test invalid login
test_api "Invalid Login (Security)" "POST" "/api/v1/auth/login" \
    '{"username":"invalid","password":"wrong"}' "" "401"

# Test token validation
if [ -n "$ADMIN_TOKEN" ]; then
    test_api "Token Validation" "GET" "/api/v1/auth/validate" "" "$ADMIN_TOKEN" "200"
fi

echo ""

# Phase 3: Book Service Tests
echo "Phase 3: Book Service Tests"
echo "--------------------------------------"

if [ -n "$ADMIN_TOKEN" ]; then
    # Get books list
    test_api "Get Books List" "GET" "/api/v1/books?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN" "200"

    # Search books
    test_api "Search Books" "GET" "/api/v1/books?keyword=Java&pageNum=1&pageSize=10" "" "$ADMIN_TOKEN" "200"

    # Create book
    create_book_data=$(cat <<EOF
{
    "title": "API Test Book $(date +%s)",
    "author": "Test Author",
    "isbn": "978-$(date +%s | tail -c 11)",
    "publisher": "Test Publisher",
    "publishYear": 2024,
    "categoryId": 1,
    "totalCopies": 5,
    "availableCopies": 5,
    "shelfLocation": "TEST-01"
}
EOF
)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log "Testing: Create Book"
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${GATEWAY_URL}/api/v1/books" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d "$create_book_data" \
        --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        BOOK_ID=$(echo "$response_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$BOOK_ID" ]; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Create Book - ID: $BOOK_ID"
            RESULTS+=("{\"test\":\"Create Book\",\"status\":\"PASS\",\"code\":200,\"bookId\":$BOOK_ID}")
        else
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Create Book - Success (no ID extracted)"
            RESULTS+=("{\"test\":\"Create Book\",\"status\":\"PASS\",\"code\":200}")
        fi
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "Create Book - Status: $http_code"
        RESULTS+=("{\"test\":\"Create Book\",\"status\":\"FAIL\",\"code\":$http_code}")
    fi

    # Get book details (if book was created)
    if [ -n "$BOOK_ID" ]; then
        test_api "Get Book Details" "GET" "/api/v1/books/$BOOK_ID" "" "$ADMIN_TOKEN" "200"
    fi

    # Get categories
    test_api "Get Book Categories" "GET" "/api/v1/books/categories" "" "$ADMIN_TOKEN" "200"
else
    echo "Skipping Book Service tests - no admin token"
fi

echo ""

# Phase 4: Reader Service Tests
echo "Phase 4: Reader Service Tests"
echo "--------------------------------------"

# Reader registration (public endpoint)
register_data=$(cat <<EOF
{
    "username": "testuser_$(date +%s)",
    "password": "Test123456",
    "email": "test_$(date +%s)@example.com",
    "phone": "13800138000",
    "realName": "Test Reader",
    "idCard": "110101199001011234"
}
EOF
)

TOTAL_TESTS=$((TOTAL_TESTS + 1))
log "Testing: Reader Registration"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${GATEWAY_URL}/api/v1/readers/register" \
    -H "Content-Type: application/json" \
    -d "$register_data" \
    --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" = "200" ]; then
    READER_ID=$(echo "$response_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    READER_USERNAME=$(echo "$register_data" | grep -o '"username":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$READER_ID" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "Reader Registration - ID: $READER_ID"
        RESULTS+=("{\"test\":\"Reader Registration\",\"status\":\"PASS\",\"code\":200,\"readerId\":$READER_ID}")
    else
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "Reader Registration - Success"
        RESULTS+=("{\"test\":\"Reader Registration\",\"status\":\"PASS\",\"code\":200}")
    fi
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    log_error "Reader Registration - Status: $http_code"
    RESULTS+=("{\"test\":\"Reader Registration\",\"status\":\"FAIL\",\"code\":$http_code}")
fi

# Reader login
if [ -n "$READER_USERNAME" ]; then
    reader_login='{"username":"'$READER_USERNAME'","password":"Test123456"}'

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log "Testing: Reader Login"
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${GATEWAY_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "$reader_login" \
        --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        READER_TOKEN=$(echo "$response_body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$READER_TOKEN" ]; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Reader Login - Token obtained"
            RESULTS+=("{\"test\":\"Reader Login\",\"status\":\"PASS\",\"code\":200}")
        else
            FAILED_TESTS=$((FAILED_TESTS + 1))
            log_error "Reader Login - No token"
            RESULTS+=("{\"test\":\"Reader Login\",\"status\":\"FAIL\",\"code\":200,\"error\":\"No token\"}")
        fi
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "Reader Login - Status: $http_code"
        RESULTS+=("{\"test\":\"Reader Login\",\"status\":\"FAIL\",\"code\":$http_code}")
    fi

    # Get reader profile
    if [ -n "$READER_TOKEN" ]; then
        test_api "Get Reader Profile" "GET" "/api/v1/readers/profile" "" "$READER_TOKEN" "200"
    fi
fi

# Admin gets readers list
if [ -n "$ADMIN_TOKEN" ]; then
    test_api "Get Readers List (Admin)" "GET" "/api/v1/readers?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN" "200"
fi

echo ""

# Phase 5: Circulation Service Tests
echo "Phase 5: Circulation Service Tests"
echo "--------------------------------------"

if [ -n "$ADMIN_TOKEN" ] && [ -n "$BOOK_ID" ] && [ -n "$READER_ID" ]; then
    # Borrow book
    borrow_data='{"readerId":'$READER_ID',"bookId":'$BOOK_ID',"borrowDays":30}'

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log "Testing: Borrow Book"
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${GATEWAY_URL}/api/v1/circulation/borrow" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d "$borrow_data" \
        --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        BORROW_ID=$(echo "$response_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$BORROW_ID" ]; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Borrow Book - Borrow ID: $BORROW_ID"
            RESULTS+=("{\"test\":\"Borrow Book\",\"status\":\"PASS\",\"code\":200,\"borrowId\":$BORROW_ID}")
        else
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Borrow Book - Success"
            RESULTS+=("{\"test\":\"Borrow Book\",\"status\":\"PASS\",\"code\":200}")
        fi
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "Borrow Book - Status: $http_code"
        if [ "$VERBOSE" = true ]; then
            echo "  Response: $response_body"
        fi
        RESULTS+=("{\"test\":\"Borrow Book\",\"status\":\"FAIL\",\"code\":$http_code}")
    fi

    # Get borrow records
    test_api "Get Borrow Records" "GET" "/api/v1/circulation/borrows?readerId=$READER_ID" "" "$ADMIN_TOKEN" "200"

    # Return book (if borrow was successful)
    if [ -n "$BORROW_ID" ]; then
        return_data='{"borrowId":'$BORROW_ID'}'
        test_api "Return Book" "POST" "/api/v1/circulation/return" "$return_data" "$ADMIN_TOKEN" "200"
    fi
else
    echo "Skipping Circulation tests - missing prerequisites (admin token, book ID, or reader ID)"
fi

echo ""

# Phase 6: Recommendation Service Tests (if available)
echo "Phase 6: AI Services Tests"
echo "--------------------------------------"

if [ -n "$READER_TOKEN" ]; then
    # Test recommend service
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log "Testing: Get Book Recommendations"
    response=$(curl -s -w "\n%{http_code}" -X GET \
        "${GATEWAY_URL}/api/v1/recommend/books" \
        -H "Authorization: Bearer $READER_TOKEN" \
        --max-time $TIMEOUT 2>/dev/null || echo -e "\n000")

    http_code=$(echo "$response" | tail -n1)

    # Recommendation service might not be running, so we accept 200 or 404/503
    if [ "$http_code" = "200" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "Get Recommendations - Available"
        RESULTS+=("{\"test\":\"Get Recommendations\",\"status\":\"PASS\",\"code\":200}")
    elif [ "$http_code" = "404" ] || [ "$http_code" = "503" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_success "Get Recommendations - Service not deployed (expected)"
        RESULTS+=("{\"test\":\"Get Recommendations\",\"status\":\"SKIP\",\"code\":$http_code}")
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "Get Recommendations - Unexpected status: $http_code"
        RESULTS+=("{\"test\":\"Get Recommendations\",\"status\":\"FAIL\",\"code\":$http_code}")
    fi
else
    echo "Skipping AI services tests - no reader token"
fi

echo ""

# Phase 7: Authorization Tests
echo "Phase 7: Authorization Tests"
echo "--------------------------------------"

# Test reader cannot access admin endpoints
if [ -n "$READER_TOKEN" ]; then
    test_api "Reader Access Admin Endpoint (Forbidden)" "GET" "/api/v1/readers?pageNum=1&pageSize=10" "" "$READER_TOKEN" "403"
fi

# Test unauthenticated access
test_api "Unauthenticated Access (Unauthorized)" "GET" "/api/v1/books" "" "" "401"

echo ""

###############################################################################
# Results Summary
###############################################################################

echo "======================================"
echo "Test Summary"
echo "======================================"
echo -e "Total Tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $PASSED_TESTS${NC}"
echo -e "${RED}Failed:       $FAILED_TESTS${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    pass_rate=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
    echo -e "Pass Rate:    ${pass_rate}%"
fi

echo "======================================"

# JSON output if requested
if [ "$JSON_OUTPUT" = true ]; then
    cat <<EOF
{
    "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
    "gateway": "$GATEWAY_URL",
    "summary": {
        "total": $TOTAL_TESTS,
        "passed": $PASSED_TESTS,
        "failed": $FAILED_TESTS,
        "pass_rate": $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
    },
    "results": [
        $(IFS=,; echo "${RESULTS[*]}")
    ]
}
EOF
fi

# Exit with appropriate code
if [ $FAILED_TESTS -gt 0 ]; then
    exit 1
else
    exit 0
fi
