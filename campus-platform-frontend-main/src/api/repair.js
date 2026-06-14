import request from './request'

// 获取报修分页
export function getRepairPage(params) {
  return request.get('/svc/repair/page', { params })
}

// 获取我的报修
export function getMyRepairs(params) {
  return request.get('/svc/repair/my', { params })
}

// 提交报修
export function submitRepair(data) {
  return request.post('/svc/repair', data)
}

// 受理报修
export function acceptRepair(id, handlerId) {
  return request.put(`/svc/repair/${id}/accept`, null, { params: { handlerId } })
}

// 完成报修
export function finishRepair(id, remark) {
  return request.put(`/svc/repair/${id}/finish`, null, { params: { remark } })
}

// 验收报修（管理员）
export function verifyRepair(id, score, remark) {
  return request.put(`/svc/repair/${id}/verify`, null, { params: { score, remark } })
}
