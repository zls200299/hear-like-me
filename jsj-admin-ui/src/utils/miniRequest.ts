import axios from 'axios'
import { ElMessage } from 'element-plus'

const miniRequest = axios.create({
  baseURL: import.meta.env.VITE_APP_MINI_API,
  timeout: 60000
})

miniRequest.interceptors.response.use(
  (res) => {
    const code = res.data?.code ?? 200
    const msg = res.data?.msg || '请求失败'
    if (code !== 200) {
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return Promise.resolve(res.data)
  },
  (error) => {
    ElMessage.error(error.message || '小程序业务接口异常')
    return Promise.reject(error)
  }
)

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
