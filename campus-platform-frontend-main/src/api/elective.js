import request from './request'

// 可选课程列表
export function getAvailableCourses(params) {
  return request.get('/edu/elective/available', { params })
}

// 选课（指定加入的班级）
export function selectCourse(courseId, semester, className) {
  return request.post('/edu/elective/select', null, { params: { courseId, semester, className } })
}

// 申请退课
export function requestDrop(data) {
  return request.post('/edu/elective/drop-request', data)
}

// 教师查看待审批退课申请
export function getPendingDropRequests(params) {
  return request.get('/edu/elective/drop-requests/pending', { params })
}

// 教师审批退课
export function approveDropRequest(id, status, remark) {
  return request.put(`/edu/elective/drop-request/${id}/approve`, null, { params: { status, remark } })
}

// 我的已选课程
export function getMyCourses(params) {
  return request.get('/edu/elective/my', { params })
}
