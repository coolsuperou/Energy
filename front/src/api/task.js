import request from './request'

/**
 * 获取工单列表（调度员）
 */
export function getTasks(params) {
  return request.get('/tasks', { params })
}

/**
 * 获取工单详情
 */
export function getTaskById(id) {
  return request.get(`/tasks/${id}`)
}

/**
 * 创建工单（调度员）
 */
export function createTask(data) {
  return request.post('/tasks', data)
}

/**
 * 派单（调度员）
 */
export function assignTask(id, assigneeId) {
  return request.post(`/tasks/${id}/assign`, null, {
    params: { assigneeId }
  })
}

/**
 * 完成工单（调度员接口）
 */
export function completeTask(id, report) {
  return request.post(`/tasks/${id}/complete`, null, {
    params: { report }
  })
}

/**
 * 获取我的工单（调度员接口）
 */
export function getMyTasks(params) {
  return request.get('/tasks/my', { params })
}

/**
 * 获取今日待完成任务（调度员接口）
 */
export function getTodayTasks() {
  return request.get('/tasks/today')
}

/**
 * 获取我的任务统计（调度员接口）
 */
export function getMyTaskStats() {
  return request.get('/tasks/my/stats')
}

// ==================== 巡检员专用接口 ====================

/**
 * 获取巡检员我的任务列表
 * @param {Object} params - 查询参数
 * @param {string} params.status - 任务状态（可选）：IN_PROGRESS进行中、COMPLETED已完成
 */
export function getInspectorMyTasks(params) {
  return request.get('/inspector/tasks/my', { params })
}

/**
 * 获取巡检员今日待完成任务
 */
export function getInspectorTodayTasks() {
  return request.get('/inspector/tasks/today')
}

/**
 * 巡检员完成工单
 * @param {number} id - 任务ID
 * @param {string} report - 巡检报告内容
 * @param {File[]} images - 完成图片（可选）
 */
export function inspectorCompleteTask(id, report, images) {
  const formData = new FormData()
  formData.append('report', report)
  if (images && images.length > 0) {
    images.forEach(file => {
      formData.append('images', file)
    })
  }
  return request.post(`/inspector/tasks/${id}/complete`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取巡检员任务统计
 */
export function getInspectorTaskStats() {
  return request.get('/inspector/tasks/stats')
}
