<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的资料</h1>
        <div class="muted">公开档案 · {{ profile.lawFirmName || '律所律师资料' }}</div>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card v-loading="loading" shadow="never" class="work-card profile-editor">
      <div class="profile-avatar-editor">
        <el-avatar :size="72" :src="profile.avatarUrl">{{ profile.name?.slice(0, 1) }}</el-avatar>
        <div>
          <ImageUpload endpoint="/api/lawyer-center/profile/avatar" label="更换头像" prominent
            @uploaded="(url) => profile.avatarUrl = url" />
          <div class="muted">JPEG 或 PNG，最大 2 MB</div>
        </div>
      </div>
      <el-form label-position="top" :model="form" @submit.prevent="save">
        <div class="profile-fields">
          <el-form-item label="姓名" required><el-input v-model.trim="form.name" maxlength="64" /></el-form-item>
          <el-form-item label="性别"><el-input v-model.trim="form.gender" maxlength="16" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model.trim="form.phone" maxlength="32" /></el-form-item>
          <el-form-item label="联系邮箱"><el-input v-model.trim="form.email" maxlength="128" /></el-form-item>
          <el-form-item label="专业分类"><el-input v-model.trim="form.category" maxlength="64" /></el-form-item>
          <el-form-item label="职称"><el-input v-model.trim="form.title" maxlength="64" /></el-form-item>
          <el-form-item label="执业年限">
            <el-input-number v-model="form.experienceYears" :min="0" :max="70" :precision="0" />
          </el-form-item>
        </div>
        <el-form-item label="擅长方向"><el-input v-model.trim="form.goodAt" type="textarea" /></el-form-item>
        <el-form-item label="个人介绍"><el-input v-model.trim="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving" :disabled="!profile.id">保存资料</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import ImageUpload from '../../components/ImageUpload.vue'
import { getLawyerProfile } from '../../api/lawyerCenter'

const profile = ref({})
const form = reactive({
  name: '', gender: '', phone: '', email: '', category: '', title: '', experienceYears: 0, goodAt: '', description: ''
})
const loading = ref(true)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    profile.value = await getLawyerProfile()
    for (const key of Object.keys(form)) form[key] = profile.value[key] ?? form[key]
  } catch (failure) {
    error.value = failure.message
  } finally {
    loading.value = false
  }
})

async function save() {
  if (!form.name || saving.value) {
    if (!form.name) ElMessage.warning('请输入姓名')
    return
  }
  saving.value = true
  try {
    profile.value = await http.put('/api/lawyer-center/profile', form)
    ElMessage.success('资料已保存')
  } catch (failure) {
    ElMessage.error(failure.message)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-editor { max-width: 760px; margin-top: 14px; }
.profile-avatar-editor { display: flex; gap: 16px; align-items: center; margin-bottom: 22px; }
.profile-avatar-editor .muted { margin-top: 4px; font-size: 12px; }
.profile-fields { display: grid; grid-template-columns: repeat(auto-fit, minmax(205px, 1fr)); gap: 0 18px; }
</style>
