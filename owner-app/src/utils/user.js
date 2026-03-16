/**
 * 用户信息工具函数
 * 从本地存储中获取当前登录业主的身份信息
 */

/**
 * 获取当前登录用户信息
 * @returns {Object|null} 用户信息对象，未登录时返回 null
 */
export function getUserInfo() {
  try {
    const info = uni.getStorageSync('userInfo')
    if (info) {
      return JSON.parse(info)
    }
  } catch (e) {
    // 解析失败返回 null
  }
  return null
}

/**
 * 获取当前用户的 Data_Domain 参数（communityId + houseNo）
 * 用于业主端 API 调用时自动附加身份参数
 * @returns {{ communityId: number, houseNo: string }}
 */
export function getOwnerParams() {
  const user = getUserInfo()
  return {
    communityId: user?.communityId,
    houseNo: user?.houseNo
  }
}
