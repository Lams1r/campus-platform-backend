<template>
  <div class="page-container">
    <!-- 教师：成绩录入 -->
    <template v-if="userRole === 'teacher'">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">成绩管理</span>
              <small>录入课程成绩并自动计算总评</small>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedCourseId" placeholder="选择课程" style="width: 200px;" @change="handleCourseChange">
                <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
              </el-select>
              <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 200px;" @change="fetchStudents">
                <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
                <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
                <el-option label="2024-2025学年第一学期" value="2024-2025-1" />
                <el-option label="2024-2025学年第二学期" value="2024-2025-2" />
              </el-select>
            </div>
          </div>
        </template>

        <div v-if="selectedCourseId && currentCourse" style="margin-bottom: 16px;">
          <el-tag type="info">成绩构成：平时成绩 {{ currentCourse.regularRatio || 30 }}% + 考试成绩 {{ currentCourse.examRatio || 70 }}%</el-tag>
          <el-tag v-if="currentCourse.semester" style="margin-left: 8px;">学期：{{ currentCourse.semester }}</el-tag>
        </div>

        <el-empty v-if="!selectedCourseId" description="请先选择课程" />
        <el-empty v-else-if="studentScores.length === 0" description="暂无学生选课" />

        <el-table v-else :data="studentScores" v-loading="loading" stripe class="business-table">
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="className" label="班级" width="80" />
          <el-table-column label="平时成绩" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.regularScore" :min="0" :max="100" :precision="1" size="small" style="width: 110px;" @change="handleScoreChange(row)" />
            </template>
          </el-table-column>
          <el-table-column label="考试成绩" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.examScore" :min="0" :max="100" :precision="1" size="small" style="width: 110px;" @change="handleScoreChange(row)" />
            </template>
          </el-table-column>
          <el-table-column label="总成绩" width="100">
            <template #default="{ row }">
              <span :style="{ fontWeight: 600, color: getGradeColor(row.totalScore) }">
                {{ row.totalScore != null ? row.totalScore : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="等级" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.gradeLevel === '优秀'" type="success">{{ row.gradeLevel }}</el-tag>
              <el-tag v-else-if="row.gradeLevel === '及格'" type="warning">{{ row.gradeLevel }}</el-tag>
              <el-tag v-else-if="row.gradeLevel === '不及格'" type="danger">{{ row.gradeLevel }}</el-tag>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="info">待审</el-tag>
              <el-tag v-else-if="row.status === 1" type="danger">驳回</el-tag>
              <el-tag v-else-if="row.status === 2" type="success">归档</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right" align="center">
            <template #default="{ row }">
              <el-button class="action-primary" size="small" @click="handleSave(row)" :disabled="row.status === 2">
                {{ row.scoreId ? '更新' : '保存' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 管理员：成绩审核 -->
    <template v-if="userRole === 'admin'">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">成绩审核</span>
              <small>归档或驳回教师提交的成绩</small>
            </div>
            <div class="header-actions">
              <el-button @click="fetchScores"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </div>
        </template>
        <el-table :data="allScores" stripe class="business-table">
          <el-table-column prop="courseName" label="课程名称" min-width="120" />
          <el-table-column prop="courseCode" label="课程代码" width="100" />
          <el-table-column prop="totalScore" label="总成绩" width="80" />
          <el-table-column prop="gradeLevel" label="等级" width="80" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="info">待审</el-tag>
              <el-tag v-else-if="row.status === 1" type="danger">驳回</el-tag>
              <el-tag v-else-if="row.status === 2" type="success">归档</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button class="action-primary" size="small" @click="handleAudit(row, 2)" :disabled="row.status === 2">归档</el-button>
                <el-dropdown trigger="click" @command="(command) => handleAuditCommand(command, row)">
                  <el-button class="action-more" size="small" :disabled="row.status === 2">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="reject" class="danger-item">驳回成绩</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 学生 -->
    <template v-if="userRole === 'student'">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">我的成绩</span>
              <small>查看各学期课程成绩和归档状态</small>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 200px;" @change="fetchMyScores">
                <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
                <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table :data="myScores" v-loading="loading" stripe class="business-table">
          <el-table-column prop="courseName" label="课程名称" />
          <el-table-column prop="regularScore" label="平时成绩" width="100">
            <template #default="{ row }">{{ row.regularScore ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="examScore" label="考试成绩" width="100">
            <template #default="{ row }">{{ row.examScore ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="总成绩" width="100">
            <template #default="{ row }">
              <span :style="{ fontWeight: 600, color: getGradeColor(row.totalScore) }">{{ row.totalScore ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="gradeLevel" label="等级" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.gradeLevel === '优秀'" type="success">{{ row.gradeLevel }}</el-tag>
              <el-tag v-else-if="row.gradeLevel === '及格'" type="warning">{{ row.gradeLevel }}</el-tag>
              <el-tag v-else-if="row.gradeLevel === '不及格'" type="danger">{{ row.gradeLevel }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="info">待审</el-tag>
              <el-tag v-else-if="row.status === 1" type="danger">驳回</el-tag>
              <el-tag v-else-if="row.status === 2" type="success">已归档</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="myScores.length === 0" description="暂无成绩记录" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseStudents, saveScore, auditScore, getScorePage } from '@/api/score'
import { getCoursePage, getCourseById } from '@/api/course'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userRole = computed(() => {
  const t = authStore.userInfo?.userType
  if (t === 2) return 'admin'
  if (t === 1) return 'teacher'
  return 'student'
})

const loading = ref(false)
const courseList = ref([])
const selectedCourseId = ref(null)
const selectedSemester = ref('2025-2026-1')
const currentCourse = ref(null)
const studentScores = ref([])
const allScores = ref([])
const myScores = ref([])

const fetchCourses = async () => {
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 100, status: 0 })
    courseList.value = res.list || []
  } catch (e) { console.error(e) }
}

const handleCourseChange = async (courseId) => {
  if (!courseId) { currentCourse.value = null; studentScores.value = []; return }
  try {
    const detail = await getCourseById(courseId)
    currentCourse.value = detail.course
  } catch (e) { console.error(e) }
  fetchStudents()
}

const fetchStudents = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  try {
    const res = await getCourseStudents(selectedCourseId.value, selectedSemester.value)
    studentScores.value = res || []
  } finally { loading.value = false }
}

const handleScoreChange = (row) => {
  // 自动计算总成绩和等级
  if (row.regularScore != null && row.examScore != null && currentCourse.value) {
    const ratio = currentCourse.value.regularRatio || 30
    const examRatio = currentCourse.value.examRatio || 70
    const total = (row.regularScore * ratio / 100 + row.examScore * examRatio / 100)
    row.totalScore = Math.round(total * 100) / 100
    if (row.totalScore < 60) row.gradeLevel = '不及格'
    else if (row.totalScore <= 89) row.gradeLevel = '及格'
    else row.gradeLevel = '优秀'
  }
}

const handleSave = async (row) => {
  if (row.regularScore == null || row.examScore == null) {
    ElMessage.warning('请填写平时成绩和考试成绩')
    return
  }
  try {
    await saveScore({
      courseId: selectedCourseId.value,
      studentId: row.studentId,
      semester: selectedSemester.value,
      regularScore: row.regularScore,
      examScore: row.examScore
    })
    ElMessage.success('保存成功')
    fetchStudents()
  } catch (e) { console.error(e) }
}

const getGradeColor = (score) => {
  if (score == null) return '#909399'
  if (score < 60) return '#f56c6c'
  if (score <= 89) return '#e6a23c'
  return '#67c23a'
}

// 管理员审核
const fetchScores = async () => {
  try {
    const res = await getScorePage({ pageNum: 1, pageSize: 50 })
    allScores.value = res.list || []
  } catch (e) { console.error(e) }
}

const handleAudit = async (row, status) => {
  const action = status === 2 ? '归档' : '驳回'
  const { value: remark } = await ElMessageBox.prompt(`${action}该成绩？`, '审核', {
    confirmButtonText: action, cancelButtonText: '取消', inputPlaceholder: '审核意见（可选）'
  }).catch(() => ({ value: null }))
  if (remark === null) return
  try {
    await auditScore(row.id, status, remark)
    ElMessage.success(`已${action}`)
    fetchScores()
    fetchStudents()
  } catch (e) { console.error(e) }
}

const handleAuditCommand = (command, row) => {
  if (command === 'reject') {
    handleAudit(row, 1)
  }
}

// 学生查看成绩
const fetchMyScores = async () => {
  loading.value = true
  try {
    const res = await getScorePage({ semester: selectedSemester.value })
    myScores.value = res.list || []
  } finally { loading.value = false }
}

onMounted(() => {
  if (userRole.value === 'student') {
    fetchMyScores()
  } else {
    fetchCourses()
    if (userRole.value === 'admin') fetchScores()
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
  gap: 12px;
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

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions .el-select,
  .header-actions .el-button {
    width: 100% !important;
  }
}
</style>
