const { request } = require('./request.js')

const mockSamples = [
  {
    code: 'vowel',
    nameCn: '元音示例',
    description: '标准元音测试音',
    assetId: null,
    audioUrl: ''
  },
  {
    code: 'tone',
    nameCn: '声调示例',
    description: '声调变化测试音',
    assetId: null,
    audioUrl: ''
  },
  {
    code: 'melody',
    nameCn: '旋律示例',
    description: '简单旋律测试音',
    assetId: null,
    audioUrl: ''
  }
]

function normalizeSample(item) {
  const code = item.code || item.sampleCode || ''
  return {
    code,
    nameCn: item.nameCn || item.name_cn || item.name || code,
    description: item.description || item.descriptionCn || item.description_cn || '',
    assetId: item.assetId != null ? item.assetId : (item.asset_id != null ? item.asset_id : null),
    audioUrl: item.audioUrl || item.audio_url || ''
  }
}

function normalizeSampleSource(data) {
  if (!data) return {}
  return {
    assetId: data.assetId != null ? String(data.assetId) : '',
    sampleCode: data.sampleCode || '',
    fileName: data.fileName || data.originalFilename || '',
    url: data.url || '',
    objectKey: data.objectKey || ''
  }
}

function extractList(response) {
  if (!response) return []
  if (Array.isArray(response)) return response
  if (Array.isArray(response.data)) return response.data
  return []
}

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

/**
 * 获取示例声音列表，失败时返回 mock 数据
 * @returns {Promise<Array>}
 */
async function listSamples() {
  try {
    const response = await request({
      url: '/api/samples',
      method: 'GET'
    })
    const list = extractList(response).map(normalizeSample).filter((item) => item.code)
    if (list.length > 0) {
      return list
    }
    console.warn('[sample] empty response, use mock fallback')
  } catch (err) {
    console.warn('[sample] listSamples failed, use mock fallback', err)
  }
  return mockSamples.map(normalizeSample)
}

/**
 * 准备内置示例原声（生成 AUDIO_SOURCE）
 * @param {string} sampleCode vowel / tone / melody
 * @returns {Promise<{ assetId, sampleCode, fileName, url, objectKey }>}
 */
async function prepareSampleSource(sampleCode) {
  try {
    const response = await request({
      url: '/api/samples/source',
      method: 'POST',
      data: { sampleCode }
    })
    const data = normalizeSampleSource(extractData(response))
    if (!data.assetId || !data.url) {
      throw new Error('示例原声响应无效')
    }
    return data
  } catch (err) {
    const message = err.message
      || (err.response && (err.response.msg || err.response.message))
      || '示例声音准备失败'
    const error = new Error(message)
    error.code = err.code
    error.response = err.response
    throw error
  }
}

module.exports = {
  listSamples,
  prepareSampleSource,
  mockSamples,
  normalizeSample,
  normalizeSampleSource
}
