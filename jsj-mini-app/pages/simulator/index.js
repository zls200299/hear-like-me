const { listScenarios } = require('../../services/scenario.js')
const { listSamples, prepareSampleSource } = require('../../services/sample.js')
const { uploadAudio } = require('../../services/file.js')
const { createTask } = require('../../services/audioTask.js')

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

function clamp(value, lo, hi) {
  return Math.max(lo, Math.min(hi, value))
}

Page({
  data: {
    sourceType: 'sample',
    sourceAssetId: null,
    originalAudioUrl: '',
    uploadedFileName: '',
    uploadedObjectKey: '',
    selectedSample: 'vowel',
    selectedScenario: 'quiet',
    nChannels: 8,
    carrier: 'noise',
    frequencyRange: '150-7000',
    envCut: 160,
    spread: 15,
    noiseLevel: 0,
    taskStatus: 'idle',
    statusText: '请选择音频来源',
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
    isProcessing: false,
    taskNo: '',
    outputAssetId: null,
    processedAudioUrl: '',
    processedKey: '',
    clarityScore: null,
    clarityGrade: '',
    errorMessage: '',
    clarityDesc: ''
  },

  _scenarioPresets: null,
  _sampleLabels: null,
  _sampleSourceCache: {},
  _preparingSampleCode: '',
  audioCtx: null,

  onLoad() {
    this._scenarioPresets = { ...SCENARIO_PRESETS }
    this._sampleLabels = { ...SAMPLE_LABELS }
    this._loadRemoteData().then(() => {
      this._updateLocalClarity()
    })
  },

  onUnload() {
    if (this.audioCtx) {
      this.audioCtx.destroy()
      this.audioCtx = null
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

  _resetConvertStatus() {
    this._invalidateProcessedResult()
  },

  _resolveReadyStatus() {
    if (this.data.sourceType === 'upload' && this.data.sourceAssetId) {
      return 'ready'
    }
    if (this.data.sourceType === 'sample') {
      const cache = this._sampleSourceCache[this.data.selectedSample]
      return cache ? 'ready' : 'idle'
    }
    return 'idle'
  },

  _buildProcessKey() {
    return [
      this.data.sourceType,
      this.data.sourceAssetId || '',
      this.data.selectedSample || '',
      this.data.selectedScenario || '',
      this.data.nChannels,
      this.data.carrier,
      this.data.frequencyRange,
      this.data.envCut,
      this.data.spread,
      this.data.noiseLevel
    ].join('|')
  },

  _invalidateProcessedResult(options = {}) {
    const patch = {
      processedAudioUrl: '',
      outputAssetId: null,
      processedKey: '',
      taskNo: '',
      errorMessage: ''
    }

    if (!options.keepStatus && !this.data.isProcessing && this.data.taskStatus !== 'uploading') {
      const status = this.data.taskStatus
      if (status === 'processing' || status === 'success' || status === 'failed') {
        patch.taskStatus = this._resolveReadyStatus()
      }
    }

    this.setData(patch, () => {
      this._updateLocalClarity()
    })
  },

  _computeLocalClarity() {
    const spreadRatio = this.data.spread / 100
    const noiseRatio = this.data.noiseLevel / 100
    const eff = this.data.nChannels * (1 - 0.5 * spreadRatio)
    const specShow = clamp(1 - Math.pow(0.72, eff), 0, 1)
    const spec = Math.pow(specShow, 1.6)
    let pitch = clamp(Math.sqrt(Math.max(0, this.data.envCut - 20) / 480), 0, 1)
    if (this.data.carrier === 'sine') {
      pitch = clamp(pitch + 0.12, 0, 1)
    }
    const noiseMargin = clamp(1 - noiseRatio * 1.05, 0, 1)
    const { fLo, fHi } = this._parseFrequencyRange(this.data.frequencyRange)
    const cover = clamp(
      (Math.log(fHi) - Math.log(fLo)) / (Math.log(8000) - Math.log(80)),
      0.4,
      1
    )
    const core = clamp(spec * 0.90 + pitch * 0.08, 0, 1)
    const s01 = clamp(core * (0.30 + 0.70 * noiseMargin) * (0.78 + 0.22 * cover), 0, 1)
    const score = Math.round(s01 * 100)

    let grade
    if (score < 24) grade = '几乎听不懂'
    else if (score < 44) grade = '很吃力'
    else if (score < 66) grade = '大致能懂'
    else if (score < 86) grade = '比较清楚'
    else grade = '接近清晰'

    return {
      score,
      grade,
      desc: this._getClarityDesc(score, grade)
    }
  },

  _updateLocalClarity() {
    const { score, grade, desc } = this._computeLocalClarity()
    this.setData({
      clarityScore: score,
      clarityGrade: grade,
      clarityDesc: desc
    })
  },

  _getClarityDesc(score, grade) {
    const num = score != null && score !== '' ? Number(score) : NaN
    if (!Number.isNaN(num)) {
      if (num >= 86) return '声音信息保留较多，接近清晰听感。'
      if (num >= 66) return '大部分语音轮廓可以听到，但细节仍有损失。'
      if (num >= 44) return '可以听到部分节奏和轮廓，理解会比较吃力。'
      if (num >= 24) return '声音被明显压缩，识别难度较高。'
      return '只保留了很少的信息，几乎难以听懂。'
    }
    return '当前参数下的可懂度参考，可对比试听原声和模拟声。'
  },

  _formatErrorMessage(err) {
    if (!err) return '未知错误'

    let text = ''
    if (typeof err === 'string') {
      text = err
    } else {
      text = err.message
        || (err.response && (err.response.msg || err.response.message))
        || (err.data && (err.data.msg || err.data.message))
        || err.errorMessage
        || '未知错误'
    }

    text = String(text)
      .replace(/[A-Za-z]:[\\/][^\s,;，；。]*/g, '本地文件路径')
      .replace(/[\\/][^\s,;，；。]*[\\/][^\s,;，；。]*/g, '本地文件路径')

    const maxLen = 80
    if (text.length > maxLen) {
      return text.substring(0, maxLen) + '…'
    }
    return text
  },

  _parseFrequencyRange(range) {
    const parts = String(range || '150-7000').split('-')
    return {
      fLo: Number(parts[0]) || 150,
      fHi: Number(parts[1]) || 7000
    }
  },

  _resolveSourceAssetId() {
    const url = this.data.originalAudioUrl || ''
    const match = String(url).match(/\/preview\/(\d+)$/)
    if (match) return match[1]
    const id = this.data.sourceAssetId
    return id != null && id !== '' ? String(id) : ''
  },

  _applySampleSourceToData(sampleSource) {
    this.setData({
      sourceType: 'sample',
      sourceAssetId: sampleSource.assetId,
      originalAudioUrl: sampleSource.url,
      uploadedFileName: sampleSource.fileName,
      uploadedObjectKey: sampleSource.objectKey,
      taskStatus: 'ready',
      statusText: '示例声音已准备好'
    })
  },

  async _ensureSampleSource() {
    const sampleCode = this.data.selectedSample
    const cached = this._sampleSourceCache[sampleCode]
    if (cached) {
      this._applySampleSourceToData(cached)
      return cached
    }

    if (this._preparingSampleCode === sampleCode) {
      throw new Error('示例声音正在准备中，请稍候')
    }

    this._preparingSampleCode = sampleCode
    this.setData({
      sourceType: 'sample',
      taskStatus: 'uploading',
      statusText: '正在准备示例声音...'
    })

    try {
      const result = await prepareSampleSource(sampleCode)
      this._sampleSourceCache[sampleCode] = result
      this._applySampleSourceToData(result)
      return result
    } finally {
      if (this._preparingSampleCode === sampleCode) {
        this._preparingSampleCode = ''
      }
    }
  },

  _playAudio(url, onPlayText, onEndedText, onErrorText) {
    if (!url) {
      wx.showToast({
        title: '暂无可播放音频',
        icon: 'none'
      })
      return
    }

    if (this.audioCtx) {
      this.audioCtx.stop()
      this.audioCtx.destroy()
      this.audioCtx = null
    }

    const audioCtx = wx.createInnerAudioContext()
    this.audioCtx = audioCtx
    audioCtx.src = url

    audioCtx.onPlay(() => {
      this.setData({ statusText: onPlayText })
    })

    audioCtx.onEnded(() => {
      this.setData({ statusText: onEndedText })
    })

    audioCtx.onError((err) => {
      console.error(err)
      wx.showToast({
        title: '播放失败',
        icon: 'none'
      })
      this.setData({ statusText: onErrorText })
    })

    audioCtx.play()
  },

  selectSource(e) {
    const type = e.currentTarget.dataset.type

    if (type === 'upload') {
      this.setData({
        sourceType: 'upload',
        sourceHint: ''
      })
      this._chooseAndUpload()
      return
    }

    let sourceHint = ''
    let statusText = '未选择音频'

    if (type === 'sample') {
      const cache = this._sampleSourceCache[this.data.selectedSample]
      const label = this._getSampleLabel(this.data.selectedSample)
      this.setData({
        isProcessing: false,
        sourceType: 'sample',
        sourceAssetId: cache ? cache.assetId : null,
        originalAudioUrl: cache ? cache.url : '',
        uploadedFileName: cache ? cache.fileName : '',
        uploadedObjectKey: cache ? cache.objectKey : '',
        taskStatus: cache ? 'ready' : 'idle',
        statusText: `已选择${label}`,
        sourceHint: ''
      }, () => {
        this._invalidateProcessedResult()
      })
      return
    }

    if (type === 'record') {
      sourceHint = '录制功能后续接入'
      statusText = '录制功能后续接入'
    } else if (type === 'realtime') {
      sourceHint = '实时麦克风功能需要真机验证，后续接入。'
      statusText = '实时麦克风后续接入'
    }

    this._resetConvertStatus()
    this.setData({
      sourceType: type,
      sourceHint,
      statusText,
      taskStatus: 'idle'
    })
  },

  selectSample(e) {
    const code = e.currentTarget.dataset.code
    const cache = this._sampleSourceCache[code]
    const label = this._getSampleLabel(code)

    this.setData({
      sourceType: 'sample',
      selectedSample: code,
      sourceAssetId: cache ? cache.assetId : null,
      originalAudioUrl: cache ? cache.url : '',
      uploadedFileName: cache ? cache.fileName : '',
      uploadedObjectKey: cache ? cache.objectKey : '',
      isProcessing: false,
      taskStatus: cache ? 'ready' : 'idle',
      statusText: `已选择${label}`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  selectScenario(e) {
    const code = e.currentTarget.dataset.code
    const preset = this._getScenarioPreset(code)
    if (!preset) return

    const scenarioItem = this.data.scenarioList.find((item) => item.code === code)
    const scenarioName = scenarioItem ? scenarioItem.name : code

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
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeChannels(e) {
    const value = Number(e.detail.value)
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      statusText: `已设置 ${value} 通道`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  quickSetChannels(e) {
    const value = Number(e.currentTarget.dataset.value)
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      statusText: `已设置 ${value} 通道`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  selectCarrier(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      carrier: value,
      statusText: value === 'noise' ? '已选择噪声载体' : '已选择正弦载体'
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  selectFrequencyRange(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      frequencyRange: value,
      statusText: `频率范围已设为 ${value} Hz`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeEnvCut(e) {
    const value = Number(e.detail.value)
    this.setData({
      envCut: value,
      statusText: `包络细节已设为 ${value} Hz`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeSpread(e) {
    const value = Number(e.detail.value)
    this.setData({
      spread: value,
      statusText: `电流扩散已设为 ${value}%`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeNoiseLevel(e) {
    const value = Number(e.detail.value)
    this.setData({
      noiseLevel: value,
      statusText: `环境噪声已设为 ${value}%`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  _chooseAndUpload() {
    if (this.data.taskStatus === 'uploading') return

    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['mp3', 'wav', 'm4a', 'aac'],
      success: (res) => {
        const file = res.tempFiles[0]
        if (!file || !file.path) return
        this._doUpload(file.path, file.name)
      },
      fail: (err) => {
        const errMsg = (err && err.errMsg) ? err.errMsg : ''
        if (errMsg.indexOf('cancel') !== -1 || errMsg.indexOf('取消') !== -1) {
          this.setData({
            sourceType: 'upload',
            statusText: '已选择上传音频，请选择文件',
            sourceHint: ''
          })
        }
      }
    })
  },

  async _doUpload(filePath, fileName) {
    this.setData({
      sourceType: 'upload',
      taskStatus: 'uploading',
      statusText: '正在上传音频...',
      uploadedFileName: fileName || '',
      sourceHint: '',
      isProcessing: false,
      errorMessage: '',
      clarityDesc: ''
    })

    try {
      const result = await uploadAudio(filePath)
      this.setData({
        sourceAssetId: result.assetId,
        originalAudioUrl: result.url,
        uploadedFileName: result.fileName || fileName,
        uploadedObjectKey: result.objectKey,
        taskNo: '',
        outputAssetId: null,
        processedAudioUrl: '',
        processedKey: '',
        taskStatus: 'ready',
        statusText: '音频上传成功，可以播放原声或模拟声',
        sourceHint: ''
      }, () => {
        this._invalidateProcessedResult({ keepStatus: true })
      })
    } catch (err) {
      console.error(err)
      this.setData({
        taskStatus: 'idle',
        statusText: '音频上传失败，请重试',
        sourceHint: '',
        errorMessage: ''
      })
      wx.showToast({
        title: '上传失败',
        icon: 'none'
      })
    }
  },

  async playOriginal() {
    if (this.data.sourceType === 'sample') {
      try {
        const sampleSource = await this._ensureSampleSource()
        this._playAudio(
          sampleSource.url,
          '正在播放原声示例',
          '原声示例播放结束',
          '原声示例播放失败'
        )
      } catch (err) {
        console.error(err)
        const errorMessage = this._formatErrorMessage(err)
        this.setData({
          taskStatus: 'failed',
          statusText: '示例声音准备失败',
          errorMessage
        })
        wx.showToast({
          title: '示例准备失败',
          icon: 'none'
        })
      }
      return
    }

    if (this.data.sourceType === 'upload') {
      if (!this.data.originalAudioUrl) {
        wx.showToast({
          title: '请先上传音频',
          icon: 'none'
        })
        return
      }

      this._playAudio(
        this.data.originalAudioUrl,
        '正在播放原声',
        '原声播放结束',
        '原声播放失败，请检查文件地址'
      )
      return
    }

    if (this.data.sourceType === 'record') {
      wx.showToast({ title: '录制功能后续接入', icon: 'none' })
      return
    }

    if (this.data.sourceType === 'realtime') {
      wx.showToast({ title: '实时麦克风功能后续接入', icon: 'none' })
      return
    }
  },

  async playProcessedAuto() {
    if (this.data.isProcessing) return

    if (this.data.sourceType === 'record') {
      wx.showToast({ title: '录制功能后续接入', icon: 'none' })
      return
    }

    if (this.data.sourceType === 'realtime') {
      wx.showToast({ title: '实时麦克风功能后续接入', icon: 'none' })
      return
    }

    let sourceAssetId = ''

    try {
      if (this.data.sourceType === 'sample') {
        const sampleSource = await this._ensureSampleSource()
        sourceAssetId = sampleSource.assetId
      } else if (this.data.sourceType === 'upload') {
        sourceAssetId = this._resolveSourceAssetId()
        if (!sourceAssetId) {
          wx.showToast({ title: '请先上传音频', icon: 'none' })
          return
        }
      } else {
        return
      }
    } catch (err) {
      console.error(err)
      const errorMessage = this._formatErrorMessage(err)
      this.setData({
        taskStatus: 'failed',
        statusText: '示例声音准备失败',
        errorMessage
      })
      wx.showToast({ title: '示例准备失败', icon: 'none' })
      return
    }

    const key = this._buildProcessKey()

    if (this.data.processedAudioUrl && this.data.processedKey === key) {
      this._playAudio(
        this.data.processedAudioUrl,
        '正在播放人工耳蜗模拟声音',
        '模拟声音播放结束',
        '模拟声音播放失败，请检查文件地址'
      )
      return
    }

    const { fLo, fHi } = this._parseFrequencyRange(this.data.frequencyRange)

    this.setData({
      isProcessing: true,
      taskStatus: 'processing',
      statusText: '正在生成模拟声音...',
      errorMessage: ''
    })

    try {
      const result = await createTask({
        sourceType: this.data.sourceType === 'sample' ? 'SAMPLE' : 'UPLOAD',
        sourceAssetId,
        sampleCode: this.data.sourceType === 'sample' ? this.data.selectedSample : '',
        scenarioCode: this.data.selectedScenario,
        nChannels: this.data.nChannels,
        carrier: this.data.carrier,
        fLo,
        fHi,
        envCut: this.data.envCut,
        spread: this.data.spread / 100,
        noiseLevel: this.data.noiseLevel / 100
      })

      if (!result || result.status !== 'SUCCESS' || !result.processedAudioUrl) {
        throw new Error((result && result.errorMessage) ? result.errorMessage : '生成失败')
      }

      const clarityScore = result.clarityScore != null ? result.clarityScore : this.data.clarityScore
      const clarityGrade = result.clarityGrade || this.data.clarityGrade || '模拟完成'
      const clarityDesc = this._getClarityDesc(clarityScore, clarityGrade)

      this.setData({
        taskNo: result.taskNo,
        outputAssetId: result.outputAssetId,
        processedAudioUrl: result.processedAudioUrl,
        processedKey: key,
        clarityScore,
        clarityGrade,
        clarityDesc,
        taskStatus: 'success',
        isProcessing: false,
        statusText: '正在播放人工耳蜗模拟声音',
        errorMessage: ''
      })

      this._playAudio(
        result.processedAudioUrl,
        '正在播放人工耳蜗模拟声音',
        '模拟声音播放结束',
        '模拟声音播放失败，请检查文件地址'
      )
    } catch (err) {
      console.error(err)
      const errorMessage = this._formatErrorMessage(err)
      this.setData({
        taskStatus: 'failed',
        isProcessing: false,
        statusText: '生成失败，请重试',
        errorMessage
      })
      wx.showToast({ title: '生成失败', icon: 'none' })
    }
  },

  playProcessed() {
    return this.playProcessedAuto()
  }
})
