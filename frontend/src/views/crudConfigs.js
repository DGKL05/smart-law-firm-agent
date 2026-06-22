const commonFields = {
  consultations: [
    ['title', '标题'], ['lawFirmId', '律所ID', 'number'], ['lawyerId', '律师ID', 'number'], ['question', '问题'], ['reply', '回复'], ['status', '状态']
  ],
  cases: [
    ['caseNo', '案件编号'], ['title', '标题'], ['caseType', '案件类型'], ['description', '描述'], ['status', '状态']
  ],
  appointments: [
    ['lawFirmId', '律所ID', 'number'], ['lawyerId', '律师ID', 'number'], ['lawFirmName', '律所名称'], ['lawyerName', '律师名称'], ['appointmentTime', '预约时间', 'datetime'], ['remark', '备注'], ['status', '状态']
  ],
  contracts: [
    ['title', '标题'], ['contractNo', '合同编号'], ['contractType', '合同类型'], ['partyA', '甲方'], ['partyB', '乙方'], ['amount', '金额', 'number'], ['status', '状态'], ['fileUrl', '文件地址']
  ],
  documents: [
    ['title', '标题'], ['documentType', '文书类型'], ['content', '内容'], ['fileUrl', '文件地址'], ['status', '状态']
  ],
  invoices: [
    ['invoiceNo', '发票号'], ['title', '抬头'], ['taxNo', '税号'], ['amount', '金额', 'number'], ['invoiceType', '发票类型'], ['status', '状态'], ['fileUrl', '文件地址']
  ],
  notifications: [
    ['title', '标题'], ['content', '内容'], ['type', '类型'], ['readStatus', '阅读状态']
  ]
}

function build(title, endpoint, fields, extraActions = []) {
  return {
    title,
    endpoint,
    columns: fields.map(([prop, label]) => ({ prop, label })),
    fields: fields.map(([prop, label, type = 'text']) => ({ prop, label, type })),
    extraActions
  }
}

export const userConfigs = {
  consultations: build('我的咨询', '/api/me/consultations', commonFields.consultations),
  cases: build('我的案件', '/api/me/cases', commonFields.cases),
  appointments: build('我的预约', '/api/me/appointments', commonFields.appointments, [{ label: '取消', action: 'cancel', method: 'put', suffix: '/cancel' }]),
  contracts: build('我的合同', '/api/me/contracts', commonFields.contracts),
  documents: build('我的文书', '/api/me/documents', commonFields.documents),
  invoices: build('我的发票', '/api/me/invoices', commonFields.invoices),
  notifications: build('消息通知', '/api/me/notifications', commonFields.notifications, [{ label: '已读', action: 'read', method: 'put', suffix: '/read' }])
}

export const adminConfigs = {
  users: build('用户管理', '/api/admin/users', [['username', '用户名'], ['password', '密码'], ['nickname', '昵称'], ['phone', '手机'], ['email', '邮箱'], ['status', '状态', 'number']]),
  'law-firms': build('律所管理', '/api/admin/law-firms', [['name', '名称'], ['provinceCode', '省份编码'], ['provinceName', '省份'], ['city', '城市'], ['address', '地址'], ['phone', '电话'], ['email', '邮箱'], ['specialties', '专长'], ['licenseNo', '许可证号'], ['status', '状态', 'number']]),
  lawyers: build('律师管理', '/api/admin/lawyers', [['lawFirmId', '律所ID', 'number'], ['name', '姓名'], ['gender', '性别'], ['phone', '电话'], ['email', '邮箱'], ['category', '分类'], ['title', '职称'], ['experienceYears', '年限', 'number'], ['goodAt', '擅长'], ['status', '状态', 'number']]),
  consultations: build('咨询管理', '/api/admin/consultations', commonFields.consultations, [{ label: '关闭', action: 'close', method: 'put', suffix: '/close' }]),
  cases: build('案件管理', '/api/admin/cases', commonFields.cases),
  appointments: build('预约管理', '/api/admin/appointments', commonFields.appointments),
  contracts: build('合同管理', '/api/admin/contracts', commonFields.contracts),
  documents: build('文书管理', '/api/admin/documents', commonFields.documents),
  invoices: build('发票管理', '/api/admin/invoices', commonFields.invoices),
  notifications: build('通知管理', '/api/admin/notifications', commonFields.notifications)
}
