import request from './request'

// 获取概览数据
export function getDashboardOverview() {
  return request.get('/dashboard/overview')
}

// 生成数据快照
export function generateSnapshot() {
  return request.post('/dashboard/snapshot')
}

// 获取最新快照
export function getLatestSnapshot() {
  return request.get('/dashboard/snapshot/latest')
}
