const { request } = require('./request.js')
const { uploadImage } = require('./file.js')

function getAppSafe() {
  try {
    return getApp()
  } catch (e) {
    return null
  }
}

function readStoredUserInfo() {
  try {
    return wx.getStorageSync('userInfo') || null
  } catch (e) {
    return null
  }
}

function saveSession(data) {
  const app = getAppSafe()
  const token = data.token || ''
  const userId = data.userId != null ? String(data.userId) : ''
  const userInfo = {
    userId,
    nickname: data.nickname || '',
    avatar: data.avatar || ''
  }

  if (app) {
    app.globalData.token = token
    app.globalData.userId = userId
    app.globalData.userInfo = userInfo
  }

  wx.setStorageSync('token', token)
  wx.setStorageSync('userId', userId)
  wx.setStorageSync('userInfo', userInfo)
  return userInfo
}

function clearSession() {
  const app = getAppSafe()
  if (app) {
    app.globalData.token = ''
    app.globalData.userId = ''
    app.globalData.userInfo = null
  }
  wx.removeStorageSync('token')
  wx.removeStorageSync('userId')
  wx.removeStorageSync('userInfo')
}

function getSession() {
  const app = getAppSafe()
  const token = (app && app.globalData.token) || wx.getStorageSync('token') || ''
  const userId = (app && app.globalData.userId) || wx.getStorageSync('userId') || ''
  const userInfo = (app && app.globalData.userInfo) || readStoredUserInfo() || null
  return { token, userId, userInfo }
}

function isLoggedIn() {
  return !!getSession().token
}

function wxLogin(code) {
  return request({
    url: '/api/auth/wx-login',
    method: 'POST',
    data: { code },
    skipAuth: true
  }).then((body) => body.data)
}

function getCurrentUser() {
  return request({
    url: '/api/auth/current-user',
    method: 'GET'
  }).then((body) => body.data)
}

function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'POST',
    data: {}
  }).finally(() => {
    clearSession()
  })
}

function wxLoginWithProfile({ nickname, avatarPath }) {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (loginRes) => resolve(loginRes),
      fail: (err) => reject(new Error(err.errMsg || '微信登录失败'))
    })
  }).then(async (loginRes) => {
    if (!loginRes.code) {
      throw new Error('获取微信登录凭证失败')
    }

    let avatarUrl = avatarPath || ''
    if (avatarUrl && !/^https?:\/\//i.test(avatarUrl)) {
      const uploaded = await uploadImage(avatarUrl)
      avatarUrl = uploaded.url || ''
    }

    const body = await request({
      url: '/api/auth/wx-login',
      method: 'POST',
      data: {
        code: loginRes.code,
        nickname: nickname || '',
        avatar: avatarUrl
      },
      skipAuth: true
    })

    const data = body.data || {}
    const userInfo = saveSession(data)
    return userInfo
  })
}

module.exports = {
  saveSession,
  clearSession,
  getSession,
  isLoggedIn,
  wxLogin,
  wxLoginWithProfile,
  getCurrentUser,
  logout
}
