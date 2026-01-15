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
