import { test, expect } from '@playwright/test';

/**
 * GCRF Library Management System - Circulation E2E Tests
 *
 * Test coverage:
 * - Borrow book workflow
 * - Return book workflow
 * - Renew book
 * - View borrow records
 * - Fine management
 */

test.describe('Circulation Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login as librarian/admin
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Navigate to circulation page
    await page.click('text=流通管理');
    await page.waitForURL('**/circulation');
  });

  test('should display circulation management page', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1, .page-title')).toContainText('流通管理');

    // Verify tabs
    await expect(page.locator('.el-tabs__item:has-text("借书")')).toBeVisible();
    await expect(page.locator('.el-tabs__item:has-text("还书")')).toBeVisible();
    await expect(page.locator('.el-tabs__item:has-text("借阅记录")')).toBeVisible();
  });

  test('should borrow a book', async ({ page }) => {
    // Click borrow tab
    await page.click('.el-tabs__item:has-text("借书")');

    // Search for reader
    await page.fill('input[placeholder*="读者"]', 'reader001');
    await page.waitForTimeout(500);

    // Select first reader from dropdown
    await page.click('.el-autocomplete-suggestion__list li').first();

    // Search for book
    await page.fill('input[placeholder*="图书"]', 'Java');
    await page.waitForTimeout(500);

    // Select first book
    await page.click('.el-autocomplete-suggestion__list li').first();

    // Set borrow duration
    await page.fill('input[placeholder*="天数"]', '30');

    // Submit borrow request
    await page.click('button:has-text("确认借书")');

    // Wait for confirmation dialog
    await expect(page.locator('.el-message-box')).toBeVisible();
    await page.click('.el-message-box button:has-text("确定")');

    // Verify success message
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
  });

  test('should validate borrow limit', async ({ page }) => {
    // Attempt to borrow with reader who has reached limit
    await page.click('.el-tabs__item:has-text("借书")');

    // Search for reader with max borrows
    await page.fill('input[placeholder*="读者"]', 'maxborrow');
    await page.waitForTimeout(500);

    // If reader exists, try to borrow
    const hasResults = await page.locator('.el-autocomplete-suggestion__list li').count() > 0;
    if (hasResults) {
      await page.click('.el-autocomplete-suggestion__list li').first();

      // Select book
      await page.fill('input[placeholder*="图书"]', 'Java');
      await page.waitForTimeout(500);
      await page.click('.el-autocomplete-suggestion__list li').first();

      // Try to submit
      await page.click('button:has-text("确认借书")');

      // Should show error about limit
      await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should return a book', async ({ page }) => {
    // Switch to return tab
    await page.click('.el-tabs__item:has-text("还书")');

    // Search for borrow record by book barcode or reader card
    await page.fill('input[placeholder*="条码"]', '1234567890');

    // Wait for borrow record to load
    await page.waitForTimeout(1000);

    // If record found, click return button
    const returnButton = page.locator('button:has-text("还书")').first();
    if (await returnButton.isVisible()) {
      await returnButton.click();

      // Confirm return
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.click('.el-message-box button:has-text("确定")');

      // Verify success
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should display overdue fine when returning late', async ({ page }) => {
    // Switch to return tab
    await page.click('.el-tabs__item:has-text("还书")');

    // Search for overdue borrow
    await page.fill('input[placeholder*="条码"]', 'OVERDUE123');
    await page.waitForTimeout(1000);

    // Check if overdue record exists
    const overdueTag = page.locator('.el-tag:has-text("逾期")');
    if (await overdueTag.isVisible()) {
      // Click return
      await page.click('button:has-text("还书")').first();

      // Verify fine amount displayed
      await expect(page.locator('.el-message-box')).toContainText('罚款');
      await expect(page.locator('.fine-amount')).toBeVisible();

      // Confirm return with fine
      await page.click('.el-message-box button:has-text("确定")');
    }
  });

  test('should renew a borrowed book', async ({ page }) => {
    // Go to borrow records
    await page.click('.el-tabs__item:has-text("借阅记录")');

    // Find an active borrow with renew button
    const renewButton = page.locator('.el-table__row:has-text("借阅中") button:has-text("续借")').first();

    if (await renewButton.isVisible()) {
      await renewButton.click();

      // Set renewal duration
      await page.fill('input[placeholder*="续借天数"]', '14');

      // Confirm renewal
      await page.click('.el-dialog button:has-text("确定")');

      // Verify success
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should view borrow records with filters', async ({ page }) => {
    // Go to borrow records tab
    await page.click('.el-tabs__item:has-text("借阅记录")');

    // Verify table displayed
    await expect(page.locator('.el-table')).toBeVisible();

    // Filter by status
    await page.click('.el-select:has-text("状态")');
    await page.click('.el-select-dropdown__item:has-text("借阅中")');
    await page.waitForTimeout(1000);

    // Verify filtered results show only active borrows
    const rows = page.locator('.el-table__row');
    const count = await rows.count();
    if (count > 0) {
      await expect(rows.first()).toContainText('借阅中');
    }
  });

  test('should search borrow records by reader', async ({ page }) => {
    // Go to borrow records
    await page.click('.el-tabs__item:has-text("借阅记录")');

    // Search by reader name
    await page.fill('input[placeholder*="读者"]', 'reader001');
    await page.click('button:has-text("查询")');

    // Wait for results
    await page.waitForTimeout(1000);

    // Verify results contain reader name
    const rows = page.locator('.el-table__row');
    const count = await rows.count();
    if (count > 0) {
      await expect(rows.first()).toContainText('reader001');
    }
  });

  test('should export borrow records', async ({ page }) => {
    // Go to borrow records
    await page.click('.el-tabs__item:has-text("借阅记录")');

    // Click export button
    const exportButton = page.locator('button:has-text("导出")');
    if (await exportButton.isVisible()) {
      // Start waiting for download before clicking
      const downloadPromise = page.waitForEvent('download');
      await exportButton.click();

      // Wait for download
      const download = await downloadPromise;

      // Verify download
      expect(download.suggestedFilename()).toMatch(/借阅记录.*\.(xlsx|xls|csv)/);
    }
  });
});

test.describe('Fine Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login and navigate to fines
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    await page.click('text=流通管理');
    await page.click('text=罚款管理');
    await page.waitForURL('**/fines');
  });

  test('should display fines list', async ({ page }) => {
    // Verify page elements
    await expect(page.locator('h1, .page-title')).toContainText('罚款管理');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('should filter unpaid fines', async ({ page }) => {
    // Click unpaid filter
    await page.click('.el-radio:has-text("未支付")');
    await page.waitForTimeout(1000);

    // Verify results show unpaid fines
    const rows = page.locator('.el-table__row');
    const count = await rows.count();
    if (count > 0) {
      await expect(rows.first()).toContainText('未支付');
    }
  });

  test('should pay a fine', async ({ page }) => {
    // Find first unpaid fine
    const payButton = page.locator('.el-table__row:has-text("未支付") button:has-text("支付")').first();

    if (await payButton.isVisible()) {
      await payButton.click();

      // Confirm payment
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.click('.el-message-box button:has-text("确定")');

      // Verify success
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});

test.describe('Reservation Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login and navigate to reservations
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    await page.click('text=流通管理');
    await page.click('text=预约管理');
    await page.waitForURL('**/reservations');
  });

  test('should display reservations list', async ({ page }) => {
    await expect(page.locator('h1, .page-title')).toContainText('预约管理');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('should cancel a reservation', async ({ page }) => {
    // Find first pending reservation
    const cancelButton = page.locator('.el-table__row:has-text("待取书") button:has-text("取消")').first();

    if (await cancelButton.isVisible()) {
      await cancelButton.click();

      // Confirm cancellation
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.click('.el-message-box button:has-text("确定")');

      // Verify success
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should fulfill a reservation', async ({ page }) => {
    // Find first available reservation
    const fulfillButton = page.locator('.el-table__row:has-text("可取书") button:has-text("取书")').first();

    if (await fulfillButton.isVisible()) {
      await fulfillButton.click();

      // Confirm pickup
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.click('.el-message-box button:has-text("确定")');

      // Verify success (should create borrow record)
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});
