const NEURO_RAMP = ['#2B2E83', '#1E7CC2', '#16B9A6', '#7DC93F', '#F2C14E']

function clamp(value, lo, hi) {
  return Math.max(lo, Math.min(hi, value))
}

function pseudoRandom(seed) {
  const x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453
  return x - Math.floor(x)
}

function hexToRgb(hex) {
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16)
  ]
}

function lerpColor(t) {
  const clamped = clamp(t, 0, 1)
  const seg = clamped * (NEURO_RAMP.length - 1)
  const i = Math.floor(seg)
  const f = seg - i
  const c1 = hexToRgb(NEURO_RAMP[i])
  const c2 = hexToRgb(NEURO_RAMP[Math.min(i + 1, NEURO_RAMP.length - 1)])
  const r = Math.round(c1[0] + (c2[0] - c1[0]) * f)
  const g = Math.round(c1[1] + (c2[1] - c1[1]) * f)
  const b = Math.round(c1[2] + (c2[2] - c1[2]) * f)
  return `rgb(${r},${g},${b})`
}

function rgbaFromColor(color, alpha) {
  if (!color) return `rgba(22,185,166,${alpha})`
  const match = color.match(/rgb\((\d+),(\d+),(\d+)\)/)
  if (!match) return `rgba(22,185,166,${alpha})`
  return `rgba(${match[1]},${match[2]},${match[3]},${alpha})`
}

function shiftCanvasLeft(ctx, canvas, step, bg = 'rgba(12, 18, 32, 1)') {
  const W = canvas.width
  const H = canvas.height
  if (W <= step || H <= 0) return
  const imageData = ctx.getImageData(step, 0, W - step, H)
  ctx.putImageData(imageData, 0, 0)
  ctx.fillStyle = bg
  ctx.fillRect(W - step, 0, step, H)
}

function getDpr() {
  if (wx.getWindowInfo) return wx.getWindowInfo().pixelRatio || 2
  return (wx.getSystemInfoSync().pixelRatio || 2)
}

function roundRect(ctx, x, y, w, h, r) {
  if (w <= 0 || h <= 0) {
    ctx.beginPath()
    return
  }
  const radius = Math.max(0, Math.min(r, h / 2, w / 2))
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

module.exports = {
  NEURO_RAMP,
  clamp,
  pseudoRandom,
  lerpColor,
  rgbaFromColor,
  shiftCanvasLeft,
  getDpr,
  roundRect
}
