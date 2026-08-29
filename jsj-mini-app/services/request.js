const config = require('../config.js')

/**
 * 封装 wx.request
 * @param {Object} options
 * @param {string} options.url - 相对路径，如 /api/scenarios
 * @param {string} [options.method='GET']
 * @param {Object} [options.data]
 * @param {Object} [options.header]
 * @returns {Promise<*>}
 */
function request(options = {}) {
  const { url, method = 'GET', data, header = {} } = options

  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + url,
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...header
      },
      success(res) {
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
