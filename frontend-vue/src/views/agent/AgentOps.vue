<template>
  <div class="agent-ops">
    <el-card shadow="never" class="ops-card">
      <template #header>
        <div class="card-head">
          <div>
            <div class="card-title">异常自动处置</div>
            <div class="card-sub">Java 编排：诊断、文案、通知、工单；写操作必须人工审批。</div>
          </div>
          <el-tag type="success" effect="plain">Java Agent</el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="form">
        <el-form-item label="处置任务">
          <el-input v-model="form.task" placeholder="如：核对延误并同步买家" style="width: 300px" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" placeholder="OMT-TMS-SEED-0005" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="running" @click="run">启动编排</el-button>
          <el-button @click="loadTasks">刷新任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="ops-card">
      <template #header>
        <div class="card-head">
          <div class="card-title">处置任务</div>
          <span class="card-sub">待审批数量来自 agent_call_log，不依赖进程内状态。</span>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" border>
        <el-table-column prop="traceId" label="TraceId" min-width="220" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="scene" label="场景" min-width="180" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'done' ? 'success' : 'warning'" effect="light">
              {{ row.status === 'done' ? '已完成' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待审批" width="90" align="center" prop="pendingSteps" />
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="loadTrace(row.traceId)">查看链路</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="AgentOps 执行链路" size="62%">
      <div v-if="traceId" class="trace-meta">
        <span>TraceId：{{ traceId }}</span>
        <el-button size="small" @click="loadTrace(traceId)">刷新</el-button>
      </div>
      <el-timeline v-if="steps.length">
        <el-timeline-item
          v-for="item in steps"
          :key="item.id"
          :timestamp="item.agent_name"
          :type="statusMeta(item.status).type === 'danger' ? 'danger' : statusMeta(item.status).type === 'warning' ? 'warning' : 'success'"
        >
          <div class="step-head">
            <strong>{{ label(item.agent_name) }}</strong>
            <el-tag size="small" :type="statusMeta(item.status).type" effect="plain">{{ statusMeta(item.status).text }}</el-tag>
          </div>
          <pre class="step-output">{{ pretty(item.output) }}</pre>
          <div v-if="item.status === 'pending_approval'" class="approval-actions">
            <el-button size="small" type="success" @click="approve(item)">批准并执行</el-button>
            <el-button size="small" type="danger" plain @click="reject(item)">拒绝</el-button>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无链路数据" />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentOpsApprove, agentOpsReject, agentOpsRun, agentOpsTasks, agentOpsTrace } from '../../api'

const form = ref({ task: '核对物流异常并同步买家', orderNo: 'OMT-TMS-SEED-0005' })
const tasks = ref([])
const steps = ref([])
const traceId = ref('')
const drawer = ref(false)
const loading = ref(false)
const running = ref(false)

const labels = {
  java_planner: '总控规划',
  java_diagnose: '异常诊断',
  java_content: '通知文案',
  java_ops: '待审批写操作',
  java_ops_summary: '处置汇总',
  JavaKnowledgeAgent: '知识问答',
  JavaAnalyticsAgent: '运营分析'
}
const statuses = {
  success: { text: '成功', type: 'success' },
  degraded: { text: '降级', type: 'warning' },
  pending_approval: { text: '待审批', type: 'warning' },
  failed: { text: '失败', type: 'danger' },
  timeout: { text: '超时', type: 'info' }
}

function label(name) { return labels[name] || name || '-' }
function statusMeta(status) { return statuses[status] || { text: status || '-', type: 'info' } }
function pretty(value) {
  if (!value) return '-'
  try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return String(value) }
}

async function loadTasks() {
  loading.value = true
  try {
    const data = await agentOpsTasks({})
    tasks.value = data.rows || []
  } finally {
    loading.value = false
  }
}

async function run() {
  if (!form.value.task || !form.value.orderNo) {
    ElMessage.warning('请填写处置任务和订单号')
    return
  }
  running.value = true
  try {
    const data = await agentOpsRun({ task: form.value.task, orderNo: form.value.orderNo })
    traceId.value = data.traceId
    drawer.value = true
    steps.value = data.plan || []
    await loadTrace(data.traceId)
    await loadTasks()
    ElMessage.success('编排已创建，写操作等待审批')
  } finally {
    running.value = false
  }
}

async function loadTrace(id) {
  traceId.value = id
  drawer.value = true
  steps.value = (await agentOpsTrace(id)) || []
}

async function approve(item) {
  await agentOpsApprove(item.id, { reason: '运营后台人工审批通过' })
  ElMessage.success('已执行')
  await loadTrace(traceId.value)
  await loadTasks()
}

async function reject(item) {
  await agentOpsReject(item.id, { reason: '运营后台人工拒绝' })
  ElMessage.success('已拒绝')
  await loadTrace(traceId.value)
  await loadTasks()
}

onMounted(loadTasks)
</script>

<style scoped>
.agent-ops { display: flex; flex-direction: column; gap: 16px; }
.ops-card { border: 1px solid #eef1f4; border-radius: 10px; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.card-title { color: #1d2129; font-size: 16px; font-weight: 700; }
.card-sub { color: #8a94a6; font-size: 12px; margin-top: 4px; }
.trace-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; color: #57606a; font-size: 13px; word-break: break-all; }
.step-head { display: flex; align-items: center; gap: 10px; }
.step-output { max-height: 180px; overflow: auto; margin: 8px 0; padding: 10px; border-radius: 8px; background: #f6f8fa; color: #34495e; font-size: 12px; white-space: pre-wrap; word-break: break-all; }
.approval-actions { display: flex; gap: 8px; }
</style>
