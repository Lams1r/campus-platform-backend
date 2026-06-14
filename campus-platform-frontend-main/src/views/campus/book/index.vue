<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <!-- 图书检索/管理 -->
      <el-tab-pane label="图书检索" name="search">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-block">
                <span class="card-title">{{ isAdmin ? '图书管理' : '图书检索' }}</span>
                <small>检索馆藏图书，管理借阅与归还记录</small>
              </div>
              <div class="header-actions">
                <el-button v-if="isAdmin" type="primary" @click="handleAddBook"><el-icon><Plus /></el-icon>新增图书</el-button>
              </div>
            </div>
          </template>
          <el-form :model="searchForm" inline class="search-form">
            <el-form-item label="书名">
              <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable />
            </el-form-item>
            <el-form-item class="search-actions">
              <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
              <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="bookList" v-loading="loading" stripe class="business-table">
            <el-table-column prop="bookName" label="书名" min-width="180" />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="publisher" label="出版社" width="140" />
            <el-table-column prop="isbn" label="ISBN" width="140" />
            <el-table-column prop="category" label="分类" width="80" />
            <el-table-column prop="totalCount" label="库存" width="70" />
            <el-table-column prop="availableCount" label="可借" width="70">
              <template #default="{ row }">
                <el-tag :type="row.availableCount > 0 ? 'success' : 'danger'" size="small">{{ row.availableCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" width="120" />
            <el-table-column label="操作" :width="isAdmin ? 150 : 110" fixed="right" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <template v-if="isAdmin">
                    <el-button class="action-primary" size="small" @click="handleEditBook(row)">编辑</el-button>
                    <el-dropdown trigger="click" @command="(command) => handleBookCommand(command, row)">
                      <el-button class="action-more" size="small">
                        更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="delete" class="danger-item">删除图书</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                  <template v-else>
                    <el-button v-if="row.availableCount > 0" class="action-primary" size="small" @click="handleBorrow(row)">借阅</el-button>
                    <el-button v-else class="action-more" size="small" disabled>暂无库存</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50]" :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange" @current-change="handleCurrentChange" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 我的借阅 -->
      <el-tab-pane label="我的借阅" name="my">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>我的借阅</span>
              <el-button @click="fetchBorrowList"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </template>
          <el-table :data="borrowList" v-loading="loading" stripe class="business-table">
            <el-table-column prop="bookName" label="书名" min-width="180" />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="borrowTime" label="借阅时间" width="170" />
            <el-table-column prop="dueTime" label="应还时间" width="170" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'primary' : row.status === 2 ? 'danger' : 'success'">
                  {{ row.status === 0 ? '借阅中' : row.status === 2 ? '已逾期' : '已归还' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button v-if="row.status === 0 || row.status === 2" class="action-primary" size="small" @click="handleReturn(row)">归还</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="borrowList.length === 0" description="暂无借阅记录" />
        </el-card>
      </el-tab-pane>

      <!-- 管理员：借阅记录 -->
      <el-tab-pane v-if="isAdmin" label="借阅记录" name="borrowRecords">
        <el-card shadow="never">
          <template #header><span>全部借阅记录</span></template>
          <el-table :data="allBorrowList" v-loading="loading" stripe class="business-table">
            <el-table-column prop="bookName" label="书名" min-width="150" />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="studentId" label="借阅人ID" width="100" />
            <el-table-column prop="borrowTime" label="借阅时间" width="170" />
            <el-table-column prop="dueTime" label="应还时间" width="170" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'primary' : row.status === 2 ? 'danger' : 'success'">
                  {{ row.status === 0 ? '借阅中' : row.status === 2 ? '已逾期' : '已归还' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑图书对话框 -->
    <el-dialog v-model="bookDialogVisible" :title="bookDialogTitle" width="600px">
      <el-form ref="bookFormRef" :model="bookForm" :rules="bookFormRules" label-width="100px">
        <el-form-item label="书名" prop="bookName">
          <el-input v-model="bookForm.bookName" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="bookForm.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input v-model="bookForm.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="bookForm.publisher" placeholder="请输入出版社" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="bookForm.category" placeholder="如：计算机、数学、英语" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="总库存" prop="totalCount">
              <el-input-number v-model="bookForm.totalCount" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放位置">
              <el-input v-model="bookForm.location" placeholder="如：A区1层01架" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBook">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookPage, addBook, updateBook, deleteBook, borrowBook, returnBook, getMyBorrows, getBorrowPage } from '@/api/book'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.userInfo?.userType === 2)

const activeTab = ref('search')
const loading = ref(false)
const searchForm = reactive({ keyword: '' })
const bookList = ref([])
const borrowList = ref([])
const allBorrowList = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// 图书表单
const bookDialogVisible = ref(false)
const bookDialogTitle = ref('')
const isEditBook = ref(false)
const bookFormRef = ref(null)
const bookForm = reactive({ id: null, bookName: '', author: '', isbn: '', publisher: '', category: '', totalCount: 1, location: '' })
const bookFormRules = {
  bookName: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入库存数量', trigger: 'blur' }]
}

const fetchBookList = async () => {
  loading.value = true
  try {
    const res = await getBookPage({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    bookList.value = res.list || []
    pagination.total = res.total || 0
  } finally { loading.value = false }
}

const fetchBorrowList = async () => {
  loading.value = true
  try {
    const res = await getMyBorrows({ pageNum: 1, pageSize: 50 })
    borrowList.value = res.list || []
  } finally { loading.value = false }
}

const fetchAllBorrowList = async () => {
  loading.value = true
  try {
    const res = await getBorrowPage({ pageNum: 1, pageSize: 50 })
    allBorrowList.value = res.list || []
  } finally { loading.value = false }
}

const onTabChange = (tab) => {
  if (tab === 'search') fetchBookList()
  else if (tab === 'my') fetchBorrowList()
  else if (tab === 'borrowRecords') fetchAllBorrowList()
}

const handleSearch = () => { pagination.pageNum = 1; fetchBookList() }
const handleReset = () => { searchForm.keyword = ''; handleSearch() }
const handleSizeChange = (val) => { pagination.pageSize = val; fetchBookList() }
const handleCurrentChange = (val) => { pagination.pageNum = val; fetchBookList() }

// 图书 CRUD
const handleAddBook = () => {
  isEditBook.value = false; bookDialogTitle.value = '新增图书'
  Object.assign(bookForm, { id: null, bookName: '', author: '', isbn: '', publisher: '', category: '', totalCount: 1, location: '' })
  bookDialogVisible.value = true
}

const handleEditBook = (row) => {
  isEditBook.value = true; bookDialogTitle.value = '编辑图书'
  Object.assign(bookForm, row)
  bookDialogVisible.value = true
}

const submitBook = async () => {
  await bookFormRef.value.validate()
  try {
    if (isEditBook.value) await updateBook(bookForm); else await addBook(bookForm)
    ElMessage.success('保存成功'); bookDialogVisible.value = false; fetchBookList()
  } catch (e) { console.error(e) }
}

const handleDeleteBook = (row) => {
  ElMessageBox.confirm(`确定删除 "${row.bookName}"？`, '提示', { type: 'warning' })
    .then(async () => { await deleteBook(row.id); ElMessage.success('删除成功'); fetchBookList() })
}

const handleBookCommand = (command, row) => {
  if (command === 'delete') {
    handleDeleteBook(row)
  }
}

// 借阅/归还
const handleBorrow = async (row) => {
  try { await borrowBook(row.id); ElMessage.success('借阅成功'); fetchBookList() } catch (e) { console.error(e) }
}

const handleReturn = async (row) => {
  try { await returnBook(row.id); ElMessage.success('归还成功'); fetchBorrowList() } catch (e) { console.error(e) }
}

onMounted(() => { fetchBookList(); fetchBorrowList() })
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

.search-actions {
  margin-left: auto !important;
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
  border-radius: 999px !important;
  min-height: 30px;
  padding: 6px 12px !important;
  font-size: 12px;
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-button {
    width: 100%;
  }

  .search-actions {
    margin-left: 0 !important;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
