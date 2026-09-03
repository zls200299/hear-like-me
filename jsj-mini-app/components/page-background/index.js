const { backgroundUrls } = require('../../config.js')

Component({
  properties: {
    src: {
      type: String,
      value: ''
    },
    variant: {
      type: String,
      value: 'study'
    },
    tone: {
      type: String,
      value: 'standard'
    }
  },

  data: {
    resolvedSrc: backgroundUrls.study
  },

  observers: {
    'src, variant'(src, variant) {
      const nextSrc = src || backgroundUrls[variant] || backgroundUrls.study
      if (nextSrc !== this.data.resolvedSrc) {
        this.setData({ resolvedSrc: nextSrc })
      }
    }
  },

  methods: {
    onImageError() {
      this.setData({ resolvedSrc: '' })
    }
  }
})
