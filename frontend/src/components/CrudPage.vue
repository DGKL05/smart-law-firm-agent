<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ config.title }}</h1>
        <div v-if="config.description" class="muted">{{ config.description }}</div>
      </div>
      <el-button v-if="config.createFields" type="primary" :icon="Plus" @click="openForm()">
        {{ config.createLabel || '新增记录' }}
      </el-button>
    </div>

    <el-card class="work-card" shadow="never">
      <div class="desk-toolbar">
        <el-form inline class="search-desk" @submit.prevent="search">
          <el-form-item v-if="config.search !== false" label="关键词">
            <el-input v-model.trim="keyword" placeholder="输入关键词" clearable maxlength="80" />
          </el-form-item>
          <el-form-item>
            <el-button :icon="Search" native-type="submit">{{ config.search === false ? '刷新' : '搜索' }}</el-button>
          </el-form-item>
        </el-form>
        <span class="record-count">{{ total }} 条记录</span>
      </div>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="list-error" />

      <el-table v-loading="loading" :data="records" border stripe class="ledger-table" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column
          v-for="column in config.columns"
          :key="column.prop"
          :label="column.label"
          :min-width="column.width || 125"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ renderField(column, row[column.prop]) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="ledger-actions">
              <el-button v-if="config.detail !== false" size="small" link @click="showDetail(row)">查看</el-button>
              <el-button v-if="config.updateFields && permits(config.canEdit, row)" size="small" link
                type="primary" @click="openForm(row)">编辑</el-button>
              <ImageUpload
                v-if="config.asset"
                :endpoint="config.endpoint + '/' + row.id + config.asset.suffix"
                :label="config.asset.label"
                @uploaded="load"
              />
              <el-button
                v-for="item in actionsFor(row)"
                :key="item.suffix"
                size="small"
                link
                :type="item.danger ? 'danger' : 'primary'"
                :loading="busyAction === row.id + item.suffix"
                @click="runExtra(row, item)"
              >{{ item.label }}</el-button>
              <el-button v-if="config.remove && permits(config.canRemove, row)" size="small" link
                type="danger" :loading="busyAction === row.id + 'delete'" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="ledger-pagination"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑记录' : '新增记录'" width="min(620px, 94vw)">
      <el-form ref="editor" :model="form" :rules="rules" label-width="104px" @submit.prevent="submit">
        <el-form-item v-for="field in fields" :key="field.prop" :label="field.label" :prop="field.prop">
          <el-date-picker v-if="field.type === 'datetime'" v-model="form[field.prop]" type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          <el-time-picker v-else-if="field.type === 'time'" v-model="form[field.prop]"
            value-format="HH:mm:ss" format="HH:mm" style="width: 100%" />
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]"
            :min="field.min ?? 1" :max="field.max" :precision="0" style="width: 100%" />
          <el-select v-else-if="field.options" v-model="form[field.prop]"
            :multiple="field.type === 'multi'" :clearable="!field.required" style="width: 100%">
            <el-option v-for="option in field.options" :key="option.value" :label="option.label"
              :value="option.value" />
          </el-select>
          <el-input v-else v-model="form[field.prop]" :type="field.type || 'text'"
            :show-password="field.type === 'password'" :maxlength="field.maxlength" :placeholder="field.placeholder" />
        </el-form-item>
        <el-alert v-if="formError" :title="formError" type="error" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="记录详情" width="min(620px, 94vw)">
      <el-descriptions v-if="detail" border :column="1">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item v-for="column in config.columns" :key="column.prop" :label="column.label">
          {{ renderField(column, detail[column.prop]) }}
        </el-descriptions-item>
      </el-descriptions>
      <img v-if="config.asset && detail?.[config.asset.prop]" :src="detail[config.asset.prop]"
        alt="资料图片" class="detail-image" />
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import ImageUpload from './ImageUpload.vue'

const props = defineProps({ config: { type: Object, required: true } })
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const editor = ref()
const saving = ref(false)
const formError = ref('')
const form = reactive({})
const detail = ref(null)
const detailVisible = ref(false)
const busyAction = ref('')
let requestNumber = 0

const fields = computed(() => (editingId.value ? props.config.updateFields : props.config.createFields) || [])
const rules = computed(() => Object.fromEntries(fields.value.filter((field) => field.required).map((field) => [
  field.prop, [{ required: true, message: '请填写' + field.label, trigger: ['blur', 'change'] }]
])))

watch(() => props.config.endpoint, () => {
  records.value = []
  total.value = 0
  keyword.value = ''
  pageNum.value = 1
  dialogVisible.value = false
  detailVisible.value = false
  load()
}, { immediate: true })

async function load() {
  const current = ++requestNumber
  loading.value = true
  loadError.value = ''
  try {
    const data = await http.get(props.config.endpoint, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined }
    })
    if (current !== requestNumber) return
    records.value = data.records || []
    total.value = data.total ?? 0
    const lastPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (!records.value.length && pageNum.value > lastPage) {
      pageNum.value = lastPage
      await load()
    }
  } catch (error) {
    if (current === requestNumber) {
      records.value = []
      total.value = 0
      loadError.value = error.message
    }
  } finally {
    if (current === requestNumber) loading.value = false
  }
}

function search() {
  pageNum.value = 1
  return load()
}

function renderField(column, value) {
  if (value == null || value === '') return '—'
  if (Array.isArray(value)) return value.map((item) => labelOf(column, item)).join('、')
  return column.type === 'datetime' ? String(value).replace('T', ' ').slice(0, 16) : labelOf(column, value)
}

function labelOf(field, value) {
  return field.options?.find((option) => option.value === value)?.label ?? value
}

function permits(predicate, row) {
  return !predicate || predicate(row)
}

function actionsFor(row) {
  return (props.config.extraActions || []).filter((item) => permits(item.when, row))
}

async function openForm(row) {
  editingId.value = row?.id ?? null
  formError.value = ''
  for (const key of Object.keys(form)) delete form[key]
  for (const field of fields.value) {
    const value = field.type === 'password' ? null : row?.[field.source || field.prop]
    form[field.prop] = value ?? field.initial ?? (field.type === 'multi' ? [] : null)
  }
  dialogVisible.value = true
  await nextTick()
  editor.value?.clearValidate()
}

async function submit() {
  if (saving.value || !await editor.value.validate().catch(() => false)) return
  saving.value = true
  formError.value = ''
  const body = Object.fromEntries(fields.value.map((field) => {
    const value = form[field.prop]
    return [field.prop, typeof value === 'string' && field.type !== 'password' ? value.trim() : value]
  }))
  try {
    const result = editingId.value
      ? await http.put(props.config.endpoint + '/' + editingId.value, body)
      : await http.post(props.config.endpoint, body)
    reportResult(result, '已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    formError.value = error.message
  } finally {
    saving.value = false
  }
}

function reportResult(result, successMessage) {
  if (result?.status?.endsWith?.('处理中')) {
    ElMessage.info('请求已受理，刷新可查看处理结果')
  } else if (result?.status === '预约失败') {
    ElMessage.warning(result.failureReason || '预约失败，请选择其他时段')
  } else {
    ElMessage.success(successMessage)
  }
}

async function showDetail(row) {
  try {
    detail.value = await http.get(props.config.endpoint + '/' + row.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确认删除该记录？', '删除记录', { type: 'warning' })
  } catch {
    return
  }
  await perform(row, 'delete', () => http.delete(props.config.endpoint + '/' + row.id), '已删除')
}

async function runExtra(row, item) {
  let body
  if (item.input) {
    try {
      const { value } = await ElMessageBox.prompt('请输入' + item.input.label, item.label, {
        inputType: 'textarea',
        inputValidator: (value) => Boolean(value?.trim()) || '请输入内容'
      })
      body = { [item.input.prop]: value.trim() }
    } catch {
      return
    }
  }
  await perform(row, item.suffix, () => http.put(props.config.endpoint + '/' + row.id + item.suffix, body), '操作已完成')
}

async function perform(row, action, request, successMessage) {
  if (busyAction.value) return
  busyAction.value = row.id + action
  try {
    reportResult(await request(), successMessage)
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    busyAction.value = ''
  }
}
</script>

<style scoped>
.ledger-actions { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.ledger-actions :deep(.el-button) { margin-left: 0; }
.ledger-pagination { margin-top: 16px; }
.list-error { margin-bottom: 12px; }
.search-desk :deep(.el-form-item) { margin-bottom: 0; }
.detail-image { display: block; max-width: 100%; max-height: 240px; margin: 16px auto 0; object-fit: contain; }
</style>
