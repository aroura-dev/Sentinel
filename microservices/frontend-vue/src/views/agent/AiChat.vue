<template>
  <div class="aichat-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="ac-header">
          <div class="ac-header-title">
            <span class="ac-badge">智</span>
            <span>智能问答</span>
            <el-tag size="small" effect="plain">物流知识与业务轨迹</el-tag>
          </div>
          <div class="ac-header-right">
            <span class="ac-online"><i class="ac-dot"></i>AI 在线</span>
          </div>
        </div>
      </template>

      <!-- 服务不可用空态 -->
      <el-alert
        v-if="serviceDown"
        type="warning"
        :closable="false"
        show-icon
        title="AI 服务建设中"
        description="智能问答功能正在接入，暂不可用。您可以先使用「智能客服」或「物流跟踪」查询包裹信息。"
        class="ac-down-alert"
      />

      <div class="ac-main">
        <div ref="chatBox" class="ac-chat">
          <div v-if="!messages.length" class="ac-welcome">
            <div class="ac-welcome-logo"><el-icon :size="44"><ChatDotRound /></el-icon></div>
            <div class="ac-welcome-title">您好，想了解什么物流问题？</div>
            <div class="ac-welcome-sub">可以问渠道时效、运费规则、异常处理，也可以带上订单号查包裹进展</div>
            <div class="ac-suggestions">
              <div v-for="s in samples" :key="s" class="ac-suggestion" @click="quickSend(s)">
                <span>{{ s }}</span><span class="ac-suggestion-icon">→</span>
              </div>
            </div>
          </div>

          <div v-for="(m, i) in messages" :key="i" class="ac-msg" :class="m.role">
            <div class="ac-avatar" :class="m.role">{{ m.role === 'bot' ? '智' : '我' }}</div>
            <div class="ac-body">
              <div class="ac-meta">
                <span class="ac-name">{{ m.role === 'bot' ? '智能助手' : '我' }}</span>
                <span class="ac-time">{{ fmtTime(m.time) }}</span>
              </div>
              <el-alert
                v-if="m.role === 'bot' && m.degraded"
                type="warning" :closable="false" show-icon class="ac-degraded"
                title="AI 服务暂不可用，以下为知识库匹配结果"
              />
              <div class="ac-bubble">{{ m.text }}</div>
              <div v-if="m.role === 'bot' && m.business && m.business.length" class="ac-biz">
                <div class="ac-biz-title">包裹信息</div>
                <div v-for="(b, j) in m.business" :key="j" class="ac-biz-item">
                  <el-tag size="small" effect="plain">{{ b.order_no }}</el-tag>
                  <span>当前：{{ b.current_node }}</span>
                  <span v-if="b.track_summary" class="ac-biz-track">{{ b.track_summary }}</span>
                </div>
              </div>
              <div v-if="m.role === 'bot' && m.sources && m.sources.length" class="ac-sources">
                <div class="ac-sources-title">参考依据</div>
                <el-collapse v-model="m.openSources">
                  <el-collapse-item
                    v-for="(s, j) in m.sources"
                    :key="j"
                    :name="j"
                    :title="`${s.type || s.status_code || '知识'} · ${s.status_code || ''}`"
                  >
                    <div class="ac-source-desc">{{ s.description }}</div>
                    <div class="ac-source-sugg">处理建议：{{ s.suggestion }}</div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
        </div>

        <div class="ac-input">
          <el-input v-model="orderNo" placeholder="订单号（可选，查询包裹进展）" style="width: 170px; margin-right: 8px" />
          <el-input
            v-model="question"
            placeholder="输入问题，如：中通到广东一般几天到？或 我的货卡在中转怎么办？"
            :disabled="serviceDown"
            @keyup.enter="send"
          />
          <el-button type="primary" :loading="loading" :disabled="serviceDown" @click="send">提问</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { agentChat } from '../../api'

const question = ref('')
const orderNo = ref('')
const loading = ref(false)
const serviceDown = ref(false)
const messages = ref([])
const chatBox = ref(null)

const samples = ['中通到广东一般几天能到？', '包裹卡在中转/延误怎么办？', '运费怎么算，首重续重是什么？', '带订单号查一下我的包裹进展']

function fmtTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

function scrollToBottom() {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}

function quickSend(text) {
  question.value = text
  send()
}

async function send() {
  if (!question.value) return
  loading.value = true
  messages.value.push({ role: 'user', text: question.value, time: Date.now() })
  try {
    const res = await agentChat({ question: question.value, order_no: orderNo.value || null })
    messages.value.push({
      role: 'bot',
      text: res.answer,
      time: Date.now(),
      degraded: !!res.degraded,
      sources: res.sources || [],
      business: res.business || [],
      openSources: []
    })
    question.value = ''
    nextTick(scrollToBottom)
  } catch (e) {
    serviceDown.value = true
    // 拦截器已提示，这里把输入还原可重试
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ac-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ac-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.ac-badge {
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
.ac-header-right {
  display: flex;
  align-items: center;
}
.ac-online {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #67c23a;
}
.ac-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
}
.ac-down-alert {
  margin-bottom: 12px;
}

.ac-main {
  min-width: 0;
}
.ac-chat {
  height: 460px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.ac-welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}
.ac-welcome-logo {
  color: #0891b2;
  margin-bottom: 4px;
}
.ac-welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.ac-welcome-sub {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.ac-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}
.ac-suggestion {
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
.ac-suggestion:hover {
  border-color: #0891b2;
  box-shadow: 0 2px 8px rgba(8, 145, 178, .15);
  transform: translateY(-1px);
}
.ac-suggestion-icon {
  color: #c0c4cc;
}

.ac-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.ac-msg.user {
  flex-direction: row-reverse;
}
.ac-avatar {
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
.ac-avatar.bot {
  background: #0891b2;
}
.ac-avatar.user {
  background: #67c23a;
}
.ac-body {
  max-width: 74%;
  display: flex;
  flex-direction: column;
}
.ac-msg.user .ac-body {
  align-items: flex-end;
}
.ac-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.ac-msg.user .ac-meta {
  flex-direction: row-reverse;
}
.ac-name {
  font-size: 12px;
  color: #909399;
}
.ac-time {
  font-size: 12px;
  color: #c0c4cc;
}
.ac-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}
.ac-msg.user .ac-bubble {
  background: #0891b2;
  color: #fff;
}
.ac-msg.bot .ac-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e6e6e6;
}
.ac-degraded {
  margin-bottom: 6px;
}

.ac-biz {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  margin-top: 6px;
}
.ac-biz-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 6px;
}
.ac-biz-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}
.ac-biz-track {
  color: #909399;
  font-size: 12px;
}

.ac-sources {
  margin-top: 6px;
}
.ac-sources-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 4px;
}
.ac-source-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.ac-source-sugg {
  font-size: 13px;
  color: #0891b2;
  margin-top: 4px;
}

.ac-input {
  display: flex;
}
</style>
