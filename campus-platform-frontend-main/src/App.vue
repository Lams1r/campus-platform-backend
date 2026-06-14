<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="transitionName" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/**
 * 过场动画名，由 router.beforeEach 在导航前写入。
 * 模块级变量，跨组件持久。
 */
let _transitionName = 'none'

router.beforeEach((to, from) => {
  const toAuth = to.path === '/login' || to.path === '/register'
  const fromAuth = from.path === '/login' || from.path === '/register'

  if (toAuth && fromAuth) {
    // login ↔ register：根据方向决定滑入方向
    _transitionName = to.path === '/register' ? 'auth-slide-left' : 'auth-slide-right'
  } else if (fromAuth && !toAuth) {
    // 登录/注册 → 主页面：不做动画，直接切换
    _transitionName = 'none'
  } else if (!fromAuth && toAuth) {
    // 主页面 → 登录页（登出）：不做动画
    _transitionName = 'none'
  } else {
    // 主页面内部切换（侧边栏导航）：轻量淡入淡出
    _transitionName = 'fade'
  }
})

// computed 在渲染时读取，保证取到 beforeEach 写入的最新值
const transitionName = computed(() => _transitionName)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: "Inter", "SF Pro Text", "SF Pro Display", system-ui, -apple-system, BlinkMacSystemFont, "PingFang SC", "PingFang TC", "Microsoft YaHei", sans-serif;
}

/* ============================================================
   Auth 页面过场 — Apple 风格水平滑入 + 模糊过渡
   login → register：左滑出，右滑入
   register → login：右滑出，左滑入
   ============================================================ */
.auth-slide-left-enter-active,
.auth-slide-left-leave-active,
.auth-slide-right-enter-active,
.auth-slide-right-leave-active {
  transition:
    opacity 0.42s cubic-bezier(0.32, 0.72, 0, 1),
    transform 0.42s cubic-bezier(0.32, 0.72, 0, 1),
    filter 0.42s cubic-bezier(0.32, 0.72, 0, 1);
}

/* ── 正向：login → register ── */
.auth-slide-left-enter-from {
  opacity: 0;
  transform: translateX(80px) scale(0.96);
  filter: blur(6px);
}
.auth-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-80px) scale(0.96);
  filter: blur(6px);
}

/* ── 反向：register → login ── */
.auth-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-80px) scale(0.96);
  filter: blur(6px);
}
.auth-slide-right-leave-to {
  opacity: 0;
  transform: translateX(80px) scale(0.96);
  filter: blur(6px);
}

/* ============================================================
   通用淡入淡出 — Layout 进出 / 页面间切换
   ============================================================ */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
