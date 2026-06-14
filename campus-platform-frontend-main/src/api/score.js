import request from './request'

// 成绩分页查询
export function getScorePage(params) {
  return request.get('/edu/score/page', { params })
}

// 获取课程的学生成绩列表（教师填写成绩用）
export function getCourseStudents(courseId, semester) {
  return request.get(`/edu/score/course/${courseId}/students`, { params: { semester } })
}

// 录入/更新成绩（平时+考试，自动计算总成绩）
export function saveScore(data) {
  return request.post('/edu/score', data)
}

// 管理员审核成绩
export function auditScore(id, status, remark) {
  return request.put(`/edu/score/${id}/audit`, null, { params: { status, remark } })
}

// 提交成绩申诉
export function submitScoreAppeal(data) {
  return request.post('/edu/score/appeal', data)
}

// 获取申诉列表
export function getScoreAppealPage(params) {
  return request.get('/edu/score/appeal/page', { params })
}

// 处理成绩申诉
export function handleScoreAppeal(id, status, result) {
  return request.put(`/edu/score/appeal/${id}/handle`, null, { params: { status, result } })
}
