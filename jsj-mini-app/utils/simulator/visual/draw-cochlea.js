const { clamp } = require('./common.js')

const THETA_START = 0.2 * Math.PI
const THETA_END = 4.8 * Math.PI
const SPIRAL_INNER = 0.14
const SPIRAL_OUTER = 0.76

function spiralRadiusNorm(theta) {
  const span = THETA_END - THETA_START
  if (span <= 0) return SPIRAL_INNER
  const t = clamp((theta - THETA_START) / span, 0, 1)
  return SPIRAL_INNER + t * (SPIRAL_OUTER - SPIRAL_INNER)
}

function spiralPoint(theta, cx, cy, rxMax, ryMax) {
  const r = spiralRadiusNorm(theta)
  return {
    x: cx + rxMax * r * Math.cos(theta),
    y: cy + ryMax * r * Math.sin(theta)
  }
}

function traceSpiralPath(ctx, cx, cy, rxMax, ryMax, steps = 220) {
  ctx.beginPath()
  for (let i = 0; i <= steps; i++) {
    const theta = THETA_START + (i / steps) * (THETA_END - THETA_START)
    const p = spiralPoint(theta, cx, cy, rxMax, ryMax)
    if (i === 0) ctx.moveTo(p.x, p.y)
    else ctx.lineTo(p.x, p.y)
  }
}

function getLayout(canvas) {
  const W = canvas.width
  const H = canvas.height
  return {
    W,
    H,
    cx: W * 0.52,
    cy: H * 0.56,
    rxMax: W * 0.4,
    ryMax: H * 0.38
  }
}

function drawCanvasBackground(ctx, W, H) {
  const bg = ctx.createLinearGradient(0, 0, 0, H)
  bg.addColorStop(0, '#07142a')
  bg.addColorStop(1, '#0a1425')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, W, H)
}

function drawSpiralTracks(ctx, cx, cy, rxMax, ryMax, dpr) {
  const strokeGrad = ctx.createLinearGradient(
    cx - rxMax,
    cy - ryMax,
    cx + rxMax,
    cy + ryMax
  )
  strokeGrad.addColorStop(0, '#F0C24B')
  strokeGrad.addColorStop(0.45, '#2EE1C4')
  strokeGrad.addColorStop(1, '#447CFF')

  traceSpiralPath(ctx, cx, cy, rxMax, ryMax)
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = 'rgba(59, 107, 255, 0.18)'
  ctx.lineWidth = 24 * dpr
  ctx.shadowColor = 'transparent'
  ctx.shadowBlur = 0
  ctx.stroke()

  traceSpiralPath(ctx, cx, cy, rxMax, ryMax)
  ctx.strokeStyle = strokeGrad
  ctx.lineWidth = 8 * dpr
  ctx.shadowColor = 'rgba(46, 225, 196, 0.28)'
  ctx.shadowBlur = 4.4 * dpr
  ctx.stroke()
  ctx.shadowColor = 'transparent'
  ctx.shadowBlur = 0
}

function getCochleaDotColor(index, count) {
  const mapped = count > 1 ? Math.round((index / (count - 1)) * 21) : 0
  const hue = Math.max(180, 220 - mapped * 5)
  const light = 58 - mapped * 0.3
  return `hsl(${hue}, 85%, ${light}%)`
}

function drawCochlea(ctx, canvas, dpr, levels, opts) {
  if (!ctx || !canvas || !levels || !levels.length) return

  const {
    spread = 0,
    noiseLevel = 0,
    isPlaying = false,
    isProcessed = false
  } = opts || {}

  const { W, H, cx, cy, rxMax, ryMax } = getLayout(canvas)
  const n = levels.length
  const spreadRatio = clamp(Number(spread) || 0, 0, 100) / 100
  const noiseRatio = clamp(Number(noiseLevel) || 0, 0, 100) / 100
  const now = Date.now()
  const active = isPlaying && isProcessed

  ctx.clearRect(0, 0, W, H)
  drawCanvasBackground(ctx, W, H)
  drawSpiralTracks(ctx, cx, cy, rxMax, ryMax, dpr)

  for (let i = 0; i < n; i++) {
    const t = n > 1 ? i / (n - 1) : 0
    const theta = THETA_START + t * (THETA_END - THETA_START)
    const p = spiralPoint(theta, cx, cy, rxMax, ryMax)
    const level = clamp(Number(levels[i]) || 0, 0, 1)
    const breathe = 1 + 0.05 * Math.sin(now / 620 + i * 0.7)
    const shimmer = active && noiseRatio > 0
      ? 1 + 0.03 * noiseRatio * Math.sin(now / 190 + i * 1.25)
      : 1

    const mapped = n > 1 ? Math.round((i / (n - 1)) * 21) : 0
    const radius = (3.5 + level * 1.2) * dpr * breathe
    const activeBand = mapped < 16 || level > 0.18
    const alpha = activeBand
      ? clamp(0.45 + level * 0.5, 0.45, 0.95)
      : clamp(0.18 + level * 0.25, 0.12, 0.45)

    ctx.fillStyle = getCochleaDotColor(i, n)
    ctx.globalAlpha = alpha * (active ? 1 : 0.72)
    ctx.shadowColor = active && level > 0.2
      ? 'rgba(60, 196, 219, 0.22)'
      : 'transparent'
    ctx.shadowBlur = active && level > 0.2 ? 8 * dpr * (1 + spreadRatio * 0.2) : 0
    ctx.beginPath()
    ctx.arc(p.x, p.y, radius * shimmer, 0, Math.PI * 2)
    ctx.fill()
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
    ctx.globalAlpha = 1
  }
}

function clearCochleaCanvas(ctx, canvas) {
  if (!ctx || !canvas) return
  ctx.clearRect(0, 0, canvas.width, canvas.height)
}

module.exports = {
  drawCochlea,
  clearCochleaCanvas
}
