import request from './request'

// 发送消息
export function sendMessage(data) {
  return request.post('/sys/message/send', data)
}

// 向课程学生发送消息
export function sendCourseMessage(data) {
  return request.post('/sys/message/send-to-course', data)
}

// 收件箱
export function getInbox() {
  return request.get('/sys/message/inbox')
}

// 发件箱
export function getSent() {
  return request.get('/sys/message/sent')
}

// 标记已读
export function markRead(id) {
  return request.put(`/sys/message/${id}/read`)
}

// 未读消息数
export function getUnreadCount() {
  return request.get('/sys/message/unread-count')
}

// 获取课程学生列表（教师用）
export function getCourseStudentsForMessage(courseId) {
  return request.get(`/sys/message/course-students/${courseId}`)
}

// 获取我的课程教师列表（学生用）
export function getMyTeachers() {
  return request.get('/sys/message/my-teachers')
}

// 教师获取自己的课程（含班级列表）
export function getTeacherCourses() {
  return request.get('/sys/message/my-courses')
}
