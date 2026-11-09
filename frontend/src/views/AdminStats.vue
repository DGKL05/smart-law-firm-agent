<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">系统统计</h1>
        <div class="muted">业务模块、用户规模和核心工作量概览</div>
      </div>
    </div>
    <div class="admin-overview">
      <el-card v-for="item in items" :key="item.key" shadow="never" class="metric-card">
        <div class="metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="muted">{{ item.label }}</div>
          <div class="metric-number">{{ stats[item.key] ?? '—' }}</div>
        </div>
      </el-card>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <el-alert v-else-if="stats.unavailableServices?.length"
      :title="'部分统计暂不可用：' + stats.unavailableServices.join('、')" type="warning" show-icon :closable="false" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const stats = ref({})
const error = ref('')
const items = [
  { key: 'userCount', label: '用户数', icon: 'User' },
  { key: 'lawFirmCount', label: '律所数', icon: 'OfficeBuilding' },
  { key: 'lawyerCount', label: '律师数', icon: 'Avatar' },
  { key: 'consultationCount', label: '咨询数', icon: 'ChatLineRound' },
  { key: 'appointmentCount', label: '预约数', icon: 'Calendar' },
  { key: 'caseCount', label: '案件数', icon: 'Document' },
  { key: 'scheduleCount', label: '排班规则', icon: 'Clock' },
  { key: 'notificationCount', label: '通知数', icon: 'Bell' },
  { key: 'sessionCount', label: 'AI 会话', icon: 'Service' }
]

onMounted(async () => {
  try {
    stats.value = await http.get('/api/admin/stats')
  } catch (failure) {
    error.value = failure.message
  }
})
</script>
