import { defineStore } from 'pinia'
import { getToken, setToken, clearAuth, getUserInfo, setUserInfo } from '@/utils/auth'
import { phoneLogin, getCurrentUser, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUserInfo()
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || ''
  },

  actions: {
    async login(phone, code) {
      const res = await phoneLogin(phone, code)
      this.token = res.token
      this.userInfo = res
      setToken(res.token)
      setUserInfo(res)
      return res
    },

    async fetchUser() {
      const res = await getCurrentUser()
      this.userInfo = res
      setUserInfo(res)
    },

    async logout() {
      try { await logoutApi() } catch (e) { /* ignore */ }
      this.token = ''
      this.userInfo = null
      clearAuth()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }
})
