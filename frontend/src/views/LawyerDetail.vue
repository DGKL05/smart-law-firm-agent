<template>
  <section class="page detail-page">
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />
    <el-card v-else v-loading="loading" class="detail-sheet profile-sheet editorial-sheet" shadow="never">
      <div class="profile-hero lawyer-hero lawyer-profile-hero">
        <div class="lawyer-identity">
          <div class="large-avatar">
            <img v-if="item.avatarUrl" :src="item.avatarUrl" :alt="item.name + '的头像'" class="lawyer-photo" />
            <span v-else>{{ item.name?.slice(0, 1) }}</span>
          </div>
          <div>
            <div class="profile-kicker">{{ item.lawFirmName }}</div>
            <h1 class="page-title">{{ item.name }}</h1>
            <p class="profile-summary">
              {{ item.category || '综合法律' }} · {{ item.title }} · {{ item.experienceYears || 0 }} 年经验
            </p>
            <div class="tag-row">
              <el-tag v-for="tag in goodAtTags" :key="tag" effect="plain" size="large">
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="profile-stats elevated-stats">
          <div class="profile-stat">
            <strong>{{ availableSlots.length }}</strong>
            <span>可约时段</span>
          </div>
          <div class="profile-stat">
            <strong>{{ item.experienceYears || 0 }}</strong>
            <span>执业年限</span>
          </div>
          <div class="profile-stat">
            <strong>{{ activeAppointments.length }}</strong>
            <span>我的预约</span>
          </div>
        </div>
      </div>

      <div class="detail-grid editorial-detail-grid">
        <section class="info-panel wide lawyer-intro-panel">
          <div class="panel-title">律师介绍</div>
          <p class="panel-copy">{{ item.description || '该律师暂未填写详细介绍。' }}</p>
          <div class="process-list refined-process">
            <span>案情评估</span>
            <span>证据梳理</span>
            <span>法律咨询</span>
            <span>后续方案</span>
          </div>
        </section>

        <section class="info-panel">
          <div class="panel-title">所属律所</div>
          <p class="panel-copy">{{ item.lawFirmName || '待补充' }}</p>
          <el-button type="primary" plain @click="$router.push(`/law-firms/${item.lawFirmId}`)">
            查看律所档案
          </el-button>
        </section>

        <section class="info-panel contact-panel">
          <div class="panel-title">联系信息</div>
          <dl class="info-list compact-info">
            <div>
              <dt>电话</dt>
              <dd>{{ item.phone || '待补充' }}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{{ item.email || '待补充' }}</dd>
            </div>
          </dl>
        </section>
      </div>

      <div class="detail-section">
        <div class="desk-toolbar">
          <div>
            <div class="desk-title">可预约法律咨询时间</div>
            <div class="muted">在月历上选择日期，再选择当天的咨询时段。</div>
          </div>
          <span class="record-count">{{ availableSlots.length }} / {{ scheduleSlots.length }} 可约</span>
        </div>
        <BookingCalendar :key="lawyerId" :slots="scheduleSlots" :busy="bookingPending || cancellingId !== null"
          @book="book" />
      </div>

      <div v-if="auth.isLogin" class="detail-section">
        <div class="desk-toolbar">
          <div>
            <div class="desk-title">我的本律师预约</div>
            <div class="muted">已预约的时间段会占用该律师日程，取消后时段会重新释放</div>
          </div>
          <span class="record-count">{{ activeAppointments.length }} 条</span>
        </div>
        <div v-if="activeAppointments.length" class="appointment-grid polished-appointment-grid">
          <article v-for="appointment in activeAppointments" :key="appointment.id" class="appointment-card mine polished-appointment-card">
            <span class="appointment-mark">{{ appointment.status }}</span>
            <strong>{{ formatDateTime(appointment.appointmentTime) }}</strong>
            <small>{{ appointment.remark || '法律咨询预约' }}</small>
            <el-button
              v-if="appointment.status !== '取消处理中'"
              type="danger"
              plain
              size="small"
              :loading="cancellingId === appointment.id"
              :disabled="bookingPending || cancellingId !== null"
              @click="cancelAppointment(appointment)"
            >
              取消预约
            </el-button>
          </article>
        </div>
        <el-empty v-else description="暂无本律师的有效预约" />
      </div>

      <div class="detail-section">
        <div class="desk-title">擅长方向</div>
        <div class="expertise-grid polished-expertise-grid">
          <article v-for="tag in goodAtTags" :key="tag" class="expertise-card polished-expertise-card">
            <strong>{{ tag }}</strong>
            <span>可提供规则解释、风险判断、证据准备和行动建议。</span>
          </article>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import BookingCalendar from '../components/BookingCalendar.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const lawyerId = computed(() => String(route.params.id || ''))
const item = ref({})
const appointments = ref([])
const bookingPending = ref(false)
const cancellingId = ref(null)
const loading = ref(true)
const loadError = ref('')
const unavailableSlots = ref(new Map())
const bookingDate = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'UTC', year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})
let loadVersion = 0
let disposed = false

const activeAppointments = computed(() => appointments.value.filter((appointment) => {
  const inactiveStatuses = ['已取消', '已完成', '预约失败']
  return String(appointment.lawyerId) === lawyerId.value && !inactiveStatuses.includes(appointment.status)
}))
const scheduleSlots = computed(() => {
  const myTimes = new Set(activeAppointments.value.map((appointment) => timeKey(appointment.appointmentTime)))
  return (item.value.scheduleSlots || []).map((slot) => {
    if (!slot.available) return slot
    const reason = unavailableSlots.value.get(slot.startTime)
      || (myTimes.has(timeKey(slot.startTime)) ? '已有预约' : '')
    return reason ? { ...slot, available: false, unavailableReason: reason } : slot
  })
})
const availableSlots = computed(() => scheduleSlots.value.filter((slot) => slot.available))
const goodAtTags = computed(() => splitText(item.value.goodAt))

function splitText(value) {
  return (value || '').split(/[、,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

function formatSlot(slot) {
  const date = bookingDate.format(new Date(slot.date + 'T00:00:00Z'))
  return `${date} ${formatTime(slot.startTime)}–${formatTime(slot.endTime)}`
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(`${value}T00:00:00`)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${value.slice(5).replace('-', '/')} ${weekdays[date.getDay()]}`
}

function formatTime(value) {
  return value?.includes('T') ? value.slice(11, 16) : value?.slice(0, 5) || ''
}

function formatDateTime(value) {
  if (!value) return ''
  return `${formatDate(value.slice(0, 10))} ${formatTime(value)}`
}

function timeKey(value) {
  return value?.slice(0, 16)
}

function isCurrent(id, version = loadVersion) {
  return !disposed && lawyerId.value === id && loadVersion === version
}

async function refresh(id, initial = false) {
  const version = ++loadVersion
  const detail = http.get(`/api/public/lawyers/${id}`).then((data) => {
    if (!isCurrent(id, version)) return
    item.value = data
    unavailableSlots.value.clear()
  }).catch((error) => {
    if (!isCurrent(id, version)) return
    if (initial) loadError.value = error.message
    else ElMessage.warning('暂时无法刷新日历，请稍后重试')
  }).finally(() => {
    if (isCurrent(id, version)) loading.value = false
  })
  if (!auth.isLogin) {
    if (isCurrent(id, version)) appointments.value = []
    await detail
    return
  }

  const mine = http.get('/api/me/appointments', { params: { pageNum: 1, pageSize: 100 } }).then((data) => {
    if (isCurrent(id, version)) appointments.value = data.records || []
  }).catch((error) => {
    if (isCurrent(id, version)) ElMessage.warning(error.message)
  })
  await Promise.all([detail, mine])
}

async function book(slot) {
  if (bookingPending.value || cancellingId.value !== null) return
  if (!auth.isLogin) {
    ElMessage.warning('请先登录后再预约')
    router.push('/login')
    return
  }
  const id = lawyerId.value
  const version = loadVersion
  const name = item.value.name
  bookingPending.value = true
  try {
    const confirmed = await ElMessageBox.confirm(
      `${name} 律师 · ${formatSlot(slot)}`, '确认预约',
      { confirmButtonText: '确认预约', cancelButtonText: '重新选择', type: 'info' }
    ).then(() => true, () => false)
    if (!confirmed || !isCurrent(id, version)) return
    if (!scheduleSlots.value.some((current) => current.startTime === slot.startTime
        && current.endTime === slot.endTime && current.available)) {
      ElMessage.warning('该时段状态已变化，请重新选择')
      await refresh(id)
      return
    }
    const result = await http.post('/api/me/appointments/book', {
      lawyerId: Number(id),
      appointmentTime: slot.startTime,
      remark: `${name} 法律咨询`
    })
    if (!isCurrent(id, version)) return
    unavailableSlots.value.set(slot.startTime, result.status === '预约失败' ? '时段状态已变化' : '已有预约')
    if (result.id && result.status !== '预约失败') {
      appointments.value = [result, ...appointments.value.filter((appointment) => appointment.id !== result.id)]
    }
    if (result.status === '已预约') ElMessage.success('预约成功')
    else if (result.status === '预约失败') ElMessage.warning(result.failureReason || '该时段无法预约')
    else ElMessage.info('预约请求已受理，请在我的预约中查看结果')
    await refresh(id)
  } catch (error) {
    if (!isCurrent(id, version)) return
    if (error.response?.status === 409 || error.response?.data?.code === 409) {
      unavailableSlots.value.set(slot.startTime, '时段状态已变化')
      ElMessage.warning(error.message || '该时段状态已变化，请重新选择')
      await refresh(id)
    } else {
      ElMessage.error(error.message)
    }
  } finally {
    bookingPending.value = false
  }
}

async function cancelAppointment(appointment) {
  if (cancellingId.value !== null || bookingPending.value) return
  const id = lawyerId.value
  const version = loadVersion
  cancellingId.value = appointment.id
  try {
    const result = await http.put('/api/me/appointments/' + appointment.id + '/cancel')
    if (!isCurrent(id, version)) return
    if (result.status === '已取消') ElMessage.success('已取消预约')
    else ElMessage.info('取消请求已受理，请稍后查看结果')
    await refresh(id)
  } catch (error) {
    if (isCurrent(id, version)) ElMessage.error(error.message)
  } finally {
    cancellingId.value = null
  }
}

watch(lawyerId, (id) => {
  if (!id) return
  item.value = {}
  appointments.value = []
  unavailableSlots.value.clear()
  loadError.value = ''
  loading.value = true
  refresh(id, true)
}, { immediate: true })

onBeforeUnmount(() => {
  disposed = true
})
</script>

<style scoped>
.lawyer-photo { width: 100%; height: 100%; object-fit: cover; border-radius: inherit; }
</style>
