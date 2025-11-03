import { defineStore } from 'pinia'
import { ref } from 'vue'
import router from '@/router'
import { login as loginAPI, logout as logoutAPI, getUserInfo as getUserInfoAPI } from '@/api/auth'

export const useUserStore = defineStore(
  'user',
  () => {
    // State
    const token = ref('')
    const userInfo = ref({})
    const permissions = ref([])

    // Actions
    function setToken(newToken) {
      token.value = newToken
    }

    function setUserInfo(info) {
      userInfo.value = info
    }

    function setPermissions(perms) {
      permissions.value = perms
    }

    async function login(loginData) {
      try {
        // 调用登录API
        const response = await loginAPI(loginData)

        // 保存Token和用户信息
        if (response.data && response.data.accessToken) {
          setToken(response.data.accessToken)
          setUserInfo({
            id: response.data.userId,
            username: response.data.username,
            name: response.data.realName || response.data.username,
            role: response.data.userType || '用户',
            avatar: response.data.avatar || '',
            email: response.data.email || '',
            phone: response.data.phone || '',
            deptName: response.data.deptName || ''
          })
          setPermissions(response.data.permissions || ['*'])
        }

        return response
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    }

    async function logout() {
      try {
        // 调用登出API
        await logoutAPI()
      } catch (error) {
        console.error('登出API调用失败:', error)
      } finally {
        // 无论API是否成功，都清除本地状态
        token.value = ''
        userInfo.value = {}
        permissions.value = []
        router.push({ name: 'Login' })
      }
    }

    function hasPermission(permission) {
      if (permissions.value.includes('*')) {
        return true
      }
      return permissions.value.includes(permission)
    }

    return {
      token,
      userInfo,
      permissions,
      setToken,
      setUserInfo,
      setPermissions,
      login,
      logout,
      hasPermission
    }
  },
  {
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'user',
          storage: localStorage
        }
      ]
    }
  }
)
