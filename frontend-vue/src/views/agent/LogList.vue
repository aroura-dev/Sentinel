<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="智能体">
          <el-select v-model="query.agentName" placeholder="全部智能体" clearable filterable style="width: 200px">
            <el-option v-for="o in AGENT_OPTIONS" :key="o.code" :label="o.label" :value="o.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="success" />
            <el-option label="待审批" value="pending_approval" />
            <el-option label="降级" value="degraded" />
            <el-option label="失败" value="failed" />
            <el-option label="超时" value="timeout" />
          </el-select>
        </el-form-item>
        <el-form-item label="TraceId">
          <el-input v-model="query.traceId" placeholder="输入 TraceId（订单号或完整链路号）" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <p><b>输入：</b></p>
              <pre class="kv">{{ prettyJson(row.input) }}</pre>
              <p><b>输出：</b></p>
              <pre class="kv">{{ prettyJson(row.output) }}</pre>
            </div>
          </template>
        </el-table-column>
        <el-table-column align="center" prop="id" label="ID" width="70" />
        <el-table-column label="智能体" min-width="150">
          <template #default="{ row }">
                        <div class="agent-cell">
              <span class="agent-avatar" :style="avatarStyle(row.agent_name)">{{ avatarChar(row.agent_name) }}</span>
              <span class="agent-text">
                <span class="agent-main">{{ agentLabel(row.agent_name) }}</span>
                <span class="agent-sub">{{ row.agent_name || '-' }}</span>
              </span>
            </div>
</template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="动作" min-width="180">
          <template #default="{ row }">
            <template v-if="toolNames(row.tools_called).length">
              <el-tag v-for="t in toolNames(row.tools_called)" :key="t" size="small" type="info" style="margin-right: 4px">{{ t }}</el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="Token" width="90" align="center">
          <template #default="{ row }">{{ row.token_usage ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">{{ row.latency_ms != null ? row.latency_ms + ' ms' : '-' }}</template>
        </el-table-column>
        <el-table-column label="调用场景" min-width="220">
          <template #default="{ row }">
            <span :title="row.trace_id || ''">{{ friendlyTraceId(row.trace_id) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" :disabled="!row.trace_id" @click="goTrace(row.trace_id)">链路追溯</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.perPage"
        :current-page="query.page"
        @current-change="onPageChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentList } from '../../api'
import { agentLabel, statusMeta, toolNames, friendlyTraceId, AGENT_OPTIONS } from '../../utils/agentLabel'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ agentName: '', status: '', traceId: '', page: 1, perPage: 10 })
function avatarChar(name) {
  const label = agentLabel(name)
  return (label || '?').charAt(0)
}

function avatarStyle(name) {
  const palette = ['#2563a8', '#0c8f62', '#b76e00', '#7c5cd6', '#c0392b', '#0e7490']
  let h = 0
  const s = String(name || 'x')
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return { background: palette[h % palette.length] }
}

function prettyJson(v) {
  if (!v) return '-'
  try {
    return JSON.stringify(JSON.parse(v), null, 2)
  } catch {
    return String(v)
  }
}

async function load() {
  loading.value = true
  try {
    const data = await agentList(query.value)
    rows.value = data.rows || []
    total.value = data.count || 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  query.value.page = p
  load()
}

function goTrace(traceId) {
  if (traceId) router.push({ path: '/agent/trace', query: { traceId } })
}

onMounted(load)
</script>

<style scoped>
.ll-page { display: flex; flex-direction: column; gap: 16px; }
.ll-filter { border: 1px solid #eef1f4; border-radius: 10px; }
.ll-table { border: 1px solid #eef1f4; border-radius: 10px; }
.ll-table :deep(.el-table__header th) { background: #f7f9fc; color: #4e5969; font-weight: 600; }
.expand-box { padding: 8px 24px; }
.expand-box p { margin: 8px 0 4px; color: #57606a; font-size: 13px; font-weight: 600; }
.kv { margin: 0 0 8px; padding: 8px 12px; background: #f7f9fa; border-radius: 6px; font-size: 12px; color: #34495e; white-space: pre-wrap; word-break: break-all; }
.agent-cell { display: flex; align-items: center; gap: 10px; }
.agent-avatar { width: 30px; height: 30px; border-radius: 8px; color: #fff; font-size: 14px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 2px 6px rgba(15,40,80,.12); }
.agent-text { display: flex; flex-direction: column; min-width: 0; }
.agent-main { color: #1d2129; font-weight: 600; line-height: 1.4; }
.agent-sub { font-size: 12px; color: #9aa3ad; }
</style>