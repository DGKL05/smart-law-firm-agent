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
          <div class="metric-number">{{ stats[item.key] || 0 }}</div>
        </div>
      </el-card>
    </div>
    <el-card shadow="never" class="work-card overview-panel">
      <div class="desk-toolbar">
        <div>
          <div class="desk-title">验收能力覆盖</div>
          <div class="muted">当前基础版本覆盖课程要求的关键技术点</div>
        </div>
      </div>
      <div class="capability-list">
        <span>Spring Security + JWT</span>
        <span>MyBatis-Plus 分页</span>
        <span>Redis 热点缓存</span>
        <span>MongoDB 操作日志</span>
        <span>Vue3 动态渲染</span>
        <span>后台角色授权</span>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const stats = ref({})
const items = [
  { key: 'userCount', label: '用户数', icon: 'User' },
  { key: 'lawFirmCount', label: '律所数', icon: 'OfficeBuilding' },
  { key: 'lawyerCount', label: '律师数', icon: 'Avatar' },
  { key: 'consultationCount', label: '咨询数', icon: 'ChatLineRound' },
  { key: 'appointmentCount', label: '预约数', icon: 'Calendar' }
]

onMounted(async () => {
  stats.value = await http.get('/api/admin/stats')
})
</script>
