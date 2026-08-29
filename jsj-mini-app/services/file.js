const config = require('../config.js')

function extractAssetIdFromPreviewUrl(url) {
  if (!url) return ''
  const match = String(url).match(/\/preview\/(\d+)$/)
  return match ? match[1] : ''
}

function normalizeAssetId(data) {
  const fromUrl = extractAssetIdFromPreviewUrl(data.url)
  if (fromUrl) return fromUrl
  if (data.assetId == null || data.assetId === '') return ''
  return String(data.assetId)
}

/**
 * 解析上传接口响应，兼容直接 data 与 R 包装
 * @param {string|Object} resData
 * @returns {{ assetId, fileName, url, objectKey }}
 */
function parseUploadResponse(resData) {
  let body = resData
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body)
    } catch (e) {
      throw { message: '上传响应解析失败' }
    }
  }

  let data = body
  if (body && typeof body.code === 'number') {
    if (body.code !== 200) {
      throw {
        message: body.msg || '上传失败',
        code: body.code,
        response: body
      }
    }
    data = body.data
  }

  if (!data || data.assetId == null) {
    throw { message: '上传响应无效', response: body }
  }

  return {
    assetId: normalizeAssetId(data),
    fileName: data.fileName || data.originalFilename || '',
    url: data.url || '',
    objectKey: data.objectKey || ''
  }
}

/**
 * 上传音频文件
 * @param {string} filePath 本地临时文件路径
 * @returns {Promise<{ assetId, fileName, url, objectKey }>}
 */
function uploadAudio(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.baseUrl + '/api/files/audio',
      filePath,
      name: 'file',
      success(res) {
        if (res.statusCode !== 200) {
          reject({
            message: `HTTP ${res.statusCode}`,
            statusCode: res.statusCode,
            response: res
          })
          return
        }

        try {
          resolve(parseUploadResponse(res.data))
        } catch (err) {
          reject(err)
        }
      },
      fail(err) {
        reject({
          message: err.errMsg || '上传失败',
          error: err
        })
      }
    })
  })
}

module.exports = {
  uploadAudio,
  parseUploadResponse
}
