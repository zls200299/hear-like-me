const BAR_COUNT = 24

const BAR_SHAPE = [
  0.28, 0.38, 0.52, 0.70, 0.88, 1.00,
  0.90, 0.82, 0.74, 0.66, 0.58, 0.50,
  0.43, 0.37, 0.31, 0.26, 0.22, 0.18,
  0.15, 0.12, 0.10, 0.08, 0.06, 0.05
]

const MIC_LEVEL_GAIN = 5.0
const EMA_KEEP = 0.72
const EMA_NEW = 0.28
const DRAW_INTERVAL_MS = 110
const IDLE_ALPHA = 0.15

function clamp(value, lo, hi) {
  return Math.max(lo, Math.min(hi, value))
}

function computePcm16Rms(arrayBuffer) {
  if (!arrayBuffer || arrayBuffer.byteLength < 2) return 0

  const view = new DataView(arrayBuffer)
  const sampleCount = Math.floor(arrayBuffer.byteLength / 2)
  if (sampleCount <= 0) return 0

  let sumSq = 0
  for (let i = 0; i < sampleCount; i++) {
    const sample = view.getInt16(i * 2, true)
    const x = sample / 32768
    sumSq += x * x
  }

  const rms = Math.sqrt(sumSq / sampleCount)
  return clamp(rms * MIC_LEVEL_GAIN, 0, 1)
}

function smoothMicLevel(previous, current) {
  const prev = Number.isFinite(previous) ? previous : 0
  const next = Number.isFinite(current) ? current : 0
  return prev * EMA_KEEP + next * EMA_NEW
}

function fillRoundRect(ctx, x, y, width, height, radius) {
  const r = Math.min(radius, width / 2, height / 2)
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + width - r, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + r)
  ctx.lineTo(x + width, y + height)
  ctx.lineTo(x, y + height)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
  ctx.fill()
}

function drawMicLevelBars(ctx, width, height, level, options) {
  if (!ctx || width <= 0 || height <= 0) return

  const isLive = !!(options && options.isLive)
  const liveLevel = clamp(level, 0, 1)
  const minBarH = height * 0.08
  const maxBarH = height * 0.92
  const gap = Math.max(1, width * 0.012)
  const barW = (width - gap * (BAR_COUNT - 1)) / BAR_COUNT
  const radius = Math.min(barW / 2, 3)

  ctx.clearRect(0, 0, width, height)
  ctx.save()
  ctx.shadowBlur = 0

  for (let i = 0; i < BAR_COUNT; i++) {
    const shape = BAR_SHAPE[i]
    const shaped = isLive ? shape * liveLevel : shape
    const barHeight = minBarH + shaped * (maxBarH - minBarH)
    const x = i * (barW + gap)
    const y = height - barHeight

    if (isLive && liveLevel > 0.02) {
      const alpha = clamp(0.18 + shape * liveLevel * 0.82, 0.12, 1)
      const gradient = ctx.createLinearGradient(x, y, x, height)
      gradient.addColorStop(0, `rgba(47, 224, 195, ${alpha})`)
      gradient.addColorStop(1, `rgba(23, 203, 177, ${Math.max(alpha * 0.88, 0.14)})`)
      ctx.fillStyle = gradient
      if (shape * liveLevel > 0.45) {
        ctx.shadowColor = 'rgba(47, 224, 195, 0.22)'
        ctx.shadowBlur = 4
      } else {
        ctx.shadowBlur = 0
      }
    } else {
      ctx.shadowBlur = 0
      ctx.fillStyle = `rgba(47, 224, 195, ${IDLE_ALPHA})`
    }

    fillRoundRect(ctx, x, y, barW, barHeight, radius)
  }

  ctx.restore()
}

module.exports = {
  BAR_COUNT,
  MIC_LEVEL_GAIN,
  EMA_KEEP,
  EMA_NEW,
  DRAW_INTERVAL_MS,
  computePcm16Rms,
  smoothMicLevel,
  drawMicLevelBars
}
