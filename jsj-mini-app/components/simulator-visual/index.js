const { getDpr } = require('../../utils/simulator/visual/common.js')
const {
  getFrameAtCurrentTime,
  buildFallbackFrame,
  buildElectrodeBars,
  normalizeChannelFrame
} = require('../../utils/simulator/visual/frames.js')
const { drawNeuroColumn, clearNeuroCanvas } = require('../../utils/simulator/visual/draw-neuro.js')
const { drawWave } = require('../../utils/simulator/visual/draw-wave.js')
const { drawSpecColumn, clearSpecCanvas } = require('../../utils/simulator/visual/draw-spec.js')
const { drawCochlea } = require('../../utils/simulator/visual/draw-cochlea.js')

const IDLE_FPS = 20
const IDLE_INTERVAL_MS = Math.round(1000 / IDLE_FPS)

const VIZ_TABS = [
  { id: 'bars', label: '电极阵列' },
  { id: 'wave', label: '示波器' },
  { id: 'spec', label: '频谱图' },
  { id: 'cochlea', label: '耳蜗' },
  { id: 'neuro', label: '神经元放电' }
]

function parseFreqRange(text) {
  const match = String(text || '').match(/(\d+)\s*-\s*(\d+)/)
  if (!match) return { lo: 150, hi: 7000 }
  return { lo: Number(match[1]), hi: Number(match[2]) }
}

function formatFreqHi(hi) {
  return hi >= 1000 ? `${(hi / 1000).toFixed(hi % 1000 === 0 ? 0 : 1)} kHz` : `${hi} Hz`
}

function isCanvasView(view) {
  return view === 'wave' || view === 'neuro' || view === 'spec' || view === 'cochlea'
}

Component({
  options: {
    styleIsolation: 'isolated'
  },

  properties: {
    nChannels: { type: Number, value: 8 },
    spread: { type: Number, value: 0 },
    carrier: { type: String, value: 'noise' },
    noiseLevel: { type: Number, value: 0 },
    envCut: { type: Number, value: 160 },
    frequencyRange: { type: String, value: '150-7000' },
    isAudioPlaying: { type: Boolean, value: false },
    playingKind: { type: String, value: '' },
    audioSeekSec: { type: Number, value: 0 },
    realtimeActive: { type: Boolean, value: false }
  },

  data: {
    vizTabs: VIZ_TABS,
    activeView: 'bars',
    vizMeta: '',
    freqLo: '150',
    freqHiLabel: '7 kHz',
    electrodeBarLevels: []
  },

  lifetimes: {
    attached() {
      this._vizCanvas = null
      this._vizCtx = null
      this._specCanvas = null
      this._specCtx = null
      this._cochleaCanvas = null
      this._cochleaCtx = null
      this._dpr = 2
      this._neuroChannelCount = 0
      this._playbackTimer = null
      this._idleTimer = null
      this._visualizationFrames = []
      this._visualizationFps = 30
      this._visualizationDurationMs = 0
      this._visualizationLevelScale = 255
      this._visualizationNChannels = 0
      this._visualizationBands = []
      this._realtimeLevels = null
      this._realtimeChannelCount = 0
      this._realtimeLevelScale = 255
      this._audioAnchorSec = 0
      this._audioAnchorWallMs = Date.now()
      this._updateMeta()
      this._refreshStaticViews()
    },

    ready() {
      this.syncPlaybackState()
    },

    detached() {
      this._stopPlaybackTimer()
      this._stopIdleTimer()
      this._clearCanvasRefs()
    }
  },

  observers: {
    'nChannels, spread, carrier, noiseLevel, envCut, frequencyRange': function () {
      this._updateMeta()
      this._refreshStaticViews()
      if (this.data.activeView === 'neuro' || this.data.activeView === 'spec') {
        this._resetScrollViews()
      }
      if (this.data.activeView === 'cochlea' && this._hasCanvasForView('cochlea')) {
        this._drawActiveCanvasFrame()
      }
    },
    'isAudioPlaying, playingKind': function () {
      this.syncPlaybackState()
    },
    audioSeekSec(value) {
      this._audioAnchorSec = Number(value) || 0
      this._audioAnchorWallMs = Date.now()
      if (this._playbackTimer) return
      this._refreshStaticViews()
    },
    realtimeActive(value) {
      if (!value) {
        this.clearRealtimeLevels()
        return
      }
      this._stopIdleTimer()
      this._refreshStaticViews()
    }
  },

  methods: {
    onTabTap(e) {
      const view = e.currentTarget.dataset.view
      if (!view || view === this.data.activeView) return
      this._stopIdleTimer()
      this._stopPlaybackTimer(false)
      this._clearCanvasRefs()

      this.setData({ activeView: view }, () => {
        if (view === 'bars') {
          this._refreshStaticViews()
          this.syncPlaybackState()
          return
        }
        this._mountCanvasView(view)
      })
    },

    applyVisualizationData(viz) {
      if (viz && Array.isArray(viz.frames) && viz.frames.length) {
        this._visualizationFrames = viz.frames
        this._visualizationFps = Number(viz.fps) || 30
        this._visualizationDurationMs = Number(viz.durationMs) || 0
        this._visualizationLevelScale = Number(viz.levelScale) || 255
        this._visualizationNChannels = Number(viz.nChannels)
          || (Array.isArray(viz.frames[0]) ? viz.frames[0].length : 0)
        this._visualizationBands = Array.isArray(viz.bands) ? viz.bands : []

        if (viz.frames[20]) {
          const scale = this._visualizationLevelScale
          const raw = viz.frames[20]
          const normalized = raw.map((v) => {
            const n = Number(v) || 0
            return Math.max(0, Math.min(1, n / scale))
          })
          console.log('[viz] frames[20] raw:', raw, 'normalized:', normalized)
        }
      } else {
        this._visualizationFrames = []
        this._visualizationFps = 30
        this._visualizationDurationMs = 0
        this._visualizationLevelScale = 255
        this._visualizationNChannels = 0
        this._visualizationBands = []
      }
      if (isCanvasView(this.data.activeView)) {
        this._resetScrollViews()
      }
      this._refreshStaticViews()
    },

    clearVisualizationData() {
      this.applyVisualizationData(null)
    },

    applyRealtimeLevels(levels, opts) {
      const options = opts || {}
      const channelCount = Number(options.channelCount) || (Array.isArray(levels) ? levels.length : 0)
      const levelScale = Number(options.levelScale) > 0 ? Number(options.levelScale) : 255
      if (!this.properties.realtimeActive || !Array.isArray(levels) || !levels.length || channelCount <= 0) {
        return
      }

      this._realtimeLevels = levels.slice(0, channelCount)
      this._realtimeChannelCount = channelCount
      this._realtimeLevelScale = levelScale
      this._refreshStaticViews()
      if (isCanvasView(this.data.activeView) && this._hasCanvasForView(this.data.activeView)) {
        this._drawActiveCanvasFrame()
      }
    },

    clearRealtimeLevels() {
      this._realtimeLevels = null
      this._realtimeChannelCount = 0
      this._realtimeLevelScale = 255
      this._refreshStaticViews()
      if (!this.properties.isAudioPlaying) {
        this._startIdleTimer()
      }
    },

    syncPlaybackState() {
      if (this.properties.isAudioPlaying) {
        this._stopIdleTimer()
        this._startPlaybackTimer()
        return
      }
      this._stopPlaybackTimer(false)
      this._startIdleTimer()
    },

    refreshViews() {
      this._refreshStaticViews()
      if (isCanvasView(this.data.activeView) && this._hasCanvasForView(this.data.activeView)) {
        this._drawActiveCanvasFrame()
      }
    },

    _clearCanvasRefs() {
      this._vizCanvas = null
      this._vizCtx = null
      this._specCanvas = null
      this._specCtx = null
      this._cochleaCanvas = null
      this._cochleaCtx = null
      this._neuroChannelCount = 0
    },

    _hasCanvasForView(view) {
      if (view === 'wave' || view === 'neuro') return !!(this._vizCtx && this._vizCanvas)
      if (view === 'spec') return !!(this._specCtx && this._specCanvas)
      if (view === 'cochlea') return !!(this._cochleaCtx && this._cochleaCanvas)
      return false
    },

    _getCanvasSelector(view) {
      if (view === 'spec') return '#specCanvas'
      if (view === 'cochlea') return '#cochleaCanvas'
      return '#vizCanvas'
    },

    _getCanvasKind(view) {
      if (view === 'spec') return 'spec'
      if (view === 'cochlea') return 'cochlea'
      return 'viz'
    },

    _mountCanvasView(view) {
      const selector = this._getCanvasSelector(view)
      const init = (retry = 0) => {
        this.createSelectorQuery()
          .in(this)
          .select(selector)
          .fields({ node: true, size: true })
          .exec((res) => {
            const item = res && res[0]
            if (!item || !item.node || !item.width || !item.height) {
              if (retry < 16) {
                setTimeout(() => init(retry + 1), 60 + retry * 40)
              } else {
                this.syncPlaybackState()
              }
              return
            }
            this._setupCanvas(this._getCanvasKind(view), item)
            if (view === 'neuro' || view === 'spec') {
              this._resetScrollViews()
            } else {
              this._drawActiveCanvasFrame()
            }
            this.syncPlaybackState()
          })
      }

      wx.nextTick(() => {
        setTimeout(() => init(0), 32)
      })
    },

    _getRuntimeFlags() {
      const isPlaying = !!this.properties.isAudioPlaying
      const playingKind = this.properties.playingKind || ''
      return {
        isPlaying,
        isProcessed: isPlaying && playingKind === 'processed',
        isOriginal: isPlaying && playingKind === 'original'
      }
    },

    _getEstimatedAudioTime() {
      const flags = this._getRuntimeFlags()
      if (flags.isProcessed) {
        return this._audioAnchorSec
          + (Date.now() - this._audioAnchorWallMs) / 1000
      }
      return Number(this.properties.audioSeekSec) || 0
    },

    _getCurrentLevels() {
      const flags = this._getRuntimeFlags()
      const nChannels = Number(this.properties.nChannels) || 8

      if (
        this.properties.realtimeActive
        && Array.isArray(this._realtimeLevels)
        && this._realtimeLevels.length
      ) {
        const channelCount = this._realtimeChannelCount || this._realtimeLevels.length || nChannels
        return normalizeChannelFrame(
          this._realtimeLevels,
          channelCount,
          this._realtimeLevelScale || 255
        )
      }

      if (this._visualizationFrames && this._visualizationFrames.length && flags.isProcessed) {
        const channelCount = this._visualizationNChannels || nChannels
        return getFrameAtCurrentTime(this._visualizationFrames, {
          channelCount,
          fps: this._visualizationFps,
          durationMs: this._visualizationDurationMs,
          audioSeekSec: this._getEstimatedAudioTime(),
          levelScale: this._visualizationLevelScale
        })
      }
      return buildFallbackFrame({
        nChannels,
        spread: this.properties.spread,
        noiseLevel: this.properties.noiseLevel,
        envCut: this.properties.envCut,
        carrier: this.properties.carrier,
        isProcessed: flags.isProcessed,
        isOriginal: flags.isOriginal,
        isPlaying: flags.isPlaying
      })
    },

    _getDrawOpts() {
      const flags = this._getRuntimeFlags()
      return {
        nChannels: Number(this.properties.nChannels) || 8,
        spread: this.properties.spread,
        carrier: this.properties.carrier,
        noiseLevel: this.properties.noiseLevel,
        isPlaying: flags.isPlaying,
        playingKind: this.properties.playingKind,
        isProcessed: flags.isProcessed
      }
    },

    _updateMeta() {
      const { lo, hi } = parseFreqRange(this.properties.frequencyRange)
      const carrierLabel = this.properties.carrier === 'sine' ? '正弦载波' : '噪声载波'
      this.setData({
        vizMeta: `${this.properties.nChannels} 个通道 · ${carrierLabel}`,
        freqLo: String(lo),
        freqHiLabel: formatFreqHi(hi)
      })
    },

    _refreshStaticViews() {
      const levels = this._getCurrentLevels()
      const flags = this._getRuntimeFlags()
      const isRealtime = !!this.properties.realtimeActive
      const isProcessed = flags.isProcessed || isRealtime
      this.setData({
        electrodeBarLevels: buildElectrodeBars(levels, { isProcessed })
      })
      if (this.data.activeView === 'cochlea' && this._hasCanvasForView('cochlea')) {
        this._drawActiveCanvasFrame()
      }
    },

    _setupCanvas(kind, item) {
      const canvas = item.node
      const ctx = canvas.getContext('2d')
      const dpr = getDpr()
      canvas.width = Math.max(1, Math.round(item.width * dpr))
      canvas.height = Math.max(1, Math.round(item.height * dpr))
      if (kind === 'viz') {
        this._vizCanvas = canvas
        this._vizCtx = ctx
      } else if (kind === 'spec') {
        this._specCanvas = canvas
        this._specCtx = ctx
      } else if (kind === 'cochlea') {
        this._cochleaCanvas = canvas
        this._cochleaCtx = ctx
      }
      this._dpr = dpr
    },

    _resetScrollViews() {
      this._neuroChannelCount = 0
      clearNeuroCanvas(this._vizCtx, this._vizCanvas)
      clearSpecCanvas(this._specCtx, this._specCanvas)
      this._drawActiveCanvasFrame()
    },

    _drawActiveCanvasFrame() {
      const view = this.data.activeView
      if (!isCanvasView(view)) return

      const levels = this._getCurrentLevels()
      const opts = this._getDrawOpts()
      const flags = this._getRuntimeFlags()
      const isProcessed = flags.isProcessed || !!this.properties.realtimeActive

      if (view === 'wave' && this._vizCtx && this._vizCanvas) {
        drawWave(this._vizCtx, this._vizCanvas, this._dpr, opts)
        return
      }

      if (view === 'neuro' && this._vizCtx && this._vizCanvas) {
        const N = levels.length
        if (N !== this._neuroChannelCount) {
          this._neuroChannelCount = N
          clearNeuroCanvas(this._vizCtx, this._vizCanvas)
        }
        drawNeuroColumn(this._vizCtx, this._vizCanvas, levels, this._dpr, isProcessed)
        return
      }

      if (view === 'spec' && this._specCtx && this._specCanvas) {
        drawSpecColumn(this._specCtx, this._specCanvas, this._dpr, opts)
        return
      }

      if (view === 'cochlea' && this._cochleaCtx && this._cochleaCanvas) {
        drawCochlea(this._cochleaCtx, this._cochleaCanvas, this._dpr, levels, {
          ...opts,
          spread: this.properties.spread,
          noiseLevel: this.properties.noiseLevel,
          isPlaying: flags.isPlaying || !!this.properties.realtimeActive
        })
      }
    },

    _tickPlayback() {
      this._refreshStaticViews()
      if (isCanvasView(this.data.activeView)) {
        this._drawActiveCanvasFrame()
      }
    },

    _tickIdle() {
      const view = this.data.activeView
      if (view === 'bars') {
        this._refreshStaticViews()
        return
      }
      if (!this._hasCanvasForView(view)) {
        this._mountCanvasView(view)
        return
      }
      this._drawActiveCanvasFrame()
    },

    _startPlaybackTimer() {
      this._stopPlaybackTimer(false)
      const fps = this._visualizationFps || 30
      const interval = Math.max(33, Math.round(1000 / fps))
      this._playbackTimer = setInterval(() => this._tickPlayback(), interval)
      this._tickPlayback()
    },

    _stopPlaybackTimer(reset = true) {
      if (this._playbackTimer) {
        clearInterval(this._playbackTimer)
        this._playbackTimer = null
      }
      if (reset) {
        this._refreshStaticViews()
        if (isCanvasView(this.data.activeView) && this._hasCanvasForView(this.data.activeView)) {
          this._resetScrollViews()
        }
      }
    },

    _startIdleTimer() {
      if (this._idleTimer || this.properties.isAudioPlaying) return
      this._tickIdle()
      this._idleTimer = setInterval(() => this._tickIdle(), IDLE_INTERVAL_MS)
    },

    _stopIdleTimer() {
      if (!this._idleTimer) return
      clearInterval(this._idleTimer)
      this._idleTimer = null
    }
  }
})
