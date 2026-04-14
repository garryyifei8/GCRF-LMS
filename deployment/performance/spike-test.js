/**
 * GCRF Library Management System - Spike Test
 *
 * Spike test to verify system behavior under sudden load surges
 * Configuration: Sudden spike to 1000 concurrent users
 *
 * Usage:
 *   k6 run spike-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 spike-test.js
 *
 * Environment variables:
 *   K6_BASE_URL - Base URL of the API gateway (default: http://localhost:8080)
 *
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

import { group, sleep, check } from 'k6';
import http from 'k6/http';
import { Trend, Counter, Rate, Gauge } from 'k6/metrics';
import { config, k6Thresholds, getRandomBookId, getRandomSearchKeyword } from './config.js';
import {
    login,
    queryBooks,
    getBookDetail,
    queryReaders,
    checkHealth,
    thinkTime,
    pause
} from './helpers.js';

// Custom metrics for spike test
const spikeErrorRate = new Rate('spike_errors');
const spikeResponseTime = new Trend('spike_response_time', true);
const spikeThroughput = new Counter('spike_throughput');
const recoveryTime = new Gauge('recovery_time');
const concurrentUsers = new Gauge('concurrent_users');

// Test configuration
export const options = {
    // Spike test scenario: sudden bursts to 1000 users
    stages: [
        // Phase 1: Baseline
        { duration: '1m', target: 50 },       // Establish baseline at 50 users

        // Phase 2: First spike (moderate)
        { duration: '10s', target: 300 },     // Sudden spike to 300 users
        { duration: '2m', target: 300 },      // Hold spike
        { duration: '30s', target: 50 },      // Recovery to baseline

        // Phase 3: Recovery check
        { duration: '1m', target: 50 },       // Verify recovery at baseline

        // Phase 4: Second spike (severe)
        { duration: '10s', target: 600 },     // Sudden spike to 600 users
        { duration: '2m', target: 600 },      // Hold spike
        { duration: '30s', target: 50 },      // Recovery to baseline

        // Phase 5: Recovery check
        { duration: '1m', target: 50 },       // Verify recovery

        // Phase 6: Maximum spike
        { duration: '10s', target: 1000 },    // Sudden spike to 1000 users
        { duration: '3m', target: 1000 },     // Hold maximum spike
        { duration: '1m', target: 200 },      // Gradual recovery

        // Phase 7: Final recovery
        { duration: '1m', target: 50 },       // Return to baseline
        { duration: '30s', target: 0 }        // Ramp down
    ],

    // Performance thresholds (adjusted for spike testing)
    thresholds: {
        'http_req_duration': [
            'p(50)<1000',   // Under spike, 50% under 1s
            'p(90)<3000',   // 90% under 3s
            'p(95)<5000',   // 95% under 5s
            'p(99)<10000'   // 99% under 10s
        ],
        'http_req_failed': ['rate<0.20'],     // Accept up to 20% errors during spike
        'spike_errors': ['rate<0.25'],         // Track spike-specific errors
        'spike_response_time': ['p(95)<5000']  // Response time during spike
    },

    // Tags for result organization
    tags: {
        testType: 'spike',
        environment: __ENV.K6_ENVIRONMENT || 'dev'
    },

    // Summary configuration
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],

    // Connection settings for spike testing
    noConnectionReuse: false,
    userAgent: 'GCRF-SpikeTest/1.0',

    // Increased batch for spike handling
    batch: 50,
    batchPerHost: 20
};

// Global state for tracking spike phases
let currentPhase = 'baseline';
let phaseStartTime = null;
let spikeStartTimes = [];

// Setup function
export function setup() {
    console.log('========================================');
    console.log('GCRF Library Management System - Spike Test');
    console.log('========================================');
    console.log(`Base URL: ${config.baseUrl}`);
    console.log('Scenario: Sudden spikes to 1000 concurrent users');
    console.log('Duration: ~14 minutes');
    console.log('----------------------------------------');
    console.log('Spike Pattern:');
    console.log('  1. Baseline: 50 VUs');
    console.log('  2. First spike: 50 -> 300 VUs (instant)');
    console.log('  3. Second spike: 50 -> 600 VUs (instant)');
    console.log('  4. Maximum spike: 50 -> 1000 VUs (instant)');
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
        baselineMetrics: null,
        spikeEvents: []
    };
}

// Main test function
export default function (data) {
    // Track concurrent users
    concurrentUsers.add(__VU);

    // Determine current phase and workload
    const vus = __VU;
    const phase = getCurrentPhase(vus);

    // Execute appropriate workload
    if (phase === 'baseline' || phase === 'recovery') {
        baselineWorkload();
    } else if (phase === 'spike_moderate') {
        moderateSpikeWorkload();
    } else if (phase === 'spike_severe') {
        severeSpikeWorkload();
    } else if (phase === 'spike_maximum') {
        maximumSpikeWorkload();
    } else {
        baselineWorkload();
    }
}

/**
 * Determine current test phase
 */
function getCurrentPhase(vus) {
    if (vus <= 100) return 'baseline';
    if (vus <= 300) return 'spike_moderate';
    if (vus <= 600) return 'spike_severe';
    if (vus <= 1000) return 'spike_maximum';
    return 'recovery';
}

/**
 * Baseline workload - normal operations for comparison
 */
function baselineWorkload() {
    group('Baseline Operations', function () {
        // Normal browse operation
        const startTime = Date.now();
        const response = queryBooks({ page: 1, size: 20 });
        spikeResponseTime.add(Date.now() - startTime);
        spikeThroughput.add(1);

        const success = check(response, {
            'baseline: status is 200': (r) => r.status === 200
        });

        if (!success) {
            spikeErrorRate.add(1);
        }

        thinkTime(2, 4);

        // Book detail view
        const bookId = getRandomBookId();
        const detailStart = Date.now();
        const detailResponse = getBookDetail(bookId);
        spikeResponseTime.add(Date.now() - detailStart);
        spikeThroughput.add(1);

        thinkTime(1, 3);

        // Search operation
        const keyword = getRandomSearchKeyword();
        const searchStart = Date.now();
        const searchResponse = queryBooks({ page: 1, size: 10, keyword: keyword });
        spikeResponseTime.add(Date.now() - searchStart);
        spikeThroughput.add(1);
    });

    thinkTime(1, 2);
}

/**
 * Moderate spike workload (300 VUs)
 */
function moderateSpikeWorkload() {
    group('Moderate Spike Operations', function () {
        // Authenticate
        const token = login(config.testUsers.reader);
        spikeThroughput.add(1);

        if (!token) {
            spikeErrorRate.add(1);
        }

        pause(0.3);

        // Rapid read operations
        for (let i = 0; i < 2; i++) {
            const startTime = Date.now();
            const response = queryBooks({ page: i + 1, size: 20 }, token);
            spikeResponseTime.add(Date.now() - startTime);
            spikeThroughput.add(1);

            if (response.status !== 200) {
                spikeErrorRate.add(1);
            }

            pause(0.2);
        }

        thinkTime(0.5, 1);

        // Detail views
        const bookId = getRandomBookId();
        const startTime = Date.now();
        const detailResponse = getBookDetail(bookId, token);
        spikeResponseTime.add(Date.now() - startTime);
        spikeThroughput.add(1);

        if (detailResponse.status >= 500) {
            spikeErrorRate.add(1);
        }
    });

    thinkTime(0.5, 1);
}

/**
 * Severe spike workload (600 VUs)
 */
function severeSpikeWorkload() {
    group('Severe Spike Operations', function () {
        // Quick auth attempt
        const token = login(config.testUsers.reader);
        spikeThroughput.add(1);

        if (!token) {
            spikeErrorRate.add(1);
        }

        // Rapid fire requests with minimal delay
        const operations = [
            () => queryBooks({ page: Math.floor(Math.random() * 5) + 1, size: 10 }, token),
            () => getBookDetail(getRandomBookId(), token),
            () => queryBooks({ page: 1, size: 10, keyword: getRandomSearchKeyword() }, token)
        ];

        for (const op of operations) {
            const startTime = Date.now();
            try {
                const response = op();
                spikeResponseTime.add(Date.now() - startTime);
                spikeThroughput.add(1);

                if (response && response.status >= 500) {
                    spikeErrorRate.add(1);
                }
            } catch (e) {
                spikeErrorRate.add(1);
            }
            pause(0.1);
        }
    });

    thinkTime(0.3, 0.5);
}

/**
 * Maximum spike workload (1000 VUs)
 */
function maximumSpikeWorkload() {
    group('Maximum Spike Operations', function () {
        // Aggressive request pattern
        const startTime = Date.now();

        // Simple unauthenticated requests during peak (reduce auth pressure)
        const opType = Math.floor(Math.random() * 3);
        let response;

        try {
            switch (opType) {
                case 0:
                    response = queryBooks({
                        page: Math.floor(Math.random() * 10) + 1,
                        size: 10
                    });
                    break;
                case 1:
                    response = getBookDetail(getRandomBookId());
                    break;
                case 2:
                    response = queryBooks({
                        page: 1,
                        size: 10,
                        keyword: getRandomSearchKeyword()
                    });
                    break;
            }

            spikeResponseTime.add(Date.now() - startTime);
            spikeThroughput.add(1);

            const success = check(response, {
                'spike: request completed': (r) => r !== undefined,
                'spike: status is not 5xx': (r) => r && r.status < 500
            });

            if (!success) {
                spikeErrorRate.add(1);
            }
        } catch (e) {
            spikeResponseTime.add(Date.now() - startTime);
            spikeErrorRate.add(1);
        }

        // Minimal pause
        pause(0.05);

        // Second rapid request
        const secondStart = Date.now();
        try {
            const secondResponse = queryBooks({
                page: Math.floor(Math.random() * 5) + 1,
                size: 5
            });
            spikeResponseTime.add(Date.now() - secondStart);
            spikeThroughput.add(1);

            if (secondResponse && secondResponse.status >= 500) {
                spikeErrorRate.add(1);
            }
        } catch (e) {
            spikeErrorRate.add(1);
        }
    });

    // Very minimal think time during maximum spike
    thinkTime(0.1, 0.2);
}

// Teardown function
export function teardown(data) {
    console.log('========================================');
    console.log('Spike Test Completed');
    console.log('========================================');
    console.log(`Started: ${data.startTime}`);
    console.log(`Ended: ${new Date().toISOString()}`);
    console.log('----------------------------------------');

    // Final health check - verify system recovered
    const healthResponse = checkHealth();
    if (healthResponse.status !== 200) {
        console.error('CRITICAL: System did not recover after spike!');
        console.error(`Final health status: ${healthResponse.status}`);
    } else {
        console.log('System Recovery: HEALTHY');
        console.log('System successfully recovered from spike load.');
    }
}

// Custom summary handler
export function handleSummary(data) {
    const summary = {
        testType: 'spike',
        timestamp: new Date().toISOString(),
        config: {
            maxVUs: 1000,
            duration: '~14m',
            baseUrl: config.baseUrl
        },
        spikeEvents: [
            { name: 'moderate', targetVUs: 300, rampTime: '10s' },
            { name: 'severe', targetVUs: 600, rampTime: '10s' },
            { name: 'maximum', targetVUs: 1000, rampTime: '10s' }
        ],
        metrics: {
            http_reqs: data.metrics.http_reqs,
            http_req_duration: data.metrics.http_req_duration,
            http_req_failed: data.metrics.http_req_failed,
            spike_errors: data.metrics.spike_errors,
            spike_response_time: data.metrics.spike_response_time,
            spike_throughput: data.metrics.spike_throughput,
            vus_max: data.metrics.vus_max
        },
        analysis: {
            peakResponseTime: data.metrics.spike_response_time ?
                data.metrics.spike_response_time.values.max : null,
            totalRequests: data.metrics.spike_throughput ?
                data.metrics.spike_throughput.values.count : null
        }
    };

    return {
        'stdout': textSummary(data),
        'results/spike-test-summary.json': JSON.stringify(summary, null, 2)
    };
}

function textSummary(data) {
    let output = '\n';
    output += '==========================================\n';
    output += '         SPIKE TEST RESULTS SUMMARY       \n';
    output += '==========================================\n\n';

    output += 'Spike Events:\n';
    output += '  - Moderate: 50 -> 300 VUs (instant)\n';
    output += '  - Severe: 50 -> 600 VUs (instant)\n';
    output += '  - Maximum: 50 -> 1000 VUs (instant)\n\n';

    if (data.metrics.http_reqs) {
        output += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
        output += `Peak Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }

    if (data.metrics.spike_response_time) {
        output += 'Response Time During Spikes (ms):\n';
        output += `  Average: ${data.metrics.spike_response_time.values.avg.toFixed(2)}\n`;
        output += `  P50: ${data.metrics.spike_response_time.values['p(50)'].toFixed(2)}\n`;
        output += `  P90: ${data.metrics.spike_response_time.values['p(90)'].toFixed(2)}\n`;
        output += `  P95: ${data.metrics.spike_response_time.values['p(95)'].toFixed(2)}\n`;
        output += `  P99: ${data.metrics.spike_response_time.values['p(99)'].toFixed(2)}\n`;
        output += `  Max: ${data.metrics.spike_response_time.values.max.toFixed(2)}\n\n`;
    }

    if (data.metrics.http_req_failed) {
        const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
        output += `Overall Error Rate: ${failRate}%\n`;
    }

    if (data.metrics.spike_errors) {
        const spikeFailRate = (data.metrics.spike_errors.values.rate * 100).toFixed(2);
        output += `Spike Error Rate: ${spikeFailRate}%\n`;
    }

    output += '\n==========================================\n';
    output += 'Spike Resilience Analysis:\n';
    output += '==========================================\n';
    output += 'Key Observations:\n';
    output += '- Review error rates at each spike level\n';
    output += '- Check response time degradation during spikes\n';
    output += '- Verify system recovery after each spike\n';
    output += '==========================================\n';

    return output;
}
