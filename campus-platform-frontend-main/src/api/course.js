import request from './request'

// 课程分页查询
export function getCoursePage(params) {
  return request.get('/edu/course/page', { params })
}

// 获取课程详情
export function getCourseById(id) {
  return request.get(`/edu/course/${id}`)
}

// 新增课程
export function addCourse(data) {
  return request.post('/edu/course', data)
}

// 修改课程
export function updateCourse(data) {
  return request.put('/edu/course', data)
}

// 删除课程
export function deleteCourse(id) {
  return request.delete(`/edu/course/${id}`)
}

// 课程结课
export function finishCourse(id) {
  return request.put(`/edu/course/${id}/finish`)
}
