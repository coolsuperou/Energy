import request from './request'

/**
 * 获取今日能耗曲线数据
 */
export function getTodayEnergyData(workshopId) {
  return request.get('/energy-data/today', {
    params: { workshopId }
  })
}

/**
 * 获取指定日期范围的能耗数据
 */
export function getEnergyDataByRange(params) {
  return request.get('/energy-data/range', { params })
}

/**
 * 获取各车间能耗统计
 */
export function getWorkshopEnergyStats(date) {
  return request.get('/energy-data/workshop-stats', {
    params: { date }
  })
}

/**
 * 获取能耗数据汇总分析（经理数据分析页面）
 * 包含：车间对比、时段分析、成本统计
 */
export function getEnergySummary(params) {
  return request.get('/energy-data/summary', { params })
}

/**
 * 获取车间能耗对比数据
 */
export function getWorkshopCompare(params) {
  return request.get('/energy-data/workshop-compare', { params })
}

/**
 * 获取时段用电分析（峰/平/谷占比）
 */
export function getPeriodAnalysis(params) {
  return request.get('/energy-data/period-analysis', { params })
}

/**
 * 获取成本趋势数据
 * @param granularity 粒度：daily（日）、weekly（周）、monthly（月）
 */
export function getCostTrend(params) {
  return request.get('/energy-data/cost-trend', { params })
}
