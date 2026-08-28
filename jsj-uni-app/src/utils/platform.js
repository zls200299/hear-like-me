export const getClientType = () => {
  // #ifdef APP-PLUS
  return 'uni-app'
  // #endif
  // #ifdef H5
  return 'uni-h5'
  // #endif
  // #ifdef MP-WEIXIN
  return 'mini-app'
  // #endif
  // eslint-disable-next-line no-unreachable
  return 'unknown'
}

export const isApp = () => {
  // #ifdef APP-PLUS
  return true
  // #endif
  return false
}

export const isH5 = () => {
  // #ifdef H5
  return true
  // #endif
  return false
}
