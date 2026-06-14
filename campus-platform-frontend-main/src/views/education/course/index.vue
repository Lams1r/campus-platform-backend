<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">{{ isAdmin ? '课程浏览' : '课程管理' }}</span>
            <small>维护课程基础信息、任课教师和关联班级</small>
          </div>
          <div class="header-actions">
            <el-button v-if="!isAdmin" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增课程
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="课程名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入课程名称/编码" clearable />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="searchForm.semester" placeholder="如 2025-2026-1" clearable />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe class="business-table">
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="hours" label="学时" width="80" />
        <el-table-column label="任课教师" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="name in row.teacherNames" :key="name" size="small" style="margin: 2px;">{{ name }}</el-tag>
            <span v-if="!row.teacherNames || row.teacherNames.length === 0" style="color: #909399;">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseType" label="课程类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.courseType">{{ row.courseType }}</el-tag>
            <span v-else style="color: #909399;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '正常' : '已结课' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 96 : 150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button class="action-primary" size="small" @click="handleView(row)">详情</el-button>
              <el-dropdown v-if="!isAdmin" trigger="click" @command="(command) => handleRowCommand(command, row)">
                <el-button class="action-more" size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑课程</el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">删除课程</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" top="6vh">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="课程代码" prop="courseCode">
          <el-input v-model="formData.courseCode" placeholder="请输入课程代码" />
        </el-form-item>
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="formData.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学分" prop="credit">
              <el-input-number v-model="formData.credit" :min="0.5" :max="10" :step="0.5" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学时" prop="hours">
              <el-input-number v-model="formData.hours" :min="1" :max="200" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程类型">
              <el-select v-model="formData.courseType" placeholder="请选择" style="width: 100%;">
                <el-option label="必修课" value="必修课" />
                <el-option label="选修课" value="选修课" />
                <el-option label="公共课" value="公共课" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期">
              <el-input v-model="formData.semester" placeholder="如 2025-2026-1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="平时成绩占比">
              <el-input-number v-model="formData.regularRatio" :min="0" :max="100" :step="10" :step-strictly="true" style="width: 100%;" @change="handleRegularRatioChange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试成绩占比">
              <el-input-number v-model="formData.examRatio" :min="0" :max="100" :step="10" :step-strictly="true" style="width: 100%;" @change="handleExamRatioChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <p style="color: #909399; font-size: 12px; margin: -8px 0 12px 100px;">两项之和应为100，当前：平时{{ formData.regularRatio }}% + 考试{{ formData.examRatio }}%</p>
        <el-form-item label="任课教师">
          <el-select
            v-model="formData.teacherIds"
            multiple
            filterable
            remote
            :remote-method="searchTeachers"
            :loading="teacherLoading"
            placeholder="搜索并选择任课教师"
            style="width: 100%;"
          >
            <el-option
              v-for="t in teacherOptions"
              :key="t.id"
              :label="t.realName"
              :value="t.id"
            >
              <span>{{ t.realName }}</span>
              <span style="float: right; color: #909399; font-size: 12px;">{{ t.username }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关联班级">
          <div style="width: 100%;">
            <el-tag
              v-for="cls in formData.classNames"
              :key="cls"
              closable
              style="margin: 0 6px 6px 0;"
              @close="removeClass(cls)"
            >{{ cls }}</el-tag>
            <el-input
              v-if="classInputVisible"
              ref="classInputRef"
              v-model="classInputValue"
              size="small"
              style="width: 160px;"
              @keyup.enter="addClass"
              @blur="addClass"
            />
            <el-button v-else size="small" @click="showClassInput">+ 添加班级</el-button>
          </div>
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="formData.description" type="textarea" rows="3" placeholder="请输入课程描述" />
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCoursePage, addCourse, updateCourse, deleteCourse, getCourseById } from '@/api/course'
import { getTeacherList } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.userInfo?.userType === 2)

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ keyword: '', semester: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// 教师选择
const teacherLoading = ref(false)
const teacherOptions = ref([])

// 班级输入
const classInputVisible = ref(false)
const classInputValue = ref('')
const classInputRef = ref(null)

// 表单
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, courseCode: '', courseName: '', credit: 2, hours: 32,
  courseType: '', semester: '', regularRatio: 30, examRatio: 70,
  description: '', teacherIds: [], classNames: []
})
const formRules = {
  courseCode: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

// 详情
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCoursePage({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = ''); handleSearch() }

const loadAllTeachers = async () => {
  try {
    const res = await getTeacherList({})
    teacherOptions.value = res || []
  } catch (e) { console.error('获取教师列表失败', e) }
}

const searchTeachers = async (query) => {
  if (!query) { loadAllTeachers(); return }
  teacherLoading.value = true
  try {
    const res = await getTeacherList({ keyword: query })
    teacherOptions.value = res || []
  } finally { teacherLoading.value = false }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增课程'
  Object.assign(formData, {
    id: null, courseCode: '', courseName: '', credit: 2, hours: 32,
    courseType: '', semester: '', regularRatio: 30, examRatio: 70,
    description: '', teacherIds: [], classNames: []
  })
  loadAllTeachers()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑课程'
  await loadAllTeachers()
  try {
    const detail = await getCourseById(row.id)
    Object.assign(formData, {
      id: detail.course.id,
      courseCode: detail.course.courseCode,
      courseName: detail.course.courseName,
      credit: detail.course.credit,
      hours: detail.course.hours,
      courseType: detail.course.courseType || '',
      semester: detail.course.semester || '',
      description: detail.course.description || '',
      teacherIds: detail.teacherIds || [],
      classNames: detail.classNames || []
    })
  } catch (e) {
    Object.assign(formData, { ...row, teacherIds: row.teacherIds || [], classNames: row.classNames || [] })
  }
  dialogVisible.value = true
}

const handleView = (row) => {
  router.push(`/course-detail/${row.id}`)
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    const payload = { ...formData }
    if (isEdit.value) await updateCourse(payload)
    else await addCourse(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) { console.error(error) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除课程 "${row.courseName}" 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCourse(row.id)
      ElMessage.success('删除成功')
      fetchData()
    })
}

const handleRowCommand = (command, row) => {
  if (command === 'edit') {
    handleEdit(row)
    return
  }
  if (command === 'delete') {
    handleDelete(row)
  }
}

const removeClass = (cls) => {
  formData.classNames = formData.classNames.filter(c => c !== cls)
}

const showClassInput = () => {
  classInputVisible.value = true
  nextTick(() => classInputRef.value?.focus())
}

const addClass = () => {
  const val = classInputValue.value.trim()
  if (val && !formData.classNames.includes(val)) {
    formData.classNames.push(val)
  }
  classInputVisible.value = false
  classInputValue.value = ''
}

const handleSizeChange = (val) => { pagination.pageSize = val; fetchData() }
const handleCurrentChange = (val) => { pagination.pageNum = val; fetchData() }

const handleRegularRatioChange = (val) => {
  formData.examRatio = Math.max(0, 100 - val)
}
const handleExamRatioChange = (val) => {
  formData.regularRatio = Math.max(0, 100 - val)
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

.header-actions {
  display: flex;
  gap: 10px;
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
