const { clamp } = require('./common.js')

const FFT_SIZE = 1024
const MIN_DB = -80
const MAX_DB = -10
const MAG_EPSILON = 1e-10

function applyHannWindow(samples, size = FFT_SIZE) {
  const windowed = new Float32Array(size)
  const denom = size > 1 ? size - 1 : 1
  for (let i = 0; i < size; i++) {
    const sample = i < samples.length ? samples[i] : 0
    const w = 0.5 * (1 - Math.cos((2 * Math.PI * i) / denom))
    windowed[i] = sample * w
  }
  return windowed
}

function fftInPlace(real, imag) {
  const n = real.length
  let j = 0

  for (let i = 0; i < n; i++) {
    if (i < j) {
      let tmp = real[i]
      real[i] = real[j]
      real[j] = tmp
      tmp = imag[i]
      imag[i] = imag[j]
      imag[j] = tmp
    }
    let m = n >> 1
    while (j >= m && m > 0) {
      j -= m
      m >>= 1
    }
    j += m
  }

  for (let size = 2; size <= n; size <<= 1) {
    const halfSize = size >> 1
    const phaseStep = (-2 * Math.PI) / size
    for (let i = 0; i < n; i += size) {
      for (let k = 0; k < halfSize; k++) {
        const theta = phaseStep * k
        const cos = Math.cos(theta)
        const sin = Math.sin(theta)
        const evenR = real[i + k]
        const evenI = imag[i + k]
        const oddR = real[i + k + halfSize]
        const oddI = imag[i + k + halfSize]
        const tR = cos * oddR - sin * oddI
        const tI = sin * oddR + cos * oddI
        real[i + k + halfSize] = evenR - tR
        imag[i + k + halfSize] = evenI - tI
        real[i + k] = evenR + tR
        imag[i + k] = evenI + tI
      }
    }
  }
}

function computePcmSpectrum(samples, sampleRate) {
  const sr = Number(sampleRate) > 0 ? Number(sampleRate) : 44100
  const windowed = applyHannWindow(samples, FFT_SIZE)
  const real = new Float32Array(windowed)
  const imag = new Float32Array(FFT_SIZE)
  fftInPlace(real, imag)

  const halfBins = FFT_SIZE / 2
  const normalizedBins = new Float32Array(halfBins)
  for (let i = 0; i < halfBins; i++) {
    const magnitude = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i])) / FFT_SIZE
    const db = 20 * Math.log10(magnitude + MAG_EPSILON)
    normalizedBins[i] = clamp((db - MIN_DB) / (MAX_DB - MIN_DB), 0, 1)
  }

  return {
    normalizedBins,
    fftSize: FFT_SIZE,
    sampleRate: sr
  }
}

module.exports = {
  FFT_SIZE,
  MIN_DB,
  MAX_DB,
  computePcmSpectrum
}
