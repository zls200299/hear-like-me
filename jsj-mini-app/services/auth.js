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

function readLastProfile() {
  try {
    return wx.getStorageSync('lastUserProfile') || null
  } catch (e) {
    return null
  }
}

function saveLastProfile(userInfo) {
  if (!userInfo) return
  const profile = {
    nickname: userInfo.nickname || '',
    avatar: userInfo.avatar || ''
  }
  if (!profile.nickname && !profile.avatar) return
  try {
    wx.setStorageSync('lastUserProfile', profile)
  } catch (e) {
    // ignore
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
  saveLastProfile(userInfo)
  return userInfo
}

function saveUserProfile(data) {
  const app = getAppSafe()
  const session = getSession()
  const userId = data.userId != null ? String(data.userId) : String(session.userId || '')
  const userInfo = {
    userId,
    nickname: data.nickname || '',
    avatar: data.avatar || ''
  }

  if (app) {
    app.globalData.userId = userId
    app.globalData.userInfo = userInfo
  }

  wx.setStorageSync('userId', userId)
  wx.setStorageSync('userInfo', userInfo)
  saveLastProfile(userInfo)
  return userInfo
}

function clearSession() {
  const app = getAppSafe()
  const cached = readStoredUserInfo()
  if (cached) {
    saveLastProfile(cached)
  }
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

/**
 * 确保已登录，可在任意业务页复用。
 * - 已有本地 token：直接返回 session
 * - 否则尝试静默微信登录并落库 token
 * - 仍失败：弹窗引导去「我的」登录，并 reject（code=NEED_LOGIN）
 *
 * @param {Object} [options]
 * @param {string} [options.tip] 引导文案
 * @returns {Promise<{token:string,userId:string,userInfo:Object|null}>}
 */
function ensureLogin(options = {}) {
  const tip = options.tip || '请先登录后再继续'

  if (isLoggedIn()) {
    return Promise.resolve(getSession())
  }

  return wxLoginSilent()
    .then(({ data }) => {
      saveSession(data || {})
      if (!isLoggedIn()) {
        throw Object.assign(new Error(tip), { code: 'NEED_LOGIN' })
      }
      return getSession()
    })
    .catch((err) => {
      if (err && err.code === 'NEED_LOGIN') {
        return promptGoLogin(tip).then(() => Promise.reject(err))
      }
      return promptGoLogin(tip).then(() =>
        Promise.reject(Object.assign(new Error(tip), { code: 'NEED_LOGIN', cause: err }))
      )
    })
}

function promptGoLogin(tip) {
  return new Promise((resolve) => {
    wx.showModal({
      title: '需要登录',
      content: tip,
      confirmText: '去登录',
      cancelText: '取消',
      success(res) {
        if (res.confirm) {
          wx.switchTab({ url: '/pages/profile/index' })
        }
        resolve()
      },
      fail() {
        resolve()
      }
    })
  })
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

function wxLoginSilent() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (loginRes) => resolve(loginRes),
      fail: (err) => reject(new Error(err.errMsg || '微信登录失败'))
    })
  }).then(async (loginRes) => {
    if (!loginRes.code) {
      throw new Error('获取微信登录凭证失败')
    }

    const body = await request({
      url: '/api/auth/wx-login',
      method: 'POST',
      data: { code: loginRes.code },
      skipAuth: true
    })

    const data = body.data || {}
    return {
      newUser: !!data.newUser,
      data
    }
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

    const body = await request({
      url: '/api/auth/wx-login',
      method: 'POST',
      data: {
        code: loginRes.code,
        nickname: nickname || ''
      },
      skipAuth: true
    })

    const data = body.data || {}
    const userInfo = saveSession(data)
    if (!avatarPath) return userInfo

    return updateProfile({
      nickname: userInfo.nickname || nickname,
      avatarPath
    })
  })
}

async function updateProfile({ nickname, avatarPath }) {
  let avatarUrl = avatarPath || ''
  if (avatarUrl && !/^https?:\/\//i.test(avatarUrl)) {
    const uploaded = await uploadImage(avatarUrl)
    avatarUrl = uploaded.url || ''
  }

  const body = await request({
    url: '/api/auth/profile',
    method: 'POST',
    data: {
      nickname: nickname || '',
      avatar: avatarUrl
    }
  })
  return saveUserProfile(body.data || {})
}

module.exports = {
  saveSession,
  saveUserProfile,
  clearSession,
  getSession,
  isLoggedIn,
  ensureLogin,
  wxLogin,
  wxLoginSilent,
  wxLoginWithProfile,
  updateProfile,
  getCurrentUser,
  logout,
  readLastProfile
}
