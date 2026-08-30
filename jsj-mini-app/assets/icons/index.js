/**
 * 听觉模拟 UI 图标清单（来源：asset/顶部.html）
 * 路径相对于小程序根目录，供 hlm-icon 组件与页面引用。
 */
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
  if (ICON_NAMES[name]) return ICON_NAMES[name]
  if (String(name).indexOf('/') === 0) return name
  return `${ICON_BASE}/${name}.svg`
}

module.exports = {
  ICON_BASE,
  ICONS,
  ICON_NAMES,
  resolveIconPath
}
