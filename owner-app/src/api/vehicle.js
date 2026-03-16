/**
 * 车牌管理相关 API
 */
import { get, post, put, del } from '@/utils/request'
import { getOwnerParams } from '@/utils/user'

/** 查询车牌列表（自动附加 communityId、houseNo） */
export function getVehicleList() {
  const { communityId, houseNo } = getOwnerParams()
  return get('/vehicles', { communityId, houseNo })
}

/** 添加车牌（自动附加 communityId、houseNo、ownerId） */
export function addVehicle(data) {
  return post('/vehicles', data)
}

/** 删除车牌 */
export function deleteVehicle(vehicleId) {
  return del(`/vehicles/${vehicleId}`)
}

/** 设置 Primary 车辆（自动附加 communityId、houseNo） */
export function setPrimary(vehicleId, data) {
  const { communityId, houseNo } = getOwnerParams()
  return put(`/vehicles/${vehicleId}/primary`, { ...data, communityId, houseNo })
}
