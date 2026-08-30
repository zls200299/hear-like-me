const { clamp, pseudoRandom } = require('./common.js')

const VISUAL_GAIN = 1.2
const MIN_BUCKETS = 160
const MAX_BUCKETS = 240

function createWaveStrokeGradient(ctx, W) {
  const g = ctx.createLinearGradient(0, 0, W, 0)
  g.addColorStop(0, '#34E2C9')
  g.addColorStop(1, '#4E8CFF')
  return g
}

function drawWaveBackground(ctx, canvas, dpr) {
  const W = canvas.width
  const H = canvas.height
  const mid = H / 2
  const padX = 18 * dpr

  ctx.clearRect(0, 0, W, H)

  const bg = ctx.createLinearGradient(0, 0, 0, H)
  bg.addColorStop(0, '#07142a')
  bg.addColorStop(1, '#0a1425')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, W, H)

  const gridColor = '#203553'
  ctx.lineWidth = 1 * dpr
  ctx.strokeStyle = gridColor
  ctx.globalAlpha = 0.35
  ;[0.24, 0.38, 0.62, 0.76].forEach((ratio) => {
    const y = H * ratio
    ctx.beginPath()
    ctx.moveTo(padX, y)
    ctx.lineTo(W - padX, y)
    ctx.stroke()
  })
  ctx.globalAlpha = 1

  ctx.beginPath()
  ctx.moveTo(padX, mid)
  ctx.lineTo(W - padX, mid)
  ctx.stroke()

  return { W, H, mid, padX }
}

function getBucketCount(W, dpr) {
  const logicalW = W / Math.max(dpr, 1)
  return Math.min(MAX_BUCKETS, Math.max(MIN_BUCKETS, Math.round(logicalW * 0.55)))
}

function applyWaveStrokeStyle(ctx, W, dpr, isPlaying, playingKind) {
  if (isPlaying) {
    ctx.strokeStyle = playingKind === 'original'
      ? '#F0C24B'
      : createWaveStrokeGradient(ctx, W)
    ctx.shadowColor = playingKind === 'original'
      ? 'rgba(240, 194, 75, 0.35)'
      : 'rgba(52, 226, 201, 0.42)'
    ctx.shadowBlur = 7 * dpr
  } else {
    ctx.strokeStyle = 'rgba(142, 160, 187, 0.45)'
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
  }
  ctx.lineWidth = 4.5 * dpr
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
}

function drawRealPcmEnvelope(ctx, canvas, dpr, samples, opts) {
  const { isPlaying = true, playingKind = 'processed' } = opts || {}
  const { W, H, mid } = drawWaveBackground(ctx, canvas, dpr)
  const bucketCount = getBucketCount(W, dpr)
  const ampScale = H * 0.42 * VISUAL_GAIN

  applyWaveStrokeStyle(ctx, W, dpr, isPlaying, playingKind)

  ctx.beginPath()
  let started = false
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
    const y = mid - clamp((min + max) * 0.5, -1, 1) * ampScale
    if (!started) {
      ctx.moveTo(x, y)
      started = true
    } else {
      ctx.lineTo(x, y)
    }
  }
  ctx.stroke()
  ctx.shadowColor = 'transparent'
  ctx.shadowBlur = 0
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
  const n = 128
  const ampBase = isPlaying ? 0.34 : 0.12
  const speed = 2.4 + nChannels * 0.08

  applyWaveStrokeStyle(ctx, W, dpr, isPlaying, playingKind)

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
  ctx.shadowColor = 'transparent'
  ctx.shadowBlur = 0
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
    drawRealPcmEnvelope(ctx, canvas, dpr, pcmSamples, { isPlaying, playingKind })
    return
  }

  drawFallbackWave(ctx, canvas, dpr, opts)
}

module.exports = {
  VISUAL_GAIN,
  drawWave
}
