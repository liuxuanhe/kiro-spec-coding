/**
 * Visitor 权限相关 API
 */
import { get, post } from '@/utils/request'
import { getOwnerParams } from '@/utils/user'
import { getUserInfo } from '@/utils/user'

/** 申请 Visitor 权限（自动附加 ownerId、communityId、houseNo 作为查询参数） */
export function applyVisitor(data) {
  const user = getUserInfo()
  const params = `?ownerId=${user?.ownerId}&communityId=${user?.communityId}&houseNo=${encodeURIComponent(user?.houseNo || '')}`
  return post(`/visitors/apply${params}`, data)
}

/** 查询 Visitor 权限列表（按 Data_Domain） */
export function getVisitorList() {
  const { communityId, houseNo } = getOwnerParams()
  return get('/visitors/by-house', { communityId, houseNo })
}

/** 查询月度配额 */
export function getQuota() {
  const { communityId, houseNo } = getOwnerParams()
  return get('/visitors/quota', { communityId, houseNo })
}
