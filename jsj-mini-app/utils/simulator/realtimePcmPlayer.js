const SAMPLE_RATE = 44100
const CHANNELS = 1
const INITIAL_BUFFER_FRAMES = 3
const MAX_PRESTART_QUEUE = 6
const INITIAL_SCHEDULE_OFFSET_SEC = 0.05
const MIN_SCHEDULE_AHEAD_SEC = 0.01
const UNDERRUN_RECOVERY_OFFSET_SEC = 0.04
const EXCESSIVE_BUFFER_AHEAD_MS = 600
const EXCESSIVE_WARN_INTERVAL_MS = 2000

function createRealtimePcmPlayer(options = {}) {
  const onState = typeof options.onState === 'function' ? options.onState : () => {}

  let audioCtx = null
  let queue = []
  let started = false
  let nextPlayTime = 0
  let destroyed = false
  let underrunCount = 0
  let lastExcessiveWarnAt = 0

  function pcm16ToFloat32(pcmBuffer) {
    const view = new DataView(pcmBuffer)
    const sampleCount = pcmBuffer.byteLength / 2
    const samples = new Float32Array(sampleCount)

    for (let i = 0; i < sampleCount; i++) {
      const value = view.getInt16(i * 2, true)
      samples[i] = value < 0 ? value / 32768 : value / 32767
    }

    return samples
  }

  function getBufferedMs() {
    if (!audioCtx) return 0

    if (!started) {
      let totalSamples = 0
      for (let i = 0; i < queue.length; i++) {
        totalSamples += queue[i].sampleCount
      }
      return (totalSamples / SAMPLE_RATE) * 1000
    }

    return Math.max(0, (nextPlayTime - audioCtx.currentTime) * 1000)
  }

  function emitState() {
    onState({
      started,
      underruns: underrunCount,
      bufferedMs: Math.round(getBufferedMs())
    })
  }

  function trimPrestartQueue() {
    while (!started && queue.length > MAX_PRESTART_QUEUE) {
      queue.shift()
    }
  }

  function scheduleChunk(samples) {
    if (destroyed || !audioCtx || !samples || !samples.length) return

    const audioBuffer = audioCtx.createBuffer(CHANNELS, samples.length, SAMPLE_RATE)
    const channelData = audioBuffer.getChannelData(0)
    if (channelData && typeof channelData.set === 'function') {
      channelData.set(samples)
    } else if (typeof audioBuffer.copyToChannel === 'function') {
      audioBuffer.copyToChannel(samples, 0, 0)
    }

    const source = audioCtx.createBufferSource()
    source.buffer = audioBuffer
    source.connect(audioCtx.destination)

    const now = audioCtx.currentTime

    if (!started) {
      started = true
      nextPlayTime = now + INITIAL_SCHEDULE_OFFSET_SEC
    }

    if (nextPlayTime < now) {
      underrunCount += 1
      console.log(`[realtime-player] underrun count=${underrunCount}`)
      nextPlayTime = now + UNDERRUN_RECOVERY_OFFSET_SEC
    }

    const startAt = Math.max(nextPlayTime, now + MIN_SCHEDULE_AHEAD_SEC)
    source.start(startAt)
    nextPlayTime = startAt + audioBuffer.duration

    const bufferAheadMs = (nextPlayTime - now) * 1000
    if (bufferAheadMs > EXCESSIVE_BUFFER_AHEAD_MS) {
      const nowMs = Date.now()
      if (nowMs - lastExcessiveWarnAt >= EXCESSIVE_WARN_INTERVAL_MS) {
        lastExcessiveWarnAt = nowMs
        console.warn('[realtime-player] excessive buffer ahead', Math.round(bufferAheadMs))
      }
    }

    emitState()
  }

  function flushPrestartQueue() {
    if (started || queue.length < INITIAL_BUFFER_FRAMES) return

    while (queue.length > 0) {
      const item = queue.shift()
      scheduleChunk(item.samples)
    }
  }

  function enqueue(pcmBuffer) {
    if (destroyed || !audioCtx) return
    if (!pcmBuffer || pcmBuffer.byteLength < 2) return

    const samples = pcm16ToFloat32(pcmBuffer)
    const item = {
      samples,
      sampleCount: samples.length
    }

    if (!started) {
      queue.push(item)
      trimPrestartQueue()
      flushPrestartQueue()
      emitState()
      return
    }

    scheduleChunk(samples)
  }

  async function init() {
    if (destroyed) return

    audioCtx = wx.createWebAudioContext()

    if (audioCtx && audioCtx.state === 'suspended' && typeof audioCtx.resume === 'function') {
      try {
        await audioCtx.resume()
      } catch (err) {
        console.warn('[realtime-player] resume failed', err)
      }
    }

    emitState()
  }

  function destroy() {
    if (destroyed) return

    destroyed = true
    queue = []
    started = false
    nextPlayTime = 0

    if (audioCtx) {
      try {
        audioCtx.close()
      } catch (err) {
        console.warn('[realtime-player] close failed', err)
      }
      audioCtx = null
    }

    emitState()
  }

  return {
    init,
    enqueue,
    destroy,
    isStarted: () => started,
    getUnderrunCount: () => underrunCount
  }
}

module.exports = {
  createRealtimePcmPlayer
}
