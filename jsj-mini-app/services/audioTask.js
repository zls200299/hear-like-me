const { request } = require('./request.js')

function extractData(response) {
  if (!response) return {}
  if (typeof response.code === 'number') {
    if (response.code !== 200) {
      const msg = response.msg || response.message || '请求失败'
      const error = new Error(msg)
      error.code = response.code
      error.response = response
      throw error
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
    visualizationData: data.visualizationData || null,
    visualizationUrl: data.visualizationUrl || ''
  }
}

/**
 * 创建音频处理任务
 * @param {Object} data
 * @returns {Promise<Object>}
 */
async function createTask(data) {
  try {
    const response = await request({
      url: '/api/audio/tasks',
      method: 'POST',
      data
    })
    return normalizeTask(extractData(response))
  } catch (err) {
    const message = err.message
      || (err.response && (err.response.msg || err.response.message))
      || (err.data && (err.data.msg || err.data.message))
      || err.errorMessage
      || '转换失败'
    const error = new Error(message)
    error.code = err.code
    error.response = err.response || err.data
    throw error
  }
}

/**
 * 查询音频处理任务
 * @param {string} taskNo
 * @returns {Promise<Object>}
 */
async function getTask(taskNo) {
  try {
    const response = await request({
      url: `/api/audio/tasks/${taskNo}`,
      method: 'GET'
    })
    return normalizeTask(extractData(response))
  } catch (err) {
    const message = err.message
      || (err.response && (err.response.msg || err.response.message))
      || '查询失败'
    const error = new Error(message)
    error.code = err.code
    error.response = err.response
    throw error
  }
}

module.exports = {
  createTask,
  getTask,
  normalizeTask
}
