import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'

let isRelogin = false

const miniRequest = axios.create({
  baseURL: import.meta.env.VITE_APP_MINI_API,
  timeout: 60000
})

miniRequest.interceptors.request.use((config: any) => {
  if (getToken()) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${getToken()}`
  }
  return config
})

miniRequest.interceptors.response.use(
  (res) => {
    const code = res.data?.code ?? 200
    const msg = res.data?.msg || '请求失败'
    if (code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(msg))
    }
    if (code !== 200) {
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return Promise.resolve(res.data)
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '小程序业务接口异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (isRelogin) return
  isRelogin = true
  ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isRelogin = false
    useUserStore().logOut().then(() => {
      location.href = '/index'
    })
  }).catch(() => {
    isRelogin = false
  })
}

export interface MiniPageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function miniPreviewUrl(assetId?: string | number | null) {
  if (assetId == null || assetId === '') return ''
  return `${import.meta.env.VITE_APP_MINI_API}/api/files/preview/${assetId}`
}

export default miniRequest
