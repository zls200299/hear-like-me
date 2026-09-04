const TIME_POLL_MS = 100

/**
 * 专用于原声循环播放。走 InnerAudioContext，避免与模拟声 WebAudio 解码/上下文争用。
 */
function createInnerLoopPlayer() {
  let generation = 0
  let inner = null
  let pollTimer = null
  let playing = false
  let destroyed = false
  let callbacks = {}

  function stopPoll() {
    if (!pollTimer) return
    clearInterval(pollTimer)
    pollTimer = null
  }

  function startPoll() {
    stopPoll()
    pollTimer = setInterval(() => {
      if (!playing || !inner) return
      const onTimeUpdate = callbacks.onTimeUpdate
      if (onTimeUpdate) onTimeUpdate(inner.currentTime || 0)
    }, TIME_POLL_MS)
  }

  function tearDownInner() {
    const ctx = inner
    inner = null
    if (!ctx) return
    try {
      ctx.stop()
    } catch (e) {}
    try {
      ctx.destroy()
    } catch (e) {}
  }

  function bind(url, gen) {
    tearDownInner()
    const audio = wx.createInnerAudioContext()
    audio.obeyMuteSwitch = false
    audio.src = url
    audio.loop = true

    audio.onPlay(() => {
      if (destroyed || inner !== audio || gen !== generation) return
      playing = true
      startPoll()
      if (callbacks.onPlay) callbacks.onPlay()
    })

    audio.onStop(() => {
      if (destroyed || inner !== audio || gen !== generation) return
      playing = false
      stopPoll()
      if (callbacks.onStop) callbacks.onStop()
    })

    audio.onError((err) => {
      if (destroyed || inner !== audio || gen !== generation) return
      playing = false
      stopPoll()
      if (callbacks.onError) callbacks.onError(err)
    })

    inner = audio
    audio.play()
  }

  function beginPlayback(nextCallbacks) {
    generation++
    const gen = generation
    playing = false
    stopPoll()
    tearDownInner()
    callbacks = nextCallbacks || {}
    return gen
  }

  function play(url, nextCallbacks = {}) {
    if (destroyed || !url) return
    const gen = beginPlayback(nextCallbacks)
    bind(url, gen)
  }

  function switchSrc(url) {
    if (destroyed || !playing || !url) return false
    const gen = ++generation
    bind(url, gen)
    return true
  }

  function stop(opts = {}) {
    const { silent = false } = opts
    generation++
    const wasPlaying = playing
    playing = false
    stopPoll()
    tearDownInner()

    if (!silent && wasPlaying && callbacks.onStop) {
      callbacks.onStop()
    }
  }

  function destroy() {
    if (destroyed) return
    destroyed = true
    generation++
    playing = false
    stopPoll()
    tearDownInner()
    callbacks = {}
  }

  function isPlaying() {
    return playing
  }

  function getCurrentTime() {
    return inner ? (inner.currentTime || 0) : 0
  }

  return {
    play,
    switchSrc,
    stop,
    destroy,
    isPlaying,
    getCurrentTime
  }
}

module.exports = {
  createInnerLoopPlayer
}
