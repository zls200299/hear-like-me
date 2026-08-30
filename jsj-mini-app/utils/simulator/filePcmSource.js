const DEFAULT_TARGET_SAMPLE_RATE = 44100
const DEFAULT_FRAME_MS = 60

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function audioBufferToMono(audioBuffer) {
  const length = audioBuffer.length
  const channelCount = audioBuffer.numberOfChannels
  const mono = new Float32Array(length)

  if (channelCount <= 1) {
    const channel = audioBuffer.getChannelData(0)
    mono.set(channel)
    return mono
  }

  for (let i = 0; i < length; i++) {
    let sum = 0
    for (let ch = 0; ch < channelCount; ch++) {
      sum += audioBuffer.getChannelData(ch)[i]
    }
    mono[i] = sum / channelCount
  }

  return mono
}

function resampleLinear(input, sourceRate, targetRate) {
  if (!input || !input.length) {
    return new Float32Array(0)
  }
  if (sourceRate === targetRate) {
    return input
  }

  const outputLength = Math.max(1, Math.round(input.length * targetRate / sourceRate))
  const output = new Float32Array(outputLength)
  const ratio = sourceRate / targetRate

  for (let i = 0; i < outputLength; i++) {
    const srcPos = i * ratio
    const index = Math.floor(srcPos)
    const frac = srcPos - index
    const s0 = input[index]
    const s1 = input[Math.min(index + 1, input.length - 1)]
    output[i] = s0 + (s1 - s0) * frac
  }

  return output
}

function floatSampleToPcm16(sample) {
  const value = clamp(sample, -1, 1)
  if (value < 0) {
    return Math.round(value * 0x8000)
  }
  return Math.round(value * 0x7fff)
}

function float32ToPcm16LE(samples) {
  const buffer = new ArrayBuffer(samples.length * 2)
  const view = new DataView(buffer)

  for (let i = 0; i < samples.length; i++) {
    view.setInt16(i * 2, floatSampleToPcm16(samples[i]), true)
  }

  return buffer
}

function decodeArrayBuffer(webCtx, arrayBuffer) {
  return new Promise((resolve, reject) => {
    webCtx.decodeAudioData(
      arrayBuffer,
      (buffer) => resolve(buffer),
      (err) => reject(err || new Error('音频解码失败'))
    )
  })
}

function requestArrayBuffer(url) {
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

function createFilePcmSource(options = {}) {
  const targetSampleRate = Number(options.targetSampleRate) > 0
    ? Number(options.targetSampleRate)
    : DEFAULT_TARGET_SAMPLE_RATE
  const frameMs = Number(options.frameMs) > 0
    ? Number(options.frameMs)
    : DEFAULT_FRAME_MS
  const frameSamples = Math.round(targetSampleRate * frameMs / 1000)
  const frameBytes = frameSamples * 2
  const loop = options.loop !== false

  let webCtx = null
  let ownsWebCtx = false
  let ready = false
  let destroyed = false
  let loadGeneration = 0
  let cursor = 0
  let monoPcm = null
  let sourceSampleRate = 0
  let channelCount = 0
  let durationMs = 0
  let loadedUrl = ''

  function ensureWebCtx() {
    if (destroyed) {
      throw new Error('file pcm source destroyed')
    }
    if (!webCtx) {
      webCtx = wx.createWebAudioContext()
      ownsWebCtx = true
    }
    return webCtx
  }

  function releaseWebCtx() {
    if (!webCtx || !ownsWebCtx) {
      webCtx = null
      ownsWebCtx = false
      return
    }

    try {
      webCtx.close()
    } catch (err) {
      // ignore close errors
    }

    webCtx = null
    ownsWebCtx = false
  }

  function readFloatFrame() {
    const frame = new Float32Array(frameSamples)

    if (!monoPcm || monoPcm.length === 0) {
      return frame
    }

    for (let i = 0; i < frameSamples; i++) {
      if (cursor >= monoPcm.length) {
        if (!loop) {
          break
        }
        cursor = 0
      }
      frame[i] = monoPcm[cursor]
      cursor += 1
    }

    if (loop && monoPcm.length > 0) {
      cursor %= monoPcm.length
    }

    return frame
  }

  async function load(url) {
    if (!url) {
      throw new Error('音频地址不能为空')
    }
    if (destroyed) {
      throw new Error('file pcm source destroyed')
    }

    const generation = ++loadGeneration
    ready = false
    cursor = 0
    monoPcm = null
    loadedUrl = url

    const ctx = ensureWebCtx()
    const arrayBuffer = await requestArrayBuffer(url)

    if (destroyed || generation !== loadGeneration) {
      return
    }

    const audioBuffer = await decodeArrayBuffer(ctx, arrayBuffer)

    if (destroyed || generation !== loadGeneration) {
      return
    }

    sourceSampleRate = audioBuffer.sampleRate
    channelCount = audioBuffer.numberOfChannels
    durationMs = Math.round(audioBuffer.duration * 1000)

    const monoAtSourceRate = audioBufferToMono(audioBuffer)
    monoPcm = resampleLinear(monoAtSourceRate, sourceSampleRate, targetSampleRate)
    cursor = 0
    ready = !destroyed && generation === loadGeneration
  }

  function readFrame() {
    if (!ready || destroyed) {
      throw new Error('file pcm source is not ready')
    }

    return float32ToPcm16LE(readFloatFrame())
  }

  function reset() {
    cursor = 0
  }

  function destroy() {
    if (destroyed) return

    destroyed = true
    ready = false
    loadGeneration += 1
    cursor = 0
    monoPcm = null
    sourceSampleRate = 0
    channelCount = 0
    durationMs = 0
    loadedUrl = ''
    releaseWebCtx()
  }

  function isReady() {
    return ready && !destroyed
  }

  function getInfo() {
    return {
      url: loadedUrl,
      sourceSampleRate,
      targetSampleRate,
      durationMs,
      frameSamples,
      frameBytes,
      channelCount,
      loop,
      cursor,
      sampleCount: monoPcm ? monoPcm.length : 0
    }
  }

  return {
    load,
    readFrame,
    reset,
    destroy,
    isReady,
    getInfo
  }
}

module.exports = {
  createFilePcmSource,
  DEFAULT_TARGET_SAMPLE_RATE,
  DEFAULT_FRAME_MS
}
