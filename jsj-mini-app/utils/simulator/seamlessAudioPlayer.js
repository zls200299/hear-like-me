const BUFFER_CACHE_MAX = 4
const TIME_POLL_MS = 100

function createSeamlessAudioPlayer() {
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
    silentStop: false,
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
    if (!state.inner) return
    state.silentStop = true
    try {
      state.inner.stop()
    } catch (e) {}
    try {
      state.inner.destroy()
    } catch (e) {}
    state.inner = null
    state.silentStop = false
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

  function bindInner(url) {
    stopInner()
    const inner = wx.createInnerAudioContext()
    inner.obeyMuteSwitch = false
    inner.src = url
    inner.loop = true

    inner.onPlay(() => {
      state.playing = true
      startTimePoll()
      if (state.callbacks.onPlay) state.callbacks.onPlay()
    })

    inner.onStop(() => {
      if (state.silentStop) return
      state.playing = false
      stopTimePoll()
      if (state.callbacks.onStop) state.callbacks.onStop()
    })

    inner.onError((err) => {
      state.playing = false
      stopTimePoll()
      if (state.callbacks.onError) state.callbacks.onError(err)
    })

    state.inner = inner
    inner.play()
  }

  async function preload(url) {
    if (!url || destroyed) return
    if (!state.useWebAudio) return
    await loadBuffer(url)
  }

  async function play(url, callbacks = {}) {
    stop({ silent: true })
    state.callbacks = callbacks || {}

    if (state.useWebAudio) {
      try {
        const buffer = await loadBuffer(url)
        startWebSource(buffer)
        state.playing = true
        startTimePoll()
        if (state.callbacks.onPlay) state.callbacks.onPlay()
        return
      } catch (err) {
        console.warn('[seamlessAudio] WebAudio 播放失败，回退 InnerAudio', err)
      }
    }

    bindInner(url)
  }

  async function switchSrc(url) {
    if (!state.playing) return false

    if (state.useWebAudio && state.webCtx) {
      try {
        const buffer = await loadBuffer(url)
        startWebSource(buffer)
        return true
      } catch (err) {
        console.warn('[seamlessAudio] WebAudio 切换失败', err)
      }
    }

    if (state.inner) {
      try {
        state.inner.stop()
      } catch (e) {}
      state.inner.src = url
      state.inner.loop = true
      state.inner.play()
      return true
    }

    return false
  }

  function stop(opts = {}) {
    const { silent = false } = opts
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
    isPlaying
  }
}

module.exports = {
  createSeamlessAudioPlayer
}
