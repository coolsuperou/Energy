import request from './request'

// 登录
export function login(data) {
  return request.post('/users/login', data)
}

// 注册
export function register(data) {
  return request.post('/users/register', data)
}

// 登出
export function logout() {
  return request.post('/users/logout')
}

// 获取当前用户
export function getCurrentUser() {
  return request.get('/users/current')
}
