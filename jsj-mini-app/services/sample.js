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

function extractList(response) {
  if (!response) return []
  if (Array.isArray(response)) return response
  if (Array.isArray(response.data)) return response.data
  return []
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

module.exports = {
  listSamples,
  mockSamples,
  normalizeSample
}
