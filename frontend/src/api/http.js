import axios from 'axios'

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
  const token = localStorage.getItem('token')
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
    return body?.data ?? body
  },
  (error) => {
    if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
      return Promise.reject(new Error('智能法律助理响应时间较长，请稍后重试或简化问题后再发送'))
    }
    return Promise.reject(error)
  }
)

export default http
