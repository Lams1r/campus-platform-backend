import request from './request'

// 获取角色列表
export function getRoleList(params) {
  return request.get('/sys/role/list', { params })
}

// 获取角色分页
export function getRolePage(params) {
  return request.get('/sys/role/page', { params })
}

// 获取角色详情
export function getRoleById(id) {
  return request.get(`/sys/role/${id}`)
}

// 新增角色
export function addRole(data) {
  return request.post('/sys/role', data)
}

// 修改角色
export function updateRole(data) {
  return request.put('/sys/role', data)
}

// 删除角色
export function deleteRole(id) {
  return request.delete(`/sys/role/${id}`)
}

// 分配菜单权限
export function assignRoleMenus(roleId, menuIds) {
  return request.post(`/sys/role/${roleId}/menus`, menuIds)
}
