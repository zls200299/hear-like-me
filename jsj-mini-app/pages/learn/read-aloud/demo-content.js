// 界面演示素材。duration 仅用于播放动效计时，不代表真实音频时长。
// 接入内容接口后，用服务端分类、卡片及音频地址替换本文件。
const categories = [
  { id: 'words', name: '日常用语', caption: '从一句问候，开始听见彼此', icon: 'chat' },
  { id: 'life', name: '生活声音', caption: '发现藏在日常里的熟悉声音', icon: 'home' },
  { id: 'nature', name: '自然声音', caption: '闭上眼，听听大自然的声音', icon: 'leaf' },
  { id: 'city', name: '城市声音', caption: '听听城市里流动的节奏', icon: 'phone' },
  { id: 'animals', name: '动物声音', caption: '认识身边小动物的声音', icon: 'bird' }
]

const items = [
  { id: 'hello', category: 'words', title: '你好', subtitle: '一句温暖的问候', icon: 'hello', duration: 4 },
  { id: 'thanks', category: 'words', title: '谢谢', subtitle: '把感谢说给你听', icon: 'heart', duration: 4 },
  { id: 'morning', category: 'words', title: '早上好', subtitle: '迎接新的一天', icon: 'sun', duration: 5 },
  { id: 'goodnight', category: 'words', title: '晚安', subtitle: '让美好陪你入梦', icon: 'moon', duration: 4 },
  { id: 'please', category: 'words', title: '请', subtitle: '有礼貌地表达', icon: 'please', duration: 3 },
  { id: 'goodbye', category: 'words', title: '再见', subtitle: '期待下一次相遇', icon: 'goodbye', duration: 4 },
  { id: 'doorbell', category: 'life', title: '门铃声', subtitle: '叮咚，有人来啦', icon: 'bell', duration: 5 },
  { id: 'phone', category: 'life', title: '电话铃声', subtitle: '听见远方的呼唤', icon: 'phone', duration: 6 },
  { id: 'water', category: 'life', title: '流水声', subtitle: '水流轻轻地经过', icon: 'water', duration: 7 },
  { id: 'steps', category: 'life', title: '脚步声', subtitle: '一步一步，走近你', icon: 'steps', duration: 6 },
  { id: 'clock', category: 'life', title: '时钟声', subtitle: '滴答，时间在行走', icon: 'clock', duration: 6 },
  { id: 'keys', category: 'life', title: '钥匙声', subtitle: '熟悉的回家信号', icon: 'keys', duration: 5 },
  { id: 'bird', category: 'nature', title: '鸟鸣声', subtitle: '树梢传来的小旋律', icon: 'bird', duration: 6 },
  { id: 'rain', category: 'nature', title: '下雨声', subtitle: '雨滴轻叩这个世界', icon: 'rain', duration: 8 },
  { id: 'waves', category: 'nature', title: '海浪声', subtitle: '听大海一呼一吸', icon: 'waves', duration: 8 },
  { id: 'wind', category: 'nature', title: '风声', subtitle: '一阵风，轻轻吹过', icon: 'wind', duration: 7 },
  { id: 'thunder', category: 'nature', title: '雷声', subtitle: '天空传来的回响', icon: 'thunder', duration: 6 },
  { id: 'leaves', category: 'nature', title: '树叶声', subtitle: '沙沙，是树叶在说话', icon: 'leaves', duration: 7 },
  { id: 'city-horn', category: 'city', title: '汽车鸣笛', subtitle: '路上忽然响起的提醒', icon: 'phone', duration: 5 },
  { id: 'city-steps', category: 'city', title: '人行脚步', subtitle: '人群匆匆走过', icon: 'steps', duration: 6 },
  { id: 'city-keys', category: 'city', title: '开门声', subtitle: '钥匙转动，门开了', icon: 'keys', duration: 5 },
  { id: 'animal-bird', category: 'animals', title: '小鸟叫', subtitle: '清脆的一声问候', icon: 'bird', duration: 5 },
  { id: 'animal-rain', category: 'animals', title: '雨中虫鸣', subtitle: '潮湿夜里的细小声音', icon: 'rain', duration: 7 }
]

module.exports = { categories, items }
