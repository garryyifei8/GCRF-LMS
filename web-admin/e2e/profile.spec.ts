import { test, expect } from '@playwright/test'
import { setAuthStateDirectly } from './helpers/auth'

/**
 * GCRF Library Management System - Profile E2E Tests
 *
 * Based on:
 * - web-admin/src/views/profile/info.vue: el-form with fields 用户名/姓名/角色/部门/联系电话/电子邮箱
 *   Submit button: "保存修改", Reset button: "重置"
 * - web-admin/src/views/profile/password.vue: el-form with 当前密码/新密码/确认新密码
 *   Submit button: "确认修改", Reset button: "重置"
 *   Validation: required fields, min 6 chars for new password, password match check
 */

test.describe('Profile', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthStateDirectly(page)
  })

  test('info page renders form with user fields', async ({ page }) => {
    await page.goto('/profile/info')
    await page.waitForLoadState('networkidle')

    // Page header
    await expect(page.locator('h1')).toContainText('个人信息')

    // el-form should be present
    await expect(page.locator('.el-form').first()).toBeVisible()

    // Username field (disabled, pre-filled with 'admin')
    await expect(page.locator('input[placeholder="请输入姓名"]')).toBeVisible()

    // Action buttons
    await expect(page.locator('button:has-text("保存修改")')).toBeVisible()
    await expect(page.locator('button:has-text("重置")')).toBeVisible()
  })

  test('info page save shows success message with valid data', async ({ page }) => {
    await page.goto('/profile/info')
    await page.waitForLoadState('networkidle')

    // Form is pre-filled with valid data (admin user), just click save
    await page.locator('button:has-text("保存修改")').click()

    // Should show success message (mock waits 500ms then resolves)
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-message--success')).toContainText('保存成功')
  })

  test('empty password form submit shows validation errors', async ({ page }) => {
    await page.goto('/profile/password')
    await page.waitForLoadState('networkidle')

    // Page header
    await expect(page.locator('h1')).toContainText('修改密码')

    // All fields are empty — click submit without filling anything
    await page.locator('button:has-text("确认修改")').click()

    // el-form validation errors should appear
    await expect(page.locator('.el-form-item__error').first()).toBeVisible({ timeout: 3_000 })

    // At minimum the "current password" error should show
    await expect(page.locator('.el-form-item__error').filter({ hasText: '请输入当前密码' })).toBeVisible()
  })

  test('password form with mismatched passwords shows validation error', async ({ page }) => {
    await page.goto('/profile/password')
    await page.waitForLoadState('networkidle')

    // Fill current password and new password, but use a different confirm password
    await page.locator('input[placeholder="请输入当前密码"]').fill('admin123')
    await page.locator('input[placeholder="请输入新密码"]').fill('newPass1')
    await page.locator('input[placeholder="请再次输入新密码"]').fill('differentPass')

    // Trigger blur on confirm field to fire validation
    await page.locator('input[placeholder="请再次输入新密码"]').blur()

    // Mismatch error should appear
    await expect(
      page.locator('.el-form-item__error').filter({ hasText: '两次输入的密码不一致' })
    ).toBeVisible({ timeout: 3_000 })
  })

  test('password form with valid data submits successfully', async ({ page }) => {
    await page.goto('/profile/password')
    await page.waitForLoadState('networkidle')

    // Fill all fields with valid matching passwords
    await page.locator('input[placeholder="请输入当前密码"]').fill('admin123')
    await page.locator('input[placeholder="请输入新密码"]').fill('newPass123')
    await page.locator('input[placeholder="请再次输入新密码"]').fill('newPass123')

    // Submit the form
    await page.locator('button:has-text("确认修改")').click()

    // Should show success message (mock waits 500ms then resolves)
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-message--success')).toContainText('密码修改成功')
  })
})
