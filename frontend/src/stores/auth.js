import { defineStore } from 'pinia'
import http from '../api/http'
import {
  clearOAuthSession, completeAuthorization, getAccessToken, getProfile, storeAccessToken, storeProfile
} from '../api/oauth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getAccessToken(),
    user: getProfile()
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.roles?.includes('ADMIN'),
    isLawyer: (state) => state.user?.roles?.includes('LAWYER')
  },
  actions: {
    async finishLogin(query) {
      const token = await completeAuthorization(query)
      storeAccessToken(token)
      try {
        this.user = await http.get('/api/auth/me')
        this.token = getAccessToken()
        storeProfile(this.user)
      } catch (error) {
        this.clear()
        throw error
      }
    },
    updateProfile(profile) {
      this.user = { ...this.user, ...profile }
      storeProfile(this.user)
    },
    async logout() {
      try {
        if (getAccessToken()) await http.post('/api/auth/logout')
      } catch (error) {
        if (error.response?.status !== 401) throw error
      } finally {
        this.clear()
      }
    },
    clear() {
      this.token = ''
      this.user = null
      clearOAuthSession()
    }
  }
})
