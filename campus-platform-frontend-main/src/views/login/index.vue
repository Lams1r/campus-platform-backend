<template>
  <div class="login-container">
    <!-- 粒子背景 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <!-- 3D 浮动几何体 — 校园主题 -->
    <div class="floating-shapes">
      <!-- 学士帽 -->
      <svg class="shape grad-cap" viewBox="0 0 100 100" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="50,18 94,38 50,58 6,38" stroke="rgba(29,29,31,0.12)" stroke-width="1.2" fill="rgba(29,29,31,0.03)"/>
        <line x1="50" y1="38" x2="50" y2="72" stroke="rgba(29,29,31,0.1)" stroke-width="1.2"/>
        <path d="M27,44 L27,60 Q50,78 73,60 L73,44" stroke="rgba(29,29,31,0.1)" stroke-width="1.2" fill="rgba(29,29,31,0.02)"/>
        <circle cx="50" cy="75" r="2.5" stroke="rgba(29,29,31,0.1)" stroke-width="1" fill="rgba(29,29,31,0.04)"/>
        <line x1="80" y1="35" x2="80" y2="68" stroke="rgba(29,29,31,0.08)" stroke-width="1"/>
      </svg>

      <!-- 翻开的书 -->
      <svg class="shape open-book" viewBox="0 0 120 90" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <path d="M60,20 Q35,15 10,22 L10,75 Q35,68 60,74" stroke="rgba(29,29,31,0.12)" stroke-width="1.2" fill="rgba(29,29,31,0.02)"/>
        <path d="M60,20 Q85,15 110,22 L110,75 Q85,68 60,74" stroke="rgba(29,29,31,0.12)" stroke-width="1.2" fill="rgba(29,29,31,0.02)"/>
        <line x1="60" y1="20" x2="60" y2="74" stroke="rgba(29,29,31,0.1)" stroke-width="1"/>
        <line x1="20" y1="32" x2="50" y2="28" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
        <line x1="20" y1="42" x2="50" y2="38" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
        <line x1="20" y1="52" x2="50" y2="48" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
        <line x1="70" y1="28" x2="100" y2="32" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
        <line x1="70" y1="38" x2="100" y2="42" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
      </svg>

      <!-- 地球仪 -->
      <svg class="shape globe" viewBox="0 0 100 100" fill="none" stroke-linecap="round">
        <ellipse cx="50" cy="50" rx="40" ry="40" stroke="rgba(29,29,31,0.12)" stroke-width="1.2" fill="rgba(29,29,31,0.02)"/>
        <ellipse cx="50" cy="50" rx="18" ry="40" stroke="rgba(29,29,31,0.08)" stroke-width="0.8"/>
        <ellipse cx="50" cy="50" rx="40" ry="12" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
        <path d="M10,50 Q30,35 50,35 Q70,35 90,50" stroke="rgba(29,29,31,0.06)" stroke-width="0.8" fill="none"/>
        <path d="M10,50 Q30,65 50,65 Q70,65 90,50" stroke="rgba(29,29,31,0.06)" stroke-width="0.8" fill="none"/>
        <line x1="50" y1="10" x2="50" y2="90" stroke="rgba(29,29,31,0.06)" stroke-width="0.8"/>
      </svg>

      <!-- 原子模型 -->
      <svg class="shape atom" viewBox="0 0 100 100" fill="none" stroke-linecap="round">
        <ellipse cx="50" cy="50" rx="40" ry="16" stroke="rgba(29,29,31,0.1)" stroke-width="1" fill="none"/>
        <ellipse cx="50" cy="50" rx="40" ry="16" stroke="rgba(29,29,31,0.08)" stroke-width="1" fill="none" transform="rotate(60,50,50)"/>
        <ellipse cx="50" cy="50" rx="40" ry="16" stroke="rgba(29,29,31,0.06)" stroke-width="1" fill="none" transform="rotate(-60,50,50)"/>
        <circle cx="50" cy="50" r="4" stroke="rgba(29,29,31,0.12)" stroke-width="1.2" fill="rgba(29,29,31,0.06)"/>
      </svg>

      <!-- 钢笔 -->
      <svg class="shape pen" viewBox="0 0 80 100" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <line x1="25" y1="85" x2="55" y2="15" stroke="rgba(29,29,31,0.12)" stroke-width="2" />
        <polygon points="55,15 60,5 50,10" stroke="rgba(29,29,31,0.12)" stroke-width="1" fill="rgba(29,29,31,0.04)"/>
        <line x1="25" y1="85" x2="20" y2="95" stroke="rgba(29,29,31,0.1)" stroke-width="1.5"/>
        <line x1="25" y1="85" x2="30" y2="95" stroke="rgba(29,29,31,0.1)" stroke-width="1.5"/>
      </svg>
    </div>

    <div class="login-card glass-panel">
      <!-- Logo -->
      <div class="logo-area">
        <img src="@/assets/campus-logo.jpg" class="logo-icon" alt="智慧校园 logo">
        <h1 class="title">Campus Platform</h1>
        <p class="subtitle">一体化智慧校园管理系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="学号 / 工号 / 账号"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="密码"
            size="large"
          >
            <template #suffix>
              <el-icon class="eye-icon" @click="showPassword = !showPassword">
                <View v-if="showPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-wrapper">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="验证码"
              size="large"
              clearable
              class="captcha-input"
            />
            <div class="captcha-img" :class="{ loading: captchaLoading }" @click="refreshCaptcha">
              <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="点击刷新验证码">
              <el-icon v-else-if="captchaLoading" class="captcha-loading"><Loading /></el-icon>
              <span v-else class="refresh-text">刷新</span>
            </div>
          </div>
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <button type="button" class="text-link" @click="showForgotTip">忘记密码？</button>
        </div>

        <el-form-item>
          <el-button
            :loading="loading"
            size="large"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button
            size="large"
            class="goto-register-btn"
            @click="$router.push('/register')"
          >
            去注册
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { animate } from 'animejs'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(localStorage.getItem('rememberUsername') === '1')
const captchaImage = ref('')
const captchaLoading = ref(false)
const showPassword = ref(false)
const canvasRef = ref(null)
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

const loginForm = ref({
  username: '',
  password: '',
  captchaCode: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const refreshCaptcha = async () => {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    await authStore.fetchCaptcha()
    captchaImage.value = authStore.captchaImage
  } catch (error) {
    console.error('获取验证码失败', error)
    ElMessage.error('验证码加载失败，请检查后端服务')
  } finally {
    captchaLoading.value = false
  }
}

const showForgotTip = () => {
  ElMessage.info('请联系系统管理员重置密码')
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.loginAction(loginForm.value)
      if (rememberMe.value) {
        localStorage.setItem('rememberUsername', '1')
        localStorage.setItem('lastUsername', loginForm.value.username)
      } else {
        localStorage.removeItem('rememberUsername')
        localStorage.removeItem('lastUsername')
      }
      ElMessage.success('登录成功')
      router.push(route.query.redirect || '/')
    } catch (error) {
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

/* ============================================================
   粒子网络动画
   ============================================================ */
let animationId = null
let particles = []
let mouseX = -1000
let mouseY = -1000

class Particle {
  constructor(w, h) {
    this.reset(w, h)
  }
  reset(w, h) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.vx = (Math.random() - 0.5) * 0.3
    this.vy = (Math.random() - 0.5) * 0.3
    this.r = Math.random() * 1.5 + 0.5
    this.opacity = Math.random() * 0.3 + 0.1
  }
  update(w, h) {
    this.x += this.vx
    this.y += this.vy
    if (this.x < 0 || this.x > w) this.vx *= -1
    if (this.y < 0 || this.y > h) this.vy *= -1
  }
}

function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas || reduceMotion || window.innerWidth < 768) return
  const ctx = canvas.getContext('2d')

  function resize() {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  // 粒子数量根据屏幕大小动态调整
  const count = Math.min(Math.floor((canvas.width * canvas.height) / 12000), 120)
  particles = Array.from({ length: count }, () => new Particle(canvas.width, canvas.height))

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // 绘制连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          const alpha = (1 - dist / 150) * 0.08
          ctx.beginPath()
          ctx.strokeStyle = `rgba(29, 29, 31, ${alpha})`
          ctx.lineWidth = 0.5
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }

    // 鼠标附近连线
    for (const p of particles) {
      const dx = p.x - mouseX
      const dy = p.y - mouseY
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 200) {
        const alpha = (1 - dist / 200) * 0.15
        ctx.beginPath()
        ctx.strokeStyle = `rgba(29, 29, 31, ${alpha})`
        ctx.lineWidth = 0.8
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(mouseX, mouseY)
        ctx.stroke()
      }
    }

    // 绘制粒子
    for (const p of particles) {
      p.update(canvas.width, canvas.height)
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(29, 29, 31, ${p.opacity})`
      ctx.fill()
    }

    animationId = requestAnimationFrame(draw)
  }

  draw()

  // 鼠标追踪
  const handleMouseMove = (e) => { mouseX = e.clientX; mouseY = e.clientY }
  const handleMouseLeave = () => { mouseX = -1000; mouseY = -1000 }
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseleave', handleMouseLeave)

  return () => {
    window.removeEventListener('resize', resize)
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseleave', handleMouseLeave)
  }
}

let cleanupCanvas = null

const initFloatingShapes = () => {
  if (reduceMotion) return
  // 登录卡片入场
  animate('.login-card', {
    translateY: [40, 0],
    opacity: [0, 1],
    duration: 800,
    easing: 'easeOutCubic'
  })

  // 学士帽
  animate('.grad-cap', {
    translateY: [0, -20, -8, -28, 0],
    rotate: [-4, -2, -5, -3, -4],
    duration: 22000,
    loop: true,
    easing: 'easeInOutSine'
  })

  // 翻开的书
  animate('.open-book', {
    translateY: [0, -26, 0],
    scale: [1, 1.04, 1],
    duration: 20000,
    loop: true,
    easing: 'easeInOutSine'
  })

  // 地球仪
  animate('.globe', {
    translateY: [0, -18, -30, 0],
    rotate: [0, 3, -2, 0],
    duration: 18000,
    loop: true,
    easing: 'easeInOutSine'
  })

  // 原子模型
  animate('.atom', {
    translateY: [0, -16, 0],
    scale: [1, 1.06, 1],
    duration: 24000,
    loop: true,
    easing: 'easeInOutSine'
  })

  // 钢笔
  animate('.pen', {
    translateY: [0, -14, -6, -20, 0],
    rotate: [8, 10, 7, 9, 8],
    duration: 16000,
    loop: true,
    easing: 'easeInOutSine'
  })
}

onMounted(() => {
  if (rememberMe.value) {
    loginForm.value.username = localStorage.getItem('lastUsername') || ''
  }
  refreshCaptcha()
  cleanupCanvas = initCanvas()
  initFloatingShapes()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (cleanupCanvas) cleanupCanvas()
})
</script>

<style scoped>
/* ============================================================
   容器
   ============================================================ */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 18% 12%, rgba(37, 99, 235, 0.16), transparent 28%),
    radial-gradient(circle at 82% 78%, rgba(14, 165, 233, 0.16), transparent 30%),
    linear-gradient(135deg, #fafafc 0%, #eef4ff 46%, #f8fafc 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.login-card {
  width: 100%;
  max-width: 400px;
  position: relative;
  z-index: 10;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: saturate(180%) blur(24px);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--apple-radius-xl, 28px);
  padding: 48px 40px;
  box-shadow: var(--apple-shadow-soft, 0 8px 60px rgba(0, 0, 0, 0.06));
}

/* ============================================================
   3D 浮动几何体 — 校园主题
   ============================================================ */
.floating-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.shape {
  position: absolute;
  will-change: transform;
}

/* ─── 学士帽 ─── */
.grad-cap {
  top: 8%;
  left: 7%;
  width: 110px;
  height: 110px;
  filter: drop-shadow(0 8px 32px rgba(0,0,0,0.04));
  animation:
    grad-cap-float 22s ease-in-out infinite,
    grad-cap-rotate 40s linear infinite;
}

/* ─── 翻开的书 ─── */
.open-book {
  top: 10%;
  right: 6%;
  width: 130px;
  height: 100px;
  filter: drop-shadow(0 8px 32px rgba(0,0,0,0.04));
  animation:
    book-float 20s ease-in-out infinite,
    book-rotate 36s linear infinite;
}

/* ─── 地球仪 ─── */
.globe {
  bottom: 15%;
  right: 8%;
  width: 100px;
  height: 100px;
  filter: drop-shadow(0 8px 32px rgba(0,0,0,0.04));
  animation:
    globe-float 18s ease-in-out infinite,
    globe-rotate 30s linear infinite;
}

/* ─── 原子模型 ─── */
.atom {
  bottom: 12%;
  left: 6%;
  width: 120px;
  height: 120px;
  filter: drop-shadow(0 8px 32px rgba(0,0,0,0.04));
  animation:
    atom-float 24s ease-in-out infinite,
    atom-rotate 20s linear infinite;
}

/* ─── 钢笔 ─── */
.pen {
  top: 50%;
  right: 3%;
  width: 60px;
  height: 80px;
  filter: drop-shadow(0 8px 32px rgba(0,0,0,0.04));
  animation:
    pen-float 16s ease-in-out infinite,
    pen-rotate 32s linear infinite;
}

/* ============================================================
   浮动动画 — 每个元素独立轨迹
   ============================================================ */
@keyframes grad-cap-float {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  25%      { transform: translateY(-22px) rotate(-2deg); }
  50%      { transform: translateY(-8px) rotate(-5deg); }
  75%      { transform: translateY(-28px) rotate(-3deg); }
}

@keyframes grad-cap-rotate {
  from { transform: rotate3d(0.2, 1, 0.15, 0deg); }
  to   { transform: rotate3d(0.2, 1, 0.15, 360deg); }
}

@keyframes book-float {
  0%, 100% { transform: translateY(0) scale(1); }
  50%      { transform: translateY(-26px) scale(1.04); }
}

@keyframes book-rotate {
  from { transform: rotate3d(0.1, 1, 0.2, 0deg); }
  to   { transform: rotate3d(0.1, 1, 0.2, 360deg); }
}

@keyframes globe-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  33%      { transform: translateY(-18px) rotate(3deg); }
  66%      { transform: translateY(-30px) rotate(-2deg); }
}

@keyframes globe-rotate {
  from { transform: rotate3d(0.4, 1, 0.2, 0deg); }
  to   { transform: rotate3d(0.4, 1, 0.2, 360deg); }
}

@keyframes atom-float {
  0%, 100% { transform: translateY(0) scale(1); }
  50%      { transform: translateY(-16px) scale(1.06); }
}

@keyframes atom-rotate {
  from { transform: rotate3d(0.3, 1, 0.4, 0deg); }
  to   { transform: rotate3d(0.3, 1, 0.4, 360deg); }
}

@keyframes pen-float {
  0%, 100% { transform: translateY(0) rotate(8deg); }
  25%      { transform: translateY(-14px) rotate(10deg); }
  50%      { transform: translateY(-6px) rotate(7deg); }
  75%      { transform: translateY(-20px) rotate(9deg); }
}

@keyframes pen-rotate {
  from { transform: rotate3d(0.1, 1, 0.3, 0deg); }
  to   { transform: rotate3d(0.1, 1, 0.3, 360deg); }
}

/* ============================================================
   Logo
   ============================================================ */
.logo-area {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 76px;
  height: 76px;
  margin-bottom: 16px;
  border-radius: 24px;
  object-fit: cover;
  object-position: center;
  background: #ffffff;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.14);
}

.title {
  font-family: var(--apple-font-display, "SF Pro Display", system-ui, -apple-system, sans-serif);
  font-size: 34px;
  font-weight: 600;
  letter-spacing: -0.374px;
  line-height: 1.1;
  color: var(--apple-ink, #1d1d1f);
  margin: 0 0 6px;
}

.subtitle {
  font-size: 17px;
  font-weight: 400;
  color: var(--apple-ink-muted-48, #7a7a7a);
  margin: 0;
  letter-spacing: -0.374px;
}

/* ============================================================
   表单
   ============================================================ */
.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 9999px !important;
  padding: 4px 20px;
  height: 48px;
  background: rgba(245, 245, 247, 0.8);
  box-shadow: none !important;
}

.login-form :deep(.el-input__wrapper:hover) {
  background: rgba(236, 236, 238, 0.9);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 2px var(--apple-ink, #1d1d1f) inset !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 17px;
  letter-spacing: -0.374px;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 48px;
  min-width: 120px;
  border-radius: var(--apple-radius-sm, 8px);
  overflow: hidden;
  cursor: pointer;
  background: rgba(245, 245, 247, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(224, 224, 224, 0.5);
  transition: border-color 0.2s, background 0.2s, transform 0.2s;
}

.captcha-img:hover {
  border-color: var(--apple-chip, #d2d2d7);
  transform: translateY(-1px);
}

.captcha-img.loading {
  cursor: wait;
  background: rgba(37, 99, 235, 0.08);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.refresh-text {
  color: var(--apple-ink-muted-48, #7a7a7a);
  font-size: 12px;
}

.captcha-loading {
  color: var(--apple-primary, #2563eb);
  animation: captcha-spin 0.8s linear infinite;
}

@keyframes captcha-spin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  font-size: 14px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  letter-spacing: -0.224px;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--apple-ink, #1d1d1f);
  border-color: var(--apple-ink, #1d1d1f);
}

.text-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--apple-link, #0066cc);
  text-decoration: none;
  font-size: 14px;
  letter-spacing: -0.224px;
  transition: color 0.2s;
  cursor: pointer;
  font-family: var(--apple-font-body, system-ui);
}

.text-link:hover {
  color: #0071e3;
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: -0.374px;
  border-radius: 9999px !important;
  background: linear-gradient(135deg, var(--apple-primary, #2563eb), #0ea5e9) !important;
  border: none !important;
  color: #ffffff !important;
  box-shadow: var(--apple-glow-primary, 0 20px 70px rgba(37, 99, 235, 0.22));
}

.login-btn:hover {
  filter: brightness(1.04);
}

.login-btn:active {
  transform: scale(0.97);
}

.goto-register-btn {
  width: 100%;
  height: 48px;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: -0.374px;
  border-radius: 9999px !important;
  background: rgba(255, 255, 255, 0.52) !important;
  border: 1px solid var(--apple-hairline, #e0e0e0) !important;
  color: var(--apple-ink, #1d1d1f) !important;
}

.goto-register-btn:hover {
  background: var(--apple-surface-strong, #ffffff) !important;
}

.goto-register-btn:active {
  transform: scale(0.97);
}

.eye-icon {
  cursor: pointer;
  color: var(--apple-ink-muted-48, #7a7a7a);
  transition: color 0.2s;
}

.eye-icon:hover {
  color: var(--apple-ink, #1d1d1f);
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 833px) {
  .floating-shapes,
  .particle-canvas { display: none; }
}

@media (max-width: 480px) {
  .login-container {
    align-items: flex-start;
    padding: 18px 12px;
  }

  .login-card {
    max-width: 100%;
    padding: 28px 20px;
    border-radius: 24px;
  }

  .title {
    font-size: 27px;
  }

  .logo-icon {
    width: 68px;
    height: 68px;
  }

  .captcha-wrapper {
    gap: 8px;
  }

  .captcha-img {
    width: 104px;
    min-width: 104px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .floating-shapes,
  .particle-canvas {
    display: none;
  }
}
</style>
