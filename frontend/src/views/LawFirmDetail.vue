<template>
  <section class="page">
    <el-card class="detail-sheet" shadow="never">
      <h1 class="page-title">{{ item.name }}</h1>
      <p class="muted">{{ item.provinceName }} {{ item.city }} · {{ item.address }}</p>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="电话">{{ item.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ item.email }}</el-descriptions-item>
        <el-descriptions-item label="许可证号">{{ item.licenseNo }}</el-descriptions-item>
        <el-descriptions-item label="业务专长">{{ item.specialties }}</el-descriptions-item>
        <el-descriptions-item label="介绍" :span="2">{{ item.description }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const item = ref({})
onMounted(async () => {
  item.value = await http.get(`/api/public/law-firms/${route.params.id}`)
})
</script>
