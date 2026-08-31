const config = require('../config.js')

function clearStoredSession() {
  try {
    const app = getApp()
    app.globalData.token = ''
    app.globalData.userId = ''
    app.globalData.userInfo = null
  } catch (e) {
    // app 未就绪时忽略
  }
  wx.removeStorageSync('token')
  wx.removeStorageSync('userId')
  wx.removeStorageSync('userInfo')
}

function buildAuthHeaders(skipAuth) {
  const headers = {}
  if (skipAuth) return headers

  let token = ''
  let userId = ''
  try {
    const app = getApp()
    token = app.globalData.token || wx.getStorageSync('token') || ''
    userId = app.globalData.userId != null
      ? String(app.globalData.userId)
      : (wx.getStorageSync('userId') || '')
  } catch (e) {
    token = wx.getStorageSync('token') || ''
    userId = wx.getStorageSync('userId') || ''
  }

  if (userId) {
    headers.userId = userId
  }
  if (token) {
    headers.token = token
    headers.Authorization = token
  }
  return headers
}

/**
 * 封装 wx.request
 * @param {Object} options
 * @param {string} options.url - 相对路径，如 /api/scenarios
 * @param {string} [options.method='GET']
 * @param {Object} [options.data]
 * @param {Object} [options.header]
 * @param {boolean} [options.skipAuth=false]
 * @returns {Promise<*>}
 */
function request(options = {}) {
  const { url, method = 'GET', data, header = {}, skipAuth = false } = options

  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + url,
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...buildAuthHeaders(skipAuth),
        ...header
      },
      success(res) {
        if (res.statusCode === 401) {
          clearStoredSession()
          reject({
            message: '登录已过期，请重新登录',
            code: 401,
            statusCode: 401,
            response: res
          })
          return
        }

        if (res.statusCode !== 200) {
          reject({
            message: `HTTP ${res.statusCode}`,
            statusCode: res.statusCode,
            response: res
          })
          return
        }

        const body = res.data
        if (body && typeof body.code === 'number') {
          if (body.code === 401) {
            clearStoredSession()
            reject({
              message: body.msg || '登录已过期，请重新登录',
              code: 401,
              response: body
            })
            return
          }
          if (body.code === 200) {
            resolve(body)
            return
          }
          reject({
            message: body.msg || '请求失败',
            code: body.code,
            response: body
          })
          return
        }

        resolve(body)
      },
      fail(err) {
        reject({
          message: err.errMsg || '网络请求失败',
          error: err
        })
      }
    })
  })
}

module.exports = {
  request
}
