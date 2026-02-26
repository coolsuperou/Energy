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

// 上传反馈图片（支持多图）
export function uploadFeedbackImages(feedbackId, files) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request.post(`/feedback/${feedbackId}/images`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000 // 上传文件超时时间设为60秒
  })
}

// 获取反馈图片列表
export function getFeedbackImages(feedbackId) {
  return request.get(`/feedback/${feedbackId}/images`)
}
