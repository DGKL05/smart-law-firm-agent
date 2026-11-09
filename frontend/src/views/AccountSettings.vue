<template>
  <section class="page">
    <div class="page-header"><h1 class="page-title">账号设置</h1></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <div class="account-panels">
      <el-card shadow="never" class="work-card" v-loading="loading">
        <template #header>个人资料</template>
        <el-form label-position="top" @submit.prevent="saveProfile">
          <el-form-item label="用户名"><el-input :model-value="username" disabled /></el-form-item>
          <el-form-item label="昵称"><el-input v-model.trim="profile.nickname" maxlength="64" /></el-form-item>
          <el-form-item label="手机"><el-input v-model.trim="profile.phone" maxlength="32" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model.trim="profile.email" maxlength="128" /></el-form-item>
          <el-button native-type="submit" type="primary" :loading="saving" :disabled="!username">保存资料</el-button>
        </el-form>
      </el-card>
      <el-card shadow="never" class="work-card">
        <template #header>修改密码</template>
        <el-form label-position="top" @submit.prevent="changePassword">
          <el-form-item label="当前密码" required>
            <el-input v-model="password.oldPassword" type="password" autocomplete="current-password" show-password />
          </el-form-item>
          <el-form-item label="新密码" required>
            <el-input v-model="password.newPassword" type="password" autocomplete="new-password" show-password />
          </el-form-item>
          <el-button native-type="submit" type="primary" :loading="changing">更新密码</el-button>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const profile = reactive({ nickname: '', phone: '', email: '' })
const password = reactive({ oldPassword: '', newPassword: '' })
const loading = ref(true)
const saving = ref(false)
const changing = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    const data = await http.get('/api/auth/me')
    username.value = data.username
    for (const key of Object.keys(profile)) profile[key] = data[key] || ''
  } catch (failure) {
    error.value = failure.message
  } finally {
    loading.value = false
  }
})

async function saveProfile() {
  if (saving.value) return
  saving.value = true
  try {
    const data = await http.put('/api/auth/me', profile)
    auth.updateProfile(data)
    ElMessage.success('资料已保存')
  } catch (failure) {
    ElMessage.error(failure.message)
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (changing.value) return
  if (!password.oldPassword || !password.newPassword) {
    ElMessage.warning('请输入当前密码和新密码')
    return
  }
  changing.value = true
  try {
    await http.put('/api/auth/password', password)
    auth.clear()
    ElMessage.success('密码已更新，请重新登录')
    await router.push('/login')
  } catch (failure) {
    ElMessage.error(failure.message)
  } finally {
    changing.value = false
  }
}
</script>

<style scoped>
.account-panels { display: grid; grid-template-columns: repeat(auto-fit, minmax(270px, 1fr)); gap: 20px; margin-top: 16px; }
</style>
