const TOTAL_CARDS = 6
const FRAME_MS = 1000 / 30

function rgba(hex, alpha) {
  const value = parseInt(hex.slice(1), 16)
  return `rgba(${(value >> 16) & 255},${(value >> 8) & 255},${value & 255},${alpha})`
}

function glowDot(ctx, x, y, radius, color, blur = 12) {
  ctx.save()
  ctx.fillStyle = color
  ctx.shadowColor = color
  ctx.shadowBlur = blur
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()
}

function drawMusic(ctx, w, h, t) {
  const accent = '#9b8cff'
  const y = h * 0.42
  const leftStart = w * 0.16
  const leftGap = w * 0.045
  const rightStart = w * 0.67
  const rightGap = w * 0.05

  ctx.save()
  ctx.lineCap = 'round'
  ctx.lineWidth = 3
  ctx.strokeStyle = accent
  ctx.shadowColor = accent
  ctx.shadowBlur = 9
  for (let i = 0; i < 8; i++) {
    const wave = 0.58 + 0.42 * Math.abs(Math.sin(t * 2.4 + i * 0.72))
    const height = (12 + (i % 4) * 8) * wave
    const x = leftStart + i * leftGap
    ctx.beginPath()
    ctx.moveTo(x, y - height)
    ctx.lineTo(x, y + height)
    ctx.stroke()
  }
  ctx.globalAlpha = 0.52
  for (let i = 0; i < 4; i++) {
    const height = 13 + 5 * Math.sin(t * 1.8 + i)
    const x = rightStart + i * rightGap
    ctx.beginPath()
    ctx.moveTo(x, y - height)
    ctx.lineTo(x, y + height)
    ctx.stroke()
  }
  ctx.restore()

  ctx.save()
  ctx.strokeStyle = 'rgba(132,151,176,0.52)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(w * 0.52, y - 5)
  ctx.lineTo(w * 0.55, y)
  ctx.lineTo(w * 0.52, y + 5)
  ctx.stroke()
  ctx.restore()

  const pulseX = w * (0.13 + ((t * 0.12) % 1) * 0.72)
  glowDot(ctx, pulseX, y, 2.5, accent, 9)
}

function drawTone(ctx, w, h, t) {
  const accent = '#31d8f4'
  const x0 = w * 0.25
  const x1 = w * 0.84
  const rows = [
    { label: '一声', from: 0.25, to: 0.25 },
    { label: '二声', from: 0.56, to: 0.29 },
    { label: '四声', from: 0.32, to: 0.6 }
  ]

  ctx.save()
  ctx.font = '11px sans-serif'
  ctx.textAlign = 'left'
  ctx.textBaseline = 'middle'
  ctx.setLineDash([13, 8])
  ctx.lineDashOffset = -t * 18
  rows.forEach((row, rowIndex) => {
    ctx.fillStyle = 'rgba(137,157,179,0.68)'
    ctx.fillText(row.label, w * 0.08, h * (0.24 + rowIndex * 0.22))
    ctx.strokeStyle = rgba(accent, 0.82 - rowIndex * 0.08)
    ctx.lineWidth = 2.2
    ctx.beginPath()
    for (let i = 0; i <= 4; i++) {
      const p = i / 4
      const x = x0 + (x1 - x0) * p
      const base = row.from + (row.to - row.from) * p
      const y = h * (base + Math.sin(t * 1.5 + i + rowIndex) * 0.012)
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.stroke()
  })
  ctx.restore()
}

function drawNoise(ctx, w, h, t) {
  const accent = '#ff9d78'
  const centerX = w * 0.5
  const centerY = h * 0.42

  ctx.save()
  ctx.lineCap = 'round'
  for (let i = 0; i < 15; i++) {
    const x = w * 0.08 + (w * 0.84 * i) / 14
    const height = 10 + 26 * Math.abs(Math.sin(t * 2.2 + i * 1.3))
    ctx.strokeStyle = rgba(accent, 0.2 + (i % 3) * 0.05)
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(x, centerY - height)
    ctx.lineTo(x, centerY + height)
    ctx.stroke()
  }
  ctx.restore()

  for (let i = 0; i < 12; i++) {
    const angle = i * 2.1 + t * (0.18 + (i % 3) * 0.05)
    const radius = 36 + (i % 4) * 16
    const x = centerX + Math.cos(angle) * radius * 1.55
    const y = centerY + Math.sin(angle) * radius * 0.72
    glowDot(ctx, x, y, 1.6 + (i % 2), '#71859c', 5)
  }

  const pulse = 27 + Math.sin(t * 2.4) * 2.5
  ctx.save()
  ctx.fillStyle = rgba(accent, 0.17)
  ctx.strokeStyle = rgba(accent, 0.7)
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.arc(centerX, centerY, pulse, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  ctx.fillStyle = '#ffd8ca'
  ctx.font = 'bold 13px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('人声', centerX, centerY)
  ctx.restore()
}

function drawLocation(ctx, w, h, t) {
  const accent = '#5b9bff'
  const cx = w * 0.5
  const cy = h * 0.43

  ctx.save()
  ctx.strokeStyle = rgba(accent, 0.25)
  ctx.lineWidth = 1.2
  ;[33, 55].forEach((radius) => {
    ctx.beginPath()
    ctx.arc(cx, cy, radius, 0, Math.PI * 2)
    ctx.stroke()
  })

  ctx.fillStyle = 'rgba(18,49,79,0.78)'
  ctx.strokeStyle = rgba('#74bcff', 0.68)
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.ellipse(cx, cy, 24, 31, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(cx - 25, cy, 7, -Math.PI / 2, Math.PI / 2)
  ctx.arc(cx + 25, cy, 7, Math.PI / 2, Math.PI * 1.5)
  ctx.stroke()
  ctx.restore()

  const angle = t * 0.72 - 0.8
  const sx = cx + Math.cos(angle) * w * 0.36
  const sy = cy + Math.sin(angle) * h * 0.28
  glowDot(ctx, sx, sy, 5, '#ffd66f', 16)

  ctx.save()
  ctx.setLineDash([4, 5])
  ctx.strokeStyle = 'rgba(255,214,111,0.26)'
  ctx.beginPath()
  ctx.moveTo(sx, sy)
  ctx.lineTo(cx - 23, cy)
  ctx.moveTo(sx, sy)
  ctx.lineTo(cx + 23, cy)
  ctx.stroke()
  ctx.restore()
}

function drawLearning(ctx, w, h, t) {
  const accent = '#73d39e'
  const nodes = [
    [0.12, 0.25], [0.12, 0.62], [0.32, 0.43],
    [0.55, 0.23], [0.55, 0.64], [0.78, 0.43]
  ].map(([x, y]) => [w * x, h * y])
  const edges = [[0, 2], [1, 2], [2, 3], [2, 4], [3, 5], [4, 5]]

  ctx.save()
  ctx.strokeStyle = rgba(accent, 0.28)
  ctx.lineWidth = 1.3
  edges.forEach(([a, b]) => {
    ctx.beginPath()
    ctx.moveTo(nodes[a][0], nodes[a][1])
    ctx.lineTo(nodes[b][0], nodes[b][1])
    ctx.stroke()
  })
  nodes.forEach(([x, y], index) => {
    ctx.fillStyle = '#081b2f'
    ctx.strokeStyle = rgba(accent, 0.8)
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.arc(x, y, index === 2 || index === 5 ? 5 : 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.stroke()
  })
  ctx.restore()

  const path = [nodes[0], nodes[2], nodes[3], nodes[5]]
  const phase = (t * 0.34) % 1
  const scaled = phase * (path.length - 1)
  const segment = Math.min(path.length - 2, Math.floor(scaled))
  const local = scaled - segment
  const x = path[segment][0] + (path[segment + 1][0] - path[segment][0]) * local
  const y = path[segment][1] + (path[segment + 1][1] - path[segment][1]) * local
  glowDot(ctx, x, y, 3.2, accent, 12)

  ctx.save()
  const bx = w * 0.88
  const by = h * 0.43
  const br = 20 + Math.sin(t * 2) * 1.5
  ctx.fillStyle = rgba(accent, 0.14)
  ctx.strokeStyle = rgba(accent, 0.56)
  ctx.beginPath()
  ctx.arc(bx, by, br, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  ctx.fillStyle = '#a2e9bf'
  ctx.font = 'bold 11px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('学习', bx, by)
  ctx.restore()
}

function drawElectrode(ctx, w, h, t) {
  const accent = '#ffd66f'
  const start = [w * 0.1, h * 0.61]
  const end = [w * 0.9, h * 0.31]

  ctx.save()
  ctx.strokeStyle = 'rgba(91,116,142,0.42)'
  ctx.lineWidth = 7
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(start[0], start[1])
  ctx.lineTo(end[0], end[1])
  ctx.stroke()

  for (let i = 0; i < 6; i++) {
    const p = i / 5
    const x = start[0] + (end[0] - start[0]) * p
    const y = start[1] + (end[1] - start[1]) * p
    const wave = (Math.sin(t * 2.3 + i * 0.8) + 1) / 2
    ctx.strokeStyle = rgba(accent, 0.12 + wave * 0.25)
    ctx.lineWidth = 1.2
    ctx.beginPath()
    ctx.arc(x, y, 10 + wave * 14, 0, Math.PI * 2)
    ctx.stroke()
    glowDot(ctx, x, y, 3.4, accent, 10)
  }
  ctx.restore()
}

Page({
  data: {
    current: 0,
    currentLabel: '01',
    dots: [0, 1, 2, 3, 4, 5]
  },

  onReady() {
    this._limitsAlive = true
    this._initLimitCanvases()
  },

  onShow() {
    this._limitsAlive = true
    if (this._limitCanvases && this._limitCanvases.length) {
      this._startLimitLoop()
    }
  },

  onHide() {
    this._stopLimitLoop()
  },

  onUnload() {
    this._limitsAlive = false
    this._stopLimitLoop()
    this._limitCanvases = null
  },

  onCardChange(e) {
    const current = Number(e.detail.current) || 0
    this._setCurrent(current)
  },

  onDotTap(e) {
    const current = Number(e.currentTarget.dataset.index)
    if (Number.isNaN(current)) return
    this._setCurrent(current)
  },

  onNext() {
    const next = this.data.current + 1
    if (next >= TOTAL_CARDS) return
    this._setCurrent(next)
  },

  _setCurrent(current) {
    const safeCurrent = Math.max(0, Math.min(TOTAL_CARDS - 1, current))
    const displayNumber = safeCurrent + 1
    this.setData({
      current: safeCurrent,
      currentLabel: displayNumber < 10 ? `0${displayNumber}` : String(displayNumber)
    }, () => {
      this._drawLimitVisual(safeCurrent, (Date.now() - (this._limitStartMs || Date.now())) / 1000)
    })
  },

  _initLimitCanvases(retry = 0) {
    if (!this._limitsAlive) return

    this.createSelectorQuery()
      .selectAll('.limit-canvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!this._limitsAlive) return
        const items = res && res[0]
        if (!Array.isArray(items) || items.length !== TOTAL_CARDS || items.some((item) => !item.node || !item.width || !item.height)) {
          if (retry < 10) {
            setTimeout(() => this._initLimitCanvases(retry + 1), 60)
          }
          return
        }

        let dpr = 2
        try {
          dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio) || 2
        } catch (e) {
          dpr = 2
        }

        this._limitCanvases = items.map((item) => {
          const canvas = item.node
          const ctx = canvas.getContext('2d')
          canvas.width = Math.floor(item.width * dpr)
          canvas.height = Math.floor(item.height * dpr)
          ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
          return { canvas, ctx, width: item.width, height: item.height }
        })

        this._limitStartMs = Date.now()
        this._lastLimitFrameMs = 0
        for (let index = 0; index < TOTAL_CARDS; index++) {
          this._drawLimitVisual(index, index * 0.4)
        }
        this._startLimitLoop()
      })
  },

  _startLimitLoop() {
    if (!this._limitsAlive || !this._limitCanvases || this._limitRaf) return
    const driver = this._limitCanvases[0] && this._limitCanvases[0].canvas
    if (!driver) return

    const tick = () => {
      if (!this._limitsAlive || !this._limitCanvases) {
        this._limitRaf = null
        return
      }

      const now = Date.now()
      if (!this._lastLimitFrameMs || now - this._lastLimitFrameMs >= FRAME_MS) {
        this._lastLimitFrameMs = now
        this._drawLimitVisual(this.data.current, (now - this._limitStartMs) / 1000)
      }
      this._limitRaf = driver.requestAnimationFrame(tick)
    }

    this._limitRaf = driver.requestAnimationFrame(tick)
  },

  _stopLimitLoop() {
    const driver = this._limitCanvases && this._limitCanvases[0] && this._limitCanvases[0].canvas
    if (this._limitRaf && driver) {
      try {
        driver.cancelAnimationFrame(this._limitRaf)
      } catch (e) {
        // 页面销毁时忽略已经失效的动画句柄
      }
    }
    this._limitRaf = null
  },

  _drawLimitVisual(index, t) {
    const item = this._limitCanvases && this._limitCanvases[index]
    if (!item) return
    const { ctx, width, height } = item
    ctx.clearRect(0, 0, width, height)

    const drawers = [drawMusic, drawTone, drawNoise, drawLocation, drawLearning, drawElectrode]
    drawers[index](ctx, width, height, t)
  }
})
