// 修改 assets/learn/*.svg 后运行：node scripts/build-learn-assets.js
// 微信 WXSS 背景图使用 data URI，保留多色 SVG，不依赖网络资源。
const fs = require('fs')
const path = require('path')
const assetDir = path.join(__dirname, '../assets/learn')
const icons = {}
for (const filename of fs.readdirSync(assetDir).filter(name => name.endsWith('.svg')).sort()) {
  const svg = fs.readFileSync(path.join(assetDir, filename), 'utf8').trim()
  icons[path.basename(filename, '.svg')] = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg).replace(/'/g, '%27')
}
fs.writeFileSync(path.join(assetDir, 'index.js'), '// 自动生成；修改本目录 SVG 后运行 node scripts/build-learn-assets.js 更新。\nmodule.exports = ' + JSON.stringify(icons, null, 2) + '\n')
