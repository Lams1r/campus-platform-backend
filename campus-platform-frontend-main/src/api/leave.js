import request from './request'

// 提交请假申请
export function submitLeave(data) {
  return request.post('/edu/leave/submit', data)
}

// 获取我的请假记录
export function getMyLeaves(params) {
  return request.get('/edu/leave/my', { params })
}

// 请假分页查询（教师/管理员）
export function getLeavePage(params) {
  return request.get('/edu/leave/page', { params })
}

// 审核请假
export function auditLeave(id, status, remark) {
  return request.put(`/edu/leave/${id}/approve`, null, { params: { status, remark } })
}
