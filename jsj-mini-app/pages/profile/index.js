const {
  getSession,
  isLoggedIn,
  wxLoginSilent,
  wxLoginWithProfile,
  getCurrentUser,
  logout,
  readLastProfile,
  saveSession
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
    sheetNickname: '',
    menuItems: MENU_ITEMS
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.PROFILE)
    this._syncLoginState()
  },

  onHide() {
    this._setTabBarHidden(false)
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
    this._updateSheetNickname(e.detail && e.detail.value)
  },

  onNicknameBlur(e) {
    this._updateSheetNickname(e.detail && e.detail.value)
  },

  onNicknameReview(e) {
    const detail = e.detail || {}
    if (detail.pass) {
      this._updateSheetNickname(detail.value)
    }
  },

  _updateSheetNickname(value) {
    this.setData({
      sheetNickname: value == null ? '' : String(value)
    })
  },

  _readSheetNickname() {
    return new Promise((resolve) => {
      this.createSelectorQuery()
        .select('#loginNicknameInput')
        .fields({ properties: ['value'] })
        .exec((res) => {
          const fromDom = res && res[0] && res[0].value
          const nickname = String(fromDom != null ? fromDom : (this.data.sheetNickname || '')).trim()
          resolve(nickname)
        })
    })
  },

  _setTabBarHidden(hidden) {
    if (typeof this.getTabBar === 'function') {
      const tabBar = this.getTabBar()
      if (tabBar) {
        tabBar.setData({ hidden: !!hidden })
      }
    }
  },

  onWechatLoginTap() {
    if (this.data.loggedIn || this.data.loggingIn) return

    this.setData({ loggingIn: true })
    wxLoginSilent()
      .then(({ newUser, data }) => {
        if (!newUser) {
          const userInfo = saveSession(data)
          this._finishLogin(userInfo)
          return
        }

        const lastProfile = readLastProfile() || {}
        this._setTabBarHidden(true)
        this.setData({
          loggingIn: false,
          loginSheetVisible: true,
          sheetNickname: lastProfile.nickname || '',
          avatarUrl: lastProfile.avatar || ''
        })
      })
      .catch((err) => {
        this.setData({ loggingIn: false })
        wx.showToast({
          title: (err && err.message) ? err.message : '登录失败',
          icon: 'none'
        })
      })
  },

  _finishLogin(userInfo) {
    this.setData({
      loggedIn: true,
      userId: userInfo.userId || '',
      nickname: userInfo.nickname || '',
      avatarUrl: userInfo.avatar || '',
      loggingIn: false,
      loginSheetVisible: false,
      sheetNickname: ''
    })
    this._setTabBarHidden(false)
    wx.showToast({ title: '登录成功', icon: 'success' })
  },

  onCloseLoginSheet() {
    if (this.data.loggingIn) return
    this._setTabBarHidden(false)
    this.setData({ loginSheetVisible: false })
  },

  onLogin() {
    if (this.data.loggingIn) return

    this._readSheetNickname().then((nickname) => {
      if (!nickname) {
        wx.showToast({ title: '请先填写昵称', icon: 'none' })
        return
      }

      this.setData({ loggingIn: true, sheetNickname: nickname })
      wxLoginWithProfile({
        nickname,
        avatarPath: this.data.avatarUrl
      })
        .then((userInfo) => {
          this._finishLogin({
            ...userInfo,
            nickname: userInfo.nickname || nickname,
            avatar: userInfo.avatar || this.data.avatarUrl
          })
        })
        .catch((err) => {
          this.setData({ loggingIn: false })
          wx.showToast({
            title: (err && err.message) ? err.message : '登录失败',
            icon: 'none'
          })
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
          sheetNickname: '',
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
