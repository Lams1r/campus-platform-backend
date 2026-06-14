import request from './request'

// 获取操作日志分页
export function getOperateLogPage(params) {
  return request.get('/sys/log/operate', { params })
}

// 获取登录日志分页
export function getLoginLogPage(params) {
  return request.get('/sys/log/login', { params })
}
