const { clamp, lerpColor, rgbaFromColor, shiftCanvasLeft } = require('./common.js')

function drawNeuroColumn(ctx, canvas, levels, dpr, isProcessed) {
  if (!ctx || !canvas || !levels || !levels.length) return 0

  const N = levels.length
  const W = canvas.width
  const H = canvas.height
  const step = Math.max(1, Math.round(2 * dpr))

  ctx.globalAlpha = 1
  shiftCanvasLeft(ctx, canvas, step)

  const rh = H / N
  const now = Date.now()

  for (let i = 0; i < N; i++) {
    const y0 = H - (i + 1) * rh
    const cy = y0 + rh / 2
    const col = lerpColor(i / Math.max(N - 1, 1))
    const lvl = clamp(levels[i], 0, 1)
    const level = isProcessed
      ? lvl
      : 0.1 + 0.07 * Math.sin(now / 700 + i * 0.8)

    ctx.fillStyle = 'rgba(255,255,255,0.03)'
    ctx.fillRect(W - step, Math.round(cy), step, 1)

    const pSpike = isProcessed
      ? clamp(0.1 + level * level * 1.15, 0, 0.94)
      : 0.07

    if (Math.random() < pSpike) {
      const amp = rh * 0.42 * (0.5 + 0.5 * level)
      ctx.strokeStyle = rgbaFromColor(col, 0.5 + 0.5 * level)
      ctx.lineWidth = Math.max(1, 1.2 * dpr * 0.5)
      ctx.beginPath()
      ctx.moveTo(W - step * 0.5, cy - amp)
      ctx.lineTo(W - step * 0.5, cy + amp)
      ctx.stroke()
      if (level > 0.55) {
        ctx.fillStyle = rgbaFromColor(col, 0.9)
        ctx.beginPath()
        ctx.arc(W - step * 0.5, cy, Math.max(1, 1.3 * dpr), 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }

  ctx.fillStyle = 'rgba(22,185,166,0.25)'
  ctx.fillRect(W - Math.max(1, dpr), 0, Math.max(1, dpr), H)
  return N
}

function clearNeuroCanvas(ctx, canvas) {
  if (!ctx || !canvas) return
  ctx.fillStyle = 'rgba(12, 18, 32, 1)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
}

module.exports = {
  drawNeuroColumn,
  clearNeuroCanvas
}
