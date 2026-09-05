const CACHE_MAX = 4
const DEFAULT_WINDOW_SAMPLES = 2646

/**
 * 仅解码、不播放。用独立 WebAudioContext 缓存原声 PCM，
 * 供 InnerAudio 播放期间驱动可视化，避免与播放链路争用。
 */
function createOriginalPcmCache() {
  const cache = new Map()
  let decodeCtx = null
  let destroyed = false
  const inflight = new Map()

  function ensureDecodeCtx() {
    if (!decodeCtx && typeof wx.createWebAudioContext === 'function') {
      decodeCtx = wx.createWebAudioContext()
    }
    return decodeCtx
  }

  function trimCache() {
    while (cache.size > CACHE_MAX) {
      const firstKey = cache.keys().next().value
      cache.delete(firstKey)
    }
  }

  function bufferToMono(audioBuffer) {
    const channels = audioBuffer.numberOfChannels
    const length = audioBuffer.length
    const mono = new Float32Array(length)
    for (let i = 0; i < length; i++) {
      let sum = 0
      for (let ch = 0; ch < channels; ch++) {
        sum += audioBuffer.getChannelData(ch)[i]
      }
      mono[i] = sum / channels
    }
    return mono
  }

  function decodeArrayBuffer(arrayBuffer) {
    const webCtx = ensureDecodeCtx()
    if (!webCtx) {
      return Promise.reject(new Error('当前环境不支持音频解码'))
    }
    return new Promise((resolve, reject) => {
      webCtx.decodeAudioData(
        arrayBuffer,
        (buffer) => resolve(buffer),
        (err) => reject(err || new Error('音频解码失败'))
      )
    })
  }

  function download(url) {
    return new Promise((resolve, reject) => {
      wx.request({
        url,
        responseType: 'arraybuffer',
        success(res) {
          if (!res || !res.data) {
            reject(new Error('音频数据为空'))
            return
          }
          resolve(res.data)
        },
        fail(err) {
          reject(err || new Error('音频下载失败'))
        }
      })
    })
  }

  async function loadEntry(url) {
    const arrayBuffer = await download(url)
    if (destroyed) throw new Error('PCM_CACHE_DESTROYED')
    const audioBuffer = await decodeArrayBuffer(arrayBuffer)
    if (destroyed) throw new Error('PCM_CACHE_DESTROYED')
    const samples = bufferToMono(audioBuffer)
    return {
      url,
      samples,
      sampleRate: audioBuffer.sampleRate || 44100,
      duration: audioBuffer.duration || (samples.length / (audioBuffer.sampleRate || 44100))
    }
  }

  function has(url) {
    return !!(url && cache.has(url))
  }

  function get(url) {
    return url ? cache.get(url) || null : null
  }

  async function ensure(url) {
    if (!url || destroyed) return null
    if (cache.has(url)) {
      const hit = cache.get(url)
      cache.delete(url)
      cache.set(url, hit)
      return hit
    }

    if (inflight.has(url)) {
      return inflight.get(url)
    }

    const promise = loadEntry(url)
      .then((entry) => {
        if (destroyed) return null
        cache.set(url, entry)
        trimCache()
        return entry
      })
      .finally(() => {
        inflight.delete(url)
      })

    inflight.set(url, promise)
    return promise
  }

  function getWindow(url, currentTime, sampleCount = DEFAULT_WINDOW_SAMPLES) {
    const entry = get(url)
    if (!entry || !entry.samples || !entry.samples.length) return null

    const count = Number(sampleCount) > 0 ? Number(sampleCount) : DEFAULT_WINDOW_SAMPLES
    const sampleRate = entry.sampleRate || 44100
    const total = entry.samples.length
    const startIndex = Math.floor((Number(currentTime) || 0) * sampleRate)
    const samples = new Float32Array(count)

    for (let i = 0; i < count; i++) {
      const idx = ((startIndex + i) % total + total) % total
      samples[i] = entry.samples[idx]
    }

    return {
      samples,
      sampleRate,
      currentTime: Number(currentTime) || 0
    }
  }

  function destroy() {
    destroyed = true
    cache.clear()
    inflight.clear()
    decodeCtx = null
  }

  return {
    has,
    get,
    ensure,
    getWindow,
    destroy
  }
}

module.exports = {
  createOriginalPcmCache
}
