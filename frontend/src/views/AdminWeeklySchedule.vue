<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">每周预约排班</h1>
        <p class="muted">按律师管理一周的开放时段，编辑后统一保存。</p>
      </div>
      <router-link :to="{ path: '/admin/schedule-blocks', query: { lawyerId: lawyerId || undefined } }"
        custom v-slot="{ navigate }">
        <el-button plain @click="navigate">管理临时停诊</el-button>
      </router-link>
    </div>

    <el-card class="work-card" shadow="never" v-loading="loading">
      <div class="schedule-toolbar">
        <div class="schedule-picker">
          <span class="muted">按姓名搜索律师</span>
          <ReferenceSelect :model-value="lawyerId" kind="lawyers" label="律师" :disabled="saving"
            @update:model-value="selectLawyer" />
        </div>
        <el-button v-if="lawyerId" :disabled="loading || saving" @click="refresh">重新加载</el-button>
        <el-tag v-if="snapshot" :type="dirty ? 'warning' : 'info'" effect="plain">
          {{ dirty ? '有未保存修改' : snapshot.configured ? '已配置排班' : '默认排班' }}
        </el-tag>
      </div>

      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="schedule-alert" />
      <el-empty v-if="!lawyerId" description="请选择一位律师查看排班" />

      <template v-if="snapshot">
        <div class="week-overview" aria-label="每周开放时段">
          <button v-for="day in week" :key="day.weekday" type="button" class="week-day"
            :class="{ 'is-active': weekday === day.weekday }" :aria-pressed="weekday === day.weekday"
            @click="weekday = day.weekday">
            <strong>{{ day.name }}</strong>
            <span v-for="rule in day.rules" :key="rule.key" class="week-time" :class="{ closed: !rule.enabled }">
              {{ rule.startTime || '—' }}–{{ rule.endTime || '—' }}
            </span>
            <small v-if="!day.rules.length">无时段</small>
            <small v-else>{{ day.rules.filter((rule) => rule.enabled).length }} 个开放</small>
          </button>
        </div>

        <section class="day-editor" :aria-label="`${selectedDay.name}时段`">
          <div class="schedule-toolbar day-toolbar">
            <div>
              <h2>{{ selectedDay.name }}的时段</h2>
              <div class="muted">每段至少一小时，同一天的时段不能重叠。</div>
            </div>
            <div class="day-actions">
              <el-button :disabled="saving || !selectedDay.rules.length" @click="openCopy">复制到其他星期</el-button>
              <el-button :disabled="saving || !selectedDay.rules.length" @click="toggleDay">
                {{ selectedDay.rules.some((rule) => rule.enabled) ? '整日停用' : '整日开放' }}
              </el-button>
              <el-button plain type="primary" :disabled="saving" @click="addRule">添加时段</el-button>
            </div>
          </div>
          <p v-if="!selectedDay.rules.length" class="muted">这一天还没有时段，可以添加后保存。</p>
          <div v-for="(rule, index) in selectedDay.rules" :key="rule.key" class="time-row">
            <label>开始 <input v-model="rule.startTime" type="time" step="60" :disabled="saving"
              :aria-label="`第 ${index + 1} 段开始时间`" /></label>
            <label>结束 <input v-model="rule.endTime" type="time" step="60" :disabled="saving"
              :aria-label="`第 ${index + 1} 段结束时间`" /></label>
            <el-switch v-model="rule.enabled" :active-value="1" :inactive-value="0" :disabled="saving"
              :aria-label="`第 ${index + 1} 段是否开放`" active-text="开放" inactive-text="停用" />
            <el-button link type="danger" :disabled="saving" @click="removeRule(rule.key)">移除</el-button>
          </div>
        </section>

        <div class="schedule-save">
          <span class="muted">已预约的时段会在保存时校验。</span>
          <el-button type="primary" :disabled="!dirty" :loading="saving" @click="saveWeek">保存整周</el-button>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="copyVisible" :title="`复制${selectedDay.name}的时段`" width="min(460px, 94vw)">
      <p class="muted">所选日期的时段会被替换。修改会在保存整周后生效。</p>
      <el-checkbox-group v-model="copyDays" class="copy-days">
        <el-checkbox v-for="day in copyTargets" :key="day.weekday" :value="day.weekday">{{ day.name }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!copyDays.length" @click="copySchedule">应用到所选日期</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import ReferenceSelect from '../components/ReferenceSelect.vue'

const endpoint = '/api/admin/schedules/weekly'
const weekdayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const route = useRoute()
const initialLawyerId = Number(route.query.lawyerId)
const lawyerId = ref(Number.isSafeInteger(initialLawyerId) && initialLawyerId > 0 ? initialLawyerId : null)
const snapshot = ref(null)
const draft = ref([])
const weekday = ref(1)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const copyVisible = ref(false)
const copyDays = ref([])
let loadNumber = 0
let ruleKey = 0

const week = computed(() => weekdayNames.map((name, index) => ({
  name, weekday: index + 1, rules: draft.value.filter((rule) => rule.weekday === index + 1)
})))
const selectedDay = computed(() => week.value[weekday.value - 1])
const copyTargets = computed(() => week.value.filter((day) => day.weekday !== weekday.value))
const dirty = computed(() => snapshot.value && JSON.stringify(payload(draft.value)) !== snapshot.value.baseline)

function payload(rules) {
  return rules.map(({ id, weekday, startTime, endTime, enabled }) => ({ id, weekday, startTime, endTime, enabled }))
}

function applySnapshot(data) {
  draft.value = data.rules.map((rule) => ({
    ...rule, id: rule.id ?? null, startTime: rule.startTime.slice(0, 5), endTime: rule.endTime.slice(0, 5), key: ++ruleKey
  }))
  snapshot.value = { revision: data.revision, configured: data.configured, baseline: JSON.stringify(payload(draft.value)) }
}

async function loadWeek() {
  const current = ++loadNumber
  loading.value = Boolean(lawyerId.value)
  snapshot.value = null
  draft.value = []
  error.value = ''
  if (!lawyerId.value) return
  try {
    const data = await http.get(endpoint, { params: { lawyerId: lawyerId.value } })
    if (current === loadNumber) applySnapshot(data)
  } catch (failure) {
    if (current === loadNumber) error.value = failure.message
  } finally {
    if (current === loadNumber) loading.value = false
  }
}

async function mayDiscard() {
  if (saving.value) return false
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前有未保存的排班修改，确定放弃吗？', '未保存的排班', { type: 'warning' })
    return true
  } catch {
    return false
  }
}

async function selectLawyer(id) {
  if (id === lawyerId.value || !await mayDiscard()) return
  lawyerId.value = id
  weekday.value = 1
  await loadWeek()
}

async function refresh() {
  if (await mayDiscard()) await loadWeek()
}

function addRule() {
  draft.value.push({ id: null, weekday: weekday.value, startTime: '', endTime: '', enabled: 1, key: ++ruleKey })
}

function removeRule(key) {
  draft.value = draft.value.filter((rule) => rule.key !== key)
}

function toggleDay() {
  const enabled = selectedDay.value.rules.some((rule) => rule.enabled) ? 0 : 1
  for (const rule of selectedDay.value.rules) rule.enabled = enabled
}

function openCopy() {
  copyDays.value = []
  copyVisible.value = true
}

function copySchedule() {
  const source = selectedDay.value.rules
  const copied = copyDays.value.flatMap((day) => source.map((rule) => {
    const match = draft.value.find((item) => item.weekday === day && item.startTime === rule.startTime
      && item.endTime === rule.endTime)
    return { ...rule, id: match?.id ?? null, weekday: day, key: ++ruleKey }
  }))
  draft.value = [...draft.value.filter((rule) => !copyDays.value.includes(rule.weekday)), ...copied]
  copyVisible.value = false
}

function minutes(value) {
  return Number(value.slice(0, 2)) * 60 + Number(value.slice(3))
}

function validationError() {
  if (!snapshot.value.configured && !draft.value.length) return '首次保存请至少保留一个时段。需要关闭时可将时段设为停用。'
  for (const day of week.value) {
    const ordered = [...day.rules].sort((a, b) => a.startTime.localeCompare(b.startTime))
    for (const [index, rule] of ordered.entries()) {
      if (!rule.startTime || !rule.endTime || minutes(rule.endTime) - minutes(rule.startTime) < 60) {
        return `${day.name}的时段需要填写完整，且至少一小时`
      }
      if (index && ordered[index - 1].endTime > rule.startTime) return `${day.name}的时段不能重叠`
    }
  }
  return ''
}

async function saveWeek() {
  if (saving.value || !dirty.value) return
  error.value = validationError()
  if (error.value) return
  saving.value = true
  try {
    const data = await http.put(endpoint, { revision: snapshot.value.revision, rules: payload(draft.value) },
      { params: { lawyerId: lawyerId.value } })
    applySnapshot(data)
    ElMessage.success('整周排班已保存')
  } catch (failure) {
    error.value = failure.message
  } finally {
    saving.value = false
  }
}

onMounted(loadWeek)
onBeforeRouteLeave(mayDiscard)
</script>

<style scoped>
.schedule-toolbar, .schedule-save { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.schedule-picker { display: grid; gap: 6px; width: min(100%, 360px); }
.schedule-alert { margin-top: 16px; }
.week-overview { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 10px; margin: 20px 0; }
.week-day { display: grid; align-content: start; gap: 7px; min-width: 0; min-height: 104px; padding: 10px;
  color: var(--ink); text-align: left; font: inherit; border: 1px solid var(--line); border-radius: var(--radius);
  background: var(--paper-strong); cursor: pointer; }
.week-day:hover, .week-day:focus-visible { border-color: var(--forest); }
.week-day.is-active { background: var(--forest); color: var(--paper-strong); border-color: var(--forest); }
.week-day small { color: var(--muted); }
.week-day.is-active small { color: var(--brass-soft); }
.week-time { font-size: 12px; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.week-time.closed { opacity: 0.65; text-decoration: line-through; }
.day-editor { padding: 16px; border: 1px solid var(--line); border-radius: var(--radius); }
.day-toolbar, .schedule-save { justify-content: space-between; }
.day-toolbar h2 { margin: 0 0 5px; color: var(--forest); font-size: 17px; }
.day-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.day-actions :deep(.el-button + .el-button) { margin-left: 0; }
.time-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--line); }
.time-row:last-child { border-bottom: 0; padding-bottom: 0; }
.time-row label { display: inline-flex; align-items: center; gap: 6px; }
.time-row input { padding: 5px; font: inherit; color: var(--ink); background: var(--paper-strong);
  border: 1px solid var(--line-strong); border-radius: 5px; }
.schedule-save { margin-top: 16px; align-items: center; }
.copy-days { display: flex; flex-wrap: wrap; }
@media (max-width: 1240px) { .week-overview { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
@media (max-width: 720px) { .week-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
