<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1 class="auth-title">账号登录</h1>
      <p v-if="!error" class="muted" role="status">正在验证登录，请稍候…</p>
      <template v-else>
        <el-alert :title="error" type="error" show-icon :closable="false" />
        <router-link to="/login">返回登录</router-link>
      </template>
    </el-card>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const error = ref('')

onMounted(async () => {
  const query = { ...route.query }
  await router.replace('/oauth/callback')
  try {
    await auth.finishLogin(query)
    const destination = auth.isAdmin ? '/admin/stats' : auth.isLawyer ? '/lawyer-center' : '/user/consultations'
    await router.replace(destination)
  } catch (failure) {
    error.value = failure.message
  }
})
</script>
