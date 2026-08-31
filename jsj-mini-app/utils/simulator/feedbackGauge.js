const GAUGE_CIRCUMFERENCE = 2 * Math.PI * 68

function buildFeedbackGaugeDataUri(score) {
  const num = Number(score)
  const hasScore = score != null && score !== '' && Number.isFinite(num)
  const s = hasScore ? Math.max(0, Math.min(100, num)) : 0
  const arcLen = hasScore ? (GAUGE_CIRCUMFERENCE * s / 100).toFixed(1) : 0

  const svg = [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 180 180">',
    '<defs>',
    '<linearGradient id="scoreArc" x1="24" y1="24" x2="158" y2="158" gradientUnits="userSpaceOnUse">',
    '<stop offset="0" stop-color="#34D9EF"/>',
    '<stop offset="1" stop-color="#46EEE5"/>',
    '</linearGradient>',
    '<filter id="arcGlow" x="-60%" y="-60%" width="220%" height="220%">',
    '<feGaussianBlur stdDeviation="5" result="blur"/>',
    '<feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>',
    '</filter>',
    '</defs>',
    '<circle cx="90" cy="90" r="72" fill="none" stroke="#0c2a43" stroke-width="2" opacity=".9"/>',
    '<circle cx="90" cy="90" r="68" fill="none" stroke="#345d81" stroke-width="6" stroke-linecap="round" stroke-dasharray="2.1 4.4" opacity=".68" transform="rotate(-90 90 90)"/>',
    hasScore
      ? `<circle cx="90" cy="90" r="68" fill="none" stroke="url(#scoreArc)" stroke-width="7" stroke-linecap="round" stroke-dasharray="${arcLen} ${GAUGE_CIRCUMFERENCE.toFixed(1)}" stroke-dashoffset="-3" transform="rotate(-90 90 90)" filter="url(#arcGlow)"/>`
      : '',
    '<circle cx="90" cy="90" r="52" fill="rgba(3,18,30,.30)"/>',
    '</svg>'
  ].join('')

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

module.exports = {
  buildFeedbackGaugeDataUri
}
