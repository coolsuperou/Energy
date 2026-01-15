import request from './request'

/**
 * 获取工单列表
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
 * 创建工单
 */
export function createTask(data) {
  return request.post('/tasks', data)
}

/**
 * 派单
 */
export function assignTask(id, assigneeId) {
  return request.post(`/tasks/${id}/assign`, null, {
    params: { assigneeId }
  })
}

/**
 * 完成工单
 */
export function completeTask(id, report) {
  return request.post(`/tasks/${id}/complete`, null, {
    params: { report }
  })
}

/**
 * 获取我的工单
 */
export function getMyTasks(params) {
  return request.get('/tasks/my', { params })
}

/**
 * 获取今日待完成任务
 */
export function getTodayTasks() {
  return request.get('/tasks/today')
}
