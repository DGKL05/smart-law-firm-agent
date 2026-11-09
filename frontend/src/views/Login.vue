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
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-form :model="form" label-position="top" method="post" action="/api/oauth2/login" @submit="submit">
        <input v-if="csrf" type="hidden" :name="csrf.parameterName" :value="csrf.token" />
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" name="username" autocomplete="username" size="large" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" name="password" autocomplete="current-password"
            size="large" type="password" show-password />
        </el-form-item>
        <el-button v-if="csrf" type="primary" native-type="submit" size="large"
          style="width: 100%" :loading="submitting">登录系统</el-button>
      </el-form>
      <el-button v-if="!csrf" type="primary" size="large" style="width: 100%"
        :loading="loading" @click="prepareLogin">继续登录</el-button>
      <div style="margin-top: 16px; text-align: center">
        <router-link class="muted" to="/register">创建普通用户账号</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'
import { beginAuthorization } from '../api/oauth'

const route = useRoute()
const form = reactive({ username: 'admin', password: '123456' })
const csrf = ref(null)
const error = ref(route.query.error === '1' ? '用户名或密码错误，或账号已停用' : '')
const loading = ref(false)
const submitting = ref(false)

async function prepareLogin() {
  loading.value = true
  try {
    if (route.query.oauth === '1') {
      csrf.value = await http.get('/api/oauth2/csrf')
    } else {
      await beginAuthorization()
    }
  } catch (failure) {
    error.value = failure.message
  } finally {
    loading.value = false
  }
}

function submit(event) {
  if (!csrf.value || submitting.value || !form.username.trim() || !form.password) {
    event.preventDefault()
    return
  }
  submitting.value = true
}

onMounted(prepareLogin)
</script>
