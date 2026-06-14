<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">课表管理</span>
            <small>查看课程安排、排课和退课审批</small>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 200px;" @change="handleSemesterChange">
              <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
              <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
              <el-option label="2024-2025学年第一学期" value="2024-2025-1" />
              <el-option label="2024-2025学年第二学期" value="2024-2025-2" />
            </el-select>
            <el-button v-if="userRole !== 'student'" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>排课
            </el-button>
          </div>
        </div>
      </template>

      <!-- 学生：Tab 切换 -->
      <el-tabs v-if="userRole === 'student'" v-model="studentTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的课表" name="myTimetable">
          <div class="timetable-grid">
            <table class="week-table">
              <thead>
                <tr>
                  <th class="time-col">节次</th>
                  <th v-for="day in weekDays" :key="day.value">{{ day.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="section in sectionRows" :key="section.label">
                  <td class="time-col">{{ section.label }}</td>
                  <td v-for="day in weekDays" :key="day.value" class="cell">
                    <div v-for="item in getCellItems(day.value, section.start, section.end)" :key="item.id" class="course-block">
                      <div class="course-name">{{ item.courseName }}</div>
                      <div class="course-meta">{{ item.classroom }}</div>
                      <div class="course-meta">第{{ item.startWeek }}-{{ item.endWeek }}周</div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <el-divider />
          <h4 style="margin: 0 0 12px;">我的选课</h4>
          <el-table :data="mySelectedCourses" stripe class="business-table">
            <el-table-column prop="courseName" label="课程名称" />
            <el-table-column prop="courseCode" label="课程代码" width="120" />
            <el-table-column prop="className" label="班级" width="80" />
            <el-table-column prop="credit" label="学分" width="70" />
            <el-table-column label="课表时间" min-width="180">
              <template #default="{ row }">
                <el-tag v-for="tt in row.timetables" :key="tt.id" size="small" style="margin: 2px;">
                  周{{ tt.dayOfWeek }} 第{{ tt.startSection }}-{{ tt.endSection }}节
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-if="!row.hasDropRequest" class="action-danger" size="small" @click="handleDropRequest(row)">申请退课</el-button>
                <el-tag v-else type="warning" size="small">退课审批中</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="可选课程" name="available">
          <el-empty v-if="availableCourses.length === 0" description="暂无可选课程（教师尚未排课）" />
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :lg="8" v-for="course in availableCourses" :key="course.courseId" style="margin-bottom: 16px;">
              <el-card shadow="hover">
                <h4 style="margin: 0 0 8px;">{{ course.courseName }}</h4>
                <p style="color: #909399; font-size: 13px; margin: 0 0 8px;">
                  {{ course.courseCode }} | {{ course.credit }}学分 | {{ course.courseType || '-' }}
                </p>
                <p style="font-size: 13px; margin: 0 0 8px;">
                  教师：{{ course.teacherNames?.join('、') || '未分配' }}
                </p>
                <div v-for="cls in course.classOptions" :key="cls.className" style="margin-bottom: 8px; padding: 8px; background: #f5f7fa; border-radius: 4px;">
                  <div style="font-size: 13px; font-weight: 600; margin-bottom: 4px;">{{ cls.className }}班</div>
                  <el-tag v-for="tt in cls.timetables" :key="tt.id" size="small" style="margin: 2px;">
                    周{{ tt.dayOfWeek }} 第{{ tt.startSection }}-{{ tt.endSection }}节 {{ tt.classroom }}
                  </el-tag>
                  <div style="text-align: right; margin-top: 6px;">
                    <el-button type="primary" size="small" @click="handleSelect(course, cls.className)">加入此班</el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>

      <!-- 教师/管理员 -->
      <template v-else>
        <div class="timetable-grid">
          <table class="week-table">
            <thead>
              <tr>
                <th class="time-col">节次</th>
                <th v-for="day in weekDays" :key="day.value">{{ day.label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="section in sectionRows" :key="section.label">
                <td class="time-col">{{ section.label }}</td>
                <td v-for="day in weekDays" :key="day.value" class="cell">
                  <div v-for="item in getCellItems(day.value, section.start, section.end)" :key="item.id" class="course-block">
                    <div class="course-name">{{ item.courseName }}</div>
                    <div class="course-meta">{{ item.className }}</div>
                    <div class="course-meta">{{ item.classroom }}</div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <el-divider />

        <el-table :data="timetableList" v-loading="loading" stripe class="business-table">
          <el-table-column prop="courseName" label="课程名称" min-width="120" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="teacherName" label="教师" width="100" />
          <el-table-column label="星期" width="70">
            <template #default="{ row }">{{ weekDays.find(d => d.value === row.dayOfWeek)?.label }}</template>
          </el-table-column>
          <el-table-column label="节次" width="110">
            <template #default="{ row }">第{{ row.startSection }}-{{ row.endSection }}节</template>
          </el-table-column>
          <el-table-column label="周范围" width="110">
            <template #default="{ row }">第{{ row.startWeek }}-{{ row.endWeek }}周</template>
          </el-table-column>
          <el-table-column prop="classroom" label="教室" width="120" />
          <el-table-column prop="semester" label="学期" width="130" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button class="action-primary" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-dropdown trigger="click" @command="(command) => handleTimetableCommand(command, row)">
                  <el-button class="action-more" size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="delete" class="danger-item">删除排课</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 退课申请管理 -->
        <el-divider />
        <div class="card-header" style="margin-bottom: 12px;">
          <span style="font-weight: 600;">退课申请管理</span>
          <el-button @click="fetchDropRequests"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
        <el-table :data="dropRequests" stripe class="business-table">
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="className" label="班级" width="80" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="reason" label="退课原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button class="action-primary" size="small" @click="handleApprove(row, 1)">同意</el-button>
                <el-dropdown trigger="click" @command="(command) => handleDropCommand(command, row)">
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
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="dropRequests.length === 0" description="暂无待审批的退课申请" :image-size="80" />
      </template>
    </el-card>

    <!-- 排课对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select v-model="formData.courseId" placeholder="请选择课程" style="width: 100%;" @change="handleFormCourseChange">
            <el-option v-for="c in myCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择班级" prop="className">
          <el-select v-model="formData.className" placeholder="请选择班级" style="width: 100%;" :disabled="!formData.courseId">
            <el-option v-for="cls in formClassList" :key="cls" :label="cls" :value="cls" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semester">
          <el-select v-model="formData.semester" placeholder="选择学期" style="width: 100%;">
            <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
            <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
            <el-option label="2024-2025学年第一学期" value="2024-2025-1" />
            <el-option label="2024-2025学年第二学期" value="2024-2025-2" />
          </el-select>
        </el-form-item>
        <el-form-item label="星期" prop="dayOfWeek">
          <el-select v-model="formData.dayOfWeek" style="width: 100%;">
            <el-option v-for="d in weekDays" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="上课节次" prop="startSection">
          <el-select v-model="sectionPair" placeholder="选择节次" style="width: 100%;">
            <el-option v-for="s in sectionPairs" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始周" prop="startWeek">
              <el-input-number v-model="formData.startWeek" :min="1" :max="18" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束周" prop="endWeek">
              <el-input-number v-model="formData.endWeek" :min="1" :max="18" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="教室" prop="classroom">
          <el-input v-model="formData.classroom" placeholder="请输入教室" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 退课申请对话框 -->
    <el-dialog v-model="dropDialogVisible" title="申请退课" width="450px">
      <p style="margin: 0 0 12px;">课程：<strong>{{ dropTarget.courseName }}</strong>（{{ dropTarget.className }}班）</p>
      <el-form label-width="80px">
        <el-form-item label="退课原因">
          <el-input v-model="dropReason" type="textarea" rows="4" placeholder="请输入退课原因，提交后需等待教师审批" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dropDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleSubmitDrop">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyTimetable, addTimetable, updateTimetable, deleteTimetable } from '@/api/timetable'
import { getAvailableCourses, selectCourse, getMyCourses, requestDrop, getPendingDropRequests, approveDropRequest } from '@/api/elective'
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
const selectedSemester = ref('2025-2026-1')
const timetableList = ref([])
const studentTab = ref('myTimetable')
const availableCourses = ref([])
const mySelectedCourses = ref([])
const dropRequests = ref([])
const dropDialogVisible = ref(false)
const dropTarget = ref({})
const dropReason = ref('')

const weekDays = [
  { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
  { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }
]

const sectionRows = [
  { label: '第1-2节', start: 1, end: 2 },
  { label: '第3-4节', start: 3, end: 4 },
  { label: '第5-6节', start: 5, end: 6 },
  { label: '第7-8节', start: 7, end: 8 },
  { label: '第9-10节', start: 9, end: 10 },
  { label: '第11-12节', start: 11, end: 12 }
]

const sectionPairs = [
  { label: '第1-2节', value: '1-2', start: 1, end: 2 },
  { label: '第3-4节', value: '3-4', start: 3, end: 4 },
  { label: '第5-6节', value: '5-6', start: 5, end: 6 },
  { label: '第7-8节', value: '7-8', start: 7, end: 8 },
  { label: '第9-10节', value: '9-10', start: 9, end: 10 },
  { label: '第11-12节', value: '11-12', start: 11, end: 12 }
]

const sectionPair = ref('1-2')

const getCellItems = (dayOfWeek, start, end) => {
  return timetableList.value.filter(item =>
    item.dayOfWeek === dayOfWeek && item.startSection <= end && item.endSection >= start
  )
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const myCourses = ref([])
const formClassList = ref([])
const formData = reactive({
  id: null, courseId: null, className: null, semester: '2025-2026-1',
  dayOfWeek: 1, startSection: 1, endSection: 2, startWeek: 1, endWeek: 18, classroom: ''
})
const formRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  className: [{ required: true, message: '请选择班级', trigger: 'change' }],
  semester: [{ required: true, message: '请选择学期', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  startSection: [{ required: true, message: '请选择节次', trigger: 'change' }],
  classroom: [{ required: true, message: '请输入教室', trigger: 'blur' }]
}

const fetchTimetable = async () => {
  loading.value = true
  try {
    const res = await getMyTimetable(selectedSemester.value)
    timetableList.value = res || []
  } finally { loading.value = false }
}

const fetchMySelectedCourses = async () => {
  try {
    const res = await getMyCourses({ semester: selectedSemester.value })
    mySelectedCourses.value = res || []
  } catch (e) { console.error(e) }
}

const fetchDropRequests = async () => {
  try {
    const res = await getPendingDropRequests({ semester: selectedSemester.value })
    dropRequests.value = res || []
  } catch (e) { console.error(e) }
}

const handleDropRequest = (row) => {
  dropTarget.value = row
  dropReason.value = ''
  dropDialogVisible.value = true
}

const handleSubmitDrop = async () => {
  if (!dropReason.value.trim()) { ElMessage.warning('请输入退课原因'); return }
  try {
    await requestDrop({ studentCourseId: dropTarget.value.selectionId, reason: dropReason.value })
    ElMessage.success('退课申请已提交，请等待教师审批')
    dropDialogVisible.value = false
    fetchMySelectedCourses()
  } catch (e) { console.error(e) }
}

const handleApprove = async (row, status) => {
  const action = status === 1 ? '同意' : '驳回'
  const { value: remark } = await ElMessageBox.prompt(`请输入${action}意见（可选）`, `${action}退课申请`, {
    confirmButtonText: action, cancelButtonText: '取消', inputPlaceholder: '审批意见（可选）'
  }).catch(() => ({ value: null }))
  if (remark === null) return
  try {
    await approveDropRequest(row.id, status, remark)
    ElMessage.success(`已${action}`)
    fetchDropRequests()
  } catch (e) { console.error(e) }
}

const handleDropCommand = (command, row) => {
  if (command === 'reject') {
    handleApprove(row, 2)
  }
}

const fetchAvailableCourses = async () => {
  try {
    const res = await getAvailableCourses({ semester: selectedSemester.value })
    availableCourses.value = res || []
  } catch (e) { console.error(e) }
}

const handleSemesterChange = () => {
  if (userRole.value === 'student') {
    if (studentTab.value === 'available') fetchAvailableCourses()
    else { fetchTimetable(); fetchMySelectedCourses() }
  } else {
    fetchTimetable(); fetchDropRequests()
  }
}

const handleTabChange = (tab) => {
  if (tab === 'myTimetable') { fetchTimetable(); fetchMySelectedCourses() }
  else fetchAvailableCourses()
}

const handleSelect = async (course, className) => {
  try {
    await selectCourse(course.courseId, selectedSemester.value, className)
    ElMessage.success('选课成功')
    fetchAvailableCourses()
  } catch (e) { console.error(e) }
}

const handleAdd = async () => {
  isEdit.value = false; dialogTitle.value = '新增排课'
  Object.assign(formData, { id: null, courseId: null, className: null, semester: selectedSemester.value, dayOfWeek: 1, startSection: 1, endSection: 2, startWeek: 1, endWeek: 18, classroom: '' })
  sectionPair.value = '1-2'
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 100, status: 0 })
    myCourses.value = res.list || []
  } catch (e) { console.error(e) }
  formClassList.value = []
  dialogVisible.value = true
}

const handleFormCourseChange = async (courseId) => {
  formData.className = null; formClassList.value = []
  if (!courseId) return
  try {
    const detail = await getCourseById(courseId)
    formClassList.value = detail.classNames || []
  } catch (e) { console.error(e) }
}


const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑排课'
  Object.assign(formData, row)
  sectionPair.value = `${row.startSection}-${row.endSection}`
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  // 从 sectionPair 解析 startSection/endSection
  const pair = sectionPairs.find(s => s.value === sectionPair.value)
  if (pair) {
    formData.startSection = pair.start
    formData.endSection = pair.end
  }
  try {
    if (isEdit.value) await updateTimetable(formData); else await addTimetable(formData)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchTimetable()
  } catch (e) { console.error(e) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该排课吗？', '提示', { type: 'warning' })
    .then(async () => { await deleteTimetable(row.id); ElMessage.success('删除成功'); fetchTimetable() })
}

const handleTimetableCommand = (command, row) => {
  if (command === 'delete') {
    handleDelete(row)
  }
}

onMounted(() => {
  fetchTimetable()
  if (userRole.value === 'student') {
    fetchMySelectedCourses()
  } else {
    fetchDropRequests()
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
  align-items: center;
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
.action-danger,
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

.action-danger {
  color: #ffffff !important;
  background: linear-gradient(135deg, var(--apple-danger, #ff3b30), #ff7a59) !important;
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

.timetable-grid { overflow-x: auto; margin-bottom: 16px; }
.week-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.week-table th, .week-table td { border: 1px solid #ebeef5; padding: 8px; text-align: center; vertical-align: top; }
.week-table th { background: #f5f7fa; font-weight: 600; font-size: 14px; height: 40px; }
.time-col { width: 100px; background: #f5f7fa; font-weight: 500; font-size: 13px; }
.cell { min-height: 60px; height: 60px; }
.course-block { background: #ecf5ff; border-radius: 4px; padding: 4px 6px; margin-bottom: 2px; border-left: 3px solid #409EFF; }
.course-name { font-size: 12px; font-weight: 600; color: #409EFF; }
.course-meta { font-size: 11px; color: #909399; }

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
