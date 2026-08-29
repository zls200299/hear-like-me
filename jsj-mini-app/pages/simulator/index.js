const { listScenarios } = require('../../services/scenario.js')
const { listSamples } = require('../../services/sample.js')

const SCENARIO_PRESETS = {
  quiet: {
    nChannels: 8,
    frequencyRange: '150-7000',
    envCut: 160,
    spread: 15,
    noiseLevel: 0
  },
  restaurant: {
    nChannels: 8,
    frequencyRange: '150-7000',
    envCut: 160,
    spread: 40,
    noiseLevel: 55
  },
  phone: {
    nChannels: 8,
    frequencyRange: '300-3400',
    envCut: 160,
    spread: 10,
    noiseLevel: 15
  },
  music: {
    nChannels: 8,
    frequencyRange: '80-8000',
    envCut: 220,
    spread: 25,
    noiseLevel: 0
  },
  tone: {
    nChannels: 8,
    frequencyRange: '150-7000',
    envCut: 120,
    spread: 20,
    noiseLevel: 10
  },
  minimal: {
    nChannels: 4,
    frequencyRange: '150-7000',
    envCut: 160,
    spread: 0,
    noiseLevel: 0
  }
}

const SAMPLE_LABELS = {
  vowel: '元音示例',
  tone: '声调示例',
  melody: '旋律示例'
}

function buildElectrodeDots(count) {
  return Array.from({ length: count }, (_, index) => index)
}

function buildScenarioPresets(scenarios) {
  const presets = {}
  scenarios.forEach((item) => {
    presets[item.code] = {
      nChannels: item.nChannels,
      frequencyRange: item.frequencyRange,
      envCut: item.envCut,
      spread: item.spread,
      noiseLevel: item.noiseLevel,
      carrier: item.carrier
    }
  })
  return presets
}

function buildSampleLabels(samples) {
  const labels = {}
  samples.forEach((item) => {
    labels[item.code] = item.nameCn
  })
  return labels
}

Page({
  data: {
    sourceType: 'sample',
    selectedSample: 'vowel',
    selectedScenario: 'quiet',
    nChannels: 8,
    carrier: 'noise',
    frequencyRange: '150-7000',
    envCut: 160,
    spread: 15,
    noiseLevel: 0,
    taskStatus: 'idle',
    statusText: '已选择示例声音',
    sourceHint: '',
    electrodeDots: buildElectrodeDots(8),
    sourceOptions: [
      { type: 'sample', label: '示例声音' },
      { type: 'upload', label: '上传音频' },
      { type: 'record', label: '录制声音' },
      { type: 'realtime', label: '实时麦克风' }
    ],
    sampleOptions: [
      { code: 'vowel', label: '元音示例' },
      { code: 'tone', label: '声调示例' },
      { code: 'melody', label: '旋律示例' }
    ],
    scenarioList: [
      { code: 'quiet', name: '安静对话' },
      { code: 'restaurant', name: '嘈杂餐厅' },
      { code: 'phone', name: '电话通话' },
      { code: 'music', name: '听音乐' },
      { code: 'tone', name: '声调语言' },
      { code: 'minimal', name: '仅4通道' }
    ],
    frequencyOptions: [
      { value: '80-8000', label: '80-8000 Hz' },
      { value: '150-7000', label: '150-7000 Hz' },
      { value: '300-3400', label: '300-3400 Hz' }
    ],
    channelQuickValues: [1, 2, 4, 8, 16, 22],
    isProcessing: false
  },

  _processTimer: null,
  _scenarioPresets: null,
  _sampleLabels: null,

  onLoad() {
    this._scenarioPresets = { ...SCENARIO_PRESETS }
    this._sampleLabels = { ...SAMPLE_LABELS }
    this._loadRemoteData()
  },

  onUnload() {
    if (this._processTimer) {
      clearTimeout(this._processTimer)
      this._processTimer = null
    }
  },

  async _loadRemoteData() {
    await Promise.all([
      this._loadScenarios(),
      this._loadSamples()
    ])
  },

  async _loadScenarios() {
    const scenarios = await listScenarios()
    if (!scenarios || !scenarios.length) return

    this._scenarioPresets = buildScenarioPresets(scenarios)
    this.setData({
      scenarioList: scenarios.map((item) => ({
        code: item.code,
        name: item.nameCn
      }))
    })
  },

  async _loadSamples() {
    const samples = await listSamples()
    if (!samples || !samples.length) return

    this._sampleLabels = buildSampleLabels(samples)

    const sampleOptions = samples.map((item) => ({
      code: item.code,
      label: item.nameCn
    }))

    const selectedSample = samples.some((item) => item.code === this.data.selectedSample)
      ? this.data.selectedSample
      : samples[0].code

    this.setData({
      sampleOptions,
      selectedSample,
      statusText: `已选择${this._sampleLabels[selectedSample] || selectedSample}`
    })
  },

  _getSampleLabel(code) {
    if (this._sampleLabels && this._sampleLabels[code]) {
      return this._sampleLabels[code]
    }
    return SAMPLE_LABELS[code] || code
  },

  _getScenarioPreset(code) {
    if (this._scenarioPresets && this._scenarioPresets[code]) {
      return this._scenarioPresets[code]
    }
    return SCENARIO_PRESETS[code]
  },

  _updateElectrodeDots(count) {
    this.setData({
      electrodeDots: buildElectrodeDots(count)
    })
  },

  _resetTaskStatus() {
    this.setData({
      taskStatus: 'idle',
      isProcessing: false
    })
  },

  selectSource(e) {
    const type = e.currentTarget.dataset.type
    let sourceHint = ''
    let statusText = '未选择音频'

    if (type === 'sample') {
      statusText = `已选择${this._getSampleLabel(this.data.selectedSample)}`
    } else if (type === 'upload') {
      sourceHint = '已选择：待上传音频示例'
      statusText = '已选择待上传音频'
    } else if (type === 'record') {
      sourceHint = '录制功能后续接入'
      statusText = '录制功能后续接入'
    } else if (type === 'realtime') {
      sourceHint = '实时麦克风功能需要真机验证，后续接入。'
      statusText = '实时麦克风后续接入'
    }

    this._resetTaskStatus()
    this.setData({
      sourceType: type,
      sourceHint,
      statusText
    })
  },

  selectSample(e) {
    const code = e.currentTarget.dataset.code
    this._resetTaskStatus()
    this.setData({
      selectedSample: code,
      statusText: `已选择${this._getSampleLabel(code)}`
    })
  },

  selectScenario(e) {
    const code = e.currentTarget.dataset.code
    const preset = this._getScenarioPreset(code)
    if (!preset) return

    const scenarioItem = this.data.scenarioList.find((item) => item.code === code)
    const scenarioName = scenarioItem ? scenarioItem.name : code

    this._resetTaskStatus()
    this._updateElectrodeDots(preset.nChannels)
    this.setData({
      selectedScenario: code,
      nChannels: preset.nChannels,
      frequencyRange: preset.frequencyRange,
      envCut: preset.envCut,
      spread: preset.spread,
      noiseLevel: preset.noiseLevel,
      carrier: preset.carrier || this.data.carrier,
      statusText: `已切换至${scenarioName}场景`
    })
  },

  changeChannels(e) {
    const value = Number(e.detail.value)
    this._resetTaskStatus()
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      statusText: `已设置 ${value} 通道`
    })
  },

  quickSetChannels(e) {
    const value = Number(e.currentTarget.dataset.value)
    this._resetTaskStatus()
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      statusText: `已设置 ${value} 通道`
    })
  },

  selectCarrier(e) {
    const value = e.currentTarget.dataset.value
    this._resetTaskStatus()
    this.setData({
      carrier: value,
      statusText: value === 'noise' ? '已选择噪声载体' : '已选择正弦载体'
    })
  },

  selectFrequencyRange(e) {
    const value = e.currentTarget.dataset.value
    this._resetTaskStatus()
    this.setData({
      frequencyRange: value,
      statusText: `频率范围已设为 ${value} Hz`
    })
  },

  changeEnvCut(e) {
    const value = Number(e.detail.value)
    this._resetTaskStatus()
    this.setData({
      envCut: value,
      statusText: `包络细节已设为 ${value} Hz`
    })
  },

  changeSpread(e) {
    const value = Number(e.detail.value)
    this._resetTaskStatus()
    this.setData({
      spread: value,
      statusText: `电流扩散已设为 ${value}%`
    })
  },

  changeNoiseLevel(e) {
    const value = Number(e.detail.value)
    this._resetTaskStatus()
    this.setData({
      noiseLevel: value,
      statusText: `环境噪声已设为 ${value}%`
    })
  },

  playOriginal() {
    this.setData({
      statusText: '正在播放原声示例'
    })
  },

  startProcess() {
    if (this.data.isProcessing) return

    if (this._processTimer) {
      clearTimeout(this._processTimer)
      this._processTimer = null
    }

    this.setData({
      taskStatus: 'processing',
      isProcessing: true,
      statusText: '正在模拟人工耳蜗声音...'
    })

    this._processTimer = setTimeout(() => {
      this._processTimer = null
      this.setData({
        taskStatus: 'success',
        isProcessing: false,
        statusText: '转换完成，可以试听模拟后声音'
      })
    }, 1200)
  },

  playProcessed() {
    if (this.data.taskStatus !== 'success') {
      wx.showToast({
        title: '请先完成转换',
        icon: 'none',
        duration: 2000
      })
      return
    }

    this.setData({
      statusText: '正在播放人工耳蜗模拟声音'
    })
  }
})
