<template>
  <el-aside
    :width="sidebarWidth"
    class="sidebar"
    :class="{ collapsed: appStore.sidebarCollapsed, mobile: appStore.isMobile, open: appStore.isMobile && !appStore.sidebarCollapsed }"
  >
    <!-- Logo -->
    <div class="logo-container">
      <img src="@/assets/campus-logo.jpg" class="logo-img" alt="校园平台 logo">
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">智慧校园</span>
      <span v-show="!appStore.sidebarCollapsed" class="logo-badge">Pro</span>
    </div>

    <!-- 菜单 -->
    <el-scrollbar class="menu-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.8)"
        active-text-color="#ffffff"
      >
        <SidebarItem
          v-for="route in menuRoutes"
          :key="route.path"
          :item="route"
          :base-path="'/'"
        />
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const appStore = useAppStore()

// 使用路由守卫中按角色过滤后的路由列表
const menuRoutes = computed(() => appStore.permissionRoutes)

const defaultOpeneds = computed(() => {
  return menuRoutes.value
    .filter(route => route.children?.length && !route.meta?.hidden)
    .map(route => resolveRootPath(route.path))
})

const sidebarWidth = computed(() => {
  if (appStore.isMobile) return '264px'
  return appStore.sidebarCollapsed ? '64px' : '210px'
})

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

const resolveRootPath = (routePath) => {
  if (routePath.startsWith('/')) {
    return routePath
  }
  return `/${routePath}`.replace('//', '/')
}
</script>

<style scoped>
.sidebar {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 22%),
    var(--apple-sidebar, #101828);
  transition: width 0.28s cubic-bezier(0.32, 0.72, 0, 1), transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 18px 0 60px rgba(15, 23, 42, 0.12);
  z-index: 999;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-img {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 13px;
  object-fit: cover;
  object-position: center;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.2);
}

.logo-text {
  color: var(--apple-on-dark, #ffffff);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  white-space: nowrap;
  font-family: var(--apple-font-display, system-ui, -apple-system, sans-serif);
}

.logo-badge {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  line-height: 18px;
}

.sidebar.collapsed .logo-container {
  justify-content: center;
  padding: 0;
}

.menu-scrollbar {
  flex: 1;
}

:deep(.el-menu) {
  border-right: none;
  padding: 12px 10px;
  background: transparent !important;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  position: relative;
  border-radius: 12px;
  margin: 4px 0;
  height: 42px;
  line-height: 42px;
  font-size: 14px;
  letter-spacing: -0.224px;
  transition: background 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.09) !important;
  transform: translateX(2px);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(14, 165, 233, 0.76)) !important;
  color: #ffffff !important;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.26);
}

:deep(.el-menu-item.is-active::before) {
  content: "";
  width: 4px;
  height: 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  position: absolute;
  left: 6px;
}

:deep(.el-sub-menu .el-menu) {
  padding: 0 0 0 8px;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  padding: 0 !important;
  text-align: center;
}

.sidebar.mobile {
  position: fixed;
  inset: 0 auto 0 0;
  height: 100vh;
  transform: translateX(-100%);
}

.sidebar.mobile.open {
  transform: translateX(0);
}

.sidebar.mobile .logo-container {
  height: 68px;
}
</style>
