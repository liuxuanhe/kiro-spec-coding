/**
 * 入场记录相关 API
 */
import { get } from '@/utils/request'
import { getOwnerParams } from '@/utils/user'

/**
 * 查询入场记录（游标分页，自动附加 communityId、houseNo）
 * @param {Object} params
 * @param {string} params.startTime - 开始时间
 * @param {string} params.endTime - 结束时间
 * @param {string} params.cursor - 游标（可选）
 * @param {number} params.pageSize - 每页条数
 */
export function getParkingRecords(params) {
  const { communityId, houseNo } = getOwnerParams()
  return get('/parking/records', { ...params, communityId, houseNo })
}
