import request from './request'

/**
 * 获取管理员首页概览数据
 * 包含：用户统计、角色分布、今日活跃用户、系统状态、最近操作日志
 */
export function getAdminDashboard() {
  return request.get('/admin/dashboard')
}

// ==================== 用户管理接口 ====================

/**
 * 获取用户列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 当前页
 * @param {number} params.size - 每页大小
 * @param {string} params.role - 角色筛选（可选）
 * @param {string} params.keyword - 关键词搜索（可选）
 */
export function getUserList(params) {
  return request.get('/admin/users', { params })
}

/**
 * 获取用户详情
 * @param {number} id - 用户ID
 */
export function getUserById(id) {
  return request.get(`/admin/users/${id}`)
}

/**
 * 创建用户
 * @param {Object} data - 用户信息
 */
export function createUser(data) {
  return request.post('/admin/users', data)
}

/**
 * 更新用户信息
 * @param {number} id - 用户ID
 * @param {Object} data - 更新的用户信息
 */
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 */
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

/**
 * 启用/禁用用户
 * @param {number} id - 用户ID
 * @param {string} status - 状态：active/disabled
 */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

/**
 * 重置用户密码
 * @param {number} id - 用户ID
 * @param {string} password - 新密码
 */
export function resetPassword(id, password) {
  return request.put(`/admin/users/${id}/reset-password`, { password })
}

// ==================== 系统配置接口 ====================

/**
 * 获取系统配置
 * 包含：电价参数、预警阈值、时段配置
 */
export function getSystemConfig() {
  return request.get('/admin/config')
}

/**
 * 更新系统配置
 * @param {Object} data - 配置参数
 * @param {number} data.peakPrice - 峰时电价
 * @param {number} data.normalPrice - 平时电价
 * @param {number} data.valleyPrice - 谷时电价
 * @param {number} data.powerOverloadThreshold - 功率超限阈值
 * @param {number} data.powerWarningThreshold - 功率预警阈值
 * @param {number} data.quotaWarningThreshold - 配额预警阈值
 * @param {number} data.peakStartHour1 - 峰时开始时间1
 * @param {number} data.peakEndHour1 - 峰时结束时间1
 * @param {number} data.peakStartHour2 - 峰时开始时间2
 * @param {number} data.peakEndHour2 - 峰时结束时间2
 * @param {number} data.normalStartHour - 平时开始时间
 * @param {number} data.normalEndHour - 平时结束时间
 */
export function updateSystemConfig(data) {
  return request.put('/admin/config', data)
}


// ==================== 操作日志接口 ====================

/**
 * 获取操作日志列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 当前页
 * @param {number} params.size - 每页大小
 * @param {string} params.operationType - 操作类型筛选（可选）
 * @param {number} params.userId - 用户ID筛选（可选）
 * @param {string} params.startDate - 开始日期（可选，格式：yyyy-MM-dd）
 * @param {string} params.endDate - 结束日期（可选，格式：yyyy-MM-dd）
 */
export function getOperationLogs(params) {
  return request.get('/admin/logs', { params })
}

/**
 * 获取所有操作类型（用于筛选下拉框）
 */
export function getOperationTypes() {
  return request.get('/admin/logs/types')
}
