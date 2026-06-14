<template>
  <div class="page-container">
    <!-- 管理员 -->
    <template v-if="isAdmin">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 楼栋管理 -->
        <el-tab-pane label="楼栋管理" name="building">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div class="title-block">
                  <span class="card-title">楼栋列表</span>
                  <small>维护宿舍楼栋和宿管信息</small>
                </div>
                <el-button type="primary" @click="handleAddBuilding"><el-icon><Plus /></el-icon>新增楼栋</el-button>
              </div>
            </template>
            <el-table :data="buildingList" v-loading="loading" stripe class="business-table">
              <el-table-column prop="buildingName" label="楼栋名称" />
              <el-table-column prop="buildingCode" label="编号" width="100" />
              <el-table-column prop="floorCount" label="楼层数" width="80" />
              <el-table-column prop="managerName" label="宿管" width="100" />
              <el-table-column prop="managerPhone" label="宿管电话" width="130" />
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button class="action-primary" size="small" @click="handleEditBuilding(row)">编辑</el-button>
                    <el-dropdown trigger="click" @command="(command) => handleBuildingCommand(command, row)">
                      <el-button class="action-more" size="small">
                        更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="delete" class="danger-item">删除楼栋</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 房间管理 -->
        <el-tab-pane label="房间管理" name="room">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div class="title-block">
                  <span class="card-title">房间列表</span>
                  <small>维护房间床位与使用状态</small>
                </div>
                <div class="header-actions">
                  <el-select v-model="roomFilterBuildingId" placeholder="筛选楼栋" clearable style="width: 160px; margin-right: 8px;" @change="fetchRoomList">
                    <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
                  </el-select>
                  <el-button type="primary" @click="handleAddRoom"><el-icon><Plus /></el-icon>新增房间</el-button>
                </div>
              </div>
            </template>
            <el-table :data="roomList" v-loading="loading" stripe class="business-table">
              <el-table-column label="楼栋" width="120">
                <template #default="{ row }">{{ getBuildingName(row.buildingId) }}</template>
              </el-table-column>
              <el-table-column prop="roomCode" label="房间号" width="100" />
              <el-table-column prop="floor" label="楼层" width="70" />
              <el-table-column prop="bedCount" label="床位" width="70" />
              <el-table-column prop="usedCount" label="已住" width="70" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 0" type="success">可用</el-tag>
                  <el-tag v-else-if="row.status === 1" type="warning">满员</el-tag>
                  <el-tag v-else type="danger">维修中</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="center">
                <template #default="{ row }">
                  <el-button class="action-primary" size="small" @click="handleEditRoom(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 住宿分配 -->
        <el-tab-pane label="住宿分配" name="allocation">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div class="title-block">
                  <span class="card-title">分配记录</span>
                  <small>查看和管理住宿分配情况</small>
                </div>
                <el-button type="primary" @click="handleAllocate"><el-icon><Plus /></el-icon>分配宿舍</el-button>
              </div>
            </template>
            <el-table :data="allocationList" v-loading="loading" stripe class="business-table">
              <el-table-column prop="userNo" label="学号/工号" width="120" />
              <el-table-column prop="userName" label="姓名" width="100" />
              <el-table-column label="身份" width="70">
                <template #default="{ row }">
                  <el-tag v-if="row.userType === 0" size="small">学生</el-tag>
                  <el-tag v-else-if="row.userType === 1" size="small" type="warning">教师</el-tag>
                  <el-tag v-else size="small" type="danger">管理员</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="buildingName" label="楼栋" width="120" />
              <el-table-column prop="roomCode" label="房间号" width="80" />
              <el-table-column prop="bedNumber" label="床位" width="60" />
              <el-table-column prop="checkInDate" label="入住时间" width="120" />
              <el-table-column label="操作" width="110" align="center">
                <template #default="{ row }">
                  <el-button class="action-danger" size="small" @click="handleDeallocate(row)">退宿</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 换宿审批 -->
        <el-tab-pane label="换宿审批" name="swap">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div class="title-block">
                  <span class="card-title">换宿申请</span>
                  <small>审批学生和教师提交的换宿请求</small>
                </div>
                <div class="header-actions">
                  <el-button @click="fetchSwapList"><el-icon><Refresh /></el-icon>刷新</el-button>
                </div>
              </div>
            </template>
            <el-table :data="swapList" v-loading="loading" stripe class="business-table">
              <el-table-column prop="userNo" label="学号/工号" width="120" />
              <el-table-column prop="userName" label="姓名" width="100" />
              <el-table-column label="身份" width="70">
                <template #default="{ row }">
                  <el-tag v-if="row.userType === 0" size="small">学生</el-tag>
                  <el-tag v-else-if="row.userType === 1" size="small" type="warning">教师</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="currentRoom" label="当前宿舍" width="150" />
              <el-table-column prop="targetRoom" label="目标宿舍" width="150" />
              <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
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
                    <el-button class="action-primary" size="small" @click="handleApproveSwap(row, 1)">同意</el-button>
                    <el-dropdown trigger="click" @command="(command) => handleSwapCommand(command, row)">
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
            <el-empty v-if="swapList.length === 0" description="暂无换宿申请" />
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 学生/教师 -->
    <template v-else>
      <!-- 我的宿舍 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div class="title-block">
            <span class="card-title">我的宿舍</span>
            <small>查看当前住宿信息并提交换宿申请</small>
          </div>
        </template>
        <div v-if="myAllocation" style="display: flex; gap: 32px; align-items: center;">
          <div>
            <div style="font-size: 13px; color: #909399;">楼栋</div>
            <div style="font-size: 16px; font-weight: 600;">{{ myAllocation.buildingName }}</div>
          </div>
          <div>
            <div style="font-size: 13px; color: #909399;">房间号</div>
            <div style="font-size: 16px; font-weight: 600;">{{ myAllocation.roomCode }}</div>
          </div>
          <div>
            <div style="font-size: 13px; color: #909399;">床位号</div>
            <div style="font-size: 16px; font-weight: 600;">{{ myAllocation.bedNumber }}</div>
          </div>
          <div>
            <div style="font-size: 13px; color: #909399;">入住时间</div>
            <div style="font-size: 16px; font-weight: 600;">{{ myAllocation.checkInDate }}</div>
          </div>
          <el-button type="primary" @click="handleSwapRequest">申请换宿舍</el-button>
        </div>
        <el-empty v-else description="暂未分配宿舍">
          <template #description>
            <span>暂未分配宿舍，请联系管理员</span>
          </template>
        </el-empty>
      </el-card>

      <!-- 我的换宿申请 -->
      <el-card shadow="never">
        <template #header>
          <div class="title-block">
            <span class="card-title">我的换宿申请</span>
            <small>查看换宿申请状态和审批意见</small>
          </div>
        </template>
        <el-table :data="mySwapList" stripe class="business-table">
          <el-table-column prop="targetRoomId" label="目标房间ID" width="120" />
          <el-table-column prop="reason" label="原因" min-width="200" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning">待审批</el-tag>
              <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approveRemark" label="审批意见" width="150" />
          <el-table-column prop="createTime" label="申请时间" width="170" />
        </el-table>
        <el-empty v-if="mySwapList.length === 0" description="暂无换宿申请" />
      </el-card>
    </template>

    <!-- 楼栋表单 -->
    <el-dialog v-model="buildingDialogVisible" :title="buildingDialogTitle" width="500px">
      <el-form :model="buildingForm" label-width="100px">
        <el-form-item label="楼栋名称"><el-input v-model="buildingForm.buildingName" placeholder="如：1号宿舍楼" /></el-form-item>
        <el-form-item label="楼栋编号"><el-input v-model="buildingForm.buildingCode" placeholder="如：A01" /></el-form-item>
        <el-form-item label="楼层数"><el-input-number v-model="buildingForm.floorCount" :min="1" :max="30" /></el-form-item>
        <el-form-item label="宿管姓名"><el-input v-model="buildingForm.managerName" /></el-form-item>
        <el-form-item label="宿管电话"><el-input v-model="buildingForm.managerPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBuilding">确定</el-button>
      </template>
    </el-dialog>

    <!-- 房间表单 -->
    <el-dialog v-model="roomDialogVisible" :title="roomDialogTitle" width="500px">
      <el-form :model="roomForm" label-width="100px">
        <el-form-item label="所属楼栋">
          <el-select v-model="roomForm.buildingId" placeholder="选择楼栋" style="width: 100%;">
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号"><el-input v-model="roomForm.roomCode" placeholder="如：101" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="roomForm.floor" :min="1" :max="30" /></el-form-item>
        <el-form-item label="床位数"><el-input-number v-model="roomForm.bedCount" :min="1" :max="12" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoom">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配宿舍表单 -->
    <el-dialog v-model="allocateDialogVisible" title="分配宿舍" width="550px">
      <el-form :model="allocateForm" label-width="100px">
        <el-form-item label="选择用户">
          <el-select v-model="allocateForm.studentId" filterable remote :remote-method="searchAllocateUser" :loading="allocateSearching" placeholder="搜索用户" style="width: 100%;">
            <el-option v-for="u in allocateUserOptions" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="allocateForm.buildingId" placeholder="选择楼栋" style="width: 100%;" @change="allocateForm.roomId = null">
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间">
          <el-select v-model="allocateForm.roomId" placeholder="选择房间" style="width: 100%;">
            <el-option v-for="r in availableRooms" :key="r.id" :label="`${r.roomCode} (空位${r.bedCount - r.usedCount})`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位号">
          <el-input-number v-model="allocateForm.bedNumber" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="入住时间">
          <el-date-picker v-model="allocateForm.checkInDate" type="date" placeholder="选择入住日期" value-format="YYYY-MM-DD" format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="allocateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAllocate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 换宿申请表单 -->
    <el-dialog v-model="swapDialogVisible" title="申请换宿舍" width="500px">
      <el-form :model="swapForm" label-width="100px">
        <el-form-item label="当前宿舍">
          <el-input :model-value="myAllocation ? `${myAllocation.buildingName} ${myAllocation.roomCode}室` : '未分配'" disabled />
        </el-form-item>
        <el-form-item label="目标楼栋">
          <el-select v-model="swapForm.targetBuildingId" placeholder="选择楼栋" style="width: 100%;" @change="swapForm.targetRoomId = null">
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标房间">
          <el-select v-model="swapForm.targetRoomId" placeholder="选择房间" style="width: 100%;">
            <el-option v-for="r in swapAvailableRooms" :key="r.id" :label="`${r.roomCode} (空位${r.bedCount - r.usedCount})`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="换宿原因">
          <el-input v-model="swapForm.reason" type="textarea" rows="3" placeholder="请输入换宿原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="swapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSwap">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBuildingList, addBuilding, updateBuilding, deleteBuilding,
  getRoomList, addRoom, updateRoom,
  allocateDorm, deallocateDorm,
  submitDormSwap, getMyDormSwaps, getDormSwapPage, approveDormSwap
} from '@/api/dorm'
import { getUserPage } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.userInfo?.userType === 2)

const activeTab = ref('building')
const loading = ref(false)
const buildingList = ref([])
const roomList = ref([])
const allocationList = ref([])
const swapList = ref([])
const myAllocation = ref(null)
const mySwapList = ref([])
const roomFilterBuildingId = ref(null)

// ========== 楼栋 ==========
const buildingDialogVisible = ref(false)
const buildingDialogTitle = ref('')
const buildingForm = reactive({ id: null, buildingName: '', buildingCode: '', floorCount: 6, managerName: '', managerPhone: '' })

const fetchBuildingList = async () => {
  try { buildingList.value = (await getBuildingList()) || [] } catch (e) { console.error(e) }
}

const handleAddBuilding = () => {
  buildingDialogTitle.value = '新增楼栋'
  Object.assign(buildingForm, { id: null, buildingName: '', buildingCode: '', floorCount: 6, managerName: '', managerPhone: '' })
  buildingDialogVisible.value = true
}

const handleEditBuilding = (row) => {
  buildingDialogTitle.value = '编辑楼栋'
  Object.assign(buildingForm, row)
  buildingDialogVisible.value = true
}

const submitBuilding = async () => {
  try {
    if (buildingForm.id) await updateBuilding(buildingForm); else await addBuilding(buildingForm)
    ElMessage.success('保存成功'); buildingDialogVisible.value = false; fetchBuildingList()
  } catch (e) { console.error(e) }
}

const handleDeleteBuilding = (row) => {
  ElMessageBox.confirm(`确定删除 "${row.buildingName}"？`, '提示', { type: 'warning' })
    .then(async () => { await deleteBuilding(row.id); ElMessage.success('删除成功'); fetchBuildingList() })
}

const handleBuildingCommand = (command, row) => {
  if (command === 'delete') {
    handleDeleteBuilding(row)
  }
}

const getBuildingName = (id) => buildingList.value.find(b => b.id === id)?.buildingName || ''

// ========== 房间 ==========
const roomDialogVisible = ref(false)
const roomDialogTitle = ref('')
const roomForm = reactive({ id: null, buildingId: null, roomCode: '', floor: 1, bedCount: 4 })

const fetchRoomList = async () => {
  loading.value = true
  try {
    const params = roomFilterBuildingId.value ? { buildingId: roomFilterBuildingId.value } : {}
    roomList.value = (await getRoomList(params)) || []
  } finally { loading.value = false }
}

const handleAddRoom = () => {
  roomDialogTitle.value = '新增房间'
  Object.assign(roomForm, { id: null, buildingId: null, roomCode: '', floor: 1, bedCount: 4 })
  roomDialogVisible.value = true
}

const handleEditRoom = (row) => {
  roomDialogTitle.value = '编辑房间'
  Object.assign(roomForm, row)
  roomDialogVisible.value = true
}

const submitRoom = async () => {
  try {
    if (roomForm.id) await updateRoom(roomForm); else await addRoom(roomForm)
    ElMessage.success('保存成功'); roomDialogVisible.value = false; fetchRoomList()
  } catch (e) { console.error(e) }
}

// ========== 分配 ==========
const allocateDialogVisible = ref(false)
const allocateForm = reactive({ studentId: null, buildingId: null, roomId: null, bedNumber: 1, checkInDate: '' })
const allocateUserOptions = ref([])
const allocateSearching = ref(false)

const availableRooms = computed(() => {
  if (!allocateForm.buildingId) return []
  return roomList.value.filter(r => r.buildingId === allocateForm.buildingId && r.status !== 2 && r.usedCount < r.bedCount)
})

const fetchAllocationList = async () => {
  loading.value = true
  try {
    const { default: request } = await import('@/api/request')
    const res = await request.get('/svc/dorm/allocation/list')
    allocationList.value = res || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleAllocate = () => {
  Object.assign(allocateForm, { studentId: null, buildingId: null, roomId: null, bedNumber: 1, checkInDate: '' })
  allocateUserOptions.value = []
  allocateDialogVisible.value = true
}

const searchAllocateUser = async (query) => {
  if (!query) return
  allocateSearching.value = true
  try {
    const res = await getUserPage({ keyword: query, pageNum: 1, pageSize: 20 })
    allocateUserOptions.value = res.list || []
  } finally { allocateSearching.value = false }
}

const submitAllocate = async () => {
  if (!allocateForm.studentId || !allocateForm.roomId) { ElMessage.warning('请选择用户和房间'); return }
  try {
    await allocateDorm(allocateForm)
    ElMessage.success('分配成功'); allocateDialogVisible.value = false
    fetchAllocationList(); fetchRoomList()
  } catch (e) { console.error(e) }
}

const handleDeallocate = (row) => {
  ElMessageBox.confirm(`确定为 "${row.userName}" 办理退宿？`, '提示', { type: 'warning' })
    .then(async () => {
      await deallocateDorm(row.id)
      ElMessage.success('退宿成功'); fetchAllocationList(); fetchRoomList()
    })
}

// ========== 换宿 ==========
const swapDialogVisible = ref(false)
const swapForm = reactive({ currentRoomId: null, targetBuildingId: null, targetRoomId: null, reason: '' })

const swapAvailableRooms = computed(() => {
  if (!swapForm.targetBuildingId) return []
  return roomList.value.filter(r => r.buildingId === swapForm.targetBuildingId && r.status !== 2 && r.usedCount < r.bedCount)
})

const fetchSwapList = async () => {
  loading.value = true
  try { swapList.value = (await getDormSwapPage()) || [] } finally { loading.value = false }
}

const fetchMySwapList = async () => {
  try { mySwapList.value = (await getMyDormSwaps()) || [] } catch (e) { console.error(e) }
}

const fetchMyAllocation = async () => {
  try {
    const { default: request } = await import('@/api/request')
    const res = await request.get('/svc/dorm/allocation/my')
    myAllocation.value = res || null
  } catch (e) { console.error(e) }
}

const handleSwapRequest = () => {
  Object.assign(swapForm, { currentRoomId: myAllocation.value?.roomId || null, targetBuildingId: null, targetRoomId: null, reason: '' })
  swapDialogVisible.value = true
}

const submitSwap = async () => {
  if (!swapForm.targetRoomId) { ElMessage.warning('请选择目标房间'); return }
  try {
    await submitDormSwap(swapForm)
    ElMessage.success('换宿申请已提交'); swapDialogVisible.value = false
    fetchMySwapList()
  } catch (e) { console.error(e) }
}

const handleApproveSwap = async (row, status) => {
  const action = status === 1 ? '同意' : '驳回'
  const { value: remark } = await ElMessageBox.prompt(`${action}该换宿申请？`, '审批', {
    confirmButtonText: action, cancelButtonText: '取消', inputPlaceholder: '审批意见（可选）'
  }).catch(() => ({ value: null }))
  if (remark === null) return
  try {
    await approveDormSwap(row.id, status, remark)
    ElMessage.success(`已${action}`); fetchSwapList()
  } catch (e) { console.error(e) }
}

const handleSwapCommand = (command, row) => {
  if (command === 'reject') {
    handleApproveSwap(row, 2)
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchBuildingList()
  if (isAdmin.value) {
    fetchRoomList(); fetchAllocationList(); fetchSwapList()
  } else {
    fetchMyAllocation(); fetchMySwapList(); fetchRoomList()
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
  gap: 10px;
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
  .header-actions .el-button,
  .card-header > .el-button {
    width: 100% !important;
    margin-right: 0 !important;
  }
}
</style>
