import request from './request'

// 获取未读消息数量
export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

// 获取我的消息列表
export function getMyNotifications(params) {
  return request.get('/notification/my', { params })
}

// 标记单条消息已读
export function markRead(id) {
  return request.post(`/notification/${id}/read`)
}

// 标记所有消息已读
export function markAllRead() {
  return request.post('/notification/read-all')
}
