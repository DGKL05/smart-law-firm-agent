<template>
  <el-select :model-value="modelValue" :disabled="disabled || !source" :clearable="!required"
    :filterable="source?.[2] !== false" remote :remote-method="search" :loading="loading"
    :placeholder="placeholder" style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event === '' ? null : $event ?? null)" @visible-change="onVisible">
    <el-option v-for="option in visibleOptions" :key="option.value" :label="option.label" :value="option.value">
      <span>{{ option.label }}</span>
      <span class="reference-id">#{{ option.value }}</span>
    </el-option>
    <template #label="{ label: optionLabel }">{{ selectedLabel || optionLabel }}</template>
    <template #empty>
      <div class="reference-empty">{{ error || '没有匹配的记录' }}</div>
    </template>
    <template v-if="hasMore || error" #footer>
      <el-button link type="primary" :loading="loading" @mousedown.prevent @click="loadNext">
        {{ error ? '重试加载' : '加载更多' }}
      </el-button>
    </template>
  </el-select>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { listReferences, readReference, referenceLabel, referencePageSize, referenceSource } from '../api/crudReferences'

const props = defineProps({ modelValue: [Number, String], kind: String, label: String, required: Boolean, disabled: Boolean })
defineEmits(['update:modelValue'])

const source = computed(() => referenceSource(props.kind))
const placeholder = computed(() => source.value ? `请选择${props.label}`
  : props.kind ? '此类型没有可选对象' : '请先选择资源类型')
const options = ref([])
const selected = ref(null)
const loading = ref(false)
const error = ref('')
const page = ref(0)
const hasMore = ref(false)
let keyword = ''
let revision = 0

const visibleOptions = computed(() => {
  if (!selected.value || options.value.some((item) => item.value === selected.value.value)) return options.value
  return [selected.value, ...options.value]
})
const selectedLabel = computed(() => visibleOptions.value.find((item) => item.value === props.modelValue)?.label)

watch(() => props.modelValue, async (value, _, onCleanup) => {
  selected.value = null
  if (value == null || value === '' || !source.value) return
  const existing = options.value.find((item) => item.value === value)
  if (existing) {
    selected.value = existing
    return
  }
  let active = true
  onCleanup(() => { active = false })
  selected.value = { value, label: `原关联 #${value}` }
  try {
    const label = await readReference(props.kind, value)
    if (active) selected.value = { value, label }
  } catch {
    if (active) selected.value = { value, label: `原关联名称不可用 #${value}` }
  }
}, { immediate: true })

function onVisible(visible) {
  if (visible) search('')
}

function search(text) {
  keyword = text.trim().slice(0, 80)
  options.value = []
  page.value = 0
  hasMore.value = false
  return loadNext()
}

async function loadNext() {
  if (!source.value) return
  const current = ++revision
  const nextPage = page.value + 1
  loading.value = true
  error.value = ''
  try {
    const data = await listReferences(props.kind, keyword, nextPage)
    if (current !== revision) return
    const next = (data.records || []).map((row) => ({ value: row.id, label: referenceLabel(props.kind, row) }))
    options.value = [...new Map([...options.value, ...next].map((item) => [item.value, item])).values()]
    page.value = nextPage
    hasMore.value = next.length > 0 && nextPage * referencePageSize < data.total
  } catch (failure) {
    if (current === revision) error.value = failure.message || '关联对象加载失败'
  } finally {
    if (current === revision) loading.value = false
  }
}
</script>

<style scoped>
.reference-id { float: right; margin-left: 16px; color: var(--muted); font-size: 12px; }
.reference-empty { padding: 10px; text-align: center; color: var(--muted); }
</style>
