const icons = require('../../../assets/read-aloud/index.js')
const { categories, items } = require('./demo-content.js')

function formatTime(seconds) {
  return '0:' + String(Math.max(0, Math.floor(seconds))).padStart(2, '0')
}

Page({
  data: {
    icons,
    categories,
    selectedCategory: 'words',
    categoryName: categories[0].name,
    categoryCaption: categories[0].caption,
    cards: items.filter(item => item.category === 'words'),
    categoryScrollIntoView: '',
    categoryScrollable: false,
    categoryHasMoreLeft: false,
    categoryHasMoreRight: false,
    activeId: '',
    activeTitle: '',
    activeIcon: '',
    progress: 0,
    elapsedLabel: '0:00',
    durationLabel: '0:00',
    waveBars: [0, 1, 2, 3, 4]
  },

  onReady() {
    wx.nextTick(() => this.measureCategoryScroll())
  },

  onCategoryTap(event) {
    const category = categories.find(item => item.id === event.currentTarget.dataset.id)
    if (!category || category.id === this.data.selectedCategory) return
    this.stopPreview()
    this.setData({
      selectedCategory: category.id,
      categoryName: category.name,
      categoryCaption: category.caption,
      cards: items.filter(item => item.category === category.id),
      categoryScrollIntoView: 'cat-' + category.id
    }, () => {
      // 清空后再设，避免同 id 无法再次触发滚动
      setTimeout(() => {
        if (!this._unloaded) this.setData({ categoryScrollIntoView: '' })
      }, 320)
    })
  },

  onCategoryScroll(event) {
    const detail = event.detail || {}
    this.updateCategoryScrollHints(detail.scrollLeft || 0)
  },

  measureCategoryScroll() {
    if (this._unloaded) return
    const query = wx.createSelectorQuery()
    query.select('.read-aloud-categories').boundingClientRect()
    query.select('.read-aloud-categories__inner').boundingClientRect()
    query.exec((res) => {
      if (this._unloaded || !res || !res[0] || !res[1]) return
      const viewWidth = res[0].width || 0
      const innerWidth = res[1].width || 0
      this._categoryViewWidth = viewWidth
      this._categoryInnerWidth = innerWidth
      const scrollable = innerWidth > viewWidth + 2
      this.setData({
        categoryScrollable: scrollable,
        categoryHasMoreLeft: false,
        categoryHasMoreRight: scrollable
      })
    })
  },

  updateCategoryScrollHints(scrollLeft) {
    const viewWidth = this._categoryViewWidth || 0
    const innerWidth = this._categoryInnerWidth || 0
    if (!viewWidth || !innerWidth) {
      this.measureCategoryScroll()
      return
    }
    const maxScroll = Math.max(0, innerWidth - viewWidth)
    const hasMoreLeft = scrollLeft > 6
    const hasMoreRight = scrollLeft < maxScroll - 6
    if (
      hasMoreLeft === this.data.categoryHasMoreLeft
      && hasMoreRight === this.data.categoryHasMoreRight
      && (innerWidth > viewWidth + 2) === this.data.categoryScrollable
    ) {
      return
    }
    this.setData({
      categoryScrollable: innerWidth > viewWidth + 2,
      categoryHasMoreLeft: hasMoreLeft,
      categoryHasMoreRight: hasMoreRight
    })
  },

  onCardTap(event) {
    const item = this.data.cards.find(card => card.id === event.currentTarget.dataset.id)
    if (!item) return
    if (item.id === this.data.activeId) {
      this.stopPreview()
      return
    }
    this.startPreview(item)
  },

  // 仅演示单条播放、进度与切换状态；当前不创建音频播放器，也不请求音频。
  startPreview(item) {
    this.stopPreview()
    const startedAt = Date.now()
    this.setData({
      activeId: item.id,
      activeTitle: item.title,
      activeIcon: item.icon,
      durationLabel: formatTime(item.duration)
    })
    this._previewTimer = setInterval(() => {
      const elapsed = (Date.now() - startedAt) / 1000
      if (elapsed >= item.duration) {
        this.stopPreview()
        return
      }
      this.setData({
        progress: Math.min(100, elapsed / item.duration * 100),
        elapsedLabel: formatTime(elapsed)
      })
    }, 100)
  },

  stopPreview() {
    this.clearPreviewTimer()
    this.setData({
      activeId: '', activeTitle: '', activeIcon: '', progress: 0,
      elapsedLabel: '0:00', durationLabel: '0:00'
    })
  },

  clearPreviewTimer() {
    if (this._previewTimer != null) {
      clearInterval(this._previewTimer)
      this._previewTimer = null
    }
  },

  onHide() { this.stopPreview() },
  onUnload() {
    this._unloaded = true
    this.clearPreviewTimer()
  }
})
