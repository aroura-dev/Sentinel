<template>
  <div>
    <!-- 审核统计卡片：点击按状态筛选表格 -->
    <div class="review-stats">
      <div v-for="s in statCards" :key="s.key" class="rs-card" @click="filterByStatus(s.key)">
        <div class="rs-row">
          <div class="rs-icon" :style="{ background: s.bg, color: s.color }">
            <el-icon :size="16"><component :is="s.icon" /></el-icon>
          </div>
          <div class="rs-meta">
            <div class="rs-label">{{ s.label }}</div>
            <div class="rs-value" :class="{ alert: s.alert }">{{ counts[s.key] }}</div>
            <div class="rs-sub">{{ s.sub }}</div>
          </div>
        </div>
      </div>
    </div>

    <DataTable ref="dt" title="订单审核" empty-text="待审核已清空，今天的订单都处理完啦"
      :columns="columns" :load="orderReviewList" :query="query" selectable action-width="120">
      <template #query="{ reload }">
        <el-select v-model="query.status" placeholder="审核状态" clearable :teleported="false" style="width: 130px" @change="reload">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="订单号/商家" clearable style="width: 180px" @keyup.enter="reload" />
        <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="resetStatus(reload)"><el-icon><RefreshRight /></el-icon>重置</el-button>
        <span style="flex:1"></span>
        <el-button type="success" :disabled="!selected.length" @click="batchApprove"><el-icon><CircleCheck /></el-icon>批量通过</el-button>
        <el-button type="danger" :disabled="!selected.length" @click="batchReject"><el-icon><CircleClose /></el-icon>批量驳回</el-button>
      </template>
      <template #cell-review_status="{ row }">
        <el-tag :type="(reviewMeta[row.review_status] && reviewMeta[row.review_status].type) || 'info'" size="small">
          {{ (reviewMeta[row.review_status] && reviewMeta[row.review_status].label) || row.review_status }}
        </el-tag>
      </template>
      <template #cell-freight_cost="{ row }">¥{{ Number(row.freight_cost || 0).toFixed(2) }}</template>
      <template #cell-destination_country="{ row }">{{ countryLabel(row.destination_country) }}</template>
      <template #cell-reviewed_at="{ row }">
        <span v-if="row.reviewed_at">{{ fmt(row.reviewed_at) }}</span><span v-else style="color:#c0c4cc">-</span>
      </template>
      <template #actions="{ row }">
        <el-button size="small" link type="primary" @click="$router.push(`/tms/order/${row.order_no}`)">详情</el-button>
        <el-button v-if="row.review_status === 'PENDING'" size="small" type="primary" @click="openDrawer(row)">审核</el-button>
        <el-button v-else size="small" type="primary" plain @click="openDrawer(row)">查看</el-button>
      </template>
    </DataTable>

    <!-- 审核弹窗 -->
    <el-dialog v-model="drawerVisible" width="640px" @closed="onDrawerClosed" class="review-dialog">
      <template #header>
        <div class="rd-head">
          <div>
            <div class="rd-title">审核订单</div>
            <div class="rd-sub">{{ drawerOrder?.order_no || '' }}</div>
          </div>
          <div class="rd-head-right">
            <el-button v-if="drawerOrder" size="small" link class="rd-newtab" @click="openOrderNewTab">
              <el-icon><Switch /></el-icon>新标签页
            </el-button>
            <el-tag v-if="drawerOrder" :type="(reviewMeta[drawerOrder.review_status] && reviewMeta[drawerOrder.review_status].type) || 'info'" size="small" effect="dark">
              {{ (reviewMeta[drawerOrder.review_status] && reviewMeta[drawerOrder.review_status].label) || drawerOrder.review_status }}
            </el-tag>
          </div>
        </div>
      </template>

      <div v-if="drawerOrder" class="rd-body">
        <!-- ① 风险提示：紧凑单行，颜色凸显警示 -->
        <div v-if="drawerRisk.length" class="risk-slim">
          <el-icon class="risk-slim-icon"><WarningFilled /></el-icon>
          <span class="risk-slim-title">{{ drawerRisk.length }} 项需关注</span>
          <el-tooltip :content="drawerRisk.join('；')" placement="top">
            <span class="risk-slim-items">{{ drawerRisk.join('；') }}</span>
          </el-tooltip>
        </div>

        <!-- ② 订单信息：订单自身的属性 -->
        <div class="rd-block">
          <div class="rd-block-title">订单信息</div>
          <div class="rd-list">
            <div class="rd-item"><span class="rd-item-k">商家</span><span class="rd-item-v">{{ drawerOrder.merchant_name }}</span></div>
            <div class="rd-item"><span class="rd-item-k">目的地</span><span class="rd-item-v">{{ countryLabel(drawerOrder.destination_country) }}</span></div>
            <div class="rd-item"><span class="rd-item-k">物流渠道</span><span class="rd-item-v">{{ channelName(drawerOrder.channel_id) }}</span></div>
            <div class="rd-item"><span class="rd-item-k">时效(SLA)</span><span class="rd-item-v"><StatusTag :status="drawerOrder.sla_status" /></span></div>
            <div class="rd-item"><span class="rd-item-k">商品件数</span><span class="rd-item-v">{{ totalQty }} 件</span></div>
            <div class="rd-item"><span class="rd-item-k">货值</span><span class="rd-item-v">¥{{ Number(drawerOrder.declared_value || 0).toFixed(0) }}</span></div>
            <div class="rd-item"><span class="rd-item-k">运费</span><span class="rd-item-v rd-freight">¥{{ Number(drawerOrder.freight_cost || 0).toFixed(2) }}</span></div>
            <div class="rd-item"><span class="rd-item-k">下单时间</span><span class="rd-item-v">{{ fmt(drawerOrder.created_at) }}</span></div>
          </div>
        </div>

        <!-- ④ 收件人 -->
        <div class="rd-block">
          <div class="rd-block-title">收件人</div>
          <div class="rd-list">
            <div class="rd-item" :class="{ 'rd-item-alert': !drawerOrder.buyer_name }">
              <span class="rd-item-k">姓名</span><span class="rd-item-v">{{ drawerOrder.buyer_name || '未填写' }}</span>
            </div>
            <div class="rd-item" :class="{ 'rd-item-alert': !drawerOrder.buyer_phone || !PHONE_RE.test(drawerOrder.buyer_phone) }">
              <span class="rd-item-k">手机号</span>
              <span class="rd-item-v"><el-tooltip :content="drawerOrder.buyer_phone || '未填写'" placement="top"><span>{{ maskPhone(drawerOrder.buyer_phone) }}</span></el-tooltip></span>
            </div>
            <div class="rd-item" :class="{ 'rd-item-alert': !drawerOrder.buyer_address }">
              <span class="rd-item-k">收货地址</span><span class="rd-item-v">{{ [drawerOrder.buyer_city, drawerOrder.buyer_address].filter(Boolean).join(' ') || '未填写' }}</span>
            </div>
          </div>
        </div>

        <!-- ⑤ 商品明细 -->
        <div class="rd-block">
          <div class="rd-block-title">商品明细</div>
          <el-table v-if="drawerItems.length" :data="drawerItems" border size="small" max-height="220">
            <el-table-column prop="sku" label="商品" min-width="120" />
            <el-table-column prop="qty" label="数量" width="70" />
            <el-table-column label="单件重量" width="90">
              <template #default="{ row }">{{ row.unit_weight_kg }} kg</template>
            </el-table-column>
            <el-table-column label="单件价值" width="90">
              <template #default="{ row }">¥{{ Number(row.unit_declared_value).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
          <div v-if="drawerItems.length" class="drawer-total">共 <b>{{ totalQty }}</b> 件 · 货值合计 <b>¥{{ totalValue }}</b></div>
          <span v-else style="color:#9ca3af">无商品明细</span>
        </div>

        <!-- ⑥ 商家信誉：历史数据辅助审核 -->
        <div class="rd-block">
          <div class="rd-block-title">商家信誉</div>
          <div class="rd-stat-grid">
            <div class="rd-stat"><div class="rd-stat-k">历史订单</div><div class="rd-stat-v">{{ merchantStats.total }}</div></div>
            <div class="rd-stat"><div class="rd-stat-k">审核通过率</div><div class="rd-stat-v">{{ passRate }}%</div></div>
            <div class="rd-stat"><div class="rd-stat-k">历史异常</div><div class="rd-stat-v" :class="{ 'stat-warn': merchantStats.anomaly > 5 }">{{ merchantStats.anomaly }}</div></div>
            <div class="rd-stat"><div class="rd-stat-k">历史退货</div><div class="rd-stat-v" :class="{ 'stat-warn': merchantStats.returned / (merchantStats.total || 1) > 0.2 }">{{ merchantStats.returned }}</div></div>
          </div>
          <div v-if="merchantRisk" class="merchant-tip">{{ merchantRisk }}</div>
        </div>

        <!-- ⑧ 历史审核记录 -->
        <div class="rd-block">
          <div class="rd-block-title">历史审核记录</div>
          <div v-if="reviewLogs.length" class="rd-log">
            <div v-for="(log, i) in reviewLogs" :key="i" class="rd-log-item">
              <span class="rd-log-time">{{ fmt(log.created_at) }}</span>
              <span class="rd-log-op">{{ log.operator || '-' }}</span>
              <el-tag size="small" :type="reviewActionType(log.action)" class="rd-log-tag">{{ reviewActionLabel(log.action) }}</el-tag>
              <span class="rd-log-detail">{{ log.detail }}</span>
            </div>
          </div>
          <span v-else style="color:#9ca3af">暂无历史审核记录</span>
        </div>

        <!-- ⑧ 驳回原因（快捷选择 + 自定义） -->
        <div class="rd-block">
          <div class="rd-block-title">驳回原因<span class="req-star">*</span></div>
          <div v-if="isPending" class="rj-radio">
            <el-radio-group v-model="rejectReason">
              <el-radio v-for="r in REJECT_OPTIONS" :key="r" :value="r">{{ r }}</el-radio>
            </el-radio-group>
          </div>
          <el-input v-model="rejectReason" type="textarea" :rows="2" placeholder="选择上方原因，或自定义填写；驳回时必填" :disabled="!isPending" :maxlength="100" />
        </div>
      </div>

      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button @click="openOrderNewTab">查看完整日志</el-button>
        <el-button v-if="isPending" type="danger" :loading="reviewing" @click="doReject"><el-icon><CircleClose /></el-icon>驳回</el-button>
        <el-button v-if="isPending" type="success" :loading="reviewing" @click="doApprove"><el-icon><CircleCheck /></el-icon>通过</el-button>
      </template>
    </el-dialog>

    <!-- 批量驳回原因弹窗 -->
    <el-dialog v-model="rejectDialog" width="480px" class="review-dialog">
      <template #header>
        <div class="rd-head">
          <div>
            <div class="rd-title">批量驳回</div>
            <div class="rd-sub">{{ rejectCount > 1 ? rejectCount + ' 个订单' : '请填写驳回原因' }}</div>
          </div>
        </div>
      </template>
      <div class="rj-wrap">
        <div class="rj-tip">很抱歉，您选中的 {{ rejectCount }} 个订单将不予通过。请选择或填写驳回原因，提交方会收到通知，并可根据原因修改后重新提交。</div>
        <div class="rj-radio">
          <el-radio-group v-model="batchReason">
            <el-radio v-for="r in REJECT_OPTIONS" :key="r" :value="r">{{ r }}</el-radio>
          </el-radio-group>
        </div>
        <el-input v-model="batchReason" type="textarea" :rows="3" placeholder="选择上方原因，或自定义填写" :maxlength="100" show-word-limit />
      </div>
      <template #footer>
        <el-button @click="rejectDialog = false">再想想</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmBatchReject"><el-icon><CircleClose /></el-icon>确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { orderReviewList, orderReview, notifSend, tmsOrderDetail, auditList, tmsChannelAll, tmsOrderList } from '../../api'
import { fmtDateTime } from '../../utils/format'
import { countryLabel } from '../../utils/country'

const dt = ref(null)
const reviewing = ref(false)
const query = reactive({ status: '', keyword: '' })
const counts = ref({ PENDING: '-', APPROVED: '-', REJECTED: '-' })

const statCards = [
  { key: 'PENDING', label: '待审核', icon: 'Clock', color: '#f59e0b', bg: 'rgba(245,158,11,.10)', alert: true, sub: '有单子在等你确认，辛苦啦' },
  { key: 'APPROVED', label: '已通过', icon: 'CircleCheckFilled', color: '#0891b2', bg: 'rgba(8,145,178,.10)', sub: '商家已经可以安排发货了' },
  { key: 'REJECTED', label: '已驳回', icon: 'CircleCloseFilled', color: '#e6653c', bg: 'rgba(230,101,60,.10)', alert: true, sub: '已通知商家，修改后即可重提' }
]
const selected = computed(() => dt.value?.selectedRows || [])

// 审核弹窗 + 连续审单队列
const drawerVisible = ref(false)
const drawerOrder = ref(null)
const drawerItems = ref([])
const queue = ref([])
const reviewLogs = ref([])
const rejectReason = ref('')

const PHONE_RE = /^1[3-9]\d{9}$/
const totalQty = computed(() => drawerItems.value.reduce((s, it) => s + Number(it.qty || 0), 0))
const totalValue = computed(() => drawerItems.value.reduce((s, it) => s + Number(it.unit_declared_value || 0) * Number(it.qty || 0), 0).toFixed(2))
const isPending = computed(() => drawerOrder.value?.review_status === 'PENDING')

// ① 风险汇总：收件人缺失/异常/高货值
const drawerRisk = computed(() => {
  const o = drawerOrder.value
  if (!o) return []
  const risks = []
  if (!o.buyer_name) risks.push('收件人姓名缺失')
  if (!o.buyer_phone) risks.push('收件人手机号缺失')
  else if (!PHONE_RE.test(o.buyer_phone)) risks.push('收件人手机号格式不正确')
  if (!o.buyer_address) risks.push('收货地址缺失')
  if (Number(o.declared_value || 0) > 2000) risks.push(`货值较高（¥${Number(o.declared_value).toFixed(0)}），建议重点核对`)
  return risks
})

// ③ 手机号脱敏
function maskPhone(p) {
  if (!p) return '未填写'
  const s = String(p)
  return s.length >= 7 ? s.slice(0, 3) + '****' + s.slice(-4) : s
}

function filterByStatus(status) {
  query.status = query.status === status ? '' : status
  dt.value.reload()
}

function resetStatus(reload) {
  query.status = ''
  query.keyword = ''
  if (reload) reload()
}

async function loadCounts() {
  try {
    const [p, a, r] = await Promise.all([
      orderReviewList({ status: 'PENDING', page: 1, perPage: 1 }),
      orderReviewList({ status: 'APPROVED', page: 1, perPage: 1 }),
      orderReviewList({ status: 'REJECTED', page: 1, perPage: 1 })
    ])
    counts.value = { PENDING: p.count || 0, APPROVED: a.count || 0, REJECTED: r.count || 0 }
  } catch (e) { /* 无权限/接口异常忽略 */ }
}

// 打开弹窗：快照当前页待审核队列，方便审完一单自动开下一单
async function openDrawer(row) {
  queue.value = (dt.value?.rows || []).filter((r) => r.review_status === 'PENDING')
  await loadDrawer(row)
}

async function loadDrawer(row) {
  drawerOrder.value = row
  drawerItems.value = []
  reviewLogs.value = []
  rejectReason.value = ''
  try {
    const d = await tmsOrderDetail(row.order_no)
    drawerItems.value = safeJson(d.items_json)
  } catch { /* 商品明细加载失败不阻塞审核 */ }
  loadReviewLogs(row.order_no)
  loadMerchantStats(row.merchant_id)
  drawerVisible.value = true
}

function onDrawerClosed() {
  drawerOrder.value = null
  drawerItems.value = []
  reviewLogs.value = []
  rejectReason.value = ''
}

// 审完当前单，自动打开下一单待审核（连续审单）
function nextPending(currentOrderNo) {
  const next = queue.value.find((r) => r.review_status === 'PENDING' && r.order_no !== currentOrderNo)
  if (next) loadDrawer(next)
  else drawerVisible.value = false
}

// ⑤ 历史审核记录
const REVIEW_ACTION = { REVIEW: '审核', APPROVE: '通过', REJECT: '驳回' }
function reviewActionLabel(a) { return REVIEW_ACTION[a] || a }
function reviewActionType(a) { return a === 'REJECT' ? 'danger' : a === 'APPROVE' ? 'success' : 'info' }
async function loadReviewLogs(orderNo) {
  try {
    const r = await auditList({ targetNo: orderNo, page: 1, perPage: 10 })
    reviewLogs.value = (r.rows || []).filter((x) => ['REVIEW', 'APPROVE', 'REJECT'].includes(x.action))
  } catch { reviewLogs.value = [] }
}

// 新标签页打开订单详情
function openOrderNewTab() {
  if (drawerOrder.value) window.open(`/tms/order/${drawerOrder.value.order_no}`, '_blank')
}

// 快捷驳回原因：先选，不合适再自定义
const REJECT_OPTIONS = ['货值与商品不符', '收件人信息有误', '商品信息错误', '渠道无法送达该目的地', '重复下单']

// ⑥ 内嵌驳回原因：单个订单（点击驳回先弹确认）
async function doReject() {
  const reason = (rejectReason.value || '').trim()
  if (!reason) return ElMessage.warning('请填写驳回原因')
  await ElMessageBox.confirm(
    `该订单将标记为「已驳回」，提交方会收到通知，并可根据驳回原因修改后重新提交。\n\n驳回原因：${reason}\n\n此操作会影响订单流转，请确认。`,
    '确认驳回订单？',
    { type: 'warning', confirmButtonText: '确认驳回', cancelButtonText: '再想想' }
  )
  reviewing.value = true
  try {
    await orderReview(drawerOrder.value.order_no, false, reason)
    notifyReject(drawerOrder.value.order_no)
    ElMessage.success('已驳回，已通知提交方')
    dt.value.reload()
    loadCounts()
    nextPending(drawerOrder.value.order_no)
  } finally { reviewing.value = false }
}

async function doApprove() {
  await ElMessageBox.confirm(
    '该订单通过后将放行发货，进入履约流程。请确认订单信息无误后再放行。',
    '确认审核通过？',
    { type: 'info', confirmButtonText: '确认通过', cancelButtonText: '再想想' }
  )
  reviewing.value = true
  try {
    await orderReview(drawerOrder.value.order_no, true, null)
    ElMessage.success('已审核通过，订单放行可发货')
    dt.value.reload()
    loadCounts()
    nextPending(drawerOrder.value.order_no)
  } finally { reviewing.value = false }
}

// 批量通过
async function batchApprove() {
  const rows = selected.value.filter((r) => r.review_status !== 'APPROVED')
  if (!rows.length) return ElMessage.warning('请选择待审核订单')
  await ElMessageBox.confirm(`确认批量通过 ${rows.length} 个订单？通过后即可发货。`, '批量通过', { type: 'warning' })
  let ok = 0
  for (const row of rows) {
    try { await orderReview(row.order_no, true, null); ok++ } catch { /* 单条失败跳过 */ }
  }
  ElMessage.success(`批量通过：${ok}/${rows.length}`)
  dt.value.reload()
  loadCounts()
}

// 批量驳回（弹窗）
const rejectDialog = ref(false)
const batchReason = ref('')
const rejectRows = ref([])
const rejecting = ref(false)
const rejectCount = computed(() => rejectRows.value.length)

async function batchReject() {
  const rows = selected.value.filter((r) => r.review_status === 'PENDING')
  if (!rows.length) return ElMessage.warning('请选择待审核订单')
  rejectRows.value = rows
  batchReason.value = ''
  rejectDialog.value = true
}

async function confirmBatchReject() {
  const reason = (batchReason.value || '').trim()
  if (!reason) return ElMessage.warning('请选择或填写驳回原因')
  rejecting.value = true
  let ok = 0
  try {
    for (const row of rejectRows.value) {
      try {
        await orderReview(row.order_no, false, reason)
        notifyReject(row.order_no)
        ok++
      } catch { /* 单条失败跳过 */ }
    }
    ElMessage.success(`已驳回 ${ok} 个订单，已通知提交方`)
    rejectDialog.value = false
    dt.value.reload()
    loadCounts()
  } finally { rejecting.value = false }
}

// 驳回后站内消息通知提交方（商家侧通知中心可见）
async function notifyReject(orderNo) {
  try { await notifSend({ orderNo, node: 'CREATED', role: 'merchant', channel: 'push' }) } catch { /* 通知失败不影响审核结果 */ }
}

const reviewMeta = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

const columns = [
  { prop: 'order_no', label: '订单号', minWidth: 190 },
  { prop: 'merchant_name', label: '商家', minWidth: 100 },
  { prop: 'destination_country', label: '目的地', minWidth: 70 },
  { prop: 'review_status', label: '审核状态', minWidth: 80 },
  { prop: 'freight_cost', label: '运费', minWidth: 80 },
  { prop: 'created_at', label: '下单时间', minWidth: 150, type: 'datetime' }
]

function fmt(v) { return fmtDateTime(v) }
function safeJson(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}

const setDesc = inject('setPageDesc')

onMounted(() => {
  setDesc('新建订单需审核通过后才能发货；驳回需填写原因')
  loadChannels()
  loadCounts()
})

// 渠道名映射：把裸 ID 转成用户看得懂的名称
const channels = ref([])
async function loadChannels() {
  try { channels.value = (await tmsChannelAll()) || [] } catch { channels.value = [] }
}
function channelName(id) {
  const c = channels.value.find((x) => Number(x.id) === Number(id))
  return c ? c.channel_name : (id ?? '-')
}

// 商家信誉：聚合该商家历史订单，辅助审核判断可信度
const merchantStats = ref({ total: 0, approved: 0, anomaly: 0, returned: 0 })
const passRate = computed(() => (merchantStats.value.total ? Math.round((merchantStats.value.approved / merchantStats.value.total) * 100) : 0))
const merchantRisk = computed(() => {
  const s = merchantStats.value
  if (!s.total) return ''
  if (s.returned / s.total > 0.2) return '该商家退货率偏高，建议重点核实'
  if (s.anomaly > 5) return '该商家历史异常较多，建议谨慎放行'
  return ''
})
async function loadMerchantStats(merchantId) {
  if (!merchantId) return
  try {
    const r = await tmsOrderList({ merchantId, page: 1, perPage: 1000 })
    const rows = r.rows || []
    const ANOMALY = ['CUSTOMS_DELAY', 'DELIVERY_FAILED', 'LOST', 'RETURNED']
    merchantStats.value = {
      total: rows.length,
      approved: rows.filter((o) => String(o.review_status) === 'APPROVED').length,
      anomaly: rows.filter((o) => ANOMALY.includes(o.current_node)).length,
      returned: rows.filter((o) => o.current_node === 'RETURNED').length
    }
  } catch { merchantStats.value = { total: 0, approved: 0, anomaly: 0, returned: 0 } }
}
</script>

<style scoped>
.review-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.rs-card {
  flex: 1;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 14px 20px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, border-color 0.15s;
}
.rs-card:hover {
  transform: translateY(-1px);
  border-color: #0891b2;
  box-shadow: 0 3px 10px rgba(29, 33, 41, 0.06);
}
.rs-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.rs-icon {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rs-meta { flex: 1; min-width: 0; }
.rs-label {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}
.rs-value {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #1d2129;
}
.rs-value.alert {
  color: #ef4444;
}
.rs-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}
/* 风险提示：紧凑单行，红色警示 */
.risk-slim {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin-bottom: 12px;
  background: #fdf3f3;
  border: 1px solid #f5c6c6;
  border-left: 3px solid #ef4444;
  border-radius: 6px;
  font-size: 12px;
}
.risk-slim-icon { color: #ef4444; flex: none; }
.risk-slim-title { font-weight: 600; color: #dc2626; flex: none; }
.risk-slim-items { color: #c05050; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }
.drawer-total {
  margin-top: 8px;
  font-size: 13px;
  color: #5f6368;
}
.drawer-total b { color: #0891b2; }

/* 审核弹窗：品牌渐变头部 + 卡片分区 */
.review-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  background: linear-gradient(135deg, #0891b2, #06b6d4);
  border-radius: 8px 8px 0 0;
}
.review-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}
.review-dialog :deep(.el-dialog__body) {
  padding: 18px 20px;
}
.review-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #f0f1f2;
}
.rd-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.rd-head-right { display: flex; align-items: center; gap: 12px; }
.rd-newtab { color: #fff !important; }
.rd-title { font-size: 16px; font-weight: 600; color: #fff; }
.rd-sub { font-size: 12px; color: rgba(255, 255, 255, 0.85); margin-top: 2px; }
/* 白色分区卡片 */
.rd-block {
  background: #fff;
  border: 1px solid #eef1f4;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.rd-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 10px;
}
.rd-block-title::before {
  content: '';
  width: 4px;
  height: 14px;
  background: #0891b2;
  border-radius: 2px;
}
.rd-list { display: flex; flex-direction: column; }
.rd-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 7px 0;
  border-bottom: 1px dashed #f0f2f4;
}
.rd-item:last-child { border-bottom: none; }
.rd-item-k { flex: none; width: 74px; font-size: 12px; color: #86909c; }
.rd-item-v { flex: 1; font-size: 13px; color: #1d2129; min-width: 0; word-break: break-all; }
.rd-item-alert { background: #fdf3f3; border-radius: 6px; padding: 7px 10px; border-bottom: none; margin-top: 4px; }


/* 历史审核记录 */
.rd-log { display: flex; flex-direction: column; gap: 6px; }
.rd-log-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #5f6368;
  background: #f8fafc;
  border: 1px solid #eef1f4;
  border-radius: 6px;
  padding: 6px 10px;
}
.rd-log-time { color: #9ca3af; white-space: nowrap; }
.rd-log-op { font-weight: 500; }
.rd-log-detail { flex: 1; min-width: 0; }

/* 必填星号 */
.req-star { color: #f56c6c; margin-left: 2px; font-weight: 400; }

/* 商家信誉 */
.rd-stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.rd-stat {
  background: #f8fafc;
  border: 1px solid #eef1f4;
  border-radius: 8px;
  padding: 8px 6px;
  text-align: center;
}
.rd-stat-k { font-size: 11px; color: #86909c; }
.rd-stat-v { margin-top: 3px; font-size: 16px; font-weight: 700; color: #1d2129; }
.stat-warn { color: #ef4444; }
.merchant-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #b45309;
  background: #fef3ec;
  border: 1px solid #f5c9b4;
  border-radius: 6px;
  padding: 6px 10px;
}

/* 驳回快捷原因：单选 */
.rj-radio { margin-bottom: 8px; }
.rj-radio .el-radio-group { display: flex; flex-wrap: wrap; gap: 6px; }

/* 批量驳回弹窗 */
.rj-wrap { display: flex; flex-direction: column; gap: 14px; }
.rj-tip { font-size: 13px; color: #5f6368; line-height: 1.6; }
</style>
