/**
 * 车位配置相关 API
 * 对接后端 ParkingConfigController
 */
import request from './request'

/**
 * 查询停车场配置
 * GET /api/v1/parking/config?communityId=xxx
 * @param {Object} params - { communityId }
 * @returns {Promise<Object>} - 车位配置信息
 */
export function getParkingConfig(params) {
  return request.get('/parking/config', { params })
}

/**
 * 修改停车场配置
 * PUT /api/v1/parking/config
 * @param {Object} data - { communityId, totalSpaces, visitorQuotaHours, visitorSingleDurationHours, version }
 * @returns {Promise<Object>} - 更新后的配置信息
 */
export function updateParkingConfig(data) {
  return request.put('/parking/config', data)
}
