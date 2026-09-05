let authService = null
let fileService = null

function isRemoteUrl(value) {
  const url = String(value || '')
  if (!/^https?:\/\//i.test(url)) return false
  return !(
    /^http:\/\/tmp\//i.test(url)
    || /\/__(?:tmp|store)__\//i.test(url)
    || /^http:\/\/127\.0\.0\.1:\d+\/__(?:tmp|store)__\//i.test(url)
  )
}

function getAuthService() {
  if (!authService) {
    authService = require('../../services/auth.js')
  }
  return authService
}

function getFileService() {
  if (!fileService) {
    fileService = require('../../services/file.js')
  }
  return fileService
}

const MENU_ITEMS = [
  { key: 'account', icon: 'profile-menu-account', label: '账号信息' },
  { key: 'about', icon: 'profile-menu-about', label: '关于项目' },
  { key: 'guide', icon: 'profile-menu-guide', label: '使用说明' },
  { key: 'feedback', icon: 'profile-menu-feedback', label: '意见反馈' },
  { key: 'privacy', icon: 'profile-menu-privacy', label: '隐私说明' }
]

const INFO_PANELS = {
  about: {
    icon: 'profile-menu-about',
    title: '关于项目',
    intro: 'Hear Like Me 希望用声音体验和易懂的科普，让更多人理解人工耳蜗如何工作，也理解每个人的听觉体验并不相同。',
    sections: [
      {
        title: '我们在做什么',
        text: '通过听觉模拟、听音挑战和科普文章，把抽象的声码器、通道与听觉概念变成可以听、可以看、可以理解的体验。'
      },
      {
        title: '适合谁使用',
        text: '适合希望了解人工耳蜗的公众、学生、家长和教育工作者，也可作为沟通与科普的辅助工具。'
      },
      {
        title: '需要说明',
        text: '模拟结果只用于教育体验，不能代表任何人工耳蜗使用者的真实听感，也不能替代听力评估、医疗诊断或康复建议。'
      }
    ]
  },
  guide: {
    icon: 'profile-menu-guide',
    title: '使用说明',
    intro: '建议在安静环境中佩戴耳机体验，并先把系统音量调低，再逐步增加到舒适水平。',
    sections: [
      {
        title: '听觉模拟',
        text: '选择示例声音、上传音频或使用实时体验，调整通道数等参数，然后对比原声与模拟声。'
      },
      {
        title: '听音挑战',
        text: '先播放题目声音，再根据听感作答。完成后可查看结果，逐渐建立对不同通道效果的直观认识。'
      },
      {
        title: '科普学习',
        text: '从基础入门开始阅读，再了解声音如何被听见、技术局限和儿童听力支持。页面较长时可直接上下滑动。'
      },
      {
        title: '安全提醒',
        text: '不要在驾驶、骑行或需要留意环境声音时使用。若感到刺耳或不适，请立即停止播放并调低音量。'
      }
    ]
  },
  feedback: {
    icon: 'profile-menu-feedback',
    title: '意见反馈',
    intro: '如果你遇到功能问题、内容错误，或者有体验建议，可以通过微信反馈页面告诉我们。',
    sections: []
  },
  privacy: {
    icon: 'profile-menu-privacy',
    title: '隐私说明',
    intro: '我们只在提供功能所必需的范围内处理信息，并尽量把选择权交给你。',
    sections: [
      {
        title: '账号资料',
        text: '微信登录后会保存用户编号、昵称、头像和登录凭证，用于展示账号信息及保持登录状态。头像和昵称只会在你主动选择或修改后提交。'
      },
      {
        title: '音频与麦克风',
        text: '只有在你主动上传音频或开启实时体验时，应用才会处理对应声音数据。麦克风权限可随时在微信设置中关闭。'
      },
      {
        title: '本机数据',
        text: '应用会在本机保存必要的登录信息和部分体验状态。退出登录会清除本机登录凭证，但不会自动注销服务端账号。'
      },
      {
        title: '你的选择',
        text: '请不要上传包含他人隐私或敏感内容的音频。你可以拒绝非必要权限，未登录时仍可使用大部分科普与体验功能。'
      }
    ]
  }
}

Page({
  data: {
    loggedIn: false,
    userId: '',
    nickname: '',
    avatarUrl: '',
    defaultAvatar: '/assets/icons/profile-avatar-placeholder.svg',
    loggingIn: false,
    loggingOut: false,
    loginSheetVisible: false,
    profileSheetMode: 'login',
    savingProfile: false,
    preparingAvatar: false,
    sheetNickname: '',
    sheetAvatarUrl: '',
    infoSheetVisible: false,
    activePanelKey: '',
    activePanel: null,
    menuItems: MENU_ITEMS
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.PROFILE)
    this._syncLoginState()
    if (this.data.loginSheetVisible || this.data.infoSheetVisible) {
      this._setTabBarHidden(true)
    }
  },

  onHide() {
    this._setTabBarHidden(false)
  },

  onUnload() {
    this._setTabBarHidden(false)
  },

  _syncLoginState() {
    const { getSession, isLoggedIn } = getAuthService()
    const { userId, userInfo } = getSession()
    const loggedIn = isLoggedIn()
    const cachedAvatar = (userInfo && userInfo.avatar) || ''
    this.setData({
      loggedIn,
      userId: userId || (userInfo && userInfo.userId) || '',
      nickname: (userInfo && userInfo.nickname) || '',
      // 微信临时路径跨启动会失效，缓存中只恢复服务端 URL。
      avatarUrl: isRemoteUrl(cachedAvatar) ? cachedAvatar : ''
    })

    if (loggedIn) {
      this._refreshCurrentUser()
    }
  },

  _refreshCurrentUser() {
    const { getCurrentUser, saveUserProfile } = getAuthService()
    getCurrentUser()
      .then((user) => {
        if (!user) return
        const profile = saveUserProfile(user)
        this.setData({
          userId: profile.userId || this.data.userId,
          nickname: profile.nickname || '',
          avatarUrl: profile.avatar || ''
        })
      })
      .catch(() => {})
  },

  onChooseAvatar(e) {
    const avatarUrl = e.detail && e.detail.avatarUrl
    if (!avatarUrl) return
    this._prepareChosenAvatar(avatarUrl, 'avatarUrl')
  },

  onSheetChooseAvatar(e) {
    const avatarUrl = e.detail && e.detail.avatarUrl
    if (!avatarUrl) return
    this._prepareChosenAvatar(avatarUrl, 'sheetAvatarUrl')
  },

  _prepareChosenAvatar(tempFilePath, targetField) {
    if (!tempFilePath || this.data.preparingAvatar) return
    if (isRemoteUrl(tempFilePath)) {
      this.setData({ [targetField]: tempFilePath })
      return
    }

    this.setData({ preparingAvatar: true })

    const { uploadImage } = getFileService()
    uploadImage(tempFilePath)
      .then((file) => {
        const remoteUrl = file && file.url
        if (!remoteUrl || !isRemoteUrl(remoteUrl)) {
          throw new Error('头像地址无效')
        }
        this.setData({
          [targetField]: remoteUrl,
          preparingAvatar: false
        })
      })
      .catch((err) => {
        this.setData({ preparingAvatar: false })
        wx.showToast({
          title: (err && err.message) ? err.message : '头像上传失败，请重试',
          icon: 'none'
        })
      })
  },

  onNicknameInput(e) {
    const value = e.detail && e.detail.value
    if (value == null) return
    this._updateSheetNickname(value)
  },

  onNicknameBlur(e) {
    const value = e.detail && e.detail.value
    if (value == null) return
    this._updateSheetNickname(value)
  },

  onNicknameReview(e) {
    const detail = e.detail || {}
    // 官方仅返回 pass / timeout，不含昵称；不通过时提示即可，勿用 detail.value 覆盖
    if (detail.timeout) {
      wx.showToast({ title: '昵称校验超时，请重试', icon: 'none' })
      return
    }
    if (detail.pass === false) {
      wx.showToast({ title: '昵称未通过安全检测，请修改', icon: 'none' })
      this._updateSheetNickname('')
    }
  },

  _updateSheetNickname(value) {
    this.setData({
      sheetNickname: value == null ? '' : String(value)
    })
  },

  _resolveNicknameFromSubmit(e) {
    const formValue = e && e.detail && e.detail.value ? e.detail.value : {}
    const fromForm = formValue.nickname != null ? String(formValue.nickname).trim() : ''
    if (fromForm) return fromForm
    return String(this.data.sheetNickname || '').trim()
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
    const { wxLoginSilent, saveSession, readLastProfile } = getAuthService()
    wxLoginSilent()
      .then(({ newUser, data }) => {
        if (!newUser) {
          const userInfo = saveSession(data)
          this._finishLogin(userInfo)
          return
        }

        const lastProfile = readLastProfile() || {}
        const lastAvatar = isRemoteUrl(lastProfile.avatar) ? lastProfile.avatar : ''
        this._setTabBarHidden(true)
        this.setData({
          loggingIn: false,
          loginSheetVisible: true,
          profileSheetMode: 'login',
          sheetNickname: lastProfile.nickname || '',
          sheetAvatarUrl: this.data.avatarUrl || lastAvatar
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
      profileSheetMode: 'login',
      sheetNickname: '',
      sheetAvatarUrl: '',
      preparingAvatar: false
    })
    this._setTabBarHidden(false)
    wx.showToast({ title: '登录成功', icon: 'success' })
  },

  onCloseLoginSheet() {
    if (this.data.loggingIn || this.data.savingProfile || this.data.preparingAvatar) return
    this._setTabBarHidden(false)
    this.setData({
      loginSheetVisible: false,
      profileSheetMode: 'login',
      sheetNickname: '',
      sheetAvatarUrl: '',
      preparingAvatar: false
    })
  },

  onProfileFormSubmit(e) {
    const nickname = this._resolveNicknameFromSubmit(e)
    if (this.data.profileSheetMode === 'edit') {
      this.onSaveProfile(nickname)
      return
    }
    this.onLogin(nickname)
  },

  onLogin(nickname) {
    if (this.data.loggingIn || this.data.preparingAvatar) return

    const name = String(nickname || '').trim()
    if (!name) {
      wx.showToast({ title: '请先填写昵称', icon: 'none' })
      return
    }

    const { wxLoginWithProfile } = getAuthService()
    const selectedAvatarUrl = this.data.sheetAvatarUrl
    this.setData({ loggingIn: true, sheetNickname: name })
    wxLoginWithProfile({
      nickname: name,
      avatarPath: selectedAvatarUrl
    })
      .then((userInfo) => {
        this._finishLogin({
          ...userInfo,
          nickname: userInfo.nickname || name,
          avatar: userInfo.avatar || selectedAvatarUrl
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

  onSaveProfile(nickname) {
    if (this.data.savingProfile || this.data.preparingAvatar) return

    const name = String(nickname || '').trim()
    if (!name) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }

    const { updateProfile } = getAuthService()
    const avatarPath = this.data.sheetAvatarUrl || this.data.avatarUrl
    if (name === this.data.nickname && avatarPath === this.data.avatarUrl) {
      this.onCloseLoginSheet()
      wx.showToast({ title: '资料没有变化', icon: 'none' })
      return
    }

    this.setData({ savingProfile: true, sheetNickname: name })
    updateProfile({ nickname: name, avatarPath })
      .then((userInfo) => {
        const persistedAvatar = userInfo.avatar || avatarPath
        this.setData({
          loggedIn: true,
          userId: userInfo.userId || this.data.userId,
          nickname: userInfo.nickname || name,
          avatarUrl: persistedAvatar,
          savingProfile: false,
          loginSheetVisible: false,
          profileSheetMode: 'login',
          sheetNickname: '',
          sheetAvatarUrl: '',
          preparingAvatar: false
        })
        this._setTabBarHidden(false)
        wx.showToast({ title: '资料已更新', icon: 'success' })
      })
      .catch((err) => {
        this.setData({ savingProfile: false })
        wx.showToast({
          title: (err && err.message) ? err.message : '保存失败',
          icon: 'none'
        })
      })
  },

  onLogout() {
    if (this.data.loggingOut) return

    this.setData({ loggingOut: true })
    const { logout } = getAuthService()
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
          loginSheetVisible: false,
          profileSheetMode: 'login',
          sheetAvatarUrl: '',
          preparingAvatar: false,
          infoSheetVisible: false,
          activePanelKey: '',
          activePanel: null
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
      this._setTabBarHidden(true)
      this.setData({
        loginSheetVisible: true,
        profileSheetMode: 'edit',
        sheetNickname: this.data.nickname || '',
        sheetAvatarUrl: this.data.avatarUrl || ''
      })
      return
    }

    const panel = INFO_PANELS[key]
    if (!panel) return
    this._setTabBarHidden(true)
    this.setData({
      infoSheetVisible: true,
      activePanelKey: key,
      activePanel: panel
    })
  },

  onCloseInfoSheet() {
    this._setTabBarHidden(false)
    this.setData({
      infoSheetVisible: false,
      activePanelKey: '',
      activePanel: null
    })
  }
})
