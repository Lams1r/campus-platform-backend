<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">公告通知</span>
            <small>发布、维护和查看校园公告信息</small>
          </div>
          <div class="header-actions">
            <el-button v-if="isAdmin" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>发布公告
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.keyword" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe class="business-table">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisherName" label="发布人" width="120" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 150 : 96" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button class="action-primary" size="small" @click="handleView(row)">查看</el-button>
              <el-dropdown v-if="isAdmin" trigger="click" @command="(command) => handleRowCommand(command, row)">
                <el-button class="action-more" size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑公告</el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">删除公告</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50, 100]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">立即发布</el-radio>
            <el-radio :label="0">保存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看公告弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="公告详情" width="600px">
      <h3 style="margin: 0 0 12px;">{{ viewData.title }}</h3>
      <p style="font-size: 12px; color: #909399; margin: 0 0 16px;">
        发布人：{{ viewData.publisherName || '-' }} | 发布时间：{{ viewData.publishTime || '-' }}
      </p>
      <div style="font-size: 15px; line-height: 1.7; white-space: pre-wrap;">{{ viewData.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticePage, addNotice, updateNotice, deleteNotice } from '@/api/notice'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.userInfo?.userType === 2)

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const formData = reactive({ id: null, title: '', content: '', status: 1 })
const formRules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getNoticePage({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = ''); handleSearch() }
const handleAdd = () => { isEdit.value = false; dialogTitle.value = '发布公告'; Object.assign(formData, { id: null, title: '', content: '', status: 1 }); dialogVisible.value = true }
const handleEdit = (row) => { isEdit.value = true; dialogTitle.value = '编辑公告'; Object.assign(formData, row); dialogVisible.value = true }
const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (isEdit.value) await updateNotice(formData); else await addNotice(formData)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch (error) { console.error(error) }
}
const handleDelete = (row) => { ElMessageBox.confirm(`确定要删除公告 "${row.title}" 吗？`, '提示', { type: 'warning' }).then(async () => { await deleteNotice(row.id); ElMessage.success('删除成功'); fetchData() }) }
const viewDialogVisible = ref(false)
const viewData = ref({})
const handleView = (row) => { viewData.value = row; viewDialogVisible.value = true }
const handleRowCommand = (command, row) => {
  if (command === 'edit') {
    handleEdit(row)
    return
  }
  if (command === 'delete') {
    handleDelete(row)
  }
}
const handleSizeChange = (val) => { pagination.pageSize = val; fetchData() }
const handleCurrentChange = (val) => { pagination.pageNum = val; fetchData() }

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

.header-actions {
  display: flex;
  justify-content: flex-end;
}

.search-actions {
  margin-left: auto !important;
}

.business-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.business-table :deep(.el-table__fixed-right) {
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
  border-radius: 999px !important;
  min-height: 30px;
  padding: 6px 12px !important;
  font-size: 12px;
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-button {
    width: 100%;
  }

  .search-actions {
    margin-left: 0 !important;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
