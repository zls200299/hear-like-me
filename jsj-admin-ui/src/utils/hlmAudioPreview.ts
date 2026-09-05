import { miniPreviewUrl } from '@/utils/miniRequest'

let audioEl: HTMLAudioElement | null = null
let currentSrc = ''

/**
 * 后台试听：同一资源正在播放时再点 → 停止；否则开始（或切换）播放。
 */
export function togglePreviewByAssetId(
  assetId?: string | number | null,
  onError?: () => void
): 'playing' | 'stopped' | 'noop' {
  if (assetId == null || assetId === '') return 'noop'
  return togglePreviewUrl(miniPreviewUrl(assetId), onError)
}

export function togglePreviewUrl(
  url: string,
  onError?: () => void
): 'playing' | 'stopped' | 'noop' {
  if (!url) return 'noop'
  if (!audioEl) {
    audioEl = new Audio()
    audioEl.addEventListener('ended', () => {
      currentSrc = ''
    })
  }

  const same = currentSrc === url
  const playing = !audioEl.paused && !audioEl.ended

  if (same && playing) {
    audioEl.pause()
    audioEl.currentTime = 0
    currentSrc = ''
    return 'stopped'
  }

  currentSrc = url
  if (audioEl.src !== url) {
    audioEl.src = url
  }
  audioEl.currentTime = 0
  audioEl.play().catch(() => {
    currentSrc = ''
    onError?.()
  })
  return 'playing'
}

export function stopPreviewAudio() {
  if (!audioEl) return
  audioEl.pause()
  audioEl.currentTime = 0
  currentSrc = ''
}
