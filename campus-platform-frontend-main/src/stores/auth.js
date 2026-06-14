import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, getUserInfo, getCaptcha, userRegister } from '@/api/auth'
import { useAppStore } from './app'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const permissions = ref([])
  const roles = ref([])
  const captchaKey = ref('')
  const captchaImage = ref('')

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const userType = computed(() => userInfo.value?.userType ?? -1)
  const isAdmin = computed(() => userType.value === 2)
  const isTeacher = computed(() => userType.value === 1)
  const isStudent = computed(() => userType.value === 0)
  const userRole = computed(() => {
    if (isAdmin.value) return 'admin'
    if (isTeacher.value) return 'teacher'
    return 'student'
  })

  // Actions
  // 获取验证码
  async function fetchCaptcha() {
    const res = await getCaptcha()
    captchaKey.value = res.captchaKey
    captchaImage.value = res.captchaImage
    console.log('[验证码] captchaKey:', res.captchaKey)
  }

  // 登录
  async function loginAction(loginData) {
    const data = await login({
      ...loginData,
      captchaKey: captchaKey.value
    })
    token.value = data.token
    localStorage.setItem('token', data.token)
    await fetchUserInfo()
    return data
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const data = await getUserInfo()
    userInfo.value = data.user
    permissions.value = data.permissions || []
    roles.value = data.roles || []
    return data
  }

  // 注册（适配 sys/user 新增用户接口）
  async function registerAction(data) {
    return await userRegister(data)
  }

  // 登出
  async function logoutAction() {
    try {
      await logout()
    } finally {
      resetState()
      router.push('/login')
    }
  }

  // 重置状态
  function resetState() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    roles.value = []
    localStorage.removeItem('token')
    // 清除过滤后的路由，下次登录重新生成
    const appStore = useAppStore()
    appStore.setPermissionRoutes([])
  }

  return {
    token,
    userInfo,
    permissions,
    roles,
    captchaKey,
    captchaImage,
    isLoggedIn,
    username,
    nickname,
    avatar,
    userType,
    isAdmin,
    isTeacher,
    isStudent,
    userRole,
    fetchCaptcha,
    loginAction,
    registerAction,
    fetchUserInfo,
    logoutAction,
    resetState
  }
})
