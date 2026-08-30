const { clamp, lerpColor, pseudoRandom, shiftCanvasLeft } = require('./common.js')

function synthSpectrumBins(count, opts) {
  const {
    nChannels = 8,
    isProcessed = false,
    carrier = 'noise',
    noiseLevel = 0
  } = opts || {}
  const now = Date.now() / 1000
  const bins = new Array(count)
  for (let i = 0; i < count; i++) {
    const frac = i / count
    let v = 0.08 + 0.25 * Math.exp(-Math.pow((frac - 0.22) / 0.18, 2))
    v += 0.12 * Math.sin(now * (1.6 + nChannels * 0.05) + i * 0.35)
    if (carrier === 'noise') {
      v += pseudoRandom(i * 2.1 + Math.floor(now * 8)) * 0.18 * (1 + noiseLevel / 80)
    }
    if (isProcessed) {
      v *= 0.72 + 0.28 * Math.sin(now * 2.2 + i * 0.5)
    }
    bins[i] = Math.round(clamp(v, 0, 1) * 255)
  }
  return bins
}

function drawSpecColumn(ctx, canvas, dpr, opts) {
  if (!ctx || !canvas) return

  const W = canvas.width
  const H = canvas.height
  const step = Math.max(1, Math.round(2 * dpr))
  const bins = synthSpectrumBins(64, opts)
  const usable = Math.floor(bins.length * 0.7)

  ctx.globalAlpha = 1
  shiftCanvasLeft(ctx, canvas, step)

  for (let y = 0; y < H; y++) {
    const frac = 1 - y / H
    const idx = Math.min(usable - 1, Math.floor(Math.pow(frac, 2) * usable))
    const v = bins[idx] / 255
    if (v < 0.03) continue
    ctx.fillStyle = lerpColor(v)
    ctx.globalAlpha = 0.25 + 0.75 * v
    ctx.fillRect(W - step, y, step, 1)
  }
  ctx.globalAlpha = 1
}

function drawRealSpecColumn(ctx, canvas, dpr, spectrum, options) {
  if (!ctx || !canvas || !spectrum) return

  const {
    displayHiHz = 7000,
    sampleRate = 44100,
    fftSize = 1024
  } = options || {}

  const normalizedBins = spectrum.normalizedBins || spectrum
  if (!normalizedBins || !normalizedBins.length) return

  const W = canvas.width
  const H = canvas.height
  const step = Math.max(1, Math.round(2 * dpr))
  const hiHz = Math.max(1, displayHiHz)

  ctx.globalAlpha = 1
  shiftCanvasLeft(ctx, canvas, step)

  for (let y = 0; y < H; y++) {
    const freq = (1 - y / H) * hiHz
    const binIndex = Math.round((freq / sampleRate) * fftSize)
    if (binIndex < 0 || binIndex >= normalizedBins.length) continue
    const v = normalizedBins[binIndex]
    if (v < 0.03) continue
    ctx.fillStyle = lerpColor(v)
    ctx.globalAlpha = 0.25 + 0.75 * v
    ctx.fillRect(W - step, y, step, 1)
  }
  ctx.globalAlpha = 1
}

function clearSpecCanvas(ctx, canvas) {
  if (!ctx || !canvas) return
  ctx.fillStyle = 'rgba(12, 18, 32, 1)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
}

module.exports = {
  drawSpecColumn,
  drawRealSpecColumn,
  clearSpecCanvas
}
