import axios from 'axios'
import { getAccessToken } from './oauth'

const http = axios.create({
  baseURL: '',
  timeout: 15000
})

export const AI_CHAT_TIMEOUT = 90000

export function postAiChat(payload) {
  return http.post('/api/ai/chat', payload, {
    timeout: AI_CHAT_TIMEOUT
  })
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body && typeof body.code === 'number' ? body.data : body
  },
  (error) => {
    if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
      return Promise.reject(new Error('请求超时，请稍后重试'))
    }
    if (error.response?.data?.message) error.message = error.response.data.message
    return Promise.reject(error)
  }
)

export default http
