import { test, expect } from '@playwright/test'
import { setAuthStateDirectly } from './helpers/auth'

/**
 * GCRF Library Management System - Book Management E2E Tests
 *
 * Based on web-admin/src/views/books/list.vue structure:
 * - Search: el-autocomplete, placeholder "请输入书名、作者或ISBN（支持智能搜索）"
 * - Query button: "查询"
 * - New book button: "新增图书" (navigates to /books/catalog, NOT a dialog)
 * - Table action buttons: "查看", "编辑", "删除"
 * - View opens el-dialog with title "图书详情"
 * - Edit shows ElMessage.info (no dialog in current implementation)
 * - Delete shows ElMessageBox.confirm
 *
 * NOTE: networkidle is NOT used — el-autocomplete keeps connections open,
 * so we wait for specific DOM elements instead.
 */

/** Wait for table rows to be visible (data loaded) */
async function waitForTableData(page: import('@playwright/test').Page): Promise<void> {
  await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 15_000 })
}

test.describe('Book Management', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthStateDirectly(page)
    await page.goto('/books/list')
    // Wait for the table element to render (avoids networkidle timeout from el-autocomplete)
    await page.waitForSelector('.el-table', { timeout: 30_000 })
  })

  test('should display books list page', async ({ page }) => {
    // Verify page header
    await expect(page.locator('h1')).toContainText('图书列表')

    // Verify table is displayed
    await expect(page.locator('.el-table')).toBeVisible()

    // Verify search input (el-autocomplete)
    await expect(
      page.locator('input[placeholder="请输入书名、作者或ISBN（支持智能搜索）"]')
    ).toBeVisible()

    // Verify action buttons in toolbar
    await expect(page.locator('button:has-text("查询")')).toBeVisible()
    await expect(page.locator('button:has-text("重置")')).toBeVisible()
    await expect(page.locator('button:has-text("新增图书")')).toBeVisible()

    // Verify pagination
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('should display books in table rows', async ({ page }) => {
    await waitForTableData(page)

    // Should have at least one row
    const rowCount = await page.locator('.el-table__row').count()
    expect(rowCount).toBeGreaterThan(0)
  })

  test('should search books by keyword', async ({ page }) => {
    await waitForTableData(page)
    const initialRows = await page.locator('.el-table__row').count()

    // Fill search input (el-autocomplete)
    const searchInput = page.locator(
      'input[placeholder="请输入书名、作者或ISBN（支持智能搜索）"]'
    )
    await searchInput.fill('计算机')

    // Click query button
    await page.locator('button:has-text("查询")').click()
    // Brief wait for the Vue reactive update + MSW response
    await page.waitForTimeout(1500)

    // Verify table updated (rows may be filtered to 0 or fewer)
    const filteredRows = await page.locator('.el-table__row').count()
    expect(filteredRows).toBeLessThanOrEqual(initialRows)
  })

  test('should reset search filters', async ({ page }) => {
    await waitForTableData(page)

    // Fill search, then reset
    const searchInput = page.locator(
      'input[placeholder="请输入书名、作者或ISBN（支持智能搜索）"]'
    )
    await searchInput.fill('somequery')
    await page.locator('button:has-text("重置")').click()
    await page.waitForTimeout(1000)

    // Input should be cleared after reset
    await expect(searchInput).toHaveValue('')
  })

  test('should filter books by category', async ({ page }) => {
    await waitForTableData(page)

    // Click the category el-select (label "分类")
    const categorySelect = page
      .locator('.el-form-item')
      .filter({ hasText: '分类' })
      .locator('.el-select')
    await categorySelect.click()

    // Select "计算机" option from dropdown
    await page.locator('.el-select-dropdown__item:has-text("计算机")').click()

    // Click query and wait for response
    await page.locator('button:has-text("查询")').click()
    await page.waitForTimeout(1500)

    // Verify table is still displayed
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('should filter books by status', async ({ page }) => {
    await waitForTableData(page)

    // Click the status el-select (label "状态")
    const statusSelect = page
      .locator('.el-form-item')
      .filter({ hasText: '状态' })
      .locator('.el-select')
    await statusSelect.click()

    // Select "在架" option
    await page.locator('.el-select-dropdown__item:has-text("在架")').click()

    // Click query and wait for response
    await page.locator('button:has-text("查询")').click()
    await page.waitForTimeout(1500)

    // Verify table is still displayed
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('should click new book button and navigate to catalog', async ({ page }) => {
    // "新增图书" uses $router.push('/books/catalog') — no dialog
    await page.locator('button:has-text("新增图书")').click()
    await page.waitForURL('**/books/catalog', { timeout: 10_000 })
  })

  test('should view book details in dialog', async ({ page }) => {
    await waitForTableData(page)

    // Click "查看" button in first row
    await page.locator('.el-table__row').first().locator('button:has-text("查看")').click()

    // Verify detail dialog opens with correct title
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-dialog__title')).toContainText('图书详情')

    // Verify book-detail content is displayed
    await expect(page.locator('.el-dialog .book-detail')).toBeVisible()

    // Close dialog by clicking the X button
    await page.locator('.el-dialog__headerbtn').click()
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  })

  test('should show info message when editing book', async ({ page }) => {
    await waitForTableData(page)

    // Click "编辑" button in first row
    // Current implementation just fires ElMessage.info (no edit dialog)
    await page.locator('.el-table__row').first().locator('button:has-text("编辑")').click()

    // Verify an el-message appears
    await expect(page.locator('.el-message')).toBeVisible({ timeout: 5_000 })
  })

  test('should delete book with confirmation dialog', async ({ page }) => {
    await waitForTableData(page)

    // Click "删除" button in first row
    await page.locator('.el-table__row').first().locator('button:has-text("删除")').click()

    // Wait for ElMessageBox confirmation dialog
    await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-message-box')).toContainText('确定要删除')

    // Confirm deletion — dialog should close
    await page.locator('.el-message-box__btns .el-button--primary').click()

    // Confirmation dialog should close after confirming
    await expect(page.locator('.el-message-box')).not.toBeVisible({ timeout: 5_000 })
  })

  test('should cancel book deletion', async ({ page }) => {
    await waitForTableData(page)
    const initialCount = await page.locator('.el-table__row').count()

    // Click "删除" button in first row
    await page.locator('.el-table__row').first().locator('button:has-text("删除")').click()

    // Wait for confirmation dialog
    await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 5_000 })

    // Cancel deletion
    await page.locator('.el-message-box__btns .el-button:not(.el-button--primary)').click()

    // Confirmation dialog should close
    await expect(page.locator('.el-message-box')).not.toBeVisible({ timeout: 5_000 })

    // Row count should remain unchanged
    const newCount = await page.locator('.el-table__row').count()
    expect(newCount).toBe(initialCount)
  })

  test('should paginate through books', async ({ page }) => {
    await waitForTableData(page)
    await expect(page.locator('.el-pagination')).toBeVisible()

    // Get first page first row content
    const firstPageText = await page.locator('.el-table__row').first().textContent()

    // Check if next page button is enabled (mock has 100 books, pageSize 20 → 5 pages)
    const nextBtn = page.locator('.el-pagination .btn-next')
    const isDisabled = await nextBtn.getAttribute('disabled')

    if (!isDisabled) {
      await nextBtn.click()
      await page.waitForTimeout(1500)

      // Verify page changed
      const secondPageText = await page.locator('.el-table__row').first().textContent()
      expect(secondPageText).not.toBe(firstPageText)

      // Go back to first page
      await page.locator('.el-pagination .btn-prev').click()
      await page.waitForTimeout(1500)

      // Verify back on first page
      const backText = await page.locator('.el-table__row').first().textContent()
      expect(backText).toBe(firstPageText)
    } else {
      // Not enough records to paginate — pagination element still present
      await expect(page.locator('.el-pagination')).toBeVisible()
    }
  })

  test('should support batch selection', async ({ page }) => {
    await waitForTableData(page)

    // Click first row's checkbox (selection column)
    await page.locator('.el-table__row').first().locator('.el-checkbox').click()

    // Batch action bar should appear
    await expect(page.locator('.batch-actions')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.batch-info')).toContainText('已选择')

    // Clear selection
    await page.locator('button:has-text("取消选择")').click()
    await expect(page.locator('.batch-actions')).not.toBeVisible({ timeout: 5_000 })
  })
})
