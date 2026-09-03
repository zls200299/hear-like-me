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
  PROFILE_WECHAT: `${ICON_BASE}/profile-wechat.svg`,
  CHALLENGE_PLAY: `${ICON_BASE}/challenge-play.svg`,
  CHALLENGE_LISTEN: `${ICON_BASE}/challenge-listen.svg`,
  CHALLENGE_CHECK: `${ICON_BASE}/challenge-check.svg`,
  CHALLENGE_SCORE_TARGET: `${ICON_BASE}/challenge-score-target.svg`,
  CHALLENGE_SCORE_CHART: `${ICON_BASE}/challenge-score-chart.svg`,
  CHALLENGE_INFO: `${ICON_BASE}/challenge-info.svg`,
  LEARN_WARNING: `${ICON_BASE}/learn-warning.svg`,
  LEARN_TOPIC_COCHLEAR: `${ICON_BASE}/learn-topic-cochlear.svg`,
  LEARN_TOPIC_HEAR: `${ICON_BASE}/learn-topic-hear.svg`,
  LEARN_TOPIC_LIMIT: `${ICON_BASE}/learn-topic-limit.svg`,
  LEARN_TOPIC_CHILD: `${ICON_BASE}/learn-topic-child.svg`,
  LEARN_ENTER: `${ICON_BASE}/learn-enter.svg`,
  LEARN_PILL_COCHLEAR: `${ICON_BASE}/learn-pill-cochlear.svg`,
  LEARN_PILL_WAVE: `${ICON_BASE}/learn-pill-wave.svg`,
  LEARN_PILL_WARN: `${ICON_BASE}/learn-pill-warn.svg`,
  LEARN_PILL_USER: `${ICON_BASE}/learn-pill-user.svg`,
  LEARN_DETAIL_INTRO: `${ICON_BASE}/learn-detail-intro.svg`,
  LEARN_DETAIL_STEP_BAND: `${ICON_BASE}/learn-detail-step-band.svg`,
  LEARN_DETAIL_STEP_ENVELOPE: `${ICON_BASE}/learn-detail-step-envelope.svg`,
  LEARN_DETAIL_STEP_SYNTH: `${ICON_BASE}/learn-detail-step-synth.svg`,
  LEARN_DETAIL_STEP_ELECTRODE: `${ICON_BASE}/learn-detail-step-electrode.svg`,
  LEARN_DETAIL_ARROW: `${ICON_BASE}/learn-detail-arrow.svg`,
  LEARN_DETAIL_COMPARE_NORMAL: `${ICON_BASE}/learn-detail-compare-normal.svg`,
  LEARN_DETAIL_COMPARE_CI: `${ICON_BASE}/learn-detail-compare-ci.svg`,
  LEARN_DETAIL_HERO: `${ICON_BASE}/learn-detail-hero.svg`,
  LEARN_DETAIL_COCHLEA_MAP: `${ICON_BASE}/learn-detail-cochlea-map.svg`,
  LEARN_HEAR_HERO: `${ICON_BASE}/learn-hear-hero.svg`,
  LEARN_HEAR_AIR: `${ICON_BASE}/learn-hear-air.svg`,
  LEARN_HEAR_PROCESSOR: `${ICON_BASE}/learn-hear-processor.svg`,
  LEARN_HEAR_ELECTRODE: `${ICON_BASE}/learn-hear-electrode.svg`,
  LEARN_HEAR_NERVE: `${ICON_BASE}/learn-hear-nerve.svg`,
  LEARN_HEAR_BRAIN: `${ICON_BASE}/learn-hear-brain.svg`,
  LEARN_HEAR_WAVE_ORIGINAL: `${ICON_BASE}/learn-hear-wave-original.svg`,
  LEARN_HEAR_WAVE_PROCESSED: `${ICON_BASE}/learn-hear-wave-processed.svg`,
  LEARN_HEAR_STIMULATION: `${ICON_BASE}/learn-hear-stimulation.svg`,
  LEARN_HEAR_TIP: `${ICON_BASE}/learn-hear-tip.svg`
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
  'profile-heart': ICONS.PROFILE_HEART,
  'challenge-play': ICONS.CHALLENGE_PLAY,
  'challenge-listen': ICONS.CHALLENGE_LISTEN,
  'challenge-check': ICONS.CHALLENGE_CHECK,
  'challenge-score-target': ICONS.CHALLENGE_SCORE_TARGET,
  'challenge-score-chart': ICONS.CHALLENGE_SCORE_CHART,
  'challenge-info': ICONS.CHALLENGE_INFO,
  'learn-warning': ICONS.LEARN_WARNING,
  'learn-topic-cochlear': ICONS.LEARN_TOPIC_COCHLEAR,
  'learn-topic-hear': ICONS.LEARN_TOPIC_HEAR,
  'learn-topic-limit': ICONS.LEARN_TOPIC_LIMIT,
  'learn-topic-child': ICONS.LEARN_TOPIC_CHILD,
  'learn-enter': ICONS.LEARN_ENTER,
  'learn-pill-cochlear': ICONS.LEARN_PILL_COCHLEAR,
  'learn-pill-wave': ICONS.LEARN_PILL_WAVE,
  'learn-pill-warn': ICONS.LEARN_PILL_WARN,
  'learn-pill-user': ICONS.LEARN_PILL_USER,
  'learn-detail-intro': ICONS.LEARN_DETAIL_INTRO,
  'learn-detail-step-band': ICONS.LEARN_DETAIL_STEP_BAND,
  'learn-detail-step-envelope': ICONS.LEARN_DETAIL_STEP_ENVELOPE,
  'learn-detail-step-synth': ICONS.LEARN_DETAIL_STEP_SYNTH,
  'learn-detail-step-electrode': ICONS.LEARN_DETAIL_STEP_ELECTRODE,
  'learn-detail-arrow': ICONS.LEARN_DETAIL_ARROW,
  'learn-detail-compare-normal': ICONS.LEARN_DETAIL_COMPARE_NORMAL,
  'learn-detail-compare-ci': ICONS.LEARN_DETAIL_COMPARE_CI,
  'learn-hear-air': ICONS.LEARN_HEAR_AIR,
  'learn-hear-processor': ICONS.LEARN_HEAR_PROCESSOR,
  'learn-hear-electrode': ICONS.LEARN_HEAR_ELECTRODE,
  'learn-hear-nerve': ICONS.LEARN_HEAR_NERVE,
  'learn-hear-brain': ICONS.LEARN_HEAR_BRAIN,
  'learn-hear-wave-original': ICONS.LEARN_HEAR_WAVE_ORIGINAL,
  'learn-hear-wave-processed': ICONS.LEARN_HEAR_WAVE_PROCESSED,
  'learn-hear-stimulation': ICONS.LEARN_HEAR_STIMULATION,
  'learn-hear-tip': ICONS.LEARN_HEAR_TIP
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
