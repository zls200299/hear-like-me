/**
 * 上传音频（占位，后续接入）
 * @param {string} filePath
 * @returns {Promise<never>}
 */
function uploadAudio(filePath) {
  return Promise.reject({
    message: 'uploadAudio 尚未实现',
    filePath
  })
}

module.exports = {
  uploadAudio
}
