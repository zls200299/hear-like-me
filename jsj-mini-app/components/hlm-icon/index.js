Component({
  options: {
    styleIsolation: 'apply-shared'
  },

  properties: {
    name: {
      type: String,
      value: ''
    },
    size: {
      type: Number,
      value: 48
    },
    color: {
      type: String,
      value: '#aab7ca'
    },
    customClass: {
      type: String,
      value: ''
    }
  },

  data: {
    src: ''
  },

  observers: {
    name(name) {
      const { resolveIconPath } = require('../../assets/icons/index.js')
      this.setData({
        src: resolveIconPath(name)
      })
    }
  },

  lifetimes: {
    attached() {
      const { resolveIconPath } = require('../../assets/icons/index.js')
      this.setData({
        src: resolveIconPath(this.properties.name)
      })
    }
  }
})
