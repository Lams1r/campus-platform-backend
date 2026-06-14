<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-block">
                <span class="card-title">角色列表</span>
                <small>管理系统角色和启停状态</small>
              </div>
              <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增</el-button>
            </div>
          </template>
          <el-table :data="roleList" v-loading="loading" highlight-current-row class="role-table" @current-change="handleRoleSelect">
            <el-table-column prop="roleName" label="角色名称" min-width="120" />
            <el-table-column prop="roleKey" label="角色标识" min-width="120" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <div class="table-actions" @click.stop>
                  <el-button class="action-primary" size="small" @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRoleCommand(command, row)">
                    <el-button class="action-more" size="small">
                      更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="delete" class="danger-item">删除角色</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-block">
                <span class="card-title">菜单权限 {{ selectedRole ? `(${selectedRole.roleName})` : '' }}</span>
                <small>为当前角色分配菜单和按钮权限</small>
              </div>
              <el-button type="primary" :disabled="!selectedRole" @click="handleSavePermission">
                <el-icon><Check /></el-icon>保存权限
              </el-button>
            </div>
          </template>
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            :default-checked-keys="checkedMenus"
            :disabled="!selectedRole"
          />
          <el-empty v-if="!selectedRole" description="请先选择一个角色" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="如：教师" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="formData.roleKey" placeholder="如：teacher" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, getRoleById, addRole, updateRole, deleteRole, assignRoleMenus } from '@/api/role'
import { getMenuTree } from '@/api/menu'

const loading = ref(false)
const roleList = ref([])
const menuTree = ref([])
const selectedRole = ref(null)
const checkedMenus = ref([])
const menuTreeRef = ref(null)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({ id: null, roleName: '', roleKey: '', sortOrder: 0, status: 0, remark: '' })
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try { roleList.value = (await getRoleList()) || [] } finally { loading.value = false }
}

const fetchMenuTree = async () => {
  try { menuTree.value = (await getMenuTree()) || [] } catch (e) { console.error(e) }
}

const handleRoleSelect = async (row) => {
  selectedRole.value = row
  if (!row) { checkedMenus.value = []; return }
  try {
    const detail = await getRoleById(row.id)
    checkedMenus.value = detail.menuIds || []
    nextTick(() => { menuTreeRef.value?.setCheckedKeys(checkedMenus.value) })
  } catch (e) {
    checkedMenus.value = []
    console.error(e)
  }
}

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增角色'
  Object.assign(formData, { id: null, roleName: '', roleKey: '', sortOrder: 0, status: 0, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑角色'
  Object.assign(formData, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, sortOrder: row.sortOrder, status: row.status, remark: row.remark })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (isEdit.value) await updateRole(formData); else await addRole(formData)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch (e) { console.error(e) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除角色 "${row.roleName}" 吗？`, '提示', { type: 'warning' })
    .then(async () => { await deleteRole(row.id); ElMessage.success('删除成功'); fetchData() })
}

const handleRoleCommand = (command, row) => {
  if (command === 'delete') {
    handleDelete(row)
  }
}

const handleSavePermission = async () => {
  if (!selectedRole.value) return
  const checkedKeys = menuTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
  const menuIds = [...checkedKeys, ...halfCheckedKeys]
  try {
    await assignRoleMenus(selectedRole.value.id, menuIds)
    ElMessage.success('权限保存成功')
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchData(); fetchMenuTree() })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--apple-ink, #1d1d1f);
}

.title-block small {
  font-size: 12px;
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.role-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.action-primary,
.action-more {
  min-height: 30px;
  padding: 6px 12px !important;
  font-size: 12px;
  border-radius: 999px !important;
}

.action-primary {
  color: #ffffff !important;
  background: linear-gradient(135deg, var(--apple-primary, #2563eb), #0ea5e9) !important;
  border: none !important;
}

.action-more {
  color: var(--apple-ink, #1d1d1f) !important;
  background: var(--apple-parchment, #f5f5f7) !important;
  border: 1px solid var(--apple-hairline, #e0e0e0) !important;
}

.danger-item {
  color: var(--apple-danger, #ff3b30) !important;
}

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .card-header .el-button {
    width: 100%;
  }
}
</style>
