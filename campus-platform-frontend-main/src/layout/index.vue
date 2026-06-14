<template>
  <el-container class="layout-container" :class="{ mobile: appStore.isMobile }">
    <!-- 侧边栏 -->
    <Sidebar />
    <div
      v-if="appStore.isMobile && !appStore.sidebarCollapsed"
      class="sidebar-mask"
      @click="appStore.closeSidebar()"
    />

    <el-container class="main-container">
      <!-- 顶部导航 -->
      <Header />

      <!-- 标签页 -->
      <TagsView v-if="showTagsView" />

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="$route.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import TagsView from './components/TagsView.vue'

const appStore = useAppStore()
const route = useRoute()
const cachedViews = computed(() => appStore.cachedViews)
const showTagsView = true

const syncDevice = () => {
  appStore.setDevice(window.innerWidth < 900 ? 'mobile' : 'desktop')
}

watch(() => route.fullPath, () => {
  if (appStore.isMobile) {
    appStore.closeSidebar()
  }
})

onMounted(() => {
  syncDevice()
  window.addEventListener('resize', syncDevice)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncDevice)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  background:
    radial-gradient(circle at 12% 0%, rgba(37, 99, 235, 0.1), transparent 28%),
    radial-gradient(circle at 88% 18%, rgba(14, 165, 233, 0.08), transparent 24%),
    var(--apple-parchment, #f5f7fa);
  color: var(--apple-ink, #1d1d1f);
  position: relative;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.main-content {
  flex: 1;
  padding: 22px;
  background: transparent;
  overflow-y: auto;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 998;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.layout-container.mobile .main-content {
  padding: 14px;
}

@media (max-width: 640px) {
  .layout-container.mobile .main-content {
    padding: 10px;
    padding-bottom: calc(10px + env(safe-area-inset-bottom));
  }
}
</style>
