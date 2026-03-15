/**
 * 小区相关 API
 * 对接后端 CommunityController
 */
import request from './request'

/**
 * 查询小区列表
 * GET /api/v1/communities
 * Super_Admin 返回所有小区，Property_Admin 仅返回本小区
 */
export function getCommunityList() {
  return request.get('/communities')
}

/**
 * 创建小区（Super_Admin 专属）
 * POST /api/v1/communities
 * @param {Object} data - { communityName, communityCode, province, city, district, address, contactPerson, contactPhone }
 */
export function createCommunity(data) {
  return request.post('/communities', data)
}

/**
 * 更新小区信息（Super_Admin 专属）
 * PUT /api/v1/communities/{id}
 * @param {number} id - 小区ID
 * @param {Object} data - 更新字段
 */
export function updateCommunity(id, data) {
  return request.put(`/communities/${id}`, data)
}

/**
 * 切换当前操作小区（Super_Admin 专属）
 * POST /api/v1/auth/switch-community
 * @param {number} communityId - 目标小区ID
 * @returns {Promise<{accessToken: string}>}
 */
export function switchCommunity(communityId) {
  return request.post('/auth/switch-community', { communityId })
}
