const TAB_LIST = [
  {
    pagePath: '/pages/simulator/index',
    text: '听觉模拟',
    icon: 'tab-simulator'
  },
  {
    pagePath: '/pages/challenge/index',
    text: '听音挑战',
    icon: 'tab-challenge'
  },
  {
    pagePath: '/pages/learn/index',
    text: '科普学习',
    icon: 'tab-learn'
  },
  {
    pagePath: '/pages/profile/index',
    text: '我的',
    icon: 'tab-profile'
  }
]

const TAB_INDEX = {
  SIMULATOR: 0,
  CHALLENGE: 1,
  LEARN: 2,
  PROFILE: 3
}

function setTabBarSelected(pageInstance, index) {
  if (!pageInstance || typeof pageInstance.getTabBar !== 'function') return
  const tabBar = pageInstance.getTabBar()
  if (tabBar && typeof tabBar.setData === 'function') {
    tabBar.setData({ selected: index })
  }
}

module.exports = {
  TAB_LIST,
  TAB_INDEX,
  setTabBarSelected
}
