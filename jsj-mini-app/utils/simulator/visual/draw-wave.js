const { clamp, pseudoRandom } = require('./common.js')

const VISUAL_GAIN = 1.2
const MIN_BUCKETS = 160
const MAX_BUCKETS = 240

function drawWaveBackground(ctx, canvas, dpr) {
  const W = canvas.width
  const H = canvas.height
  const mid = H / 2

  ctx.clearRect(0, 0, W, H)

  ctx.lineWidth = 1 * dpr
  ctx.strokeStyle = 'rgba(255,255,255,0.05)'
  for (let gx = 1; gx < 8; gx++) {
    const x = (W * gx) / 8
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, H)
    ctx.stroke()
  }
  for (let gy = 1; gy < 4; gy++) {
    const y = (H * gy) / 4
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(W, y)
    ctx.stroke()
  }

  ctx.strokeStyle = 'rgba(255,255,255,0.14)'
  ctx.beginPath()
  ctx.moveTo(0, mid)
  ctx.lineTo(W, mid)
  ctx.stroke()

  return { W, H, mid }
}

function getBucketCount(W, dpr) {
  const logicalW = W / Math.max(dpr, 1)
  return Math.min(MAX_BUCKETS, Math.max(MIN_BUCKETS, Math.round(logicalW * 0.55)))
}

function drawRealPcmEnvelope(ctx, canvas, dpr, samples, col) {
  const { W, H, mid } = drawWaveBackground(ctx, canvas, dpr)
  const bucketCount = getBucketCount(W, dpr)
  const ampScale = H * 0.42 * VISUAL_GAIN

  ctx.lineWidth = 1.6 * dpr
  ctx.strokeStyle = col
  ctx.lineCap = 'round'

  for (let bi = 0; bi < bucketCount; bi++) {
    const start = Math.floor(bi * samples.length / bucketCount)
    const end = Math.floor((bi + 1) * samples.length / bucketCount)
    if (end <= start) continue

    let min = 1
    let max = -1
    for (let si = start; si < end; si++) {
      const v = samples[si]
      if (v < min) min = v
      if (v > max) max = v
    }

    const x = (W * (bi + 0.5)) / bucketCount
    const yMin = mid - clamp(min, -1, 1) * ampScale
    const yMax = mid - clamp(max, -1, 1) * ampScale

    ctx.beginPath()
    ctx.moveTo(x, yMin)
    ctx.lineTo(x, yMax)
    ctx.stroke()
  }
}

function drawFallbackWave(ctx, canvas, dpr, opts) {
  const {
    isPlaying = false,
    playingKind = 'processed',
    nChannels = 8,
    carrier = 'noise',
    noiseLevel = 0
  } = opts || {}

  const { W, H, mid } = drawWaveBackground(ctx, canvas, dpr)
  const now = Date.now() / 1000

  const col = isPlaying
    ? (playingKind === 'original' ? '#F2C14E' : '#16B9A6')
    : 'rgba(108,124,163,0.55)'

  const n = 128
  const ampBase = isPlaying ? 0.34 : 0.12
  const speed = 2.4 + nChannels * 0.08

  ctx.lineWidth = 2.2 * dpr
  ctx.strokeStyle = col
  ctx.beginPath()
  for (let i = 0; i < n; i++) {
    const x = (W * i) / (n - 1)
    const phase = now * speed + i * 0.18
    let v = Math.sin(phase) * 0.45
    if (carrier === 'noise') {
      v += (pseudoRandom(i * 1.3 + Math.floor(now * 10)) - 0.5) * 0.35 * (1 + noiseLevel / 100)
    } else {
      v += Math.sin(phase * 2.1) * 0.18
    }
    v = clamp(v, -1, 1)
    const y = mid - v * H * ampBase
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.stroke()

  const t = (Date.now() / 2600) % 1
  const sx = t * W
  const si = Math.min(n - 1, Math.floor(t * (n - 1)))
  const phase = now * speed + si * 0.18
  let sv = Math.sin(phase) * 0.45
  if (carrier === 'noise') {
    sv += (pseudoRandom(si * 1.3 + Math.floor(now * 10)) - 0.5) * 0.35 * (1 + noiseLevel / 100)
  }
  const sy = mid - clamp(sv, -1, 1) * H * ampBase

  ctx.fillStyle = isPlaying ? col : 'rgba(108,124,163,0.5)'
  ctx.globalAlpha = 0.9
  ctx.beginPath()
  ctx.arc(sx, sy, 3.2 * dpr, 0, Math.PI * 2)
  ctx.fill()
  ctx.globalAlpha = 1
}

function drawWave(ctx, canvas, dpr, opts) {
  if (!ctx || !canvas) return

  const {
    useRealPcm = false,
    pcmSamples = null,
    isPlaying = false,
    playingKind = 'processed'
  } = opts || {}

  if (useRealPcm && pcmSamples && pcmSamples.length) {
    const col = playingKind === 'original' ? '#F2C14E' : '#16B9A6'
    drawRealPcmEnvelope(ctx, canvas, dpr, pcmSamples, col)
    return
  }

  drawFallbackWave(ctx, canvas, dpr, opts)
}

module.exports = {
  VISUAL_GAIN,
  drawWave
}
