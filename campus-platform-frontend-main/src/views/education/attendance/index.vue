<template>
  <div class="page-container">
    <!-- 教师：发起考勤 + 记录 -->
    <el-row :gutter="16" v-if="userRole === 'teacher'">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header>
            <div class="title-block">
              <span class="card-title">发起考勤</span>
              <small>选择课程和班级后生成签到码</small>
            </div>
          </template>
          <el-form :model="attendanceForm" label-width="100px">
            <el-form-item label="选择课程">
              <el-select v-model="attendanceForm.courseId" placeholder="请选择课程" style="width: 100%;" @change="handleCourseChange">
                <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="考勤班级">
              <el-select v-model="attendanceForm.className" placeholder="请选择班级" style="width: 100%;" :disabled="!attendanceForm.courseId">
                <el-option v-for="cls in classNameList" :key="cls" :label="cls" :value="cls" />
              </el-select>
            </el-form-item>
            <el-form-item label="考勤时长">
              <el-input-number v-model="attendanceForm.duration" :min="5" :max="60" /> 分钟
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleCreateSession" :loading="creating" :disabled="!!currentSession">
                {{ currentSession ? '考勤进行中...' : '发起考勤' }}
              </el-button>
              <el-button v-if="currentSession" type="danger" @click="handleFinishSession">结束考勤</el-button>
            </el-form-item>
          </el-form>

          <div v-if="currentSession" class="session-info">
            <el-divider />
            <h4>当前考勤场次</h4>
            <p>签到码：<span class="session-code">{{ currentSession.sessionCode }}</span></p>
            <p>剩余时间：<strong>{{ formatTime(remainingTime) }}</strong></p>
            <el-progress :percentage="progressPercent" :status="progressStatus" />
            <p style="margin-top: 12px; color: #909399; font-size: 13px;">
              已签到 {{ liveSignedCount }} 人
            </p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-block">
                <span class="card-title">考勤记录</span>
                <small>查看课程考勤场次和签到统计</small>
              </div>
              <div class="header-actions">
                <el-button @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button>
              </div>
            </div>
          </template>
          <el-table :data="attendanceList" v-loading="loading" stripe class="business-table">
            <el-table-column prop="courseName" label="课程名称" min-width="120" />
            <el-table-column prop="className" label="班级" width="120" />
            <el-table-column prop="sessionCode" label="签到码" width="90" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column label="签到情况" width="120">
              <template #default="{ row }">
                <span>{{ row.signedCount }}/{{ row.totalCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="出勤率" width="100">
              <template #default="{ row }">
                <el-progress :percentage="row.totalCount > 0 ? Math.round((row.signedCount / row.totalCount) * 100) : 0" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '进行中' : '已结束' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="96" fixed="right" align="center">
              <template #default="{ row }">
                <el-button class="action-primary" size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理员：只看考勤记录 -->
    <template v-if="userRole === 'admin'">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">考勤记录</span>
              <small>查看全校考勤场次和签到概览</small>
            </div>
            <div class="header-actions">
              <el-button @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </div>
        </template>
        <el-table :data="attendanceList" v-loading="loading" stripe class="business-table">
          <el-table-column prop="courseName" label="课程名称" min-width="120" />
          <el-table-column prop="className" label="班级" width="100" />
          <el-table-column prop="sessionCode" label="签到码" width="90" />
          <el-table-column prop="startTime" label="开始时间" width="170" />
          <el-table-column label="签到情况" width="120">
            <template #default="{ row }"><span>{{ row.signedCount }}/{{ row.totalCount }}</span></template>
          </el-table-column>
          <el-table-column label="出勤率" width="100">
            <template #default="{ row }">
              <el-progress :percentage="row.totalCount > 0 ? Math.round((row.signedCount / row.totalCount) * 100) : 0" :stroke-width="6" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '进行中' : '已结束' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right" align="center">
            <template #default="{ row }">
              <el-button class="action-primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 学生 -->
    <template v-if="userRole === 'student'">
      <el-card shadow="never">
        <template #header>
          <div class="title-block">
            <span class="card-title">学生签到</span>
            <small>输入教师提供的签到码完成签到</small>
          </div>
        </template>
        <div class="student-sign">
          <el-input v-model="signCode" placeholder="请输入签到码" style="width: 240px; margin-right: 12px;" size="large" />
          <el-button type="primary" @click="handleSign" :loading="signing" size="large">立即签到</el-button>
        </div>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header">
            <div class="title-block">
              <span class="card-title">我的考勤记录</span>
              <small>查看个人签到状态和课程考勤情况</small>
            </div>
            <div class="header-actions">
              <el-button @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </div>
        </template>
        <el-table :data="myAttendanceList" v-loading="loading" stripe class="business-table">
          <el-table-column prop="courseName" label="课程名称" min-width="140" />
          <el-table-column prop="className" label="班级" width="100" />
          <el-table-column prop="signTime" label="签到时间" width="170" />
          <el-table-column label="签到状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.recordStatus === 0" type="success">已签到</el-tag>
              <el-tag v-else-if="row.recordStatus === 1" type="danger">缺勤</el-tag>
              <el-tag v-else-if="row.recordStatus === 2" type="warning">请假</el-tag>
              <el-tag v-else type="info">补签</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="考勤状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.sessionStatus === 0 ? 'success' : 'info'">{{ row.sessionStatus === 0 ? '进行中' : '已结束' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="myAttendanceList.length === 0" description="暂无考勤记录" />
      </el-card>
    </template>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="考勤详情" width="800px" top="5vh">
      <el-descriptions :column="2" border style="margin-bottom: 16px;">
        <el-descriptions-item label="课程">{{ detailData.courseName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detailData.className }}</el-descriptions-item>
        <el-descriptions-item label="签到码">{{ detailData.sessionCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.status === 0 ? 'success' : 'info'">{{ detailData.status === 0 ? '进行中' : '已结束' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailData.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailData.endTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16">
        <el-col :span="12">
          <h4 style="color: #67c23a;">已签到 ({{ statsData.signedCount || 0 }})</h4>
          <el-table :data="statsData.signedList || []" size="small" max-height="300" stripe>
            <el-table-column prop="username" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="signTime" label="签到时间" />
          </el-table>
        </el-col>
        <el-col :span="12">
          <h4 style="color: #f56c6c;">未签到 ({{ statsData.unsignedCount || 0 }})</h4>
          <el-table :data="statsData.unsignedList || []" size="small" max-height="300" stripe>
            <el-table-column prop="username" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column label="状态" width="80">
              <template #default>
                <el-tag type="danger" size="small">缺勤</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createAttendanceSession, getAttendanceSessionPage, getAttendanceStats, attendanceSign, finishAttendanceSession, getActiveSession, getMyAttendanceRecords } from '@/api/attendance'
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
const creating = ref(false)
const signing = ref(false)
const courseList = ref([])
const attendanceList = ref([])
const myAttendanceList = ref([])
const currentSession = ref(null)
const remainingTime = ref(0)
const signCode = ref('')
const liveSignedCount = ref(0)
let timer = null

const classNameList = ref([])
const attendanceForm = reactive({ courseId: null, className: null, duration: 30 })

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref({})
const statsData = ref({})

const progressPercent = computed(() => {
  if (!currentSession.value) return 0
  const total = (currentSession.value.durationMinutes || attendanceForm.duration) * 60
  return Math.round((remainingTime.value / total) * 100)
})

const progressStatus = computed(() => {
  if (remainingTime.value < 60) return 'exception'
  if (remainingTime.value < 300) return 'warning'
  return ''
})

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}分${s < 10 ? '0' : ''}${s}秒`
}

const fetchCourseList = async () => {
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 100, status: 0 })
    courseList.value = res.list || []
  } catch (e) { console.error(e) }
}

const handleCourseChange = async (courseId) => {
  attendanceForm.className = null
  classNameList.value = []
  if (!courseId) return
  try {
    const detail = await getCourseById(courseId)
    classNameList.value = detail.classNames || []
  } catch (e) { console.error(e) }
}

const fetchData = async () => {
  loading.value = true
  try {
    if (userRole.value === 'student') {
      const res = await getMyAttendanceRecords({ semester: '' })
      myAttendanceList.value = res || []
    } else {
      const res = await getAttendanceSessionPage({ pageNum: 1, pageSize: 20 })
      attendanceList.value = res.list || []
    }
  } finally { loading.value = false }
}

// 恢复活跃场次倒计时
const restoreActiveSession = async () => {
  try {
    const session = await getActiveSession()
    if (session && session.status === 0) {
      const endTime = new Date(session.endTime).getTime()
      const now = Date.now()
      const remain = Math.max(0, Math.floor((endTime - now) / 1000))
      if (remain > 0) {
        currentSession.value = session
        remainingTime.value = remain
        startTimer()
      } else {
        // 已过期，自动结束
        try { await finishAttendanceSession(session.id) } catch (e) { /* ignore */ }
        currentSession.value = null
        fetchData()
      }
    }
  } catch (e) { console.error(e) }
}

const handleCreateSession = async () => {
  if (!attendanceForm.courseId) { ElMessage.warning('请选择课程'); return }
  if (!attendanceForm.className) { ElMessage.warning('请选择考勤班级'); return }
  creating.value = true
  try {
    const res = await createAttendanceSession({
      courseId: attendanceForm.courseId,
      className: attendanceForm.className,
      durationMinutes: attendanceForm.duration
    })
    currentSession.value = res
    remainingTime.value = attendanceForm.duration * 60
    liveSignedCount.value = 0
    startTimer()
    ElMessage.success('考勤发起成功')
    fetchData()
  } finally { creating.value = false }
}

const handleFinishSession = async () => {
  if (!currentSession.value) return
  try {
    await finishAttendanceSession(currentSession.value.id)
    ElMessage.success('考勤已结束')
    clearInterval(timer)
    currentSession.value = null
    remainingTime.value = 0
    fetchData()
  } catch (e) { console.error(e) }
}

const startTimer = () => {
  if (timer) clearInterval(timer)
  timer = setInterval(async () => {
    if (remainingTime.value > 0) {
      remainingTime.value--
      // 每10秒刷新一次签到人数
      if (remainingTime.value % 10 === 0 && currentSession.value) {
        try {
          const stats = await getAttendanceStats(currentSession.value.id)
          liveSignedCount.value = stats.signedCount || 0
        } catch (e) { /* ignore */ }
      }
    } else {
      clearInterval(timer)
      // 自动调用后端结束考勤
      if (currentSession.value) {
        try {
          await finishAttendanceSession(currentSession.value.id)
        } catch (e) { /* ignore */ }
      }
      currentSession.value = null
      ElMessage.info('考勤时间已到，已自动结束')
      fetchData()
    }
  }, 1000)
}

const handleSign = async () => {
  if (!signCode.value) { ElMessage.warning('请输入签到码'); return }
  signing.value = true
  try {
    await attendanceSign(signCode.value)
    ElMessage.success('签到成功')
    signCode.value = ''
  } finally { signing.value = false }
}

const handleViewDetail = async (row) => {
  detailData.value = row
  detailVisible.value = true
  try {
    const stats = await getAttendanceStats(row.id)
    statsData.value = stats
  } catch (e) {
    console.error(e)
    statsData.value = {}
  }
}

onMounted(() => {
  fetchCourseList()
  fetchData()
  if (userRole.value !== 'student') {
    restoreActiveSession()
  }
})
onUnmounted(() => { if (timer) clearInterval(timer) })
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

.action-primary {
  min-height: 30px;
  padding: 6px 14px !important;
  color: #ffffff !important;
  font-size: 12px;
  background: linear-gradient(135deg, var(--apple-primary, #2563eb), #0ea5e9) !important;
  border: none !important;
  border-radius: 999px !important;
}

.session-info { margin-top: 20px; }
.session-code { font-size: 28px; font-weight: bold; color: #409EFF; letter-spacing: 6px; }
.student-sign { display: flex; align-items: center; padding: 20px; }
h4 { margin: 0 0 12px; }

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-button {
    width: 100%;
  }

  .student-sign {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
    padding: 12px 0;
  }

  .student-sign .el-input,
  .student-sign .el-button {
    width: 100% !important;
    margin-right: 0 !important;
  }
}
</style>
