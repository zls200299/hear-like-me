const TOKEN_KEY = 'jsj_token'
const USER_KEY = 'jsj_user_info'

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || ''

export const setToken = (token) => uni.setStorageSync(TOKEN_KEY, token)

export const getUserInfo = () => {
  const raw = uni.getStorageSync(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export const setUserInfo = (info) => {
  uni.setStorageSync(USER_KEY, JSON.stringify(info))
}

export const clearAuth = () => {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export const isLoggedIn = () => !!getToken()
