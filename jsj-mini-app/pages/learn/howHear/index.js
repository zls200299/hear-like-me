const PAL = ['#2B2E83', '#1E7CC2', '#16B9A6', '#7DC93F', '#F2C14E']
const STAGE_X = [0.08, 0.36, 0.67, 0.91]
const FRAME_MS = 1000 / 28
const SPAWN_INTERVAL = 0.28
const SPEED = 0.42

function rgba(hex, alpha) {
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${alpha})`
}

Page({
  onReady() {
    this._journeyAlive = true
    this._initJourneyCanvas()
  },

  onShow() {
    this._journeyAlive = true
    if (this._journeyCanvas && this._journeyCtx) {
      this._startJourneyLoop()
    }
  },

  onHide() {
    this._stopJourneyLoop()
  },

  onUnload() {
    this._journeyAlive = false
    this._stopJourneyLoop()
    this._journeyCanvas = null
    this._journeyCtx = null
    this._journeyCssW = 0
    this._journeyCssH = 0
  },

  _initJourneyCanvas(retry = 0) {
    if (!this._journeyAlive) return

    this.createSelectorQuery()
      .select('#journeyCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!this._journeyAlive) return
        const item = res && res[0]
        if (!item || !item.node || !item.width || !item.height) {
          if (retry < 8) {
            setTimeout(() => this._initJourneyCanvas(retry + 1), 50)
          }
          return
        }

        const canvas = item.node
        const ctx = canvas.getContext('2d')
        const dpr = (wx.getWindowInfo
          ? wx.getWindowInfo().pixelRatio
          : wx.getSystemInfoSync().pixelRatio) || 2

        canvas.width = Math.floor(item.width * dpr)
        canvas.height = Math.floor(item.height * dpr)
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

        this._journeyCanvas = canvas
        this._journeyCtx = ctx
        this._journeyCssW = item.width
        this._journeyCssH = item.height
        this._pulses = []
        this._lastSpawn = 0
        this._journeyStartMs = Date.now()
        this._lastFrameMs = 0

        this._startJourneyLoop()
      })
  },

  _startJourneyLoop() {
    if (!this._journeyAlive || !this._journeyCanvas || this._journeyRaf) return

    const canvas = this._journeyCanvas
    const tick = () => {
      if (!this._journeyAlive || !this._journeyCanvas) {
        this._journeyRaf = null
        return
      }

      const now = Date.now()
      if (!this._lastFrameMs || now - this._lastFrameMs >= FRAME_MS) {
        const elapsed = this._lastFrameMs ? (now - this._lastFrameMs) / 1000 : FRAME_MS / 1000
        this._lastFrameMs = now
        const t = (now - (this._journeyStartMs || now)) / 1000
        this._drawJourney(t, Math.min(0.05, elapsed))
      }

      this._journeyRaf = canvas.requestAnimationFrame(tick)
    }

    this._journeyRaf = canvas.requestAnimationFrame(tick)
  },

  _stopJourneyLoop() {
    if (this._journeyRaf && this._journeyCanvas) {
      try {
        this._journeyCanvas.cancelAnimationFrame(this._journeyRaf)
      } catch (e) {
        // ignore
      }
    }
    this._journeyRaf = null
  },

  _drawJourney(t, dt) {
    const c = this._journeyCtx
    const w = this._journeyCssW
    const h = this._journeyCssH
    if (!c || !w || !h) return

    c.clearRect(0, 0, w, h)

    const yMid = h * 0.46
    const sx = STAGE_X.map((p) => w * p)
    const soundX = sx[0]
    const procX = sx[1]
    const elecX = sx[2]
    const brainX = sx[3]

    // baseline
    c.strokeStyle = 'rgba(255,255,255,0.08)'
    c.lineWidth = 2
    c.beginPath()
    c.moveTo(soundX, yMid)
    c.lineTo(brainX, yMid)
    c.stroke()

    // Sound → Processor: many thin information lines
    for (let i = 0; i < 14; i++) {
      const yy = yMid + (i - 6.5) * (h * 0.05)
      c.strokeStyle = rgba(PAL[i % 5], 0.16)
      c.lineWidth = 1
      c.beginPath()
      c.moveTo(soundX, yy)
      c.lineTo(procX, yMid)
      c.stroke()
    }

    // Processor → Electrodes → Brain: few thick channels
    for (let i = 0; i < 6; i++) {
      const yy = yMid + (i - 2.5) * (h * 0.06)
      c.strokeStyle = rgba(PAL[i % 5], 0.5)
      c.lineWidth = 2.4
      c.beginPath()
      c.moveTo(procX, yMid)
      c.lineTo(elecX, yy)
      c.stroke()
      c.beginPath()
      c.moveTo(elecX, yy)
      c.lineTo(brainX, yMid)
      c.stroke()
    }

    // Spawn green information dots
    if (!this._pulses) this._pulses = []
    if (t - (this._lastSpawn || 0) > SPAWN_INTERVAL) {
      this._lastSpawn = t
      this._pulses.push({
        p: 0,
        seg: 'in',
        r: 2.2 + Math.random() * 1.4
      })
    }

    // Advance pulses (match original ~60fps step of 0.016 * speed)
    const step = dt * 60 * 0.016 * SPEED
    this._pulses.forEach((pu) => {
      pu.p += step * (pu.seg === 'in' ? 1.4 : 1)
      if (pu.seg === 'in' && pu.p >= 1) {
        pu.seg = 'out'
        pu.p = 0
        if (Math.random() < 0.6) pu._drop = true
      }
    })
    this._pulses = this._pulses.filter((pu) => !(pu.seg === 'out' && pu.p >= 1.02))

    this._pulses.forEach((pu) => {
      if (pu._drop) return

      const x = pu.seg === 'in'
        ? soundX + (procX - soundX) * Math.min(pu.p, 1)
        : procX + (brainX - procX) * Math.min(pu.p, 1)

      const col = pu.seg === 'in' ? '#7DC93F' : '#16B9A6'
      const rr = (pu.r || 2.6) * (pu.seg === 'out' ? 1.5 : 1)
      c.fillStyle = col
      c.shadowColor = col
      c.shadowBlur = 10
      c.beginPath()
      c.arc(x, yMid, rr, 0, Math.PI * 2)
      c.fill()
      c.shadowBlur = 0
    })

    // Electrode discharge ticks
    for (let k = 0; k < 6; k++) {
      const yy = yMid + (k - 2.5) * (h * 0.06)
      const a = 0.3 + 0.5 * Math.abs(Math.sin(t * 3 + k))
      c.strokeStyle = rgba('#F2C14E', a * 0.7)
      c.lineWidth = 1.5
      c.beginPath()
      c.moveTo(elecX, yy - 4)
      c.lineTo(elecX, yy + 4)
      c.stroke()
    }

    // Brain breathing glow
    const glowR = 26 + 4 * Math.sin(t * 2)
    const bg = c.createRadialGradient(brainX, yMid, 2, brainX, yMid, glowR)
    bg.addColorStop(0, 'rgba(22,185,166,0.5)')
    bg.addColorStop(1, 'rgba(22,185,166,0)')
    c.fillStyle = bg
    c.beginPath()
    c.arc(brainX, yMid, 26, 0, Math.PI * 2)
    c.fill()
  }
})
