const { listScenarios } = require('../../services/scenario.js')
const { listSamples, prepareSampleSource } = require('../../services/sample.js')
const { uploadAudio } = require('../../services/file.js')
const { createTask } = require('../../services/audioTask.js')
const { createSeamlessAudioPlayer } = require('../../utils/simulator/seamlessAudioPlayer.js')
const { createRealtimePcmPlayer } = require('../../utils/simulator/realtimePcmPlayer.js')
const config = require('../../config.js')

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

const ELECTRODE_TOTAL = 22

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
    audioSeekSec: 0,
    clarityLevelClass: 'level-mid',
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
    playingKind: '',
    listenHint: '播放模拟声时会按当前参数生成。',
    realtimeRecording: false,
    realtimeRecorderReady: false,
    realtimeFrameCount: 0,
    realtimeLastFrameBytes: 0,
    realtimeTotalBytes: 0,
    realtimeError: '',
    realtimeStatusText: '未开始实时体验',
    realtimeSocketConnected: false,
    realtimeSentFrames: 0,
    realtimeReceivedFrames: 0,
    realtimeSentBytes: 0,
    realtimeReceivedBytes: 0,
    realtimeLastRttMs: null,
    realtimeAvgRttMs: null,
    realtimeLostFrames: 0,
    realtimePlaybackStarted: false,
    realtimePlaybackUnderruns: 0,
    realtimeBufferedMs: 0,
    realtimePlaybackPendingFrames: 0,
    realtimePlaybackDroppedFrames: 0,
    realtimePlaybackRecoveryCount: 0
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
  _audioPlayer: null,
  _processedResultCache: null,
  _recorderManager: null,
  _realtimeRecorderBound: false,
  _realtimeStats: null,
  _realtimeStarting: false,
  _realtimeSocket: null,
  _realtimeSocketReady: false,
  _realtimeSeq: 0,
  _realtimePendingFrames: null,
  _realtimeRttTotal: 0,
  _realtimeRttCount: 0,
  _realtimePendingSweepTimer: null,
  _realtimePcmPlayer: null,
  _realtimeReadyTimeoutTimer: null,
  _realtimeParamPromise: null,
  _realtimeParamTimeoutTimer: null,

  onLoad() {
    this._audioPlayer = createSeamlessAudioPlayer()
    this._ensurePageState()
    this._initRealtimeRecorder()
    this._scenarioPresets = { ...SCENARIO_PRESETS }
    this._sampleLabels = { ...SAMPLE_LABELS }
    this._syncRuntimeParamsFromData()
    this._loadRemoteData().then(() => {
      this._syncRuntimeParamsFromData()
      this._refreshVisualFeedback()
    })
    this._refreshVisualFeedback()
  },

  onShow() {
    if (this._unloaded) return
    this._syncVisualPlaybackState()
  },

  onReady() {
    wx.nextTick(() => {
      this._syncVisualPlaybackState()
      const panel = this._getVisualPanel()
      if (panel) panel.refreshViews()
    })
  },

  onUnload() {
    this._unloaded = true
    this._cancelAutoRefresh()
    this._stopAudio('已退出页面')
    if ((this.data.realtimeRecording || this._realtimeStarting) && this._recorderManager) {
      try {
        this._recorderManager.stop()
      } catch (e) {}
      this._realtimeStarting = false
    }
    this._closeRealtimeSocket({ silent: true })
    this._destroyRealtimePcmPlayer()
    if (this._audioPlayer) {
      this._audioPlayer.destroy()
      this._audioPlayer = null
    }
    if (this._processedResultCache) {
      this._processedResultCache.clear()
      this._processedResultCache = null
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

  _getVisualPanel() {
    if (!this._visualPanel) {
      this._visualPanel = this.selectComponent('#visualPanel')
    }
    return this._visualPanel
  },

  _syncVisualPlaybackState() {
    const panel = this._getVisualPanel()
    if (!panel) return
    const patch = {}
    if (this._audioPlayer && this._audioPlayer.isPlaying()) {
      patch.audioSeekSec = this._audioPlayer.getCurrentTime()
    }
    if (Object.keys(patch).length) {
      this.setData(patch, () => panel.syncPlaybackState())
      return
    }
    panel.syncPlaybackState()
  },

  _applyVisualizationData(visualizationData) {
    const panel = this._getVisualPanel()
    if (panel) {
      panel.applyVisualizationData(visualizationData)
    }
  },

  _applyVisualizationFromResult(result) {
    this._applyVisualizationData(result && result.visualizationData)
  },

  _clearVisualizationData() {
    const panel = this._getVisualPanel()
    if (panel) panel.clearVisualizationData()
  },

  _refreshVisualFeedback() {
    if (this._unloaded) return

    const { score, grade, desc } = this._computeLocalClarity()
    this.setData({
      clarityScore: score,
      clarityGrade: grade,
      clarityDesc: desc,
      clarityLevelClass: this._getClarityLevelClass(score),
      listenHint: this._resolveListenHint()
    })

    const panel = this._getVisualPanel()
    if (panel) panel.refreshViews()
  },

  _resolveListenHint() {
    if (this.data.isProcessing && this.data.isAudioPlaying && this.data.playingKind === 'processed') {
      return '正在按新参数更新模拟声，当前仍在播放上一版声音。'
    }
    if (this.data.isProcessing && this.data.taskStatus === 'processing') {
      return '正在生成模拟声音，请稍候...'
    }
    if (this.data.processedAudioUrl) {
      return '当前模拟声已生成，可循环试听。'
    }
    return '播放模拟声时会按当前参数生成。'
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
    const cacheApplied = wasPlayingProcessed && options.autoRefresh !== false
      ? this._tryApplyCachedCurrentResult()
      : false
    const shouldKeepVisualization = wasPlayingProcessed && options.autoRefresh !== false

    const patch = {
      errorMessage: '',
    }

    if (!cacheApplied) {
      patch.processedAudioUrl = ''
      patch.outputAssetId = null
      patch.processedKey = ''
      patch.taskNo = ''
    }

    if (!cacheApplied && !options.keepStatus && !this.data.isProcessing && this.data.taskStatus !== 'uploading') {
      const status = this.data.taskStatus
      if (status === 'processing' || status === 'success' || status === 'failed') {
        patch.taskStatus = this._resolveReadyStatus()
      }
    }

    if (wasPlayingProcessed && options.autoRefresh !== false) {
      if (cacheApplied) {
        this._shouldAutoPlayProcessed = false
      } else {
        patch.statusText = '参数已变化，正在更新模拟声...'
        this._shouldAutoPlayProcessed = true
        this._autoRefreshSeq += 1
        this._scheduleAutoRefreshProcessed()
      }
    }

    this.setData(patch, () => {
      if (!shouldKeepVisualization) {
        this._clearVisualizationData()
      }
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
    if (!this._processedResultCache) {
      this._processedResultCache = new Map()
    }
  },

  _getCachedProcessedResult(key) {
    const cache = this._processedResultCache
    if (!cache || !cache.has(key)) return null

    const value = cache.get(key)
    cache.delete(key)
    cache.set(key, value)
    return value
  },

  _setCachedProcessedResult(key, value) {
    if (!this._processedResultCache) {
      this._processedResultCache = new Map()
    }

    const cache = this._processedResultCache

    if (cache.has(key)) {
      cache.delete(key)
    }

    cache.set(key, value)

    while (cache.size > 12) {
      const oldestKey = cache.keys().next().value
      cache.delete(oldestKey)
    }
  },

  _tryApplyCachedCurrentResult() {
    const key = this._buildProcessKey(this._getRuntimeParams())
    const cached = this._getCachedProcessedResult(key)
    if (!cached) {
      return false
    }

    if (this._autoRefreshTimer) {
      clearTimeout(this._autoRefreshTimer)
      this._autoRefreshTimer = null
    }

    this._autoRefreshSeq += 1

    console.log('[processed-cache] hit', key)
    this._applyProcessedResult(key, cached, {
      autoPlay: true,
      replacePlaying: true,
      fromCache: true
    })

    return true
  },

  _applyProcessedResult(key, result, options = {}) {
    const { autoPlay = false, replacePlaying = false, fromCache = false } = options

    const statusText = fromCache
      ? '已使用缓存的模拟结果'
      : replacePlaying
        ? '模拟声已按新参数更新'
        : '模拟声音已生成'

    this.setData({
      taskNo: result.taskNo || '',
      outputAssetId: result.outputAssetId || null,
      processedAudioUrl: result.processedAudioUrl,
      processedKey: key,
      clarityScore: result.clarityScore,
      clarityGrade: result.clarityGrade,
      clarityDesc: result.clarityDesc,
      taskStatus: 'success',
      isProcessing: false,
      errorMessage: '',
      statusText
    }, () => {
      this._refreshVisualFeedback()
      if (!autoPlay && !(this.data.isAudioPlaying && this.data.playingKind === 'processed')) {
        this._applyVisualizationData(result.visualizationData)
      }
    })

    if (autoPlay) {
      this._playAudio(
        result.processedAudioUrl,
        'processed',
        '正在循环播放人工耳蜗模拟声音',
        '已停止模拟声音播放',
        '模拟声音播放失败，请检查文件地址',
        {
          forceRestart: true,
          visualizationData: result.visualizationData
        }
      )
    }
  },

  _initRealtimeRecorder() {
    if (this._realtimeRecorderBound) return

    this._recorderManager = wx.getRecorderManager()
    const recorder = this._recorderManager

    recorder.onStart(() => {
      this._realtimeStarting = false
      this._realtimeStats = { frameCount: 0, totalBytes: 0 }
      if (this._unloaded) return
      this.setData({
        realtimeRecording: true,
        realtimeRecorderReady: true,
        realtimeFrameCount: 0,
        realtimeLastFrameBytes: 0,
        realtimeTotalBytes: 0,
        realtimeError: '',
        realtimeStatusText: '正在采集麦克风音频'
      })
    })

    recorder.onFrameRecorded((res) => {
      const frameBuffer = res && res.frameBuffer
      if (!(frameBuffer instanceof ArrayBuffer)) return

      const bytes = frameBuffer.byteLength || 0
      if (!this._realtimeStats) {
        this._realtimeStats = { frameCount: 0, totalBytes: 0 }
      }
      this._realtimeStats.frameCount += 1
      this._realtimeStats.totalBytes += bytes

      console.log(
        '[realtime-recorder]',
        'frame=', this._realtimeStats.frameCount,
        'bytes=', bytes,
        'last=', !!(res && res.isLastFrame)
      )

      if (this._unloaded) return
      this.setData({
        realtimeFrameCount: this._realtimeStats.frameCount,
        realtimeLastFrameBytes: bytes,
        realtimeTotalBytes: this._realtimeStats.totalBytes
      })

      this._sendRealtimeFrame(frameBuffer, bytes)
    })

    recorder.onStop(() => {
      this._realtimeStarting = false
      if (this._unloaded) return
      this.setData({
        realtimeRecording: false,
        realtimeStatusText: '实时麦克风已停止'
      })
    })

    recorder.onError((err) => {
      this._realtimeStarting = false
      console.error('[realtime-recorder] error', err)
      this._closeRealtimeSocket()
      if (this._unloaded) return
      this.setData({
        realtimeRecording: false,
        realtimeError: (err && err.errMsg) ? err.errMsg : '录音失败',
        realtimeStatusText: '麦克风录音失败'
      })
    })

    recorder.onInterruptionBegin(() => {
      if (this._unloaded) return
      this.setData({
        realtimeRecording: false,
        realtimeStatusText: '麦克风被系统音频任务中断'
      })
    })

    recorder.onInterruptionEnd(() => {
      if (this._unloaded) return
      this.setData({
        realtimeStatusText: '系统音频中断已结束，请重新开始实时体验'
      })
    })

    this._realtimeRecorderBound = true
  },

  _ensureRecordPermission() {
    return new Promise((resolve) => {
      wx.getSetting({
        success: (settingRes) => {
          if (settingRes.authSetting && settingRes.authSetting['scope.record']) {
            resolve(true)
            return
          }
          wx.authorize({
            scope: 'scope.record',
            success: () => resolve(true),
            fail: () => {
              wx.showModal({
                title: '需要麦克风权限',
                content: '需要麦克风权限才能使用实时体验',
                confirmText: '去设置',
                cancelText: '取消',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.openSetting()
                  }
                }
              })
              resolve(false)
            }
          })
        },
        fail: () => resolve(false)
      })
    })
  },

  _startRealtimeRecorder() {
    if (!this._recorderManager) {
      this._initRealtimeRecorder()
    }
    this._recorderManager.start({
      duration: 600000,
      sampleRate: 44100,
      numberOfChannels: 1,
      encodeBitRate: 128000,
      format: 'pcm',
      frameSize: 4
    })
  },

  _connectRealtimeSocket() {
    return new Promise((resolve, reject) => {
      this._closeRealtimeSocket({ silent: true })

      this._realtimeSeq = 0
      this._realtimePendingFrames = new Map()
      this._realtimeRttTotal = 0
      this._realtimeRttCount = 0
      this._startRealtimePendingSweep()

      const url = `${config.wsBaseUrl}/ws/realtime/echo`
      this._realtimeConnectSettled = false

      const socketTask = wx.connectSocket({ url })
      this._realtimeSocket = socketTask
      this._realtimeConnectResolve = resolve
      this._realtimeConnectReject = reject

      const failConnect = (err) => {
        if (this._realtimeConnectSettled) return
        this._realtimeConnectSettled = true
        this._clearRealtimeReadyTimeout()
        this._realtimeSocketReady = false
        this._realtimeConnectResolve = null
        this._realtimeConnectReject = null
        reject(err || new Error('WebSocket 连接失败'))
      }

      socketTask.onMessage((res) => {
        this._handleRealtimeSocketMessage(res)
      })

      socketTask.onOpen(() => {
        if (this._unloaded) return
        this._clearRealtimeReadyTimeout()
        this._realtimeReadyTimeoutTimer = setTimeout(() => {
          if (this._realtimeConnectSettled) return
          failConnect(new Error('声码器初始化超时'))
          this._closeRealtimeSocket({ silent: true })
          if (!this._unloaded) {
            this.setData({
              realtimeError: '声码器初始化超时，请检查服务端 Python 环境',
              realtimeStatusText: '声码器初始化失败'
            })
          }
        }, 20000)
        this.setData({
          realtimeStatusText: '实时连接已建立，正在初始化声码器...'
        })
      })

      socketTask.onClose(() => {
        this._realtimeSocketReady = false
        if (!this._realtimeConnectSettled) {
          failConnect(new Error('WebSocket closed before READY'))
        }
        if (!this._unloaded) {
          this.setData({ realtimeSocketConnected: false })
        }
      })

      socketTask.onError((err) => {
        console.error('[realtime-ws] error', err)
        failConnect(err)
        if (!this._unloaded) {
          this.setData({
            realtimeSocketConnected: false,
            realtimeError: (err && err.errMsg) ? err.errMsg : '实时连接失败',
            realtimeStatusText: '实时连接失败，请检查网络或服务端'
          })
        }
      })
    })
  },

  _onRealtimeSocketReady() {
    if (this._realtimeConnectSettled) return
    this._realtimeConnectSettled = true
    this._clearRealtimeReadyTimeout()
    this._realtimeSocketReady = true

    if (this._realtimeConnectResolve) {
      this._realtimeConnectResolve()
      this._realtimeConnectResolve = null
      this._realtimeConnectReject = null
    }

    if (this._unloaded) return
    this.setData({
      realtimeSocketConnected: true,
      realtimeSentFrames: 0,
      realtimeReceivedFrames: 0,
      realtimeSentBytes: 0,
      realtimeReceivedBytes: 0,
      realtimeLastRttMs: null,
      realtimeAvgRttMs: null,
      realtimeLostFrames: 0,
      realtimeStatusText: '声码器已就绪，正在采集麦克风音频',
      realtimePlaybackStarted: false,
      realtimePlaybackUnderruns: 0,
      realtimeBufferedMs: 0,
      realtimePlaybackPendingFrames: 0,
      realtimePlaybackDroppedFrames: 0,
      realtimePlaybackRecoveryCount: 0
    })
  },

  _buildRealtimeParams() {
    const params = this._getRuntimeParams()
    const { fLo, fHi } = this._parseFrequencyRange(params.frequencyRange)
    return {
      nChannels: params.nChannels,
      carrier: params.carrier,
      fLo,
      fHi,
      envCut: params.envCut,
      spread: params.spread / 100,
      noiseLevel: params.noiseLevel / 100
    }
  },

  _clearRealtimeParamPromise(error) {
    if (this._realtimeParamTimeoutTimer) {
      clearTimeout(this._realtimeParamTimeoutTimer)
      this._realtimeParamTimeoutTimer = null
    }
    if (!this._realtimeParamPromise) return
    const pending = this._realtimeParamPromise
    this._realtimeParamPromise = null
    if (error) {
      pending.reject(error)
    }
  },

  _resolveRealtimeParamPromise(msg) {
    if (this._realtimeParamTimeoutTimer) {
      clearTimeout(this._realtimeParamTimeoutTimer)
      this._realtimeParamTimeoutTimer = null
    }
    if (!this._realtimeParamPromise) return
    const pending = this._realtimeParamPromise
    this._realtimeParamPromise = null
    pending.resolve(msg)
  },

  _sendRealtimeParams() {
    return new Promise((resolve, reject) => {
      if (!this._realtimeSocketReady || !this._realtimeSocket) {
        reject(new Error('实时连接未就绪'))
        return
      }

      this._clearRealtimeParamPromise(new Error('参数更新被新的请求取代'))
      this._realtimeParamPromise = { resolve, reject }
      this._realtimeParamTimeoutTimer = setTimeout(() => {
        if (!this._realtimeParamPromise) return
        this._clearRealtimeParamPromise(new Error('参数同步超时'))
      }, 8000)

      const payload = JSON.stringify({
        type: 'PARAM_UPDATE',
        version: 1,
        params: this._buildRealtimeParams()
      })

      this._realtimeSocket.send({
        data: payload,
        fail: (err) => {
          this._clearRealtimeParamPromise(err || new Error('参数发送失败'))
        }
      })
    })
  },

  _maybeSendRealtimeParams() {
    if (this.data.sourceType !== 'realtime') return
    if (!this._realtimeSocketReady) return
    if (!this.data.realtimeRecording && !this._realtimeStarting) return

    this._sendRealtimeParams().catch((err) => {
      console.warn('[realtime-ws] param update failed', err)
      if (!this._unloaded) {
        this.setData({
          realtimeError: (err && err.message) ? err.message : '实时参数更新失败'
        })
      }
    })
  },

  async _initRealtimePcmPlayer() {
    this._destroyRealtimePcmPlayer()

    const player = createRealtimePcmPlayer({
      onState: (state) => {
        if (this._unloaded) return
        this.setData({
          realtimePlaybackStarted: !!state.started,
          realtimePlaybackUnderruns: state.underruns || 0,
          realtimeBufferedMs: state.bufferedMs || 0,
          realtimePlaybackPendingFrames: state.pendingFrames || 0,
          realtimePlaybackDroppedFrames: state.droppedFrames || 0,
          realtimePlaybackRecoveryCount: state.recoveryCount || 0
        })
      }
    })

    this._realtimePcmPlayer = player
    await player.init()
  },

  _destroyRealtimePcmPlayer() {
    if (this._realtimePcmPlayer) {
      this._realtimePcmPlayer.destroy()
      this._realtimePcmPlayer = null
    }

    if (!this._unloaded) {
      this.setData({
        realtimePlaybackStarted: false,
        realtimePlaybackUnderruns: 0,
        realtimeBufferedMs: 0,
        realtimePlaybackPendingFrames: 0,
        realtimePlaybackDroppedFrames: 0,
        realtimePlaybackRecoveryCount: 0
      })
    }
  },

  _sendRealtimeFrame(frameBuffer, bytes) {
    if (!this._realtimeSocketReady || !this._realtimeSocket) return

    const seq = ++this._realtimeSeq
    const pcm = new Uint8Array(frameBuffer)
    const packet = new ArrayBuffer(4 + pcm.byteLength)
    const view = new DataView(packet)
    view.setUint32(0, seq, false)
    new Uint8Array(packet, 4).set(pcm)

    if (!this._realtimePendingFrames) {
      this._realtimePendingFrames = new Map()
    }
    this._realtimePendingFrames.set(seq, {
      sentAt: Date.now(),
      bytes
    })

    this._realtimeSocket.send({
      data: packet,
      fail: (err) => {
        console.warn('[realtime-ws] send failed', err)
      }
    })

    if (this._unloaded) return
    this.setData({
      realtimeSentFrames: this.data.realtimeSentFrames + 1,
      realtimeSentBytes: this.data.realtimeSentBytes + bytes
    })
  },

  _clearRealtimeReadyTimeout() {
    if (!this._realtimeReadyTimeoutTimer) return
    clearTimeout(this._realtimeReadyTimeoutTimer)
    this._realtimeReadyTimeoutTimer = null
  },

  _decodeRealtimeSocketText(data) {
    if (typeof data === 'string') return data
    if (!(data instanceof ArrayBuffer)) return ''

    const bytes = new Uint8Array(data)
    let text = ''
    for (let i = 0; i < bytes.length; i++) {
      text += String.fromCharCode(bytes[i])
    }
    return text
  },

  _tryHandleRealtimeControlMessage(data) {
    const text = this._decodeRealtimeSocketText(data)
    if (!text || text.charCodeAt(0) !== 123) return false

    try {
      const msg = JSON.parse(text)
      if (msg && msg.type === 'READY') {
        this._onRealtimeSocketReady()
        return true
      }
      if (msg && msg.type === 'PARAM_APPLIED') {
        this._resolveRealtimeParamPromise(msg)
        return true
      }
      if (msg && msg.type === 'PARAM_ERROR') {
        const err = new Error((msg && msg.message) ? msg.message : '参数更新失败')
        this._clearRealtimeParamPromise(err)
        return true
      }
    } catch (e) {
      console.warn('[realtime-ws] ignore invalid control message', text)
    }
    return false
  },

  _handleRealtimeSocketMessage(res) {
    const data = res && res.data
    if (this._tryHandleRealtimeControlMessage(data)) return
    if (!(data instanceof ArrayBuffer)) {
      console.warn('[realtime-ws] ignore unsupported message type')
      return
    }
    if (data.byteLength < 4) return

    const view = new DataView(data)
    const seq = view.getUint32(0, false)
    const pcmBytes = data.byteLength - 4

    const pending = this._realtimePendingFrames && this._realtimePendingFrames.get(seq)
    if (!pending) return

    const rtt = Date.now() - pending.sentAt
    this._realtimePendingFrames.delete(seq)

    if (pcmBytes !== pending.bytes) {
      console.warn('[realtime-ws] echo size mismatch', {
        seq,
        pcmBytes,
        expected: pending.bytes
      })
    }

    if (pcmBytes > 0 && this._realtimePcmPlayer) {
      const pcmBuffer = data.slice(4)
      this._realtimePcmPlayer.enqueue(pcmBuffer)
    }

    this._realtimeRttTotal += rtt
    this._realtimeRttCount += 1
    const avgRtt = Math.round(this._realtimeRttTotal / this._realtimeRttCount)

    if (this._unloaded) return
    this.setData({
      realtimeReceivedFrames: this.data.realtimeReceivedFrames + 1,
      realtimeReceivedBytes: this.data.realtimeReceivedBytes + pcmBytes,
      realtimeLastRttMs: rtt,
      realtimeAvgRttMs: avgRtt
    })
  },

  _startRealtimePendingSweep() {
    this._stopRealtimePendingSweep()
    this._realtimePendingSweepTimer = setInterval(() => {
      this._sweepRealtimePendingFrames()
    }, 2000)
  },

  _stopRealtimePendingSweep() {
    if (!this._realtimePendingSweepTimer) return
    clearInterval(this._realtimePendingSweepTimer)
    this._realtimePendingSweepTimer = null
  },

  _sweepRealtimePendingFrames() {
    const pending = this._realtimePendingFrames
    if (!pending || !pending.size) return

    const now = Date.now()
    let lost = 0
    pending.forEach((item, seq) => {
      if (now - item.sentAt > 3000) {
        pending.delete(seq)
        lost += 1
      }
    })

    if (lost > 0 && !this._unloaded) {
      this.setData({
        realtimeLostFrames: this.data.realtimeLostFrames + lost
      })
    }
  },

  _closeRealtimeSocket(options = {}) {
    const { silent = false } = options
    this._stopRealtimePendingSweep()
    this._clearRealtimeReadyTimeout()
    this._realtimeSocketReady = false
    this._realtimeConnectSettled = false
    this._realtimeConnectResolve = null
    this._realtimeConnectReject = null
    this._clearRealtimeParamPromise(new Error('实时连接已关闭'))

    if (this._realtimeSocket) {
      try {
        this._realtimeSocket.close({
          code: 1000,
          reason: 'user stop'
        })
      } catch (e) {
        console.warn('[realtime-ws] close failed', e)
      }
      this._realtimeSocket = null
    }

    this._realtimePendingFrames = null

    if (!silent && !this._unloaded) {
      this.setData({ realtimeSocketConnected: false })
    }
  },

  async startRealtimeMic() {
    if (this.data.realtimeRecording || this._realtimeStarting) return

    const granted = await this._ensureRecordPermission()
    if (!granted) return

    if (this.data.realtimeRecording || this._realtimeStarting) return

    this.setData({ realtimeError: '' })
    this._realtimeStarting = true
    this._stopAudio('已停止播放')

    try {
      await this._connectRealtimeSocket()
      await this._sendRealtimeParams()
      await this._initRealtimePcmPlayer()
      this._startRealtimeRecorder()
    } catch (err) {
      this._realtimeStarting = false
      this._destroyRealtimePcmPlayer()
      this._closeRealtimeSocket({ silent: true })
      console.error('[realtime-ws] connect failed', err)
      if (!this._unloaded) {
        this.setData({
          realtimeError: (err && err.errMsg) ? err.errMsg : '实时连接失败',
          realtimeStatusText: '实时连接失败，请检查网络或服务端'
        })
      }
    }
  },

  stopRealtimeMic() {
    if (this._recorderManager && (this.data.realtimeRecording || this._realtimeStarting)) {
      try {
        this._recorderManager.stop()
      } catch (e) {
        console.warn('[realtime-recorder] stop failed', e)
      }
    }
    this._realtimeStarting = false
    this._destroyRealtimePcmPlayer()
    this._closeRealtimeSocket()
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
    if (this._audioPlayer) {
      this._audioPlayer.stop()
    }

    if (this._unloaded) return

    this.setData({
      isAudioPlaying: false,
      playingKind: '',
      statusText
    }, () => {
      this._syncVisualPlaybackState()
      this._refreshVisualFeedback()
    })
  },

  _switchPlayback(url, kind, onPlayText) {
    if (!url || !this._audioPlayer || !this._audioPlayer.isPlaying() || this.data.playingKind !== kind) {
      return false
    }

    this._audioPlayer.switchSrc(url).then((ok) => {
      if (!ok || this._unloaded) return
      this.setData({ statusText: onPlayText }, () => {
        this._refreshVisualFeedback()
        this._syncVisualPlaybackState()
      })
    }).catch((err) => {
      console.error(err)
    })
    return true
  },

  _playAudio(url, kind, onPlayText, onStopText, onErrorText, options = {}) {
    if (!url) {
      wx.showToast({
        title: '暂无可播放音频',
        icon: 'none'
      })
      return
    }

    if (!this._audioPlayer) {
      this._audioPlayer = createSeamlessAudioPlayer()
    }

    if (this.data.isAudioPlaying && this.data.playingKind === kind) {
      if (options.forceRestart) {
        if (this._audioPlayer) this._audioPlayer.stop({ silent: true })
      } else {
        this._stopAudio(onStopText || '已停止播放')
        return
      }
    } else {
      this._stopAudio('')
    }

    if (kind === 'processed' && options.visualizationData !== undefined) {
      this._applyVisualizationData(options.visualizationData)
    }

    this._audioPlayer.play(url, {
      onTimeUpdate: (currentTime) => {
        if (this._unloaded) return
        this.setData({ audioSeekSec: currentTime })
      },
      onPlay: () => {
        if (this._unloaded) return
        this.setData({
          isAudioPlaying: true,
          playingKind: kind,
          statusText: onPlayText,
          audioSeekSec: 0
        }, () => {
          this._refreshVisualFeedback()
          this._syncVisualPlaybackState()
        })
      },
      onStop: () => {
        if (this._unloaded) return
        this._syncVisualPlaybackState()
        this.setData({
          isAudioPlaying: false,
          playingKind: '',
          statusText: onStopText || '已停止播放'
        }, () => {
          this._refreshVisualFeedback()
        })
      },
      onError: (err) => {
        console.error(err)
        if (this._unloaded) return
        this._syncVisualPlaybackState()
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
      }
    }).catch((err) => {
      console.error(err)
    })
  },

  selectSource(e) {
    this._ensurePageState()
    const type = e.currentTarget.dataset.type

    if (this.data.sourceType === 'realtime' && type !== 'realtime') {
      if (this.data.realtimeRecording || this._realtimeStarting) {
        this.stopRealtimeMic()
      }
    }

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

    if (type === 'realtime') {
      this._stopAudio('已停止播放')
      this._cancelAutoRefresh()
      this._invalidateProcessedResult({ autoRefresh: false })
      this._applyRuntimePatch({
        sourceType: 'realtime',
        selectedScenario: ''
      })
      this.setData({
        sourceType: 'realtime',
        sourceHint: '',
        statusText: '已选择实时麦克风',
        taskStatus: 'idle',
        selectedScenario: '',
        realtimeError: '',
        realtimeStatusText: '未开始实时体验',
        realtimeSocketConnected: false,
        realtimeSentFrames: 0,
        realtimeReceivedFrames: 0,
        realtimeSentBytes: 0,
        realtimeReceivedBytes: 0,
        realtimeLastRttMs: null,
        realtimeAvgRttMs: null,
        realtimeLostFrames: 0,
        realtimePlaybackStarted: false,
        realtimePlaybackUnderruns: 0,
        realtimeBufferedMs: 0,
        realtimePlaybackPendingFrames: 0,
        realtimePlaybackDroppedFrames: 0,
        realtimePlaybackRecoveryCount: 0
      })
      return
    }

    if (type === 'record') {
      sourceHint = '录制功能后续接入'
      statusText = '录制功能后续接入'
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

  async _continueOriginalAfterSampleSwitch(cache, label) {
    try {
      const sampleSource = cache || await this._ensureSampleSource()
      const playText = `正在循环播放${label}`
      if (!this._switchPlayback(sampleSource.url, 'original', playText)) {
        this._playAudio(
          sampleSource.url,
          'original',
          playText,
          '已停止原声示例播放',
          '原声示例播放失败',
          { forceRestart: true }
        )
      }
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
  },

  selectSample(e) {
    this._ensurePageState()
    const code = e.currentTarget.dataset.code
    if (code === this.data.selectedSample) return

    const cache = this._sampleSourceCache[code]
    const label = this._getSampleLabel(code)
    const wasPlayingOriginal = this.data.isAudioPlaying && this.data.playingKind === 'original'
    const wasPlayingProcessed = this.data.isAudioPlaying && this.data.playingKind === 'processed'

    if (!wasPlayingOriginal && !wasPlayingProcessed) {
      this._cancelAutoRefresh()
    }

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
      statusText: wasPlayingOriginal
        ? `正在切换${label}...`
        : wasPlayingProcessed
          ? '正在切换示例并更新模拟声...'
          : `已选择${label}`,
      selectedScenario: ''
    }, () => {
      this._syncRuntimeParamsFromData()
      this._invalidateProcessedResult({
        keepStatus: true,
        autoRefresh: wasPlayingProcessed
      })
      if (wasPlayingOriginal) {
        this._continueOriginalAfterSampleSwitch(cache, label)
      }
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
      this._maybeSendRealtimeParams()
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
      this._maybeSendRealtimeParams()
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
      this._maybeSendRealtimeParams()
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
      this._maybeSendRealtimeParams()
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
      this._maybeSendRealtimeParams()
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
      this._maybeSendRealtimeParams()
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
      wx.showToast({ title: '请先使用下方实时体验', icon: 'none' })
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
      wx.showToast({ title: '实时麦克风模式下请使用实时体验', icon: 'none' })
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
        const cached = this._getCachedProcessedResult(key)
        if (cached) {
          this._applyProcessedResult(key, cached, {
            autoPlay: true,
            replacePlaying,
            fromCache: true
          })
        } else {
          this._playAudio(
            this.data.processedAudioUrl,
            'processed',
            '正在循环播放人工耳蜗模拟声音',
            '已停止模拟声音播放',
            '模拟声音播放失败，请检查文件地址'
          )
        }
      }
      return
    }

    const cached = this._getCachedProcessedResult(key)
    if (cached) {
      if (requestSeq !== this._autoRefreshSeq) {
        return
      }
      console.log('[processed-cache] hit', key)
      this._applyProcessedResult(key, cached, {
        autoPlay,
        replacePlaying,
        fromCache: true
      })
      return
    }

    console.log('[processed-cache] miss', key)

    this.setData({
      isProcessing: true,
      taskStatus: 'processing',
      statusText: replacePlaying ? '正在按新参数更新模拟声...' : '正在生成模拟声音...',
      errorMessage: ''
    }, () => {
      this._refreshVisualFeedback()
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

      const cachedResult = {
        taskNo: result.taskNo,
        outputAssetId: result.outputAssetId,
        processedAudioUrl: result.processedAudioUrl,
        clarityScore,
        clarityGrade,
        clarityDesc,
        visualizationData: result.visualizationData || null
      }

      console.log('[processed-cache] save', key)
      this._setCachedProcessedResult(key, cachedResult)
      this._applyProcessedResult(key, cachedResult, {
        autoPlay,
        replacePlaying,
        fromCache: false
      })
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
