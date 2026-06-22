<template>
  <div class="auth-page">
    <section class="auth-story">
      <div class="brand" style="margin-bottom: 34px">
        <span class="brand-mark">法</span>
        <span>Smart Law Firm</span>
      </div>
      <h1>建立个人法律服务档案</h1>
      <p>注册后可管理自己的咨询、案件、预约、合同、文书、发票和消息通知，后台接口会按登录用户自动过滤数据。</p>
    </section>
    <el-card class="auth-card" shadow="never">
      <template #header>
        <div>
          <h2 class="auth-title">用户注册</h2>
          <div class="muted">注册后默认绑定 USER 角色</div>
        </div>
      </template>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名" required><el-input v-model="form.username" size="large" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="form.password" size="large" type="password" show-password /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" size="large" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" size="large" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" size="large" /></el-form-item>
      </el-form>
      <el-button type="primary" size="large" style="width: 100%" @click="submit">创建账号</el-button>
      <div style="margin-top: 16px; text-align: center">
        <router-link class="muted" to="/login">已有账号，返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const router = useRouter()
const form = reactive({ username: '', password: '', nickname: '', phone: '', email: '' })

async function submit() {
  await http.post('/api/auth/register', form)
  ElMessage.success('注册成功，请登录')
  router.push('/login')
}
</script>
