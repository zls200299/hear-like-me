import { getToken, clearAuth } from './auth'
import { getClientType } from './platform'
import config from '@/config'

const request = (options) => {
  const { url, method = 'POST', data, header = {} } = options

  const token = getToken()
  const defaultHeader = {
    'content-type': 'application/json',
    'X-Client-Type': getClientType(),
    'X-App-Version': config.appVersion
  }
  if (token) {
    defaultHeader['Authorization'] = `Bearer ${token}`
    defaultHeader['token'] = token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: config.baseUrl + url,
      method,
      data,
      header: { ...defaultHeader, ...header },
      success: (res) => {
        const { statusCode, data: resData } = res
        if (statusCode === 200) {
          if (resData.code === 200) {
            resolve(resData.data)
          } else if (resData.code === 401) {
            clearAuth()
            uni.reLaunch({ url: '/pages/login/index' })
            reject(resData)
          } else {
            uni.showToast({ title: resData.msg || '请求失败', icon: 'none' })
            reject(resData)
          }
        } else {
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })

export default request
