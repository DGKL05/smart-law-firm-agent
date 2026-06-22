<template>
  <section class="page">
    <el-card class="detail-sheet" shadow="never">
      <h1 class="page-title">{{ item.name }}</h1>
      <p class="muted">{{ item.category }} · {{ item.title }} · {{ item.experienceYears }} 年经验</p>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="电话">{{ item.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ item.email }}</el-descriptions-item>
        <el-descriptions-item label="擅长" :span="2">{{ item.goodAt }}</el-descriptions-item>
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
  item.value = await http.get(`/api/public/lawyers/${route.params.id}`)
})
</script>
