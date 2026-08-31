const {
  getSession,
  isLoggedIn,
  wxLoginWithProfile,
  getCurrentUser,
  logout
} = require('../../services/auth.js')

const { ICONS } = require('../../assets/icons/index.js')

const MENU_ITEMS = [
  { key: 'account', icon: 'profile-menu-account', label: '账号信息' },
  { key: 'about', icon: 'profile-menu-about', label: '关于项目' },
  { key: 'guide', icon: 'profile-menu-guide', label: '使用说明' },
  { key: 'feedback', icon: 'profile-menu-feedback', label: '意见反馈' },
  { key: 'privacy', icon: 'profile-menu-privacy', label: '隐私说明' }
]

Page({
  data: {
    loggedIn: false,
    userId: '',
    nickname: '',
    avatarUrl: '',
    defaultAvatar: ICONS.PROFILE_AVATAR_PLACEHOLDER,
    loggingIn: false,
    loggingOut: false,
    loginSheetVisible: false,
    menuItems: MENU_ITEMS
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.PROFILE)
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
      .catch(() => {})
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

  onWechatLoginTap() {
    if (this.data.loggedIn || this.data.loggingIn) return
    this.setData({ loginSheetVisible: true })
  },

  onCloseLoginSheet() {
    if (this.data.loggingIn) return
    this.setData({ loginSheetVisible: false })
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
          loggingIn: false,
          loginSheetVisible: false
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
      .catch(() => {})
      .finally(() => {
        this.setData({
          loggedIn: false,
          userId: '',
          nickname: '',
          avatarUrl: '',
          loggingOut: false,
          loginSheetVisible: false
        })
        wx.showToast({ title: '已退出登录', icon: 'none' })
      })
  },

  onMenuTap(e) {
    const key = e.currentTarget.dataset && e.currentTarget.dataset.key
    if (key === 'account') {
      if (!this.data.loggedIn) {
        this.onWechatLoginTap()
        return
      }
      wx.showToast({ title: '账号信息开发中', icon: 'none' })
      return
    }

    const labels = {
      about: '关于项目',
      guide: '使用说明',
      feedback: '意见反馈',
      privacy: '隐私说明'
    }
    wx.showToast({
      title: `${labels[key] || '功能'}开发中`,
      icon: 'none'
    })
  }
})
