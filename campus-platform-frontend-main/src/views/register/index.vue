<template>
  <div class="register-container">
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

    <div class="register-card">
      <!-- Logo -->
      <div class="logo-area">
        <img src="@/assets/campus-logo.jpg" class="logo-icon" alt="智慧校园 logo">
        <h1 class="title">Campus Platform</h1>
        <p class="subtitle">填写信息完成注册</p>
      </div>

      <!-- 注册表单 -->
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="学号 / 工号 / 账号" size="large" clearable />
        </el-form-item>

        <el-form-item prop="realName">
          <el-input v-model="registerForm.realName" placeholder="真实姓名" size="large" clearable />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号" size="large" clearable />
        </el-form-item>

        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" size="large" clearable />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
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

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="确认密码"
            size="large"
          >
            <template #suffix>
              <el-icon class="eye-icon" @click="showConfirmPassword = !showConfirmPassword">
                <View v-if="showConfirmPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="userType">
          <el-select v-model="registerForm.userType" placeholder="选择角色" size="large" style="width: 100%;">
            <el-option label="学生" :value="0" />
            <el-option label="教师" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item prop="deptName">
          <el-input v-model="registerForm.deptName" placeholder="院系 / 部门" size="large" clearable />
        </el-form-item>

        <el-form-item v-if="registerForm.userType === 0" prop="className">
          <el-input v-model="registerForm.className" placeholder="班级名称" size="large" clearable />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-wrapper">
            <el-input v-model="registerForm.captchaCode" placeholder="验证码" size="large" clearable class="captcha-input" />
            <div class="captcha-img" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码">
              <span v-else class="refresh-text">刷新</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item prop="agreement">
          <el-checkbox v-model="registerForm.agreement">
            <span class="agreement-text">
              我已阅读并同意
              <a href="#" class="text-link">用户协议</a>
              和
              <a href="#" class="text-link">隐私政策</a>
            </span>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button :loading="loading" size="large" class="register-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button size="large" class="goto-login-btn" @click="$router.push('/login')">
            去登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const registerFormRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const captchaImage = ref('')
const canvasRef = ref(null)

const registerForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  userType: undefined,
  deptName: '',
  className: '',
  captchaCode: '',
  agreement: false
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validateAgreement = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请阅读并同意用户协议'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
    return
  }
  const reg = /^1[3-9]\d{9}$/
  if (!reg.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入学号/工号/账号', trigger: 'blur' },
    { min: 4, max: 50, message: '账号长度4-50位', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  agreement: [
    { validator: validateAgreement, trigger: 'change' }
  ]
}

const refreshCaptcha = async () => {
  try {
    await authStore.fetchCaptcha()
    captchaImage.value = authStore.captchaImage
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const payload = {
        username: registerForm.username,
        realName: registerForm.realName,
        password: registerForm.password,
        phone: registerForm.phone,
        email: registerForm.email,
        userType: registerForm.userType,
        deptName: registerForm.deptName,
        className: registerForm.userType === 0 ? registerForm.className : undefined,
        captchaKey: authStore.captchaKey,
        captchaCode: registerForm.captchaCode
      }
      await authStore.registerAction(payload)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败', error)
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
  constructor(w, h) { this.reset(w, h) }
  reset(w, h) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.vx = (Math.random() - 0.5) * 0.3
    this.vy = (Math.random() - 0.5) * 0.3
    this.r = Math.random() * 1.5 + 0.5
    this.opacity = Math.random() * 0.3 + 0.1
  }
  update(w, h) {
    this.x += this.vx; this.y += this.vy
    if (this.x < 0 || this.x > w) this.vx *= -1
    if (this.y < 0 || this.y > h) this.vy *= -1
  }
}

function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  function resize() { canvas.width = window.innerWidth; canvas.height = window.innerHeight }
  resize()
  window.addEventListener('resize', resize)

  const count = Math.min(Math.floor((canvas.width * canvas.height) / 12000), 120)
  particles = Array.from({ length: count }, () => new Particle(canvas.width, canvas.height))

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x, dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          ctx.beginPath()
          ctx.strokeStyle = `rgba(29, 29, 31, ${(1 - dist / 150) * 0.08})`
          ctx.lineWidth = 0.5
          ctx.moveTo(particles[i].x, particles[i].y); ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }
    for (const p of particles) {
      const dx = p.x - mouseX, dy = p.y - mouseY, dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 200) {
        ctx.beginPath()
        ctx.strokeStyle = `rgba(29, 29, 31, ${(1 - dist / 200) * 0.15})`
        ctx.lineWidth = 0.8
        ctx.moveTo(p.x, p.y); ctx.lineTo(mouseX, mouseY); ctx.stroke()
      }
    }
    for (const p of particles) {
      p.update(canvas.width, canvas.height)
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(29, 29, 31, ${p.opacity})`; ctx.fill()
    }
    animationId = requestAnimationFrame(draw)
  }
  draw()

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

onMounted(() => { refreshCaptcha(); cleanupCanvas = initCanvas() })
onUnmounted(() => { if (animationId) cancelAnimationFrame(animationId); if (cleanupCanvas) cleanupCanvas() })
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fafafc 0%, #f0f0f2 50%, #e8e8ea 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  z-index: 0; pointer-events: none;
}

.register-card {
  width: 100%; max-width: 420px;
  position: relative; z-index: 10;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(24px);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px; padding: 40px 36px;
  box-shadow: 0 8px 60px rgba(0, 0, 0, 0.06);
  max-height: 95vh; overflow-y: auto;
}

/* 3D 浮动几何体 — 校园主题 */
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

/* 学士帽 */
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

/* 翻开的书 */
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

/* 地球仪 */
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

/* 原子模型 */
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

/* 钢笔 */
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

/* 浮动动画 */
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

/* Logo */
.logo-area { text-align: center; margin-bottom: 28px; }
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
  font-size: 34px; font-weight: 600; letter-spacing: -0.374px; line-height: 1.1;
  color: var(--apple-ink, #1d1d1f); margin: 0 0 6px;
}
.subtitle { font-size: 17px; color: var(--apple-ink-muted-48, #7a7a7a); margin: 0; letter-spacing: -0.374px; }

/* 表单 */
.register-form :deep(.el-form-item) { margin-bottom: 16px; }
.register-form :deep(.el-input__wrapper) {
  border-radius: 9999px !important; padding: 4px 20px; height: 46px;
  background: rgba(245,245,247,0.8); box-shadow: none !important;
}
.register-form :deep(.el-input__wrapper:hover) { background: rgba(236,236,238,0.9); }
.register-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255,255,255,0.9); box-shadow: 0 0 0 2px var(--apple-ink,#1d1d1f) inset !important;
}
.register-form :deep(.el-input__inner) { font-size: 17px; letter-spacing: -0.374px; }
.register-form :deep(.el-select .el-input__wrapper) { border-radius: 9999px !important; }

.captcha-wrapper { display: flex; gap: 12px; align-items: center; width: 100%; }
.captcha-input { flex: 1; }
.captcha-img {
  width: 120px; height: 46px; min-width: 120px; border-radius: 8px; overflow: hidden;
  cursor: pointer; background: rgba(245,245,247,0.8); display: flex; align-items: center;
  justify-content: center; border: 1px solid rgba(224,224,224,0.5); transition: border-color 0.2s;
}
.captcha-img:hover { border-color: var(--apple-chip, #d2d2d7); }
.captcha-img img { width: 100%; height: 100%; object-fit: contain; }
.refresh-text { color: var(--apple-ink-muted-48, #7a7a7a); font-size: 12px; }
.agreement-text { font-size: 13px; color: var(--apple-ink-muted-48, #7a7a7a); }

.register-form :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--apple-ink, #1d1d1f); border-color: var(--apple-ink, #1d1d1f);
}
.register-btn {
  width: 100%; height: 46px; font-size: 17px; font-weight: 400; letter-spacing: -0.374px;
  border-radius: 9999px !important;
  background: linear-gradient(135deg, var(--apple-primary, #2563eb), #0ea5e9) !important;
  border: none !important;
  color: #ffffff !important;
  box-shadow: var(--apple-glow-primary, 0 20px 70px rgba(37, 99, 235, 0.22));
}
.register-btn:hover {
  filter: brightness(1.04);
}
.register-btn:active { transform: scale(0.97); }

.goto-login-btn {
  width: 100%; height: 46px; font-size: 17px; font-weight: 400; letter-spacing: -0.374px;
  border-radius: 9999px !important;
  background: rgba(255, 255, 255, 0.52) !important;
  border: 1px solid var(--apple-hairline, #e0e0e0) !important;
  color: var(--apple-ink, #1d1d1f) !important;
}
.goto-login-btn:hover { background: var(--apple-surface-strong, #ffffff) !important; }
.goto-login-btn:active { transform: scale(0.97); }

.eye-icon { cursor: pointer; color: var(--apple-ink-muted-48, #7a7a7a); }
.eye-icon:hover { color: var(--apple-ink, #1d1d1f); }

@media (max-width: 833px) {
  .floating-shapes,
  .particle-canvas { display: none; }
}
@media (max-width: 480px) {
  .register-container {
    align-items: flex-start;
    padding: 18px 12px;
  }

  .register-card {
    max-width: 100%;
    padding: 28px 20px;
    border-radius: 24px;
  }

  .title { font-size: 27px; }

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
</style>
