const { request } = require('./request.js')

const mockScenarios = [
  {
    code: 'quiet',
    nameCn: '安静对话',
    description: '安静环境下的日常对话场景',
    nChannels: 8,
    carrier: 'noise',
    fLo: 150,
    fHi: 7000,
    envCut: 160,
    spread: 15,
    noiseLevel: 0
  },
  {
    code: 'restaurant',
    nameCn: '嘈杂餐厅',
    description: '背景噪声较高的餐厅环境',
    nChannels: 8,
    carrier: 'noise',
    fLo: 150,
    fHi: 7000,
    envCut: 160,
    spread: 40,
    noiseLevel: 55
  },
  {
    code: 'phone',
    nameCn: '电话通话',
    description: '电话频带受限的通话场景',
    nChannels: 8,
    carrier: 'noise',
    fLo: 300,
    fHi: 3400,
    envCut: 160,
    spread: 10,
    noiseLevel: 15
  },
  {
    code: 'music',
    nameCn: '听音乐',
    description: '较宽频带的音乐聆听场景',
    nChannels: 8,
    carrier: 'noise',
    fLo: 80,
    fHi: 8000,
    envCut: 220,
    spread: 25,
    noiseLevel: 0
  },
  {
    code: 'tone',
    nameCn: '声调语言',
    description: '强调声调信息的语言场景',
    nChannels: 8,
    carrier: 'noise',
    fLo: 150,
    fHi: 7000,
    envCut: 120,
    spread: 20,
    noiseLevel: 10
  },
  {
    code: 'minimal',
    nameCn: '仅4通道',
    description: '极低通道数对比体验',
    nChannels: 4,
    carrier: 'noise',
    fLo: 150,
    fHi: 7000,
    envCut: 160,
    spread: 0,
    noiseLevel: 0
  }
]

function toPercent(value) {
  if (value == null || value === '') return 0
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  return num <= 1 ? Math.round(num * 100) : Math.round(num)
}

function toNumber(value, fallback) {
  const num = Number(value)
  return Number.isNaN(num) ? fallback : num
}

function buildFrequencyRange(fLo, fHi) {
  return `${Math.round(fLo)}-${Math.round(fHi)}`
}

function normalizeScenario(item) {
  const code = item.code || item.scenarioCode || ''
  const fLo = toNumber(
    item.fLo != null ? item.fLo : (item.f_lo != null ? item.f_lo : item.flo),
    150
  )
  const fHi = toNumber(
    item.fHi != null ? item.fHi : (item.f_hi != null ? item.f_hi : item.fhi),
    7000
  )

  return {
    code,
    nameCn: item.nameCn || item.name_cn || item.name || code,
    description: item.description || item.descriptionCn || item.description_cn || '',
    nChannels: toNumber(
      item.nChannels != null ? item.nChannels : (item.n_channels != null ? item.n_channels : item.nchannels),
      8
    ),
    carrier: item.carrier || 'noise',
    fLo,
    fHi,
    envCut: toNumber(item.envCut != null ? item.envCut : item.env_cut, 160),
    spread: toPercent(item.spread),
    noiseLevel: toPercent(item.noiseLevel != null ? item.noiseLevel : item.noise_level),
    frequencyRange: item.frequencyRange || buildFrequencyRange(fLo, fHi)
  }
}

function extractList(response) {
  if (!response) return []
  if (Array.isArray(response)) return response
  if (Array.isArray(response.data)) return response.data
  return []
}

/**
 * 获取场景列表，失败时返回 mock 数据
 * @returns {Promise<Array>}
 */
async function listScenarios() {
  try {
    const response = await request({
      url: '/api/scenarios',
      method: 'GET'
    })
    const list = extractList(response).map(normalizeScenario).filter((item) => item.code)
    if (list.length > 0) {
      return list
    }
    console.warn('[scenario] empty response, use mock fallback')
  } catch (err) {
    console.warn('[scenario] listScenarios failed, use mock fallback', err)
  }
  return mockScenarios.map(normalizeScenario)
}

module.exports = {
  listScenarios,
  mockScenarios,
  normalizeScenario
}
