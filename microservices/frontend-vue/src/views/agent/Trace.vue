<template>
  <div class="trace-page">
    <el-card shadow="never" class="trace-search">
      <div class="ts-row">
        <div class="ts-label">TraceId</div>
        <el-input v-model="traceId" placeholder="输入 TraceId（订单号或完整链路号）" clearable style="width: 380px" @keyup.enter="load" />
        <el-button type="primary" @click="load">追溯</el-button>
        <el-button @click="loadLatest">最近链路</el-button>
      </div>
      <div v-if="list.length" class="ts-scene">
        <el-icon><i class="el-icon-connection" /></el-icon>
        <span>调用场景：{{ friendlyTraceId(traceId) }}</span>
      </div>
    </el-card>

    <el-card shadow="never" class="trace-body">
      <template #header>
        <div class="ac-head">
          <div class="ac-title">AI 链路明细</div>
          <el-tag v-if="list.length" type="success" effect="light" round>共 {{ list.length }} 步</el-tag>
        </div>
      </template>

      <div v-if="list.length" class="tl">
        <div v-for="(item, idx) in list" :key="item.id" class="tl-node">
          <div class="tl-rail">
            <div class="tl-dot" :class="'dot-' + (item.status || 'info')">{{ idx + 1 }}</div>
            <div v-if="idx < list.length - 1" class="tl-line" />
          </div>
          <div class="tl-card">
            <div class="tl-head">
              <span class="tl-agent">{{ agentLabel(item.agent_name) }}</span>
              <el-tag size="small" effect="light" round :type="statusMeta(item.status).type">{{ statusMeta(item.status).text }}</el-tag>
            </div>
            <div class="tl-sub">{{ item.agent_name }} · 第 {{ idx + 1 }} 步</div>
            <div class="tl-meta">
              <span v-if="acts(item).length" class="tl-m"><span class="tl-k">动作</span>{{ acts(item).join('、') }}</span>
              <span class="tl-m"><span class="tl-k">Token</span>{{ item.token_usage ?? '-' }}</span>
              <span class="tl-m"><span class="tl-k">耗时</span>{{ item.latency_ms != null ? item.latency_ms + ' ms' : '-' }}</span>
            </div>
            <div v-if="item.input" class="tl-io">
              <details>
                <summary>查看输入 / 输出</summary>
                <pre>{{ prettyJson(item.input) }}</pre>
                <pre>{{ prettyJson(item.output) }}</pre>
              </details>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else-if="traceId" description="未找到该 TraceId 的链路数据" />
      <el-empty v-else description="暂无链路数据，点击「最近链路」自动加载" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { agentTrace, agentTraceLatest } from '../../api'
import { agentLabel, statusMeta, toolNames, friendlyTraceId } from '../../utils/agentLabel'

const route = useRoute()
const traceId = ref(route.query.traceId || '')
const list = ref([])

async function load() {
  if (!traceId.value) {
    list.value = []
    return
  }
  list.value = (await agentTrace(traceId.value)) || []
}

async function loadLatest() {
  try {
    const d = (await agentTraceLatest()) || {}
    const id = d.traceId || ''
    if (!id) {
      list.value = []
      return
    }
    traceId.value = id
    await load()
  } catch {
    list.value = []
  }
}

function acts(item) {
  return toolNames(item.tools_called)
}

function prettyJson(v) {
  if (!v) return '-'
  try {
    return JSON.stringify(JSON.parse(v), null, 2)
  } catch {
    return String(v)
  }
}

watch(traceId, load)
onMounted(() => {
  if (traceId.value) return load()
  loadLatest()
})
</script>

<style scoped>
.trace-page { display: flex; flex-direction: column; gap: 16px; }
.trace-search { border: 1px solid #eef1f4; border-radius: 10px; }
.ts-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.ts-label { font-size: 13px; color: #57606a; font-weight: 600; }
.ts-scene { margin-top: 12px; padding: 8px 12px; background: #f0f7fb; border: 1px solid #dcebf4; border-radius: 8px; color: #0b7fa0; font-size: 13px; display: flex; align-items: center; gap: 6px; word-break: break-all; }
.trace-body { border: 1px solid #eef1f4; border-radius: 10px; }
.ac-head { display: flex; align-items: center; justify-content: space-between; }
.ac-title { font-size: 15px; color: #1d2129; font-weight: 600; }

.tl { padding: 8px 4px 4px; }
.tl-node { display: flex; gap: 16px; }
.tl-rail { width: 34px; display: flex; flex-direction: column; align-items: center; }
.tl-dot { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 13px; font-weight: 700; flex-shrink: 0; box-shadow: 0 3px 8px rgba(37, 99, 168, 0.18); }
.dot-success { background: linear-gradient(135deg, #12b981, #0c8f62); }
.dot-degraded { background: linear-gradient(135deg, #f0a020, #d98018); }
.dot-failed { background: linear-gradient(135deg, #e05252, #c0392b); }
.dot-timeout { background: linear-gradient(135deg, #5aa2c4, #3d7ea8); }
.dot-info { background: linear-gradient(135deg, #7f8fa6, #57606a); }
.tl-line { width: 2px; flex: 1; min-height: 18px; background: #e3eaf0; margin: 4px 0; border-radius: 2px; }
.tl-card { flex: 1; min-width: 0; padding: 14px 16px; background: #fbfcfe; border: 1px solid #edf1f5; border-radius: 10px; margin-bottom: 18px; transition: box-shadow 0.2s; }
.tl-card:hover { box-shadow: 0 6px 16px rgba(15, 40, 80, 0.06); }
.tl-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.tl-agent { font-size: 15px; font-weight: 700; color: #1d2129; }
.tl-sub { font-size: 12px; color: #9aa3ad; margin-top: 3px; }
.tl-meta { display: flex; gap: 20px; flex-wrap: wrap; margin-top: 10px; }
.tl-m { font-size: 12px; color: #57606a; display: inline-flex; align-items: center; gap: 5px; }
.tl-k { color: #9aa3ad; }
.tl-io { margin-top: 10px; border-top: 1px dashed #e3e9ee; padding-top: 8px; }
.tl-io summary { font-size: 12px; color: #2563a8; cursor: pointer; outline: none; }
.tl-io pre { margin: 8px 0 4px; padding: 8px 10px; background: #f6f8f9; border-radius: 6px; font-size: 12px; color: #34495e; white-space: pre-wrap; word-break: break-all; }
</style>