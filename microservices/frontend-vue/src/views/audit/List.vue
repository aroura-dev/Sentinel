<template>
  <div>
    <div class="au-note">自动记录关键操作的操作人、时间、动作与业务单号，支持按时间与操作人检索与导出；内容只读，供审计与问题追溯。</div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="时间范围">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 270px" clearable @change="onRange" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 130px" @change="reload">
            <el-option v-for="m in MODULES" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operator" placeholder="用户名" clearable style="width: 150px" @keyup.enter="reload" />
        </el-form-item>
        <el-form-item label="单号">
          <el-input v-model="query.targetNo" placeholder="订单/运单/账单号" clearable style="width: 200px" @keyup.enter="reload" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button :type="mineOnly ? 'warning' : 'default'" @click="toggleMine">{{ mineOnly ? '只看我 ✓' : '只看我' }}</el-button>
          <el-button :loading="exporting" @click="exportCsv">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

        <el-card shadow="never">
      <div class="au-feed">
        <div v-if="!rows.length && !loading" class="au-empty">暂无操作记录 — 完成一笔关键操作后会自动留痕</div>
        <div v-for="row in rows" :key="row.id || row.created_at" class="au-item" @click="openDetail(row)">
          <div class="au-main">
            <div class="au-sentence">
              {{ row.operator }}<span class="au-role">（{{ roleLabel(row.operator_role) }}）</span>于 {{ fmt(row.created_at) }}
              在「{{ moduleLabel(row.module) }}」执行了<span class="au-action-word">{{ actionLabel(row.action) }}</span>
              <template v-if="row.target_no">，涉及单号 {{ row.target_no }}</template>
            </div>
            <div v-if="row.detail" class="au-detail">{{ row.detail }}</div>
          </div>
          <span class="au-go">查看详情 ›</span>
        </div>
      </div>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        background layout="total, prev, pager, next"
        :total="total"
        :page-size="query.perPage"
        :current-page="query.page"
        @current-change="onPageChange"
      />
    </el-card>
    <el-dialog v-model="detailVisible" title="操作详情" width="620px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="操作时间">{{ fmt(detail.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleLabel(detail.operator_role) }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ moduleLabel(detail.module) }}</el-descriptions-item>
          <el-descriptions-item label="动作">{{ detail.action }}</el-descriptions-item>
          <el-descriptions-item label="业务单号">{{ detail.target_no || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 14px">
          <div style="font-size:12px;color:#8b95a1;margin-bottom:6px">操作详情</div>
          <div style="background:#f7f9fa;border:1px solid #eef1f4;border-radius:8px;padding:10px 14px;font-size:13px;line-height:1.8;white-space:pre-wrap">{{ detail.detail || '-' }}</div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditList } from '../../api'

const MODULES = [
  { value: 'order', label: '订单' }, { value: 'waybill', label: '运单' }, { value: 'workorder', label: '工单' },
  { value: 'bill', label: '账单' }, { value: 'reconcile', label: '对账' }, { value: 'user', label: '用户' },
  { value: 'role', label: '角色' }, { value: 'risk', label: '风险预警' }, { value: '风险预警', label: '风险预警(中文)' },
  { value: 'notification', label: '通知' }, { value: 'after_sale', label: '售后' }, { value: 'merchant', label: '商家' },
  { value: 'carrier', label: '承运商' }, { value: 'product', label: '商品' }, { value: 'warehouse', label: '仓库' },
  { value: 'stocktake', label: '盘点' }, { value: 'knowledge', label: '知识库' }
]
const rows = ref([])
const total = ref(0)
const emptyText = '暂无操作记录 — 完成一笔关键操作后会自动留痕'
const loading = ref(false)
const exporting = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const range = ref(null)
const mineOnly = ref(false)
const me = sessionStorage.getItem('sentinel_username') || localStorage.getItem('sentinel_username') || ''
const query = reactive({ module: '', operator: '', targetNo: '', page: 1, perPage: 15 })

const setDesc = inject('setPageDesc')
const fmt = (v) => (v ? String(v).slice(0, 19) : '-')
function moduleLabel(m) { return (MODULES.find((x) => x.value === m) || {}).label || m }
const ACTION_LABELS = {
  CREATE: '创建', UPDATE: '修改', DELETE: '删除', TOGGLE: '启停', RESET_PWD: '重置密码',
  SUBMIT: '提交', VERIFY: '核销', SETTLE: '结算', REJECT: '驳回', REOPEN: '重开',
  MARK: '标记原因', SAVE_MENUS: '调整权限', SEND: '发送', PUSH: '推送', MERGE: '合并', GENERATE: '生成'
}
function actionLabel(a) { return ACTION_LABELS[a] || a || '-' }
const ROLE_LABELS = { ADMIN: '管理员', OPERATOR: '运营', CUSTOMER_SERVICE: '客服', MERCHANT: '商家', FINANCE: '财务' }
const roleLabel = (r) => ROLE_LABELS[r] || r || '-'

async function load() {
  loading.value = true
  try {
    const data = await auditList(query.value)
    rows.value = data.rows || []
    total.value = data.count || 0
  } finally {
    loading.value = false
  }
}
function reload() { query.page = 1; load() }
function onPageChange(p) { query.page = p; load() }

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}
function onRange() {
  query.start = range.value && range.value[0] ? range.value[0] : ''
  query.end = range.value && range.value[1] ? range.value[1] : ''
  reload()
}
function toggleMine() {
  mineOnly.value = !mineOnly.value
  query.operator = mineOnly.value ? me : ''
  reload()
}

async function exportCsv() {
  if (!total.value) return ElMessage.warning('当前没有可导出的记录')
  exporting.value = true
  try {
    const per = 200
    const pages = Math.min(Math.ceil(total.value / per), 10)
    const all = []
    for (let p = 1; p <= pages; p++) {
      const d = await auditList({ ...query.value, page: p, perPage: per })
      all.push(...(d.rows || []))
    }
    const head = ['操作时间', '操作人', '角色', '模块', '动作', '业务单号', '操作详情']
    const lines = all.map((r) => [fmt(r.created_at), r.operator, roleLabel(r.operator_role), moduleLabel(r.module), r.action, r.target_no, (r.detail || '').replace(/\n/g, ' ')]
      .map((c) => `"${String(c == null ? '' : c).replace(/"/g, '""')}"`).join(','))
    const csv = '\uFEFF' + [head.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `操作日志_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success(`已导出 ${all.length} 条`)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  setDesc('关键操作留痕与审计：按时间、模块、操作人回溯')
  load()
})
</script>

<style scoped>
.au-note { margin-bottom: 16px; padding: 10px 14px; font-size: 13px; color: #374151; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 8px; line-height: 1.7; }
.au-note b { color: #1f2937; }
.au-feed { min-height: 240px; }
.au-item { display: flex; align-items: flex-start; gap: 12px; padding: 13px 6px; border-bottom: 1px solid #f1f4f6; cursor: pointer; }
.au-item:hover { background: #f7fbfc; }
.au-item:last-child { border-bottom: none; }
.au-mark { width: 8px; height: 8px; border-radius: 50%; background: #bcdde6; margin-top: 7px; flex-shrink: 0; }
.au-main { flex: 1; min-width: 0; }
.au-sentence { font-size: 14px; color: #1d2129; line-height: 1.8; }
.au-role { color: #8b95a1; }
.au-action-word { color: #0891b2; font-weight: 600; margin: 0 2px; }
.au-detail { margin-top: 6px; font-size: 13px; color: #57606a; background: #f7f9fa; border-radius: 6px; padding: 6px 10px; line-height: 1.7; }
.au-go { color: #0891b2; font-size: 13px; flex-shrink: 0; align-self: center; }
.au-empty { padding: 60px 0; text-align: center; color: #aab4bd; font-size: 13px; }
</style>