<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-block">
                <span class="card-title">字典类型</span>
                <small>维护系统通用字典分类</small>
              </div>
              <el-button type="primary" @click="handleAddType"><el-icon><Plus /></el-icon>新增</el-button>
            </div>
          </template>
          <el-table :data="dictTypeList" v-loading="loading" highlight-current-row class="dict-table" @current-change="handleTypeSelect">
            <el-table-column prop="dictName" label="字典名称" min-width="120" />
            <el-table-column prop="dictType" label="字典标识" min-width="150" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <div class="table-actions" @click.stop>
                  <el-button class="action-primary" size="small" @click="handleEditType(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleTypeCommand(command, row)">
                    <el-button class="action-more" size="small">
                      更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="delete" class="danger-item">删除类型</el-dropdown-item>
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
                <span class="card-title">字典数据 {{ selectedType ? `(${selectedType.dictName})` : '' }}</span>
                <small>管理当前字典类型下的键值数据</small>
              </div>
              <el-button type="primary" :disabled="!selectedType" @click="handleAddData"><el-icon><Plus /></el-icon>新增数据</el-button>
            </div>
          </template>
          <el-table :data="dictDataList" v-loading="dataLoading" class="dict-table">
            <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
            <el-table-column prop="dictValue" label="字典值" min-width="120" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button class="action-primary" size="small" @click="handleEditData(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleDataCommand(command, row)">
                    <el-button class="action-more" size="small">
                      更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="delete" class="danger-item">删除数据</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="500px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName"><el-input v-model="typeForm.dictName" placeholder="请输入字典名称" /></el-form-item>
        <el-form-item label="字典标识" prop="dictType"><el-input v-model="typeForm.dictType" placeholder="如：sys_user_gender" :disabled="isEditType" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="typeForm.remark" type="textarea" rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitType">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="500px">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px">
        <el-form-item label="字典标签" prop="dictLabel"><el-input v-model="dataForm.dictLabel" placeholder="如：男" /></el-form-item>
        <el-form-item label="字典值" prop="dictValue"><el-input v-model="dataForm.dictValue" placeholder="如：1" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dataForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="dataForm.remark" type="textarea" rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypeList, addDictType, updateDictType, deleteDictType, getDictDataByType, addDictData, updateDictData, deleteDictData } from '@/api/dict'

const loading = ref(false)
const dataLoading = ref(false)
const dictTypeList = ref([])
const dictDataList = ref([])
const selectedType = ref(null)

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const isEditType = ref(false)
const typeFormRef = ref(null)
const typeForm = reactive({ id: null, dictName: '', dictType: '', remark: '', status: 0 })
const typeRules = { dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }], dictType: [{ required: true, message: '请输入字典标识', trigger: 'blur' }] }

const dataDialogVisible = ref(false)
const dataDialogTitle = ref('')
const isEditData = ref(false)
const dataFormRef = ref(null)
const dataForm = reactive({ id: null, dictType: '', dictLabel: '', dictValue: '', sortOrder: 0, remark: '', status: 0 })
const dataRules = { dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }], dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }] }

const fetchTypeList = async () => {
  loading.value = true
  try { dictTypeList.value = (await getDictTypeList()) || [] } finally { loading.value = false }
}

const handleTypeSelect = async (row) => {
  selectedType.value = row
  if (!row) { dictDataList.value = []; return }
  dataLoading.value = true
  try { dictDataList.value = (await getDictDataByType(row.dictType)) || [] } finally { dataLoading.value = false }
}

const handleAddType = () => {
  isEditType.value = false; typeDialogTitle.value = '新增字典类型'
  Object.assign(typeForm, { id: null, dictName: '', dictType: '', remark: '', status: 0 })
  typeDialogVisible.value = true
}

const handleEditType = (row) => {
  isEditType.value = true; typeDialogTitle.value = '编辑字典类型'
  Object.assign(typeForm, row)
  typeDialogVisible.value = true
}

const handleSubmitType = async () => {
  await typeFormRef.value.validate()
  try {
    if (isEditType.value) await updateDictType(typeForm); else await addDictType(typeForm)
    ElMessage.success('保存成功'); typeDialogVisible.value = false; fetchTypeList()
  } catch (e) { console.error(e) }
}

const handleDeleteType = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.dictName}" 吗？`, '提示', { type: 'warning' })
    .then(async () => { await deleteDictType(row.id); ElMessage.success('删除成功'); fetchTypeList() })
}

const handleTypeCommand = (command, row) => {
  if (command === 'delete') {
    handleDeleteType(row)
  }
}

const handleAddData = () => {
  isEditData.value = false; dataDialogTitle.value = '新增字典数据'
  Object.assign(dataForm, { id: null, dictType: selectedType.value.dictType, dictLabel: '', dictValue: '', sortOrder: 0, remark: '', status: 0 })
  dataDialogVisible.value = true
}

const handleEditData = (row) => {
  isEditData.value = true; dataDialogTitle.value = '编辑字典数据'
  Object.assign(dataForm, row)
  dataDialogVisible.value = true
}

const handleSubmitData = async () => {
  await dataFormRef.value.validate()
  try {
    if (isEditData.value) await updateDictData(dataForm); else await addDictData(dataForm)
    ElMessage.success('保存成功'); dataDialogVisible.value = false; handleTypeSelect(selectedType.value)
  } catch (e) { console.error(e) }
}

const handleDeleteData = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.dictLabel}" 吗？`, '提示', { type: 'warning' })
    .then(async () => { await deleteDictData(row.id); ElMessage.success('删除成功'); handleTypeSelect(selectedType.value) })
}

const handleDataCommand = (command, row) => {
  if (command === 'delete') {
    handleDeleteData(row)
  }
}

onMounted(() => { fetchTypeList() })
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

.dict-table :deep(.el-table__cell) {
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
