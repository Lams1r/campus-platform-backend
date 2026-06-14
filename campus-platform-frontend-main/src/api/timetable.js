import request from './request'

// 获取我的课表
export function getMyTimetable(semester) {
  return request.get('/edu/timetable/my', { params: { semester } })
}

// 获取班级课表
export function getClassTimetable(className, semester) {
  return request.get('/edu/timetable/class', { params: { className, semester } })
}

// 课表分页查询
export function getTimetablePage(params) {
  return request.get('/edu/timetable/page', { params })
}

// 新增课表
export function addTimetable(data) {
  return request.post('/edu/timetable', data)
}

// 修改课表
export function updateTimetable(data) {
  return request.put('/edu/timetable', data)
}

// 删除课表
export function deleteTimetable(id) {
  return request.delete(`/edu/timetable/${id}`)
}
