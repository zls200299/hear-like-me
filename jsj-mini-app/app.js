// app.js
const config = require('./config.js')
const { getSession } = require('./services/auth.js')

App({
  globalData: {
    userInfo: null,
    userId: '',
    token: '',
    baseUrl: config.baseUrl
  },

  onLaunch() {
    const { token, userId, userInfo } = getSession()
    if (userId) {
      this.globalData.userId = userId
    }
    if (token) {
      this.globalData.token = token
    }
    if (userInfo) {
      this.globalData.userInfo = userInfo
    }
  }
})
