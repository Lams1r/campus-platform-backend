<template>
  <div class="mail-container">
    <!-- 左侧文件夹 -->
    <div class="mail-sidebar">
      <el-button type="primary" style="width: 100%; margin-bottom: 16px;" @click="openCompose">
        <el-icon><Edit /></el-icon>写消息
      </el-button>
      <div class="folder-item" :class="{ active: folder === 'inbox' }" @click="switchFolder('inbox')">
        <el-icon><Message /></el-icon>
        <span>收件箱</span>
        <el-badge v-if="unreadCount > 0" :value="unreadCount" class="folder-badge" />
      </div>
      <div class="folder-item" :class="{ active: folder === 'sent' }" @click="switchFolder('sent')">
        <el-icon><Promotion /></el-icon>
        <span>已发送</span>
      </div>
    </div>

    <!-- 中间消息列表 -->
    <div class="mail-list">
      <div class="mail-list-header">
        <span>{{ folder === 'inbox' ? '收件箱' : '已发送' }}</span>
        <el-button text @click="refresh"><el-icon><Refresh /></el-icon></el-button>
      </div>
      <div v-if="loading" class="mail-loading">加载中...</div>
      <div v-else-if="messages.length === 0" class="mail-empty">暂无消息</div>
      <div v-else class="mail-items">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="mail-item"
          :class="{ unread: msg.isRead === 0 && folder === 'inbox', selected: selectedMsg?.id === msg.id }"
          @click="selectMessage(msg)"
        >
          <div class="mail-item-avatar">
            <el-avatar :size="36">{{ (folder === 'inbox' ? msg.senderName : msg.receiverName)?.[0] || '?' }}</el-avatar>
          </div>
          <div class="mail-item-body">
            <div class="mail-item-top">
              <span class="mail-item-name">{{ folder === 'inbox' ? msg.senderName : msg.receiverName }}</span>
              <span class="mail-item-time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <div class="mail-item-title">{{ msg.title || '(无标题)' }}</div>
            <div class="mail-item-preview">{{ msg.content }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧消息详情 -->
    <div class="mail-detail">
      <div v-if="!selectedMsg" class="mail-empty">选择一条消息查看</div>
      <template v-else>
        <div class="mail-detail-header">
          <h2 class="mail-detail-title">{{ selectedMsg.title || '(无标题)' }}</h2>
          <div class="mail-detail-meta">
            <div class="mail-detail-sender">
              <el-avatar :size="40">{{ (folder === 'inbox' ? selectedMsg.senderName : selectedMsg.receiverName)?.[0] || '?' }}</el-avatar>
              <div>
                <div class="sender-name">{{ folder === 'inbox' ? selectedMsg.senderName : selectedMsg.receiverName }}</div>
                <div class="sender-time">{{ selectedMsg.createTime }}</div>
              </div>
            </div>
            <div v-if="folder === 'inbox'" class="mail-detail-to">
              收件人：我
            </div>
            <div v-else class="mail-detail-to">
              收件人：{{ selectedMsg.receiverName }}
            </div>
          </div>
        </div>
        <el-divider />
        <div class="mail-detail-content">{{ selectedMsg.content }}</div>
      </template>
    </div>

    <!-- 写消息对话框 -->
    <el-dialog v-model="composeVisible" title="写消息" width="600px" top="5vh">
      <el-form :model="composeForm" label-width="80px">
        <!-- 学生：从课程教师中选 -->
        <el-form-item v-if="userRole === 'student'" label="收件人">
          <el-select v-model="composeForm.receiverIds" multiple placeholder="选择教师" style="width: 100%;">
            <el-option v-for="t in teacherList" :key="t.userId" :label="`${t.realName} (${t.username})`" :value="t.userId" />
          </el-select>
        </el-form-item>

        <!-- 教师：选课程+选班级 -->
        <el-form-item v-if="userRole === 'teacher'" label="发送方式">
          <el-radio-group v-model="composeForm.sendMode">
            <el-radio value="course">向班级群发</el-radio>
            <el-radio value="individual">指定学生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="userRole === 'teacher'" label="选择课程">
          <el-select v-model="composeForm.courseId" placeholder="选择课程" style="width: 100%;" @change="handleTeacherCourseChange">
            <el-option v-for="c in teacherCourseList" :key="c.courseId" :label="`${c.courseName} (${c.semester || ''})`" :value="c.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userRole === 'teacher' && composeForm.sendMode === 'course'" label="选择班级">
          <el-select v-model="composeForm.classNames" multiple placeholder="选择班级（可多选）" style="width: 100%;">
            <el-option v-for="cls in currentClassList" :key="cls" :label="cls + '班'" :value="cls" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userRole === 'teacher' && composeForm.sendMode === 'individual'" label="选择班级">
          <el-select v-model="composeForm.className" placeholder="选择班级" style="width: 100%;" @change="handleTeacherClassChange">
            <el-option v-for="cls in currentClassList" :key="cls" :label="cls + '班'" :value="cls" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userRole === 'teacher' && composeForm.sendMode === 'individual'" label="选择学生">
          <el-select v-model="composeForm.receiverIds" multiple placeholder="选择学生" style="width: 100%;">
            <el-option v-for="s in studentList" :key="s.userId" :label="`${s.realName} (${s.username})`" :value="s.userId" />
          </el-select>
        </el-form-item>

        <!-- 管理员：搜索任意用户 -->
        <el-form-item v-if="userRole === 'admin'" label="收件人">
          <el-select v-model="composeForm.receiverIds" multiple filterable remote :remote-method="searchUsers" :loading="userSearching" placeholder="搜索用户" style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="标题">
          <el-input v-model="composeForm.title" placeholder="消息标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="composeForm.content" type="textarea" rows="6" placeholder="消息内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInbox, getSent, sendMessage, sendCourseMessage, markRead, getUnreadCount, getMyTeachers, getCourseStudentsForMessage, getTeacherCourses } from '@/api/message'
import { getMyCourses } from '@/api/elective'
import { getUserPage } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userRole = computed(() => {
  const t = authStore.userInfo?.userType
  if (t === 2) return 'admin'
  if (t === 1) return 'teacher'
  return 'student'
})

const folder = ref('inbox')
const loading = ref(false)
const messages = ref([])
const selectedMsg = ref(null)
const unreadCount = ref(0)

// 写消息
const composeVisible = ref(false)
const composeForm = reactive({ sendMode: 'course', courseId: null, className: null, classNames: [], receiverIds: [], title: '', content: '' })
const teacherList = ref([])
const teacherCourseList = ref([])
const currentClassList = ref([])
const studentList = ref([])
const userOptions = ref([])
const userSearching = ref(false)

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const switchFolder = (f) => {
  folder.value = f
  selectedMsg.value = null
  refresh()
}

const refresh = async () => {
  loading.value = true
  try {
    const res = folder.value === 'inbox' ? await getInbox() : await getSent()
    messages.value = res || []
  } finally { loading.value = false }
}

const fetchUnread = async () => {
  try { unreadCount.value = (await getUnreadCount()) || 0 } catch (e) { /* ignore */ }
}

const selectMessage = async (msg) => {
  selectedMsg.value = msg
  if (msg.isRead === 0 && folder.value === 'inbox') {
    try { await markRead(msg.id); msg.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1) } catch (e) { /* ignore */ }
  }
}

const openCompose = async () => {
  composeForm.sendMode = 'course'
  composeForm.courseId = null
  composeForm.className = null
  composeForm.classNames = []
  composeForm.receiverIds = []
  composeForm.title = ''
  composeForm.content = ''
  currentClassList.value = []
  studentList.value = []

  if (userRole.value === 'student') {
    try { teacherList.value = (await getMyTeachers()) || [] } catch (e) { console.error(e) }
  } else if (userRole.value === 'teacher') {
    try { teacherCourseList.value = (await getTeacherCourses()) || [] } catch (e) { console.error(e) }
  }

  composeVisible.value = true
}

const handleTeacherCourseChange = (courseId) => {
  composeForm.classNames = []
  composeForm.className = null
  composeForm.receiverIds = []
  studentList.value = []
  const course = teacherCourseList.value.find(c => c.courseId === courseId)
  currentClassList.value = course?.classNames || []
}

const handleTeacherClassChange = async (className) => {
  composeForm.receiverIds = []
  if (!composeForm.courseId || !className) { studentList.value = []; return }
  try {
    const all = (await getCourseStudentsForMessage(composeForm.courseId)) || []
    studentList.value = all.filter(s => s.className === className)
  } catch (e) { console.error(e) }
}

const searchUsers = async (query) => {
  if (!query) return
  userSearching.value = true
  try {
    const res = await getUserPage({ keyword: query, pageNum: 1, pageSize: 20 })
    userOptions.value = res.list || []
  } finally { userSearching.value = false }
}

const handleSend = async () => {
  if (!composeForm.title || !composeForm.content) { ElMessage.warning('请填写标题和内容'); return }
  try {
    if (userRole.value === 'teacher' && composeForm.sendMode === 'course' && composeForm.courseId) {
      if (!composeForm.classNames || composeForm.classNames.length === 0) { ElMessage.warning('请选择班级'); return }
      await sendCourseMessage({ courseId: composeForm.courseId, classNames: composeForm.classNames, title: composeForm.title, content: composeForm.content })
    } else {
      if (composeForm.receiverIds.length === 0) { ElMessage.warning('请选择收件人'); return }
      await sendMessage({ receiverIds: composeForm.receiverIds, title: composeForm.title, content: composeForm.content })
    }
    ElMessage.success('发送成功')
    composeVisible.value = false
    switchFolder('sent')
  } catch (e) { console.error(e) }
}

onMounted(() => { refresh(); fetchUnread() })
</script>

<style scoped>
.mail-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  overflow: hidden;
}

/* 左侧文件夹 */
.mail-sidebar {
  width: 200px;
  padding: 16px;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
}
.folder-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  transition: background 0.15s;
  position: relative;
}
.folder-item:hover { background: #f0f0f0; }
.folder-item.active { background: #1d1d1f; color: #fff; }
.folder-badge { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); }

/* 中间列表 */
.mail-list {
  width: 360px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}
.mail-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 15px;
}
.mail-items { flex: 1; overflow-y: auto; }
.mail-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background 0.15s;
}
.mail-item:hover { background: #f8f8f8; }
.mail-item.selected { background: #f0f0f0; }
.mail-item.unread { background: #f0f7ff; }
.mail-item.unread .mail-item-name { font-weight: 700; }
.mail-item.unread .mail-item-title { font-weight: 600; }
.mail-item-avatar { flex-shrink: 0; }
.mail-item-body { flex: 1; min-width: 0; }
.mail-item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.mail-item-name { font-size: 13px; font-weight: 500; color: #303133; }
.mail-item-time { font-size: 11px; color: #909399; flex-shrink: 0; }
.mail-item-title { font-size: 13px; color: #303133; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mail-item-preview { font-size: 12px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 右侧详情 */
.mail-detail { flex: 1; display: flex; flex-direction: column; overflow-y: auto; }
.mail-detail-header { padding: 24px 24px 0; }
.mail-detail-title { font-size: 20px; font-weight: 600; margin: 0 0 16px; color: #1d1d1f; }
.mail-detail-meta { display: flex; justify-content: space-between; align-items: flex-start; }
.mail-detail-sender { display: flex; gap: 12px; align-items: center; }
.sender-name { font-weight: 600; font-size: 14px; }
.sender-time { font-size: 12px; color: #909399; }
.mail-detail-to { font-size: 12px; color: #909399; }
.mail-detail-content { padding: 0 24px 24px; font-size: 15px; line-height: 1.7; color: #303133; white-space: pre-wrap; }

.mail-loading, .mail-empty { padding: 40px; text-align: center; color: #909399; font-size: 14px; }
</style>
