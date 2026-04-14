/**
 * GCRF Library Management System - Performance Test Configuration
 *
 * Central configuration for all k6 performance tests
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

// Environment configuration
export const config = {
    // Base URL - can be overridden via K6_BASE_URL environment variable
    baseUrl: __ENV.K6_BASE_URL || 'http://localhost:8080',

    // API Gateway path prefix
    apiPrefix: '/api/v1',

    // Authentication credentials for testing
    testUsers: {
        admin: {
            username: __ENV.K6_ADMIN_USER || 'admin',
            password: __ENV.K6_ADMIN_PASS || 'admin123'
        },
        librarian: {
            username: __ENV.K6_LIBRARIAN_USER || 'librarian',
            password: __ENV.K6_LIBRARIAN_PASS || 'librarian123'
        },
        reader: {
            username: __ENV.K6_READER_USER || 'reader',
            password: __ENV.K6_READER_PASS || 'reader123'
        }
    },

    // Performance thresholds
    thresholds: {
        // Response time thresholds (in milliseconds)
        http_req_duration: {
            p50: 200,   // 50th percentile
            p90: 400,   // 90th percentile
            p95: 500,   // 95th percentile (primary SLO)
            p99: 1000   // 99th percentile
        },

        // Error rate threshold
        http_req_failed: {
            rate: 0.01  // Maximum 1% error rate
        },

        // Throughput target
        http_reqs: {
            rate: 500   // Target: 500 TPS
        }
    },

    // Test data pools
    testData: {
        // Sample book IDs for testing (will be populated dynamically)
        bookIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],

        // Sample reader IDs for testing
        readerIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],

        // Search keywords for book queries
        searchKeywords: [
            'Java', 'Python', 'Spring', 'Cloud', 'Database',
            'Algorithm', 'Network', 'Security', 'Design', 'Architecture'
        ],

        // ISBN patterns for testing
        isbnPatterns: [
            '978-7-302-', '978-7-111-', '978-0-596-', '978-0-321-'
        ]
    },

    // Request timeouts
    timeouts: {
        default: '30s',
        login: '10s',
        search: '15s',
        fileUpload: '60s'
    }
};

// API Endpoints configuration
export const endpoints = {
    // Authentication endpoints
    auth: {
        login: `${config.apiPrefix}/auth/login`,
        logout: `${config.apiPrefix}/auth/logout`,
        refresh: `${config.apiPrefix}/auth/refresh`
    },

    // Book service endpoints
    books: {
        list: `${config.apiPrefix}/books`,
        detail: (id) => `${config.apiPrefix}/books/${id}`,
        search: `${config.apiPrefix}/books/search`,
        categories: `${config.apiPrefix}/books/categories`
    },

    // Circulation service endpoints
    circulation: {
        borrow: `${config.apiPrefix}/circulation/borrow`,
        return: `${config.apiPrefix}/circulation/return`,
        renew: `${config.apiPrefix}/circulation/renew`,
        records: `${config.apiPrefix}/circulation/records`,
        active: `${config.apiPrefix}/circulation/active`
    },

    // Reader service endpoints
    readers: {
        list: `${config.apiPrefix}/readers`,
        detail: (id) => `${config.apiPrefix}/readers/${id}`,
        search: `${config.apiPrefix}/readers/search`
    },

    // System endpoints
    system: {
        health: '/actuator/health',
        metrics: '/actuator/metrics'
    }
};

// K6 thresholds configuration (compatible with k6 options)
export const k6Thresholds = {
    // HTTP request duration thresholds
    'http_req_duration': [
        `p(50)<${config.thresholds.http_req_duration.p50}`,
        `p(90)<${config.thresholds.http_req_duration.p90}`,
        `p(95)<${config.thresholds.http_req_duration.p95}`,
        `p(99)<${config.thresholds.http_req_duration.p99}`
    ],

    // Error rate threshold
    'http_req_failed': [`rate<${config.thresholds.http_req_failed.rate}`],

    // Custom metric thresholds
    'login_duration': ['p(95)<1000'],
    'book_search_duration': ['p(95)<800'],
    'book_detail_duration': ['p(95)<300'],
    'borrow_duration': ['p(95)<1000'],
    'return_duration': ['p(95)<1000'],
    'reader_list_duration': ['p(95)<500'],

    // Group-specific thresholds
    'group_duration{group:::Authentication}': ['p(95)<1000'],
    'group_duration{group:::Book Operations}': ['p(95)<800'],
    'group_duration{group:::Circulation}': ['p(95)<1000'],
    'group_duration{group:::Reader Operations}': ['p(95)<500']
};

// Helper function to get random item from array
export function getRandomItem(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// Helper function to get random book ID
export function getRandomBookId() {
    return getRandomItem(config.testData.bookIds);
}

// Helper function to get random reader ID
export function getRandomReaderId() {
    return getRandomItem(config.testData.readerIds);
}

// Helper function to get random search keyword
export function getRandomSearchKeyword() {
    return getRandomItem(config.testData.searchKeywords);
}

// Default export for easy import
export default config;
