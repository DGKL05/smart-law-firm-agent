import http from './http'

const sources = {
  users: ['用户', (row) => row.nickname && row.nickname !== row.username
    ? `${row.nickname}（${row.username}）` : row.username],
  roles: ['角色', (row) => row.roleName],
  'law-firms': ['律所', (row) => row.name],
  lawyers: ['律师', (row) => row.name],
  consultations: ['咨询', (row) => row.title],
  cases: ['案件', (row) => row.title],
  appointments: ['预约', (row) => [row.lawyerName, dateTime(row.appointmentTime)].filter(Boolean).join(' · ')],
  schedules: ['排班规则', (row) => `周${'一二三四五六日'[row.weekday - 1] || '?'} ${row.startTime}–${row.endTime}`, false],
  'schedule-blocks': ['停诊记录', (row) => [row.reason, dateTime(row.startTime)].filter(Boolean).join(' · '), false],
  notifications: ['通知', (row) => row.title],
  'notification-templates': ['通知模板', (row) => row.title],
  'audit-events': ['操作审计', (row) => row.action]
}

export const referencePageSize = 20
export const auditResourceOptions = Object.entries(sources).map(([value, [label]]) => ({ value, label }))

export function referenceSource(kind) {
  return Object.hasOwn(sources, kind) ? sources[kind] : undefined
}

export function referenceLabel(kind, row) {
  const [label, format] = sources[kind]
  return format(row) || `${label} #${row.id}`
}

export async function listReferences(kind, keyword, pageNum) {
  return http.get(`/api/admin/${kind}`, { params: { keyword: keyword || undefined, pageNum, pageSize: referencePageSize } })
}

export async function readReference(kind, id) {
  const row = await http.get(`/api/admin/${kind}/${encodeURIComponent(id)}`)
  return referenceLabel(kind, row)
}

function dateTime(value) {
  return value?.replace('T', ' ').slice(0, 16)
}
