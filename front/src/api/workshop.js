import request from './request'

/**
 * 车间用户相关 API
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	车间用户首页概览相关接口
 * -- =============================================
 */

/**
 * 获取车间首页概览数据
 * 返回：今日用电量、本月用电量、最近申请状态、未读消息数
 */
export function getDashboard() {
  return request.get('/workshop/dashboard')
}

/**
 * 获取本车间能耗数据
 * 支持日期范围查询，返回按小时的能耗记录和费用统计
 * 
 * @param {Object} params - 查询参数
 * @param {string} params.startDate - 开始日期（可选，格式：YYYY-MM-DD，默认今天）
 * @param {string} params.endDate - 结束日期（可选，格式：YYYY-MM-DD，默认今天）
 * @returns {Promise} 返回能耗数据和费用统计
 * 
 * 返回格式：
 * {
 *   energyData: [...],  // 能耗记录列表
 *   costStats: {
 *     peakEnergy: 0, peakCost: 0,
 *     normalEnergy: 0, normalCost: 0,
 *     valleyEnergy: 0, valleyCost: 0,
 *     totalEnergy: 0, totalCost: 0
 *   },
 *   workshopId: 1,
 *   startDate: "2024-01-16",
 *   endDate: "2024-01-16"
 * }
 */
export function getMyWorkshopEnergy(params = {}) {
  return request.get('/energy-data/my-workshop', { params })
}
