import request from './request'

/**
 * 调度员相关 API
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	调度员工作台首页概览相关接口
 * -- =============================================
 */

/**
 * 获取调度员工作台概览数据
 * 返回：待审批数、待处理预警数、进行中工单数、今日关键指标
 * 
 * 返回格式：
 * {
 *   pendingApplicationCount: 5,      // 待审批申请数量
 *   pendingAlertCount: 2,            // 待处理预警数量
 *   inProgressTaskCount: 3,          // 进行中工单数量
 *   todayMetrics: {
 *     todayTotalEnergy: 1234.56,     // 今日总用电量 (kWh)
 *     todayTotalCost: 987.65,        // 今日总费用 (元)
 *     monthTotalEnergy: 12345.67,    // 本月总用电量 (kWh)
 *     todayAlertCount: 2,            // 今日预警次数
 *     todayApprovedCount: 8,         // 今日审批数量
 *     todayCompletedTaskCount: 5     // 今日完成工单数
 *   }
 * }
 */
export function getDashboard() {
  return request.get('/dispatcher/dashboard')
}
