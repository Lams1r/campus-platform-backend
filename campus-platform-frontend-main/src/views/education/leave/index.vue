<template>
  <div class="page-container">
    <!-- 学生端 -->
    <template v-if="userRole === 'student'">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">我的请假</span>
              <small>查看请假申请和审批结果</small>
            </div>
            <el-button type="primary" @click="handleApply"><el-icon><Plus /></el-icon>申请请假</el-button>
          </div>
        </template>
        <el-table :data="myLeaveList" v-loading="loading" stripe class="business-table">
          <el-table-column prop="courseName" label="课程" />
          <el-table-column label="请假类型" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.leaveType === 0">事假</el-tag>
              <el-tag v-else-if="row.leaveType === 1" type="warning">病假</el-tag>
              <el-tag v-else type="info">其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
          <el-table-column prop="startTime" label="开始时间" width="170" />
          <el-table-column prop="endTime" label="结束时间" width="170" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning">待审批</el-tag>
              <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approveRemark" label="审批意见" width="150" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="myLeaveList.length === 0" description="暂无请假记录" />
      </el-card>
    </template>

    <!-- 教师/管理员端 -->
    <template v-else>
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">请假审批</span>
              <small>处理学生课程请假申请</small>
            </div>
            <div class="header-actions">
              <el-button @click="fetchLeavePage"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </div>
        </template>
        <el-table :data="leavePageData" v-loading="loading" stripe class="business-table">
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="courseName" label="课程" />
          <el-table-column label="请假类型" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.leaveType === 0">事假</el-tag>
              <el-tag v-else-if="row.leaveType === 1" type="warning">病假</el-tag>
              <el-tag v-else type="info">其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
          <el-table-column prop="startTime" label="开始时间" width="170" />
          <el-table-column prop="endTime" label="结束时间" width="170" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning">待审批</el-tag>
              <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <div v-if="row.status === 0" class="table-actions">
                <el-button class="action-primary" size="small" @click="handleApprove(row, 1)">通过</el-button>
                <el-dropdown trigger="click" @command="(command) => handleApproveCommand(command, row)">
                  <el-button class="action-more" size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="reject" class="danger-item">驳回申请</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <el-button v-else class="action-more" size="small" disabled>已处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 请假申请对话框 -->
    <el-dialog v-model="dialogVisible" title="申请请假" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select v-model="formData.courseId" placeholder="请选择课程" style="width: 100%;">
            <el-option v-for="c in myCourses" :key="c.courseId" :label="c.courseName" :value="c.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="formData.leaveType" placeholder="请选择" style="width: 100%;">
            <el-option label="事假" :value="0" />
            <el-option label="病假" :value="1" />
            <el-option label="其他" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="formData.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="formData.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="formData.reason" type="textarea" rows="3" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitLeave, getMyLeaves, getLeavePage, auditLeave } from '@/api/leave'
import { getMyCourses } from '@/api/elective'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userRole = computed(() => {
  const t = authStore.userInfo?.userType
  if (t === 2) return 'admin'
  if (t === 1) return 'teacher'
  return 'student'
})

const loading = ref(false)
const myLeaveList = ref([])
const leavePageData = ref([])
const myCourses = ref([])

const selectedSemester = ref('2025-2026-1')

// 请假对话框
const dialogVisible = ref(false)
const formRef = ref(null)
const formData = reactive({ courseId: null, leaveType: null, startTime: null, endTime: null, reason: '' })
const formRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
}

// 学生：加载我的请假
const fetchMyLeaves = async () => {
  loading.value = true
  try {
    const res = await getMyLeaves({ semester: selectedSemester.value })
    myLeaveList.value = res || []
  } finally { loading.value = false }
}

// 学生：加载已选课程
const fetchMyCourseList = async () => {
  try {
    const res = await getMyCourses({ semester: selectedSemester.value })
    myCourses.value = res || []
  } catch (e) { console.error(e) }
}

// 教师：加载待审批请假
const fetchLeavePage = async () => {
  loading.value = true
  try {
    const res = await getLeavePage({ pageNum: 1, pageSize: 50 })
    leavePageData.value = res.list || []
  } finally { loading.value = false }
}

const handleApply = async () => {
  Object.assign(formData, { courseId: null, leaveType: null, startTime: null, endTime: null, reason: '' })
  await fetchMyCourseList()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    await submitLeave(formData)
    ElMessage.success('请假申请已提交')
    dialogVisible.value = false
    fetchMyLeaves()
  } catch (e) { console.error(e) }
}

const handleApprove = async (row, status) => {
  const action = status === 1 ? '通过' : '驳回'
  const { value: remark } = await ElMessageBox.prompt(`确定${action}该请假申请？`, '审批', {
    confirmButtonText: action, cancelButtonText: '取消', inputPlaceholder: '审批意见（可选）'
  }).catch(() => ({ value: null }))
  if (remark === null) return
  try {
    await auditLeave(row.id, status, remark)
    ElMessage.success(`已${action}`)
    fetchLeavePage()
  } catch (e) { console.error(e) }
}

const handleApproveCommand = (command, row) => {
  if (command === 'reject') {
    handleApprove(row, 2)
  }
}

onMounted(() => {
  if (userRole.value === 'student') {
    fetchMyLeaves()
  } else {
    fetchLeavePage()
  }
})
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

  .header-actions,
  .card-header .el-button {
    width: 100%;
  }
}
</style>
