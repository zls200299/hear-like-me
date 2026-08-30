/**
 * 听觉模拟 UI 图标清单（来源：asset/顶部.html）
 * 真机 mask 需 data URI，见 icon-svgs.js
 */
const { ICON_SVGS, svgToMaskDataUri } = require('./icon-svgs.js')

const ICON_BASE = '/assets/icons'

const ICONS = {
  MORE_DOTS: `${ICON_BASE}/more-dots.svg`,
  SCAN_TARGET: `${ICON_BASE}/scan-target.svg`,
  WARNING: `${ICON_BASE}/warning.svg`,
  MUSIC: `${ICON_BASE}/music.svg`,
  UPLOAD: `${ICON_BASE}/upload.svg`,
  MICROPHONE: `${ICON_BASE}/microphone.svg`,
  MICROPHONE_SM: `${ICON_BASE}/microphone-sm.svg`,
  HEADPHONES: `${ICON_BASE}/headphones.svg`,
  WAVEFORM: `${ICON_BASE}/waveform.svg`,
  TROPHY: `${ICON_BASE}/trophy.svg`,
  BOOK_OPEN: `${ICON_BASE}/book-open.svg`,
  USER: `${ICON_BASE}/user.svg`
}

const ICON_NAMES = Object.freeze({
  'more-dots': ICONS.MORE_DOTS,
  'scan-target': ICONS.SCAN_TARGET,
  warning: ICONS.WARNING,
  music: ICONS.MUSIC,
  upload: ICONS.UPLOAD,
  microphone: ICONS.MICROPHONE,
  'microphone-sm': ICONS.MICROPHONE_SM,
  headphones: ICONS.HEADPHONES,
  waveform: ICONS.WAVEFORM,
  trophy: ICONS.TROPHY,
  'book-open': ICONS.BOOK_OPEN,
  user: ICONS.USER
})

function resolveIconPath(name) {
  if (!name) return ''
  const key = String(name)
  if (ICON_SVGS[key]) return svgToMaskDataUri(ICON_SVGS[key])
  if (key.indexOf('/') === 0) {
    const base = key.split('/').pop().replace(/\.svg$/i, '')
    if (ICON_SVGS[base]) return svgToMaskDataUri(ICON_SVGS[base])
    return key
  }
  return `${ICON_BASE}/${key}.svg`
}

module.exports = {
  ICON_BASE,
  ICONS,
  ICON_NAMES,
  resolveIconPath
}
