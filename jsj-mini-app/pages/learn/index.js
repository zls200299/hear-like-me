const icons = require('../../assets/learn/index.js')

Page({
  data: {
    icons,
    topics: [
      {
        id: 'cochlear-basic',
        icon: 'ear',
        title: '认识人工耳蜗',
        desc: '了解组成、工作原理与声音处理方式'
      },
      {
        id: 'how-hear',
        icon: 'chat',
        title: '声音是怎样被听见的',
        desc: '跟随声音走过耳朵与听觉神经，最终被大脑理解'
      },
      {
        id: 'limitations',
        icon: 'wave',
        title: '人工耳蜗的能力与局限',
        desc: '认识音质、声调、噪声环境和声音定位上的差异'
      },
      {
        id: 'child-hearing',
        icon: 'child',
        title: '儿童听力与成长支持',
        desc: '了解听力筛查、早期干预与家庭支持'
      }
    ]
  },

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.LEARN)
  },

  onReadAloudTap() {
    wx.navigateTo({ url: '/pages/learn/read-aloud/index' })
  },

  onTopicTap(e) {
    const id = e.currentTarget.dataset && e.currentTarget.dataset.id
    if (!id) return

    if (id === 'cochlear-basic') {
      wx.navigateTo({
        url: '/pages/learn/cochlear-basic/index'
      })
      return
    }

    if (id === 'how-hear') {
      wx.navigateTo({
        url: '/pages/learn/howHear/index'
      })
      return
    }

    if (id === 'limitations') {
      wx.navigateTo({
        url: '/pages/learn/limitations/index'
      })
      return
    }

    if (id === 'child-hearing') {
      wx.navigateTo({
        url: '/pages/learn/childHearing/index'
      })
      return
    }

    wx.showToast({
      title: '专题内容即将上线',
      icon: 'none'
    })
  }
})
