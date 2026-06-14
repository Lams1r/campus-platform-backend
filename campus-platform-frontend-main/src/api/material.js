import request from './request'

// 获取课程资料列表
export function getMaterialList(courseId) {
  return request.get('/edu/material/list', { params: { courseId } })
}

// 上传课程资料
export function uploadMaterial(file, courseId) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('courseId', courseId)
  return request.post('/edu/material/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 下载课程资料
export function downloadMaterial(id) {
  return request.get(`/edu/material/download/${id}`, {
    responseType: 'blob'
  })
}

// 删除课程资料
export function deleteMaterial(id) {
  return request.delete(`/edu/material/${id}`)
}
