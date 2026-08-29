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

function pseudoRandom(seed) {
  const x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453
  return x - Math.floor(x)
}

const FREQ_AXIS_MIN = 80
const FREQ_AXIS_MAX = 8000
const ELECTRODE_TOTAL = 22
const NEURAL_BAR_COUNT = 18

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
    selectedScenario: '',
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
    electrodeVisualList: [],
    clarityLevelClass: 'level-mid',
    freqCoverageLeft: '0%',
    freqCoverageWidth: '100%',
    freqCoverageLabel: '150 - 7000 Hz',
    neuralBars: [],
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
    clarityDesc: '',
    isAudioPlaying: false,
    playingKind: ''
  },

  _scenarioPresets: null,
  _sampleLabels: null,
  _sampleSourceCache: {},
  _samplePreparePromises: {},
  _unloaded: false,
  _autoRefreshTimer: null,
  _autoRefreshSeq: 0,
  _shouldAutoPlayProcessed: false,
  _runtimeParams: null,
  audioCtx: null,

  onLoad() {
    this._ensurePageState()
    this._scenarioPresets = { ...SCENARIO_PRESETS }
    this._sampleLabels = { ...SAMPLE_LABELS }
    this._syncRuntimeParamsFromData()
    this._loadRemoteData().then(() => {
      this._syncRuntimeParamsFromData()
      this._refreshVisualFeedback()
    })
    this._refreshVisualFeedback()
  },

  onUnload() {
    this._unloaded = true
    this._cancelAutoRefresh()
    this._stopAudio('已退出页面')
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
    this._refreshVisualFeedback()
  },

  _getClarityLevelClass(score) {
    const s = score != null && score !== '' ? Number(score) : 0
    if (Number.isNaN(s) || s < 24) return 'level-lowest'
    if (s < 44) return 'level-low'
    if (s < 66) return 'level-mid'
    if (s < 86) return 'level-good'
    return 'level-best'
  },

  _freqToAxisPercent(freq) {
    const lnMin = Math.log(FREQ_AXIS_MIN)
    const lnMax = Math.log(FREQ_AXIS_MAX)
    const lnSpan = lnMax - lnMin
    return clamp((Math.log(freq) - lnMin) / lnSpan, 0, 1) * 100
  },

  _getFrequencyCoverageStyle() {
    const { fLo, fHi } = this._parseFrequencyRange(this.data.frequencyRange)
    const left = this._freqToAxisPercent(fLo)
    const right = this._freqToAxisPercent(fHi)
    return {
      left: `${left.toFixed(2)}%`,
      width: `${Math.max(right - left, 1).toFixed(2)}%`
    }
  },

  _buildElectrodeVisualState() {
    const nChannels = Number(this.data.nChannels) || 8
    const spread = Number(this.data.spread) || 0
    const noiseLevel = Number(this.data.noiseLevel) || 0
    const isPlaying = this.data.isAudioPlaying
    const playingKind = this.data.playingKind
    const isProcessedPlaying = isPlaying && playingKind === 'processed'
    const isOriginalPlaying = isPlaying && playingKind === 'original'

    let spreadLevel = 'spread-low'
    if (spread >= 66) spreadLevel = 'spread-high'
    else if (spread >= 33) spreadLevel = 'spread-mid'

    const list = []
    for (let i = 0; i < ELECTRODE_TOTAL; i++) {
      const active = i < nChannels
      const centerDist = nChannels > 1 ? Math.abs(i - (nChannels - 1) / 2) / ((nChannels - 1) / 2) : 0
      const intensity = active ? clamp(1 - centerDist * 0.35, 0.55, 1) : 0.15
      const noiseFlicker = active && noiseLevel >= 25 && (isProcessedPlaying || noiseLevel >= 50)

      list.push({
        index: i,
        active,
        intensity,
        pulse: active && isProcessedPlaying,
        weakPulse: active && isOriginalPlaying && !isProcessedPlaying,
        spreadLevel: active ? spreadLevel : '',
        noiseFlicker,
        opacity: active ? (0.65 + intensity * 0.35).toFixed(2) : '0.2'
      })
    }
    return list
  },

  _buildNeuralBars() {
    const nChannels = Number(this.data.nChannels) || 8
    const spreadRatio = (Number(this.data.spread) || 0) / 100
    const noiseRatio = (Number(this.data.noiseLevel) || 0) / 100
    const envCut = Number(this.data.envCut) || 160
    const carrier = this.data.carrier || 'noise'
    const isProcessed = this.data.isAudioPlaying && this.data.playingKind === 'processed'
    const isOriginal = this.data.isAudioPlaying && this.data.playingKind === 'original'
    const isPlaying = this.data.isAudioPlaying

    const baseDuration = clamp(1.8 - ((envCut - 20) / 480) * 1.3, 0.45, 1.8)
    const channelFactor = clamp(nChannels / 22, 0.15, 1)
    const spreadUniform = clamp(1 - spreadRatio * 0.75, 0.2, 1)

    const bars = []
    for (let i = 0; i < NEURAL_BAR_COUNT; i++) {
      const seed = i + 1
      const sineWave = Math.sin((i / NEURAL_BAR_COUNT) * Math.PI * 2) * 0.5 + 0.5
      const rand = isPlaying && carrier === 'noise'
        ? pseudoRandom(seed + Date.now() % 97)
        : pseudoRandom(seed)

      const pattern = carrier === 'sine' ? sineWave : rand
      const blended = spreadUniform * pattern + (1 - spreadUniform) * 0.5
      let height = 18 + blended * 62 * channelFactor

      if (noiseRatio > 0) {
        const noiseJitter = (pseudoRandom(seed * 3.7) - 0.5) * noiseRatio * 36
        height += noiseJitter
      }

      if (!isPlaying) {
        height *= 0.55
      } else if (isOriginal) {
        height *= 0.72
      }

      height = clamp(height, 10, 96)

      const delay = carrier === 'sine'
        ? `${(i * 0.07).toFixed(2)}s`
        : `${(pseudoRandom(seed * 1.9) * 0.55).toFixed(2)}s`

      let duration = baseDuration
      if (carrier === 'noise') {
        duration *= 0.82 + pseudoRandom(seed * 2.3) * 0.36
      }

      let animClass = 'neural-bar--idle'
      if (isProcessed) animClass = 'neural-bar--active'
      else if (isOriginal) animClass = 'neural-bar--weak'

      bars.push({
        height: `${Math.round(height)}%`,
        delay,
        duration: `${duration.toFixed(2)}s`,
        animClass,
        flicker: noiseRatio >= 0.2 && isProcessed
      })
    }
    return bars
  },

  _refreshVisualFeedback() {
    if (this._unloaded) return

    const { score, grade, desc } = this._computeLocalClarity()
    const freqStyle = this._getFrequencyCoverageStyle()
    const { fLo, fHi } = this._parseFrequencyRange(this.data.frequencyRange)

    this.setData({
      clarityScore: score,
      clarityGrade: grade,
      clarityDesc: desc,
      clarityLevelClass: this._getClarityLevelClass(score),
      freqCoverageLeft: freqStyle.left,
      freqCoverageWidth: freqStyle.width,
      freqCoverageLabel: `${fLo} - ${fHi} Hz`,
      electrodeVisualList: this._buildElectrodeVisualState(),
      neuralBars: this._buildNeuralBars()
    })
  },

  _resetConvertStatus() {
    this._invalidateProcessedResult({ autoRefresh: false })
  },

  _cancelAutoRefresh() {
    if (this._autoRefreshTimer) {
      clearTimeout(this._autoRefreshTimer)
      this._autoRefreshTimer = null
    }
    this._shouldAutoPlayProcessed = false
    this._autoRefreshSeq += 1

    if (!this._unloaded && this.data.isProcessing) {
      this.setData({
        isProcessing: false,
        taskStatus: this._resolveReadyStatus()
      })
    }
  },

  _scheduleAutoRefreshProcessed() {
    if (this._autoRefreshTimer) {
      clearTimeout(this._autoRefreshTimer)
    }

    this._autoRefreshTimer = setTimeout(() => {
      this._autoRefreshTimer = null
      this._refreshProcessedForCurrentParams()
    }, 800)
  },

  async _refreshProcessedForCurrentParams() {
    if (!this._shouldAutoPlayProcessed) return

    const seq = this._autoRefreshSeq

    try {
      await this._generateProcessedAudio({
        autoPlay: true,
        replacePlaying: true,
        seq
      })
    } catch (err) {
      console.error(err)
    }
  },

  _resolveReadyStatus() {
    this._ensurePageState()
    if (this.data.sourceType === 'upload' && this.data.sourceAssetId) {
      return 'ready'
    }
    if (this.data.sourceType === 'sample') {
      const cache = this._sampleSourceCache[this.data.selectedSample]
      return cache ? 'ready' : 'idle'
    }
    return 'idle'
  },

  _syncRuntimeParamsFromData() {
    this._runtimeParams = {
      sourceType: this.data.sourceType,
      sourceAssetId: this.data.sourceAssetId || '',
      selectedSample: this.data.selectedSample || '',
      selectedScenario: this.data.selectedScenario || '',
      nChannels: this.data.nChannels,
      carrier: this.data.carrier,
      frequencyRange: this.data.frequencyRange,
      envCut: this.data.envCut,
      spread: this.data.spread,
      noiseLevel: this.data.noiseLevel
    }
  },

  _applyRuntimePatch(patch) {
    if (!this._runtimeParams) {
      this._syncRuntimeParamsFromData()
    }
    Object.assign(this._runtimeParams, patch)
  },

  _getRuntimeParams() {
    if (!this._runtimeParams) {
      this._syncRuntimeParamsFromData()
    }
    return this._runtimeParams
  },

  _buildProcessKey(params) {
    const p = params || this._getRuntimeParams()
    return [
      p.sourceType,
      p.sourceAssetId || '',
      p.selectedSample || '',
      p.selectedScenario || '',
      p.nChannels,
      p.carrier,
      p.frequencyRange,
      p.envCut,
      p.spread,
      p.noiseLevel
    ].join('|')
  },

  _invalidateProcessedResult(options = {}) {
    const wasPlayingProcessed = this.data.isAudioPlaying && this.data.playingKind === 'processed'

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

    if (wasPlayingProcessed && options.autoRefresh !== false) {
      patch.statusText = '参数已变化，正在更新模拟声...'
      this._shouldAutoPlayProcessed = true
      this._autoRefreshSeq += 1
      this._scheduleAutoRefreshProcessed()
    }

    this.setData(patch, () => {
      this._refreshVisualFeedback()
    })
  },

  _computeLocalClarity() {
    const params = this._getRuntimeParams()
    const spreadRatio = params.spread / 100
    const noiseRatio = params.noiseLevel / 100
    const eff = params.nChannels * (1 - 0.5 * spreadRatio)
    const specShow = clamp(1 - Math.pow(0.72, eff), 0, 1)
    const spec = Math.pow(specShow, 1.6)
    let pitch = clamp(Math.sqrt(Math.max(0, params.envCut - 20) / 480), 0, 1)
    if (params.carrier === 'sine') {
      pitch = clamp(pitch + 0.12, 0, 1)
    }
    const noiseMargin = clamp(1 - noiseRatio * 1.05, 0, 1)
    const { fLo, fHi } = this._parseFrequencyRange(params.frequencyRange)
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
    this._refreshVisualFeedback()
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
    this._applyRuntimePatch({
      sourceType: 'sample',
      sourceAssetId: sampleSource.assetId
    })
    this.setData({
      sourceType: 'sample',
      sourceAssetId: sampleSource.assetId,
      originalAudioUrl: sampleSource.url,
      uploadedFileName: sampleSource.fileName,
      uploadedObjectKey: sampleSource.objectKey,
      taskStatus: 'ready',
      statusText: '示例声音已准备好'
    }, () => {
      this._refreshVisualFeedback()
    })
  },

  _ensurePageState() {
    if (!this._sampleSourceCache || typeof this._sampleSourceCache !== 'object') {
      this._sampleSourceCache = {}
    }
    if (!this._samplePreparePromises || typeof this._samplePreparePromises !== 'object') {
      this._samplePreparePromises = {}
    }
    if (this._autoRefreshSeq == null) {
      this._autoRefreshSeq = 0
    }
    if (this._unloaded == null) {
      this._unloaded = false
    }
  },

  async _ensureSampleSource() {
    this._ensurePageState()
    const sampleCode = this.data.selectedSample
    const cached = this._sampleSourceCache[sampleCode]
    if (cached) {
      this._applySampleSourceToData(cached)
      return cached
    }

    const pending = this._samplePreparePromises[sampleCode]
    if (pending) {
      return pending
    }

    const preparePromise = (async () => {
      if (!this._unloaded) {
        this.setData({
          sourceType: 'sample',
          taskStatus: 'uploading',
          statusText: '正在准备示例声音...'
        })
      }

      try {
        const result = await prepareSampleSource(sampleCode)
        this._sampleSourceCache[sampleCode] = result
        if (!this._unloaded) {
          this._applySampleSourceToData(result)
        }
        return result
      } finally {
        delete this._samplePreparePromises[sampleCode]
      }
    })()

    this._samplePreparePromises[sampleCode] = preparePromise
    return preparePromise
  },

  _stopAudio(statusText = '已停止播放') {
    if (this.audioCtx) {
      try {
        this.audioCtx.stop()
      } catch (e) {}
      try {
        this.audioCtx.destroy()
      } catch (e) {}
      this.audioCtx = null
    }

    if (this._unloaded) return

    this.setData({
      isAudioPlaying: false,
      playingKind: '',
      statusText
    }, () => {
      this._refreshVisualFeedback()
    })
  },

  _playAudio(url, kind, onPlayText, onStopText, onErrorText, options = {}) {
    if (!url) {
      wx.showToast({
        title: '暂无可播放音频',
        icon: 'none'
      })
      return
    }

    if (this.data.isAudioPlaying && this.data.playingKind === kind) {
      if (options.forceRestart) {
        this._stopAudio('')
      } else {
        this._stopAudio(onStopText || '已停止播放')
        return
      }
    } else {
      this._stopAudio('')
    }

    const audioCtx = wx.createInnerAudioContext()
    this.audioCtx = audioCtx
    audioCtx.src = url
    audioCtx.loop = true

    audioCtx.onPlay(() => {
      if (this._unloaded) return
      this.setData({
        isAudioPlaying: true,
        playingKind: kind,
        statusText: onPlayText
      }, () => {
        this._refreshVisualFeedback()
      })
    })

    audioCtx.onStop(() => {
      if (this._unloaded) return
      this.setData({
        isAudioPlaying: false,
        playingKind: '',
        statusText: onStopText || '已停止播放'
      }, () => {
        this._refreshVisualFeedback()
      })
    })

    audioCtx.onEnded(() => {
      if (this._unloaded) return
      this.setData({
        isAudioPlaying: false,
        playingKind: '',
        statusText: onStopText || '播放结束'
      }, () => {
        this._refreshVisualFeedback()
      })
    })

    audioCtx.onError((err) => {
      console.error(err)
      if (this._unloaded) return
      this.setData({
        isAudioPlaying: false,
        playingKind: '',
        statusText: onErrorText
      }, () => {
        this._refreshVisualFeedback()
      })
      wx.showToast({
        title: '播放失败',
        icon: 'none'
      })
    })

    audioCtx.play()
  },

  selectSource(e) {
    this._ensurePageState()
    const type = e.currentTarget.dataset.type

    if (type === 'upload') {
      this._stopAudio('已停止播放')
      this._cancelAutoRefresh()
      this._applyRuntimePatch({
        sourceType: 'upload',
        sourceAssetId: '',
        selectedScenario: ''
      })
      this.setData({
        sourceType: 'upload',
        sourceAssetId: '',
        selectedScenario: '',
        sourceHint: ''
      })
      this._chooseAndUpload()
      return
    }

    let sourceHint = ''
    let statusText = '未选择音频'

    if (type === 'sample') {
      this._stopAudio('已停止播放')
      this._cancelAutoRefresh()
      const cache = this._sampleSourceCache[this.data.selectedSample]
      const label = this._getSampleLabel(this.data.selectedSample)
      this._applyRuntimePatch({
        sourceType: 'sample',
        sourceAssetId: cache ? cache.assetId : '',
        selectedSample: this.data.selectedSample,
        selectedScenario: ''
      })
      this.setData({
        isProcessing: false,
        sourceType: 'sample',
        sourceAssetId: cache ? cache.assetId : null,
        originalAudioUrl: cache ? cache.url : '',
        uploadedFileName: cache ? cache.fileName : '',
        uploadedObjectKey: cache ? cache.objectKey : '',
        taskStatus: cache ? 'ready' : 'idle',
        statusText: `已选择${label}`,
        sourceHint: '',
        selectedScenario: ''
      }, () => {
        this._invalidateProcessedResult({ autoRefresh: false })
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

    this._stopAudio('已停止播放')
    this._cancelAutoRefresh()
    this._invalidateProcessedResult({ autoRefresh: false })
    this.setData({
      sourceType: type,
      sourceHint,
      statusText,
      taskStatus: 'idle'
    })
  },

  selectSample(e) {
    this._ensurePageState()
    const code = e.currentTarget.dataset.code
    const cache = this._sampleSourceCache[code]
    const label = this._getSampleLabel(code)

    this._stopAudio('已停止播放')
    this._cancelAutoRefresh()
    this._applyRuntimePatch({
      sourceType: 'sample',
      selectedSample: code,
      sourceAssetId: cache ? cache.assetId : '',
      selectedScenario: ''
    })
    this.setData({
      sourceType: 'sample',
      selectedSample: code,
      sourceAssetId: cache ? cache.assetId : null,
      originalAudioUrl: cache ? cache.url : '',
      uploadedFileName: cache ? cache.fileName : '',
      uploadedObjectKey: cache ? cache.objectKey : '',
      isProcessing: false,
      taskStatus: cache ? 'ready' : 'idle',
      statusText: `已选择${label}`,
      selectedScenario: ''
    }, () => {
      this._syncRuntimeParamsFromData()
      this._invalidateProcessedResult({ autoRefresh: false })
    })
  },

  selectScenario(e) {
    const code = e.currentTarget.dataset.code
    const preset = this._getScenarioPreset(code)
    if (!preset) return

    const scenarioItem = this.data.scenarioList.find((item) => item.code === code)
    const scenarioName = scenarioItem ? scenarioItem.name : code

    this._applyRuntimePatch({
      selectedScenario: code,
      nChannels: preset.nChannels,
      frequencyRange: preset.frequencyRange,
      envCut: preset.envCut,
      spread: preset.spread,
      noiseLevel: preset.noiseLevel,
      carrier: preset.carrier || this.data.carrier
    })
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

  onChannelsChanging(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ nChannels: value, selectedScenario: '' })
    this.setData({
      nChannels: value,
      selectedScenario: ''
    }, () => {
      this._refreshVisualFeedback()
    })
  },

  changeChannels(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ nChannels: value, selectedScenario: '' })
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      selectedScenario: '',
      statusText: `已设置 ${value} 通道`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  quickSetChannels(e) {
    const value = Number(e.currentTarget.dataset.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ nChannels: value, selectedScenario: '' })
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      selectedScenario: '',
      statusText: `已设置 ${value} 通道`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  selectCarrier(e) {
    const value = e.currentTarget.dataset.value
    this._applyRuntimePatch({ carrier: value, selectedScenario: '' })
    this.setData({
      carrier: value,
      selectedScenario: '',
      statusText: value === 'noise' ? '已选择噪声载体' : '已选择正弦载体'
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  selectFrequencyRange(e) {
    const value = e.currentTarget.dataset.value
    this._applyRuntimePatch({ frequencyRange: value, selectedScenario: '' })
    this.setData({
      frequencyRange: value,
      selectedScenario: '',
      statusText: `频率范围已设为 ${value} Hz`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeEnvCut(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ envCut: value, selectedScenario: '' })
    this.setData({
      envCut: value,
      selectedScenario: '',
      statusText: `包络细节已设为 ${value} Hz`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeSpread(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ spread: value, selectedScenario: '' })
    this.setData({
      spread: value,
      selectedScenario: '',
      statusText: `电流扩散已设为 ${value}%`
    }, () => {
      this._invalidateProcessedResult()
    })
  },

  changeNoiseLevel(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ noiseLevel: value, selectedScenario: '' })
    this.setData({
      noiseLevel: value,
      selectedScenario: '',
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
    this._stopAudio('已停止播放')
    this._cancelAutoRefresh()
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
        this._syncRuntimeParamsFromData()
        this._invalidateProcessedResult({ keepStatus: true, autoRefresh: false })
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
          'original',
          '正在循环播放原声示例',
          '已停止原声示例播放',
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
        'original',
        '正在循环播放原声',
        '已停止原声播放',
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

  async _generateProcessedAudio(options = {}) {
    const { autoPlay = false, replacePlaying = false } = options
    const requestSeq = options.seq != null ? options.seq : (++this._autoRefreshSeq)

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
      throw err
    }

    const key = this._buildProcessKey(this._getRuntimeParams())

    if (this.data.processedAudioUrl && this.data.processedKey === key) {
      if (autoPlay) {
        this._playAudio(
          this.data.processedAudioUrl,
          'processed',
          '正在循环播放人工耳蜗模拟声音',
          '已停止模拟声音播放',
          '模拟声音播放失败，请检查文件地址'
        )
      }
      return
    }

    this.setData({
      isProcessing: true,
      taskStatus: 'processing',
      statusText: replacePlaying ? '正在按新参数更新模拟声...' : '正在生成模拟声音...',
      errorMessage: ''
    })

    try {
      if (this._runtimeParams && this._runtimeParams.sourceType !== this.data.sourceType) {
        this._syncRuntimeParamsFromData()
      }
      const params = this._getRuntimeParams()
      const { fLo, fHi } = this._parseFrequencyRange(params.frequencyRange)
      const effectiveSourceType = params.sourceType === this.data.sourceType
        ? params.sourceType
        : this.data.sourceType
      const taskPayload = {
        sourceType: effectiveSourceType === 'sample' ? 'SAMPLE' : 'UPLOAD',
        sourceAssetId,
        sampleCode: effectiveSourceType === 'sample' ? params.selectedSample : '',
        nChannels: Number(params.nChannels),
        carrier: params.carrier,
        fLo: Number(fLo),
        fHi: Number(fHi),
        envCut: Number(params.envCut),
        spread: Number(params.spread) / 100,
        noiseLevel: Number(params.noiseLevel) / 100
      }
      if (params.selectedScenario) {
        taskPayload.scenarioCode = params.selectedScenario
      }
      const result = await createTask(taskPayload)

      if (requestSeq !== this._autoRefreshSeq) {
        return
      }

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
        errorMessage: '',
        statusText: replacePlaying ? '模拟声已按新参数更新' : '正在循环播放人工耳蜗模拟声音'
      }, () => {
        this._refreshVisualFeedback()
      })

      if (autoPlay) {
        this._playAudio(
          result.processedAudioUrl,
          'processed',
          '正在循环播放人工耳蜗模拟声音',
          '已停止模拟声音播放',
          '模拟声音播放失败，请检查文件地址',
          { forceRestart: true }
        )
      }
    } catch (err) {
      if (requestSeq !== this._autoRefreshSeq) {
        return
      }
      console.error(err)
      const errorMessage = this._formatErrorMessage(err)
      this.setData({
        taskStatus: 'failed',
        isProcessing: false,
        statusText: '生成失败，请重试',
        errorMessage
      })
      if (!replacePlaying) {
        wx.showToast({ title: '生成失败', icon: 'none' })
      }
      throw err
    }
  },

  async playProcessedAuto() {
    const key = this._buildProcessKey()

    if (
      this.data.isAudioPlaying &&
      this.data.playingKind === 'processed' &&
      this.data.processedAudioUrl &&
      this.data.processedKey === key
    ) {
      this._cancelAutoRefresh()
      this._stopAudio('已停止模拟声音播放')
      return
    }

    if (this.data.isAudioPlaying && this.data.playingKind === 'processed') {
      this._cancelAutoRefresh()
      this._stopAudio('已停止模拟声音播放')
      return
    }

    if (this.data.isProcessing) return

    await this._generateProcessedAudio({
      autoPlay: true,
      replacePlaying: false
    })
  },

  playProcessed() {
    return this.playProcessedAuto()
  }
})
