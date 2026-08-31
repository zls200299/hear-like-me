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
  WAVEFORM_GRID: `${ICON_BASE}/waveform-grid.svg`,
  TROPHY: `${ICON_BASE}/trophy.svg`,
  BOOK_OPEN: `${ICON_BASE}/book-open.svg`,
  USER: `${ICON_BASE}/user.svg`,
  TAB_SIMULATOR: `${ICON_BASE}/tab-simulator.svg`,
  TAB_CHALLENGE: `${ICON_BASE}/tab-challenge.svg`,
  TAB_LEARN: `${ICON_BASE}/tab-learn.svg`,
  TAB_PROFILE: `${ICON_BASE}/tab-profile.svg`,
  PROFILE_MENU_ACCOUNT: `${ICON_BASE}/profile-menu-account.svg`,
  PROFILE_MENU_ABOUT: `${ICON_BASE}/profile-menu-about.svg`,
  PROFILE_MENU_GUIDE: `${ICON_BASE}/profile-menu-guide.svg`,
  PROFILE_MENU_FEEDBACK: `${ICON_BASE}/profile-menu-feedback.svg`,
  PROFILE_MENU_PRIVACY: `${ICON_BASE}/profile-menu-privacy.svg`,
  PROFILE_CHEVRON_RIGHT: `${ICON_BASE}/profile-chevron-right.svg`,
  PROFILE_HEART: `${ICON_BASE}/profile-heart.svg`,
  PROFILE_AVATAR_PLACEHOLDER: `${ICON_BASE}/profile-avatar-placeholder.svg`,
  PROFILE_WECHAT: `${ICON_BASE}/profile-wechat.svg`
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
  'waveform-grid': ICONS.WAVEFORM_GRID,
  trophy: ICONS.TROPHY,
  'book-open': ICONS.BOOK_OPEN,
  user: ICONS.USER,
  'tab-simulator': ICONS.TAB_SIMULATOR,
  'tab-challenge': ICONS.TAB_CHALLENGE,
  'tab-learn': ICONS.TAB_LEARN,
  'tab-profile': ICONS.TAB_PROFILE,
  'profile-menu-account': ICONS.PROFILE_MENU_ACCOUNT,
  'profile-menu-about': ICONS.PROFILE_MENU_ABOUT,
  'profile-menu-guide': ICONS.PROFILE_MENU_GUIDE,
  'profile-menu-feedback': ICONS.PROFILE_MENU_FEEDBACK,
  'profile-menu-privacy': ICONS.PROFILE_MENU_PRIVACY,
  'profile-chevron-right': ICONS.PROFILE_CHEVRON_RIGHT,
  'profile-heart': ICONS.PROFILE_HEART
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
