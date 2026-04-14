/**
 * GCRF Library Management System - Load Test
 *
 * Basic load test simulating normal production traffic
 * Configuration: 100 concurrent users for 5 minutes
 *
 * Usage:
 *   k6 run load-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 load-test.js
 *
 * Environment variables:
 *   K6_BASE_URL - Base URL of the API gateway (default: http://localhost:8080)
 *   K6_ADMIN_USER/K6_ADMIN_PASS - Admin credentials
 *   K6_READER_USER/K6_READER_PASS - Reader credentials
 *
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

import { group, sleep } from 'k6';
import { config, k6Thresholds, getRandomBookId, getRandomSearchKeyword } from './config.js';
import {
    login,
    getToken,
    queryBooks,
    getBookDetail,
    borrowBook,
    returnBook,
    queryReaders,
    checkHealth,
    thinkTime,
    pause
} from './helpers.js';

// Test configuration
export const options = {
    // Load test scenario: ramp up to 100 VUs over 1 minute, hold for 5 minutes, ramp down
    stages: [
        { duration: '30s', target: 25 },    // Warm-up: ramp up to 25 users
        { duration: '30s', target: 50 },    // Ramp up to 50 users
        { duration: '1m', target: 100 },    // Ramp up to 100 users
        { duration: '5m', target: 100 },    // Stay at 100 users for 5 minutes
        { duration: '1m', target: 50 },     // Ramp down to 50 users
        { duration: '30s', target: 0 }      // Ramp down to 0
    ],

    // Performance thresholds
    thresholds: k6Thresholds,

    // Tags for result organization
    tags: {
        testType: 'load',
        environment: __ENV.K6_ENVIRONMENT || 'dev'
    },

    // Summary configuration
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

    // No graceful stop - complete all iterations
    noConnectionReuse: false,
    userAgent: 'GCRF-LoadTest/1.0'
};

// Setup function - runs once before test
export function setup() {
    console.log('========================================');
    console.log('GCRF Library Management System - Load Test');
    console.log('========================================');
    console.log(`Base URL: ${config.baseUrl}`);
    console.log('Scenario: 100 concurrent users for 5 minutes');
    console.log('----------------------------------------');

    // Verify system is healthy before starting
    const healthResponse = checkHealth();
    if (healthResponse.status !== 200) {
        console.error('WARNING: System health check failed!');
        console.error(`Status: ${healthResponse.status}`);
    } else {
        console.log('System health check: PASSED');
    }

    return {
        startTime: new Date().toISOString()
    };
}

// Main test function - executed by each VU
export default function (data) {
    // Simulate realistic user session
    const sessionScenarios = [
        browseAndSearchSession,      // 40% - Browsing and searching books
        borrowReturnSession,         // 30% - Borrowing and returning books
        readerManagementSession,     // 20% - Reader queries
        mixedOperationsSession       // 10% - Mixed operations
    ];

    // Weight-based scenario selection
    const rand = Math.random();
    if (rand < 0.4) {
        browseAndSearchSession();
    } else if (rand < 0.7) {
        borrowReturnSession();
    } else if (rand < 0.9) {
        readerManagementSession();
    } else {
        mixedOperationsSession();
    }
}

/**
 * Browse and search books session
 * Most common user behavior - browsing and searching for books
 */
function browseAndSearchSession() {
    group('Book Operations', function () {
        // Browse books (first page)
        group('Browse Books', function () {
            queryBooks({ page: 1, size: 20 });
            pause(0.5);
        });

        thinkTime(1, 2);

        // Search for books
        group('Search Books', function () {
            const keyword = getRandomSearchKeyword();
            queryBooks({ page: 1, size: 10, keyword: keyword });
            pause(0.5);
        });

        thinkTime(1, 3);

        // View book details
        group('View Book Detail', function () {
            const bookId = getRandomBookId();
            getBookDetail(bookId);
            pause(0.3);
        });

        thinkTime(1, 2);

        // Browse next page
        group('Browse Next Page', function () {
            queryBooks({ page: 2, size: 20 });
        });
    });

    thinkTime(2, 5);
}

/**
 * Borrow and return books session
 * Circulation operations - requires authentication
 */
function borrowReturnSession() {
    group('Authentication', function () {
        const token = login(config.testUsers.librarian);
        if (!token) {
            console.error('Login failed, skipping borrow/return session');
            return;
        }

        thinkTime(1, 2);

        group('Circulation', function () {
            // Search for available book
            group('Search Available Books', function () {
                queryBooks({ page: 1, size: 10, status: 'AVAILABLE' }, token);
                pause(0.5);
            });

            thinkTime(1, 2);

            // Simulate borrow operation (using test data)
            group('Borrow Book', function () {
                const bookId = getRandomBookId();
                const readerId = 1; // Use fixed reader for testing
                borrowBook(bookId, readerId, token);
                pause(0.5);
            });

            thinkTime(2, 4);

            // Simulate return operation
            group('Return Book', function () {
                // In real test, we would get the actual record ID from borrow response
                const recordId = Math.floor(Math.random() * 100) + 1;
                returnBook(recordId, token);
            });
        });
    });

    thinkTime(3, 6);
}

/**
 * Reader management session
 * Query and manage readers - requires authentication
 */
function readerManagementSession() {
    group('Authentication', function () {
        const token = login(config.testUsers.admin);
        if (!token) {
            console.error('Login failed, skipping reader management session');
            return;
        }

        thinkTime(1, 2);

        group('Reader Operations', function () {
            // List readers with pagination
            group('List Readers', function () {
                queryReaders({ page: 1, size: 20 }, token);
                pause(0.5);
            });

            thinkTime(1, 2);

            // Search readers
            group('Search Readers', function () {
                queryReaders({ page: 1, size: 10, keyword: 'test' }, token);
                pause(0.5);
            });

            thinkTime(1, 3);

            // View next page
            group('Readers Next Page', function () {
                queryReaders({ page: 2, size: 20 }, token);
            });
        });
    });

    thinkTime(2, 5);
}

/**
 * Mixed operations session
 * Various operations simulating power user behavior
 */
function mixedOperationsSession() {
    group('Authentication', function () {
        const token = login(config.testUsers.librarian);
        if (!token) {
            console.error('Login failed, skipping mixed operations session');
            return;
        }

        thinkTime(1, 2);

        // Mix of different operations
        group('Book Operations', function () {
            // Browse books
            queryBooks({ page: 1, size: 10 }, token);
            pause(0.5);

            // Quick detail view
            const bookId = getRandomBookId();
            getBookDetail(bookId, token);
            pause(0.3);
        });

        thinkTime(1, 2);

        group('Reader Operations', function () {
            // Quick reader lookup
            queryReaders({ page: 1, size: 5 }, token);
            pause(0.5);
        });

        thinkTime(1, 2);

        group('Book Operations', function () {
            // Another search
            const keyword = getRandomSearchKeyword();
            queryBooks({ page: 1, size: 10, keyword: keyword }, token);
            pause(0.5);

            // View another book
            const anotherBookId = getRandomBookId();
            getBookDetail(anotherBookId, token);
        });
    });

    thinkTime(2, 4);
}

// Teardown function - runs once after test
export function teardown(data) {
    console.log('========================================');
    console.log('Load Test Completed');
    console.log('========================================');
    console.log(`Started: ${data.startTime}`);
    console.log(`Ended: ${new Date().toISOString()}`);
    console.log('----------------------------------------');
}

// Custom summary handler
export function handleSummary(data) {
    const summary = {
        testType: 'load',
        timestamp: new Date().toISOString(),
        config: {
            maxVUs: 100,
            duration: '8m 30s',
            baseUrl: config.baseUrl
        },
        metrics: {
            http_reqs: data.metrics.http_reqs,
            http_req_duration: data.metrics.http_req_duration,
            http_req_failed: data.metrics.http_req_failed,
            vus: data.metrics.vus,
            vus_max: data.metrics.vus_max
        },
        thresholds: data.root_group ? data.root_group.checks : {}
    };

    return {
        'stdout': textSummary(data, { indent: '  ', enableColors: true }),
        'results/load-test-summary.json': JSON.stringify(summary, null, 2)
    };
}

// Text summary helper
function textSummary(data, opts) {
    let output = '\n';
    output += '==========================================\n';
    output += '         LOAD TEST RESULTS SUMMARY        \n';
    output += '==========================================\n\n';

    if (data.metrics.http_reqs) {
        output += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
        output += `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }

    if (data.metrics.http_req_duration) {
        output += 'Response Time (ms):\n';
        output += `  Average: ${data.metrics.http_req_duration.values.avg.toFixed(2)}\n`;
        output += `  P50: ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)}\n`;
        output += `  P90: ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)}\n`;
        output += `  P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}\n`;
        output += `  P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}\n`;
        output += `  Max: ${data.metrics.http_req_duration.values.max.toFixed(2)}\n\n`;
    }

    if (data.metrics.http_req_failed) {
        const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
        output += `Error Rate: ${failRate}%\n\n`;
    }

    output += '==========================================\n';
    return output;
}
