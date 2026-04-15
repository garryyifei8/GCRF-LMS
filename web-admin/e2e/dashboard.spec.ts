import { test, expect } from '@playwright/test'
import { setAuthStateDirectly } from './helpers/auth'

/**
 * GCRF Library Management System - Dashboard E2E Tests
 *
 * Based on web-admin/src/views/dashboard/index.vue structure:
 * - Stat cards: `.stat-card` (4 cards: 总馆藏量, 注册读者, 今日借阅, 逾期未还)
 * - Charts: ECharts renders into div refs (trendChartRef, categoryChartRef) as canvas elements
 * - Trend filter: el-radio-group with labels 本周/本月/本年
 * - Quick action cards: `.action-card` (4 cards)
 *
 * MSW analytics handlers provide mock data for /api/v1/analytics/* endpoints.
 */

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthStateDirectly(page)
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
  })

  test('mounts without console errors', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', (e) => errors.push(e.message))

    // Give ECharts and async data time to settle
    await page.waitForTimeout(2000)

    expect(errors).toEqual([])
  })

  test('stat cards are visible (4 cards)', async ({ page }) => {
    // Dashboard renders 4 stat-card divs: 总馆藏量, 注册读者, 今日借阅, 逾期未还
    const cards = page.locator('.stat-card')
    await expect(cards.first()).toBeVisible({ timeout: 10_000 })

    const count = await cards.count()
    expect(count).toBeGreaterThanOrEqual(4)

    // Verify key stat labels are displayed
    await expect(page.locator('.stat-label').filter({ hasText: '总馆藏量' })).toBeVisible()
    await expect(page.locator('.stat-label').filter({ hasText: '注册读者' })).toBeVisible()
    await expect(page.locator('.stat-label').filter({ hasText: '今日借阅' })).toBeVisible()
    await expect(page.locator('.stat-label').filter({ hasText: '逾期未还' })).toBeVisible()
  })

  test('ECharts canvas elements render', async ({ page }) => {
    // ECharts mounts into the chart div refs and renders a <canvas> element
    // Give charts time to initialize after data loads
    await page.waitForTimeout(1500)

    const canvases = page.locator('canvas')
    await expect(canvases.first()).toBeVisible({ timeout: 10_000 })

    const count = await canvases.count()
    // Expect at least 2 charts: trend chart + category chart
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('trend period radio filter switches correctly', async ({ page }) => {
    // The chart header has el-radio-group with 本周/本月/本年
    const radioGroup = page.locator('.el-radio-group')
    await expect(radioGroup.first()).toBeVisible({ timeout: 10_000 })

    // Click "本月" radio button
    await page.locator('.el-radio-button').filter({ hasText: '本月' }).click()
    await page.waitForTimeout(500)

    // Verify "本月" is now the active selection
    const monthBtn = page.locator('.el-radio-button').filter({ hasText: '本月' })
    await expect(monthBtn).toHaveClass(/is-active/, { timeout: 3_000 })

    // Click "本年" and verify it becomes active
    await page.locator('.el-radio-button').filter({ hasText: '本年' }).click()
    await page.waitForTimeout(500)

    const yearBtn = page.locator('.el-radio-button').filter({ hasText: '本年' })
    await expect(yearBtn).toHaveClass(/is-active/, { timeout: 3_000 })
  })
})
