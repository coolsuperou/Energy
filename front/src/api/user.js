import request from './request'

/**
 * 获取用户列表
 * @param {string} role - 用户角色，可选参数 (INSPECTOR, WORKSHOP, DISPATCHER, MANAGER, ADMIN)
 */
export function getUsers(role) {
  return request.get('/users', {
    params: { role }
  })
}

/**
 * 根据ID获取用户详情
 * @param {number} id - 用户ID
 */
export function getUserById(id) {
  return request.get(`/users/${id}`)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request.get('/users/current')
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return request.put('/users/current', data)
}

/**
 * 获取用户统计数据
 */
export function getUserStats() {
  return request.get('/users/current/stats')
}

/**
 * 获取用户技能认证
 */
export function getUserSkills() {
  return request.get('/users/current/skills')
}

/**
 * 获取用户排班信息
 */
export function getUserSchedule(startDate, endDate) {
  return request.get('/users/current/schedule', {
    params: { startDate, endDate }
  })
}

/**
 * 获取工作量统计
 */
export function getWorkStats(days = 7) {
  return request.get('/users/current/work-stats', {
    params: { days }
  })
}
