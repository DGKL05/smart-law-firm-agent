<template>
  <section class="page agent-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">智能法律助理</h1>
        <div class="muted">接入云端 Dify Agent，可通过对话完成法律咨询、预约咨询和取消预约</div>
      </div>
      <el-button plain :icon="RefreshLeft" @click="resetConversation">新会话</el-button>
    </div>

    <el-card class="agent-card" shadow="never">
      <div ref="messagePanel" class="agent-messages">
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

      <div class="agent-examples">
        <el-button
          v-for="example in examples"
          :key="example"
          size="small"
          plain
          @click="useExample(example)"
        >
          {{ example }}
        </el-button>
      </div>

      <form class="agent-composer" @submit.prevent="sendMessage">
        <el-input
          v-model="query"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="500"
          show-word-limit
          placeholder="输入你的法律问题，或直接说：帮我预约 2026-06-29 15:00 在北京明德律师事务所找张明律师咨询"
          :disabled="loading"
        />
        <el-button type="primary" native-type="submit" :loading="loading" :icon="Promotion">
          发送
        </el-button>
      </form>
    </el-card>
  </section>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'

const conversationId = ref(localStorage.getItem('difyConversationId') || '')
const query = ref('')
const loading = ref(false)
const messagePanel = ref(null)
const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '你好，我是智能法律助理。你可以描述法律问题，也可以让我帮你预约或取消法律咨询。'
  }
])

const examples = [
  '查询《民法典》第五百七十七条原文',
  '帮我预约 2026-06-29 15:00 在北京明德律师事务所找张明律师咨询',
  '取消 2026-06-29 15:00 北京明德律师事务所张明律师的法律咨询预约'
]

function useExample(example) {
  query.value = example
}

function resetConversation() {
  conversationId.value = ''
  localStorage.removeItem('difyConversationId')
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: '已开启新会话。'
    }
  ]
}

async function sendMessage() {
  const content = query.value.trim()
  if (!content || loading.value) {
    return
  }
  messages.value.push({ id: Date.now(), role: 'user', content })
  query.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const response = await http.post('/api/me/agent/chat', {
      query: content,
      conversationId: conversationId.value
    })
    if (response.conversationId) {
      conversationId.value = response.conversationId
      localStorage.setItem('difyConversationId', response.conversationId)
    }
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer || 'Dify Agent 暂未返回文本内容。'
    })
  } catch (error) {
    const message = error.message || 'Dify Agent 调用失败'
    messages.value.push({ id: Date.now() + 1, role: 'assistant', content: message })
    ElMessage.error(message)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagePanel.value) {
    messagePanel.value.scrollTop = messagePanel.value.scrollHeight
  }
}
</script>

<style scoped>
.agent-card {
  max-width: 980px;
}

.agent-messages {
  height: 520px;
  overflow-y: auto;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fffdf7;
}

.agent-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.agent-message.user {
  flex-direction: row-reverse;
}

.agent-avatar {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: #fffdf7;
  background: var(--forest);
}

.agent-message.user .agent-avatar {
  background: var(--brass);
}

.agent-bubble {
  max-width: min(720px, calc(100% - 48px));
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f8f3e8;
}

.agent-message.user .agent-bubble {
  background: #e9f2ef;
}

.agent-role {
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 12px;
}

.agent-bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}

.agent-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0;
}

.agent-composer {
  display: grid;
  grid-template-columns: 1fr 108px;
  gap: 12px;
  align-items: stretch;
}

.agent-composer .el-button {
  height: auto;
}

@media (max-width: 760px) {
  .agent-messages {
    height: 460px;
  }

  .agent-composer {
    grid-template-columns: 1fr;
  }
}
</style>
