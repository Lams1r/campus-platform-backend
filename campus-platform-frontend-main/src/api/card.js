import request from './request'

// 获取我的消费记录
export function getMyCardRecords(params) {
  return request.get('/svc/card/record/my', { params })
}

// 获取全部消费记录（管理员）
export function getCardRecordPage(params) {
  return request.get('/svc/card/record/page', { params })
}

// 挂失
export function reportLoss(cardNo) {
  return request.post('/svc/card/loss/report', null, { params: { cardNo } })
}

// 解除挂失
export function unlockLoss(id) {
  return request.put(`/svc/card/loss/${id}/unlock`)
}
