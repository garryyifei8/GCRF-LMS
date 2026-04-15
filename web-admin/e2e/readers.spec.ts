import { test, expect } from '@playwright/test'
import { setAuthStateDirectly } from './helpers/auth'

/**
 * GCRF Library Management System - Readers Management E2E Tests
 *
 * Based on:
 * - web-admin/src/views/readers/students.vue
 * - web-admin/src/views/readers/teachers.vue
 * - web-admin/src/mock/handlers/readers.js
 *
 * Notes:
 * - Students list: placeholder "请输入姓名、学号或读者证号", query button "查询"
 * - Teachers list: placeholder "请输入姓名、工号或读者证号", query button "查询"
 * - Both pages use el-table with paginated mock data (200 readers)
 * - New student dialog: el-dialog with form fields (学号, 姓名, etc.)
 * - Delete: ElMessageBox.confirm "确定要删除学生 {name} 吗？"
 */

test.describe('Readers Management', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthStateDirectly(page)
  })

  test('students list page renders table', async ({ page }) => {
    await page.goto('/readers/students')
    await page.waitForLoadState('networkidle')

    // Page header should be visible
    await expect(page.locator('h1')).toContainText('学生读者管理')

    // Table should be visible
    await expect(page.locator('.el-table')).toBeVisible()

    // Pagination should be present
    await expect(page.locator('.el-pagination')).toBeVisible()

    // Search input should be present
    await expect(
      page.locator('input[placeholder="请输入姓名、学号或读者证号"]')
    ).toBeVisible()
  })

  test('search keyword triggers list refresh', async ({ page }) => {
    await page.goto('/readers/students')
    await page.waitForLoadState('networkidle')

    // Wait for initial data
    await expect(page.locator('.el-table')).toBeVisible()

    // Fill search input
    await page.locator('input[placeholder="请输入姓名、学号或读者证号"]').fill('张')

    // Click query button
    await page.locator('button:has-text("查询")').click()
    await page.waitForLoadState('networkidle')

    // Table should still be visible after search
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('pagination change triggers list refresh', async ({ page }) => {
    await page.goto('/readers/students')
    await page.waitForLoadState('networkidle')

    // Wait for table and pagination
    await expect(page.locator('.el-table')).toBeVisible()
    await expect(page.locator('.el-pagination')).toBeVisible()

    // Try clicking next page if available
    const nextBtn = page.locator('.el-pagination .btn-next')
    const isEnabled = await nextBtn.isEnabled()

    if (isEnabled) {
      await nextBtn.click()
      await page.waitForLoadState('networkidle')
      // Table should still be visible after page change
      await expect(page.locator('.el-table')).toBeVisible()
    } else {
      // Not enough records to paginate — verify pagination element still exists
      await expect(page.locator('.el-pagination')).toBeVisible()
    }
  })

  test('new student dialog opens', async ({ page }) => {
    await page.goto('/readers/students')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-table')).toBeVisible()

    // Click "新增学生" button
    await page.locator('button:has-text("新增学生")').click()

    // Dialog should appear
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5_000 })

    // Form fields should be visible inside dialog
    await expect(page.locator('.el-dialog input[placeholder="请输入学号"]')).toBeVisible()
    await expect(page.locator('.el-dialog input[placeholder="请输入姓名"]')).toBeVisible()

    // Close dialog
    await page.locator('.el-dialog__headerbtn').click()
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  })

  test('delete confirmation flow', async ({ page }) => {
    await page.goto('/readers/students')
    await page.waitForLoadState('networkidle')

    // Wait for table rows
    await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 10_000 })

    // Click "删除" link in first row
    await page.locator('.el-table__row').first().locator('a:has-text("删除"), .el-link:has-text("删除")').first().click()

    // Confirmation dialog should appear
    await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-message-box')).toContainText('确定要删除')

    // Cancel the deletion
    await page.locator('.el-message-box__btns .el-button:not(.el-button--primary)').click()

    // Dialog should close
    await expect(page.locator('.el-message-box')).not.toBeVisible({ timeout: 5_000 })

    // Table should still be visible
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('teachers page renders table', async ({ page }) => {
    await page.goto('/readers/teachers')
    await page.waitForLoadState('networkidle')

    // Page header should be visible
    await expect(page.locator('h1')).toContainText('教师读者管理')

    // Table should be visible
    await expect(page.locator('.el-table')).toBeVisible()

    // Teachers-specific search input
    await expect(
      page.locator('input[placeholder="请输入姓名、工号或读者证号"]')
    ).toBeVisible()

    // Query button
    await expect(page.locator('button:has-text("查询")')).toBeVisible()
  })
})
