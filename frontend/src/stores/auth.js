import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.roles?.includes('ADMIN')
  },
  actions: {
    async login(form) {
      const data = await http.post('/api/auth/login', form)
      this.token = data.token
      this.user = data
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
