<template>
  <div class="image-upload">
    <input ref="fileInput" type="file" accept="image/png,image/jpeg" hidden @change="upload" />
    <el-button :loading="uploading" :link="!prominent" :plain="prominent" size="small" type="primary"
      @click="fileInput.click()">{{ label }}</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const props = defineProps({
  endpoint: { type: String, required: true },
  label: { type: String, required: true },
  prominent: { type: Boolean, default: false }
})
const emit = defineEmits(['uploaded'])
const fileInput = ref()
const uploading = ref(false)
const maxImageBytes = 2 * 1024 * 1024

async function upload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > maxImageBytes) {
    ElMessage.warning('请选择不超过 2 MB 的 JPEG 或 PNG 图片')
    return
  }
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const result = await http.post(props.endpoint, form)
    emit('uploaded', result.url)
    ElMessage.success('图片已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    uploading.value = false
  }
}
</script>
