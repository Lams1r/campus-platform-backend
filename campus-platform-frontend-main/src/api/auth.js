import request from './request'

// 获取验证码
export function getCaptcha() {
  return request.get('/auth/captcha')
}

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 用户注册（调用 /auth/register 开放注册接口）
export function userRegister(data) {
  return request.post('/auth/register', data)
}

// 兼容旧名称
export const register = userRegister

// 登出
export function logout() {
  return request.post('/auth/logout')
}

// 获取当前用户信息
export function getUserInfo() {
  return request.get('/auth/userInfo')
}
