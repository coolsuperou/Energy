import request from './request'

/**
 * 提交用电申请
 */
export function submitApplication(data) {
  return request.post('/applications', data)
}

/**
 * 获取申请列表
 */
export function getApplications(params) {
  return request.get('/applications', { params })
}

/**
 * 获取我的申请列表（车间用户）
 */
export function getMyApplications(params) {
  return request.get('/applications/my', { params })
}

/**
 * 获取待审批申请列表（调度员）
 */
export function getPendingApplications(params) {
  return request.get('/applications/pending', { params })
}

/**
 * 获取申请详情
 */
export function getApplicationById(id) {
  return request.get(`/applications/${id}`)
}

/**
 * 批准申请
 */
export function approveApplication(id, data) {
  return request.put(`/applications/${id}/approve`, data)
}

/**
 * 拒绝申请
 */
export function rejectApplication(id, data) {
  return request.put(`/applications/${id}/reject`, data)
}

/**
 * 调整并批准
 */
export function adjustApplication(id, data) {
  return request.put(`/applications/${id}/adjust`, data)
}
