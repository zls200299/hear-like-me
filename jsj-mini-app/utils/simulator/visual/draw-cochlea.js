const { clamp } = require('./common.js')

const COCHLEA_COLORS = [
  '#2B2E83',
  '#1E7CC2',
  '#16B9A6',
  '#35D0BA',
  '#7DC93F',
  '#C4D43A',
  '#F2C14E',
  '#F2A65A'
]

const THETA_START = 0.2 * Math.PI
const THETA_END = 4.8 * Math.PI
const SPIRAL_INNER = 0.14
const SPIRAL_OUTER = 0.76

function hexToRgb(hex) {
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16)
  ]
}

function rgbaHex(hex, alpha) {
  const [r, g, b] = hexToRgb(hex)
  return `rgba(${r},${g},${b},${alpha})`
}

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
    cx: W * 0.54,
    cy: H * 0.52,
    rxMax: W * 0.41,
    ryMax: H * 0.39
  }
}

function drawSpiralTracks(ctx, cx, cy, rxMax, ryMax, dpr) {
  traceSpiralPath(ctx, cx, cy, rxMax, ryMax)
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'

  ctx.strokeStyle = 'rgba(24, 52, 96, 0.42)'
  ctx.lineWidth = 16 * dpr
  ctx.stroke()

  traceSpiralPath(ctx, cx, cy, rxMax, ryMax)
  ctx.strokeStyle = 'rgba(30, 124, 194, 0.22)'
  ctx.lineWidth = 22 * dpr
  ctx.stroke()

  traceSpiralPath(ctx, cx, cy, rxMax, ryMax)
  ctx.strokeStyle = 'rgba(22, 185, 166, 0.55)'
  ctx.lineWidth = 3.5 * dpr
  ctx.stroke()
}

function getElectrodeColorIndex(index, count) {
  if (count <= 1) return 0
  return Math.round(index / (count - 1) * 7)
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

  const bg = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.max(W, H) * 0.55)
  bg.addColorStop(0, 'rgba(16, 28, 48, 1)')
  bg.addColorStop(1, 'rgba(10, 16, 28, 1)')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, W, H)

  drawSpiralTracks(ctx, cx, cy, rxMax, ryMax, dpr)

  const modiolusR = 5 * dpr
  const modGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, modiolusR * 2.2)
  modGrad.addColorStop(0, 'rgba(22, 185, 166, 0.35)')
  modGrad.addColorStop(1, 'rgba(22, 185, 166, 0)')
  ctx.fillStyle = modGrad
  ctx.beginPath()
  ctx.arc(cx, cy, modiolusR * 2.2, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(30, 124, 194, 0.55)'
  ctx.beginPath()
  ctx.arc(cx, cy, modiolusR, 0, Math.PI * 2)
  ctx.fill()

  const electrodes = []
  for (let i = 0; i < n; i++) {
    const t = n > 1 ? i / (n - 1) : 0
    const theta = THETA_START + t * (THETA_END - THETA_START)
    const p = spiralPoint(theta, cx, cy, rxMax, ryMax)
    const level = clamp(Number(levels[i]) || 0, 0, 1)
    const color = COCHLEA_COLORS[getElectrodeColorIndex(i, n)]
    const breathe = 1 + 0.05 * Math.sin(now / 620 + i * 0.7)
    const shimmer = active && noiseRatio > 0
      ? 1 + 0.03 * noiseRatio * Math.sin(now / 190 + i * 1.25)
      : 1

    let haloR = (6 + level * 12) * dpr * breathe * shimmer
    haloR *= 1 + spreadRatio * 0.35

    const coreR = (3 + level * 3) * dpr * breathe
    const haloAlpha = clamp((0.07 + level * 0.45) * (active ? 1 : 0.5), 0.04, 0.55)
    const coreAlpha = clamp(0.22 + level * 0.78, 0.18, 1)

    electrodes.push({
      x: p.x,
      y: p.y,
      color,
      haloR,
      coreR,
      haloAlpha,
      coreAlpha
    })
  }

  electrodes.forEach((e) => {
    const grad = ctx.createRadialGradient(e.x, e.y, 0, e.x, e.y, e.haloR)
    grad.addColorStop(0, rgbaHex(e.color, e.haloAlpha))
    grad.addColorStop(0.42, rgbaHex(e.color, e.haloAlpha * 0.42))
    grad.addColorStop(1, rgbaHex(e.color, 0))
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(e.x, e.y, e.haloR, 0, Math.PI * 2)
    ctx.fill()
  })

  electrodes.forEach((e) => {
    ctx.fillStyle = rgbaHex(e.color, e.coreAlpha)
    ctx.beginPath()
    ctx.arc(e.x, e.y, e.coreR, 0, Math.PI * 2)
    ctx.fill()
  })
}

function clearCochleaCanvas(ctx, canvas) {
  if (!ctx || !canvas) return
  ctx.clearRect(0, 0, canvas.width, canvas.height)
}

module.exports = {
  COCHLEA_COLORS,
  drawCochlea,
  clearCochleaCanvas
}
