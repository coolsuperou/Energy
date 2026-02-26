import request from './request'

/**
 * 获取经理首页概览数据
 * 包含：全厂用电指标、车间排名、待处理事项、用电趋势
 */
export function getManagerDashboard() {
  return request.get('/manager/dashboard')
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
  return request.get('/manager/users', { params })
}

/**
 * 获取用户详情
 * @param {number} id - 用户ID
 */
export function getUserById(id) {
  return request.get(`/manager/users/${id}`)
}

/**
 * 创建用户
 * @param {Object} data - 用户信息
 */
export function createUser(data) {
  return request.post('/manager/users', data)
}

/**
 * 更新用户信息
 * @param {number} id - 用户ID
 * @param {Object} data - 更新的用户信息
 */
export function updateUser(id, data) {
  return request.put(`/manager/users/${id}`, data)
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 */
export function deleteUser(id) {
  return request.delete(`/manager/users/${id}`)
}

/**
 * 启用/禁用用户
 * @param {number} id - 用户ID
 * @param {string} status - 状态：active/disabled
 */
export function updateUserStatus(id, status) {
  return request.put(`/manager/users/${id}/status`, { status })
}

// ==================== 技能认证审核接口 ====================

/**
 * 获取技能认证列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 当前页
 * @param {number} params.size - 每页大小
 * @param {string} params.status - 状态筛选（可选）：pending/certified/rejected
 */
export function getCertificationList(params) {
  return request.get('/manager/certifications', { params })
}

/**
 * 审核通过技能认证
 * @param {number} id - 认证ID
 */
export function approveCertification(id) {
  return request.put(`/manager/certifications/${id}/approve`)
}

/**
 * 审核拒绝技能认证
 * @param {number} id - 认证ID
 * @param {string} rejectReason - 拒绝原因
 */
export function rejectCertification(id, rejectReason) {
  return request.put(`/manager/certifications/${id}/reject`, { rejectReason })
}
