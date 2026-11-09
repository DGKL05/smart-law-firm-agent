<template>
  <div class="floating-agent" :style="agentStyle">
    <transition name="agent-panel">
      <section v-if="open" class="floating-agent-panel" aria-label="智能法律助理">
        <header class="floating-agent-header" @pointerdown="startDrag">
          <div>
            <span class="assistant-kicker">AI LEGAL ASSISTANT</span>
            <h2>智能法律助理</h2>
          </div>
          <div class="assistant-window-actions" @pointerdown.stop>
            <el-button
              :icon="Clock"
              circle
              plain
              title="历史记录"
              :class="{ 'is-active': historyOpen }"
              @click.stop="historyOpen = !historyOpen"
            />
            <el-button :icon="Plus" circle plain title="新会话" @click.stop="createAndActivateSession" />
            <el-button :icon="RefreshLeft" circle plain title="重新开始" @click.stop="createAndActivateSession" />
            <el-button :icon="Close" circle plain title="关闭" @click.stop="open = false" />
          </div>
        </header>

        <div class="floating-agent-body" :class="{ 'history-hidden': !historyOpen }">
          <transition name="history-panel">
            <aside v-if="historyOpen" class="floating-agent-history" aria-label="历史聊天记录">
              <div class="agent-history-head">
                <strong>历史记录</strong>
                <span>{{ orderedSessions.length }} 条</span>
              </div>
              <div class="agent-history-list">
                <article
                  v-for="session in orderedSessions"
                  :key="session.id"
                  class="agent-history-item"
                  :class="{ active: session.id === activeSessionId }"
                  role="button"
                  tabindex="0"
                  @click="switchSession(session.id)"
                  @keydown.enter.prevent="switchSession(session.id)"
                >
                  <div>
                    <strong>{{ session.title }}</strong>
                    <span>{{ formatHistoryTime(session.updatedAt) }}</span>
                  </div>
                  <el-button
                    v-if="isServerSessionId(session.id)"
                    :icon="EditPen"
                    circle
                    text
                    title="重命名会话"
                    @click.stop="renameSession(session)"
                  />
                  <el-button
                    :icon="Delete"
                    circle
                    text
                    title="删除历史记录"
                    @click.stop="deleteSession(session.id)"
                  />
                </article>
              </div>
            </aside>
          </transition>

          <div ref="messagePanel" class="floating-agent-messages">
            <article
              v-for="message in messages"
              :key="message.id"
              class="agent-message"
              :class="message.role"
            >
              <div class="agent-avatar">
                <el-icon v-if="message.role === 'assistant'"><Service /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              <div class="agent-bubble">
                <div class="agent-role">{{ message.role === 'assistant' ? '智能法律助理' : '我' }}</div>
                <p>{{ message.content }}</p>
              </div>
            </article>
            <article v-if="loading" class="agent-message assistant">
              <div class="agent-avatar"><el-icon><Service /></el-icon></div>
              <div class="agent-bubble">
                <div class="agent-role">智能法律助理</div>
                <p>正在连接 Dify Agent...</p>
              </div>
            </article>
          </div>
        </div>

        <div class="floating-agent-examples">
          <button v-for="example in examples" :key="example" type="button" @click="useExample(example)">
            {{ example }}
          </button>
        </div>

        <form class="floating-agent-composer" @submit.prevent="sendMessage">
          <el-input
            v-model="query"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="500"
            placeholder="输入法律问题"
            :disabled="loading"
            @keydown.ctrl.enter.prevent="sendMessage"
          />
          <el-button type="primary" native-type="submit" :loading="loading" :icon="Promotion">
            发送
          </el-button>
        </form>
      </section>
    </transition>

    <button
      class="floating-agent-launcher"
      type="button"
      :aria-expanded="open"
      aria-label="打开智能法律助理"
      @click="toggleOpen"
      @pointerdown="startDrag"
    >
      <span class="launcher-core">
        <el-icon><Service /></el-icon>
      </span>
      <span class="launcher-label">法律助理</span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, Close, Delete, EditPen, Plus, Promotion, RefreshLeft, Service, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { postAiChat } from '../api/http'
import { useAuthStore } from '../stores/auth'

const POSITION_KEY = 'floating-agent-position'
const MAX_HISTORY_COUNT = 30
const defaultPosition = { x: 24, y: 96 }
const auth = useAuthStore()
const router = useRouter()

const position = ref(loadPosition())
const open = ref(false)
const historyOpen = ref(true)
const dragging = ref(false)
const dragStart = ref({ pointerX: 0, pointerY: 0, x: 0, y: 0, moved: false })
const activeSessionId = ref('')
const conversationId = ref('')
const query = ref('')
const loading = ref(false)
const historyLoading = ref(false)
const creatingSession = ref(false)
const messagePanel = ref(null)
const sessions = ref([])
const messages = ref([])

const examples = [
  '我遇到合同违约该怎么处理？',
  '劳动争议需要准备哪些材料？',
  '租房押金纠纷可以怎样处理？'
]

const agentStyle = computed(() => ({
  right: `${position.value.x}px`,
  bottom: `${position.value.y}px`
}))

const orderedSessions = computed(() => [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt))

function resetHistory() {
  sessions.value = [createLocalSession()]
  activateSession(sessions.value[0])
}

async function loadServerSessions({ activateLatest = false } = {}) {
  if (!auth.isLogin || historyLoading.value) return
  const token = auth.token
  historyLoading.value = true
  try {
    const page = await http.get('/api/ai/sessions', { params: { pageNum: 1, pageSize: MAX_HISTORY_COUNT } })
    if (token !== auth.token) return
    const serverSessions = (page.records || []).map(normalizeServerSession)
    if (!serverSessions.length) {
      if (!sessions.value.length || isServerSessionId(activeSessionId.value)) {
        sessions.value = [createLocalSession()]
        activateSession(sessions.value[0])
      }
      return
    }

    sessions.value = serverSessions
    const activeExists = sessions.value.some((session) => session.id === activeSessionId.value)
    const nextSession = activateLatest || !activeExists
      ? orderedSessions.value[0]
      : sessions.value.find((session) => session.id === activeSessionId.value)
    if (nextSession) {
      await switchSession(nextSession.id, { force: true, silent: true })
    }
  } catch (error) {
    if (token !== auth.token) return
    if (!sessions.value.length) {
      resetHistory()
    }
    if (open.value) ElMessage.error(error.message || '读取会话列表失败')
  } finally {
    if (token === auth.token) historyLoading.value = false
  }
}

function normalizeServerSession(session) {
  return {
    id: Number(session.id),
    title: session.title || '新会话',
    conversationId: session.conversationId || '',
    lastMessage: session.lastMessage || '',
    createdAt: parseTime(session.createTime),
    updatedAt: parseTime(session.updateTime),
    messages: []
  }
}

function normalizeServerMessage(message) {
  return {
    id: message.id || createMessageId(),
    role: message.role === 'user' ? 'user' : 'assistant',
    content: message.content || '',
    createdAt: parseTime(message.createTime)
  }
}

function parseTime(value) {
  if (!value) {
    return Date.now()
  }
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? Date.now() : time
}

function writeStorage(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // Storage can fail in private mode; backend history still works.
  }
}

function loadPosition() {
  try {
    const saved = JSON.parse(localStorage.getItem(POSITION_KEY) || 'null')
    if (saved && Number.isFinite(saved.x) && Number.isFinite(saved.y)) {
      return clampPosition(saved)
    }
  } catch {
    // ignore invalid stored position
  }
  return defaultPosition
}

function createLocalSession() {
  const now = Date.now()
  return {
    id: createLocalSessionId(),
    title: '新会话',
    conversationId: '',
    lastMessage: '',
    messages: [createWelcomeMessage()],
    createdAt: now,
    updatedAt: now
  }
}

function createLocalSessionId() {
  return `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function createWelcomeMessage() {
  return {
    id: createMessageId(),
    role: 'assistant',
    content: '你好，我是智能法律助理。登录后可以提问和查看自己的会话记录。'
  }
}

function copyMessages(value) {
  return (value || []).map((message) => ({ ...message }))
}

function persistCurrentSession() {
  const index = sessions.value.findIndex((session) => session.id === activeSessionId.value)
  if (index < 0) {
    return
  }
  const now = Date.now()
  const current = sessions.value[index]
  sessions.value.splice(index, 1, {
    ...current,
    title: current.title === '新会话' ? buildSessionTitle(messages.value) : current.title,
    conversationId: conversationId.value || current.conversationId || '',
    lastMessage: lastMessageText(messages.value),
    messages: copyMessages(messages.value),
    updatedAt: now
  })
}

function buildSessionTitle(sessionMessages) {
  const firstUserMessage = sessionMessages.find((message) => message.role === 'user')
  if (!firstUserMessage) {
    return '新会话'
  }
  return firstUserMessage.content.replace(/\s+/g, ' ').slice(0, 18)
}

function lastMessageText(sessionMessages) {
  const last = [...sessionMessages].reverse().find((message) => message.content)
  return last ? last.content.replace(/\s+/g, ' ').slice(0, 80) : ''
}

function activateSession(session) {
  activeSessionId.value = session.id
  conversationId.value = session.conversationId || ''
  messages.value = copyMessages(session.messages?.length ? session.messages : [createWelcomeMessage()])
  query.value = ''
  scrollToBottom()
}

function toggleOpen() {
  if (dragStart.value.moved) {
    return
  }
  open.value = !open.value
  if (open.value) {
    loadServerSessions()
    scrollToBottom()
  }
}

function startDrag(event) {
  if (event.button !== 0) {
    return
  }
  dragging.value = true
  dragStart.value = {
    pointerX: event.clientX,
    pointerY: event.clientY,
    x: position.value.x,
    y: position.value.y,
    moved: false
  }
  window.addEventListener('pointermove', onDrag)
  window.addEventListener('pointerup', stopDrag, { once: true })
}

function onDrag(event) {
  if (!dragging.value) {
    return
  }
  const deltaX = event.clientX - dragStart.value.pointerX
  const deltaY = event.clientY - dragStart.value.pointerY
  if (Math.abs(deltaX) + Math.abs(deltaY) > 5) {
    dragStart.value.moved = true
  }
  position.value = clampPosition({
    x: dragStart.value.x - deltaX,
    y: dragStart.value.y - deltaY
  })
}

function stopDrag() {
  dragging.value = false
  writeStorage(POSITION_KEY, JSON.stringify(position.value))
  window.removeEventListener('pointermove', onDrag)
  setTimeout(() => {
    dragStart.value.moved = false
  }, 0)
}

function clampPosition(value) {
  const maxX = Math.max(12, window.innerWidth - 86)
  const maxY = Math.max(12, window.innerHeight - 86)
  return {
    x: Math.min(Math.max(value.x, 12), maxX),
    y: Math.min(Math.max(value.y, 12), maxY)
  }
}

function useExample(example) {
  query.value = example
  open.value = true
}

async function createAndActivateSession() {
  if (!requireLogin() || creatingSession.value) return
  if (loading.value || historyLoading.value) {
    ElMessage.warning('当前回复还在生成中，请稍后再新建会话')
    return
  }
  const token = auth.token
  creatingSession.value = true
  try {
    const created = await http.post('/api/ai/sessions', { title: '新会话' })
    if (token !== auth.token) return
    persistCurrentSession()
    const session = normalizeServerSession(created)
    sessions.value = [session, ...sessions.value.filter((item) => isServerSessionId(item.id))]
    activateSession(session)
    historyOpen.value = true
  } catch (error) {
    if (token === auth.token) ElMessage.error(error.message)
  } finally {
    if (token === auth.token) creatingSession.value = false
  }
}

function requireLogin() {
  if (auth.isLogin) return true
  ElMessage.warning('请先登录')
  open.value = false
  router.push('/login')
  return false
}

async function renameSession(session) {
  if (loading.value || !isServerSessionId(session.id) || !requireLogin()) return
  const token = auth.token
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      inputValue: session.title,
      inputValidator: (value) => Boolean(value?.trim() && value.trim().length <= 80) || '请输入 1 到 80 个字符'
    })
    if (token !== auth.token) return
    const updated = await http.put('/api/ai/sessions/' + session.id, { title: value.trim() })
    if (token === auth.token) replaceSession({ ...session, title: updated?.title || value.trim() })
  } catch (error) {
    if (token === auth.token && error?.message) ElMessage.error(error.message)
  }
}

async function switchSession(sessionId, options = {}) {
  const { force = false, silent = false } = options
  if (!force && sessionId === activeSessionId.value) {
    return
  }
  if (!force && loading.value) {
    ElMessage.warning('当前回复还在生成中，请稍后再切换历史记录')
    return
  }
  persistCurrentSession()
  const session = sessions.value.find((item) => item.id === sessionId)
  if (!session) {
    return
  }

  if (isServerSessionId(sessionId)) {
    const token = auth.token
    try {
      const detail = await http.get(`/api/ai/sessions/${sessionId}`)
      if (token !== auth.token) return
      const serverMessages = (detail.messages || []).map(normalizeServerMessage)
      const hydrated = {
        ...session,
        title: detail.title || session.title,
        conversationId: detail.conversationId || '',
        lastMessage: detail.lastMessage || session.lastMessage,
        createdAt: parseTime(detail.createTime),
        updatedAt: parseTime(detail.updateTime),
        messages: serverMessages.length ? serverMessages : [createWelcomeMessage()]
      }
      replaceSession(hydrated)
      activateSession(hydrated)
      return
    } catch (error) {
      if (token !== auth.token) return
      if (!silent) {
        ElMessage.error(error.message || '读取历史记录失败')
      }
    }
  }

  activateSession(session)
}

async function deleteSession(sessionId) {
  if (loading.value && sessionId === activeSessionId.value) {
    ElMessage.warning('当前回复还在生成中，请稍后再删除该会话')
    return
  }
  if (isServerSessionId(sessionId)) {
    const token = auth.token
    try {
      await http.delete(`/api/ai/sessions/${sessionId}`)
      if (token !== auth.token) return
    } catch (error) {
      ElMessage.error(error.message || '删除历史记录失败')
      return
    }
  }
  sessions.value = sessions.value.filter((session) => session.id !== sessionId)
  if (!sessions.value.length) {
    sessions.value = [createLocalSession()]
  }
  if (sessionId === activeSessionId.value) {
    activateSession(orderedSessions.value[0])
  }
}

async function sendMessage() {
  const content = query.value.trim()
  if (!content || loading.value || creatingSession.value || !requireLogin()) {
    return
  }
  messages.value.push({ id: createMessageId(), role: 'user', content })
  const token = auth.token
  query.value = ''
  loading.value = true
  persistCurrentSession()
  await scrollToBottom()
  if (token !== auth.token) return

  try {
    const payload = {
      query: content,
      conversationId: conversationId.value
    }
    if (isServerSessionId(activeSessionId.value)) {
      payload.sessionId = Number(activeSessionId.value)
    }
    const response = await postAiChat(payload)
    if (token !== auth.token) return
    const previousSessionId = activeSessionId.value
    if (response.sessionId) {
      activeSessionId.value = Number(response.sessionId)
    }
    if (response.conversationId) {
      conversationId.value = response.conversationId
    }
    messages.value.push({
      id: createMessageId(),
      role: 'assistant',
      content: response.answer || 'Dify Agent 暂未返回文本内容。'
    })
    upsertActiveSession(previousSessionId)
    await loadServerSessions()
  } catch (error) {
    if (token !== auth.token) return
    const message = error.message || 'Dify Agent 调用失败'
    messages.value.push({ id: createMessageId(), role: 'assistant', content: message })
    ElMessage.error(message)
  } finally {
    if (token === auth.token) {
      loading.value = false
      persistCurrentSession()
      await scrollToBottom()
    }
  }
}

function replaceSession(session) {
  const index = sessions.value.findIndex((item) => item.id === session.id)
  if (index >= 0) {
    sessions.value.splice(index, 1, session)
  } else {
    sessions.value.unshift(session)
  }
}

function upsertActiveSession(previousSessionId) {
  const nextSession = {
    id: activeSessionId.value,
    title: buildSessionTitle(messages.value),
    conversationId: conversationId.value,
    lastMessage: lastMessageText(messages.value),
    messages: copyMessages(messages.value),
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
  sessions.value = sessions.value.filter((session) => session.id !== previousSessionId && session.id !== nextSession.id)
  sessions.value.unshift(nextSession)
}

async function scrollToBottom() {
  await nextTick()
  if (messagePanel.value) {
    messagePanel.value.scrollTop = messagePanel.value.scrollHeight
  }
}

function formatHistoryTime(timestamp) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (sameDay) {
    return `今天 ${time}`
  }
  if (date.toDateString() === yesterday.toDateString()) {
    return `昨天 ${time}`
  }
  return `${date.getMonth() + 1}/${date.getDate()} ${time}`
}

function isServerSessionId(sessionId) {
  return Number.isInteger(sessionId) || (/^\d+$/.test(String(sessionId)) && !String(sessionId).startsWith('local-'))
}

function handleResize() {
  position.value = clampPosition(position.value)
}

watch(() => auth.token, () => {
  loading.value = false
  historyLoading.value = false
  creatingSession.value = false
  resetHistory()
  if (open.value) loadServerSessions({ activateLatest: true })
}, { immediate: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('pointermove', onDrag)
})
</script>
