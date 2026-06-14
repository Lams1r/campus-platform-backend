import request from './request'

// 用户分页查询
export function getUserPage(params) {
  return request.get('/sys/user/page', { params })
}

// 获取用户详情
export function getUserById(id) {
  return request.get(`/sys/user/${id}`)
}

// 新增用户
export function addUser(data) {
  return request.post('/sys/user', data)
}

// 修改用户
export function updateUser(data) {
  return request.put('/sys/user', data)
}

// 删除用户
export function deleteUser(id) {
  return request.delete(`/sys/user/${id}`)
}

// 批量删除用户
export function batchDeleteUser(ids) {
  return request.delete('/sys/user/batch', { data: ids })
}

// 导入用户
export function importUser(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/sys/user/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 导出用户
export function exportUser(params) {
  return request.get('/sys/user/export', { 
    params,
    responseType: 'blob'
  })
}

// 重置密码
export function resetPassword(id, newPassword) {
  return request.put(`/sys/user/${id}/resetPwd`, null, { params: { newPassword } })
}

// 获取教师列表（用于课程关联选择）
export function getTeacherList(params) {
  return request.get('/sys/user/teachers', { params })
}
