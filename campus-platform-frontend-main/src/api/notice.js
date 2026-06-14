import request from './request'

// 获取公告分页
export function getNoticePage(params) {
  return request.get('/svc/notice/page', { params })
}

// 获取公告详情
export function getNoticeById(id) {
  return request.get(`/svc/notice/${id}`)
}

// 发布公告
export function addNotice(data) {
  return request.post('/svc/notice', data)
}

// 修改公告
export function updateNotice(data) {
  return request.put('/svc/notice', data)
}

// 删除公告
export function deleteNotice(id) {
  return request.delete(`/svc/notice/${id}`)
}

// 发布公告
export function publishNotice(id) {
  return request.put(`/svc/notice/${id}/publish`)
}
