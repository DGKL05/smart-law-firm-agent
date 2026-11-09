<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">律师工作台</h1>
        <p class="muted">查看预约、回复咨询、维护可用时段</p>
      </div>
    </div>
    <div class="dashboard-cards">
      <router-link v-for="item in items" :key="item.path" :to="item.path" class="dashboard-card">
        <span>{{ item.label }}</span>
        <strong>{{ counts[item.path] ?? '—' }}</strong>
        <span>查看记录 →</span>
      </router-link>
    </div>
    <el-alert v-if="unavailable.length" type="warning" show-icon :closable="false"
      :title="'暂时无法读取：' + unavailable.join('、')" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/http'

const items = [
  { label: '预约记录', path: '/lawyer-center/appointments', endpoint: '/api/lawyer/appointments' },
  { label: '收到的咨询', path: '/lawyer-center/consultations', endpoint: '/api/lawyer/consultations' },
  { label: '排班规则', path: '/lawyer-center/schedule', endpoint: '/api/lawyer/schedules' }
]
const counts = reactive({})
const unavailable = ref([])

onMounted(async () => {
  const results = await Promise.allSettled(items.map(({ endpoint }) =>
    http.get(endpoint, { params: { pageNum: 1, pageSize: 1 } })))
  results.forEach((result, index) => {
    const { path, label } = items[index]
    if (result.status === 'fulfilled') counts[path] = result.value.total
    else unavailable.value.push(label)
  })
})
</script>

<style scoped>
.dashboard-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; margin: 18px 0; }
.dashboard-card { display: grid; gap: 10px; background: #fff; padding: 20px; border: 1px solid #e2d8c5; border-radius: 8px; }
.dashboard-card strong { font-size: 32px; color: #123f36; }
.dashboard-card span { color: #47655a; }
</style>
