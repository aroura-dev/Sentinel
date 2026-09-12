<template>
  <div class="cs-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="cs-header">
          <div class="cs-header-title">
            <span class="cs-badge">客</span>
            <span>客服智能路由</span>
          </div>
          <div class="cs-header-right">
            <el-tag v-if="currentBuyer" size="small" effect="plain">当前会话 {{ currentBuyer }}</el-tag>
            <span class="cs-online"><i class="cs-dot"></i>在线</span>
          </div>
        </div>
      </template>

      <div class="cs-console">
        <!-- 左侧：会话列表（按收件人分组） -->
        <div class="cs-side">
          <div class="cs-side-title">
            会话列表
            <el-tag v-if="sessions.length" size="small" round>{{ sessions.length }}</el-tag>
          </div>
          <div v-if="!sessions.length" class="cs-side-empty">暂无会话</div>
          <div
            v-for="s in sessions"
            :key="s.buyerId"
            class="cs-session"
            :class="{ active: currentBuyer === s.buyerId }"
            @click="openSession(s)"
          >
            <div class="cs-session-top">
              <span class="cs-session-buyer">{{ s.buyerId }}</span>
              <span class="cs-session-time">{{ fmtTime(s.items[0].timestamp) }}</span>
            </div>
            <div class="cs-session-preview">{{ s.items[0].message }}</div>
            <div class="cs-session-tags">
              <el-tag
                v-if="s.items[0].intent"
                :type="intentMeta[s.items[0].intent]?.type || 'info'"
                size="small"
                effect="light"
              >{{ intentLabel(s.items[0].intent) }}</el-tag>
              <el-tag
                v-if="s.items[0].route"
                :type="routeMeta[s.items[0].route]?.type || 'info'"
                size="small"
                effect="plain"
              >{{ routeLabel(s.items[0].route) }}</el-tag>
            </div>
          </div>
        </div>

        <!-- 右侧：对话窗口 + 输入区 -->
        <div class="cs-main">
          <div ref="chatBox" class="chat-box">
            <div v-if="!messages.length" class="cs-welcome">
              <div class="cs-welcome-logo">
                <el-icon :size="44"><Headset /></el-icon>
              </div>
              <div class="cs-welcome-title">您好，有什么可以帮您？</div>
              <div class="cs-welcome-sub">我是客服智能助手，可以为您解答包裹物流、订单、运费等问题</div>
              <div class="cs-suggestions">
                <div v-for="s in samples" :key="s" class="cs-suggestion" @click="quickSend(s)">
                  <span>{{ s }}</span>
                  <span class="cs-suggestion-icon">→</span>
                </div>
              </div>
            </div>

            <div v-for="(m, i) in messages" :key="i" class="chat-msg" :class="m.role">
              <div class="chat-avatar" :class="m.role">{{ m.role === 'bot' ? '客' : '买' }}</div>
              <div class="chat-body">
                <div class="chat-meta">
                  <span class="chat-name">{{ m.role === 'bot' ? '客服助手' : m.who }}</span>
                  <span class="chat-time">{{ fmtTime(m.time) }}</span>
                </div>
                <div v-if="m.role === 'bot'" class="chat-tags">
                  <el-tag
                    v-if="m.intent"
                    :type="intentMeta[m.intent]?.type || 'info'"
                    size="small"
                    effect="light"
                  >{{ intentLabel(m.intent) }}</el-tag>
                  <el-tag
                    v-if="m.route"
                    :type="routeMeta[m.route]?.type || 'info'"
                    size="small"
                    effect="plain"
                  >{{ routeLabel(m.route) }}</el-tag>
                </div>
                <div class="chat-bubble">{{ m.text }}</div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input v-model="buyerId" placeholder="收件人 ID" style="width: 140px; margin-right: 8px" />
            <el-input v-model="message" placeholder="输入收件人咨询，如：我的包裹到哪了" @keyup.enter="send" />
            <el-button type="primary" :loading="loading" @click="send">发送</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { csChat, csHistory } from '../../api'

const intentMeta = {
  query_track: { label: '查轨迹', type: 'primary' },
  complaint: { label: '投诉', type: 'danger' },
  refund: { label: '退款', type: 'warning' },
  other: { label: '其他', type: 'info' }
}
const routeMeta = {
  auto: { label: '自动回复', type: 'success' },
  human: { label: '转人工', type: 'danger' }
}
const intentLabel = (k) => (intentMeta[k] && intentMeta[k].label) || k || '未知'
const routeLabel = (k) => (routeMeta[k] && routeMeta[k].label) || k || ''

const buyerId = ref('buyer001')
const message = ref('')
const loading = ref(false)
const messages = ref([])
const sessions = ref([])
const currentBuyer = ref(null)
const chatBox = ref(null)

const samples = ['我的包裹到哪了', '查一下订单运费', '我的订单预计多久能到']

function fmtTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = (n) => String(n).padStart(2, '0')
  const hm = `${p(d.getHours())}:${p(d.getMinutes())}`
  const today = new Date()
  if (d.getFullYear() === today.getFullYear() && d.getMonth() === today.getMonth() && d.getDate() === today.getDate()) {
    return hm
  }
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${hm}`
}

/** 历史按收件人分组为会话，最近活跃在前 */
function buildSessions(list) {
  const map = new Map()
  for (const r of list) {
    if (!map.has(r.buyerId)) map.set(r.buyerId, { buyerId: r.buyerId, items: [] })
    map.get(r.buyerId).items.push(r)
  }
  const arr = Array.from(map.values())
  arr.sort((a, b) => (b.items[0].timestamp || 0) - (a.items[0].timestamp || 0))
  return arr
}

/** 把某个会话的多轮记录展开为「提问 + 回复」的消息线程（时间正序） */
function buildThread(items) {
  const msgs = []
  for (const it of [...items].reverse()) {
    msgs.push({ role: 'user', who: it.buyerId, text: it.message, time: it.timestamp })
    msgs.push({ role: 'bot', who: '客服助手', text: it.reply, time: it.timestamp, intent: it.intent, route: it.route })
  }
  return msgs
}

function scrollToBottom() {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}

/** 点击会话：载入该收件人全部对话 */
function openSession(s) {
  currentBuyer.value = s.buyerId
  buyerId.value = s.buyerId
  messages.value = buildThread(s.items)
  nextTick(scrollToBottom)
}

function quickSend(text) {
  message.value = text
  send()
}

async function send() {
  if (!message.value) return
  loading.value = true
  messages.value.push({ role: 'user', who: buyerId.value, text: message.value, time: Date.now() })
  try {
    const res = await csChat({ message: message.value, buyerId: buyerId.value })
    messages.value.push({
      role: 'bot',
      who: '客服助手',
      text: res.reply,
      time: Date.now(),
      intent: res.intent,
      route: res.route
    })
    currentBuyer.value = buyerId.value
    message.value = ''
    nextTick(scrollToBottom)
    await loadHistory()
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  try {
    const list = (await csHistory()) || []
    sessions.value = buildSessions(list)
  } catch (e) {
    // 已提示
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.cs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cs-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.cs-badge {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #0891b2;
  color: #fff;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.cs-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cs-online {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #67c23a;
}
.cs-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
}

.cs-console {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

/* 左侧会话列表 */
.cs-side {
  width: 264px;
  flex-shrink: 0;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  max-height: 480px;
  overflow-y: auto;
}
.cs-side-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 4px 6px 10px;
}
.cs-side-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 30px 0;
}
.cs-session {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background .2s, box-shadow .2s;
}
.cs-session:hover {
  background: #fff;
}
.cs-session.active {
  background: #fff;
  box-shadow: inset 0 0 0 2px #0891b2;
}
.cs-session-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cs-session-buyer {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.cs-session-time {
  font-size: 12px;
  color: #c0c4cc;
}
.cs-session-preview {
  font-size: 12px;
  color: #909399;
  margin: 4px 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cs-session-tags {
  display: flex;
  gap: 4px;
}

/* 右侧对话窗口 */
.cs-main {
  flex: 1;
  min-width: 0;
}
.chat-box {
  height: 400px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.cs-welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}
.cs-welcome-logo {
  color: #0891b2;
  margin-bottom: 4px;
}
.cs-welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.cs-welcome-sub {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.cs-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}
.cs-suggestion {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  font-size: 14px;
  color: #0891b2;
  cursor: pointer;
  transition: all .2s;
}
.cs-suggestion:hover {
  border-color: #0891b2;
  box-shadow: 0 2px 8px rgba(8, 145, 178, .15);
  transform: translateY(-1px);
}
.cs-suggestion-icon {
  color: #c0c4cc;
}

.chat-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.chat-msg.user {
  flex-direction: row-reverse;
}
.chat-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;
}
.chat-avatar.bot {
  background: #0891b2;
}
.chat-avatar.user {
  background: #67c23a;
}
.chat-body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}
.chat-msg.user .chat-body {
  align-items: flex-end;
}
.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.chat-msg.user .chat-meta {
  flex-direction: row-reverse;
}
.chat-name {
  font-size: 12px;
  color: #909399;
}
.chat-time {
  font-size: 12px;
  color: #c0c4cc;
}
.chat-tags {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}
.chat-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.chat-msg.user .chat-bubble {
  background: #0891b2;
  color: #fff;
}
.chat-msg.bot .chat-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e6e6e6;
}

.chat-input {
  display: flex;
}
</style>
