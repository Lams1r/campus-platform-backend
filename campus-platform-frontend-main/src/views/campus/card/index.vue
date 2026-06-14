<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="card-balance">
          <div class="balance-header">
            <span>校园卡余额</span>
            <div>
              <el-button type="danger" size="small" @click="handleReportLoss">挂失</el-button>
              <el-button type="warning" size="small" @click="handleUnlock">解挂</el-button>
            </div>
          </div>
          <div class="balance-amount">¥ {{ cardInfo.balance || '0.00' }}</div>
          <div class="balance-actions">
            <el-button @click="handleQuery">查询明细</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>消费记录</span>
              <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @change="handleDateChange" />
            </div>
          </template>
          <el-table :data="recordList" v-loading="loading" stripe>
            <el-table-column prop="transactionNo" label="交易号" width="180" />
            <el-table-column prop="transactionType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.transactionType === 'recharge' ? 'success' : 'primary'">{{ row.transactionType === 'recharge' ? '充值' : '消费' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="{ row }">
                <span :style="{ color: row.transactionType === 'recharge' ? '#67C23A' : '#F56C6C' }">{{ row.transactionType === 'recharge' ? '+' : '-' }}{{ row.amount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="消费地点" />
            <el-table-column prop="transactionTime" label="交易时间" width="180" />
          </el-table>
          <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50, 100]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 解挂对话框 -->
    <el-dialog v-model="unlockDialogVisible" title="解除挂失" width="400px">
      <el-form :model="unlockForm" label-width="100px">
        <el-form-item label="挂失记录ID">
          <el-input v-model="unlockForm.lossId" placeholder="请输入挂失记录ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unlockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUnlockSubmit">确认解挂</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyCardRecords, reportLoss, unlockLoss } from '@/api/card'

const loading = ref(false)
const cardInfo = reactive({ balance: 256.5, cardNo: '20240001' })
const recordList = ref([])
const dateRange = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const unlockDialogVisible = ref(false)
const unlockForm = reactive({ lossId: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (dateRange.value && dateRange.value.length === 2) { params.startTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
    const res = await getMyCardRecords(params)
    const list = res.list || res.data || []
    recordList.value = list
    pagination.total = res.total || 0
    if (list.length > 0 && list[0].cardNo) {
      cardInfo.cardNo = list[0].cardNo
    }
  } finally { loading.value = false }
}

const handleDateChange = () => { fetchData() }
const handleReportLoss = () => {
  ElMessageBox.confirm('确定要挂失校园卡吗？挂失后卡片将被冻结。', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await reportLoss(cardInfo.cardNo)
        ElMessage.success('挂失成功')
      } catch (error) { ElMessage.error('挂失失败') }
    })
}
const handleUnlock = () => {
  unlockForm.lossId = ''
  unlockDialogVisible.value = true
}
const handleUnlockSubmit = async () => {
  if (!unlockForm.lossId) {
    ElMessage.warning('请输入挂失记录ID')
    return
  }
  try {
    await unlockLoss(unlockForm.lossId)
    ElMessage.success('解挂成功')
    unlockDialogVisible.value = false
  } catch (error) { ElMessage.error('解挂失败') }
}
const handleQuery = () => { fetchData() }
const handleSizeChange = (val) => { pagination.pageSize = val; fetchData() }
const handleCurrentChange = (val) => { pagination.pageNum = val; fetchData() }

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-balance { text-align: center; padding: 20px; }
.balance-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; font-size: 16px; color: #606266; }
.balance-amount { font-size: 48px; font-weight: bold; color: #409EFF; margin: 30px 0; }
.balance-actions { display: flex; justify-content: center; gap: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
