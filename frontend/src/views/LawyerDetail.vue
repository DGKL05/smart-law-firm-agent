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
            <div class="muted">按律师最新排班展示。已占用、已过期和停诊时段不可预约。</div>
          </div>
          <span class="record-count">{{ availableSlots.length }} / {{ scheduleSlots.length }} 可约</span>
        </div>
        <el-empty v-if="!scheduleSlots.length" description="暂无开放时段" />
        <div class="appointment-grid polished-appointment-grid">
          <article
            v-for="slot in scheduleSlots"
            :key="slot.startTime"
            class="appointment-card polished-appointment-card"
            :class="{ unavailable: !slot.available }"
          >
            <span class="appointment-mark">{{ slot.available ? '可约' : slot.unavailableReason }}</span>
            <strong>{{ formatSlot(slot) }}</strong>
            <small>{{ slot.available ? '适合初次咨询、材料审查和方案沟通' : '该时间段暂不可预约' }}</small>
            <el-button
              v-if="slot.available"
              type="primary"
              size="small"
              :loading="bookingTime === slot.startTime"
              @click="book(slot)"
            >
              预约此时段
            </el-button>
          </article>
        </div>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const item = ref({})
const appointments = ref([])
const bookingTime = ref('')
const cancellingId = ref(null)
const loading = ref(true)
const loadError = ref('')
const scheduleSlots = computed(() => item.value.scheduleSlots || [])
const availableSlots = computed(() => scheduleSlots.value.filter((slot) => slot.available))
const activeAppointments = computed(() => appointments.value.filter((appointment) => {
  const inactiveStatuses = ['已取消', '已完成', '预约失败']
  return appointment.lawyerId === Number(route.params.id) && !inactiveStatuses.includes(appointment.status)
}))
const goodAtTags = computed(() => splitText(item.value.goodAt))

function splitText(value) {
  return (value || '').split(/[、,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

function formatSlot(slot) {
  return `${formatDate(slot.date)} ${formatTime(slot.startTime)}-${formatTime(slot.endTime)}`
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(`${value}T00:00:00`)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${value.slice(5).replace('-', '/')} ${weekdays[date.getDay()]}`
}

function formatTime(value) {
  return value?.slice(11, 16) || ''
}

function formatDateTime(value) {
  if (!value) return ''
  return `${formatDate(value.slice(0, 10))} ${formatTime(value)}`
}

async function loadDetail() {
  item.value = await http.get(`/api/public/lawyers/${route.params.id}`)
}

async function loadAppointments() {
  if (!auth.isLogin) {
    appointments.value = []
    return
  }
  const data = await http.get('/api/me/appointments', { params: { pageNum: 1, pageSize: 100 } })
  appointments.value = data.records || []
}

async function book(slot) {
  if (!auth.isLogin) {
    ElMessage.warning('请先登录后再预约')
    router.push('/login')
    return
  }
  bookingTime.value = slot.startTime
  try {
    const result = await http.post('/api/me/appointments/book', {
      lawyerId: Number(route.params.id),
      appointmentTime: slot.startTime,
      remark: `${item.value.name} 法律咨询`
    })
    if (result.status === '已预约') ElMessage.success('预约成功')
    else if (result.status === '预约失败') ElMessage.warning(result.failureReason || '该时段无法预约')
    else ElMessage.info('预约请求已受理，请在我的预约中查看结果')
    await Promise.all([loadDetail(), loadAppointments()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    bookingTime.value = ''
  }
}

async function cancelAppointment(appointment) {
  cancellingId.value = appointment.id
  try {
    const result = await http.put('/api/me/appointments/' + appointment.id + '/cancel')
    if (result.status === '已取消') ElMessage.success('已取消预约')
    else ElMessage.info('取消请求已受理，请稍后查看结果')
    await Promise.all([loadDetail(), loadAppointments()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    cancellingId.value = null
  }
}

onMounted(async () => {
  try {
    await loadDetail()
  } catch (error) {
    loadError.value = error.message
  } finally {
    loading.value = false
  }
  try {
    await loadAppointments()
  } catch (error) {
    ElMessage.warning(error.message)
  }
})
</script>

<style scoped>
.lawyer-photo { width: 100%; height: 100%; object-fit: cover; border-radius: inherit; }
</style>
