import request from './request'

// 提交反馈
export function submitFeedback(data) {
  return request.post('/feedback', data)
}

// 获取我的反馈列表
export function getMyFeedbacks(params) {
  return request.get('/feedback/my', { params })
}

// 获取所有反馈（管理端）
export function getAllFeedbacks(params) {
  return request.get('/feedback/all', { params })
}

// 获取反馈详情
export function getFeedbackDetail(id) {
  return request.get(`/feedback/${id}`)
}

// 撤回反馈
export function withdrawFeedback(id) {
  return request.post(`/feedback/${id}/withdraw`)
}

// 处理反馈
export function handleFeedback(id, data) {
  return request.post(`/feedback/${id}/handle`, data)
}
