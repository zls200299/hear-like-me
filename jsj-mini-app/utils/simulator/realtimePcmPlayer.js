const SAMPLE_RATE = 44100
const CHANNELS = 1
const INITIAL_BUFFER_FRAMES = 3
const MAX_PENDING_FRAMES = 5
const TARGET_BUFFER_MS = 180
const MAX_BUFFER_MS = 350
const HARD_RESET_BUFFER_MS = 500
const SCHEDULER_INTERVAL_MS = 20
const INITIAL_SCHEDULE_OFFSET_SEC = 0.04
const MIN_SCHEDULE_AHEAD_SEC = 0.01
const UNDERRUN_RECOVERY_OFFSET_SEC = 0.04
const HARD_RESET_KEEP_FRAMES = 3
const HARD_RESET_NEXT_PLAY_OFFSET_SEC = 0.06
const HARD_RESET_STOP_THRESHOLD_SEC = 0.05

const TARGET_BUFFER_SEC = TARGET_BUFFER_MS / 1000

function createRealtimePcmPlayer(options = {}) {
  const onState = typeof options.onState === 'function' ? options.onState : () => {}

  let audioCtx = null
  let pendingQueue = []
  let scheduledSources = []
  let started = false
  let nextPlayTime = 0
  let destroyed = false
  let underrunCount = 0
  let droppedFrames = 0
  let recoveryCount = 0
  let schedulerTimer = null

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

  function getBufferAheadMs() {
    if (!audioCtx || !started) return 0
    return Math.max(0, (nextPlayTime - audioCtx.currentTime) * 1000)
  }

  function getBufferedMs() {
    if (!audioCtx) return 0

    if (!started) {
      let totalSamples = 0
      for (let i = 0; i < pendingQueue.length; i++) {
        totalSamples += pendingQueue[i].sampleCount
      }
      return (totalSamples / SAMPLE_RATE) * 1000
    }

    return getBufferAheadMs()
  }

  function emitState() {
    onState({
      started,
      underruns: underrunCount,
      bufferedMs: Math.round(getBufferedMs()),
      pendingFrames: pendingQueue.length,
      droppedFrames,
      recoveryCount
    })
  }

  function trimPendingQueue() {
    let dropped = 0
    while (pendingQueue.length > MAX_PENDING_FRAMES) {
      pendingQueue.shift()
      dropped += 1
    }

    if (dropped > 0) {
      droppedFrames += dropped
      console.log(`[realtime-player] drop stale frames count=${dropped}`)
    }
  }

  function stopScheduler() {
    if (!schedulerTimer) return
    clearInterval(schedulerTimer)
    schedulerTimer = null
  }

  function startScheduler() {
    stopScheduler()
    schedulerTimer = setInterval(() => {
      tickScheduler()
    }, SCHEDULER_INTERVAL_MS)
  }

  function scheduleOneChunk(samples) {
    if (destroyed || !audioCtx || !samples || !samples.length) return false

    const now = audioCtx.currentTime
    const bufferAheadSec = nextPlayTime - now
    if (bufferAheadSec >= TARGET_BUFFER_SEC) {
      return false
    }

    if (nextPlayTime <= now) {
      underrunCount += 1
      console.log(`[realtime-player] underrun count=${underrunCount}`)
      nextPlayTime = now + UNDERRUN_RECOVERY_OFFSET_SEC
    }

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

    const startAt = Math.max(nextPlayTime, now + MIN_SCHEDULE_AHEAD_SEC)
    const endAt = startAt + audioBuffer.duration
    source.start(startAt)
    nextPlayTime = endAt

    const entry = {
      source,
      startAt,
      endAt
    }
    scheduledSources.push(entry)
    source.onended = () => {
      const index = scheduledSources.indexOf(entry)
      if (index >= 0) {
        scheduledSources.splice(index, 1)
      }
    }

    return true
  }

  function resetToRealtime(oldBufferedMs) {
    if (!audioCtx) return

    const now = audioCtx.currentTime
    const stopThreshold = now + HARD_RESET_STOP_THRESHOLD_SEC

    scheduledSources = scheduledSources.filter((entry) => {
      if (entry.startAt > stopThreshold) {
        try {
          entry.source.stop()
        } catch (err) {
          // ignore stop errors for already-started sources
        }
        return false
      }
      return true
    })

    if (pendingQueue.length > HARD_RESET_KEEP_FRAMES) {
      const dropCount = pendingQueue.length - HARD_RESET_KEEP_FRAMES
      pendingQueue.splice(0, dropCount)
      droppedFrames += dropCount
      console.log(`[realtime-player] drop stale frames count=${dropCount}`)
    }

    nextPlayTime = now + HARD_RESET_NEXT_PLAY_OFFSET_SEC
    recoveryCount += 1
    console.warn('[realtime-player] latency reset', Math.round(oldBufferedMs))
    emitState()
  }

  function tickScheduler() {
    if (destroyed || !audioCtx || !started) return

    const bufferAheadMs = getBufferAheadMs()

    if (bufferAheadMs > HARD_RESET_BUFFER_MS) {
      resetToRealtime(bufferAheadMs)
      return
    }

    if (bufferAheadMs > MAX_BUFFER_MS && pendingQueue.length > 1) {
      const dropCount = pendingQueue.length - 1
      pendingQueue.splice(0, dropCount)
      droppedFrames += dropCount
      console.log(`[realtime-player] drop stale frames count=${dropCount}`)
    }

    while (pendingQueue.length > 0) {
      if (getBufferAheadMs() >= TARGET_BUFFER_MS) {
        break
      }

      const item = pendingQueue.shift()
      const scheduled = scheduleOneChunk(item.samples)
      if (!scheduled) {
        pendingQueue.unshift(item)
        break
      }
    }

    emitState()
  }

  function startPlayback() {
    if (started || !audioCtx || pendingQueue.length < INITIAL_BUFFER_FRAMES) return

    started = true
    nextPlayTime = audioCtx.currentTime + INITIAL_SCHEDULE_OFFSET_SEC
    startScheduler()
    tickScheduler()
    emitState()
  }

  function enqueue(pcmBuffer) {
    if (destroyed || !audioCtx) return
    if (!pcmBuffer || pcmBuffer.byteLength < 2) return

    const samples = pcm16ToFloat32(pcmBuffer)
    pendingQueue.push({
      samples,
      sampleCount: samples.length
    })
    trimPendingQueue()

    if (!started) {
      if (pendingQueue.length >= INITIAL_BUFFER_FRAMES) {
        startPlayback()
      }
      emitState()
      return
    }

    emitState()
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
    stopScheduler()

    scheduledSources.forEach((entry) => {
      try {
        entry.source.stop()
      } catch (err) {
        // ignore
      }
    })
    scheduledSources = []
    pendingQueue = []
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
    getUnderrunCount: () => underrunCount,
    getDroppedFrames: () => droppedFrames,
    getRecoveryCount: () => recoveryCount
  }
}

module.exports = {
  createRealtimePcmPlayer
}
