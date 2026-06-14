<template>
  <el-header class="header">
    <div class="header-left">
      <!-- 折叠按钮 -->
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>

      <!-- 面包屑 -->
      <Breadcrumb />
    </div>

    <div class="header-right">
      <!-- 搜索 -->
      <button class="search-trigger" @click="openSearch">
        <el-icon><Search /></el-icon>
        <span>搜索功能</span>
        <kbd>Ctrl K</kbd>
      </button>

      <!-- 全屏 -->
      <el-icon class="action-icon" @click="toggleFullScreen">
        <FullScreen v-if="!isFullscreen" />
        <Aim v-else />
      </el-icon>

      <!-- 主题切换 -->
      <el-icon class="action-icon" @click="appStore.toggleTheme()">
        <Moon v-if="appStore.theme === 'light'" />
        <Sunny v-else />
      </el-icon>

      <!-- 消息通知 -->
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="action-icon">
        <el-icon @click="router.push('/campus/message')"><Bell /></el-icon>
      </el-badge>

      <!-- 用户信息 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="30" :src="authStore.avatar" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <span class="username">{{ authStore.nickname || authStore.username }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><HomeFilled /></el-icon>回到工作台
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><ChatDotRound /></el-icon>消息中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>

  <el-dialog
    v-model="searchVisible"
    width="560px"
    class="command-dialog"
    :show-close="false"
    append-to-body
  >
    <div class="command-panel">
      <div class="command-input-wrap">
        <el-icon><Search /></el-icon>
        <input
          ref="searchInputRef"
          v-model="keyword"
          class="command-input"
          placeholder="搜索页面、功能或模块"
          @keydown.enter="goFirstResult"
        >
        <span class="command-hint">Enter</span>
      </div>
      <div class="command-list">
        <button
          v-for="item in filteredCommands"
          :key="item.path"
          class="command-item"
          @click="goCommand(item)"
        >
          <span class="command-icon">
            <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
          </span>
          <span class="command-main">
            <strong>{{ item.title }}</strong>
            <small>{{ item.path }}</small>
          </span>
        </button>
        <el-empty v-if="filteredCommands.length === 0" description="没有匹配的功能" :image-size="96" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCount } from '@/api/message'
import Breadcrumb from './Breadcrumb.vue'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const isFullscreen = ref(false)
const unreadCount = ref(0)
const searchVisible = ref(false)
const keyword = ref('')
const searchInputRef = ref(null)

const flattenRoutes = (routes, parentPath = '') => {
  const result = []
  routes.forEach(route => {
    if (route.meta?.hidden) return
    const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`.replace('//', '/')
    if (route.children?.length) {
      result.push(...flattenRoutes(route.children, path))
    } else if (route.meta?.title) {
      result.push({
        title: route.meta.title,
        icon: route.meta.icon,
        path
      })
    }
  })
  return result
}

const commands = computed(() => flattenRoutes(appStore.permissionRoutes))
const filteredCommands = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return commands.value.slice(0, 8)
  return commands.value
    .filter(item => `${item.title} ${item.path}`.toLowerCase().includes(value))
    .slice(0, 10)
})

const openSearch = async () => {
  searchVisible.value = true
  await nextTick()
  searchInputRef.value?.focus()
}

const goCommand = (item) => {
  searchVisible.value = false
  keyword.value = ''
  router.push(item.path)
}

const goFirstResult = () => {
  if (filteredCommands.value[0]) {
    goCommand(filteredCommands.value[0])
  }
}

const handleShortcut = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    openSearch()
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = Number(res || 0)
  } catch (error) {
    unreadCount.value = 0
  }
}

const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
    isFullscreen.value = true
  } else {
    document.exitFullscreen?.()
    isFullscreen.value = false
  }
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/dashboard')
      break
    case 'settings':
      router.push('/campus/message')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        authStore.logoutAction()
      })
      break
  }
}

onMounted(() => {
  fetchUnreadCount()
  window.addEventListener('keydown', handleShortcut)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleShortcut)
})
</script>

<style scoped>
.header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--apple-surface, rgba(255, 255, 255, 0.82));
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--apple-hairline, #e0e0e0);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
  padding: 0 22px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--apple-ink-muted-48, #7a7a7a);
  transition: color 0.2s;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.collapse-btn:hover {
  color: var(--apple-ink, #1d1d1f);
  background: var(--apple-parchment, #f5f5f7);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  font-size: 17px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  cursor: pointer;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon:hover {
  color: var(--apple-ink, #1d1d1f);
  background: var(--apple-parchment, #f5f5f7);
}

.search-trigger {
  height: 36px;
  min-width: 210px;
  border: 1px solid var(--apple-hairline, #e0e0e0);
  border-radius: 999px;
  background: var(--apple-surface-strong, #ffffff);
  color: var(--apple-ink-muted-48, #7a7a7a);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px 0 13px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.search-trigger:hover {
  color: var(--apple-ink, #1d1d1f);
  border-color: var(--apple-chip, #d2d2d7);
  transform: translateY(-1px);
}

.search-trigger span {
  flex: 1;
  text-align: left;
  font-size: 13px;
}

.search-trigger kbd {
  border: 1px solid var(--apple-hairline, #e0e0e0);
  border-radius: 7px;
  padding: 2px 6px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  background: var(--apple-parchment, #f5f5f7);
  font-size: 11px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--apple-radius-pill, 9999px);
  transition: background 0.2s, border-color 0.2s;
}

.user-info:hover {
  background: var(--apple-parchment, #f5f5f7);
  border-color: var(--apple-hairline, #e0e0e0);
}

.user-avatar {
  background: var(--apple-parchment, #f5f5f7);
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-ink, #1d1d1f);
  letter-spacing: -0.224px;
}

.arrow-icon {
  font-size: 12px;
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.command-panel {
  padding: 4px;
}

.command-input-wrap {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--apple-hairline, #e0e0e0);
  background: var(--apple-parchment, #f5f5f7);
  border-radius: 16px;
  padding: 0 14px;
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.command-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--apple-ink, #1d1d1f);
  font-size: 16px;
  font-family: var(--apple-font-body, system-ui);
}

.command-hint {
  border: 1px solid var(--apple-hairline, #e0e0e0);
  border-radius: 8px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.command-list {
  max-height: 360px;
  overflow-y: auto;
  padding-top: 10px;
}

.command-item {
  width: 100%;
  border: none;
  background: transparent;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.16s ease, transform 0.16s ease;
}

.command-item:hover {
  background: var(--apple-parchment, #f5f5f7);
  transform: translateY(-1px);
}

.command-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--apple-primary, #2563eb);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.command-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.command-main strong {
  color: var(--apple-ink, #1d1d1f);
  font-size: 14px;
}

.command-main small {
  color: var(--apple-ink-muted-48, #7a7a7a);
  font-size: 12px;
}

@media (max-width: 760px) {
  .header {
    padding: 0 12px;
    height: 58px;
  }

  .search-trigger {
    min-width: 36px;
    width: 36px;
    padding: 0;
    justify-content: center;
  }

  .search-trigger span,
  .search-trigger kbd,
  .username {
    display: none;
  }

  .header-right {
    gap: 6px;
  }

  .action-icon,
  .collapse-btn {
    width: 34px;
    height: 34px;
  }
}
</style>
