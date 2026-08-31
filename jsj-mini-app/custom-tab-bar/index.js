const { TAB_LIST } = require('../utils/tabBar.js')

Component({
  data: {
    selected: 0,
    color: '#6f809c',
    selectedColor: '#29dbc7',
    list: TAB_LIST
  },

  methods: {
    onSwitchTab(e) {
      const dataset = e.currentTarget.dataset || {}
      const index = Number(dataset.index)
      const path = dataset.path
      if (!path || !Number.isFinite(index)) return
      if (index === this.data.selected) return

      wx.switchTab({ url: path })
      this.setData({ selected: index })
    }
  }
})
