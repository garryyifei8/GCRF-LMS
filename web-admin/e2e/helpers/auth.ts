import { Page, expect } from '@playwright/test'

/**
 * Login as admin using the MSW-mocked auth endpoint.
 *
 * Selectors (verified against web-admin/src/views/login/index.vue):
 * - Username input: placeholder="请输入用户名" (Element Plus el-input, no name attr)
 * - Password input: placeholder="请输入密码" (type="password")
 * - Submit button: text "立即登录" (el-button with @click="handleLogin")
 *
 * After login, router redirects to '/' which auto-redirects to '/dashboard'.
 * The route guard is currently disabled (commented out), so all routes are accessible.
 *
 * MSW credentials (from src/mock/handlers/auth.js):
 * - admin / admin123
 * - librarian / lib123
 * - operator / op123
 */
export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login')

  // Wait for the login form to be fully rendered
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 })

  // Fill credentials
  await page.locator('input[placeholder="请输入用户名"]').fill('admin')
  await page.locator('input[placeholder="请输入密码"]').fill('admin123')

  // Click the login button
  await page.locator('button:has-text("立即登录")').click()

  // Wait for navigation away from login page
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15_000 })
}

/**
 * Login with arbitrary credentials.
 *
 * @param page - Playwright page object
 * @param username - Username to fill
 * @param password - Password to fill
 */
export async function loginAs(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login')
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 })

  await page.locator('input[placeholder="请输入用户名"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('button:has-text("立即登录")').click()

  // Wait for navigation away from login page
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15_000 })
}

/**
 * Set auth state directly via localStorage (bypasses UI, faster for test setup).
 * Useful when you need to be "logged in" as a precondition and don't want to test
 * the login flow itself.
 *
 * Note: The Pinia user store uses localStorage key "user" with persist plugin.
 */
export async function setAuthStateDirectly(page: Page): Promise<void> {
  await page.goto('/login')
  await page.evaluate(() => {
    const userState = {
      token: 'mock-token-test-admin',
      userInfo: {
        id: 1,
        username: 'admin',
        name: '系统管理员',
        role: 'admin',
        avatar: '',
        email: 'admin@gcrf.com',
        phone: '13800138000',
        deptName: ''
      },
      permissions: ['*']
    }
    localStorage.setItem('user', JSON.stringify(userState))
  })
  await page.goto('/dashboard')
  await page.waitForLoadState('networkidle')
}

/**
 * Logout via the header dropdown.
 *
 * The logout button is inside an el-dropdown triggered by clicking .user-info.
 * The logout option triggers ElMessageBox.confirm, which we must confirm.
 *
 * After confirming, the router pushes to '/login'.
 */
export async function logout(page: Page): Promise<void> {
  // Click the user-info div in the header to open the dropdown
  await page.locator('.user-info').click()

  // Click "退出登录" in the dropdown menu
  await page.locator('.el-dropdown-menu').getByText('退出登录').click()

  // Handle the ElMessageBox confirm dialog
  await page.locator('.el-message-box').waitFor({ timeout: 5_000 })
  await page.locator('.el-message-box__btns .el-button--primary').click()

  // Wait for redirect to login
  await page.waitForURL('**/login', { timeout: 10_000 })
}
