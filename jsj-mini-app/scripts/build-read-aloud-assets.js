// 修改 assets/read-aloud/*.svg 后运行：node scripts/build-read-aloud-assets.js
const fs = require('fs')
const path = require('path')
const assetDir = path.join(__dirname, '../assets/read-aloud')
const icons = {}
for (const name of fs.readdirSync(assetDir).filter(file => file.endsWith('.svg')).sort()) {
  const svg = fs.readFileSync(path.join(assetDir, name), 'utf8').trim()
  icons[path.basename(name, '.svg')] = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg).replace(/'/g, '%27')
}
fs.writeFileSync(path.join(assetDir, 'index.js'), [
  '// 自动生成；源文件为本目录 SVG，运行 node scripts/build-read-aloud-assets.js 更新。',
  'module.exports = ' + JSON.stringify(icons, null, 2),
  ''
].join('\n'))
