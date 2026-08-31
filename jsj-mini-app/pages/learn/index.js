Page({
  data: {},

  onShow() {
    const { setTabBarSelected, TAB_INDEX } = require('../../utils/tabBar.js')
    setTabBarSelected(this, TAB_INDEX.LEARN)
  }
})
