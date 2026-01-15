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
 * 获取实时预警数据
 */
export function getAlerts() {
  return request.get('/energy-data/alerts')
}
