/**
 * 品牌 Store - 多租户品牌信息（系统名/副标题/Logo）
 * 由超级管理员在 系统设置 中维护，所有学校共享显示
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSystemConfig } from '@/api/system'

const DEFAULT_BRAND = {
  name: '国创睿峰智能图书馆',
  subtitle: '智慧图书馆管理平台',
  logoUrl: '',
  loginTitle: '欢迎使用智慧图书馆系统',
  loginSubtitle: 'AI 驱动的现代化图书管理平台'
}

export const useBrandStore = defineStore('brand', () => {
  const brand = ref({ ...DEFAULT_BRAND })
  const loaded = ref(false)

  const fullTitle = computed(() =>
    brand.value.subtitle ? `${brand.value.name}` : brand.value.name
  )

  async function loadBrand() {
    try {
      const res = await getSystemConfig()
      const data = res?.data || res || {}
      brand.value = {
        name: data.brand_name || DEFAULT_BRAND.name,
        subtitle: data.brand_subtitle || DEFAULT_BRAND.subtitle,
        logoUrl: data.brand_logo_url || '',
        loginTitle: data.brand_login_title || DEFAULT_BRAND.loginTitle,
        loginSubtitle: data.brand_login_subtitle || DEFAULT_BRAND.loginSubtitle
      }
      loaded.value = true
    } catch (e) {
      console.warn('[brand] load failed, fallback to defaults', e)
    }
  }

  function applyLocal(patch) {
    brand.value = { ...brand.value, ...patch }
  }

  function reset() {
    brand.value = { ...DEFAULT_BRAND }
    loaded.value = false
  }

  return { brand, loaded, fullTitle, loadBrand, applyLocal, reset }
})
