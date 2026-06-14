import request from './request'

// 获取楼栋列表
export function getBuildingList() {
  return request.get('/svc/dorm/building/list')
}

// 新增楼栋
export function addBuilding(data) {
  return request.post('/svc/dorm/building', data)
}

// 修改楼栋
export function updateBuilding(data) {
  return request.put('/svc/dorm/building', data)
}

// 删除楼栋
export function deleteBuilding(id) {
  return request.delete(`/svc/dorm/building/${id}`)
}

// 获取房间列表
export function getRoomList(params) {
  return request.get('/svc/dorm/room/list', { params })
}

// 新增房间
export function addRoom(data) {
  return request.post('/svc/dorm/room', data)
}

// 修改房间
export function updateRoom(data) {
  return request.put('/svc/dorm/room', data)
}

// 获取住宿分配列表（管理员）
export function getAllocationList(params) {
  return request.get('/svc/dorm/allocation/list', { params })
}

// 查看我的宿舍分配
export function getMyAllocation() {
  return request.get('/svc/dorm/allocation/my')
}

// 分配宿舍
export function allocateDorm(data) {
  return request.post('/svc/dorm/allocation', data)
}

// 办理退宿
export function deallocateDorm(id) {
  return request.delete(`/svc/dorm/allocation/${id}`)
}

// 提交换宿舍申请
export function submitDormSwap(data) {
  return request.post('/svc/dorm/swap', data)
}

// 我的换宿申请
export function getMyDormSwaps() {
  return request.get('/svc/dorm/swap/my')
}

// 管理员查看换宿申请
export function getDormSwapPage(params) {
  return request.get('/svc/dorm/swap/page', { params })
}

// 管理员审批换宿
export function approveDormSwap(id, status, remark) {
  return request.put(`/svc/dorm/swap/${id}/approve`, null, { params: { status, remark } })
}
