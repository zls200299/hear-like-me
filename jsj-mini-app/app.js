// app.js
const config = require('./config.js')

App({
  globalData: {
    userInfo: null,
    userId: '',
    token: '',
    baseUrl: config.baseUrl
  },

  onLaunch() {
    const userId = wx.getStorageSync('userId')
    const token = wx.getStorageSync('token')
    if (userId) {
      this.globalData.userId = userId
    }
    if (token) {
      this.globalData.token = token
    }
  }
})
