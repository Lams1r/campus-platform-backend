import request from './request'

// 获取课程评价列表（管理员查看某课程的评价）
export function getEvaluationPage(params) {
  return request.get('/edu/evaluation/page', { params })
}

// 学生查看自己的评价
export function getMyEvaluations() {
  return request.get('/edu/evaluation/my')
}

// 提交课程评价（首次）
export function submitEvaluation(data) {
  return request.post('/edu/evaluation', data)
}

// 修改课程评价
export function updateEvaluation(data) {
  return request.put('/edu/evaluation', data)
}

// 删除评价（管理员）
export function deleteEvaluation(id) {
  return request.delete(`/edu/evaluation/${id}`)
}
