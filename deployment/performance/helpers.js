/**
 * GCRF Library Management System - Performance Test Helpers
 *
 * Common helper functions for k6 performance tests
 * Version: 1.0.0
 * Last Updated: 2025-12-01
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { config, endpoints } from './config.js';

// Custom metrics
export const loginDuration = new Trend('login_duration', true);
export const bookSearchDuration = new Trend('book_search_duration', true);
export const bookDetailDuration = new Trend('book_detail_duration', true);
export const borrowDuration = new Trend('borrow_duration', true);
export const returnDuration = new Trend('return_duration', true);
export const readerListDuration = new Trend('reader_list_duration', true);

export const successfulLogins = new Counter('successful_logins');
export const failedLogins = new Counter('failed_logins');
export const successfulBorrows = new Counter('successful_borrows');
export const failedBorrows = new Counter('failed_borrows');
export const successfulReturns = new Counter('successful_returns');
export const failedReturns = new Counter('failed_returns');

export const errorRate = new Rate('errors');

// Token storage for authenticated requests
let authTokens = {};

/**
 * Get default HTTP headers
 * @param {string} token - Optional JWT token for authentication
 * @returns {object} HTTP headers
 */
export function getHeaders(token = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'User-Agent': 'k6-performance-test/1.0'
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
}

/**
 * Perform login and return JWT token
 * @param {object} user - User credentials {username, password}
 * @returns {string|null} JWT token or null if login failed
 */
export function login(user = config.testUsers.reader) {
    const url = `${config.baseUrl}${endpoints.auth.login}`;
    const payload = JSON.stringify({
        username: user.username,
        password: user.password
    });

    const params = {
        headers: getHeaders(),
        timeout: config.timeouts.login
    };

    const startTime = Date.now();
    const response = http.post(url, payload, params);
    const duration = Date.now() - startTime;

    loginDuration.add(duration);

    const success = check(response, {
        'login status is 200': (r) => r.status === 200,
        'login response has token': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && (body.data.token || body.data.accessToken);
            } catch (e) {
                return false;
            }
        }
    });

    if (success) {
        successfulLogins.add(1);
        try {
            const body = JSON.parse(response.body);
            const token = body.data.token || body.data.accessToken;
            authTokens[user.username] = token;
            return token;
        } catch (e) {
            return null;
        }
    } else {
        failedLogins.add(1);
        errorRate.add(1);
        return null;
    }
}

/**
 * Get cached token or perform login
 * @param {object} user - User credentials
 * @returns {string|null} JWT token
 */
export function getToken(user = config.testUsers.reader) {
    if (authTokens[user.username]) {
        return authTokens[user.username];
    }
    return login(user);
}

/**
 * Perform authenticated GET request
 * @param {string} endpoint - API endpoint (relative to baseUrl)
 * @param {object} params - Query parameters
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function authGet(endpoint, params = {}, token = null) {
    const url = `${config.baseUrl}${endpoint}`;
    const reqParams = {
        headers: getHeaders(token),
        timeout: config.timeouts.default
    };

    // Add query parameters if provided
    if (Object.keys(params).length > 0) {
        const queryString = Object.entries(params)
            .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
            .join('&');
        return http.get(`${url}?${queryString}`, reqParams);
    }

    return http.get(url, reqParams);
}

/**
 * Perform authenticated POST request
 * @param {string} endpoint - API endpoint (relative to baseUrl)
 * @param {object} data - Request body
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function authPost(endpoint, data = {}, token = null) {
    const url = `${config.baseUrl}${endpoint}`;
    const params = {
        headers: getHeaders(token),
        timeout: config.timeouts.default
    };

    return http.post(url, JSON.stringify(data), params);
}

/**
 * Query books with pagination
 * @param {object} params - Query parameters {page, size, keyword, categoryId}
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function queryBooks(params = {}, token = null) {
    const defaultParams = {
        page: 1,
        size: 10,
        ...params
    };

    const startTime = Date.now();
    const response = authGet(endpoints.books.list, defaultParams, token);
    const duration = Date.now() - startTime;

    bookSearchDuration.add(duration);

    const success = check(response, {
        'books query status is 200': (r) => r.status === 200,
        'books query has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.code === 200 && body.data;
            } catch (e) {
                return false;
            }
        }
    });

    if (!success) {
        errorRate.add(1);
    }

    return response;
}

/**
 * Get book detail by ID
 * @param {number} bookId - Book ID
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function getBookDetail(bookId, token = null) {
    const startTime = Date.now();
    const response = authGet(endpoints.books.detail(bookId), {}, token);
    const duration = Date.now() - startTime;

    bookDetailDuration.add(duration);

    const success = check(response, {
        'book detail status is 200': (r) => r.status === 200,
        'book detail has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.code === 200 && body.data;
            } catch (e) {
                return false;
            }
        }
    });

    if (!success) {
        errorRate.add(1);
    }

    return response;
}

/**
 * Perform borrow operation
 * @param {number} bookId - Book ID
 * @param {number} readerId - Reader ID
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function borrowBook(bookId, readerId, token) {
    const data = {
        bookId: bookId,
        readerId: readerId
    };

    const startTime = Date.now();
    const response = authPost(endpoints.circulation.borrow, data, token);
    const duration = Date.now() - startTime;

    borrowDuration.add(duration);

    const success = check(response, {
        'borrow status is 200': (r) => r.status === 200,
        'borrow operation successful': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.code === 200;
            } catch (e) {
                return false;
            }
        }
    });

    if (success) {
        successfulBorrows.add(1);
    } else {
        failedBorrows.add(1);
        errorRate.add(1);
    }

    return response;
}

/**
 * Perform return operation
 * @param {number} recordId - Circulation record ID
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function returnBook(recordId, token) {
    const data = {
        recordId: recordId
    };

    const startTime = Date.now();
    const response = authPost(endpoints.circulation.return, data, token);
    const duration = Date.now() - startTime;

    returnDuration.add(duration);

    const success = check(response, {
        'return status is 200': (r) => r.status === 200,
        'return operation successful': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.code === 200;
            } catch (e) {
                return false;
            }
        }
    });

    if (success) {
        successfulReturns.add(1);
    } else {
        failedReturns.add(1);
        errorRate.add(1);
    }

    return response;
}

/**
 * Query readers with pagination
 * @param {object} params - Query parameters
 * @param {string} token - JWT token
 * @returns {object} HTTP response
 */
export function queryReaders(params = {}, token = null) {
    const defaultParams = {
        page: 1,
        size: 10,
        ...params
    };

    const startTime = Date.now();
    const response = authGet(endpoints.readers.list, defaultParams, token);
    const duration = Date.now() - startTime;

    readerListDuration.add(duration);

    const success = check(response, {
        'readers query status is 200': (r) => r.status === 200,
        'readers query has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.code === 200 && body.data;
            } catch (e) {
                return false;
            }
        }
    });

    if (!success) {
        errorRate.add(1);
    }

    return response;
}

/**
 * Check system health
 * @returns {object} HTTP response
 */
export function checkHealth() {
    const url = `${config.baseUrl}${endpoints.system.health}`;
    return http.get(url, {
        headers: getHeaders(),
        timeout: '10s'
    });
}

/**
 * Random think time to simulate realistic user behavior
 * @param {number} min - Minimum seconds
 * @param {number} max - Maximum seconds
 */
export function thinkTime(min = 1, max = 3) {
    sleep(Math.random() * (max - min) + min);
}

/**
 * Short pause between operations
 * @param {number} seconds - Seconds to pause
 */
export function pause(seconds = 0.5) {
    sleep(seconds);
}

// Export all helpers
export default {
    getHeaders,
    login,
    getToken,
    authGet,
    authPost,
    queryBooks,
    getBookDetail,
    borrowBook,
    returnBook,
    queryReaders,
    checkHealth,
    thinkTime,
    pause,
    // Metrics
    loginDuration,
    bookSearchDuration,
    bookDetailDuration,
    borrowDuration,
    returnDuration,
    readerListDuration,
    successfulLogins,
    failedLogins,
    successfulBorrows,
    failedBorrows,
    successfulReturns,
    failedReturns,
    errorRate
};
