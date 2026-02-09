import request from './request'

/**
 * 获取巡检任务列表
 */
export function getInspectionTasks(params) {
  return request.get('/inspections', { params })
}

/**
 * 获取今日巡检任务
 */
export function getTodayInspections() {
  return request.get('/inspections/today')
}

/**
 * 获取巡检任务详情
 */
export function getInspectionById(id) {
  return request.get(`/inspections/${id}`)
}

/**
 * 开始巡检
 */
export function startInspection(id) {
  return request.post(`/inspections/${id}/start`)
}

/**
 * 提交巡检记录
 */
export function submitInspection(id, data) {
  return request.post(`/inspections/${id}/submit`, data)
}

/**
 * 获取巡检检查项
 */
export function getInspectionItems(inspectionId) {
  return request.get(`/inspections/${inspectionId}/items`)
}

/**
 * 更新检查项结果
 */
export function updateInspectionItem(inspectionId, itemId, data) {
  return request.put(`/inspections/${inspectionId}/items/${itemId}`, data)
}
