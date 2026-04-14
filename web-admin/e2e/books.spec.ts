import { test, expect } from '@playwright/test';

/**
 * GCRF Library Management System - Book Management E2E Tests
 *
 * Test coverage:
 * - View books list
 * - Search books
 * - Create new book
 * - Edit book details
 * - Delete book
 * - Category management
 */

test.describe('Book Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login as admin before each test
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Navigate to books page
    await page.click('text=图书管理');
    await page.waitForURL('**/books');
  });

  test('should display books list page', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1, .page-title')).toContainText('图书管理');

    // Verify table is displayed
    await expect(page.locator('.el-table')).toBeVisible();

    // Verify action buttons
    await expect(page.locator('button:has-text("新增图书")')).toBeVisible();
    await expect(page.locator('input[placeholder*="搜索"]')).toBeVisible();
  });

  test('should search books by keyword', async ({ page }) => {
    // Get initial row count
    const initialRows = await page.locator('.el-table__row').count();

    // Enter search keyword
    await page.fill('input[placeholder*="搜索"]', 'Java');
    await page.press('input[placeholder*="搜索"]', 'Enter');

    // Wait for table to update
    await page.waitForTimeout(1000);

    // Verify filtered results
    const filteredRows = await page.locator('.el-table__row').count();

    // Results should be filtered (may be less or equal)
    expect(filteredRows).toBeLessThanOrEqual(initialRows);

    // Verify search keyword appears in results
    const firstRow = page.locator('.el-table__row').first();
    await expect(firstRow).toContainText(/Java/i);
  });

  test('should filter books by category', async ({ page }) => {
    // Click category filter dropdown
    await page.click('.el-select:has-text("分类")');

    // Select a category
    await page.click('.el-select-dropdown__item:has-text("计算机")');

    // Wait for table to update
    await page.waitForTimeout(1000);

    // Verify table shows filtered results
    const rows = await page.locator('.el-table__row').count();
    expect(rows).toBeGreaterThan(0);
  });

  test('should create new book', async ({ page }) => {
    // Click add book button
    await page.click('button:has-text("新增图书")');

    // Wait for dialog to open
    await expect(page.locator('.el-dialog')).toBeVisible();
    await expect(page.locator('.el-dialog__title')).toContainText('新增图书');

    // Fill in book details
    const timestamp = Date.now();
    await page.fill('input[placeholder*="书名"]', `E2E Test Book ${timestamp}`);
    await page.fill('input[placeholder*="作者"]', 'Test Author');
    await page.fill('input[placeholder*="ISBN"]', `978-${timestamp.toString().slice(-10)}`);
    await page.fill('input[placeholder*="出版社"]', 'Test Publisher');
    await page.fill('input[placeholder*="出版年份"]', '2024');

    // Select category
    await page.click('.el-form-item:has-text("分类") .el-select');
    await page.click('.el-select-dropdown__item').first();

    // Fill inventory
    await page.fill('input[placeholder*="总量"]', '10');
    await page.fill('input[placeholder*="可借量"]', '10');
    await page.fill('input[placeholder*="位置"]', 'A1-01');

    // Submit form
    await page.click('.el-dialog button:has-text("确定")');

    // Wait for success message
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    // Verify dialog closed
    await expect(page.locator('.el-dialog')).not.toBeVisible();

    // Verify new book appears in table
    await expect(page.locator(`.el-table__row:has-text("E2E Test Book ${timestamp}")`)).toBeVisible();
  });

  test('should validate required fields when creating book', async ({ page }) => {
    // Click add book button
    await page.click('button:has-text("新增图书")');
    await expect(page.locator('.el-dialog')).toBeVisible();

    // Try to submit without filling required fields
    await page.click('.el-dialog button:has-text("确定")');

    // Verify validation errors
    await expect(page.locator('.el-form-item__error')).toHaveCount(3, { timeout: 2000 });

    // Dialog should remain open
    await expect(page.locator('.el-dialog')).toBeVisible();
  });

  test('should edit book details', async ({ page }) => {
    // Find first edit button and click
    await page.click('.el-table__row .el-button:has-text("编辑")').first();

    // Wait for dialog
    await expect(page.locator('.el-dialog')).toBeVisible();
    await expect(page.locator('.el-dialog__title')).toContainText('编辑图书');

    // Modify book title
    const titleInput = page.locator('input[placeholder*="书名"]');
    await titleInput.clear();
    await titleInput.fill('Updated Book Title');

    // Submit form
    await page.click('.el-dialog button:has-text("确定")');

    // Wait for success message
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    // Verify dialog closed
    await expect(page.locator('.el-dialog')).not.toBeVisible();
  });

  test('should view book details', async ({ page }) => {
    // Click first view button
    await page.click('.el-table__row .el-button:has-text("详情")').first();

    // Wait for details dialog
    await expect(page.locator('.el-dialog')).toBeVisible();
    await expect(page.locator('.el-dialog__title')).toContainText('图书详情');

    // Verify book information is displayed
    await expect(page.locator('.el-dialog .book-info')).toBeVisible();

    // Close dialog
    await page.click('.el-dialog button:has-text("关闭")');
    await expect(page.locator('.el-dialog')).not.toBeVisible();
  });

  test('should delete book with confirmation', async ({ page }) => {
    // Get initial row count
    const initialCount = await page.locator('.el-table__row').count();

    // Click first delete button
    await page.click('.el-table__row .el-button:has-text("删除")').first();

    // Wait for confirmation dialog
    await expect(page.locator('.el-message-box')).toBeVisible();
    await expect(page.locator('.el-message-box__title')).toContainText('确认');

    // Confirm deletion
    await page.click('.el-message-box button:has-text("确定")');

    // Wait for success message
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    // Verify row count decreased
    await page.waitForTimeout(1000);
    const newCount = await page.locator('.el-table__row').count();
    expect(newCount).toBeLessThan(initialCount);
  });

  test('should cancel book deletion', async ({ page }) => {
    // Get initial row count
    const initialCount = await page.locator('.el-table__row').count();

    // Click delete button
    await page.click('.el-table__row .el-button:has-text("删除")').first();

    // Wait for confirmation
    await expect(page.locator('.el-message-box')).toBeVisible();

    // Cancel deletion
    await page.click('.el-message-box button:has-text("取消")');

    // Verify confirmation closed
    await expect(page.locator('.el-message-box')).not.toBeVisible();

    // Verify row count unchanged
    const newCount = await page.locator('.el-table__row').count();
    expect(newCount).toBe(initialCount);
  });

  test('should paginate through books', async ({ page }) => {
    // Verify pagination component exists
    await expect(page.locator('.el-pagination')).toBeVisible();

    // Get first page data
    const firstPageFirstTitle = await page.locator('.el-table__row').first().textContent();

    // Click next page
    await page.click('.el-pagination .btn-next');
    await page.waitForTimeout(1000);

    // Verify page changed
    const secondPageFirstTitle = await page.locator('.el-table__row').first().textContent();
    expect(secondPageFirstTitle).not.toBe(firstPageFirstTitle);

    // Go back to first page
    await page.click('.el-pagination .btn-prev');
    await page.waitForTimeout(1000);

    // Verify back on first page
    const backToFirstTitle = await page.locator('.el-table__row').first().textContent();
    expect(backToFirstTitle).toBe(firstPageFirstTitle);
  });

  test('should change page size', async ({ page }) => {
    // Get initial row count
    const initialCount = await page.locator('.el-table__row').count();

    // Open page size selector
    await page.click('.el-pagination .el-select');

    // Select larger page size
    await page.click('.el-select-dropdown__item:has-text("20")');

    // Wait for table update
    await page.waitForTimeout(1000);

    // Verify row count increased (if there are more than 10 records)
    const newCount = await page.locator('.el-table__row').count();
    expect(newCount).toBeGreaterThanOrEqual(initialCount);
  });
});

test.describe('Book Category Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login and navigate to categories
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Navigate to categories page
    await page.click('text=图书管理');
    await page.click('text=分类管理');
    await page.waitForURL('**/categories');
  });

  test('should display categories in tree structure', async ({ page }) => {
    // Verify tree component
    await expect(page.locator('.el-tree')).toBeVisible();

    // Verify root categories exist
    const rootNodes = await page.locator('.el-tree > .el-tree-node').count();
    expect(rootNodes).toBeGreaterThan(0);
  });

  test('should create new category', async ({ page }) => {
    // Click add category button
    await page.click('button:has-text("新增分类")');

    // Wait for dialog
    await expect(page.locator('.el-dialog')).toBeVisible();

    // Fill category name
    const timestamp = Date.now();
    await page.fill('input[placeholder*="分类名称"]', `Test Category ${timestamp}`);

    // Submit
    await page.click('.el-dialog button:has-text("确定")');

    // Verify success
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    // Verify new category in tree
    await expect(page.locator(`.el-tree-node:has-text("Test Category ${timestamp}")`)).toBeVisible();
  });
});
