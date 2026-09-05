const icons = require('../../../assets/read-aloud/index.js')
const { listCategories, listItems } = require('../../../services/readAloud.js')

Page({
  data: {
    icons,
    loading: true,
    loadError: '',
    categories: [],
    selectedCategory: '',
    categoryName: '',
    cards: [],
    cardSwiperCurrent: 0,
    categoryScrollIntoView: '',
    categoryScrollable: false,
    categoryHasMoreLeft: false,
    categoryHasMoreRight: false,
    activeId: '',
    waveBars: [0, 1, 2, 3, 4]
  },

  onLoad() {
    this._unloaded = false
    this.initAudio()
    this.loadCatalog()
  },

  onReady() {
    wx.nextTick(() => this.measureCategoryScroll())
  },

  onHide() {
    this.stopPlayback()
  },

  onUnload() {
    this._unloaded = true
    this.destroyAudio()
  },

  initAudio() {
    this.audio = wx.createInnerAudioContext()
    this.audio.obeyMuteSwitch = false
    this.audio.onEnded(() => {
      this.stopPlayback()
    })
    this.audio.onError(() => {
      this.stopPlayback()
      wx.showToast({ title: '音频播放失败', icon: 'none' })
    })
  },

  destroyAudio() {
    this.stopPlayback(false)
    if (this.audio) {
      try {
        this.audio.destroy()
      } catch (e) {
        // ignore
      }
      this.audio = null
    }
  },

  async loadCatalog() {
    this.setData({ loading: true, loadError: '' })
    try {
      const categories = await listCategories()
      if (this._unloaded) return
      if (!categories.length) {
        this.setData({
          loading: false,
          categories: [],
          selectedCategory: '',
          categoryName: '',
          cards: [],
          loadError: '暂无点读分类，请先在后台配置'
        })
        return
      }
      const first = categories[0]
      this.setData({
        loading: false,
        categories,
        selectedCategory: first.id,
        categoryName: first.name
      })
      await this.loadCards(first.id)
      wx.nextTick(() => this.measureCategoryScroll())
    } catch (e) {
      if (this._unloaded) return
      this.setData({
        loading: false,
        loadError: (e && e.message) || '加载失败，请稍后重试'
      })
    }
  },

  async loadCards(categoryId) {
    if (!categoryId) {
      this.setData({ cards: [] })
      return
    }
    try {
      const cards = await listItems(categoryId)
      if (this._unloaded) return
      this.setData({ cards, cardSwiperCurrent: 0 })
    } catch (e) {
      if (this._unloaded) return
      this.setData({ cards: [], cardSwiperCurrent: 0 })
      wx.showToast({ title: (e && e.message) || '加载卡片失败', icon: 'none' })
    }
  },

  onCardSwiperChange(event) {
    const current = Number(event.detail.current) || 0
    const prev = this.data.cardSwiperCurrent
    if (current === prev) return
    const leaving = this.data.cards[prev]
    if (leaving && leaving.id === this.data.activeId) {
      this.stopPlayback()
    }
    this.setData({ cardSwiperCurrent: current })
  },
  onCategoryTap(event) {
    const id = event.currentTarget.dataset.id
    const category = this.data.categories.find(item => item.id === id)
    if (!category || category.id === this.data.selectedCategory) return
    this.stopPlayback()
    this.setData({
      selectedCategory: category.id,
      categoryName: category.name,
      categoryScrollIntoView: 'cat-' + category.id,
      cards: [],
      cardSwiperCurrent: 0
    }, () => {
      setTimeout(() => {
        if (!this._unloaded) this.setData({ categoryScrollIntoView: '' })
      }, 320)
    })
    this.loadCards(category.id)
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
      this.stopPlayback()
      return
    }
    this.startPlayback(item)
  },

  startPlayback(item) {
    if (!item.audioUrl) {
      wx.showToast({ title: '暂无模拟音频', icon: 'none' })
      return
    }
    this.stopPlayback(false)
    this.setData({
      activeId: item.id
    })
    if (!this.audio) this.initAudio()
    this.audio.src = item.audioUrl
    this.audio.play()
  },

  stopPlayback(updateUi = true) {
    if (this.audio) {
      try {
        this.audio.stop()
      } catch (e) {
        // ignore
      }
    }
    if (!updateUi || this._unloaded) return
    this.setData({
      activeId: ''
    })
  },

  onRetryTap() {
    this.loadCatalog()
  }
})
