// 网络请求封装
const app = getApp()

const request = (url, data = {}, method = 'POST') => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + url,
      method: method,
      data: data,
      header: (() => {
        const headers = {
          'content-type': 'application/json',
          userId: app.globalData.userId != null ? String(app.globalData.userId) : ''
        }
        const token = app.globalData.token || wx.getStorageSync('token')
        if (token) {
          // 小程序部分基础库/环境下 Authorization 会被拦截，后端同时认「token」头
          headers.token = token
          headers.Authorization = token
        }
        return headers
      })(),
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(res)
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

module.exports = {
  get: (url, data) => request(url, data, 'GET'),
  post: (url, data) => request(url, data, 'POST')
}
