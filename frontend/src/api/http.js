import axios from 'axios'

const http = axios.create({
  baseURL: '',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.code && body.code !== 200) {
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body?.data ?? body
})

export default http
