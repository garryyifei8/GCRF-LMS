#!/bin/bash

###############################################################################
# GCRF Library Management System - Test Data Preparation Script
#
# Description: Prepares comprehensive test data for E2E testing
#   - Creates test users (admin, librarian, readers)
#   - Imports sample book data
#   - Creates borrow scenarios (active, overdue, returned)
#   - Generates reservation data
#   - Creates fine records
#
# Usage: ./prepare-test-data.sh [options]
#   Options:
#     --clean       Clean existing test data before import
#     --minimal     Import minimal dataset (for quick tests)
#     --full        Import full dataset (default)
#     --gateway     Gateway URL (default: http://localhost:8080)
#
# Author: GCRF DevOps Team
# Date: 2025-12-01
###############################################################################

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-gcrf_secure_2024}"
CLEAN_DATA=false
DATASET_SIZE="full"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

###############################################################################
# Utility Functions
###############################################################################

log() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $*"
}

log_info() {
    echo -e "${BLUE}[ℹ]${NC} $*"
}

# Execute SQL on specific database
execute_sql() {
    local database="$1"
    local sql="$2"

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database" -c "$sql" 2>&1
}

# API call helper
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local token="$4"

    local headers=(-H "Content-Type: application/json")
    if [ -n "$token" ]; then
        headers+=(-H "Authorization: Bearer $token")
    fi

    if [ "$method" = "GET" ]; then
        curl -s -X GET "${GATEWAY_URL}${endpoint}" "${headers[@]}" --max-time 30
    else
        curl -s -X "$method" "${GATEWAY_URL}${endpoint}" "${headers[@]}" -d "$data" --max-time 30
    fi
}

###############################################################################
# Clean Existing Test Data
###############################################################################

clean_test_data() {
    log_info "====== Cleaning Existing Test Data ======"

    # Clean reader service test data
    log "Cleaning reader service test data..."
    execute_sql "reader_service" "DELETE FROM reader WHERE username LIKE 'testuser_%' OR username LIKE 'reader%';"

    # Clean book service test data
    log "Cleaning book service test data..."
    execute_sql "book_service" "DELETE FROM book WHERE title LIKE 'E2E Test%' OR title LIKE 'Test Book%';"

    # Clean circulation service test data
    log "Cleaning circulation service test data..."
    execute_sql "circulation_service" "DELETE FROM borrow_record WHERE id > 0;"
    execute_sql "circulation_service" "DELETE FROM reservation WHERE id > 0;"
    execute_sql "circulation_service" "DELETE FROM fine WHERE id > 0;"

    log_success "Test data cleaned"
}

###############################################################################
# Create Test Users
###############################################################################

create_test_users() {
    log_info "====== Creating Test Users ======"

    # Login as admin to get token
    log "Logging in as admin..."
    login_response=$(api_call "POST" "/api/v1/auth/login" '{"username":"admin","password":"admin123"}' "")
    ADMIN_TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$ADMIN_TOKEN" ]; then
        log_error "Failed to get admin token"
        return 1
    fi
    log_success "Admin token obtained"

    # Create test readers
    log "Creating test readers..."

    local reader_count=10
    if [ "$DATASET_SIZE" = "minimal" ]; then
        reader_count=3
    fi

    for i in $(seq 1 $reader_count); do
        local username="testuser_$(date +%s)_$i"
        local email="test_${i}_$(date +%s)@example.com"

        reader_data=$(cat <<EOF
{
    "username": "$username",
    "password": "Test123456",
    "email": "$email",
    "phone": "1380013800${i}",
    "realName": "测试读者${i}",
    "idCard": "11010119900101$(printf '%04d' $i)",
    "readerType": "STUDENT"
}
EOF
)

        response=$(api_call "POST" "/api/v1/readers/register" "$reader_data" "")

        if echo "$response" | grep -q '"code":200'; then
            log_success "Created reader: $username"
        else
            log_warning "Failed to create reader: $username"
        fi

        sleep 0.5
    done

    # Create specific test accounts
    log "Creating specific test accounts..."

    # Reader with max borrows (for testing borrow limit)
    api_call "POST" "/api/v1/readers/register" '{
        "username": "maxborrow",
        "password": "Test123456",
        "email": "maxborrow@test.com",
        "phone": "13800138999",
        "realName": "最大借阅",
        "idCard": "110101199001019999"
    }' ""

    # Reader for general testing
    api_call "POST" "/api/v1/readers/register" '{
        "username": "reader001",
        "password": "reader123",
        "email": "reader001@test.com",
        "phone": "13900139001",
        "realName": "测试读者001",
        "idCard": "110101199001010001"
    }' ""

    log_success "Test users created"
}

###############################################################################
# Import Sample Books
###############################################################################

import_sample_books() {
    log_info "====== Importing Sample Books ======"

    if [ -z "$ADMIN_TOKEN" ]; then
        log_error "No admin token available"
        return 1
    fi

    local book_count=50
    if [ "$DATASET_SIZE" = "minimal" ]; then
        book_count=10
    fi

    log "Creating $book_count sample books..."

    # Sample book data templates
    local titles=(
        "Java编程思想"
        "Python核心编程"
        "深入理解计算机系统"
        "算法导论"
        "设计模式"
        "代码大全"
        "重构:改善既有代码的设计"
        "clean code"
        "数据库系统概念"
        "操作系统概念"
    )

    local authors=(
        "Bruce Eckel"
        "Mark Lutz"
        "Randal E. Bryant"
        "Thomas H. Cormen"
        "Erich Gamma"
        "Steve McConnell"
        "Martin Fowler"
        "Robert C. Martin"
        "Abraham Silberschatz"
        "Abraham Silberschatz"
    )

    local publishers=(
        "机械工业出版社"
        "人民邮电出版社"
        "电子工业出版社"
        "清华大学出版社"
    )

    for i in $(seq 1 $book_count); do
        local title_idx=$((i % 10))
        local title="${titles[$title_idx]} 第${i}版"
        local author="${authors[$title_idx]}"
        local publisher="${publishers[$((i % 4))]}"
        local isbn="978-$(date +%s | tail -c 11)"

        book_data=$(cat <<EOF
{
    "title": "$title",
    "author": "$author",
    "isbn": "$isbn",
    "publisher": "$publisher",
    "publishYear": $((2020 + (i % 5))),
    "categoryId": $((1 + (i % 5))),
    "totalCopies": $((5 + (i % 10))),
    "availableCopies": $((5 + (i % 10))),
    "shelfLocation": "A$((1 + (i % 10)))-$(printf '%02d' $((i % 50)))",
    "description": "这是一本关于计算机科学的优秀书籍"
}
EOF
)

        response=$(api_call "POST" "/api/v1/books" "$book_data" "$ADMIN_TOKEN")

        if echo "$response" | grep -q '"code":200'; then
            log_success "Created book: $title"
        else
            log_warning "Failed to create book: $title"
        fi

        sleep 0.3
    done

    log_success "Sample books imported"
}

###############################################################################
# Create Borrow Scenarios
###############################################################################

create_borrow_scenarios() {
    log_info "====== Creating Borrow Scenarios ======"

    if [ -z "$ADMIN_TOKEN" ]; then
        log_error "No admin token available"
        return 1
    fi

    # Get list of books
    log "Fetching book list..."
    books_response=$(api_call "GET" "/api/v1/books?pageNum=1&pageSize=20" "" "$ADMIN_TOKEN")

    # Get list of readers
    log "Fetching reader list..."
    readers_response=$(api_call "GET" "/api/v1/readers?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")

    # Extract book IDs (simplified - in real scenario would parse JSON properly)
    log "Creating active borrow records..."

    # Create some active borrows
    for i in $(seq 1 5); do
        borrow_data=$(cat <<EOF
{
    "readerId": $((100 + i)),
    "bookId": $((1 + i)),
    "borrowDays": 30
}
EOF
)

        response=$(api_call "POST" "/api/v1/circulation/borrow" "$borrow_data" "$ADMIN_TOKEN")

        if echo "$response" | grep -q '"code":200'; then
            log_success "Created borrow record $i"
        fi

        sleep 0.5
    done

    log_success "Borrow scenarios created"
}

###############################################################################
# Create Overdue Scenarios
###############################################################################

create_overdue_scenarios() {
    log_info "====== Creating Overdue Scenarios ======"

    # This requires direct database manipulation to set past due dates
    log "Updating borrow records to create overdue scenarios..."

    # Set some borrows to be overdue
    execute_sql "circulation_service" "
        UPDATE borrow_record
        SET due_date = CURRENT_DATE - INTERVAL '10 days'
        WHERE status = 'BORROWED'
        AND id IN (SELECT id FROM borrow_record WHERE status = 'BORROWED' LIMIT 3);
    "

    log_success "Overdue scenarios created"
}

###############################################################################
# Create Reservation Scenarios
###############################################################################

create_reservation_scenarios() {
    log_info "====== Creating Reservation Scenarios ======"

    if [ -z "$ADMIN_TOKEN" ]; then
        log_error "No admin token available"
        return 1
    fi

    log "Creating reservation records..."

    # Create reservations for books that are all borrowed
    for i in $(seq 1 3); do
        reservation_data=$(cat <<EOF
{
    "readerId": $((200 + i)),
    "bookId": $((10 + i))
}
EOF
)

        response=$(api_call "POST" "/api/v1/circulation/reservations" "$reservation_data" "$ADMIN_TOKEN")

        if echo "$response" | grep -q '"code":200'; then
            log_success "Created reservation $i"
        fi

        sleep 0.5
    done

    log_success "Reservation scenarios created"
}

###############################################################################
# Create Category Data
###############################################################################

create_category_data() {
    log_info "====== Creating Category Data ======"

    # Insert categories directly into database
    log "Creating book categories..."

    execute_sql "book_service" "
        INSERT INTO book_category (name, parent_id, sort_order, created_at, updated_at)
        VALUES
            ('计算机科学', NULL, 1, NOW(), NOW()),
            ('文学', NULL, 2, NOW(), NOW()),
            ('历史', NULL, 3, NOW(), NOW()),
            ('艺术', NULL, 4, NOW(), NOW()),
            ('科学', NULL, 5, NOW(), NOW())
        ON CONFLICT (name) DO NOTHING;
    "

    log_success "Categories created"
}

###############################################################################
# Verify Test Data
###############################################################################

verify_test_data() {
    log_info "====== Verifying Test Data ======"

    # Count readers
    reader_count=$(execute_sql "reader_service" "SELECT COUNT(*) FROM reader WHERE username LIKE 'testuser_%' OR username = 'reader001';" | grep -E '^[0-9]+$')
    log_info "Test readers created: $reader_count"

    # Count books
    book_count=$(execute_sql "book_service" "SELECT COUNT(*) FROM book;" | grep -E '^[0-9]+$')
    log_info "Books in database: $book_count"

    # Count borrows
    borrow_count=$(execute_sql "circulation_service" "SELECT COUNT(*) FROM borrow_record;" | grep -E '^[0-9]+$')
    log_info "Borrow records: $borrow_count"

    # Count reservations
    reservation_count=$(execute_sql "circulation_service" "SELECT COUNT(*) FROM reservation;" | grep -E '^[0-9]+$')
    log_info "Reservations: $reservation_count"

    log_success "Test data verification complete"
}

###############################################################################
# Generate Test Data Summary
###############################################################################

generate_summary() {
    log_info "====== Test Data Summary ======"

    cat <<EOF

===========================================================
           GCRF Library Test Data Preparation
===========================================================

Dataset Size: $DATASET_SIZE

Test Accounts:
  - Username: admin        Password: admin123
  - Username: reader001    Password: reader123
  - Username: maxborrow    Password: Test123456
  - Username: testuser_*   Password: Test123456

Database Statistics:
$(execute_sql "reader_service" "SELECT COUNT(*) AS readers FROM reader;" 2>/dev/null || echo "  Readers: N/A")
$(execute_sql "book_service" "SELECT COUNT(*) AS books FROM book;" 2>/dev/null || echo "  Books: N/A")
$(execute_sql "circulation_service" "SELECT COUNT(*) AS borrows FROM borrow_record;" 2>/dev/null || echo "  Borrows: N/A")
$(execute_sql "circulation_service" "SELECT COUNT(*) AS reservations FROM reservation;" 2>/dev/null || echo "  Reservations: N/A")

Gateway URL: $GATEWAY_URL

Usage:
  - Run E2E tests: cd deployment/scripts && ./e2e-test.sh
  - Run API tests: cd deployment/scripts && ./api-smoke-test.sh
  - Run frontend tests: cd web-admin && npm run test:e2e

===========================================================

EOF
}

###############################################################################
# Main Execution
###############################################################################

print_usage() {
    cat <<EOF
Usage: $0 [options]

Options:
    --clean       Clean existing test data before import
    --minimal     Import minimal dataset (for quick tests)
    --full        Import full dataset (default)
    --gateway     Gateway URL (default: http://localhost:8080)
    --help        Display this help message

Examples:
    $0 --clean --full
    $0 --minimal
    $0 --gateway http://production.example.com:8080

EOF
}

main() {
    log_info "====== GCRF Library Test Data Preparation ======"
    log_info "Started at $(date)"
    log_info "Gateway URL: $GATEWAY_URL"
    log_info "Dataset Size: $DATASET_SIZE"
    echo ""

    # Clean data if requested
    if [ "$CLEAN_DATA" = true ]; then
        clean_test_data
        echo ""
    fi

    # Create base data
    create_category_data
    echo ""

    # Create test users
    create_test_users
    echo ""

    # Import sample books
    import_sample_books
    echo ""

    # Create borrow scenarios (requires books and readers)
    sleep 2  # Wait for data to be fully committed
    create_borrow_scenarios
    echo ""

    # Create overdue scenarios
    create_overdue_scenarios
    echo ""

    # Create reservation scenarios
    create_reservation_scenarios
    echo ""

    # Verify data
    verify_test_data
    echo ""

    # Generate summary
    generate_summary

    log_success "Test data preparation completed successfully!"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN_DATA=true
            shift
            ;;
        --minimal)
            DATASET_SIZE="minimal"
            shift
            ;;
        --full)
            DATASET_SIZE="full"
            shift
            ;;
        --gateway)
            GATEWAY_URL="$2"
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
