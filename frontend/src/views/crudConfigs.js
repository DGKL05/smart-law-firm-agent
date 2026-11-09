const field = (prop, label, type = 'text', options = {}) => ({ prop, label, type, ...options })
const required = (prop, label, type, options) => field(prop, label, type, { required: true, ...options })
const numeric = (prop, label, options) => field(prop, label, 'number', options)
const option = (value, label) => ({ value, label })
const choice = (prop, label, options, initial) => field(prop, label, 'select', { options, initial })
const personId = required('userId', '用户 ID', 'number')
const lawyerId = required('lawyerId', '律师 ID', 'number')
const status = choice('status', '状态', [option(1, '启用'), option(0, '停用')], 1)
const readFlag = choice('readFlag', '阅读状态', [option(0, '未读'), option(1, '已读')], 0)
const time = (prop, label) => required(prop, label, 'datetime')
const title = required('title', '标题', 'text', { maxlength: 128 })
const body = required('content', '内容', 'textarea', { maxlength: 500 })
const description = field('description', '说明', 'textarea')
const remark = field('remark', '备注', 'textarea', { maxlength: 500 })
const lawyerName = field('lawyerName', '律师')
const dateCreated = field('createTime', '创建时间', 'datetime')
const roles = [option('ADMIN', '管理员'), option('USER', '普通用户'), option('LAWYER', '律师')]

const consultationFields = [title, required('question', '问题', 'textarea'), numeric('lawyerId', '律师 ID（选填）')]
const consultationStatus = choice('status', '状态', [
  option('OPEN', '待回复'), option('REPLIED', '已回复'), option('CLOSED', '已关闭')
])
const consultationColumns = [title, numeric('lawyerId', '律师 ID'), field('question', '问题'),
  field('reply', '回复'), consultationStatus, dateCreated]
const caseStatus = choice('status', '状态', [option('ACTIVE', '办理中'), option('CLOSED', '已结案')], 'ACTIVE')
const caseFields = [personId, lawyerId, required('caseNo', '案件编号'), title,
  field('caseType', '案件类型'), description, time('startTime', '开始时间'), time('endTime', '结束时间'), caseStatus]
const caseColumns = [field('caseNo', '案件编号'), title, field('caseType', '案件类型'), description,
  time('startTime', '开始时间'), time('endTime', '结束时间'), caseStatus]
const appointmentFields = [lawyerId, time('appointmentTime', '预约时间'), remark]
const appointmentColumns = [lawyerName, field('lawFirmName', '律所'), time('appointmentTime', '预约时间'),
  field('status', '状态'), remark, field('failureReason', '失败原因')]
const noticeFields = [personId, title, body, readFlag]
const noticeColumns = [title, body, readFlag, dateCreated]
const firmFields = [required('name', '律所名称'), field('provinceCode', '省份编码'), field('provinceName', '省份'),
  field('city', '城市'), field('address', '地址'), field('phone', '电话'), field('email', '邮箱'),
  field('specialties', '专长'), description, field('licenseNo', '许可证号'), status]
const lawyerFields = [required('lawFirmId', '律所 ID', 'number'), numeric('userId', '绑定账号 ID'),
  required('name', '姓名'), field('gender', '性别'), field('phone', '电话'), field('email', '邮箱'),
  field('category', '分类'), field('title', '职称'), numeric('experienceYears', '执业年限', { min: 0, max: 70 }),
  field('goodAt', '擅长方向'), description, status]
const userFields = [required('username', '用户名'), field('nickname', '昵称'), field('phone', '手机'),
  field('email', '邮箱'), status,
  field('roleCodes', '角色', 'multi', { options: roles, initial: ['USER'], source: 'roles' })]
const password = field('password', '密码', 'password', { placeholder: '留空时保留原密码' })
const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'].map((name, index) => option(index + 1, name))
const ruleFields = [
  required('weekday', '星期', 'select', { options: weekdays }),
  required('startTime', '开始时间', 'time'), required('endTime', '结束时间', 'time'),
  choice('enabled', '状态', [option(1, '开放'), option(0, '关闭')], 1)
]
const blockFields = [time('startTime', '开始时间'), time('endTime', '结束时间'), required('reason', '停诊原因')]
const cancel = {
  label: '取消预约', suffix: '/cancel', danger: true,
  when: (row) => row.status === '已预约' || row.status === '预约处理中'
}
const close = { label: '关闭咨询', suffix: '/close', when: (row) => row.status !== 'CLOSED' }
const read = { label: '标记已读', suffix: '/read', when: (row) => row.readFlag === 0 }
const canRemoveAppointment = (row) => ['已取消', '预约失败'].includes(row.status)

function list(title, endpoint, columns, options) {
  return { title, endpoint, columns, ...options }
}

function crud(title, endpoint, fields, options) {
  return list(title, endpoint, fields, { createFields: fields, updateFields: fields, remove: true, ...options })
}

export const userConfigs = {
  consultations: crud('我的咨询', '/api/me/consultations', consultationFields, {
    columns: consultationColumns, createLabel: '发起咨询',
    canEdit: (row) => row.status === 'OPEN', canRemove: (row) => row.status === 'OPEN'
  }),
  cases: list('我的案件', '/api/me/cases', caseColumns),
  appointments: list('我的预约', '/api/me/appointments', appointmentColumns, {
    updateFields: [remark], remove: true, canRemove: canRemoveAppointment,
    canEdit: (row) => row.status === '已预约', extraActions: [cancel],
    description: '在律师详情页选取开放时段预约，在此查看进度、修改备注或取消预约。'
  }),
  notifications: list('消息通知', '/api/me/notifications', noticeColumns, { remove: true, extraActions: [read] })
}

export const lawyerConfigs = {
  appointments: list('预约管理', '/api/lawyer/appointments', appointmentColumns, {
    extraActions: [cancel], search: false
  }),
  consultations: list('咨询回复', '/api/lawyer/consultations', consultationColumns, {
    extraActions: [{
      label: '回复咨询', suffix: '/reply', when: (row) => row.status !== 'CLOSED',
      input: { prop: 'reply', label: '回复内容' }
    }]
  }),
  schedules: crud('每周开放时段', '/api/lawyer/schedules', ruleFields, {
    description: '按星期设置每周可预约的开始和结束时间。', search: false
  }),
  scheduleBlocks: crud('临时停诊', '/api/lawyer/schedule-blocks', blockFields, {
    description: '临时停诊覆盖每周排班，可设置请假或临时外出时段。', search: false
  })
}

export const adminConfigs = {
  users: crud('用户管理', '/api/admin/users', userFields, {
    columns: [field('username', '用户名'), field('nickname', '昵称'), field('phone', '手机'),
      field('email', '邮箱'), field('roles', '角色', 'multi', { options: roles }), status],
    createFields: [...userFields, { ...password, required: true, placeholder: '请输入初始密码' }],
    updateFields: [...userFields, password]
  }),
  roles: crud('角色管理', '/api/admin/roles', [required('roleCode', '角色编码'), required('roleName', '角色名称')]),
  'law-firms': crud('律所管理', '/api/admin/law-firms', firmFields, {
    columns: [field('name', '律所'), field('city', '城市'), field('address', '地址'), field('licenseNo', '许可证号'), status],
    asset: { suffix: '/photo', prop: 'photoUrl', label: '上传照片' }
  }),
  lawyers: crud('律师管理', '/api/admin/lawyers', lawyerFields, {
    columns: [field('name', '律师'), field('lawFirmName', '律所'), numeric('userId', '账号 ID'),
      field('category', '分类'), field('title', '职称'), status],
    asset: { suffix: '/avatar', prop: 'avatarUrl', label: '上传头像' }
  }),
  consultations: crud('咨询管理', '/api/admin/consultations', [personId, ...consultationFields], {
    columns: [personId, ...consultationColumns], extraActions: [close], canEdit: (row) => row.status === 'OPEN'
  }),
  cases: crud('案件管理', '/api/admin/cases', caseFields, { columns: [personId, lawyerId, ...caseColumns] }),
  appointments: crud('预约管理', '/api/admin/appointments', [personId, ...appointmentFields], {
    columns: [personId, ...appointmentColumns], updateFields: [remark],
    canRemove: canRemoveAppointment, canEdit: (row) => row.status === '已预约', extraActions: [cancel]
  }),
  schedules: crud('排班规则', '/api/admin/schedules', [lawyerId, ...ruleFields], { search: false }),
  'schedule-blocks': crud('停诊管理', '/api/admin/schedule-blocks', [lawyerId, ...blockFields], { search: false }),
  notifications: crud('通知管理', '/api/admin/notifications', noticeFields, { columns: [personId, ...noticeColumns] }),
  'notification-templates': crud('通知模板', '/api/admin/notification-templates', [
    required('code', '模板编码'), title, body
  ]),
  'audit-events': crud('操作审计', '/api/admin/audit-events', [
    required('action', '操作'), field('targetType', '资源类型'), field('targetId', '资源 ID'),
    field('detail', '说明', 'textarea')
  ], {
    columns: [numeric('adminUserId', '操作员'), field('action', '操作'), field('targetType', '资源类型'),
      field('targetId', '资源 ID'), field('detail', '说明'), choice('source', '来源',
        [option('GATEWAY', '系统'), option('MANUAL', '人工')]), dateCreated],
    updateFields: [field('detail', '说明', 'textarea')],
    canEdit: (row) => row.source === 'MANUAL', canRemove: (row) => row.source === 'MANUAL'
  })
}
