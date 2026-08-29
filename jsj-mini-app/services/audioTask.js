const { request } = require('./request.js')

function extractData(response) {
  if (!response) return {}
  if (typeof response.code === 'number') {
    if (response.code !== 200) {
      throw {
        message: response.msg || '请求失败',
        code: response.code,
        response
      }
    }
    return response.data || {}
  }
  return response
}

function normalizeId(value) {
  if (value == null || value === '') return null
  return String(value)
}

function normalizeTask(data) {
  if (!data) return {}
  return {
    taskNo: data.taskNo || '',
    status: data.status || '',
    sourceAssetId: normalizeId(data.sourceAssetId),
    outputAssetId: normalizeId(data.outputAssetId),
    processedAudioUrl: data.processedAudioUrl || data.outputUrl || '',
    clarityScore: data.clarityScore != null ? data.clarityScore : null,
    clarityGrade: data.clarityGrade || '',
    errorMessage: data.errorMessage || ''
  }
}

/**
 * 创建音频处理任务
 * @param {Object} data
 * @returns {Promise<Object>}
 */
async function createTask(data) {
  const response = await request({
    url: '/api/audio/tasks',
    method: 'POST',
    data
  })
  return normalizeTask(extractData(response))
}

/**
 * 查询音频处理任务
 * @param {string} taskNo
 * @returns {Promise<Object>}
 */
async function getTask(taskNo) {
  const response = await request({
    url: `/api/audio/tasks/${taskNo}`,
    method: 'GET'
  })
  return normalizeTask(extractData(response))
}

module.exports = {
  createTask,
  getTask,
  normalizeTask
}
