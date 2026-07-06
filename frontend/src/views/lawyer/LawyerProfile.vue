<template>
  <section class="page">
    <h1>我的资料</h1>
    <el-card shadow="never">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="服务状态">{{ profile.status || 'ready' }}</el-descriptions-item>
        <el-descriptions-item label="服务来源">{{ profile.service || 'lawyer-service' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getLawyerProfile } from '../../api/lawyerCenter'

const profile = ref({})

onMounted(async () => {
  profile.value = await getLawyerProfile()
})
</script>
