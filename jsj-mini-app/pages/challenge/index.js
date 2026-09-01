const ANSWERS = [
  { value: 2, label: '2 通道' },
  { value: 4, label: '4 通道' },
  { value: 8, label: '8 通道' },
  { value: 16, label: '16 通道' }
]

Page({
  data: {
    questionIndex: 1,
    selectedAnswer: 4,
    answers: ANSWERS,
    playing: false,
    playText: '播放模拟声音',
    correctCount: 3,
    totalCount: 5,
    completedCount: 5
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.CHALLENGE)
  },

  onPlayTap() {
    const playing = !this.data.playing
    this.setData({
      playing,
      playText: playing ? '正在播放…' : '播放模拟声音'
    })
  },

  onAnswerTap(e) {
    const value = Number(e.currentTarget.dataset && e.currentTarget.dataset.value)
    if (!Number.isFinite(value)) return
    this.setData({ selectedAnswer: value })
  }
})
