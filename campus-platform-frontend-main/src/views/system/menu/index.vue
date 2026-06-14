<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">菜单管理</span>
            <small>维护系统菜单、路由路径和权限标识</small>
          </div>
          <el-button type="primary" @click="handleAdd()"><el-icon><Plus /></el-icon>新增菜单</el-button>
        </div>
      </template>

      <el-table :data="tableData" row-key="id" default-expand-all class="menu-table">
        <el-table-column prop="menuName" label="菜单名称" min-width="180">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
            <span style="margin-left: 8px">{{ row.menuName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 'M'" type="success">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'C'" type="primary">菜单</el-tag>
            <el-tag v-else type="info">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" min-width="180" />
        <el-table-column prop="path" label="路由路径" min-width="140" />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column prop="visible" label="可见" width="70">
          <template #default="{ row }">
            <el-tag :type="row.visible === 0 ? 'success' : 'info'">{{ row.visible === 0 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button class="action-primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-dropdown trigger="click" @command="(command) => handleRowCommand(command, row)">
                <el-button class="action-more" size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="add">新增子级</el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">删除菜单</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="formData.parentId" :data="menuTreeData" :props="{ label: 'menuName', value: 'id' }" placeholder="无则为顶级" clearable check-strictly style="width: 100%;" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="formData.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="formData.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="图标" v-if="formData.menuType !== 'F'">
          <el-input v-model="formData.icon" placeholder="如：HomeFilled" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="formData.menuType !== 'F'">
          <el-input v-model="formData.path" placeholder="如：/system/user" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="formData.menuType === 'C'">
          <el-input v-model="formData.component" placeholder="如：system/user/index" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="formData.menuType !== 'M'">
          <el-input v-model="formData.perms" placeholder="如：sys:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="可见" v-if="formData.menuType !== 'F'">
          <el-radio-group v-model="formData.visible">
            <el-radio :value="0">显示</el-radio>
            <el-radio :value="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenuTree, addMenu, updateMenu, deleteMenu } from '@/api/menu'

const tableData = ref([])
const menuTreeData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null, parentId: 0, menuType: 'M', menuName: '', icon: '', path: '', component: '', perms: '', sortOrder: 0, visible: 0, status: 0
})

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const fetchData = async () => {
  try {
    const res = (await getMenuTree()) || []
    tableData.value = res
    menuTreeData.value = [{ id: 0, menuName: '根目录', children: res }]
  } catch (e) { console.error(e) }
}

const handleAdd = (row = null) => {
  isEdit.value = false; dialogTitle.value = '新增菜单'
  Object.assign(formData, { id: null, parentId: row?.id || 0, menuType: 'M', menuName: '', icon: '', path: '', component: '', perms: '', sortOrder: 0, visible: 0, status: 0 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑菜单'
  Object.assign(formData, { id: row.id, parentId: row.parentId, menuType: row.menuType, menuName: row.menuName, icon: row.icon, path: row.path, component: row.component, perms: row.perms, sortOrder: row.sortOrder, visible: row.visible, status: row.status })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (isEdit.value) await updateMenu(formData); else await addMenu(formData)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch (e) { console.error(e) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除菜单 "${row.menuName}" 吗？`, '提示', { type: 'warning' })
    .then(async () => { await deleteMenu(row.id); ElMessage.success('删除成功'); fetchData() })
}

const handleRowCommand = (command, row) => {
  if (command === 'add') {
    handleAdd(row)
    return
  }
  if (command === 'delete') {
    handleDelete(row)
  }
}

onMounted(() => { fetchData() })
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

.menu-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.menu-table :deep(.el-table__fixed-right) {
  box-shadow: -10px 0 24px rgba(15, 23, 42, 0.04);
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
