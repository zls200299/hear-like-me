const { clamp, pseudoRandom } = require('./common.js')

function normalizeChannelFrame(frame, channelCount, levelScale = 1) {
  const n = channelCount || 8
  const scale = Number(levelScale) > 0 ? Number(levelScale) : 1
  const src = Array.isArray(frame) ? frame : []
  const out = []
  for (let i = 0; i < n; i++) {
    const raw = Number(src[i]) || 0
    const value = clamp(Number.isFinite(raw) ? raw / scale : 0, 0, 1)
    out.push(value)
  }
  return out
}

function getFrameAtCurrentTime(frames, opts) {
  const {
    channelCount = 8,
    fps = 20,
    durationMs = 0,
    audioSeekSec = 0,
    levelScale = 255
  } = opts || {}
  const n = channelCount || 8
  if (!frames || !frames.length) {
    return normalizeChannelFrame([], n, 1)
  }

  const durationSec = durationMs > 0 ? durationMs / 1000 : frames.length / fps
  const loopT = durationSec > 0 ? audioSeekSec % durationSec : 0
  let idx = Math.floor(loopT * fps)
  if (idx < 0) idx = 0
  if (idx >= frames.length) idx = frames.length - 1
  return normalizeChannelFrame(frames[idx], n, levelScale)
}

function buildFallbackFrame(params) {
  const {
    nChannels = 8,
    spread = 0,
    noiseLevel = 0,
    envCut = 160,
    carrier = 'noise',
    isProcessed = false,
    isOriginal = false,
    isPlaying = false
  } = params || {}

  const n = Number(nChannels) || 8
  const spreadRatio = (Number(spread) || 0) / 100
  const noiseRatio = (Number(noiseLevel) || 0) / 100
  const now = Date.now()
  const t = now / 1000
  const speed = 0.75 + (Number(envCut) || 160) / 500 * 1.35
  const frame = []

  for (let i = 0; i < n; i++) {
    let level

    if (!isPlaying) {
      // 未播放：对齐 HTML computeLevels 的 idle 正弦基线（连续、缓慢起伏）
      level = 0.10 + 0.06 * Math.sin(now / 650 + i * 0.7)
    } else {
      const center = n > 1 ? 1 - Math.abs(i - (n - 1) / 2) / ((n - 1) / 2) : 1
      level = 0.14 + 0.22 * center * (1 - spreadRatio * 0.45)

      if (carrier === 'sine') {
        level += 0.2 * Math.sin(t * speed + i * 0.72)
      } else {
        level += (pseudoRandom(i + Math.floor(t * 12)) - 0.5) * 0.14 * (1 + noiseRatio)
      }

      if (isProcessed) {
        level += 0.28 * Math.sin(t * speed * 1.5 + i * 0.95)
        level += (pseudoRandom(i * 1.7 + Math.floor(t * 20)) - 0.5) * 0.16 * (1 + noiseRatio * 2)
      } else if (isOriginal) {
        level += 0.07 * Math.sin(t * 0.55 + i * 0.48)
      }
    }

    if (spreadRatio > 0 && n > 1 && isPlaying) {
      const prev = frame[i - 1]
      const nextBase = 0.14 + 0.22 * (1 - spreadRatio * 0.45)
      if (i > 0) {
        level = level * (1 - spreadRatio * 0.25) + prev * (spreadRatio * 0.25)
      } else {
        level = level * (1 - spreadRatio * 0.1) + nextBase * (spreadRatio * 0.1)
      }
    }

    frame.push(clamp(level, 0.04, isPlaying && isProcessed ? 1 : 0.42))
  }
  return frame
}

function buildElectrodeBars(levels, opts) {
  const { isProcessed = false } = opts || {}
  const count = levels.length
  const bars = levels.map((value, index) => {
    const percent = Math.max(2, Math.round(value * 1000) / 10)
    const opacity = (0.35 + value * 0.65).toFixed(2)
    const colorIndex = count > 1
      ? Math.round(index / (count - 1) * 7)
      : 0
    return {
      channelIndex: index,
      value,
      percent,
      opacity,
      colorIndex,
      glow: isProcessed && value > 0.35
    }
  })
  return bars.reverse()
}

module.exports = {
  normalizeChannelFrame,
  getFrameAtCurrentTime,
  buildFallbackFrame,
  buildElectrodeBars
}
