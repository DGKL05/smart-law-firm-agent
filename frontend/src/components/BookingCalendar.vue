<template>
  <div class="booking-calendar">
    <section class="calendar-ledger" aria-label="预约日期">
      <div class="calendar-heading">
        <div>
          <p class="calendar-kicker">01 · 选择日期</p>
          <h3 class="calendar-month" aria-live="polite">{{ monthLabel }}</h3>
        </div>
        <div class="calendar-navigation">
          <button type="button" aria-label="上个月" :disabled="busy || !canMove(-1)" @click="moveMonth(-1)">
            <ArrowLeft aria-hidden="true" />
          </button>
          <button type="button" aria-label="下个月" :disabled="busy || !canMove(1)" @click="moveMonth(1)">
            <ArrowRight aria-hidden="true" />
          </button>
        </div>
      </div>

      <div ref="grid" class="calendar-grid" role="grid" :aria-label="monthLabel + '预约日历'">
        <div class="calendar-week calendar-weekdays" role="row">
          <span v-for="(day, index) in weekdays" :key="day" role="columnheader" :aria-label="fullWeekdays[index]">
            {{ day }}
          </span>
        </div>
        <div v-for="(week, index) in weeks" :key="index" class="calendar-week" role="row">
          <div v-for="(day, column) in week" :key="day?.date || column" role="gridcell"
            :aria-selected="day ? day.date === selectedDate : undefined">
            <button v-if="day" type="button" class="calendar-day" :class="`day-${day.kind}`"
              :data-date="day.date" :tabindex="day.date === focusedDate ? 0 : -1"
              :aria-current="day.date === today ? 'date' : undefined"
              :aria-label="`${longDate(day.date)}，${day.label}`" :aria-disabled="busy || !day.hasSlots"
              @focus="focusedDate = day.date" @keydown="onDayKey($event, day.date)"
              @click="selectDate(day.date)">
              <span class="day-number">{{ day.number }}</span>
              <span v-if="day.kind !== 'closed'" class="day-status">{{ day.label }}</span>
            </button>
            <span v-else class="calendar-blank" aria-hidden="true"></span>
          </div>
        </div>
      </div>

      <ul class="calendar-legend" aria-label="日期状态说明">
        <li><span class="legend-swatch legend-open"></span>可预约</li>
        <li><span class="legend-swatch legend-full"></span>已约满</li>
        <li><span class="legend-swatch"></span>无可约</li>
      </ul>
    </section>

    <section class="calendar-times" aria-label="预约时段">
      <div class="times-heading" aria-live="polite">
        <p class="calendar-kicker">02 · 选择时段</p>
        <h3>{{ selectedDate ? longDate(selectedDate) : '请选择日期' }}</h3>
        <p>{{ selectedDay ? `${selectedDay.availableCount} 个可约时段` : emptyMessage }}</p>
      </div>

      <fieldset v-if="selectedDay" class="time-options" :disabled="busy">
        <legend class="visually-hidden">选择咨询时间</legend>
        <label v-for="slot in selectedDay.slots" :key="slotKey(slot)" class="time-option"
          :class="{ 'time-unavailable': !slot.available, 'time-selected': selectedSlotKey === slotKey(slot) }">
          <input v-model="selectedSlotKey" type="radio" :name="radioGroupName"
            :value="slotKey(slot)" :disabled="!slot.available" />
          <span class="time-range">{{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}</span>
          <span class="time-status">{{ slot.available ? '可预约' : slot.unavailableReason || '不可预约' }}</span>
        </label>
      </fieldset>
      <p v-else class="times-empty">点击日历中的排班日期，查看当天的咨询时段。</p>

      <div class="times-action">
        <el-button type="primary" class="booking-confirm" :disabled="busy || !selectedSlot"
          :loading="busy" @click="requestBooking">预约所选时段</el-button>
        <p>{{ selectedDay && !selectedDay.availableCount ? '当天无可约时段，请选择其他日期。' : '提交前将再次核对预约时间。' }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, useId, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  slots: { type: Array, default: () => [] },
  busy: Boolean
})
const emit = defineEmits(['book'])
const radioGroupName = 'booking-time-' + useId()
const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const fullWeekdays = weekdays.map((day) => '星期' + day)
const dayFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'UTC', month: 'long', day: 'numeric', weekday: 'long'
})
const today = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date())
const grid = ref(null)
const visibleMonth = ref(today.slice(0, 7))
const selectedDate = ref('')
const focusedDate = ref('')
const selectedSlotKey = ref('')

const daySchedules = computed(() => {
  const schedules = new Map()
  for (const slot of props.slots) {
    if (!isCalendarDate(slot.date) || !slot.startTime || !slot.endTime) continue
    if (!schedules.has(slot.date)) schedules.set(slot.date, { slots: [], availableCount: 0 })
    const day = schedules.get(slot.date)
    day.slots.push(slot)
    if (slot.available) day.availableCount += 1
  }
  for (const day of schedules.values()) {
    day.slots.sort((first, second) => first.startTime.localeCompare(second.startTime))
  }
  return schedules
})
const scheduledDates = computed(() => [...daySchedules.value.keys()].sort())
const firstMonth = computed(() => scheduledDates.value[0]?.slice(0, 7) || today.slice(0, 7))
const lastMonth = computed(() => scheduledDates.value.at(-1)?.slice(0, 7) || firstMonth.value)
const selectedDay = computed(() => daySchedules.value.get(selectedDate.value))
const selectedSlot = computed(() =>
  selectedDay.value?.slots.find((slot) => slot.available && slotKey(slot) === selectedSlotKey.value)
)
const emptyMessage = computed(() => scheduledDates.value.length ? '当前月份暂无开放时段' : '暂无开放时段')
const monthLabel = computed(() => {
  const [year, month] = visibleMonth.value.split('-')
  return `${year}年${Number(month)}月`
})
const weeks = computed(() => {
  const cells = Array(weekOffset(visibleMonth.value + '-01')).fill(null)
  for (let number = 1; number <= daysInMonth(visibleMonth.value); number += 1) {
    const date = `${visibleMonth.value}-${String(number).padStart(2, '0')}`
    const day = daySchedules.value.get(date)
    const count = day?.availableCount || 0
    const fullyBooked = day?.slots.every((slot) => ['已预约', '已约满', '已有预约'].includes(slot.unavailableReason))
    const kind = count ? 'open' : fullyBooked ? 'full' : day ? 'unavailable' : 'closed'
    const label = count ? `${count} 个可约` : fullyBooked ? '已约满' : day ? '无可约' : '未开放'
    cells.push({ date, number, kind, label, hasSlots: Boolean(day) })
  }
  while (cells.length % 7) cells.push(null)
  return Array.from({ length: cells.length / 7 }, (_, row) => cells.slice(row * 7, row * 7 + 7))
})

watch(daySchedules, () => {
  if (!selectedDate.value || !daySchedules.value.has(selectedDate.value)) {
    const visible = scheduledDates.value.filter((date) => date.startsWith(visibleMonth.value))
    const dates = selectedDate.value && visible.length ? visible : scheduledDates.value
    selectedDate.value = preferredDate(dates)
    visibleMonth.value = selectedDate.value.slice(0, 7) || firstMonth.value
    focusedDate.value = selectedDate.value || visibleMonth.value + '-01'
  }
  if (!selectedSlot.value) selectedSlotKey.value = ''
}, { immediate: true })

function isCalendarDate(date) {
  if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return false
  const value = utcDate(date)
  return Number.isFinite(value.getTime()) && value.toISOString().slice(0, 10) === date
}

function utcDate(date) {
  return new Date(date + 'T00:00:00Z')
}

function weekOffset(date) {
  return (utcDate(date).getUTCDay() + 6) % 7
}

function daysInMonth(month) {
  const [year, number] = month.split('-').map(Number)
  return new Date(Date.UTC(year, number, 0)).getUTCDate()
}

function offsetMonth(month, delta) {
  const date = utcDate(month + '-01')
  date.setUTCMonth(date.getUTCMonth() + delta)
  return date.toISOString().slice(0, 7)
}

function preferredDate(dates) {
  return dates.find((date) => daySchedules.value.get(date)?.availableCount > 0) || dates[0] || ''
}

function selectDate(date) {
  if (props.busy || !daySchedules.value.has(date) || selectedDate.value === date) return
  selectedDate.value = date
  selectedSlotKey.value = ''
}

function canMove(delta) {
  return isVisibleMonth(offsetMonth(visibleMonth.value, delta))
}

function isVisibleMonth(month) {
  return month >= firstMonth.value && month <= lastMonth.value
}

function showMonth(month) {
  visibleMonth.value = month
  selectedDate.value = preferredDate(scheduledDates.value.filter((date) => date.startsWith(month)))
  focusedDate.value = selectedDate.value || month + '-01'
  selectedSlotKey.value = ''
}

function moveMonth(delta) {
  if (props.busy || !canMove(delta)) return
  showMonth(offsetMonth(visibleMonth.value, delta))
}

async function onDayKey(event, date) {
  if (props.busy) return
  const day = utcDate(date)
  const offsets = {
    ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7,
    Home: -weekOffset(date), End: 6 - weekOffset(date)
  }
  if (Object.hasOwn(offsets, event.key)) {
    event.preventDefault()
    day.setUTCDate(day.getUTCDate() + offsets[event.key])
    const target = day.toISOString().slice(0, 10)
    const month = target.slice(0, 7)
    if (!isVisibleMonth(month)) return
    if (month !== visibleMonth.value) showMonth(month)
    focusedDate.value = target
  } else if (event.key === 'PageUp' || event.key === 'PageDown') {
    event.preventDefault()
    moveMonth(event.key === 'PageUp' ? -1 : 1)
  } else {
    return
  }
  await nextTick()
  grid.value?.querySelector(`[data-date="${focusedDate.value}"]`)?.focus()
}

function longDate(date) {
  return dayFormatter.format(utcDate(date))
}

function formatTime(value) {
  return value.includes('T') ? value.slice(11, 16) : value.slice(0, 5)
}

function slotKey(slot) {
  return slot.startTime + '|' + slot.endTime
}

function requestBooking() {
  if (!props.busy && selectedSlot.value) emit('book', selectedSlot.value)
}
</script>

<style scoped>
.booking-calendar {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(15rem, 1fr);
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper-strong);
}

.calendar-ledger, .calendar-times { min-width: 0; padding: clamp(.85rem, 2vw, 1.55rem); }
.calendar-heading { display: flex; align-items: center; justify-content: space-between; gap: .75rem; margin-bottom: 1rem; }
.calendar-kicker { margin: 0 0 .25rem; color: var(--forest); font-size: .75rem; font-weight: 700; letter-spacing: .13em; }
.calendar-month { margin: 0; font: 700 clamp(1.45rem, 3vw, 2.15rem)/1.15 var(--font-display); color: var(--forest); }
.calendar-navigation { display: flex; gap: .4rem; }
.calendar-navigation button {
  display: grid; place-items: center; width: 2.5rem; height: 2.5rem;
  border: 1px solid var(--line); border-radius: 5px; color: var(--forest); background: var(--paper-strong); cursor: pointer;
}
.calendar-navigation button svg { width: 1rem; height: 1rem; }
.calendar-navigation button:disabled { opacity: .38; cursor: default; }
.calendar-navigation button:hover:not(:disabled) { border-color: var(--forest); background: #edf2eb; }
.calendar-grid { display: grid; gap: .3rem; }
.calendar-week { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: .3rem; }
.calendar-weekdays { text-align: center; color: var(--ink-soft); font-size: .78rem; font-weight: 700; padding: .2rem 0 .4rem; }
.calendar-day, .calendar-blank { width: 100%; min-height: 3.5rem; }
.calendar-day {
  display: flex; flex-direction: column; align-items: flex-start; justify-content: space-between; gap: .15rem;
  padding: .42rem .45rem; border: 1px solid #e1ddcf; border-radius: 5px;
  color: var(--ink-soft); background: #f7f4eb; text-align: left; font: inherit; cursor: pointer;
}
.calendar-day[aria-disabled="true"] { cursor: default; }
.day-number { font: 700 1.13rem/1 var(--font-display); font-variant-numeric: tabular-nums; }
.day-status { font-size: .68rem; font-weight: 700; overflow-wrap: anywhere; }
.day-open { background: #eaf2eb; border-color: #d4e3d6; color: var(--forest); }
.day-full { background: #f3ece0; border-color: #e6dac6; color: #6e542e; }
.day-closed { border-color: transparent; color: #5c6257; background: #f7f5ed; }
.calendar-blank { display: block; }
.calendar-day[aria-current="date"] { box-shadow: inset 0 -2px 0 var(--brass); }
.calendar-day:hover:not([aria-disabled="true"]) { border-color: var(--forest); }
[aria-selected="true"] .calendar-day { color: var(--paper-strong); background: var(--forest); border-color: var(--forest); }
.calendar-navigation button:focus-visible, .calendar-day:focus-visible, .time-option:has(input:focus-visible) {
  outline: 2px solid var(--brass); outline-offset: 2px;
}
.calendar-legend {
  display: flex; flex-wrap: wrap; gap: .45rem 1rem; margin: .9rem 0 0; padding: 0;
  list-style: none; color: var(--ink-soft); font-size: .76rem;
}
.calendar-legend li { display: flex; align-items: center; gap: .35rem; }
.legend-swatch { width: .7rem; height: .7rem; border: 1px solid var(--line); border-radius: 2px; background: #f7f4eb; }
.legend-open { background: #eaf2eb; border-color: #9dbfa8; }
.legend-full { background: #f3ece0; border-color: #c5ad87; }
.calendar-times {
  display: flex; flex-direction: column; gap: 1rem; border-left: 1px solid var(--line); background: #f6f3e9;
}
.times-heading h3 { margin: 0; color: var(--forest); font: 700 1.26rem/1.25 var(--font-display); }
.times-heading p:last-child, .times-action p, .times-empty { margin: .35rem 0 0; color: var(--ink-soft); font-size: .78rem; }
.time-options { min-width: 0; display: grid; gap: .45rem; margin: 0; padding: 0; border: 0; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.time-option {
  display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: .55rem;
  min-height: 2.7rem; padding: .48rem .6rem; border: 1px solid var(--line); border-radius: 5px;
  color: var(--ink); background: var(--paper-strong); cursor: pointer;
}
.time-option input { margin: 0; accent-color: var(--forest); cursor: inherit; }
.time-range { font-weight: 700; font-variant-numeric: tabular-nums; white-space: nowrap; }
.time-status { color: var(--forest); font-size: .73rem; text-align: right; overflow-wrap: anywhere; }
.time-selected { border-color: var(--forest); background: #eaf2eb; }
.time-unavailable { color: var(--ink-soft); background: #f4f0e5; cursor: default; }
.time-unavailable .time-status { color: var(--ink-soft); }
.times-action { margin-top: auto; padding-top: .2rem; }
.booking-confirm { width: 100%; min-height: 2.75rem; }

@media (max-width: 760px) {
  .booking-calendar { grid-template-columns: minmax(0, 1fr); }
  .calendar-times { border-top: 1px solid var(--line); border-left: 0; }
}

@media (max-width: 460px) {
  .calendar-ledger, .calendar-times { padding: .7rem; }
  .calendar-grid, .calendar-week { gap: .18rem; }
  .calendar-day, .calendar-blank { min-height: 2.75rem; }
  .calendar-day { padding: .32rem .13rem; align-items: center; text-align: center; }
  .day-status { font-size: .56rem; }
  .time-range { font-size: .87rem; }
}
</style>
