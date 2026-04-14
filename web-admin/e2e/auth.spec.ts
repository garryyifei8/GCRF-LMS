import { test, expect } from '@playwright/test';

/**
 * GCRF Library Management System - Authentication E2E Tests
 *
 * Test coverage:
 * - Admin login flow
 * - Reader login flow
 * - Invalid credentials handling
 * - Session persistence
 * - Logout functionality
 */

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to login page before each test
    await page.goto('/login');
  });

  test('should display login page correctly', async ({ page }) => {
    // Verify page title
    await expect(page).toHaveTitle(/国创睿峰图书馆/);

    // Verify login form elements
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('admin should login successfully', async ({ page }) => {
    // Fill in admin credentials
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');

    // Submit form
    await page.click('button[type="submit"]');

    // Wait for navigation to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Verify we're on the dashboard
    await expect(page).toHaveURL(/dashboard/);

    // Verify user info is displayed
    await expect(page.locator('text=admin')).toBeVisible();
  });

  test('reader should login successfully', async ({ page }) => {
    // Fill in reader credentials
    await page.fill('input[name="username"]', 'reader001');
    await page.fill('input[name="password"]', 'reader123');

    // Submit form
    await page.click('button[type="submit"]');

    // Wait for navigation
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Verify successful login
    await expect(page).toHaveURL(/dashboard/);
    await expect(page.locator('text=reader001')).toBeVisible();
  });

  test('should show error for invalid credentials', async ({ page }) => {
    // Fill in invalid credentials
    await page.fill('input[name="username"]', 'invaliduser');
    await page.fill('input[name="password"]', 'wrongpassword');

    // Submit form
    await page.click('button[type="submit"]');

    // Wait for error message
    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 });

    // Verify still on login page
    await expect(page).toHaveURL(/login/);
  });

  test('should show validation errors for empty fields', async ({ page }) => {
    // Submit without filling fields
    await page.click('button[type="submit"]');

    // Verify validation errors
    await expect(page.locator('.el-form-item__error')).toHaveCount(2);
  });

  test('should toggle password visibility', async ({ page }) => {
    const passwordInput = page.locator('input[name="password"]');

    // Password should be hidden by default
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Click toggle button (if exists)
    const toggleButton = page.locator('.el-input__suffix .el-icon');
    if (await toggleButton.count() > 0) {
      await toggleButton.click();

      // Password should now be visible
      await expect(passwordInput).toHaveAttribute('type', 'text');
    }
  });

  test('should persist session after page reload', async ({ page }) => {
    // Login first
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Reload page
    await page.reload();

    // Should still be logged in
    await expect(page).toHaveURL(/dashboard/);
    await expect(page.locator('text=admin')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Click user dropdown
    await page.click('.user-info');

    // Click logout
    await page.click('text=退出登录');

    // Should redirect to login page
    await page.waitForURL('**/login');
    await expect(page).toHaveURL(/login/);

    // Session should be cleared - try to access dashboard
    await page.goto('/dashboard');

    // Should be redirected back to login
    await expect(page).toHaveURL(/login/);
  });
});

test.describe('Session Management', () => {
  test('should redirect to login when accessing protected route without auth', async ({ page }) => {
    // Try to access dashboard directly
    await page.goto('/dashboard');

    // Should be redirected to login
    await expect(page).toHaveURL(/login/);
  });

  test('should redirect to dashboard when accessing login while authenticated', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Try to access login page
    await page.goto('/login');

    // Should be redirected back to dashboard
    await expect(page).toHaveURL(/dashboard/);
  });
});
