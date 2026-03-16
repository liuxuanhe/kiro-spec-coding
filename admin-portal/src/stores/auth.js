/**
 * 认证状态管理
 * 管理 Access Token、用户信息、登录状态
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复登录态
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const role = ref(localStorage.getItem('role') || '')
  const communityId = ref(Number(localStorage.getItem('communityId')) || null)
  const adminId = ref(Number(localStorage.getItem('adminId')) || null)

  /** 是否需要强制修改密码（首次登录） */
  const mustChangePassword = ref(false)

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!accessToken.value)

  /**
   * 保存登录信息
   * @param {Object} loginData - 后端登录响应
   */
  function setLoginInfo(loginData) {
    accessToken.value = loginData.accessToken
    refreshToken.value = loginData.refreshToken || ''
    role.value = loginData.role || ''
    communityId.value = loginData.communityId || null
    adminId.value = loginData.adminId || null

    localStorage.setItem('accessToken', loginData.accessToken)
    localStorage.setItem('refreshToken', loginData.refreshToken || '')
    localStorage.setItem('role', loginData.role || '')
    localStorage.setItem('communityId', String(loginData.communityId || ''))
    localStorage.setItem('adminId', String(loginData.adminId || ''))
  }

  /** 清除登录态 */
  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    role.value = ''
    communityId.value = null
    adminId.value = null
    mustChangePassword.value = false

    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('role')
    localStorage.removeItem('communityId')
    localStorage.removeItem('adminId')
  }

  return {
    accessToken,
    refreshToken,
    role,
    communityId,
    adminId,
    mustChangePassword,
    isLoggedIn,
    setLoginInfo,
    logout
  }
})
