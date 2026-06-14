<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="logType">
            <el-radio-button value="operate">操作日志</el-radio-button>
            <el-radio-button value="login">登录日志</el-radio-button>
          </el-radio-group>
          <el-button @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
      </template>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="操作人" v-if="logType === 'operate'">
          <el-input v-model="searchForm.operateUserName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="操作模块" v-if="logType === 'operate'">
          <el-input v-model="searchForm.module" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="用户名" v-if="logType === 'login'">
          <el-input v-model="searchForm.username" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <!-- 操作日志列 -->
        <el-table-column v-if="logType === 'operate'" prop="module" label="操作模块" width="120" />
        <el-table-column v-if="logType === 'operate'" prop="operateType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperTypeType(row.operateType)">{{ row.operateType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="logType === 'operate'" prop="operateUserName" label="操作人" width="100" />
        <el-table-column v-if="logType === 'operate'" prop="requestMethod" label="请求方式" width="90" />
        <el-table-column v-if="logType === 'operate'" prop="requestUrl" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="logType === 'operate'" prop="requestParams" label="请求参数" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="logType === 'operate'" prop="ip" label="IP" width="130" />
        <el-table-column v-if="logType === 'operate'" prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="logType === 'operate'" prop="costTime" label="耗时(ms)" width="90" />
        <el-table-column v-if="logType === 'operate'" prop="createTime" label="操作时间" width="170" />

        <!-- 登录日志列 -->
        <el-table-column v-if="logType === 'login'" prop="username" label="用户名" width="120" />
        <el-table-column v-if="logType === 'login'" prop="loginType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.loginType === 0 ? 'success' : 'info'">{{ row.loginType === 0 ? '登录' : '登出' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="logType === 'login'" prop="msg" label="消息" min-width="150" />
        <el-table-column v-if="logType === 'login'" prop="ip" label="IP" width="130" />
        <el-table-column v-if="logType === 'login'" prop="userAgent" label="浏览器" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="logType === 'login'" prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="logType === 'login'" prop="createTime" label="时间" width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange" @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { getOperateLogPage, getLoginLogPage } from '@/api/log'

const logType = ref('operate')
const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ operateUserName: '', module: '', username: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const getOperTypeType = (type) => {
  const types = { '新增': 'success', '修改': 'warning', '删除': 'danger', '导入': 'primary', '导出': 'info' }
  return types[type] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    const res = logType.value === 'operate' ? await getOperateLogPage(params) : await getLoginLogPage(params)
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = ''); handleSearch() }
const handleSizeChange = (val) => { pagination.pageSize = val; fetchData() }
const handleCurrentChange = (val) => { pagination.pageNum = val; fetchData() }

watch(logType, () => { handleSearch() })
onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 20px; }
</style>
