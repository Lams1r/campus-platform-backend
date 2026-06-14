<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">课程评价</span>
            <small>对已选课程提交或修改教学评价</small>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 200px;" @change="fetchData">
              <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
              <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="courseList" v-loading="loading" stripe class="business-table">
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column label="任课教师" width="120">
          <template #default="{ row }">{{ row.teacherNames?.join('、') || '-' }}</template>
        </el-table-column>
        <el-table-column label="我的评分" width="150">
          <template #default="{ row }">
            <el-rate v-if="row.myRating" :model-value="row.myRating" disabled show-score />
            <span v-else style="color: #909399;">未评价</span>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.myContent || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button class="action-primary" size="small" @click="handleEvaluate(row)">
              {{ row.myRating ? '修改评价' : '评价' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="courseList.length === 0" description="暂无已选课程" />
    </el-card>

    <!-- 评价对话框 -->
    <el-dialog v-model="dialogVisible" :title="isModify ? '修改评价' : '课程评价'" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="课程">
          <el-input :model-value="formData.courseName" disabled />
        </el-form-item>
        <el-form-item label="评分" prop="starRating">
          <el-rate v-model="formData.starRating" :max="5" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input v-model="formData.content" type="textarea" rows="4" placeholder="请输入您的评价（最多200字）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ isModify ? '保存修改' : '提交评价' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitEvaluation, updateEvaluation, getMyEvaluations } from '@/api/evaluation'
import { getMyCourses } from '@/api/elective'

const loading = ref(false)
const selectedSemester = ref('2025-2026-1')
const courseList = ref([])
const myEvaluations = ref([])

const dialogVisible = ref(false)
const isModify = ref(false)
const formRef = ref(null)
const formData = reactive({ courseId: null, courseName: '', starRating: 5, content: '' })
const formRules = {
  starRating: [{ required: true, message: '请选择评分', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    // 获取已选课程
    const courses = (await getMyCourses({ semester: selectedSemester.value })) || []
    // 获取我的评价
    const evals = (await getMyEvaluations()) || []
    myEvaluations.value = evals

    // 合并：课程 + 评价状态
    courseList.value = courses.map(c => {
      const ev = evals.find(e => e.courseId === c.courseId)
      return {
        ...c,
        myRating: ev ? ev.starRating : null,
        myContent: ev ? ev.content : null,
        evaluationId: ev ? ev.id : null
      }
    })
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleEvaluate = (row) => {
  isModify.value = !!row.myRating
  Object.assign(formData, {
    courseId: row.courseId,
    courseName: row.courseName,
    starRating: row.myRating || 5,
    content: row.myContent || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (isModify.value) {
      await updateEvaluation({ courseId: formData.courseId, starRating: formData.starRating, content: formData.content })
      ElMessage.success('评价修改成功')
    } else {
      await submitEvaluation({ courseId: formData.courseId, starRating: formData.starRating, content: formData.content })
      ElMessage.success('评价提交成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) }
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

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-select {
    width: 100% !important;
  }
}
</style>
