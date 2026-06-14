import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 创建 axios 实例
// baseURL 由环境变量控制:
//   开发:.env.development -> /api(走 vite proxy)
//   生产:.env.production  -> /api(走 nginx 同域反代)
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const ISO_DATE_TIME_RE = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$/

const normalizeDateTimeText = (value) => {
  if (typeof value !== 'string') return value
  return value.replace(ISO_DATE_TIME_RE, '$1 $2')
}

const normalizeResponseData = (value) => {
  if (Array.isArray(value)) {
    return value.map(normalizeResponseData)
  }

  if (value && typeof value === 'object') {
    Object.keys(value).forEach((key) => {
      value[key] = normalizeResponseData(value[key])
    })
    return value
  }

  return normalizeDateTimeText(value)
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers['Authorization'] = authStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data
    
    // 成功响应
    if (code === 200) {
      return normalizeResponseData(data)
    }
    
    // 处理特定错误码
    switch (code) {
      case 401: {
        ElMessage.error('登录已过期，请重新登录')
        const authStore = useAuthStore()
        authStore.logoutAction()
        router.push('/login')
        break
      }
      case 403:
        ElMessage.error('没有操作权限')
        break
      case 400:
        ElMessage.error(msg || '请求参数错误')
        break
      case 500:
        ElMessage.error(msg || '服务器内部错误')
        break
      default:
        ElMessage.error(msg || '未知错误')
    }
    
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    const { response } = error
    
    if (response) {
      switch (response.status) {
        case 401: {
          ElMessage.error('登录已过期，请重新登录')
          const authStore = useAuthStore()
          authStore.logoutAction()
          router.push('/login')
          break
        }
        case 403:
          ElMessage.error('没有操作权限')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.msg || '网络请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    
    return Promise.reject(error)
  }
)

export default request
