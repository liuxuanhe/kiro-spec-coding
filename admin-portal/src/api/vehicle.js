/**
 * 车辆管理相关 API
 * 对接后端 VehicleController
 */
import request from './request'

/**
 * 查询车辆列表（支持筛选）
 * GET /api/v1/vehicles?communityId=xxx&houseNo=xxx&carNumber=xxx&page=1&pageSize=10
 * @param {Object} params - { communityId, houseNo, carNumber, page, pageSize }
 * @returns {Promise<Object>} - { records: [...], total }
 */
export function getVehicleList(params) {
  return request.get('/vehicles', { params })
}
