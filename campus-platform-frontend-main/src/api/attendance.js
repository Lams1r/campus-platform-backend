import request from './request'

// 创建考勤场次 测试
export function createAttendanceSession(data) {
  return request.post('/edu/attendance/session/create', data)
}

// 获取当前教师的活跃考勤场次（倒计时恢复）
export function getActiveSession() {
  return request.get('/edu/attendance/session/active')
}

// 获取考勤场次列表
export function getAttendanceSessionPage(params) {
  return request.get('/edu/attendance/session/page', { params })
}

// 获取考勤场次统计
export function getAttendanceStats(sessionId) {
  return request.get(`/edu/attendance/session/${sessionId}/stats`)
}

// 学生签到
export function attendanceSign(sessionCode) {
  return request.post('/edu/attendance/sign', null, { params: { sessionCode } })
}

// 结束考勤场次
export function finishAttendanceSession(sessionId) {
  return request.put(`/edu/attendance/session/${sessionId}/finish`)
}

// 获取我的考勤记录
export function getMyAttendanceRecords(params) {
  return request.get('/edu/attendance/my', { params })
}
