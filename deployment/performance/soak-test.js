/**
 * GCRF Library Management System - Soak Test (Endurance Test)
 *
 * Soak test to verify system stability over extended period
 * Configuration: 50 concurrent users for 1 hour
 *
 * Purpose:
 * - Detect memory leaks
 * - Identify resource exhaustion
 * - Verify long-term stability
 * - Check for performance degradation over time
 *
 * Usage:
 *   k6 run soak-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 soak-test.js
 *
 * Environment variables:
 *   K6_BASE_URL - Base URL of the API gateway (default: http://localhost:8080)
 *   K6_SOAK_DURATION - Duration in minutes (default: 60)
 *
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

import { group, sleep, check } from 'k6';
import http from 'k6/http';
import { Trend, Counter, Rate, Gauge } from 'k6/metrics';
import { config, k6Thresholds, getRandomBookId, getRandomSearchKeyword, getRandomReaderId } from './config.js';
import {
    login,
    queryBooks,
    getBookDetail,
    borrowBook,
    returnBook,
    queryReaders,
    checkHealth,
    thinkTime,
    pause
} from './helpers.js';

// Custom metrics for soak test
const soakErrorRate = new Rate('soak_errors');
const soakResponseTime = new Trend('soak_response_time', true);
const soakThroughput = new Counter('soak_throughput');
const memoryUsageEstimate = new Gauge('memory_usage_estimate');
const activeConnections = new Gauge('active_connections');

// Time-based metrics (for tracking degradation)
const responseTimeEarly = new Trend('response_time_early', true);     // First 15 minutes
const responseTimeMid = new Trend('response_time_mid', true);         // 15-45 minutes
const responseTimeLate = new Trend('response_time_late', true);       // Last 15 minutes

// Configuration
const SOAK_DURATION = (__ENV.K6_SOAK_DURATION || 60) + 'm';
const TARGET_VUS = 50;

// Test configuration
export const options = {
    // Soak test scenario: steady load for extended duration
    stages: [
        // Phase 1: Warm-up
        { duration: '2m', target: 25 },       // Gradual ramp up
        { duration: '2m', target: TARGET_VUS }, // Reach target

        // Phase 2: Sustained load (main soak period)
        { duration: SOAK_DURATION, target: TARGET_VUS },  // Hold for 1 hour

        // Phase 3: Cool down
        { duration: '2m', target: 25 },       // Gradual ramp down
        { duration: '1m', target: 0 }         // Complete ramp down
    ],

    // Performance thresholds (strict for soak testing)
    thresholds: {
        'http_req_duration': [
            'p(50)<300',    // 50% under 300ms
            'p(90)<500',    // 90% under 500ms
            'p(95)<800',    // 95% under 800ms
            'p(99)<1500'    // 99% under 1.5s
        ],
        'http_req_failed': ['rate<0.01'],      // Less than 1% errors
        'soak_errors': ['rate<0.02'],           // Less than 2% soak errors

        // Performance degradation checks
        'response_time_early': ['p(95)<500'],   // Early performance
        'response_time_mid': ['p(95)<600'],     // Mid-test (allow 20% degradation)
        'response_time_late': ['p(95)<700'],    // Late test (allow 40% degradation)

        // Custom metrics
        'soak_response_time': ['p(95)<600']
    },

    // Tags for result organization
    tags: {
        testType: 'soak',
        environment: __ENV.K6_ENVIRONMENT || 'dev'
    },

    // Summary configuration
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],

    // Connection settings for soak testing
    noConnectionReuse: false,
    userAgent: 'GCRF-SoakTest/1.0',

    // Keep connections alive for soak testing
    httpDebug: 'none'
};

// Global timing for phase tracking
let testStartTime = null;
const EARLY_PHASE_MINUTES = 15;
const MID_PHASE_END_MINUTES = 45;

// Setup function
export function setup() {
    console.log('========================================');
    console.log('GCRF Library Management System - Soak Test');
    console.log('========================================');
    console.log(`Base URL: ${config.baseUrl}`);
    console.log(`Target VUs: ${TARGET_VUS}`);
    console.log(`Soak Duration: ${SOAK_DURATION}`);
    console.log('----------------------------------------');
    console.log('Monitoring for:');
    console.log('  - Memory leaks');
    console.log('  - Connection pool exhaustion');
    console.log('  - Performance degradation over time');
    console.log('  - Resource utilization trends');
    console.log('----------------------------------------');

    // Initial health check with baseline metrics
    const healthResponse = checkHealth();
    if (healthResponse.status !== 200) {
        console.error('WARNING: Initial health check failed!');
    } else {
        console.log('Initial health check: PASSED');
    }

    // Record start time
    testStartTime = Date.now();

    return {
        startTime: new Date().toISOString(),
        startTimestamp: testStartTime,
        initialHealth: healthResponse.status === 200
    };
}

// Main test function
export default function (data) {
    // Track active connections
    activeConnections.add(__VU);

    // Determine phase for time-based metrics
    const elapsedMinutes = (Date.now() - data.startTimestamp) / 60000;
    const phase = getPhase(elapsedMinutes);

    // Execute realistic user session
    const sessionType = selectSessionType();

    switch (sessionType) {
        case 'browse':
            browseSession(phase);
            break;
        case 'search':
            searchSession(phase);
            break;
        case 'circulation':
            circulationSession(phase);
            break;
        case 'management':
            managementSession(phase);
            break;
        default:
            browseSession(phase);
    }

    // Periodic health check (every ~100 iterations per VU)
    if (Math.random() < 0.01) {
        performHealthCheck();
    }
}

/**
 * Determine test phase based on elapsed time
 */
function getPhase(elapsedMinutes) {
    if (elapsedMinutes < EARLY_PHASE_MINUTES) return 'early';
    if (elapsedMinutes < MID_PHASE_END_MINUTES) return 'mid';
    return 'late';
}

/**
 * Select session type based on realistic distribution
 */
function selectSessionType() {
    const rand = Math.random();
    if (rand < 0.40) return 'browse';      // 40% browsing
    if (rand < 0.70) return 'search';      // 30% searching
    if (rand < 0.90) return 'circulation'; // 20% circulation
    return 'management';                    // 10% management
}

/**
 * Record response time to phase-specific metric
 */
function recordResponseTime(duration, phase) {
    soakResponseTime.add(duration);

    switch (phase) {
        case 'early':
            responseTimeEarly.add(duration);
            break;
        case 'mid':
            responseTimeMid.add(duration);
            break;
        case 'late':
            responseTimeLate.add(duration);
            break;
    }
}

/**
 * Browse session - simulates casual browsing
 */
function browseSession(phase) {
    group('Browse Session', function () {
        // Browse books list
        const startTime = Date.now();
        const response = queryBooks({ page: 1, size: 20 });
        const duration = Date.now() - startTime;

        recordResponseTime(duration, phase);
        soakThroughput.add(1);

        const success = check(response, {
            'browse: status is 200': (r) => r.status === 200
        });

        if (!success) {
            soakErrorRate.add(1);
        }

        thinkTime(2, 5);

        // View a few book details
        for (let i = 0; i < 2; i++) {
            const bookId = getRandomBookId();
            const detailStart = Date.now();
            const detailResponse = getBookDetail(bookId);
            recordResponseTime(Date.now() - detailStart, phase);
            soakThroughput.add(1);

            if (detailResponse.status !== 200) {
                soakErrorRate.add(1);
            }

            thinkTime(3, 7);
        }

        // Navigate pages
        const page = Math.floor(Math.random() * 5) + 2;
        const navStart = Date.now();
        const navResponse = queryBooks({ page: page, size: 20 });
        recordResponseTime(Date.now() - navStart, phase);
        soakThroughput.add(1);

        if (navResponse.status !== 200) {
            soakErrorRate.add(1);
        }
    });

    thinkTime(5, 10);
}

/**
 * Search session - simulates search activity
 */
function searchSession(phase) {
    group('Search Session', function () {
        // Perform search
        const keyword = getRandomSearchKeyword();
        const searchStart = Date.now();
        const searchResponse = queryBooks({ page: 1, size: 10, keyword: keyword });
        recordResponseTime(Date.now() - searchStart, phase);
        soakThroughput.add(1);

        const success = check(searchResponse, {
            'search: status is 200': (r) => r.status === 200
        });

        if (!success) {
            soakErrorRate.add(1);
        }

        thinkTime(2, 4);

        // View search results
        for (let i = 0; i < 2; i++) {
            const bookId = getRandomBookId();
            const detailStart = Date.now();
            const detailResponse = getBookDetail(bookId);
            recordResponseTime(Date.now() - detailStart, phase);
            soakThroughput.add(1);

            if (detailResponse.status !== 200) {
                soakErrorRate.add(1);
            }

            thinkTime(2, 5);
        }

        // Another search
        const secondKeyword = getRandomSearchKeyword();
        const secondSearchStart = Date.now();
        const secondSearchResponse = queryBooks({ page: 1, size: 10, keyword: secondKeyword });
        recordResponseTime(Date.now() - secondSearchStart, phase);
        soakThroughput.add(1);

        if (secondSearchResponse.status !== 200) {
            soakErrorRate.add(1);
        }
    });

    thinkTime(5, 10);
}

/**
 * Circulation session - simulates borrow/return operations
 */
function circulationSession(phase) {
    group('Circulation Session', function () {
        // Authenticate
        const loginStart = Date.now();
        const token = login(config.testUsers.librarian);
        recordResponseTime(Date.now() - loginStart, phase);
        soakThroughput.add(1);

        if (!token) {
            soakErrorRate.add(1);
            return;
        }

        thinkTime(1, 2);

        // Search for book
        const searchStart = Date.now();
        const searchResponse = queryBooks({ page: 1, size: 10, status: 'AVAILABLE' }, token);
        recordResponseTime(Date.now() - searchStart, phase);
        soakThroughput.add(1);

        if (searchResponse.status !== 200) {
            soakErrorRate.add(1);
        }

        thinkTime(2, 4);

        // Simulate borrow operation
        const bookId = getRandomBookId();
        const readerId = getRandomReaderId();
        const borrowStart = Date.now();
        const borrowResponse = borrowBook(bookId, readerId, token);
        recordResponseTime(Date.now() - borrowStart, phase);
        soakThroughput.add(1);

        // Allow business errors (book not available, etc.)
        if (borrowResponse.status >= 500) {
            soakErrorRate.add(1);
        }

        thinkTime(3, 6);

        // Simulate return operation
        const recordId = Math.floor(Math.random() * 100) + 1;
        const returnStart = Date.now();
        const returnResponse = returnBook(recordId, token);
        recordResponseTime(Date.now() - returnStart, phase);
        soakThroughput.add(1);

        if (returnResponse.status >= 500) {
            soakErrorRate.add(1);
        }
    });

    thinkTime(5, 10);
}

/**
 * Management session - simulates admin operations
 */
function managementSession(phase) {
    group('Management Session', function () {
        // Authenticate as admin
        const loginStart = Date.now();
        const token = login(config.testUsers.admin);
        recordResponseTime(Date.now() - loginStart, phase);
        soakThroughput.add(1);

        if (!token) {
            soakErrorRate.add(1);
            return;
        }

        thinkTime(1, 2);

        // Query readers
        const readersStart = Date.now();
        const readersResponse = queryReaders({ page: 1, size: 20 }, token);
        recordResponseTime(Date.now() - readersStart, phase);
        soakThroughput.add(1);

        if (readersResponse.status !== 200) {
            soakErrorRate.add(1);
        }

        thinkTime(2, 5);

        // Browse books
        const booksStart = Date.now();
        const booksResponse = queryBooks({ page: 1, size: 20 }, token);
        recordResponseTime(Date.now() - booksStart, phase);
        soakThroughput.add(1);

        if (booksResponse.status !== 200) {
            soakErrorRate.add(1);
        }

        thinkTime(2, 4);

        // Search readers
        const searchStart = Date.now();
        const searchResponse = queryReaders({ page: 1, size: 10, keyword: 'test' }, token);
        recordResponseTime(Date.now() - searchStart, phase);
        soakThroughput.add(1);

        if (searchResponse.status !== 200) {
            soakErrorRate.add(1);
        }
    });

    thinkTime(5, 15);
}

/**
 * Periodic health check
 */
function performHealthCheck() {
    group('Health Check', function () {
        const response = checkHealth();
        soakThroughput.add(1);

        const success = check(response, {
            'health check: status is 200': (r) => r.status === 200
        });

        if (!success) {
            console.warn(`Health check failed at ${new Date().toISOString()}`);
            soakErrorRate.add(1);
        }
    });
}

// Teardown function
export function teardown(data) {
    console.log('========================================');
    console.log('Soak Test Completed');
    console.log('========================================');
    console.log(`Started: ${data.startTime}`);
    console.log(`Ended: ${new Date().toISOString()}`);

    const durationMinutes = (Date.now() - data.startTimestamp) / 60000;
    console.log(`Duration: ${durationMinutes.toFixed(2)} minutes`);
    console.log('----------------------------------------');

    // Final health check
    const finalHealth = checkHealth();
    if (finalHealth.status !== 200) {
        console.error('CRITICAL: System unhealthy after soak test!');
    } else {
        console.log('Final Health Status: HEALTHY');
    }

    console.log('----------------------------------------');
    console.log('Review time-based metrics to detect:');
    console.log('  - Memory leaks (increasing response times)');
    console.log('  - Connection exhaustion (rising error rates)');
    console.log('  - Performance degradation patterns');
}

// Custom summary handler
export function handleSummary(data) {
    // Calculate degradation metrics
    let degradationAnalysis = {};

    if (data.metrics.response_time_early && data.metrics.response_time_late) {
        const earlyP95 = data.metrics.response_time_early.values['p(95)'];
        const lateP95 = data.metrics.response_time_late.values['p(95)'];
        const degradation = ((lateP95 - earlyP95) / earlyP95 * 100).toFixed(2);

        degradationAnalysis = {
            earlyP95: earlyP95,
            midP95: data.metrics.response_time_mid ?
                data.metrics.response_time_mid.values['p(95)'] : null,
            lateP95: lateP95,
            degradationPercent: parseFloat(degradation),
            hasDegradation: parseFloat(degradation) > 50  // Flag if > 50% degradation
        };
    }

    const summary = {
        testType: 'soak',
        timestamp: new Date().toISOString(),
        config: {
            targetVUs: TARGET_VUS,
            duration: SOAK_DURATION,
            baseUrl: config.baseUrl
        },
        metrics: {
            http_reqs: data.metrics.http_reqs,
            http_req_duration: data.metrics.http_req_duration,
            http_req_failed: data.metrics.http_req_failed,
            soak_errors: data.metrics.soak_errors,
            soak_response_time: data.metrics.soak_response_time,
            soak_throughput: data.metrics.soak_throughput,
            response_time_early: data.metrics.response_time_early,
            response_time_mid: data.metrics.response_time_mid,
            response_time_late: data.metrics.response_time_late
        },
        analysis: {
            degradation: degradationAnalysis,
            totalRequests: data.metrics.soak_throughput ?
                data.metrics.soak_throughput.values.count : null,
            errorRate: data.metrics.soak_errors ?
                data.metrics.soak_errors.values.rate : null
        }
    };

    return {
        'stdout': textSummary(data, degradationAnalysis),
        'results/soak-test-summary.json': JSON.stringify(summary, null, 2)
    };
}

function textSummary(data, degradationAnalysis) {
    let output = '\n';
    output += '==========================================\n';
    output += '         SOAK TEST RESULTS SUMMARY        \n';
    output += '==========================================\n\n';

    output += `Duration: ${SOAK_DURATION}\n`;
    output += `Target VUs: ${TARGET_VUS}\n\n`;

    if (data.metrics.http_reqs) {
        output += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
        output += `Average Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }

    if (data.metrics.soak_response_time) {
        output += 'Overall Response Time (ms):\n';
        output += `  Average: ${data.metrics.soak_response_time.values.avg.toFixed(2)}\n`;
        output += `  P50: ${data.metrics.soak_response_time.values['p(50)'].toFixed(2)}\n`;
        output += `  P95: ${data.metrics.soak_response_time.values['p(95)'].toFixed(2)}\n`;
        output += `  P99: ${data.metrics.soak_response_time.values['p(99)'].toFixed(2)}\n\n`;
    }

    output += '==========================================\n';
    output += 'Performance Degradation Analysis\n';
    output += '==========================================\n';

    if (degradationAnalysis && degradationAnalysis.earlyP95) {
        output += `Early Phase P95: ${degradationAnalysis.earlyP95.toFixed(2)}ms\n`;
        if (degradationAnalysis.midP95) {
            output += `Mid Phase P95: ${degradationAnalysis.midP95.toFixed(2)}ms\n`;
        }
        output += `Late Phase P95: ${degradationAnalysis.lateP95.toFixed(2)}ms\n`;
        output += `Degradation: ${degradationAnalysis.degradationPercent}%\n\n`;

        if (degradationAnalysis.hasDegradation) {
            output += '*** WARNING: Significant performance degradation detected! ***\n';
            output += 'This may indicate memory leaks or resource exhaustion.\n\n';
        } else {
            output += 'Performance remained stable throughout the test.\n\n';
        }
    }

    if (data.metrics.soak_errors) {
        const errorRate = (data.metrics.soak_errors.values.rate * 100).toFixed(4);
        output += `Error Rate: ${errorRate}%\n`;

        if (data.metrics.soak_errors.values.rate > 0.01) {
            output += '*** WARNING: Error rate exceeds 1% threshold ***\n';
        }
    }

    output += '\n==========================================\n';
    output += 'Recommendations:\n';
    output += '==========================================\n';
    output += '- Monitor JVM heap usage and GC patterns\n';
    output += '- Check database connection pool utilization\n';
    output += '- Review Redis connection counts\n';
    output += '- Analyze Prometheus metrics for trends\n';
    output += '==========================================\n';

    return output;
}
