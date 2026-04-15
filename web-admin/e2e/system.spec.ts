import { test, expect } from '@playwright/test'
import { setAuthStateDirectly } from './helpers/auth'

/**
 * GCRF Library Management System - System Management E2E Tests
 *
 * Based on:
 * - web-admin/src/views/system/users.vue
 * - web-admin/src/views/system/roles.vue
 * - web-admin/src/views/system/departments.vue
 * - web-admin/src/mock/handlers/system.js
 *
 * Notes:
 * - Users: el-table with paginated list, search "请输入用户名或姓名", "新增用户" button
 * - Roles: two-column layout — left el-table (role list), right el-tree (permissions)
 * - Departments: el-table with tree data (IT部, 图书馆, 前台)
 * - MSW returns records: [...] for paginated endpoints
 */

test.describe('System Management', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthStateDirectly(page)
  })

  test('users list renders', async ({ page }) => {
    await page.goto('/system/users')
    await page.waitForLoadState('networkidle')

    // Page header
    await expect(page.locator('h1')).toContainText('用户管理')

    // Table should be visible
    await expect(page.locator('.el-table')).toBeVisible()

    // Search input present
    await expect(page.locator('input[placeholder="请输入用户名或姓名"]')).toBeVisible()

    // Action buttons present
    await expect(page.locator('button:has-text("查询")')).toBeVisible()
    await expect(page.locator('button:has-text("新增用户")')).toBeVisible()
  })

  test('user search works', async ({ page }) => {
    await page.goto('/system/users')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-table')).toBeVisible()

    // Fill search keyword
    await page.locator('input[placeholder="请输入用户名或姓名"]').fill('admin')

    // Click query
    await page.locator('button:has-text("查询")').click()
    await page.waitForLoadState('networkidle')

    // Table should remain visible after search
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('roles page renders table', async ({ page }) => {
    await page.goto('/system/roles')
    await page.waitForLoadState('networkidle')

    // Page header
    await expect(page.locator('h1')).toContainText('角色管理')

    // Role list table (left panel)
    await expect(page.locator('.el-table')).toBeVisible()

    // New role button
    await expect(page.locator('button:has-text("新增角色")')).toBeVisible()
  })

  test('departments page renders table', async ({ page }) => {
    await page.goto('/system/departments')
    await page.waitForLoadState('networkidle')

    // Departments page uses el-table (not el-tree)
    await expect(page.locator('.el-table')).toBeVisible()

    // New department button
    await expect(page.locator('button:has-text("新增部门")')).toBeVisible()

    // Search form visible
    await expect(page.locator('input[placeholder="请输入部门名称"]')).toBeVisible()
  })

  test('new user dialog opens', async ({ page }) => {
    await page.goto('/system/users')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-table')).toBeVisible()

    // Click "新增用户"
    await page.locator('button:has-text("新增用户")').click()

    // Dialog should appear
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5_000 })

    // Form fields should be inside the dialog
    await expect(page.locator('.el-dialog input[placeholder="请输入用户名"]')).toBeVisible()
    await expect(page.locator('.el-dialog input[placeholder="请输入姓名"]')).toBeVisible()

    // Close dialog
    await page.locator('.el-dialog__headerbtn').click()
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  })

  test('permissions assignment dialog opens from roles', async ({ page }) => {
    await page.goto('/system/roles')
    await page.waitForLoadState('networkidle')

    // Wait for role list table
    await expect(page.locator('.el-table')).toBeVisible()
    await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 10_000 })

    // Click on first role row to select it — triggers permission tree on right panel
    await page.locator('.el-table__row').first().click()

    // Permission tree (el-tree) should appear in the right panel after selecting a role
    await expect(page.locator('.el-tree')).toBeVisible({ timeout: 5_000 })

    // "保存权限" button should be visible after a role is selected
    await expect(page.locator('button:has-text("保存权限")')).toBeVisible({ timeout: 5_000 })
  })
})
