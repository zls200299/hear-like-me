const {
  getSession,
  isLoggedIn,
  wxLoginWithProfile,
  getCurrentUser,
  logout
} = require('../../services/auth.js')

const DEFAULT_AVATAR = '/assets/icons/user.svg'

Page({
  data: {
    loggedIn: false,
    userId: '',
    nickname: '',
    avatarUrl: '',
    defaultAvatar: DEFAULT_AVATAR,
    loggingIn: false,
    loggingOut: false
  },

  onShow() {
    this._syncLoginState()
  },

  _syncLoginState() {
    const { userId, userInfo } = getSession()
    const loggedIn = isLoggedIn()
    this.setData({
      loggedIn,
      userId: userId || (userInfo && userInfo.userId) || '',
      nickname: (userInfo && userInfo.nickname) || '',
      avatarUrl: (userInfo && userInfo.avatar) || ''
    })

    if (loggedIn) {
      this._refreshCurrentUser()
    }
  },

  _refreshCurrentUser() {
    getCurrentUser()
      .then((user) => {
        if (!user) return
        this.setData({
          userId: user.userId != null ? String(user.userId) : this.data.userId,
          nickname: user.nickname || this.data.nickname,
          avatarUrl: user.avatar || this.data.avatarUrl
        })
      })
      .catch(() => {
        // 401 等由 request 层处理
      })
  },

  onChooseAvatar(e) {
    const avatarUrl = e.detail && e.detail.avatarUrl
    if (!avatarUrl) return
    this.setData({ avatarUrl })
  },

  onNicknameInput(e) {
    this.setData({
      nickname: (e.detail && e.detail.value) || ''
    })
  },

  onLogin() {
    if (this.data.loggingIn) return

    const nickname = (this.data.nickname || '').trim()
    if (!nickname) {
      wx.showToast({ title: '请先填写昵称', icon: 'none' })
      return
    }

    this.setData({ loggingIn: true })
    wxLoginWithProfile({
      nickname,
      avatarPath: this.data.avatarUrl
    })
      .then((userInfo) => {
        this.setData({
          loggedIn: true,
          userId: userInfo.userId || '',
          nickname: userInfo.nickname || nickname,
          avatarUrl: userInfo.avatar || this.data.avatarUrl,
          loggingIn: false
        })
        wx.showToast({ title: '登录成功', icon: 'success' })
      })
      .catch((err) => {
        this.setData({ loggingIn: false })
        wx.showToast({
          title: (err && err.message) ? err.message : '登录失败',
          icon: 'none'
        })
      })
  },

  onLogout() {
    if (this.data.loggingOut) return

    this.setData({ loggingOut: true })
    logout()
      .catch(() => {
        // 即使接口失败也清本地态
      })
      .finally(() => {
        this.setData({
          loggedIn: false,
          userId: '',
          nickname: '',
          avatarUrl: '',
          loggingOut: false
        })
        wx.showToast({ title: '已退出登录', icon: 'none' })
      })
  }
})
