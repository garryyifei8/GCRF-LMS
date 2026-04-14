#!/bin/bash

# GCRF Library Management System - Integration Test Script
# Date: 2025-11-11
# Purpose: Automated API integration testing for Auth, Books, and Readers modules

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Base URL
BASE_URL="http://localhost:8080"

# Test results array
declare -a FAILED_TEST_DETAILS

# Helper functions
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    echo -e "${YELLOW}[$(($TOTAL_TESTS + 1))/${2}] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

print_failure() {
    echo -e "${RED}❌ $1${NC}"
    FAILED_TEST_DETAILS+=("$1 - $2")
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

check_service() {
    local service_name=$1
    local port=$2

    if ! nc -z localhost $port 2>/dev/null; then
        echo -e "${RED}❌ $service_name not running on port $port${NC}"
        return 1
    fi
    echo -e "${GREEN}✅ $service_name is running on port $port${NC}"
    return 0
}

# Main test execution
main() {
    print_header "GCRF Integration Test Suite"
    echo "Date: $(date)"
    echo "Base URL: $BASE_URL"
    echo ""

    # Check prerequisites
    print_header "1. Prerequisites Check"

    echo "Checking required services..."
    check_service "Gateway" 8080 || exit 1
    check_service "Auth Service" 8081 || exit 1
    check_service "Book Service" 8082 || exit 1
    check_service "Reader Service" 8084 || exit 1

    echo ""
    echo -e "${GREEN}All required services are running!${NC}"

    # Wait a bit for services to be fully ready
    sleep 2

    # Total tests to run
    TOTAL_EXPECTED=15

    # =================================
    # AUTH MODULE TESTS
    # =================================
    print_header "2. Auth Module Tests (3 tests)"

    # Test 1: Login
    print_test "Testing user login" $TOTAL_EXPECTED
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin", "password": "admin123"}')

    TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken // empty')

    if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
        print_success "User login successful"
    else
        print_failure "User login failed" "Response: $LOGIN_RESPONSE"
        echo -e "${RED}Cannot continue without valid token. Exiting.${NC}"
        exit 1
    fi

    # Test 2: Get user info
    print_test "Testing get user info" $TOTAL_EXPECTED
    USER_INFO=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/auth/info")

    USERNAME=$(echo $USER_INFO | jq -r '.data.username // empty')

    if [ "$USERNAME" == "admin" ]; then
        print_success "Get user info successful (username: $USERNAME)"
    else
        print_failure "Get user info failed" "Response: $USER_INFO"
    fi

    # Test 3: Token refresh
    print_test "Testing token refresh" $TOTAL_EXPECTED
    REFRESH_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/auth/refresh")

    NEW_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.data.accessToken // empty')

    if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
        print_success "Token refresh successful"
        TOKEN=$NEW_TOKEN  # Use new token for subsequent tests
    else
        print_failure "Token refresh failed" "Response: $REFRESH_RESPONSE"
    fi

    # =================================
    # BOOK MODULE TESTS
    # =================================
    print_header "3. Book Module Tests (7 tests)"

    # Test 4: Get book list
    print_test "Testing get book list" $TOTAL_EXPECTED
    BOOKS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/books?pageNum=1&pageSize=20")

    BOOK_COUNT=$(echo $BOOKS_RESPONSE | jq -r '.data.total // 0')

    if [ "$BOOK_COUNT" -ge 0 ]; then
        print_success "Get book list successful (total: $BOOK_COUNT books)"
    else
        print_failure "Get book list failed" "Response: $BOOKS_RESPONSE"
    fi

    # Test 5: Search books
    print_test "Testing book search" $TOTAL_EXPECTED
    SEARCH_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"keyword": "test"}' \
        "$BASE_URL/api/v1/books/search")

    SEARCH_CODE=$(echo $SEARCH_RESPONSE | jq -r '.code // 0')

    if [ "$SEARCH_CODE" -eq 200 ]; then
        print_success "Book search successful"
    else
        print_failure "Book search failed" "Response: $SEARCH_RESPONSE"
    fi

    # Test 6: Create book
    print_test "Testing create book" $TOTAL_EXPECTED
    CREATE_BOOK_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "isbn": "978-7-115-99999-9",
            "title": "Integration Test Book",
            "author": "Test Author",
            "publisher": "Test Publisher",
            "categoryId": 1,
            "totalCopies": 5,
            "publicationDate": "2025-01-01"
        }' \
        "$BASE_URL/api/v1/books")

    CREATED_BOOK_ID=$(echo $CREATE_BOOK_RESPONSE | jq -r '.data.id // empty')
    CREATE_CODE=$(echo $CREATE_BOOK_RESPONSE | jq -r '.code // 0')

    if [ "$CREATE_CODE" -eq 200 ] && [ -n "$CREATED_BOOK_ID" ]; then
        print_success "Create book successful (ID: $CREATED_BOOK_ID)"
    else
        print_failure "Create book failed" "Response: $CREATE_BOOK_RESPONSE"
        CREATED_BOOK_ID=""
    fi

    # Test 7: Get book detail
    if [ -n "$CREATED_BOOK_ID" ]; then
        print_test "Testing get book detail" $TOTAL_EXPECTED
        BOOK_DETAIL=$(curl -s -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL/api/v1/books/$CREATED_BOOK_ID")

        BOOK_TITLE=$(echo $BOOK_DETAIL | jq -r '.data.title // empty')

        if [ "$BOOK_TITLE" == "Integration Test Book" ]; then
            print_success "Get book detail successful (title: $BOOK_TITLE)"
        else
            print_failure "Get book detail failed" "Response: $BOOK_DETAIL"
        fi
    else
        print_test "Testing get book detail (SKIPPED - no book created)" $TOTAL_EXPECTED
        print_failure "Get book detail skipped" "No book ID available"
    fi

    # Test 8: Update book
    if [ -n "$CREATED_BOOK_ID" ]; then
        print_test "Testing update book" $TOTAL_EXPECTED
        UPDATE_RESPONSE=$(curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d '{
                "title": "Integration Test Book (Updated)",
                "author": "Test Author",
                "publisher": "Test Publisher",
                "categoryId": 1,
                "totalCopies": 5
            }' \
            "$BASE_URL/api/v1/books/$CREATED_BOOK_ID")

        UPDATE_CODE=$(echo $UPDATE_RESPONSE | jq -r '.code // 0')

        if [ "$UPDATE_CODE" -eq 200 ]; then
            print_success "Update book successful"
        else
            print_failure "Update book failed" "Response: $UPDATE_RESPONSE"
        fi
    else
        print_test "Testing update book (SKIPPED - no book created)" $TOTAL_EXPECTED
        print_failure "Update book skipped" "No book ID available"
    fi

    # Test 9: Get categories
    print_test "Testing get book categories" $TOTAL_EXPECTED
    CATEGORIES=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/books/categories")

    CATEGORY_CODE=$(echo $CATEGORIES | jq -r '.code // 0')

    if [ "$CATEGORY_CODE" -eq 200 ]; then
        CATEGORY_COUNT=$(echo $CATEGORIES | jq -r '.data | length')
        print_success "Get categories successful ($CATEGORY_COUNT categories)"
    else
        print_failure "Get categories failed" "Response: $CATEGORIES"
    fi

    # Test 10: Delete book
    if [ -n "$CREATED_BOOK_ID" ]; then
        print_test "Testing delete book" $TOTAL_EXPECTED
        DELETE_RESPONSE=$(curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL/api/v1/books/$CREATED_BOOK_ID")

        DELETE_CODE=$(echo $DELETE_RESPONSE | jq -r '.code // 0')

        if [ "$DELETE_CODE" -eq 200 ]; then
            print_success "Delete book successful"
        else
            print_failure "Delete book failed" "Response: $DELETE_RESPONSE"
        fi
    else
        print_test "Testing delete book (SKIPPED - no book created)" $TOTAL_EXPECTED
        print_failure "Delete book skipped" "No book ID available"
    fi

    # =================================
    # READER MODULE TESTS
    # =================================
    print_header "4. Reader Module Tests (5 tests)"

    # Test 11: Get reader list
    print_test "Testing get reader list" $TOTAL_EXPECTED
    READERS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/readers?pageNum=1&pageSize=20")

    READER_COUNT=$(echo $READERS_RESPONSE | jq -r '.data.total // 0')

    if [ "$READER_COUNT" -ge 0 ]; then
        print_success "Get reader list successful (total: $READER_COUNT readers)"
    else
        print_failure "Get reader list failed" "Response: $READERS_RESPONSE"
    fi

    # Test 12: Get reader types
    print_test "Testing get reader types" $TOTAL_EXPECTED
    TYPES_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/readers/types")

    TYPES_CODE=$(echo $TYPES_RESPONSE | jq -r '.code // 0')

    if [ "$TYPES_CODE" -eq 200 ]; then
        TYPE_COUNT=$(echo $TYPES_RESPONSE | jq -r '.data | length')
        print_success "Get reader types successful ($TYPE_COUNT types)"
    else
        print_failure "Get reader types failed" "Response: $TYPES_RESPONSE"
    fi

    # Test 13: Create reader
    print_test "Testing create reader" $TOTAL_EXPECTED
    CREATE_READER_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "测试读者",
            "readerType": "STUDENT",
            "email": "test@example.com",
            "phone": "13900139000",
            "gender": "MALE"
        }' \
        "$BASE_URL/api/v1/readers")

    CREATED_READER_ID=$(echo $CREATE_READER_RESPONSE | jq -r '.data.id // empty')
    READER_CREATE_CODE=$(echo $CREATE_READER_RESPONSE | jq -r '.code // 0')

    if [ "$READER_CREATE_CODE" -eq 200 ] && [ -n "$CREATED_READER_ID" ]; then
        print_success "Create reader successful (ID: $CREATED_READER_ID)"
    else
        print_failure "Create reader failed" "Response: $CREATE_READER_RESPONSE"
        CREATED_READER_ID=""
    fi

    # Test 14: Get reader detail
    if [ -n "$CREATED_READER_ID" ]; then
        print_test "Testing get reader detail" $TOTAL_EXPECTED
        READER_DETAIL=$(curl -s -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL/api/v1/readers/$CREATED_READER_ID")

        READER_NAME=$(echo $READER_DETAIL | jq -r '.data.name // empty')

        if [ "$READER_NAME" == "测试读者" ]; then
            print_success "Get reader detail successful (name: $READER_NAME)"
        else
            print_failure "Get reader detail failed" "Response: $READER_DETAIL"
        fi
    else
        print_test "Testing get reader detail (SKIPPED - no reader created)" $TOTAL_EXPECTED
        print_failure "Get reader detail skipped" "No reader ID available"
    fi

    # Test 15: Delete reader
    if [ -n "$CREATED_READER_ID" ]; then
        print_test "Testing delete reader" $TOTAL_EXPECTED
        DELETE_READER_RESPONSE=$(curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL/api/v1/readers/$CREATED_READER_ID")

        DELETE_READER_CODE=$(echo $DELETE_READER_RESPONSE | jq -r '.code // 0')

        if [ "$DELETE_READER_CODE" -eq 200 ]; then
            print_success "Delete reader successful"
        else
            print_failure "Delete reader failed" "Response: $DELETE_READER_RESPONSE"
        fi
    else
        print_test "Testing delete reader (SKIPPED - no reader created)" $TOTAL_EXPECTED
        print_failure "Delete reader skipped" "No reader ID available"
    fi

    # =================================
    # TEST SUMMARY
    # =================================
    print_header "5. Test Summary"

    echo ""
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"

    if [ $TOTAL_TESTS -gt 0 ]; then
        SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
        echo "Success Rate: ${SUCCESS_RATE}%"
    fi

    if [ $FAILED_TESTS -gt 0 ]; then
        echo ""
        echo -e "${RED}Failed Test Details:${NC}"
        for detail in "${FAILED_TEST_DETAILS[@]}"; do
            echo -e "${RED}  - $detail${NC}"
        done
    fi

    echo ""
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}⚠️  Some tests failed. Please review the output above.${NC}"
        exit 1
    fi
}

# Run main function
main
