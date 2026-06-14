import request from './request'

// 获取字典类型列表
export function getDictTypeList() {
  return request.get('/sys/dict/type/list')
}

// 新增字典类型
export function addDictType(data) {
  return request.post('/sys/dict/type', data)
}

// 修改字典类型
export function updateDictType(data) {
  return request.put('/sys/dict/type', data)
}

// 删除字典类型
export function deleteDictType(id) {
  return request.delete(`/sys/dict/type/${id}`)
}

// 根据字典类型获取数据
export function getDictDataByType(dictType) {
  return request.get(`/sys/dict/data/${dictType}`)
}

// 新增字典数据
export function addDictData(data) {
  return request.post('/sys/dict/data', data)
}

// 修改字典数据
export function updateDictData(data) {
  return request.put('/sys/dict/data', data)
}

// 删除字典数据
export function deleteDictData(id) {
  return request.delete(`/sys/dict/data/${id}`)
}
