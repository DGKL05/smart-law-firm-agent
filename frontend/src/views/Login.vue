<template>
  <div class="auth-page">
    <section class="auth-story">
      <div class="brand" style="margin-bottom: 34px">
        <span class="brand-mark">法</span>
        <span>Smart Law Firm</span>
      </div>
      <h1>进入律所业务工作台</h1>
      <p>统一处理公开检索、用户中心、后台数据维护与后续 Agent 能力接入。默认演示账号已填入，可直接登录验证。</p>
    </section>
    <el-card class="auth-card" shadow="never">
      <template #header>
        <div>
          <h2 class="auth-title">账号登录</h2>
          <div class="muted">管理员：admin / 123456</div>
        </div>
      </template>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" size="large" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" size="large" type="password" show-password @keyup.enter="submit" />
        </el-form-item>
      </el-form>
      <el-button type="primary" size="large" style="width: 100%" @click="submit">登录系统</el-button>
      <div style="margin-top: 16px; text-align: center">
        <router-link class="muted" to="/register">创建普通用户账号</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'admin', password: '123456' })

async function submit() {
  await auth.login(form)
  ElMessage.success('登录成功')
  router.push(auth.isAdmin ? '/admin/stats' : '/user/consultations')
}
</script>
