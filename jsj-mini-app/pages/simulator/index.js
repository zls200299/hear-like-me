const { listScenarios } = require('../../services/scenario.js')
const { listSamples, prepareSampleSource } = require('../../services/sample.js')
const { uploadAudio } = require('../../services/file.js')
const { createTask } = require('../../services/audioTask.js')
const { createSeamlessAudioPlayer } = require('../../utils/simulator/seamlessAudioPlayer.js')
const { createRealtimePcmPlayer } = require('../../utils/simulator/realtimePcmPlayer.js')
const { createFilePcmSource } = require('../../utils/simulator/filePcmSource.js')
const {
  computePcm16Rms,
  smoothMicLevel,
  drawMicLevelBars,
  DRAW_INTERVAL_MS
} = require('../../utils/simulator/micLevelCanvas.js')
const { buildFeedbackGaugeDataUri } = require('../../utils/simulator/feedbackGauge.js')
const config = require('../../config.js')

const REALTIME_PARAM_THROTTLE_MS = 120
const REALTIME_PARAM_SUPERSEDED = 'PARAM_SUPERSEDED'
const DEBUG_UI_INTERVAL_MS = 300
const FILE_STREAM_FRAME_MS = 60
const FILE_STREAM_MAX_PENDING_FRAMES = 5
const FILE_STREAM_BOOTSTRAP_FRAMES = 3
const PROCESSED_UI_ERROR_HOLD_MS = 5000

const PROCESSED_UI_TEXT = {
  idle: '',
  starting: '正在准备模拟声…',
  playing: '正在播放模拟声',
  switching: '正在切换音频…',
  stopping: '正在停止…',
  error: '模拟声启动失败，请重试'
}

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
    uploadUiState: 'idle',
    uploadUiText: '',
    uploadErrorText: '',
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
      { code: 'minimal', name: '仅 4 通道' }
    ],
    frequencyOptions: [
      { value: '80-8000', label: '80-8000 Hz' },
      { value: '150-7000', label: '150-7000 Hz' },
      { value: '300-3400', label: '300-3400 Hz' }
    ],
    channelQuickValues: [1, 2, 4, 8, 16, 22],
    channelSliderPct: 33,
    envCutSliderPct: 29,
    spreadSliderPct: 15,
    noiseSliderPct: 0,
    isProcessing: false,
    taskNo: '',
    outputAssetId: null,
    processedAudioUrl: '',
    processedKey: '',
    clarityScore: null,
    clarityGrade: '',
    errorMessage: '',
    clarityDesc: '',
    feedbackGaugeSrc: '',
    feedbackLiveActive: false,
    feedbackCarrierLabel: '噪声载体',
    exportAudioDisabled: true,
    isAudioPlaying: false,
    playingKind: '',
    listenHint: '播放模拟声时会按当前参数生成。',
    realtimeRecording: false,
    realtimeConnecting: false,
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
    showRealtimeDebug: false,
    realtimePlaybackStarted: false,
    realtimePlaybackUnderruns: 0,
    realtimeBufferedMs: 0,
    realtimePlaybackPendingFrames: 0,
    realtimePlaybackDroppedFrames: 0,
    realtimePlaybackRecoveryCount: 0,
    fileStreamingActive: false,
    sourceDetailIcon: 'music',
    sourceDetailTitle: '示例声音',
    sourceDetailHint: '可直接试听原声与模拟声',
    sourceDetailShowAction: false,
    sourceDetailActionLabel: '',
    sourceDetailActionTone: 'neutral',
    sourceDetailActionDisabled: false,
    realtimeMicStatusLabel: '等待开始',
    processedUiState: 'idle',
    processedUiText: '',
    processedUiBusy: false,
    processedUiSpinning: false,
    processedBtnLabel: '播放模拟声'
  },

  _scenarioPresets: null,
  _sampleLabels: null,
  _sampleSourceCache: {},
  _samplePreparePromises: {},
  _unloaded: false,
  _autoRefreshTimer: null,
  _autoRefreshSeq: 0,
  _shouldAutoPlayProcessed: false,
  _prefetchTimer: null,
  _prefetchSeq: 0,
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
  _realtimeParamVersion: 0,
  _realtimeAppliedParamVersion: 0,
  _realtimeParamThrottleTimer: null,
  _realtimeParamLastSentAt: 0,
  _realtimeParamPending: false,
  _streamingMode: null,
  _fileStreamActive: false,
  _fileStreamStarting: false,
  _filePcmSource: null,
  _fileStreamTimer: null,
  _fileStreamBootstrapRemaining: 0,
  _fileStreamGeneration: 0,
  _micLevelCanvas: null,
  _micLevelCtx: null,
  _micLevelCssWidth: 0,
  _micLevelCssHeight: 0,
  _micLevelSmoothed: 0,
  _micLevelActive: false,
  _micLevelDrawTimer: null,
  _micLevelDecayTimer: null,
  _realtimeConnectTimeoutTimer: null,
  _realtimeRecorderStartTimeoutTimer: null,
  _realtimePermissionRequesting: false,
  _realtimePermissionRequestId: 0,
  _realtimeSocketClosingByUser: false,
  _realtimeSessionStopping: false,
  _realtimeUserStop: false,
  _realtimeDebugUiTimer: null,
  _realtimeDebugStats: null,
  _processedUiGeneration: 0,
  _processedUiErrorTimer: null,
  _uploadSourceSnapshot: null,

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
      this._schedulePrefetchProcessed(400)
      this._syncSourceDetailUI()
      this.setData(this._midSliderPercents())
    })
    this._refreshVisualFeedback()
    this._syncSourceDetailUI()
    this.setData(this._midSliderPercents())
  },

  onShow() {
    if (this._unloaded) return
    // 不自动恢复 realtime / file stream / 播放
    this._syncVisualPlaybackState()
    this._syncSourceDetailUI()
  },

  onHide() {
    if (this._unloaded) return

    if (this.data.realtimeRecording || this._realtimeStarting || this.data.realtimeConnecting) {
      this.stopRealtimeMic()
    }

    if (this._fileStreamActive || this._fileStreamStarting) {
      this._stopFileStreamingProcessed({ silent: true })
    }

    if (this.data.isAudioPlaying) {
      this._stopAudio('已暂停页面播放')
    }
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
    this._clearProcessedUiErrorTimer()
    this._stopRealtimeDebugUiTimer()
    this._clearRealtimeConnectTimeout()
    this._clearRealtimeRecorderStartTimeout()
    this._destroyMicLevelCanvas()
    this._stopFileStreamingProcessed({ silent: true })
    this._clearRealtimeParamThrottle()
    this._cancelPrefetch()
    this._cancelAutoRefresh()
    this._stopAudio('已退出页面')
    this.stopRealtimeMic()
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

  _syncSourceDetailUI() {
    if (this._unloaded) return

    const {
      sourceType,
      uploadedFileName,
      realtimeRecording,
      realtimeConnecting
    } = this.data
    const realtimeStarting = !!this._realtimeStarting
    const permissionRequesting = !!this._realtimePermissionRequesting

    let icon = 'music'
    let title = '示例声音'
    let hint = '可直接试听原声与模拟声'
    let showAction = false
    let actionLabel = ''
    let actionTone = 'neutral'
    let actionDisabled = false
    let micStatusLabel = '等待开始'

    if (sourceType === 'sample') {
      icon = 'music'
      title = '示例声音'
      hint = '可直接试听原声与模拟声'
    } else if (sourceType === 'upload') {
      icon = 'upload'
      title = '上传音频'
      showAction = true
      const uploadUiState = this.data.uploadUiState || 'idle'
      if (uploadUiState === 'uploading') {
        hint = '请稍候，上传完成后即可试听。'
        actionLabel = '上传中…'
        actionTone = 'connecting'
        actionDisabled = true
      } else if (uploadUiState === 'success') {
        hint = this.data.uploadUiText || '上传成功，可以试听原声或模拟声'
        actionLabel = '重新选择'
        actionTone = 'neutral'
      } else if (uploadUiState === 'error') {
        hint = '可重新选择文件后重试'
        actionLabel = '重新选择'
        actionTone = 'neutral'
      } else {
        hint = '支持 MP3 / WAV 等常见格式'
        actionLabel = '选择文件'
        actionTone = 'neutral'
      }
    } else if (sourceType === 'realtime') {
      icon = 'microphone-sm'
      title = '实时麦克风'
      hint = '建议佩戴耳机，避免回声'
      showAction = true
      if (realtimeRecording) {
        actionLabel = '停止采集'
        actionTone = 'stop'
        micStatusLabel = '正在采集麦克风音频'
      } else if (permissionRequesting) {
        actionLabel = '取消'
        actionTone = 'connecting'
        actionDisabled = false
        micStatusLabel = '等待麦克风授权'
      } else if (realtimeConnecting || realtimeStarting) {
        actionLabel = '取消连接'
        actionTone = 'connecting'
        actionDisabled = false
        micStatusLabel = '连接中…'
      } else {
        actionLabel = '开始采集'
        actionTone = 'start'
        micStatusLabel = '等待开始'
      }
    }

    this.setData({
      sourceDetailIcon: icon,
      sourceDetailTitle: title,
      sourceDetailHint: hint,
      sourceDetailShowAction: showAction,
      sourceDetailActionLabel: actionLabel,
      sourceDetailActionTone: actionTone,
      sourceDetailActionDisabled: actionDisabled,
      realtimeMicStatusLabel: micStatusLabel
    })
  },

  _ensureMicLevelCanvas(callback) {
    if (this._unloaded || this.data.sourceType !== 'realtime') return

    if (this._micLevelCtx && this._micLevelCanvas && this._micLevelCssWidth > 0) {
      if (typeof callback === 'function') callback()
      return
    }

    this.createSelectorQuery()
      .select('#micLevelCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (this._unloaded || this.data.sourceType !== 'realtime') return
        const item = res && res[0]
        if (!item || !item.node || !item.width || !item.height) return

        const canvas = item.node
        const ctx = canvas.getContext('2d')
        const dpr = wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : (wx.getSystemInfoSync().pixelRatio || 2)
        canvas.width = Math.floor(item.width * dpr)
        canvas.height = Math.floor(item.height * dpr)
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

        this._micLevelCanvas = canvas
        this._micLevelCtx = ctx
        this._micLevelCssWidth = item.width
        this._micLevelCssHeight = item.height
        if (typeof callback === 'function') callback()
      })
  },

  _drawMicLevelFrame(isLiveOverride) {
    if (!this._micLevelCtx || this._micLevelCssWidth <= 0 || this._micLevelCssHeight <= 0) return

    const isLive = typeof isLiveOverride === 'boolean'
      ? isLiveOverride
      : !!(this._micLevelActive && (this.data.realtimeRecording || this._micLevelSmoothed > 0.02))

    drawMicLevelBars(
      this._micLevelCtx,
      this._micLevelCssWidth,
      this._micLevelCssHeight,
      this._micLevelSmoothed || 0,
      { isLive }
    )
  },

  _stopMicLevelDrawLoop() {
    if (!this._micLevelDrawTimer) return
    clearInterval(this._micLevelDrawTimer)
    this._micLevelDrawTimer = null
  },

  _stopMicLevelDecay() {
    if (!this._micLevelDecayTimer) return
    clearInterval(this._micLevelDecayTimer)
    this._micLevelDecayTimer = null
  },

  _startMicLevelDrawLoop() {
    this._stopMicLevelDrawLoop()
    this._micLevelDrawTimer = setInterval(() => {
      if (this._unloaded || this.data.sourceType !== 'realtime') {
        this._stopMicLevelDrawLoop()
        return
      }
      this._drawMicLevelFrame()
    }, DRAW_INTERVAL_MS)
    this._drawMicLevelFrame(true)
  },

  _startMicLevelDecay() {
    this._stopMicLevelDecay()
    this._micLevelDecayTimer = setInterval(() => {
      if (this._unloaded || this.data.sourceType !== 'realtime') {
        this._stopMicLevelDecay()
        return
      }

      this._micLevelSmoothed = smoothMicLevel(this._micLevelSmoothed, 0)
      this._drawMicLevelFrame(this._micLevelSmoothed > 0.02)

      if (this._micLevelSmoothed <= 0.02) {
        this._micLevelSmoothed = 0
        this._stopMicLevelDecay()
        this._drawMicLevelFrame(false)
      }
    }, DRAW_INTERVAL_MS)
  },

  _startMicLevelVisualizer() {
    this._micLevelActive = true
    this._micLevelSmoothed = 0
    this._stopMicLevelDecay()
    this._ensureMicLevelCanvas(() => {
      this._startMicLevelDrawLoop()
    })
  },

  _stopMicLevelVisualizer(options) {
    const opts = options || {}
    this._micLevelActive = false
    this._stopMicLevelDrawLoop()

    if (opts.decay) {
      this._startMicLevelDecay()
      return
    }

    this._stopMicLevelDecay()
    this._micLevelSmoothed = 0
    this._ensureMicLevelCanvas(() => {
      this._drawMicLevelFrame(false)
    })
  },

  _destroyMicLevelCanvas() {
    this._stopMicLevelDrawLoop()
    this._stopMicLevelDecay()
    this._micLevelActive = false
    this._micLevelSmoothed = 0
    this._micLevelCanvas = null
    this._micLevelCtx = null
    this._micLevelCssWidth = 0
    this._micLevelCssHeight = 0
  },

  _feedMicLevelFromFrame(frameBuffer) {
    if (!this._micLevelActive || this._unloaded || this.data.sourceType !== 'realtime') return

    try {
      const current = computePcm16Rms(frameBuffer)
      this._micLevelSmoothed = smoothMicLevel(this._micLevelSmoothed, current)
    } catch (err) {
      console.warn('[mic-level] rms failed', err)
    }
  },

  _mountMicLevelCanvasIdle() {
    if (this._unloaded || this.data.sourceType !== 'realtime') return
    wx.nextTick(() => {
      this._ensureMicLevelCanvas(() => {
        this._drawMicLevelFrame(false)
      })
    })
  },

  onSourceDetailAction() {
    const { sourceType, sourceDetailActionDisabled } = this.data
    if (sourceDetailActionDisabled) return

    if (sourceType === 'upload') {
      this._chooseAndUpload()
      return
    }
    if (sourceType === 'realtime') {
      if (this._realtimePermissionRequesting) {
        this._realtimePermissionRequestId += 1
        this._realtimePermissionRequesting = false
        this._syncSourceDetailUI()
        return
      }
      if (this.data.realtimeRecording || this._realtimeStarting || this.data.realtimeConnecting) {
        this.stopRealtimeMic()
        return
      }
      this.setData({ realtimeError: '' })
      this.startRealtimeMic()
    }
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

  _sliderPct(value, min, max) {
    const v = Number(value)
    const lo = Number(min)
    const hi = Number(max)
    if (!Number.isFinite(v) || hi <= lo) return 0
    return Math.round(((v - lo) / (hi - lo)) * 100)
  },

  _midSliderPercents(params = {}) {
    const nChannels = params.nChannels != null ? params.nChannels : this.data.nChannels
    const envCut = params.envCut != null ? params.envCut : this.data.envCut
    const spread = params.spread != null ? params.spread : this.data.spread
    const noiseLevel = params.noiseLevel != null ? params.noiseLevel : this.data.noiseLevel
    return {
      channelSliderPct: this._sliderPct(nChannels, 1, 22),
      envCutSliderPct: this._sliderPct(envCut, 20, 500),
      spreadSliderPct: this._sliderPct(spread, 0, 100),
      noiseSliderPct: this._sliderPct(noiseLevel, 0, 100)
    }
  },

  _getProcessedDisplayName() {
    if (this.data.sourceType === 'sample') {
      return this._getSampleLabel(this.data.selectedSample)
    }
    if (this.data.sourceType === 'upload') {
      return this.data.uploadedFileName || '上传音频'
    }
    return ''
  },

  _resolveProcessedUiText(state, displayName) {
    if (state === 'playing') {
      return displayName ? `正在播放：${displayName}` : PROCESSED_UI_TEXT.playing
    }
    if (state === 'switching') {
      return displayName ? `正在切换到「${displayName}」…` : PROCESSED_UI_TEXT.switching
    }
    return PROCESSED_UI_TEXT[state] || ''
  },

  _buildProcessedUiView(state, displayName) {
    const busy = state === 'starting' || state === 'switching' || state === 'stopping'
    const spinning = state === 'starting' || state === 'switching'
    let processedBtnLabel = '播放模拟声'
    if (this.data.isProcessing) {
      processedBtnLabel = this.data.isAudioPlaying && this.data.playingKind === 'processed'
        ? '更新中...'
        : '正在生成...'
    } else if (state === 'starting') {
      processedBtnLabel = '正在启动…'
    } else if (state === 'switching') {
      processedBtnLabel = '正在切换…'
    } else if (state === 'stopping') {
      processedBtnLabel = '正在停止…'
    } else if (state === 'playing') {
      processedBtnLabel = '停止模拟声'
    }

    return {
      processedUiState: state,
      processedUiText: this._resolveProcessedUiText(state, displayName),
      processedUiBusy: busy,
      processedUiSpinning: spinning,
      processedBtnLabel
    }
  },

  _clearProcessedUiErrorTimer() {
    if (!this._processedUiErrorTimer) return
    clearTimeout(this._processedUiErrorTimer)
    this._processedUiErrorTimer = null
  },

  _scheduleProcessedUiErrorClear() {
    this._clearProcessedUiErrorTimer()
    this._processedUiErrorTimer = setTimeout(() => {
      this._processedUiErrorTimer = null
      if (this._unloaded || this.data.processedUiState !== 'error') return
      this._setProcessedUiState('idle', { force: true })
    }, PROCESSED_UI_ERROR_HOLD_MS)
  },

  _isProcessedUiBusy() {
    const state = this.data.processedUiState
    return state === 'starting' || state === 'switching' || state === 'stopping'
  },

  _isProcessedUiGeneration(generation) {
    return Number(generation) === Number(this._processedUiGeneration)
  },

  _setProcessedUiState(state, options = {}) {
    const { generation, displayName, force = false } = options
    if (!force && generation != null && !this._isProcessedUiGeneration(generation)) {
      return
    }

    if (state !== 'error') {
      this._clearProcessedUiErrorTimer()
    }

    const name = displayName != null ? displayName : this._getProcessedDisplayName()
    const patch = this._buildProcessedUiView(state, name)

    if (state === 'playing') {
      patch.isAudioPlaying = true
      patch.playingKind = 'processed'
    } else if (state === 'idle' || state === 'error') {
      patch.isAudioPlaying = false
      patch.playingKind = ''
    }

    if (state === 'idle' || state === 'error') {
      this._processedUiGeneration = 0
    } else if (generation != null) {
      this._processedUiGeneration = generation
    }

    if (state === 'error') {
      this._scheduleProcessedUiErrorClear()
    }

    if (this._unloaded) return
    this.setData(patch, () => {
      this._refreshVisualFeedback()
    })
  },

  _handleRealtimePcmPlayerState(state, generation) {
    if (this._unloaded || !this._isProcessedUiGeneration(generation)) return

    const patch = {
      realtimePlaybackStarted: !!state.started,
      realtimePlaybackUnderruns: state.underruns || 0,
      realtimeBufferedMs: state.bufferedMs || 0,
      realtimePlaybackPendingFrames: state.pendingFrames || 0,
      realtimePlaybackDroppedFrames: state.droppedFrames || 0,
      realtimePlaybackRecoveryCount: state.recoveryCount || 0
    }

    if (state.started) {
      const uiState = this.data.processedUiState
      if (uiState === 'starting' || uiState === 'switching') {
        Object.assign(patch, this._buildProcessedUiView('playing', this._getProcessedDisplayName()))
        patch.isAudioPlaying = true
        patch.playingKind = 'processed'
      }
    }

    this.setData(patch)
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

  _clearRealtimeVisualLevels() {
    const panel = this._getVisualPanel()
    if (panel && panel.clearRealtimeLevels) {
      panel.clearRealtimeLevels()
    }
  },

  _refreshVisualFeedback() {
    if (this._unloaded) return

    const { score, grade, desc } = this._computeLocalClarity()
    const listenHint = this._resolveListenHint()
    const params = this._getRuntimeParams()
    const patch = {
      clarityScore: score,
      clarityGrade: grade,
      clarityDesc: desc,
      clarityLevelClass: this._getClarityLevelClass(score),
      listenHint,
      feedbackGaugeSrc: buildFeedbackGaugeDataUri(score),
      feedbackLiveActive: !!(
        this.data.isProcessing
        || this.data.realtimeRecording
        || this.data.fileStreamingActive
        || (this.data.isAudioPlaying && (this.data.playingKind === 'processed' || this.data.playingKind === 'original'))
      ),
      feedbackCarrierLabel: params.carrier === 'sine' ? '正弦载体' : '噪声载体',
      exportAudioDisabled: !this._resolveExportAudioUrl()
    }
    if (this.data.processedUiState === 'idle' || this.data.processedUiState === 'error') {
      if (this.data.isProcessing) {
        patch.processedBtnLabel = this.data.isAudioPlaying && this.data.playingKind === 'processed'
          ? '更新中...'
          : '正在生成...'
        patch.processedUiBusy = true
        patch.processedUiSpinning = false
      } else if (this.data.isAudioPlaying && this.data.playingKind === 'processed') {
        patch.processedBtnLabel = '停止模拟声'
        patch.processedUiBusy = false
      } else if (!listenHint) {
        patch.processedBtnLabel = '播放模拟声'
        patch.processedUiBusy = false
      }
    }
    this.setData(patch)

    const panel = this._getVisualPanel()
    if (panel) panel.refreshViews()
  },

  _resolveListenHint() {
    if (this.data.processedUiText) {
      return ''
    }
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

  _resolveExportAudioUrl() {
    if (this.data.processedAudioUrl) return this.data.processedAudioUrl
    if (this.data.originalAudioUrl) return this.data.originalAudioUrl
    return ''
  },

  onExportAudio() {
    const url = this._resolveExportAudioUrl()
    if (!url) {
      wx.showToast({
        title: '暂无可导出音频',
        icon: 'none'
      })
      return
    }

    wx.showLoading({ title: '准备导出...', mask: true })
    wx.downloadFile({
      url,
      success: (res) => {
        if (!res || res.statusCode !== 200 || !res.tempFilePath) {
          wx.hideLoading()
          wx.showToast({ title: '导出失败', icon: 'none' })
          return
        }
        wx.saveFile({
          tempFilePath: res.tempFilePath,
          success: () => {
            wx.hideLoading()
            wx.showToast({ title: '已保存到本地', icon: 'success' })
          },
          fail: () => {
            wx.hideLoading()
            wx.showToast({ title: '保存失败', icon: 'none' })
          }
        })
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
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
    const wasFileStreamingProcessed = this._fileStreamActive
      && this.data.isAudioPlaying
      && this.data.playingKind === 'processed'
    const wasPlayingProcessed = this.data.isAudioPlaying && this.data.playingKind === 'processed'
    const wasPlayingProcessedOffline = wasPlayingProcessed && !wasFileStreamingProcessed
    const cacheApplied = wasPlayingProcessedOffline && options.autoRefresh !== false
      ? this._tryApplyCachedCurrentResult()
      : false
    const shouldKeepVisualization = wasPlayingProcessedOffline && options.autoRefresh !== false

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

    if (wasPlayingProcessedOffline && options.autoRefresh !== false) {
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
      if (
        options.schedulePrefetch !== false
        && !wasPlayingProcessed
        && !wasFileStreamingProcessed
        && this.data.sourceType !== 'realtime'
      ) {
        this._invalidateInFlightPrefetch()
        this._schedulePrefetchProcessed()
      }
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

  _resolveUploadError(err) {
    const rawParts = []
    if (typeof err === 'string') {
      rawParts.push(err)
    } else if (err) {
      rawParts.push(err.errMsg, err.message, err.errorMessage)
      if (err.response) {
        rawParts.push(err.response.msg, err.response.message, err.response.code)
      }
      if (err.data) {
        rawParts.push(err.data.msg, err.data.message, err.data.code)
      }
      if (err.statusCode != null) rawParts.push(String(err.statusCode))
      if (err.status != null) rawParts.push(String(err.status))
    }

    const raw = rawParts.filter(Boolean).join(' ').toLowerCase()

    if (/timeout|超时/.test(raw)) {
      return '上传超时，请重试'
    }
    if (/network|request:fail|网络/.test(raw)) {
      return '网络连接异常，请检查网络后重试'
    }
    if (/size|too large|文件过大|过大/.test(raw)) {
      return '音频文件过大，请选择较短的音频文件'
    }
    if (/format|decode|unsupported|格式/.test(raw)) {
      return '暂不支持该音频格式，请尝试 MP3 或 WAV'
    }
    if (/\b500\b|server|服务/.test(raw)) {
      return '上传服务暂时不可用，请稍后重试'
    }

    const fallback = this._formatErrorMessage(err)
    if (!fallback || fallback === '未知错误') {
      return '上传失败，请重试'
    }
    return fallback
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
    if (this.data.sourceType !== 'sample') return

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
      this._syncSourceDetailUI()
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
    const {
      autoPlay = false,
      replacePlaying = false,
      fromCache = false,
      background = false
    } = options

    if (background) {
      this._setCachedProcessedResult(key, { ...result })
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
        errorMessage: ''
      }, () => {
        this._refreshVisualFeedback()
      })
      this._preloadProcessedAudio(result.processedAudioUrl)
      return
    }

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
      this._clearRealtimeRecorderStartTimeout()
      this._resetRealtimeDebugStats()
      if (this._unloaded) return
      this.setData({
        realtimeConnecting: false,
        realtimeRecording: true,
        realtimeRecorderReady: true,
        realtimeFrameCount: 0,
        realtimeLastFrameBytes: 0,
        realtimeTotalBytes: 0,
        realtimeError: '',
        realtimeStatusText: '正在采集麦克风音频'
      }, () => {
        this._syncSourceDetailUI()
        this._startMicLevelVisualizer()
        this._startRealtimeDebugUiTimer()
      })
    })

    recorder.onFrameRecorded((res) => {
      const frameBuffer = res && res.frameBuffer
      if (!(frameBuffer instanceof ArrayBuffer)) return

      const bytes = frameBuffer.byteLength || 0
      const stats = this._ensureRealtimeDebugStats()
      stats.frameCount += 1
      stats.totalBytes += bytes
      stats.lastFrameBytes = bytes

      console.log(
        '[realtime-recorder]',
        'frame=', stats.frameCount,
        'bytes=', bytes,
        'last=', !!(res && res.isLastFrame)
      )

      this._feedMicLevelFromFrame(frameBuffer)
      this._sendRealtimeFrame(frameBuffer, bytes)
    })

    recorder.onStop(() => {
      this._realtimeStarting = false
      this._clearRealtimeRecorderStartTimeout()
      this._stopRealtimeDebugUiTimer()

      const userStop = this._realtimeUserStop
      this._realtimeUserStop = false

      if (userStop || this._realtimeSessionStopping) {
        this._clearRealtimeVisualLevels()
        this._stopMicLevelVisualizer({ decay: true })
        if (this._unloaded) return
        this.setData({
          realtimeConnecting: false,
          realtimeRecording: false,
          realtimeStatusText: '实时麦克风已停止'
        }, () => {
          this._syncSourceDetailUI()
        })
        return
      }

      // Recorder 意外停止：连带清理 socket / player，避免只录不发
      this._stopRealtimeSession({
        byUser: false,
        stopRecorder: false,
        closeSocket: true,
        destroyPlayer: true,
        statusText: '实时麦克风已停止',
        showToast: false
      })
    })

    recorder.onError((err) => {
      this._realtimeStarting = false
      this._realtimeUserStop = false
      this._clearRealtimeRecorderStartTimeout()
      console.error('[realtime-recorder] error', err)
      this._stopRealtimeSession({
        byUser: false,
        stopRecorder: false,
        closeSocket: true,
        destroyPlayer: true,
        statusText: '麦克风录音失败',
        error: (err && err.errMsg) ? err.errMsg : '录音失败',
        showToast: false
      })
    })

    recorder.onInterruptionBegin(() => {
      if (this._unloaded) return
      this._stopRealtimeSession({
        byUser: false,
        stopRecorder: true,
        closeSocket: true,
        destroyPlayer: true,
        statusText: '麦克风被系统中断，请重新开始实时体验',
        showToast: true
      })
    })

    recorder.onInterruptionEnd(() => {
      if (this._unloaded) return
      // 不自动重连，保持 idle
      this.setData({
        realtimeStatusText: '系统音频中断已结束，请重新开始实时体验'
      }, () => {
        this._syncSourceDetailUI()
      })
    })

    this._realtimeRecorderBound = true
  },

  _ensureRecordPermission() {
    return new Promise((resolve) => {
      let settled = false
      const finish = (granted) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        resolve(!!granted)
      }

      const timer = setTimeout(() => {
        finish(false)
        if (!this._unloaded) {
          wx.showToast({
            title: '麦克风授权超时，请重试',
            icon: 'none'
          })
        }
      }, 12000)

      const openPermissionSettings = () => {
        wx.openSetting({
          success: (openRes) => {
            finish(openRes.authSetting && openRes.authSetting['scope.record'] === true)
          },
          fail: () => finish(false)
        })
      }

      const promptOpenSettings = () => {
        wx.showModal({
          title: '需要麦克风权限',
          content: '请在设置中开启麦克风权限后重试',
          confirmText: '去设置',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              openPermissionSettings()
            } else {
              finish(false)
            }
          },
          fail: () => finish(false)
        })
      }

      wx.getSetting({
        success: (settingRes) => {
          const auth = (settingRes && settingRes.authSetting) || {}
          if (auth['scope.record'] === true) {
            finish(true)
            return
          }
          if (auth['scope.record'] === false) {
            promptOpenSettings()
            return
          }
          wx.authorize({
            scope: 'scope.record',
            success: () => finish(true),
            fail: () => promptOpenSettings()
          })
        },
        fail: () => finish(false)
      })
    })
  },

  _startRealtimeRecorder() {
    if (!this._recorderManager) {
      this._initRealtimeRecorder()
    }
    this._clearRealtimeRecorderStartTimeout()
    this._realtimeRecorderStartTimeoutTimer = setTimeout(() => {
      if (this._unloaded || this.data.realtimeRecording) return
      if (!this._realtimeStarting && !this.data.realtimeConnecting) return
      this._failRealtimeConnectAttempt('麦克风启动超时，请检查权限后重试')
    }, 8000)

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
      this._realtimeParamVersion = 0
      this._realtimeAppliedParamVersion = 0

      const socketTask = wx.connectSocket({ url })
      this._realtimeSocket = socketTask
      this._realtimeConnectResolve = resolve
      this._realtimeConnectReject = reject

      const isCurrentSocket = () => this._realtimeSocket === socketTask

      const failConnect = (err) => {
        if (!isCurrentSocket()) return
        if (this._realtimeConnectSettled) return
        this._realtimeConnectSettled = true
        this._clearRealtimeReadyTimeout()
        this._clearRealtimeConnectTimeout()
        this._realtimeSocketReady = false
        this._realtimeConnectResolve = null
        this._realtimeConnectReject = null
        reject(err || new Error('WebSocket 连接失败'))
      }

      this._clearRealtimeConnectTimeout()
      this._realtimeConnectTimeoutTimer = setTimeout(() => {
        if (!isCurrentSocket() || this._realtimeConnectSettled) return
        failConnect(new Error('WebSocket 连接超时'))
      }, 15000)

      socketTask.onMessage((res) => {
        if (!isCurrentSocket()) return
        this._handleRealtimeSocketMessage(res)
      })

      socketTask.onOpen(() => {
        if (!isCurrentSocket()) return
        if (this._unloaded) return
        this._clearRealtimeConnectTimeout()
        this._clearRealtimeReadyTimeout()
        this._realtimeReadyTimeoutTimer = setTimeout(() => {
          if (!isCurrentSocket() || this._realtimeConnectSettled) return
          failConnect(new Error('声码器初始化超时'))
        }, 20000)
        this.setData({
          realtimeStatusText: '实时连接已建立，正在初始化声码器...'
        })
      })

      socketTask.onClose(() => {
        if (!isCurrentSocket()) return
        this._realtimeSocketReady = false

        const closingByUser = this._realtimeSocketClosingByUser
        this._realtimeSocketClosingByUser = false

        const wasMicSession = this._streamingMode === 'mic'
          && (
            this.data.realtimeRecording
            || this._realtimeStarting
            || this.data.realtimeConnecting
            || !!this._realtimePcmPlayer
          )

        if (this._fileStreamActive || this._fileStreamStarting) {
          this._stopFileStreamingProcessed({ silent: true })
        }
        if (!this._realtimeConnectSettled) {
          failConnect(new Error('WebSocket closed before READY'))
        }

        if (this._realtimeSocket === socketTask) {
          this._realtimeSocket = null
        }

        // READY 之后异常断开：必须停 mic / player，避免继续采集却无处可发
        if (!closingByUser && wasMicSession && !this._realtimeSessionStopping) {
          this._stopRealtimeSession({
            byUser: false,
            stopRecorder: true,
            closeSocket: false,
            destroyPlayer: true,
            statusText: '实时连接已断开，请重新开始',
            showToast: true
          })
          return
        }

        if (!this._unloaded) {
          this.setData({ realtimeSocketConnected: false })
        }
      })

      socketTask.onError((err) => {
        if (!isCurrentSocket()) return
        console.error('[realtime-ws] error', err)
        failConnect(err)
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
    const statusText = this._streamingMode === 'sample'
      ? '声码器已就绪，正在流式播放示例模拟声'
      : (this._streamingMode === 'upload'
        ? '声码器已就绪，正在流式播放上传模拟声'
        : '声码器已就绪，正在采集麦克风音频')
    this._resetRealtimeDebugStats()
    this.setData({
      realtimeSocketConnected: true,
      realtimeSentFrames: 0,
      realtimeReceivedFrames: 0,
      realtimeSentBytes: 0,
      realtimeReceivedBytes: 0,
      realtimeLastRttMs: null,
      realtimeAvgRttMs: null,
      realtimeLostFrames: 0,
      realtimeStatusText: statusText,
      realtimePlaybackStarted: false,
      realtimePlaybackUnderruns: 0,
      realtimeBufferedMs: 0,
      realtimePlaybackPendingFrames: 0,
      realtimePlaybackDroppedFrames: 0,
      realtimePlaybackRecoveryCount: 0
    })
    if (this._streamingMode === 'mic' && this.data.realtimeRecording) {
      this._startRealtimeDebugUiTimer()
    }
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

  _createParamSupersededError() {
    const err = new Error('参数更新被新的请求取代')
    err.code = REALTIME_PARAM_SUPERSEDED
    return err
  },

  _isRealtimeParamSupersededError(err) {
    return !!(err && err.code === REALTIME_PARAM_SUPERSEDED)
  },

  _handleRealtimeParamSendError(err) {
    if (this._isRealtimeParamSupersededError(err)) return
    console.warn('[realtime-ws] param update failed', err)
    if (!this._unloaded) {
      this.setData({
        realtimeError: (err && err.message) ? err.message : '实时参数更新失败'
      })
    }
  },

  _isStreamingVocoderActive() {
    if (this.data.sourceType === 'realtime'
      && (this.data.realtimeRecording || this._realtimeStarting)) {
      return true
    }
    if (this._fileStreamActive || this._fileStreamStarting) {
      return true
    }
    return false
  },

  _isFileStreamingMode() {
    return this._streamingMode === 'sample' || this._streamingMode === 'upload'
  },

  _isCurrentFileStream(generation, mode) {
    return !this._unloaded
      && generation === this._fileStreamGeneration
      && this._fileStreamStarting
      && this._streamingMode === mode
      && this.data.sourceType === mode
  },

  _isFileDecodeError(err) {
    if (!err) return false
    const msg = String(err.message || err.errMsg || err).toLowerCase()
    return msg.includes('解码')
      || msg.includes('decodeaudiodata')
      || msg.includes('decode')
  },

  _destroyFilePcmSource() {
    if (!this._filePcmSource) return
    this._filePcmSource.destroy()
    this._filePcmSource = null
  },

  _stopFileStreamPump() {
    if (!this._fileStreamTimer) return
    clearTimeout(this._fileStreamTimer)
    this._fileStreamTimer = null
  },

  _scheduleNextFileStreamFrame(delayMs = FILE_STREAM_FRAME_MS) {
    if (!this._fileStreamActive || this._unloaded) return
    this._stopFileStreamPump()
    this._fileStreamTimer = setTimeout(() => {
      this._fileStreamTimer = null
      this._pumpFileStreamFrame()
    }, delayMs)
  },

  _pumpFileStreamFrame() {
    if (!this._fileStreamActive || this._unloaded) return
    if (!this._realtimeSocketReady || !this._filePcmSource || !this._filePcmSource.isReady()) {
      this._scheduleNextFileStreamFrame()
      return
    }

    const pendingCount = this._realtimePendingFrames ? this._realtimePendingFrames.size : 0
    if (pendingCount >= FILE_STREAM_MAX_PENDING_FRAMES) {
      this._scheduleNextFileStreamFrame()
      return
    }

    try {
      const frame = this._filePcmSource.readFrame()
      this._sendRealtimeFrame(frame, frame.byteLength)
    } catch (err) {
      console.warn('[file-stream] readFrame failed', err)
      this._stopFileStreamingProcessed()
      return
    }

    if (this._fileStreamBootstrapRemaining > 0) {
      this._fileStreamBootstrapRemaining -= 1
      if (this._fileStreamBootstrapRemaining > 0) {
        this._fileStreamTimer = setTimeout(() => {
          this._fileStreamTimer = null
          this._pumpFileStreamFrame()
        }, 0)
        return
      }
    }

    this._scheduleNextFileStreamFrame()
  },

  _startFileStreamPump() {
    this._stopFileStreamPump()
    this._pumpFileStreamFrame()
  },

  _stopFileStreamingProcessed(options = {}) {
    const { silent = false, keepStatus = false, keepUiState = false } = options
    this._fileStreamGeneration += 1
    const wasActive = this._fileStreamActive || this._fileStreamStarting

    this._stopFileStreamPump()
    this._destroyFilePcmSource()
    this._clearRealtimeParamThrottle()
    this._clearRealtimeVisualLevels()
    this._destroyRealtimePcmPlayer()

    this._fileStreamActive = false
    this._fileStreamStarting = false
    this._fileStreamBootstrapRemaining = 0

    if (this._isFileStreamingMode()) {
      this._streamingMode = null
      this._closeRealtimeSocket({ silent: true })
    }

    if (!this._unloaded && wasActive) {
      const patch = {
        fileStreamingActive: false,
        realtimeSocketConnected: false
      }
      if (!keepStatus) {
        patch.isAudioPlaying = false
        patch.playingKind = ''
        patch.statusText = silent ? this.data.statusText : '已停止模拟声音播放'
      }
      if (!keepUiState) {
        const nextUiState = this.data.processedUiState === 'stopping' ? 'idle' : 'idle'
        Object.assign(patch, this._buildProcessedUiView(nextUiState, ''))
        if (nextUiState === 'idle') {
          patch.isAudioPlaying = false
          patch.playingKind = ''
          this._processedUiGeneration = 0
        }
      }
      this.setData(patch, () => {
        this._syncVisualPlaybackState()
        this._refreshVisualFeedback()
      })
    }
  },

  async _startFileStreamingProcessed(options = {}) {
    const {
      mode,
      url,
      prepareUrl,
      startingStatusText = '正在启动流式模拟...',
      playingStatusText = '正在循环播放人工耳蜗模拟声音',
      realtimeStatusText = '流式模拟播放中',
      preservePlayingState = false,
      displayName = ''
    } = options

    if (!mode || (mode !== 'sample' && mode !== 'upload')) {
      return
    }
    if (this._fileStreamStarting) return

    if (this._fileStreamActive
      && this.data.isAudioPlaying
      && this.data.playingKind === 'processed') {
      this._setProcessedUiState('stopping', { force: true })
      this._stopFileStreamingProcessed()
      return
    }

    if (!preservePlayingState) {
      this._stopAudio('')
    } else if (this._audioPlayer) {
      this._audioPlayer.stop({ silent: true })
    }
    this._clearOriginalVisualPcm()
    this._cancelAutoRefresh()
    const generation = ++this._fileStreamGeneration
    this._fileStreamStarting = true
    this._streamingMode = mode
    this._processedUiGeneration = generation

    if (!this._unloaded) {
      const uiState = preservePlayingState ? 'switching' : 'starting'
      const patch = {
        realtimeError: '',
        isProcessing: false,
        statusText: startingStatusText,
        ...this._buildProcessedUiView(uiState, displayName || this._getProcessedDisplayName())
      }
      patch.isAudioPlaying = false
      patch.playingKind = ''
      this.setData(patch)
    }

    try {
      const audioUrl = url || (prepareUrl ? await prepareUrl() : '')
      if (!this._isCurrentFileStream(generation, mode)) return
      if (!audioUrl) {
        throw new Error(mode === 'upload' ? '请先上传音频' : '示例声音不可用')
      }

      this._destroyFilePcmSource()
      const fileSource = createFilePcmSource({
        targetSampleRate: 44100,
        frameMs: FILE_STREAM_FRAME_MS,
        loop: true
      })
      this._filePcmSource = fileSource
      await fileSource.load(audioUrl)
      if (!this._isCurrentFileStream(generation, mode)) {
        if (this._filePcmSource === fileSource) {
          fileSource.destroy()
          this._filePcmSource = null
        }
        return
      }

      await this._connectRealtimeSocket()
      if (!this._isCurrentFileStream(generation, mode)) return

      await this._sendRealtimeParams()
      if (!this._isCurrentFileStream(generation, mode)) return

      const pcmPlayer = await this._initRealtimePcmPlayer(generation)
      if (!this._isCurrentFileStream(generation, mode)) {
        if (this._realtimePcmPlayer === pcmPlayer) {
          pcmPlayer.destroy()
          this._realtimePcmPlayer = null
        }
        return
      }

      this._fileStreamActive = true
      this._fileStreamStarting = false
      this._fileStreamBootstrapRemaining = FILE_STREAM_BOOTSTRAP_FRAMES

      this.setData({
        fileStreamingActive: true,
        realtimeSocketConnected: true,
        statusText: playingStatusText,
        realtimeStatusText
      }, () => {
        this._syncVisualPlaybackState()
        this._refreshVisualFeedback()
      })

      this._startFileStreamPump()
    } catch (err) {
      console.error('[file-stream] start failed', err)
      if (generation !== this._fileStreamGeneration
        || !this._isFileStreamingMode()
        || this._streamingMode !== mode) {
        return
      }
      this._stopFileStreamingProcessed({ silent: true, keepUiState: true })
      if (!this._unloaded) {
        const decodeError = this._isFileDecodeError(err)
        this._setProcessedUiState('error', { generation, force: true })
        this.setData({
          realtimeError: decodeError
            ? '该音频格式暂不支持实时模拟'
            : ((err && err.message) ? err.message : '流式模拟启动失败'),
          statusText: decodeError ? '音频格式不支持实时模拟' : '流式模拟启动失败'
        })
        wx.showToast({
          title: decodeError
            ? '该音频格式暂不支持实时模拟，请尝试 MP3 或 WAV'
            : '模拟声启动失败',
          icon: 'none'
        })
      }
    }
  },

  async _startSampleStreamingProcessed(options = {}) {
    await this._startFileStreamingProcessed({
      mode: 'sample',
      prepareUrl: async () => {
        const sampleSource = await this._ensureSampleSource()
        return sampleSource.url
      },
      startingStatusText: '正在启动示例流式模拟...',
      playingStatusText: '正在循环播放人工耳蜗模拟声音',
      realtimeStatusText: '示例流式模拟播放中',
      ...options
    })
  },

  async _startUploadStreamingProcessed() {
    if (!this.data.originalAudioUrl) {
      wx.showToast({
        title: '请先上传音频',
        icon: 'none'
      })
      return
    }

    await this._startFileStreamingProcessed({
      mode: 'upload',
      url: this.data.originalAudioUrl,
      startingStatusText: '正在启动上传流式模拟...',
      playingStatusText: '正在循环播放人工耳蜗模拟声音',
      realtimeStatusText: '上传流式模拟播放中'
    })
  },

  _clearRealtimeParamThrottle() {
    if (this._realtimeParamThrottleTimer) {
      clearTimeout(this._realtimeParamThrottleTimer)
      this._realtimeParamThrottleTimer = null
    }
    this._realtimeParamPending = false
  },

  _scheduleRealtimeParamUpdate() {
    if (!this._isStreamingVocoderActive()) return
    if (!this._realtimeSocketReady) return

    this._realtimeParamPending = true
    const now = Date.now()
    const elapsed = now - (this._realtimeParamLastSentAt || 0)

    if (elapsed >= REALTIME_PARAM_THROTTLE_MS) {
      this._flushRealtimeParamThrottlePending()
      return
    }

    if (this._realtimeParamThrottleTimer) return

    this._realtimeParamThrottleTimer = setTimeout(() => {
      this._realtimeParamThrottleTimer = null
      this._flushRealtimeParamThrottlePending()
    }, REALTIME_PARAM_THROTTLE_MS - elapsed)
  },

  _flushRealtimeParamThrottlePending() {
    if (!this._realtimeParamPending) return
    this._realtimeParamPending = false
    this._sendRealtimeParams().catch((err) => this._handleRealtimeParamSendError(err))
  },

  _flushRealtimeParamUpdate() {
    if (!this._isStreamingVocoderActive()) return
    if (!this._realtimeSocketReady) return

    this._clearRealtimeParamThrottle()
    this._sendRealtimeParams().catch((err) => this._handleRealtimeParamSendError(err))
  },

  _isPendingParamVersion(version) {
    if (!this._realtimeParamPromise) return false
    const responseVersion = Number(version)
    return Number.isFinite(responseVersion) && responseVersion === this._realtimeParamPromise.version
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
    if (!this._isPendingParamVersion(msg && msg.version)) return

    if (this._realtimeParamTimeoutTimer) {
      clearTimeout(this._realtimeParamTimeoutTimer)
      this._realtimeParamTimeoutTimer = null
    }
    const pending = this._realtimeParamPromise
    this._realtimeParamPromise = null
    this._realtimeAppliedParamVersion = pending.version
    pending.resolve(msg)
  },

  _rejectRealtimeParamPromise(msg) {
    if (!this._isPendingParamVersion(msg && msg.version)) return

    const err = new Error((msg && msg.message) ? msg.message : '参数更新失败')
    this._clearRealtimeParamPromise(err)
  },

  _sendRealtimeParams() {
    return new Promise((resolve, reject) => {
      if (!this._realtimeSocketReady || !this._realtimeSocket) {
        reject(new Error('实时连接未就绪'))
        return
      }

      this._clearRealtimeParamPromise(this._createParamSupersededError())
      const version = ++this._realtimeParamVersion
      this._realtimeParamPromise = { resolve, reject, version }
      this._realtimeParamTimeoutTimer = setTimeout(() => {
        if (!this._realtimeParamPromise || this._realtimeParamPromise.version !== version) return
        this._clearRealtimeParamPromise(new Error('参数同步超时'))
      }, 8000)

      const payload = JSON.stringify({
        type: 'PARAM_UPDATE',
        version,
        params: this._buildRealtimeParams()
      })

      this._realtimeParamLastSentAt = Date.now()
      this._realtimeSocket.send({
        data: payload,
        fail: (err) => {
          if (
            !this._realtimeParamPromise
            || this._realtimeParamPromise.version !== version
          ) {
            return
          }

          this._clearRealtimeParamPromise(err || new Error('参数发送失败'))
        }
      })
    })
  },

  _maybeSendRealtimeParams() {
    this._flushRealtimeParamUpdate()
  },

  async _initRealtimePcmPlayer(generation) {
    this._destroyRealtimePcmPlayer()

    const player = createRealtimePcmPlayer({
      onState: (state) => {
        if (this._unloaded) return
        this._handleRealtimePcmPlayerState(state, generation)
      },
      onFramePlay: (meta, frame) => {
        if (this._unloaded) return
        const panel = this._getVisualPanel()
        if (!panel) return
        if (meta && Array.isArray(meta.levels) && meta.levels.length && panel.applyRealtimeLevels) {
          panel.applyRealtimeLevels(meta.levels, {
            levelScale: 255,
            channelCount: meta.channelCount
          })
        }
        if (frame && frame.samples && frame.samples.length && panel.applyRealtimePcm) {
          panel.applyRealtimePcm(frame.samples, {
            sampleRate: frame.sampleRate
          })
        }
      }
    })

    this._realtimePcmPlayer = player
    await player.init()
    return player
  },

  _destroyRealtimePcmPlayer() {
    if (this._realtimePcmPlayer) {
      this._realtimePcmPlayer.destroy()
      this._realtimePcmPlayer = null
    }
    this._clearRealtimeVisualLevels()

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

    const stats = this._ensureRealtimeDebugStats()
    stats.sentFrames += 1
    stats.sentBytes += bytes
  },

  _clearRealtimeReadyTimeout() {
    if (!this._realtimeReadyTimeoutTimer) return
    clearTimeout(this._realtimeReadyTimeoutTimer)
    this._realtimeReadyTimeoutTimer = null
  },

  _clearRealtimeConnectTimeout() {
    if (!this._realtimeConnectTimeoutTimer) return
    clearTimeout(this._realtimeConnectTimeoutTimer)
    this._realtimeConnectTimeoutTimer = null
  },

  _clearRealtimeRecorderStartTimeout() {
    if (!this._realtimeRecorderStartTimeoutTimer) return
    clearTimeout(this._realtimeRecorderStartTimeoutTimer)
    this._realtimeRecorderStartTimeoutTimer = null
  },

  _setRealtimeConnecting(active) {
    if (this._unloaded) return
    const next = !!active
    if (this.data.realtimeConnecting === next) return
    this.setData({ realtimeConnecting: next }, () => {
      this._syncSourceDetailUI()
    })
  },

  _failRealtimeConnectAttempt(message) {
    this._realtimeStarting = false
    this._stopRealtimeDebugUiTimer()
    this._clearRealtimeRecorderStartTimeout()
    this._destroyRealtimePcmPlayer()
    this._closeRealtimeSocket({ silent: true, byUser: true })
    this._stopMicLevelVisualizer({ decay: false })
    if (this._unloaded) return
    this.setData({
      realtimeConnecting: false,
      realtimeRecording: false,
      realtimeError: message || '实时连接失败',
      realtimeStatusText: message || '实时连接失败，请检查网络或服务端'
    }, () => {
      this._syncSourceDetailUI()
    })
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
        this._rejectRealtimeParamPromise(msg)
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
    if (data.byteLength < 8) return

    const view = new DataView(data)
    const seq = view.getUint32(0, false)
    let offset = 4

    const pending = this._realtimePendingFrames && this._realtimePendingFrames.get(seq)
    if (!pending) return

    const rtt = Date.now() - pending.sentAt
    this._realtimePendingFrames.delete(seq)

    const pcmLength = view.getUint32(offset, false)
    offset += 4
    if (!Number.isFinite(pcmLength) || pcmLength < 0 || offset + pcmLength > data.byteLength) {
      console.warn('[realtime-ws] invalid pcmLength', { seq, pcmLength, bytes: data.byteLength })
      return
    }

    const pcmBytes = pcmLength
    if (pcmBytes !== pending.bytes) {
      console.warn('[realtime-ws] echo pcm size mismatch', {
        seq,
        pcmBytes,
        expected: pending.bytes
      })
    }

    let frameMeta = null
    const pcmEndOffset = offset + pcmLength
    if (pcmEndOffset < data.byteLength) {
      const channelCount = view.getUint8(pcmEndOffset)
      const levelsOffset = pcmEndOffset + 1
      const levelsAvailable = data.byteLength - levelsOffset
      if (channelCount > 0 && levelsAvailable >= channelCount) {
        const levels = []
        for (let i = 0; i < channelCount; i++) {
          levels.push(view.getUint8(levelsOffset + i))
        }
        frameMeta = { levels, channelCount }
      } else if (channelCount > 0) {
        console.warn('[realtime-ws] invalid levels payload', {
          seq,
          channelCount,
          levelsAvailable
        })
      }
    }

    if (pcmLength > 0 && this._realtimePcmPlayer) {
      const pcmBuffer = data.slice(offset, pcmEndOffset)
      this._realtimePcmPlayer.enqueue(pcmBuffer, frameMeta)
    }

    const receivedBytes = data.byteLength - 4

    this._realtimeRttTotal += rtt
    this._realtimeRttCount += 1
    const avgRtt = this._realtimeRttCount > 0
      ? Math.round(this._realtimeRttTotal / this._realtimeRttCount)
      : null

    const stats = this._ensureRealtimeDebugStats()
    stats.receivedFrames += 1
    stats.receivedBytes += receivedBytes
    stats.lastRttMs = rtt
    stats.rttTotal = this._realtimeRttTotal
    stats.rttCount = this._realtimeRttCount
    stats.avgRttMs = avgRtt
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
      const stats = this._ensureRealtimeDebugStats()
      stats.lostFrames += lost
    }
  },

  _ensureRealtimeDebugStats() {
    if (!this._realtimeDebugStats) {
      this._resetRealtimeDebugStats()
    }
    // 兼容旧字段引用
    this._realtimeStats = this._realtimeDebugStats
    return this._realtimeDebugStats
  },

  _resetRealtimeDebugStats() {
    this._realtimeDebugStats = {
      frameCount: 0,
      lastFrameBytes: 0,
      totalBytes: 0,
      sentFrames: 0,
      receivedFrames: 0,
      sentBytes: 0,
      receivedBytes: 0,
      lastRttMs: null,
      avgRttMs: null,
      rttTotal: 0,
      rttCount: 0,
      lostFrames: 0
    }
    this._realtimeStats = this._realtimeDebugStats
    this._realtimeRttTotal = 0
    this._realtimeRttCount = 0
    return this._realtimeDebugStats
  },

  _startRealtimeDebugUiTimer() {
    if (this._unloaded) return
    if (!this.data.showRealtimeDebug) {
      this._stopRealtimeDebugUiTimer()
      return
    }
    if (this._realtimeDebugUiTimer) return
    this._flushRealtimeDebugUi()
    this._realtimeDebugUiTimer = setInterval(() => {
      this._flushRealtimeDebugUi()
    }, DEBUG_UI_INTERVAL_MS)
  },

  _stopRealtimeDebugUiTimer() {
    if (!this._realtimeDebugUiTimer) return
    clearInterval(this._realtimeDebugUiTimer)
    this._realtimeDebugUiTimer = null
  },

  _flushRealtimeDebugUi() {
    if (this._unloaded || !this.data.showRealtimeDebug) return
    const stats = this._ensureRealtimeDebugStats()
    const avgRtt = stats.rttCount > 0
      ? Math.round(stats.rttTotal / stats.rttCount)
      : stats.avgRttMs
    this.setData({
      realtimeFrameCount: stats.frameCount,
      realtimeLastFrameBytes: stats.lastFrameBytes,
      realtimeTotalBytes: stats.totalBytes,
      realtimeSentFrames: stats.sentFrames,
      realtimeReceivedFrames: stats.receivedFrames,
      realtimeSentBytes: stats.sentBytes,
      realtimeReceivedBytes: stats.receivedBytes,
      realtimeLastRttMs: stats.lastRttMs,
      realtimeAvgRttMs: avgRtt,
      realtimeLostFrames: stats.lostFrames
    })
  },

  _stopRealtimeSession(options = {}) {
    const {
      byUser = false,
      stopRecorder = true,
      closeSocket = true,
      destroyPlayer = true,
      statusText = '实时麦克风已停止',
      error = '',
      showToast = false
    } = options

    if (this._realtimeSessionStopping) return
    this._realtimeSessionStopping = true

    try {
      const wasActive = !!(
        this.data.realtimeRecording
        || this._realtimeStarting
        || this.data.realtimeConnecting
        || this._realtimeSocket
        || this._realtimePcmPlayer
      )

      this._realtimePermissionRequesting = false
      this._realtimeStarting = false
      this._stopRealtimeDebugUiTimer()

      if (stopRecorder && this._recorderManager
        && (this.data.realtimeRecording || this.data.realtimeConnecting || this._realtimeSocketReady || this._realtimeStarting)) {
        // 由 session stop 主动触发的 recorder.stop，onStop 走轻量分支避免重复 close
        this._realtimeUserStop = true
        try {
          this._recorderManager.stop()
        } catch (e) {
          this._realtimeUserStop = false
          console.warn('[realtime-recorder] stop failed', e)
        }
      }

      if (destroyPlayer) {
        this._destroyRealtimePcmPlayer()
      }

      if (closeSocket) {
        this._closeRealtimeSocket({ silent: true, byUser: !!byUser })
      }

      this._stopMicLevelVisualizer({ decay: !byUser })
      this._clearRealtimeVisualLevels()

      if (this._streamingMode === 'mic') {
        this._streamingMode = null
      }

      if (!this._unloaded && wasActive) {
        this.setData({
          realtimeConnecting: false,
          realtimeRecording: false,
          realtimeSocketConnected: false,
          realtimeStatusText: statusText,
          realtimeError: error || ''
        }, () => {
          this._syncSourceDetailUI()
        })
        if (showToast && statusText) {
          wx.showToast({
            title: statusText.length > 20 ? statusText.slice(0, 20) : statusText,
            icon: 'none'
          })
        }
      }
    } finally {
      this._realtimeSessionStopping = false
    }
  },

  _closeRealtimeSocket(options = {}) {
    const { silent = false, byUser = false } = options
    if (byUser) {
      this._realtimeSocketClosingByUser = true
    }
    this._stopFileStreamPump()
    this._clearRealtimeParamThrottle()
    this._stopRealtimePendingSweep()
    this._clearRealtimeReadyTimeout()
    this._clearRealtimeConnectTimeout()
    this._clearRealtimeRecorderStartTimeout()
    this._realtimeSocketReady = false

    const connectReject = this._realtimeConnectReject
    const wasConnecting = !this._realtimeConnectSettled
      && typeof connectReject === 'function'
    this._realtimeConnectSettled = true
    this._realtimeConnectResolve = null
    this._realtimeConnectReject = null

    if (wasConnecting) {
      connectReject(new Error('WebSocket connection cancelled'))
    }

    this._clearRealtimeParamPromise(new Error('实时连接已关闭'))
    this._clearRealtimeVisualLevels()

    if (this._realtimeSocket) {
      try {
        this._realtimeSocket.close({
          code: 1000,
          reason: byUser ? 'user stop' : 'session stop'
        })
      } catch (e) {
        console.warn('[realtime-ws] close failed', e)
        this._realtimeSocketClosingByUser = false
      }
      this._realtimeSocket = null
    } else if (byUser) {
      this._realtimeSocketClosingByUser = false
    }

    this._realtimePendingFrames = null

    if (!silent && !this._unloaded) {
      this.setData({ realtimeSocketConnected: false })
    }
  },

  stopRealtimeMic() {
    this._stopRealtimeSession({
      byUser: true,
      stopRecorder: true,
      closeSocket: true,
      destroyPlayer: true,
      statusText: '实时麦克风已停止',
      showToast: false
    })
  },

  async startRealtimeMic() {
    if (this.data.realtimeRecording) {
      this._setRealtimeConnecting(false)
      return
    }
    if (this._realtimePermissionRequesting || this._realtimeStarting || this.data.realtimeConnecting) {
      return
    }

    this._realtimePermissionRequesting = true
    const permissionRequestId = ++this._realtimePermissionRequestId
    this._syncSourceDetailUI()

    let granted = false
    try {
      granted = await this._ensureRecordPermission()
    } finally {
      if (permissionRequestId === this._realtimePermissionRequestId) {
        this._realtimePermissionRequesting = false
        this._syncSourceDetailUI()
      }
    }

    if (permissionRequestId !== this._realtimePermissionRequestId) {
      return
    }

    if (!granted) {
      this._realtimeStarting = false
      this._setRealtimeConnecting(false)
      return
    }

    if (this.data.realtimeRecording) {
      this._setRealtimeConnecting(false)
      return
    }

    if (!this._unloaded) {
      this.setData({
        realtimeConnecting: true,
        realtimeError: ''
      }, () => {
        this._syncSourceDetailUI()
      })
    }

    this._stopFileStreamingProcessed({ silent: true })
    this._realtimeStarting = true
    this._streamingMode = 'mic'
    this._stopAudio('已停止播放')

    try {
      await this._connectRealtimeSocket()
      await this._sendRealtimeParams()
      await this._initRealtimePcmPlayer()
      this._startRealtimeRecorder()
    } catch (err) {
      if (err && err.message === 'WebSocket connection cancelled') {
        this._realtimeStarting = false
        this._clearRealtimeRecorderStartTimeout()
        this._destroyRealtimePcmPlayer()
        this._setRealtimeConnecting(false)
        return
      }
      console.error('[realtime-ws] connect failed', err)
      this._failRealtimeConnectAttempt(
        (err && err.errMsg) ? err.errMsg : ((err && err.message) ? err.message : '实时连接失败')
      )
    }
  },

  async _ensureSampleSource() {
    this._ensurePageState()
    const sampleCode = this.data.selectedSample
    const cached = this._sampleSourceCache[sampleCode]
    if (cached) {
      if (this.data.sourceType === 'sample') {
        this._applySampleSourceToData(cached)
      }
      return cached
    }

    const pending = this._samplePreparePromises[sampleCode]
    if (pending) {
      return pending
    }

    const preparePromise = (async () => {
      if (!this._unloaded && this.data.sourceType === 'sample') {
        this.setData({
          sourceType: 'sample',
          taskStatus: 'uploading',
          statusText: '正在准备示例声音...'
        }, () => {
          this._syncSourceDetailUI()
        })
      }

      try {
        const result = await prepareSampleSource(sampleCode)
        this._sampleSourceCache[sampleCode] = result
        if (!this._unloaded && this.data.sourceType === 'sample') {
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

  _clearOriginalVisualPcm() {
    const panel = this._getVisualPanel()
    if (panel && panel.clearOriginalPcm) {
      panel.clearOriginalPcm()
    }
  },

  _stopAudio(statusText = '已停止播放') {
    if (this._audioPlayer) {
      this._audioPlayer.stop()
    }

    if (this._unloaded) return

    this._clearOriginalVisualPcm()

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
    if (this._fileStreamActive || this._fileStreamStarting) {
      this._stopFileStreamingProcessed({ silent: true })
    }

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

    const playerCallbacks = {
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
        this._clearOriginalVisualPcm()
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
        this._clearOriginalVisualPcm()
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
    }

    if (kind === 'original') {
      const panel = this._getVisualPanel()
      if (panel && panel.clearRealtimePcm) {
        panel.clearRealtimePcm()
      }
      playerCallbacks.onPcmFrame = (frame) => {
        if (
          this._unloaded
          || kind !== 'original'
          || !frame
          || !frame.samples
          || !frame.samples.length
        ) {
          return
        }
        const visualPanel = this._getVisualPanel()
        if (visualPanel && visualPanel.applyOriginalPcm) {
          visualPanel.applyOriginalPcm(frame.samples, {
            sampleRate: frame.sampleRate,
            currentTime: frame.currentTime
          })
        }
      }
    } else {
      this._clearOriginalVisualPcm()
    }

    this._audioPlayer.play(url, playerCallbacks).catch((err) => {
      console.error(err)
    })
  },

  selectSource(e) {
    this._ensurePageState()
    const type = e.currentTarget.dataset.type

    if (this._fileStreamActive || this._fileStreamStarting) {
      this._stopFileStreamingProcessed({ silent: true })
    }

    if (this.data.sourceType === 'realtime' && type !== 'realtime') {
      if (this.data.realtimeRecording || this._realtimeStarting) {
        this.stopRealtimeMic()
      }
      this._destroyMicLevelCanvas()
    }

    if (type === 'upload') {
      this._stopAudio('已停止播放')
      this._cancelAutoRefresh()

      const snap = this._uploadSourceSnapshot
      const hasSuccess = !!(snap && snap.sourceAssetId && snap.originalAudioUrl)
      const keepError = !hasSuccess && this.data.uploadUiState === 'error'

      const nextData = {
        sourceType: 'upload',
        selectedScenario: '',
        sourceHint: '',
        isProcessing: false
      }

      if (hasSuccess) {
        nextData.sourceAssetId = snap.sourceAssetId
        nextData.originalAudioUrl = snap.originalAudioUrl
        nextData.uploadedFileName = snap.uploadedFileName || ''
        nextData.uploadedObjectKey = snap.uploadedObjectKey || ''
        nextData.uploadUiState = 'success'
        nextData.uploadUiText = '上传成功，可以试听原声或模拟声'
        nextData.uploadErrorText = ''
        nextData.taskStatus = 'ready'
        nextData.statusText = '音频上传成功，可以播放原声或模拟声'
        nextData.errorMessage = ''
      } else if (keepError) {
        nextData.uploadUiState = 'error'
        nextData.uploadUiText = this.data.uploadUiText || '上传失败'
        nextData.uploadErrorText = this.data.uploadErrorText || ''
        nextData.uploadedFileName = this.data.uploadedFileName || ''
        nextData.taskStatus = 'idle'
        nextData.statusText = '音频上传失败，请重试'
      } else {
        nextData.uploadUiState = 'idle'
        nextData.uploadUiText = ''
        nextData.uploadErrorText = ''
        nextData.taskStatus = 'idle'
        nextData.statusText = '已选择上传音频，请选择文件'
      }

      this._applyRuntimePatch({
        sourceType: 'upload',
        sourceAssetId: hasSuccess ? snap.sourceAssetId : (this.data.sourceAssetId || ''),
        selectedScenario: ''
      })
      this.setData(nextData, () => {
        this._syncSourceDetailUI()
      })
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
        this._syncSourceDetailUI()
      })
      return
    }

    if (type === 'realtime') {
      this._stopAudio('已停止播放')
      this._cancelAutoRefresh()
      this._cancelPrefetch()
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
        realtimeConnecting: false,
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
      }, () => {
        this._syncSourceDetailUI()
        this._mountMicLevelCanvasIdle()
      })
      return
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
    const wasFileStreaming = this._fileStreamActive
      && this.data.isAudioPlaying
      && this.data.playingKind === 'processed'
    const wasPlayingOriginal = this.data.isAudioPlaying && this.data.playingKind === 'original'
    const wasPlayingProcessedOffline = this.data.isAudioPlaying
      && this.data.playingKind === 'processed'
      && !wasFileStreaming

    if (wasFileStreaming || this._fileStreamStarting) {
      this._setProcessedUiState('switching', { displayName: label, force: true })
      this._stopFileStreamingProcessed({ silent: true, keepUiState: true })
    }

    if (!wasPlayingOriginal && !wasPlayingProcessedOffline && !wasFileStreaming) {
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
        : (wasFileStreaming || wasPlayingProcessedOffline)
          ? '正在切换示例并更新模拟声...'
          : `已选择${label}`,
      selectedScenario: '',
      ...(wasFileStreaming || this._fileStreamStarting
        ? this._buildProcessedUiView('switching', label)
        : {})
    }, () => {
      this._syncRuntimeParamsFromData()
      this._invalidateProcessedResult({
        keepStatus: true,
        autoRefresh: wasPlayingProcessedOffline
      })
      this._syncSourceDetailUI()
      if (wasFileStreaming) {
        this._startSampleStreamingProcessed({ preservePlayingState: true, displayName: label })
      } else if (wasPlayingOriginal) {
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
      statusText: `已切换至${scenarioName}场景`,
      ...this._midSliderPercents(preset)
    }, () => {
      this._invalidateProcessedResult()
      this._maybeSendRealtimeParams()
    })
  },

  onChannelsChanging(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ nChannels: value, selectedScenario: '' })
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      selectedScenario: '',
      channelSliderPct: this._sliderPct(value, 1, 22)
    }, () => {
      this._refreshVisualFeedback()
    })
    this._scheduleRealtimeParamUpdate()
  },

  changeChannels(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ nChannels: value, selectedScenario: '' })
    this._updateElectrodeDots(value)
    this.setData({
      nChannels: value,
      selectedScenario: '',
      channelSliderPct: this._sliderPct(value, 1, 22),
      statusText: `已设置 ${value} 通道`
    }, () => {
      this._invalidateProcessedResult()
      this._flushRealtimeParamUpdate()
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
      channelSliderPct: this._sliderPct(value, 1, 22),
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
      this._maybeSendRealtimeParams()
    })
  },

  changeEnvCut(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ envCut: value, selectedScenario: '' })
    this.setData({
      envCut: value,
      selectedScenario: '',
      envCutSliderPct: this._sliderPct(value, 20, 500),
      statusText: `包络细节已设为 ${value} Hz`
    }, () => {
      this._invalidateProcessedResult()
      this._flushRealtimeParamUpdate()
    })
  },

  onEnvCutChanging(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ envCut: value, selectedScenario: '' })
    this.setData({
      envCut: value,
      selectedScenario: '',
      envCutSliderPct: this._sliderPct(value, 20, 500)
    }, () => {
      this._refreshVisualFeedback()
    })
    this._scheduleRealtimeParamUpdate()
  },

  changeSpread(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ spread: value, selectedScenario: '' })
    this.setData({
      spread: value,
      selectedScenario: '',
      spreadSliderPct: this._sliderPct(value, 0, 100),
      statusText: `电流扩散已设为 ${value}%`
    }, () => {
      this._invalidateProcessedResult()
      this._flushRealtimeParamUpdate()
    })
  },

  onSpreadChanging(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ spread: value, selectedScenario: '' })
    this.setData({
      spread: value,
      selectedScenario: '',
      spreadSliderPct: this._sliderPct(value, 0, 100)
    }, () => {
      this._refreshVisualFeedback()
    })
    this._scheduleRealtimeParamUpdate()
  },

  changeNoiseLevel(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ noiseLevel: value, selectedScenario: '' })
    this.setData({
      noiseLevel: value,
      selectedScenario: '',
      noiseSliderPct: this._sliderPct(value, 0, 100),
      statusText: `环境噪声已设为 ${value}%`
    }, () => {
      this._invalidateProcessedResult()
      this._flushRealtimeParamUpdate()
    })
  },

  onNoiseLevelChanging(e) {
    const value = Number(e.detail.value)
    if (!Number.isFinite(value)) return
    this._applyRuntimePatch({ noiseLevel: value, selectedScenario: '' })
    this.setData({
      noiseLevel: value,
      selectedScenario: '',
      noiseSliderPct: this._sliderPct(value, 0, 100)
    }, () => {
      this._refreshVisualFeedback()
    })
    this._scheduleRealtimeParamUpdate()
  },

  _chooseAndUpload() {
    if (this.data.taskStatus === 'uploading' || this.data.uploadUiState === 'uploading') return

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
            statusText: this.data.uploadUiState === 'success'
              ? '音频上传成功，可以播放原声或模拟声'
              : '已选择上传音频，请选择文件',
            sourceHint: ''
          }, () => {
            this._syncSourceDetailUI()
          })
        }
      }
    })
  },

  async _doUpload(filePath, fileName) {
    if (this._fileStreamActive || this._fileStreamStarting) {
      this._stopFileStreamingProcessed({ silent: true })
    }
    this._stopAudio('已停止播放')
    this._cancelAutoRefresh()

    const displayName = fileName || '音频文件'
    this.setData({
      sourceType: 'upload',
      taskStatus: 'uploading',
      statusText: '正在上传音频...',
      uploadedFileName: displayName,
      sourceHint: '',
      isProcessing: false,
      errorMessage: '',
      clarityDesc: '',
      uploadUiState: 'uploading',
      uploadUiText: `正在上传「${displayName}」…`,
      uploadErrorText: ''
    }, () => {
      this._syncSourceDetailUI()
    })

    try {
      const result = await uploadAudio(filePath)
      const successName = result.fileName || displayName
      this._uploadSourceSnapshot = {
        sourceAssetId: result.assetId,
        originalAudioUrl: result.url,
        uploadedFileName: successName,
        uploadedObjectKey: result.objectKey
      }
      this.setData({
        sourceAssetId: result.assetId,
        originalAudioUrl: result.url,
        uploadedFileName: successName,
        uploadedObjectKey: result.objectKey,
        taskNo: '',
        outputAssetId: null,
        processedAudioUrl: '',
        processedKey: '',
        taskStatus: 'ready',
        statusText: '音频上传成功，可以播放原声或模拟声',
        sourceHint: '',
        uploadUiState: 'success',
        uploadUiText: '上传成功，可以试听原声或模拟声',
        uploadErrorText: ''
      }, () => {
        this._syncRuntimeParamsFromData()
        this._invalidateProcessedResult({ keepStatus: true, autoRefresh: false })
        this._syncSourceDetailUI()
      })
      wx.showToast({
        title: '上传成功',
        icon: 'success'
      })
    } catch (err) {
      console.error(err)
      const errorText = this._resolveUploadError(err)
      this.setData({
        taskStatus: 'idle',
        statusText: '音频上传失败，请重试',
        sourceHint: '',
        errorMessage: errorText,
        uploadUiState: 'error',
        uploadUiText: '上传失败',
        uploadErrorText: errorText,
        uploadedFileName: displayName
      }, () => {
        this._syncSourceDetailUI()
      })
      wx.showToast({
        title: '上传失败',
        icon: 'none'
      })
    }
  },

  async playOriginal() {
    if (this._fileStreamActive || this._fileStreamStarting) {
      this._stopFileStreamingProcessed()
    }

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

    if (this.data.sourceType === 'realtime') {
      wx.showToast({ title: '请先使用下方实时体验', icon: 'none' })
      return
    }
  },

  _preloadProcessedAudio(url) {
    if (!url || !this._audioPlayer || typeof this._audioPlayer.preload !== 'function') {
      return
    }
    this._audioPlayer.preload(url).catch((err) => {
      console.warn('[processed-prefetch] audio preload failed', err)
    })
  },

  _invalidateInFlightPrefetch() {
    if (this._prefetchTimer) {
      clearTimeout(this._prefetchTimer)
      this._prefetchTimer = null
    }
    this._prefetchSeq += 1
  },

  _cancelPrefetch() {
    this._invalidateInFlightPrefetch()
  },

  _schedulePrefetchProcessed(delayMs = 800) {
    if (this._unloaded) return
    if (this.data.sourceType === 'realtime') return
    if (this._fileStreamActive || this._fileStreamStarting) return
    if (this.data.isProcessing) return

    if (this._prefetchTimer) {
      clearTimeout(this._prefetchTimer)
    }

    this._prefetchTimer = setTimeout(() => {
      this._prefetchTimer = null
      this._prefetchProcessedAudio()
    }, delayMs)
  },

  async _prefetchProcessedAudio() {
    if (this._unloaded || this.data.isProcessing) return
    if (this.data.sourceType === 'realtime') return
    if (this._fileStreamActive || this._fileStreamStarting) return

    const key = this._buildProcessKey()
    if (this.data.processedKey === key && this.data.processedAudioUrl) {
      this._preloadProcessedAudio(this.data.processedAudioUrl)
      return
    }

    if (this._processedResultCache && this._processedResultCache.has(key)) {
      const cached = this._processedResultCache.get(key)
      if (cached && cached.processedAudioUrl) {
        this._preloadProcessedAudio(cached.processedAudioUrl)
      }
      return
    }

    const prefetchSeq = ++this._prefetchSeq
    try {
      await this._generateProcessedAudio({
        autoPlay: false,
        background: true,
        prefetchSeq
      })
    } catch (err) {
      console.warn('[processed-prefetch] generate failed', err)
    }
  },

  _isGenerateRequestStale(background, requestSeq) {
    if (background) {
      return requestSeq !== this._prefetchSeq
    }
    return requestSeq !== this._autoRefreshSeq
  },

  async _generateProcessedAudio(options = {}) {
    const { autoPlay = false, replacePlaying = false, background = false } = options
    const requestSeq = background
      ? (options.prefetchSeq != null ? options.prefetchSeq : (++this._prefetchSeq))
      : (options.seq != null ? options.seq : (++this._autoRefreshSeq))

    if (!background) {
      this._cancelPrefetch()
    }

    if (this.data.sourceType === 'realtime') {
      if (!background) {
        wx.showToast({ title: '实时麦克风模式下请使用实时体验', icon: 'none' })
      }
      return
    }

    let sourceAssetId = ''

    try {
      if (this.data.sourceType === 'sample') {
        const sampleSource = await this._ensureSampleSource()
        if (this.data.sourceType !== 'sample') {
          return
        }
        sourceAssetId = sampleSource.assetId
      } else if (this.data.sourceType === 'upload') {
        sourceAssetId = this._resolveSourceAssetId()
        if (!sourceAssetId) {
          if (!background) {
            wx.showToast({ title: '请先上传音频', icon: 'none' })
          }
          return
        }
      } else {
        return
      }
    } catch (err) {
      console.error(err)
      if (background) {
        console.warn('[processed-prefetch] source prepare failed', err)
        return
      }
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
      } else if (background) {
        this._preloadProcessedAudio(this.data.processedAudioUrl)
      }
      return
    }

    const cached = this._getCachedProcessedResult(key)
    if (cached) {
      if (this._isGenerateRequestStale(background, requestSeq)) {
        return
      }
      console.log('[processed-cache] hit', key)
      this._applyProcessedResult(key, cached, {
        autoPlay,
        replacePlaying,
        fromCache: true,
        background
      })
      return
    }

    console.log('[processed-cache] miss', key)

    if (!background) {
      this.setData({
        isProcessing: true,
        taskStatus: 'processing',
        statusText: replacePlaying ? '正在按新参数更新模拟声...' : '正在生成模拟声音...',
        errorMessage: ''
      }, () => {
        this._refreshVisualFeedback()
      })
    }

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

      if (this._isGenerateRequestStale(background, requestSeq)) {
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
        fromCache: false,
        background
      })
    } catch (err) {
      if (this._isGenerateRequestStale(background, requestSeq)) {
        return
      }
      console.error(err)
      if (background) {
        console.warn('[processed-prefetch] generate failed', err)
        return
      }
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
    if (this.data.sourceType === 'sample') {
      if (this._isProcessedUiBusy()) return
      if (this._fileStreamActive
        && (this.data.processedUiState === 'playing'
          || (this.data.isAudioPlaying && this.data.playingKind === 'processed'))) {
        this._setProcessedUiState('stopping', { force: true })
        this._stopFileStreamingProcessed()
        return
      }
      await this._startSampleStreamingProcessed()
      return
    }

    if (this.data.sourceType === 'upload') {
      if (this._isProcessedUiBusy()) return
      if (this._fileStreamActive
        && (this.data.processedUiState === 'playing'
          || (this.data.isAudioPlaying && this.data.playingKind === 'processed'))) {
        this._setProcessedUiState('stopping', { force: true })
        this._stopFileStreamingProcessed()
        return
      }
      await this._startUploadStreamingProcessed()
      return
    }

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
