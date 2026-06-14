<template>
  <div class="page-container">
    <!-- 顶部返回 -->
    <div style="margin-bottom: 16px;">
      <el-button @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回课程列表</el-button>
    </div>

    <!-- 课程基本信息 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <template #header><span style="font-weight: 600;">课程信息</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="课程名称">{{ course.courseName }}</el-descriptions-item>
        <el-descriptions-item label="课程代码">{{ course.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="课程类型">{{ course.courseType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ course.credit }}</el-descriptions-item>
        <el-descriptions-item label="学时">{{ course.hours }}</el-descriptions-item>
        <el-descriptions-item label="学期">{{ course.semester || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成绩构成">
          平时 {{ course.regularRatio || 30 }}% + 考试 {{ course.examRatio || 70 }}%
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="course.status === 0 ? 'success' : 'info'">{{ course.status === 0 ? '正常' : '已结课' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="课程描述">{{ course.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2);">
          <div class="stat-value">{{ stats.totalStudents }}</div>
          <div class="stat-label">选课学生</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #11998e, #38ef7d);">
          <div class="stat-value">{{ stats.avgScore }}</div>
          <div class="stat-label">平均成绩</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe);">
          <div class="stat-value">{{ stats.passRate }}%</div>
          <div class="stat-label">及格率</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #fa709a, #fee140);">
          <div class="stat-value">{{ stats.excellentRate }}%</div>
          <div class="stat-label">优秀率</div>
        </div>
      </el-col>
    </el-row>

    <!-- 成绩分布图 + 学生列表 -->
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span style="font-weight: 600;">成绩分布</span></template>
          <div ref="gradeChartRef" style="height: 280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span style="font-weight: 600;">学生名单（{{ studentScores.length }}人）</span></template>
          <el-table :data="studentScores" stripe max-height="280">
            <el-table-column prop="studentNo" label="学号" width="110" />
            <el-table-column prop="studentName" label="姓名" width="90" />
            <el-table-column prop="className" label="班级" width="80" />
            <el-table-column label="平时成绩" width="90">
              <template #default="{ row }">{{ row.regularScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="考试成绩" width="90">
              <template #default="{ row }">{{ row.examScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="总成绩" width="80">
              <template #default="{ row }">
                <span :style="{ fontWeight: 600, color: getGradeColor(row.totalScore) }">{{ row.totalScore ?? '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.gradeLevel === '优秀'" type="success" size="small">优秀</el-tag>
                <el-tag v-else-if="row.gradeLevel === '及格'" type="warning" size="small">及格</el-tag>
                <el-tag v-else-if="row.gradeLevel === '不及格'" type="danger" size="small">不及格</el-tag>
                <span v-else style="color: #909399;">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 课程评价 -->
    <el-card shadow="never">
      <template #header><span style="font-weight: 600;">学生评价（{{ evaluations.length }}条）</span></template>
      <el-empty v-if="evaluations.length === 0" description="暂无评价" />
      <div v-for="ev in evaluations" :key="ev.id" class="eval-item">
        <div class="eval-header">
          <el-rate :model-value="ev.starRating" disabled />
          <span class="eval-time">{{ ev.createTime }}</span>
        </div>
        <div class="eval-content">{{ ev.content || '（仅评分，无文字评价）' }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { animate, stagger } from 'animejs'
import { getCourseById } from '@/api/course'
import { getCourseStudents } from '@/api/score'
import { getEvaluationPage } from '@/api/evaluation'

const route = useRoute()
const courseId = route.params.id

const course = ref({})
const studentScores = ref([])
const evaluations = ref([])
const gradeChartRef = ref(null)
let chartInstance = null

const stats = reactive({
  totalStudents: 0,
  avgScore: 0,
  passRate: 0,
  excellentRate: 0
})

const getGradeColor = (score) => {
  if (score == null) return '#909399'
  if (score < 60) return '#f56c6c'
  if (score <= 89) return '#e6a23c'
  return '#67c23a'
}

const fetchCourse = async () => {
  try {
    const res = await getCourseById(courseId)
    course.value = res.course || {}
  } catch (e) { console.error(e) }
}

const fetchStudents = async () => {
  try {
    const res = await getCourseStudents(courseId, '')
    studentScores.value = res || []
    calcStats()
    renderChart()
  } catch (e) { console.error(e) }
}

const fetchEvaluations = async () => {
  try {
    const res = await getEvaluationPage({ courseId, pageNum: 1, pageSize: 50 })
    evaluations.value = res.list || []
  } catch (e) { console.error(e) }
}

const calcStats = () => {
  const list = studentScores.value
  const totalStudents = list.length
  const withScore = list.filter(s => s.totalScore != null)
  if (withScore.length === 0) { stats.totalStudents = totalStudents; return }

  const sum = withScore.reduce((a, s) => a + s.totalScore, 0)
  const avgScore = Math.round(sum / withScore.length * 10) / 10
  const passCount = withScore.filter(s => s.totalScore >= 60).length
  const passRate = Math.round(passCount / withScore.length * 100)
  const excellentCount = withScore.filter(s => s.totalScore >= 90).length
  const excellentRate = Math.round(excellentCount / withScore.length * 100)

  // 数字滚动动画
  const animTarget = { students: 0, avg: 0, pass: 0, excellent: 0 }
  animate(animTarget, {
    students: totalStudents,
    avg: avgScore,
    pass: passRate,
    excellent: excellentRate,
    duration: 1500,
    easing: 'easeOutExpo',
    onUpdate: () => {
      stats.totalStudents = Math.round(animTarget.students)
      stats.avgScore = Math.round(animTarget.avg * 10) / 10
      stats.passRate = Math.round(animTarget.pass)
      stats.excellentRate = Math.round(animTarget.excellent)
    }
  })
}

const renderChart = () => {
  if (!gradeChartRef.value) return
  chartInstance = echarts.init(gradeChartRef.value)
  const withScore = studentScores.value.filter(s => s.totalScore != null)
  const ranges = ['<60', '60-69', '70-79', '80-89', '90-100']
  const counts = [0, 0, 0, 0, 0]
  withScore.forEach(s => {
    const score = s.totalScore
    if (score < 60) counts[0]++
    else if (score < 70) counts[1]++
    else if (score < 80) counts[2]++
    else if (score < 90) counts[3]++
    else counts[4]++
  })
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ranges },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: counts,
      itemStyle: {
        color: (params) => {
          const colors = ['#f56c6c', '#e6a23c', '#409EFF', '#67c23a', '#34c759']
          return colors[params.dataIndex]
        }
      },
      barWidth: '50%'
    }]
  })
}

const handleResize = () => chartInstance?.resize()

onMounted(() => {
  fetchCourse()
  fetchStudents()
  fetchEvaluations()
  window.addEventListener('resize', handleResize)

  // 卡片入场动画
  animate('.stat-card', {
    translateY: [24, 0],
    opacity: [0, 1],
    delay: stagger(120),
    duration: 600,
    easing: 'easeOutCubic'
  })
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.stat-card { padding: 24px; border-radius: 12px; color: #fff; text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 14px; opacity: 0.9; margin-top: 4px; }
.eval-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.eval-item:last-child { border-bottom: none; }
.eval-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.eval-time { font-size: 12px; color: #909399; }
.eval-content { font-size: 14px; color: #303133; }
</style>
