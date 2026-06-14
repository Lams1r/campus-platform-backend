import request from './request'

// 获取菜单树
export function getMenuTree() {
  return request.get('/sys/menu/tree')
}

// 获取菜单列表
export function getMenuList(params) {
  return request.get('/sys/menu/list', { params })
}

// 新增菜单
export function addMenu(data) {
  return request.post('/sys/menu', data)
}

// 修改菜单
export function updateMenu(data) {
  return request.put('/sys/menu', data)
}

// 删除菜单
export function deleteMenu(id) {
  return request.delete(`/sys/menu/${id}`)
}
