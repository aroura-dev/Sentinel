<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待处理" value="OPEN" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 120px">
            <el-option label="P0 紧急" value="P0" />
            <el-option label="P1 优先" value="P1" />
            <el-option label="P2 常规" value="P2" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="按订单号筛选" clearable style="width: 180px" @clear="load" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
      <div class="wo-tip"><b>处理提示：</b>级别越靠前越紧急（P0 需优先响应）。先用「AI 诊断」确认原因，处理完可「登记索赔」，会自动进入「理赔流程」走审批。金额列仅在登记索赔后出现。</div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border :empty-text="emptyText" @row-click="onRowClick">
        <el-table-column label="订单号" min-width="210" align="left">
          <template #default="{ row }">
            <router-link :to="'/tms/order/' + row.order_no" style="color: #0891b2; text-decoration: none">{{ row.order_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="96" align="center">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="liability" label="责任方" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.liability" size="small">{{ liabilityLabel(row.liability) }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="索赔额" width="110" align="center">
          <template #default="{ row }">
            <template v-if="Number(row.claim_amount) > 0">
              <span class="money">¥{{ Number(row.claim_amount).toFixed(2) }}</span><span v-if="row.currency && row.currency !== 'CNY'" class="cur">{{ row.currency }}</span>
            </template>
            <span v-else class="muted">未索赔</span>
          </template>
        </el-table-column>
        <el-table-column label="核定额" width="104" align="center">
          <template #default="{ row }">
            <span v-if="Number(row.compensation_amount) > 0" class="comp">¥{{ Number(row.compensation_amount).toFixed(2) }}</span>
            <span v-else class="muted">未核定</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" min-width="176" align="center">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" style="margin-right: 8px" @click="openDetail(row, false)">详情</el-link>
            <el-link v-if="['OPEN','PROCESSING','RESOLVED'].includes(row.status)" type="success" :underline="false" @click="openDetail(row, true)">处理</el-link>
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

      <el-dialog v-model="diagDialog" title="AI 异常诊断" width="520px">
        <el-alert v-if="diagLoading" type="info" :closable="false" show-icon>Agent 诊断中，请稍候…</el-alert>
        <template v-else-if="diagResult">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="异常原因">{{ diagResult.reason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处理建议">{{ diagResult.suggestion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="levelType(diagResult.priority)">{{ levelLabel(diagResult.priority) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="链路ID">
              <code style="font-size: 12px">{{ diagResult.traceId || '-' }}</code>
            </el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <div style="color: #6b7280; font-size: 13px">诊断结果可一键带入索赔登记（责任方/理赔金额自动填充）</div>
        </template>
        <template #footer>
          <el-button @click="diagDialog = false">关闭</el-button>
          <el-button v-if="diagResult" type="primary" @click="applyToClaim">带入索赔</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="claimDialog" title="登记索赔" width="480px">
        <el-form :model="claimForm" label-width="90px">
          <el-form-item label="责任方">
            <el-select v-model="claimForm.liability" style="width: 100%">
              <el-option label="承运商" value="carrier" />
              <el-option label="商家" value="merchant" />
              <el-option label="平台" value="platform" />
            </el-select>
          </el-form-item>
          <el-form-item label="索赔金额"><el-input-number v-model="claimForm.claimAmount" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="理赔金额"><el-input-number v-model="claimForm.compensationAmount" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="处理结果"><el-input v-model="claimForm.resolution" type="textarea" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="claimDialog = false">取消</el-button>
          <el-button type="primary" :loading="claimLoading" @click="doClaim">提交</el-button>
        </template>
      </el-dialog>

      <!-- 工单详情抽屉：点进行查看并处理 -->
      <el-drawer v-model="detailOpen" :title="detail ? '工单 #' + detail.id + '（' + detail.order_no + '）' : '工单详情'" size="540px">
        <template v-if="detail">
          <el-alert v-if="!detailAct" type="info" :closable="false" show-icon title="查看模式" description="如需执行状态流转、AI 诊断、登记索赔等处理，请在列表中点击「处理」进入。" style="margin-bottom: 12px" />
        <el-alert v-if="detailAct" type="success" :closable="false" show-icon title="处理方案" description="推荐流程：AI 诊断定位原因 → 处理中 → 标记已解决；有损失可登记索赔进入理赔流程；无需继续可关闭工单。" style="margin-bottom: 12px" />
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="订单号">
              <router-link :to="'/tms/order/' + detail.order_no" style="color: #0891b2; text-decoration: none">{{ detail.order_no }}</router-link>
            </el-descriptions-item>
            <el-descriptions-item label="异常类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
            <el-descriptions-item label="级别"><el-tag size="small" :type="levelType(detail.level)">{{ levelLabel(detail.level) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag size="small" :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="责任方">
              <el-tag v-if="detail.liability" size="small">{{ liabilityLabel(detail.liability) }}</el-tag>
              <span v-else>待认定</span>
            </el-descriptions-item>
            <el-descriptions-item label="异常描述"><span style="white-space: pre-wrap; word-break: break-all">{{ detail.description || '-' }}</span></el-descriptions-item>
            <el-descriptions-item v-if="detail.sop" label="处理 SOP"><span style="white-space: pre-wrap">{{ detail.sop }}</span></el-descriptions-item>
            <el-descriptions-item v-if="Number(detail.claim_amount) > 0" label="索赔额"><span class="money">¥{{ Number(detail.claim_amount).toFixed(2) }}</span></el-descriptions-item>
            <el-descriptions-item v-if="Number(detail.compensation_amount) > 0" label="核定额"><span class="comp">¥{{ Number(detail.compensation_amount).toFixed(2) }}</span></el-descriptions-item>
            <el-descriptions-item v-if="detail.resolution" label="处理结果">{{ detail.resolution }}</el-descriptions-item>
            <el-descriptions-item label="登记时间">{{ fmt(detail.created_at) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.resolved_at" label="解决时间">{{ fmt(detail.resolved_at) }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <div v-else class="drawer-loading">加载中…</div>
        <template #footer>
          <div class="wo-foot">
            <el-button size="small" @click="detailOpen = false">返回</el-button>
            <el-button v-if="detailAct && detail && detail.status === 'OPEN'" size="small" type="warning" @click="act('PROCESSING')">开始处理</el-button>
            <el-button v-if="detailAct && detail && detail.status === 'PROCESSING'" size="small" type="primary" @click="act('RESOLVED')">标记已解决</el-button>
            <el-button v-if="detailAct && detail && ['OPEN','PROCESSING','RESOLVED'].includes(detail.status)" size="small" @click="act('CLOSED')">关闭工单</el-button>
            <el-button v-if="detailAct" size="small" type="primary" plain @click="act('diagnose')">AI 诊断</el-button>
            <el-button v-if="detailAct && detail && detail.claim_status === 'NONE'" size="small" type="success" plain @click="act('claim')">登记索赔</el-button>
            <el-button v-if="detailAct && detail && ['OPEN','PROCESSING'].includes(detail.status)" size="small" @click="act('push')">推送商家</el-button>
          </div>
        </template>
      </el-drawer>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { fmtDateTime } from '../../utils/format'
import { woList, woPush, woStatus, woClaim, woDiagnose } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const route = useRoute()
const query = ref({ status: '', level: '', orderNo: route.query.orderNo || '', page: 1, perPage: 10 })
const detailOpen = ref(false)
const detail = ref(null)
const detailAct = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await woList(query.value)
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

async function push(row) {
  await woPush(row.id)
  ElMessage.success(`工单 ${row.id} 已推送商家系统`)
}

async function updateStatus(row, status) {
  await woStatus(row.id, status)
  ElMessage.success('状态已更新')
  load()
}

// 操作列统一下拉：诊断/推送/登记索赔走各自逻辑，其余为状态流转
function onCmd(row, cmd) {
  if (cmd === 'diagnose') return diagnose(row)
  if (cmd === 'push') return push(row)
  if (cmd === 'claim') return openClaim(row)
  updateStatus(row, cmd)
}
function openDetail(row, act = true) {
  detail.value = row
  detailAct.value = act !== false
  detailOpen.value = true
}
function onRowClick(row, column, event) {
  if (event && event.target && event.target.closest && event.target.closest('a, button')) return
  openDetail(row, true)
}
// 抽屉内动作：先收起详情，再执行对应处理
function act(name) {
  const row = detail.value
  detailOpen.value = false
  if (name === 'diagnose') return diagnose(row)
  if (name === 'push') return push(row)
  if (name === 'claim') return openClaim(row)
  return updateStatus(row, name)
}

const TYPE_LABELS = {
  lost: '丢件', damaged: '破损', returned: '退回', delivery_failed: '派送失败',
  customs_delay: '中转延误', sla_breach: '时效违约', claim: '理赔', OTHER: '其他'
}
function typeLabel(t) { return TYPE_LABELS[t] || t || '-' }
const LEVEL_LABELS = { P0: 'P0 紧急', P1: 'P1 优先', P2: 'P2 常规' }
const levelLabel = (l) => LEVEL_LABELS[l] || l || '-'
const STATUS_LABELS = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭', PUSHED: '已推送商家' }
const statusLabel = (s) => STATUS_LABELS[s] || s || '-'
const fmt = (v) => fmtDateTime(v)
const emptyText = '暂无问题工单 — 物流异常会自动生成到这里，也可在订单详情发起售后登记'
function levelType(level) {
  return { P0: 'danger', P1: 'warning', P2: 'info' }[level] || 'info'
}
function statusType(status) {
  return { OPEN: 'danger', PROCESSING: 'warning', RESOLVED: 'success', CLOSED: 'info', PUSHED: 'primary' }[status] || 'info'
}
function liabilityLabel(l) {
  return { carrier: '承运商', merchant: '商家', platform: '平台' }[l] || l
}

const claimDialog = ref(false)
const claimLoading = ref(false)
const claimForm = ref({ id: null, liability: 'carrier', claimAmount: 0, compensationAmount: 0, resolution: '' })

function openClaim(row) {
  claimForm.value = { id: row.id, liability: row.liability || 'carrier', claimAmount: Number(row.claim_amount || 0), compensationAmount: Number(row.compensation_amount || 0), resolution: row.resolution || '' }
  claimDialog.value = true
}

const diagDialog = ref(false)
const diagLoading = ref(false)
const diagResult = ref(null)

async function diagnose(row) {
  row._aiLoading = true
  diagDialog.value = true
  diagLoading.value = true
  diagResult.value = null
  try {
    diagResult.value = await woDiagnose(row.id)
  } catch (e) {
    ElMessage.error('AI 诊断失败，请检查 DASHSCOPE_API_KEY')
  } finally {
    diagLoading.value = false
    row._aiLoading = false
  }
}

function applyToClaim() {
  const r = diagResult.value || {}
  // 按诊断建议推断责任方：涉及承运商措辞 → carrier，否则默认 carrier，用户可在弹窗调整
  const reason = String(r.reason || '').toLowerCase()
  const liability = /商家|merchant|漏发|错发|包装/.test(reason) ? 'merchant'
    : /承运|carrier|丢件|破损|延误|运输/.test(reason) ? 'carrier' : 'carrier'
  claimForm.value = {
    id: claimForm.value.id,
    liability,
    claimAmount: 0,
    compensationAmount: 0,
    resolution: r.suggestion || ''
  }
  diagDialog.value = false
  claimDialog.value = true
}

async function doClaim() {
  claimLoading.value = true
  try {
    await woClaim(claimForm.value.id, {
      liability: claimForm.value.liability,
      claimAmount: claimForm.value.claimAmount,
      compensationAmount: claimForm.value.compensationAmount,
      resolution: claimForm.value.resolution
    })
    ElMessage.success('索赔已登记')
    claimDialog.value = false
    load()
  } finally { claimLoading.value = false }
}

const setDesc = inject('setPageDesc')
onMounted(() => { setDesc('物流异常处理与 AI 诊断，支持按单号 / 类型 / 状态筛选'); load() })
</script>

<style scoped>
/* 长内容单行省略，溢出悬停显示全文 */
:deep(.el-table .cell) { white-space: nowrap; }
.wo-tip { margin-top: 12px; padding: 9px 12px; font-size: 13px; color: #334155; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; line-height: 1.6; }
.wo-tip b { color: #0f766e; }
.money { color: #b45309; font-weight: 600; }
.cur { font-size: 12px; color: #94a3b8; margin-left: 2px; }
.comp { color: #10b981; font-weight: 600; }
.muted { color: #9ca3af; }
.wo-foot { display: flex; flex-wrap: wrap; gap: 8px; }
.drawer-loading { color: #9ca3af; text-align: center; padding: 40px 0; }
</style>
