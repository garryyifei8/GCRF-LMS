/**
 * GCRF Library Management System - Stress Test
 *
 * Stress test to find system breaking point
 * Configuration: Gradually increase to 500 concurrent users
 *
 * Usage:
 *   k6 run stress-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 stress-test.js
 *
 * Environment variables:
 *   K6_BASE_URL - Base URL of the API gateway (default: http://localhost:8080)
 *   K6_ADMIN_USER/K6_ADMIN_PASS - Admin credentials
 *
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

import { group, sleep, check } from 'k6';
import http from 'k6/http';
import { Trend, Counter, Rate } from 'k6/metrics';
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
    pause,
    errorRate
} from './helpers.js';

// Custom metrics for stress test
const stressErrorRate = new Rate('stress_errors');
const responseTimeUnderLoad = new Trend('response_time_under_load', true);
const throughputUnderLoad = new Counter('throughput_under_load');

// Test configuration
export const options = {
    // Stress test scenario: gradually increase load to find breaking point
    stages: [
        // Phase 1: Warm-up
        { duration: '1m', target: 50 },      // Ramp up to 50 users

        // Phase 2: Moderate load
        { duration: '2m', target: 100 },     // Ramp up to 100 users
        { duration: '3m', target: 100 },     // Hold at 100 users

        // Phase 3: High load
        { duration: '2m', target: 200 },     // Ramp up to 200 users
        { duration: '3m', target: 200 },     // Hold at 200 users

        // Phase 4: Stress load
        { duration: '2m', target: 300 },     // Ramp up to 300 users
        { duration: '3m', target: 300 },     // Hold at 300 users

        // Phase 5: Peak load
        { duration: '2m', target: 400 },     // Ramp up to 400 users
        { duration: '3m', target: 400 },     // Hold at 400 users

        // Phase 6: Maximum stress
        { duration: '2m', target: 500 },     // Ramp up to 500 users
        { duration: '5m', target: 500 },     // Hold at 500 users (breaking point test)

        // Phase 7: Recovery
        { duration: '2m', target: 200 },     // Ramp down to 200 users
        { duration: '1m', target: 50 },      // Ramp down to 50 users
        { duration: '30s', target: 0 }       // Ramp down to 0
    ],

    // Performance thresholds (more lenient for stress testing)
    thresholds: {
        'http_req_duration': [
            'p(50)<500',   // 50% under 500ms
            'p(90)<2000',  // 90% under 2s
            'p(95)<3000',  // 95% under 3s
            'p(99)<5000'   // 99% under 5s
        ],
        'http_req_failed': ['rate<0.10'],     // Accept up to 10% errors under extreme stress
        'stress_errors': ['rate<0.15'],        // Track stress-specific errors
        'response_time_under_load': ['p(95)<3000']
    },

    // Tags for result organization
    tags: {
        testType: 'stress',
        environment: __ENV.K6_ENVIRONMENT || 'dev'
    },

    // Summary configuration
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],

    // Connection settings for stress testing
    noConnectionReuse: false,
    userAgent: 'GCRF-StressTest/1.0',

    // Batch settings
    batch: 20,
    batchPerHost: 10
};

// Setup function
export function setup() {
    console.log('========================================');
    console.log('GCRF Library Management System - Stress Test');
    console.log('========================================');
    console.log(`Base URL: ${config.baseUrl}`);
    console.log('Scenario: Gradual ramp up to 500 concurrent users');
    console.log('Duration: ~31 minutes');
    console.log('----------------------------------------');
    console.log('Phases:');
    console.log('  1. Warm-up: 50 VUs');
    console.log('  2. Moderate: 100 VUs');
    console.log('  3. High: 200 VUs');
    console.log('  4. Stress: 300 VUs');
    console.log('  5. Peak: 400 VUs');
    console.log('  6. Maximum: 500 VUs');
    console.log('  7. Recovery');
    console.log('----------------------------------------');

    // Initial health check
    const healthResponse = checkHealth();
    if (healthResponse.status !== 200) {
        console.error('WARNING: Initial health check failed!');
    } else {
        console.log('Initial health check: PASSED');
    }

    return {
        startTime: new Date().toISOString(),
        phases: []
    };
}

// Main test function
export default function (data) {
    // Determine current phase based on VU count
    const currentVUs = __VU;
    const phase = getPhase(currentVUs);

    // Execute appropriate workload based on phase
    switch (phase) {
        case 'warmup':
            warmupWorkload();
            break;
        case 'moderate':
            moderateWorkload();
            break;
        case 'high':
            highLoadWorkload();
            break;
        case 'stress':
            stressWorkload();
            break;
        case 'peak':
        case 'maximum':
            maximumStressWorkload();
            break;
        default:
            moderateWorkload();
    }
}

/**
 * Determine test phase based on VU count
 */
function getPhase(vus) {
    if (vus <= 50) return 'warmup';
    if (vus <= 100) return 'moderate';
    if (vus <= 200) return 'high';
    if (vus <= 300) return 'stress';
    if (vus <= 400) return 'peak';
    return 'maximum';
}

/**
 * Warm-up workload - light operations
 */
function warmupWorkload() {
    group('Warmup Operations', function () {
        // Simple book browse
        const startTime = Date.now();
        const response = queryBooks({ page: 1, size: 10 });
        responseTimeUnderLoad.add(Date.now() - startTime);
        throughputUnderLoad.add(1);

        if (response.status !== 200) {
            stressErrorRate.add(1);
        }

        thinkTime(2, 4);

        // Book detail
        const bookId = getRandomBookId();
        getBookDetail(bookId);
        throughputUnderLoad.add(1);
    });

    thinkTime(1, 3);
}

/**
 * Moderate workload - normal operations
 */
function moderateWorkload() {
    group('Moderate Operations', function () {
        // Authenticate
        const token = login(config.testUsers.reader);
        throughputUnderLoad.add(1);

        if (!token) {
            stressErrorRate.add(1);
            return;
        }

        thinkTime(1, 2);

        // Book operations
        const startTime = Date.now();
        queryBooks({ page: 1, size: 20 }, token);
        responseTimeUnderLoad.add(Date.now() - startTime);
        throughputUnderLoad.add(1);

        pause(0.5);

        const bookId = getRandomBookId();
        getBookDetail(bookId, token);
        throughputUnderLoad.add(1);

        thinkTime(1, 2);

        // Search
        const keyword = getRandomSearchKeyword();
        queryBooks({ page: 1, size: 10, keyword: keyword }, token);
        throughputUnderLoad.add(1);
    });

    thinkTime(1, 2);
}

/**
 * High load workload - increased operations
 */
function highLoadWorkload() {
    group('High Load Operations', function () {
        // Quick authentication
        const token = login(config.testUsers.librarian);
        throughputUnderLoad.add(1);

        if (!token) {
            stressErrorRate.add(1);
            return;
        }

        pause(0.5);

        // Rapid book operations
        for (let i = 0; i < 3; i++) {
            const startTime = Date.now();
            const response = queryBooks({ page: i + 1, size: 10 }, token);
            responseTimeUnderLoad.add(Date.now() - startTime);
            throughputUnderLoad.add(1);

            if (response.status !== 200) {
                stressErrorRate.add(1);
            }

            pause(0.3);
        }

        thinkTime(0.5, 1);

        // Multiple detail views
        for (let i = 0; i < 2; i++) {
            const bookId = getRandomBookId();
            getBookDetail(bookId, token);
            throughputUnderLoad.add(1);
            pause(0.2);
        }

        thinkTime(0.5, 1);

        // Reader query
        queryReaders({ page: 1, size: 10 }, token);
        throughputUnderLoad.add(1);
    });

    thinkTime(0.5, 1);
}

/**
 * Stress workload - intensive operations
 */
function stressWorkload() {
    group('Stress Operations', function () {
        // Rapid fire requests
        const token = login(config.testUsers.librarian);
        throughputUnderLoad.add(1);

        if (!token) {
            stressErrorRate.add(1);
            // Continue with unauthenticated requests
        }

        pause(0.2);

        // Batch of read operations
        const operations = [
            () => queryBooks({ page: Math.floor(Math.random() * 5) + 1, size: 20 }, token),
            () => getBookDetail(getRandomBookId(), token),
            () => queryReaders({ page: 1, size: 10 }, token),
            () => queryBooks({ page: 1, size: 10, keyword: getRandomSearchKeyword() }, token)
        ];

        for (const op of operations) {
            const startTime = Date.now();
            try {
                const response = op();
                responseTimeUnderLoad.add(Date.now() - startTime);
                throughputUnderLoad.add(1);

                if (response && response.status >= 500) {
                    stressErrorRate.add(1);
                }
            } catch (e) {
                stressErrorRate.add(1);
            }
            pause(0.1);
        }

        // Circulation operations (with retries)
        if (token) {
            const bookId = getRandomBookId();
            const readerId = Math.floor(Math.random() * 10) + 1;
            const startTime = Date.now();
            const borrowResponse = borrowBook(bookId, readerId, token);
            responseTimeUnderLoad.add(Date.now() - startTime);
            throughputUnderLoad.add(1);

            if (borrowResponse.status >= 500) {
                stressErrorRate.add(1);
            }
        }
    });

    thinkTime(0.2, 0.5);
}

/**
 * Maximum stress workload - breaking point test
 */
function maximumStressWorkload() {
    group('Maximum Stress Operations', function () {
        // No think time, rapid fire
        const token = login(config.testUsers.reader);
        throughputUnderLoad.add(1);

        // Burst of requests without waiting
        const burstSize = 5;
        for (let i = 0; i < burstSize; i++) {
            const startTime = Date.now();
            try {
                // Random operation selection
                const opType = Math.floor(Math.random() * 4);
                let response;

                switch (opType) {
                    case 0:
                        response = queryBooks({ page: Math.floor(Math.random() * 10) + 1, size: 20 }, token);
                        break;
                    case 1:
                        response = getBookDetail(getRandomBookId(), token);
                        break;
                    case 2:
                        response = queryBooks({ page: 1, size: 10, keyword: getRandomSearchKeyword() }, token);
                        break;
                    case 3:
                        response = queryReaders({ page: 1, size: 10 }, token);
                        break;
                }

                responseTimeUnderLoad.add(Date.now() - startTime);
                throughputUnderLoad.add(1);

                if (response && response.status >= 500) {
                    stressErrorRate.add(1);
                }
            } catch (e) {
                stressErrorRate.add(1);
            }

            // Minimal pause
            pause(0.05);
        }
    });

    // Very short think time under maximum stress
    thinkTime(0.1, 0.3);
}

// Teardown function
export function teardown(data) {
    console.log('========================================');
    console.log('Stress Test Completed');
    console.log('========================================');
    console.log(`Started: ${data.startTime}`);
    console.log(`Ended: ${new Date().toISOString()}`);
    console.log('----------------------------------------');

    // Final health check
    const healthResponse = checkHealth();
    if (healthResponse.status !== 200) {
        console.error('WARNING: System did not recover properly!');
        console.error(`Final health status: ${healthResponse.status}`);
    } else {
        console.log('System recovery: HEALTHY');
    }
}

// Custom summary handler
export function handleSummary(data) {
    const summary = {
        testType: 'stress',
        timestamp: new Date().toISOString(),
        config: {
            maxVUs: 500,
            duration: '~31m',
            baseUrl: config.baseUrl
        },
        phases: [
            { name: 'warmup', maxVUs: 50 },
            { name: 'moderate', maxVUs: 100 },
            { name: 'high', maxVUs: 200 },
            { name: 'stress', maxVUs: 300 },
            { name: 'peak', maxVUs: 400 },
            { name: 'maximum', maxVUs: 500 }
        ],
        metrics: {
            http_reqs: data.metrics.http_reqs,
            http_req_duration: data.metrics.http_req_duration,
            http_req_failed: data.metrics.http_req_failed,
            stress_errors: data.metrics.stress_errors,
            response_time_under_load: data.metrics.response_time_under_load,
            throughput_under_load: data.metrics.throughput_under_load,
            vus_max: data.metrics.vus_max
        }
    };

    return {
        'stdout': textSummary(data),
        'results/stress-test-summary.json': JSON.stringify(summary, null, 2)
    };
}

function textSummary(data) {
    let output = '\n';
    output += '==========================================\n';
    output += '        STRESS TEST RESULTS SUMMARY       \n';
    output += '==========================================\n\n';

    if (data.metrics.http_reqs) {
        output += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
        output += `Peak Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }

    if (data.metrics.http_req_duration) {
        output += 'Response Time Under Stress (ms):\n';
        output += `  Average: ${data.metrics.http_req_duration.values.avg.toFixed(2)}\n`;
        output += `  P50: ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)}\n`;
        output += `  P90: ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)}\n`;
        output += `  P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}\n`;
        output += `  P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}\n`;
        output += `  Max: ${data.metrics.http_req_duration.values.max.toFixed(2)}\n\n`;
    }

    if (data.metrics.http_req_failed) {
        const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
        output += `Error Rate: ${failRate}%\n`;
    }

    if (data.metrics.stress_errors) {
        const stressFailRate = (data.metrics.stress_errors.values.rate * 100).toFixed(2);
        output += `Stress Error Rate: ${stressFailRate}%\n`;
    }

    output += '\n==========================================\n';
    output += 'Breaking Point Analysis:\n';
    output += '==========================================\n';
    output += 'Review response times at each VU level to\n';
    output += 'identify the system breaking point.\n';
    output += '==========================================\n';

    return output;
}
