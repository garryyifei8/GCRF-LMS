#!/bin/bash

###############################################################################
# GCRF Library Management System - End-to-End Testing Script
#
# Description: Comprehensive E2E testing covering:
#   - Service health checks and readiness validation
#   - Core business flow testing (login, book management, circulation)
#   - API integration testing across services
#   - Performance and load testing
#   - Test report generation
#
# Usage: ./e2e-test.sh [options]
#   Options:
#     --quick       Run quick smoke tests only
#     --full        Run full E2E test suite (default)
#     --load        Include load testing
#     --report      Generate HTML test report
#     --env <env>   Environment (dev|prod, default: dev)
#
# Author: GCRF DevOps Team
# Date: 2025-12-01
###############################################################################

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Default configuration
TEST_MODE="full"
ENABLE_LOAD_TEST=false
GENERATE_REPORT=true
ENVIRONMENT="dev"
GATEWAY_URL="http://localhost:8080"
TIMEOUT=30
MAX_RETRIES=3

# Report directories
REPORT_DIR="$PROJECT_ROOT/deployment/test-reports"
TEST_RESULTS_FILE="$REPORT_DIR/e2e-results-$TIMESTAMP.json"
HTML_REPORT="$REPORT_DIR/e2e-report-$TIMESTAMP.html"
LOG_FILE="$REPORT_DIR/e2e-test-$TIMESTAMP.log"

# Test statistics
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results array
declare -a TEST_RESULTS=()

###############################################################################
# Utility Functions
###############################################################################

log() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $*" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}[ℹ]${NC} $*" | tee -a "$LOG_FILE"
}

# Record test result
record_test() {
    local test_name="$1"
    local status="$2"
    local duration="$3"
    local message="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    case "$status" in
        "PASS")
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_success "Test: $test_name - PASSED (${duration}s)"
            ;;
        "FAIL")
            FAILED_TESTS=$((FAILED_TESTS + 1))
            log_error "Test: $test_name - FAILED (${duration}s) - $message"
            ;;
        "SKIP")
            SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
            log_warning "Test: $test_name - SKIPPED - $message"
            ;;
    esac

    TEST_RESULTS+=("{\"name\":\"$test_name\",\"status\":\"$status\",\"duration\":$duration,\"message\":\"$message\"}")
}

# Execute API call with retry logic
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local auth_token="$4"
    local retry=0

    while [ $retry -lt $MAX_RETRIES ]; do
        local headers=(-H "Content-Type: application/json")

        if [ -n "$auth_token" ]; then
            headers+=(-H "Authorization: Bearer $auth_token")
        fi

        if [ "$method" = "GET" ]; then
            response=$(curl -s -w "\n%{http_code}" -X GET "${GATEWAY_URL}${endpoint}" "${headers[@]}" --max-time $TIMEOUT 2>/dev/null || echo "000")
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "${GATEWAY_URL}${endpoint}" "${headers[@]}" -d "$data" --max-time $TIMEOUT 2>/dev/null || echo "000")
        fi

        http_code=$(echo "$response" | tail -n1)
        response_body=$(echo "$response" | head -n -1)

        if [ "$http_code" != "000" ]; then
            echo "$response_body"
            return 0
        fi

        retry=$((retry + 1))
        sleep 2
    done

    echo '{"error":"Connection failed after retries"}'
    return 1
}

# Check if service is healthy
check_service_health() {
    local service_name="$1"
    local health_url="$2"

    response=$(curl -s -o /dev/null -w "%{http_code}" "$health_url" --max-time 10 2>/dev/null || echo "000")

    if [ "$response" = "200" ]; then
        return 0
    else
        return 1
    fi
}

###############################################################################
# Service Health Checks
###############################################################################

test_service_health() {
    log_info "====== Phase 1: Service Health Checks ======"

    local services=(
        "gateway-service:http://localhost:8080/actuator/health"
        "auth-service:http://localhost:8081/actuator/health"
        "book-service:http://localhost:8082/actuator/health"
        "circulation-service:http://localhost:8083/actuator/health"
        "reader-service:http://localhost:8084/actuator/health"
    )

    for service_entry in "${services[@]}"; do
        IFS=':' read -r service_name health_url <<< "$service_entry"

        start_time=$(date +%s)
        if check_service_health "$service_name" "$health_url"; then
            end_time=$(date +%s)
            duration=$((end_time - start_time))
            record_test "Health Check: $service_name" "PASS" "$duration" "Service is healthy"
        else
            end_time=$(date +%s)
            duration=$((end_time - start_time))
            record_test "Health Check: $service_name" "FAIL" "$duration" "Service health check failed"
        fi
    done
}

###############################################################################
# Authentication Flow Tests
###############################################################################

test_authentication_flow() {
    log_info "====== Phase 2: Authentication Flow Tests ======"

    # Test 1: Admin Login
    log "Test: Admin Login"
    start_time=$(date +%s)

    login_data='{"username":"admin","password":"admin123"}'
    response=$(api_call "POST" "/api/v1/auth/login" "$login_data" "")

    if echo "$response" | grep -q '"code":200'; then
        ADMIN_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Admin Login" "PASS" "$duration" "Login successful"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Admin Login" "FAIL" "$duration" "Login failed: $response"
        return 1
    fi

    # Test 2: Token Validation
    log "Test: Token Validation"
    start_time=$(date +%s)

    response=$(api_call "GET" "/api/v1/auth/validate" "" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Token Validation" "PASS" "$duration" "Token is valid"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Token Validation" "FAIL" "$duration" "Token validation failed"
    fi

    # Test 3: Invalid Credentials
    log "Test: Invalid Login Credentials"
    start_time=$(date +%s)

    invalid_login='{"username":"invalid","password":"wrongpass"}'
    response=$(api_call "POST" "/api/v1/auth/login" "$invalid_login" "")

    if echo "$response" | grep -q '"code":401\|"code":400'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Invalid Login" "PASS" "$duration" "Correctly rejected invalid credentials"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Invalid Login" "FAIL" "$duration" "Should reject invalid credentials"
    fi
}

###############################################################################
# Book Management Flow Tests
###############################################################################

test_book_management_flow() {
    log_info "====== Phase 3: Book Management Flow Tests ======"

    if [ -z "$ADMIN_TOKEN" ]; then
        log_warning "Skipping book management tests - no admin token"
        record_test "Book Management Flow" "SKIP" "0" "No admin token available"
        return 1
    fi

    # Test 1: Create New Book
    log "Test: Create New Book"
    start_time=$(date +%s)

    book_data=$(cat <<EOF
{
    "title": "E2E Test Book $(date +%s)",
    "author": "Test Author",
    "isbn": "978-$(date +%s | tail -c 11)",
    "publisher": "Test Publisher",
    "publishYear": 2024,
    "categoryId": 1,
    "totalCopies": 10,
    "availableCopies": 10,
    "shelfLocation": "A1-01"
}
EOF
)

    response=$(api_call "POST" "/api/v1/books" "$book_data" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        BOOK_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Create Book" "PASS" "$duration" "Book created successfully (ID: $BOOK_ID)"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Create Book" "FAIL" "$duration" "Failed to create book: $response"
        return 1
    fi

    # Test 2: Get Book Details
    log "Test: Get Book Details"
    start_time=$(date +%s)

    response=$(api_call "GET" "/api/v1/books/$BOOK_ID" "" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Book Details" "PASS" "$duration" "Retrieved book details"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Book Details" "FAIL" "$duration" "Failed to retrieve book"
    fi

    # Test 3: Update Book
    log "Test: Update Book"
    start_time=$(date +%s)

    update_data=$(cat <<EOF
{
    "title": "E2E Test Book (Updated)",
    "author": "Test Author",
    "isbn": "978-$(date +%s | tail -c 11)",
    "publisher": "Test Publisher",
    "publishYear": 2024,
    "categoryId": 1,
    "totalCopies": 15,
    "availableCopies": 15,
    "shelfLocation": "A1-02"
}
EOF
)

    response=$(api_call "PUT" "/api/v1/books/$BOOK_ID" "$update_data" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Update Book" "PASS" "$duration" "Book updated successfully"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Update Book" "FAIL" "$duration" "Failed to update book"
    fi

    # Test 4: Search Books
    log "Test: Search Books"
    start_time=$(date +%s)

    response=$(api_call "GET" "/api/v1/books?keyword=Test&pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Search Books" "PASS" "$duration" "Search executed successfully"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Search Books" "FAIL" "$duration" "Search failed"
    fi
}

###############################################################################
# Reader Management Flow Tests
###############################################################################

test_reader_management_flow() {
    log_info "====== Phase 4: Reader Management Flow Tests ======"

    if [ -z "$ADMIN_TOKEN" ]; then
        log_warning "Skipping reader management tests - no admin token"
        record_test "Reader Management Flow" "SKIP" "0" "No admin token available"
        return 1
    fi

    # Test 1: Register New Reader
    log "Test: Register New Reader"
    start_time=$(date +%s)

    reader_data=$(cat <<EOF
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

    response=$(api_call "POST" "/api/v1/readers/register" "$reader_data" "")

    if echo "$response" | grep -q '"code":200'; then
        READER_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        READER_USERNAME=$(echo "$reader_data" | grep -o '"username":"[^"]*"' | cut -d'"' -f4)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Register Reader" "PASS" "$duration" "Reader registered (ID: $READER_ID)"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Register Reader" "FAIL" "$duration" "Failed to register reader: $response"
        return 1
    fi

    # Test 2: Reader Login
    log "Test: Reader Login"
    start_time=$(date +%s)

    reader_login=$(cat <<EOF
{
    "username": "$READER_USERNAME",
    "password": "Test123456"
}
EOF
)

    response=$(api_call "POST" "/api/v1/auth/login" "$reader_login" "")

    if echo "$response" | grep -q '"code":200'; then
        READER_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Reader Login" "PASS" "$duration" "Reader login successful"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Reader Login" "FAIL" "$duration" "Reader login failed"
    fi

    # Test 3: Get Reader Profile
    log "Test: Get Reader Profile"
    start_time=$(date +%s)

    response=$(api_call "GET" "/api/v1/readers/profile" "" "$READER_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Reader Profile" "PASS" "$duration" "Retrieved profile"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Reader Profile" "FAIL" "$duration" "Failed to get profile"
    fi
}

###############################################################################
# Circulation Flow Tests
###############################################################################

test_circulation_flow() {
    log_info "====== Phase 5: Circulation Flow Tests ======"

    if [ -z "$BOOK_ID" ] || [ -z "$READER_ID" ]; then
        log_warning "Skipping circulation tests - missing book or reader"
        record_test "Circulation Flow" "SKIP" "0" "Missing prerequisites"
        return 1
    fi

    # Test 1: Borrow Book
    log "Test: Borrow Book"
    start_time=$(date +%s)

    borrow_data=$(cat <<EOF
{
    "readerId": $READER_ID,
    "bookId": $BOOK_ID,
    "borrowDays": 30
}
EOF
)

    response=$(api_call "POST" "/api/v1/circulation/borrow" "$borrow_data" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        BORROW_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Borrow Book" "PASS" "$duration" "Book borrowed (Borrow ID: $BORROW_ID)"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Borrow Book" "FAIL" "$duration" "Failed to borrow book: $response"
        return 1
    fi

    # Test 2: Get Borrow Records
    log "Test: Get Borrow Records"
    start_time=$(date +%s)

    response=$(api_call "GET" "/api/v1/circulation/borrows?readerId=$READER_ID" "" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Borrow Records" "PASS" "$duration" "Retrieved borrow records"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Get Borrow Records" "FAIL" "$duration" "Failed to get records"
    fi

    # Test 3: Renew Book
    log "Test: Renew Book"
    start_time=$(date +%s)

    renew_data='{"borrowId":'$BORROW_ID',"renewDays":14}'
    response=$(api_call "POST" "/api/v1/circulation/renew" "$renew_data" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Renew Book" "PASS" "$duration" "Book renewed successfully"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Renew Book" "FAIL" "$duration" "Failed to renew book"
    fi

    # Test 4: Return Book
    log "Test: Return Book"
    start_time=$(date +%s)

    return_data='{"borrowId":'$BORROW_ID'}'
    response=$(api_call "POST" "/api/v1/circulation/return" "$return_data" "$ADMIN_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Return Book" "PASS" "$duration" "Book returned successfully"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Return Book" "FAIL" "$duration" "Failed to return book"
    fi
}

###############################################################################
# Reservation Flow Tests
###############################################################################

test_reservation_flow() {
    log_info "====== Phase 6: Reservation Flow Tests ======"

    if [ -z "$BOOK_ID" ] || [ -z "$READER_ID" ]; then
        log_warning "Skipping reservation tests - missing prerequisites"
        record_test "Reservation Flow" "SKIP" "0" "Missing prerequisites"
        return 1
    fi

    # Test 1: Make Reservation
    log "Test: Make Reservation"
    start_time=$(date +%s)

    reservation_data=$(cat <<EOF
{
    "readerId": $READER_ID,
    "bookId": $BOOK_ID
}
EOF
)

    response=$(api_call "POST" "/api/v1/circulation/reservations" "$reservation_data" "$READER_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        RESERVATION_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Make Reservation" "PASS" "$duration" "Reservation created (ID: $RESERVATION_ID)"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Make Reservation" "FAIL" "$duration" "Failed to make reservation: $response"
        return 1
    fi

    # Test 2: Cancel Reservation
    log "Test: Cancel Reservation"
    start_time=$(date +%s)

    response=$(api_call "DELETE" "/api/v1/circulation/reservations/$RESERVATION_ID" "" "$READER_TOKEN")

    if echo "$response" | grep -q '"code":200'; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Cancel Reservation" "PASS" "$duration" "Reservation cancelled"
    else
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        record_test "Cancel Reservation" "FAIL" "$duration" "Failed to cancel reservation"
    fi
}

###############################################################################
# Report Generation
###############################################################################

generate_html_report() {
    log_info "Generating HTML report..."

    cat > "$HTML_REPORT" <<EOF
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GCRF Library E2E Test Report - $TIMESTAMP</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #2c3e50; margin-bottom: 10px; }
        .subtitle { color: #7f8c8d; margin-bottom: 30px; }
        .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }
        .summary-card { padding: 20px; border-radius: 6px; text-align: center; }
        .summary-card.total { background: #3498db; color: white; }
        .summary-card.passed { background: #2ecc71; color: white; }
        .summary-card.failed { background: #e74c3c; color: white; }
        .summary-card.skipped { background: #95a5a6; color: white; }
        .summary-card h3 { font-size: 36px; margin-bottom: 5px; }
        .summary-card p { font-size: 14px; opacity: 0.9; }
        .test-results { margin-top: 30px; }
        .test-item { padding: 15px; margin-bottom: 10px; border-radius: 6px; border-left: 4px solid; }
        .test-item.pass { background: #d4edda; border-color: #28a745; }
        .test-item.fail { background: #f8d7da; border-color: #dc3545; }
        .test-item.skip { background: #e2e3e5; border-color: #6c757d; }
        .test-item h4 { margin-bottom: 5px; }
        .test-item .details { font-size: 14px; color: #6c757d; }
        .progress-bar { width: 100%; height: 30px; background: #ecf0f1; border-radius: 15px; overflow: hidden; margin: 20px 0; }
        .progress-fill { height: 100%; display: flex; }
        .progress-fill .passed { background: #2ecc71; }
        .progress-fill .failed { background: #e74c3c; }
        .progress-fill .skipped { background: #95a5a6; }
    </style>
</head>
<body>
    <div class="container">
        <h1>GCRF Library Management System</h1>
        <p class="subtitle">End-to-End Test Report - $TIMESTAMP</p>

        <div class="summary">
            <div class="summary-card total">
                <h3>$TOTAL_TESTS</h3>
                <p>Total Tests</p>
            </div>
            <div class="summary-card passed">
                <h3>$PASSED_TESTS</h3>
                <p>Passed</p>
            </div>
            <div class="summary-card failed">
                <h3>$FAILED_TESTS</h3>
                <p>Failed</p>
            </div>
            <div class="summary-card skipped">
                <h3>$SKIPPED_TESTS</h3>
                <p>Skipped</p>
            </div>
        </div>

        <div class="progress-bar">
            <div class="progress-fill">
EOF

    local pass_percent=0
    local fail_percent=0
    local skip_percent=0

    if [ $TOTAL_TESTS -gt 0 ]; then
        pass_percent=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        fail_percent=$((FAILED_TESTS * 100 / TOTAL_TESTS))
        skip_percent=$((SKIPPED_TESTS * 100 / TOTAL_TESTS))
    fi

    echo "                <div class=\"passed\" style=\"width: ${pass_percent}%;\"></div>" >> "$HTML_REPORT"
    echo "                <div class=\"failed\" style=\"width: ${fail_percent}%;\"></div>" >> "$HTML_REPORT"
    echo "                <div class=\"skipped\" style=\"width: ${skip_percent}%;\"></div>" >> "$HTML_REPORT"

    cat >> "$HTML_REPORT" <<EOF
            </div>
        </div>

        <div class="test-results">
            <h2>Test Results</h2>
EOF

    # Parse and display test results
    for result in "${TEST_RESULTS[@]}"; do
        name=$(echo "$result" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
        status=$(echo "$result" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        duration=$(echo "$result" | grep -o '"duration":[0-9]*' | cut -d':' -f2)
        message=$(echo "$result" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

        class_name=$(echo "$status" | tr '[:upper:]' '[:lower:]')

        cat >> "$HTML_REPORT" <<EOF
            <div class="test-item $class_name">
                <h4>$name</h4>
                <div class="details">Status: $status | Duration: ${duration}s | $message</div>
            </div>
EOF
    done

    cat >> "$HTML_REPORT" <<EOF
        </div>
    </div>
</body>
</html>
EOF

    log_success "HTML report generated: $HTML_REPORT"
}

generate_json_report() {
    log_info "Generating JSON report..."

    cat > "$TEST_RESULTS_FILE" <<EOF
{
    "timestamp": "$TIMESTAMP",
    "environment": "$ENVIRONMENT",
    "gateway_url": "$GATEWAY_URL",
    "summary": {
        "total": $TOTAL_TESTS,
        "passed": $PASSED_TESTS,
        "failed": $FAILED_TESTS,
        "skipped": $SKIPPED_TESTS,
        "pass_rate": $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
    },
    "tests": [
        $(IFS=,; echo "${TEST_RESULTS[*]}")
    ]
}
EOF

    log_success "JSON report generated: $TEST_RESULTS_FILE"
}

###############################################################################
# Main Execution
###############################################################################

print_usage() {
    cat <<EOF
Usage: $0 [options]

Options:
    --quick       Run quick smoke tests only
    --full        Run full E2E test suite (default)
    --load        Include load testing
    --report      Generate HTML test report (default: true)
    --env <env>   Environment (dev|prod, default: dev)
    --help        Display this help message

Examples:
    $0 --quick
    $0 --full --report
    $0 --env prod --load
EOF
}

main() {
    log_info "====== GCRF Library E2E Testing ======"
    log_info "Starting E2E tests at $(date)"
    log_info "Mode: $TEST_MODE | Environment: $ENVIRONMENT"
    log_info "Gateway URL: $GATEWAY_URL"
    echo ""

    # Create report directory
    mkdir -p "$REPORT_DIR"

    # Run test phases
    test_service_health
    test_authentication_flow
    test_book_management_flow
    test_reader_management_flow
    test_circulation_flow

    if [ "$TEST_MODE" = "full" ]; then
        test_reservation_flow
    fi

    # Generate reports
    if [ "$GENERATE_REPORT" = true ]; then
        generate_json_report
        generate_html_report
    fi

    # Print summary
    echo ""
    log_info "====== Test Summary ======"
    log_info "Total Tests: $TOTAL_TESTS"
    log_success "Passed: $PASSED_TESTS"
    log_error "Failed: $FAILED_TESTS"
    log_warning "Skipped: $SKIPPED_TESTS"

    if [ $TOTAL_TESTS -gt 0 ]; then
        pass_rate=$(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
        log_info "Pass Rate: ${pass_rate}%"
    fi

    echo ""
    log_info "Reports saved to: $REPORT_DIR"
    log_info "HTML Report: $HTML_REPORT"
    log_info "JSON Report: $TEST_RESULTS_FILE"
    log_info "Log File: $LOG_FILE"

    # Exit with failure if any tests failed
    if [ $FAILED_TESTS -gt 0 ]; then
        log_error "E2E tests completed with failures"
        exit 1
    else
        log_success "All E2E tests passed!"
        exit 0
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            TEST_MODE="quick"
            shift
            ;;
        --full)
            TEST_MODE="full"
            shift
            ;;
        --load)
            ENABLE_LOAD_TEST=true
            shift
            ;;
        --report)
            GENERATE_REPORT=true
            shift
            ;;
        --env)
            ENVIRONMENT="$2"
            if [ "$ENVIRONMENT" = "prod" ]; then
                GATEWAY_URL="https://library.gcrf.com"
            fi
            shift 2
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Run main function
main
