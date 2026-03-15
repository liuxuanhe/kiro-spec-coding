/**
 * 登录相关 API
 * 对接后端 AuthController
 */
import request from './request'

/**
 * 管理员登录
 * POST /api/v1/auth/login
 * @param {Object} data - { username, password }
 * @returns {Promise<Object>} - { accessToken, refreshToken, mustChangePassword, adminId, role, communityId }
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 修改密码（首次登录强制修改）
 * POST /api/v1/auth/change-password
 * @param {Object} data - { oldPassword, newPassword }
 * @returns {Promise<void>}
 */
export function changePassword(data) {
  return request.post('/auth/change-password', data)
}
