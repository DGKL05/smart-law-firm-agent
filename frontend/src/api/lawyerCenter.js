import http from './http'

export function getLawyerProfile() {
  return http.get('/api/lawyer-center/profile')
}

export function getLawyerAppointments() {
  return http.get('/api/lawyer/appointments')
}
