const BUFFER_CACHE_MAX = 4
const TIME_POLL_MS = 100

function createSeamlessAudioPlayer() {
  let playGeneration = 0

  const state = {
    useWebAudio: typeof wx.createWebAudioContext === 'function',
    webCtx: null,
    source: null,
    buffer: null,
    startedAt: 0,
    inner: null,
    bufferCache: new Map(),
    pollTimer: null,
    playing: false,
    destroyed: false,
    callbacks: {}
  }

  function trimCache() {
    while (state.bufferCache.size > BUFFER_CACHE_MAX) {
      const firstKey = state.bufferCache.keys().next().value
      state.bufferCache.delete(firstKey)
    }
  }

  function stopTimePoll() {
    if (state.pollTimer) {
      clearInterval(state.pollTimer)
      state.pollTimer = null
    }
  }

  function startTimePoll() {
    stopTimePoll()
    state.pollTimer = setInterval(() => {
      if (!state.playing) return
      const onTimeUpdate = state.callbacks.onTimeUpdate
      if (onTimeUpdate) onTimeUpdate(getCurrentTime())

      const onPcmFrame = state.callbacks.onPcmFrame
      if (onPcmFrame && state.buffer && state.source && state.webCtx) {
        const frame = getPcmWindow({ sampleCount: 2646 })
        if (frame) onPcmFrame(frame)
      }
    }, TIME_POLL_MS)
  }

  function stopWebSource() {
    if (!state.source) return
    try {
      state.source.stop()
    } catch (e) {}
    try {
      state.source.disconnect()
    } catch (e) {}
    state.source = null
  }

  function stopInner() {
    const inner = state.inner
    if (!inner) return

    // 先摘掉当前实例。微信的 onStop/onError 可能在 stop/destroy 之后异步到达，
    // 旧实例的事件不得再修改下一段音频的播放状态。
    state.inner = null
    try {
      inner.stop()
    } catch (e) {}
    try {
      inner.destroy()
    } catch (e) {}
  }

  function getCurrentTime() {
    if (state.source && state.webCtx && state.buffer) {
      const elapsed = state.webCtx.currentTime - state.startedAt
      const duration = state.buffer.duration
      return duration > 0 ? elapsed % duration : 0
    }
    if (state.inner) {
      return state.inner.currentTime || 0
    }
    return 0
  }

  function readMonoSample(buffer, index) {
    const channels = buffer.numberOfChannels
    const frameIndex = ((index % buffer.length) + buffer.length) % buffer.length
    let sum = 0
    for (let ch = 0; ch < channels; ch++) {
      sum += buffer.getChannelData(ch)[frameIndex]
    }
    return sum / channels
  }

  function getPcmWindow(options = {}) {
    if (!state.playing || !state.buffer || !state.source || !state.webCtx || state.inner) {
      return null
    }

    const sampleCount = Number(options.sampleCount) > 0 ? Number(options.sampleCount) : 2646
    const buffer = state.buffer
    const sampleRate = buffer.sampleRate
    const currentTime = getCurrentTime()
    const startIndex = Math.floor(currentTime * sampleRate)
    const samples = new Float32Array(sampleCount)

    for (let i = 0; i < sampleCount; i++) {
      samples[i] = readMonoSample(buffer, startIndex + i)
    }

    return { samples, sampleRate, currentTime }
  }

  function ensureWebCtx() {
    if (!state.webCtx) {
      state.webCtx = wx.createWebAudioContext()
    }
    return state.webCtx
  }

  function loadBuffer(url) {
    if (state.bufferCache.has(url)) {
      return Promise.resolve(state.bufferCache.get(url))
    }

    const webCtx = ensureWebCtx()
    return new Promise((resolve, reject) => {
      wx.request({
        url,
        responseType: 'arraybuffer',
        success(res) {
          if (!res || !res.data) {
            reject(new Error('音频数据为空'))
            return
          }
          webCtx.decodeAudioData(
            res.data,
            (buffer) => {
              state.bufferCache.set(url, buffer)
              trimCache()
              resolve(buffer)
            },
            (err) => reject(err || new Error('音频解码失败'))
          )
        },
        fail(err) {
          reject(err || new Error('音频下载失败'))
        }
      })
    })
  }

  function startWebSource(buffer) {
    stopWebSource()
    state.buffer = buffer
    const webCtx = ensureWebCtx()
    const source = webCtx.createBufferSource()
    source.buffer = buffer
    source.loop = true
    source.connect(webCtx.destination)
    state.startedAt = webCtx.currentTime
    source.start(0)
    state.source = source
  }

  function bindInner(url, generation) {
    stopInner()
    const inner = wx.createInnerAudioContext()
    inner.obeyMuteSwitch = false
    inner.src = url
    inner.loop = true

    inner.onPlay(() => {
      if (state.destroyed || state.inner !== inner || generation !== playGeneration) return
      state.playing = true
      startTimePoll()
      if (state.callbacks.onPlay) state.callbacks.onPlay()
    })

    inner.onStop(() => {
      if (state.destroyed || state.inner !== inner || generation !== playGeneration) return
      state.playing = false
      stopTimePoll()
      if (state.callbacks.onStop) state.callbacks.onStop()
    })

    inner.onError((err) => {
      if (state.destroyed || state.inner !== inner || generation !== playGeneration) return
      state.playing = false
      stopTimePoll()
      if (state.callbacks.onError) state.callbacks.onError(err)
    })

    state.inner = inner
    inner.play()
  }

  async function preload(url) {
    if (!url || state.destroyed) return
    if (!state.useWebAudio) return
    await loadBuffer(url)
  }

  async function play(url, callbacks = {}) {
    if (state.destroyed) return
    stop({ silent: true })
    const generation = playGeneration
    state.callbacks = callbacks || {}

    if (state.useWebAudio) {
      try {
        const buffer = await loadBuffer(url)
        if (state.destroyed || generation !== playGeneration) return
        startWebSource(buffer)
        state.playing = true
        startTimePoll()
        if (state.callbacks.onPlay) state.callbacks.onPlay()
        return
      } catch (err) {
        console.warn('[seamlessAudio] WebAudio 播放失败，回退 InnerAudio', err)
      }
    }

    if (generation !== playGeneration) return
    bindInner(url, generation)
  }

  async function switchSrc(url) {
    if (state.destroyed || !state.playing) return false
    // 每次切换独立 bump，保证「最后一次选择」获胜，旧的 decode/start 全部失效
    const generation = ++playGeneration

    if (state.useWebAudio && state.webCtx) {
      try {
        const buffer = await loadBuffer(url)
        if (state.destroyed || generation !== playGeneration || !state.playing) return false
        startWebSource(buffer)
        return true
      } catch (err) {
        if (generation !== playGeneration) return false
        console.warn('[seamlessAudio] WebAudio 切换失败', err)
      }
    }

    if (state.destroyed || generation !== playGeneration || !state.playing) return false

    if (state.inner) {
      // 不复用旧 InnerAudioContext。复用时旧 src 的 onStop 可能晚于新 src 的
      // onPlay 到达，造成“新音频刚播放就被标记停止”的竞态。
      bindInner(url, generation)
      return true
    }

    return false
  }

  function stop(opts = {}) {
    const { silent = false } = opts
    playGeneration++
    const wasPlaying = state.playing
    state.playing = false
    stopTimePoll()
    stopWebSource()
    stopInner()

    if (!silent && wasPlaying && state.callbacks.onStop) {
      state.callbacks.onStop()
    }
  }

  function destroy() {
    if (state.destroyed) return

    state.destroyed = true
    stop({ silent: true })
    state.bufferCache.clear()
    state.webCtx = null
    state.callbacks = {}
  }

  function isPlaying() {
    return state.playing
  }

  return {
    play,
    preload,
    switchSrc,
    stop,
    destroy,
    getCurrentTime,
    getPcmWindow,
    isPlaying
  }
}

module.exports = {
  createSeamlessAudioPlayer
}
