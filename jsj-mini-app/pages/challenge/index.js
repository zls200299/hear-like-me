const {
  getCurrentQuestion,
  submitAnswer
} = require('../../services/challenge.js')

const ANSWERS = [
  { value: 2, label: '2 通道' },
  { value: 4, label: '4 通道' },
  { value: 8, label: '8 通道' },
  { value: 16, label: '16 通道' }
]

Page({
  data: {
    loading: true,
    questionId: '',
    questionIndex: 1,
    questionTitle: '',
    selectedAnswer: null,
    answers: ANSWERS,
    playing: false,
    playText: '播放模拟声音',
    hasPlayed: false,
    submitting: false,
    correctCount: 0,
    totalCount: 0,
    completedCount: 0,
    audioUrl: ''
  },

  onLoad() {
    this.audio = wx.createInnerAudioContext()
    this.audio.obeyMuteSwitch = false
    this.audio.onPlay(() => {
      this.setData({ playing: true, playText: '正在播放…' })
    })
    this.audio.onEnded(() => {
      this.resetPlayState()
    })
    this.audio.onStop(() => {
      this.resetPlayState()
    })
    this.audio.onError(() => {
      this.resetPlayState()
      wx.showToast({ title: '音频播放失败', icon: 'none' })
    })

    this.loadQuestion(1)
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.CHALLENGE)
  },

  onUnload() {
    this.destroyAudio()
  },

  onHide() {
    this.stopAudio()
  },

  resetPlayState() {
    this.setData({
      playing: false,
      playText: '播放模拟声音'
    })
  },

  stopAudio() {
    if (!this.audio) return
    try {
      this.audio.stop()
    } catch (e) {
      // ignore
    }
    this.resetPlayState()
  },

  destroyAudio() {
    if (!this.audio) return
    try {
      this.audio.stop()
      this.audio.destroy()
    } catch (e) {
      // ignore
    }
    this.audio = null
  },

  async loadQuestion(index) {
    this.stopAudio()
    this.setData({
      loading: true,
      hasPlayed: false,
      selectedAnswer: null,
      submitting: false
    })

    wx.showLoading({ title: '加载题目…', mask: true })

    try {
      const question = await getCurrentQuestion(index)
      this.setData({
        loading: false,
        questionId: question.id,
        questionIndex: question.index,
        questionTitle: question.title,
        totalCount: question.total,
        audioUrl: question.audioUrl
      })
    } catch (err) {
      this.setData({ loading: false })
      wx.showToast({
        title: (err && err.message) || '加载题目失败',
        icon: 'none'
      })
    } finally {
      wx.hideLoading()
    }
  },

  onPlayTap() {
    if (this.data.loading) return

    const { audioUrl, playing } = this.data
    if (!audioUrl) {
      wx.showToast({ title: '暂无音频', icon: 'none' })
      return
    }

    if (playing) {
      this.stopAudio()
      return
    }

    this.audio.src = audioUrl
    this.audio.play()
    this.setData({ hasPlayed: true })
  },

  onAnswerTap(e) {
    if (this.data.submitting) return
    const value = Number(e.currentTarget.dataset && e.currentTarget.dataset.value)
    if (!Number.isFinite(value)) return
    this.setData({ selectedAnswer: value })
  },

  async onSubmitTap() {
    if (this.data.loading || this.data.submitting) return

    if (!this.data.hasPlayed) {
      wx.showToast({ title: '请先播放本轮声音', icon: 'none' })
      return
    }

    if (this.data.selectedAnswer == null) {
      wx.showToast({ title: '请选择答案', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    try {
      const result = await submitAnswer(this.data.questionId, this.data.selectedAnswer)
      const correctCount = this.data.correctCount + (result.correct ? 1 : 0)
      const completedCount = this.data.completedCount + 1

      this.setData({
        correctCount,
        completedCount,
        submitting: false
      })

      const title = result.correct ? '回答正确' : '回答错误'
      let content = result.tip || ''
      if (!content) {
        content = result.correct
          ? '很棒，继续下一题吧！'
          : `正确答案是 ${result.correctChannels} 通道`
      } else if (!result.correct && result.correctChannels) {
        content = `${content}\n正确答案是 ${result.correctChannels} 通道`
      }

      wx.showModal({
        title,
        content,
        showCancel: false,
        confirmText: result.hasNext ? '下一题' : '完成',
        success: (res) => {
          if (!res.confirm) return
          if (result.hasNext && result.nextIndex) {
            this.loadQuestion(result.nextIndex)
            return
          }
          wx.showToast({ title: '挑战完成', icon: 'success' })
          this.setData({
            correctCount: 0,
            completedCount: 0
          })
          this.loadQuestion(1)
        }
      })
    } catch (err) {
      this.setData({ submitting: false })
      wx.showToast({
        title: (err && err.message) || '提交失败',
        icon: 'none'
      })
    }
  }
})
