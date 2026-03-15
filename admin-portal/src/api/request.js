/**
 * Axios 统一封装
 * 处理 ApiResponse {code, message, data, requestId} 格式
 * 自动携带 Authorization 头
 */
import axios from 'axios'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

/**
 * 请求拦截器：自动注入 Access Token
 */
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器：统一处理 ApiResponse 格式
 * code === 0 表示成功，否则抛出错误
 */
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }
    // 401 未授权，清除登录态并跳转登录页
    if (res.code === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
      message.error('登录已过期，请重新登录')
      return Promise.reject(new Error('未授权'))
    }
    // 其他业务错误
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    message.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
