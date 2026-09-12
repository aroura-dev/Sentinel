import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth, readRole } from '../utils/authStorage'
import { landingFor } from '../utils/menu'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截：附加 Bearer token（读取两处存储，兼容记住我/会话）
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截：统一解包 BasicResultVO { status, data, msg }
request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data && (data.status === '0' || data.status === 0)) {
      return data.data
    }
    const msg = (data && data.msg) || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      clearAuth()
      ElMessage.warning('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (err.response && err.response.status === 403) {
      ElMessage.warning('无权限访问该资源')
      const home = landingFor(readRole())
      if (window.location.pathname !== home) {
        window.location.href = home
      }
    } else {
      ElMessage.error(err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default request
