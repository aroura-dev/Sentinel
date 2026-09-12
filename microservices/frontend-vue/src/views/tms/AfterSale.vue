<template>
  <div>
    <!-- 售后总览横幅（与订单/运单同款） -->
    <div v-if="snapshotLoaded" class="as-banner">
      <div class="b-head">
        <el-icon :size="15"><Service /></el-icon>
        <span class="b-title">售后总览</span>
        <span class="b-tip">退货/换货从受理到退款、重发，这里一眼看全</span>
      </div>
      <div class="b-stats">
        <span class="b-item">受理中 <b>{{ pendingCount }}</b> 单</span>
        <span class="b-item">退款中 <b>{{ refundingCount }}</b> 单</span>
        <span class="b-item">已退款 <b>{{ refundedCount }}</b> 单</span>
        <span class="b-item">已重发 <b>{{ reshippedCount }}</b> 单</span>
        <span class="b-item">累计退款 <b>¥{{ refundedTotal }}</b></span>
      </div>
      <div v-if="pendingCount" class="b-warn">
        有 <b>{{ pendingCount }}</b> 单售后在受理中，请尽快处理退款或换货重发。
      </div>
      <div v-else-if="refundingCount" class="b-warn ok">
        有 <b>{{ refundingCount }}</b> 单退款处理中，完成后自动进入「已退款」。
      </div>
      <div v-else class="b-warn ok">最近没有待受理的售后退货，售后流转顺畅，辛苦啦。</div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 200px" @clear="load" @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option v-for="(v, k) in STATUS_LABELS" :key="k" :label="v" :value="k" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <div class="spacer" />
        <el-button type="success" @click="openRegister()">登记退货</el-button>
      </div>

      <div class="as-note">
        <b>关于金额：</b>退货会按订单申报的货值退还货款；换货不用退钱，我们会安排重新发货。点击任意一行即可查看详情，并按当前状态处理（受理退款、换货重发、关闭售后）。
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small" :empty-text="emptyText" @row-click="onRowClick">
        <el-table-column label="订单号" min-width="200" align="left">
          <template #default="{ row }">
            <router-link :to="'/tms/order/' + row.order_no" style="color: #0891b2; text-decoration: none">{{ row.order_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="买家" min-width="110" align="center">
          <template #default="{ row }">{{ row.buyer_name || row.buyer_id || '-' }}</template>
        </el-table-column>
        <el-table-column label="售后方式" width="160" align="center">
          <template #default="{ row }">
            <template v-if="row.type !== 'EXCHANGE'">
              <span class="type-prefix">退货退款</span><span class="money">¥{{ Number(row.refund_amount || 0).toFixed(2) }}</span>
              <span v-if="row.currency && row.currency !== 'CNY'" class="cur">{{ row.currency }}</span>
            </template>
            <template v-else>
              <span class="exch">换货重发</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.status)">{{ STATUS_LABELS[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" min-width="176" align="center">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" style="margin-right: 8px" @click="openDetail(row, false)">详情</el-link>
            <el-link v-if="['PENDING','REFUNDED','RESHIPPED'].includes(row.status)" type="success" :underline="false" @click="openDetail(row, true)">处理</el-link>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
        :page-size="query.perPage" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />
    </el-card>

    <!-- 登记退货：自动带单号，可先校验订单 -->
    <el-dialog v-model="regDialog" title="登记退货 / 换货" width="480px">
      <el-form :model="reg" label-width="96px">
        <el-form-item label="订单号" required>
          <el-input v-model="reg.orderNo" placeholder="输入待退/换订单号（自动校验订单）" @blur="lookupOrder" @keyup.enter="lookupOrder" />
        </el-form-item>
        <template v-if="checkedOrder">
          <el-form-item label="商家"><span>{{ checkedOrder.merchant_name || '-' }}</span></el-form-item>
          <el-form-item label="货值"><span>¥{{ Number(checkedOrder.declared_value || 0).toFixed(2) }}</span></el-form-item>
          <el-form-item label="运费"><span>¥{{ Number(checkedOrder.freight_cost || 0).toFixed(2) }}</span></el-form-item>
          <el-form-item label="商品">{{ orderItemsText }}</el-form-item>
          <el-alert type="info" :closable="false" show-icon title="将按订单申报货值生成退款金额，确认后进入「受理中」。" style="margin-bottom: 12px" />
        </template>
        <el-form-item label="类型">
          <el-select v-model="reg.type" style="width: 100%">
            <el-option label="退货退款" value="RETURN" />
            <el-option label="换货重发" value="EXCHANGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="reg.reason" type="textarea" :rows="3" placeholder="填退货/换货原因（如破损、错发、不想要）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regDialog = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="doRegister">提交</el-button>
      </template>
    </el-dialog>

    <!-- 售后单详情抽屉 -->
    <el-drawer v-model="detailOpen" :title="detail?.order_no ? '售后单 ' + detail.order_no : '售后详情'" size="520px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">
            <router-link :to="'/tms/order/' + detail.order_no" style="color: #0891b2; text-decoration: none">{{ detail.order_no }}</router-link>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag size="small" :type="tagType(detail.status)">{{ STATUS_LABELS[detail.status] || detail.status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="退款金额"><span class="money">¥{{ Number(detail.refund_amount || 0).toFixed(2) }}</span>（{{ detail.currency || 'CNY' }}）</el-descriptions-item>
          <el-descriptions-item label="原因">{{ detail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyer_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ detail.buyer_address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ fmt(detail.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ fmt(detail.updated_at) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="orderDetail" class="drawer-section">关联订单</div>
        <el-descriptions v-if="orderDetail" :column="1" border size="small">
          <el-descriptions-item label="商家">{{ orderDetail.merchant_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前进度"><StatusTag :status="orderDetail.current_node" /></el-descriptions-item>
          <el-descriptions-item label="货值">¥{{ Number(orderDetail.declared_value || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ Number(orderDetail.freight_cost || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="商品明细">{{ orderItemsText2 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailAct && detail && ['PENDING','REFUNDED','RESHIPPED'].includes(detail.status)" class="drawer-section">处理方案</div>
        <div v-if="detailAct && detail && ['PENDING','REFUNDED','RESHIPPED'].includes(detail.status)" class="plan-desc">请按售后单情况选择方案执行，处理结果会同步更新到列表与订单节点：</div>
        <div v-if="detailAct && detail && ['PENDING','REFUNDED','RESHIPPED'].includes(detail.status)" class="act-row">
          <el-button @click="doAction(detail, 'close'); detailOpen = false">关闭售后</el-button>
          <el-button v-if="detail.status === 'PENDING'" type="warning" @click="doAction(detail, 'refund'); detailOpen = false">受理退款</el-button>
          <el-button v-if="detail.status === 'PENDING'" type="primary" @click="doAction(detail, 'reship'); detailOpen = false">换货重发</el-button>
        </div>
      </template>
      <div v-else class="drawer-loading">加载中…</div>
      <template #footer>
        <el-button @click="detailOpen = false">返回</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { afterSaleList, afterSaleDetail, afterSaleRegister, afterSaleRefund, afterSaleReship, afterSaleClose, tmsOrderDetail } from '../../api'
import { fmtDateTime } from '../../utils/format'

const STATUS_LABELS = { PENDING: '受理中', REFUNDING: '退款中', REFUNDED: '已退款', RESHIPPED: '已重发', CLOSED: '已关闭' }
const TYPE_LABELS = { RETURN: '退货退款', EXCHANGE: '换货重发' }
const tagType = (s) => ({ PENDING: 'warning', REFUNDING: 'primary', REFUNDED: 'success', RESHIPPED: 'primary', CLOSED: 'info' }[s] || 'info')
const typeLabel = (t) => TYPE_LABELS[t] || t || '退货退款'

const setDesc = inject('setPageDesc')
const route = useRoute()

const rows = ref([])
const total = ref(0)
const emptyText = '暂无售后记录 — 有退货 / 换货需求时，点击右上角「登记退货」，受理后处理进度会同步展示在这里'
const loading = ref(false)
const query = reactive({ orderNo: '', status: '', page: 1, perPage: 10 })
// 概览快照：拉取整表算计数与累计金额
const snapshot = ref([])
const snapshotLoaded = ref(false)

const regDialog = ref(false)
const regLoading = ref(false)
const looking = ref(false)
const reg = reactive({ orderNo: '', type: 'RETURN', reason: '' })
const checkedOrder = ref(null)
// 登记退货：订单号输入停顿后自动校验，无需人工点“校验”
let lookupTimer = null
watch(
  () => reg.orderNo,
  (v) => {
    clearTimeout(lookupTimer)
    if (!v || !String(v).trim()) { checkedOrder.value = null; return }
    lookupTimer = setTimeout(() => lookupOrder(), 600)
  }
)

const detailOpen = ref(false)
const detail = ref(null)
const detailAct = ref(false)
const orderDetail = ref(null)

const pendingCount = computed(() => snapshot.value.filter((r) => r.status === 'PENDING').length)
const refundingCount = computed(() => snapshot.value.filter((r) => r.status === 'REFUNDING').length)
const refundedCount = computed(() => snapshot.value.filter((r) => r.status === 'REFUNDED').length)
const reshippedCount = computed(() => snapshot.value.filter((r) => r.status === 'RESHIPPED').length)
const refundedTotal = computed(() => snapshot.value
  .filter((r) => r.status === 'REFUNDED' || r.status === 'CLOSED')
  .reduce((s, r) => s + Number(r.refund_amount || 0), 0).toFixed(2))

const fmt = (v) => fmtDateTime(v)

// 商品行文本（登记预填展示）
const orderItemsText = computed(() => checkedOrder.value ? summarizeItems(checkedOrder.value) : '')
const orderItemsText2 = computed(() => orderDetail.value ? summarizeItems(orderDetail.value) : '')

function summarizeItems(o) {
  const arr = safeParse(o.items_json)
  if (!arr.length) return '-'
  return arr.map((it) => `${it.sku || it.product_id}×${it.qty}`).join('、')
}

function safeParse(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}

async function load() {
  loading.value = true
  try {
    const data = await afterSaleList({ ...query })
    rows.value = data.rows || []
    total.value = data.count || 0
  } catch (e) { rows.value = []; total.value = 0 } finally { loading.value = false }
}

async function loadSnapshot() {
  try {
    const data = await afterSaleList({ orderNo: '', status: '', page: 1, perPage: 5000 })
    snapshot.value = data.rows || []
  } catch { snapshot.value = [] } finally { snapshotLoaded.value = true }
}

function openRegister(orderNo) {
  checkedOrder.value = null
  Object.assign(reg, { orderNo: orderNo || '', type: 'RETURN', reason: '' })
  regDialog.value = true
}

// 登记前校验订单：能查到才可提交，避免误登记
async function lookupOrder() {
  if (!reg.orderNo) return
  looking.value = true
  try {
    const o = await tmsOrderDetail(reg.orderNo.trim())
    checkedOrder.value = o || null
  } catch {
    checkedOrder.value = null
    ElMessage.warning(`未找到订单：${reg.orderNo}`)
  } finally { looking.value = false }
}

async function doRegister() {
  if (!reg.orderNo) return ElMessage.warning('请填写订单号')
  if (!checkedOrder.value) {
    const ok = await ElMessageBox.confirm(`未校验到订单 ${reg.orderNo}，仍要提交吗？`, '提示', { type: 'warning' }).catch(() => false)
    if (!ok) return
  }
  regLoading.value = true
  try {
    const res = await afterSaleRegister({ ...reg })
    ElMessage.success(`已登记售后单（${STATUS_LABELS[res.status] || res.status}）`)
    regDialog.value = false
    checkedOrder.value = null
    query.orderNo = reg.orderNo
    query.page = 1
    load(); loadSnapshot()
  } finally { regLoading.value = false }
}

async function doAction(row, action) {
  const label = { refund: '退款', reship: '换货重发', close: '关闭' }[action]
  await ElMessageBox.confirm(`确认对订单 ${row.order_no} 执行「${label}」？`, '逆向售后', { type: 'warning' })
  const api = { refund: afterSaleRefund, reship: afterSaleReship, close: afterSaleClose }[action]
  const res = await api(row.id)
  ElMessage.success(res.status ? `操作完成：${STATUS_LABELS[res.status] || res.status}` : '操作完成')
  load(); loadSnapshot()
}

// 操作列统一下拉：detail 打开详情，其余走状态流转
function onCmd(row, cmd) {
  if (cmd === 'detail') return openDetail(row)
  doAction(row, cmd)
}
function onRowClick(row, column, event) {
  if (event && event.target && event.target.closest && event.target.closest('a, button')) return
  openDetail(row, true)
}

async function openDetail(row, act = true) {
  detail.value = null
  orderDetail.value = null
  detailAct.value = act !== false
  detailOpen.value = true
  try { detail.value = await afterSaleDetail(row.id) } catch { detail.value = null }
  const no = (detail.value && detail.value.order_no) || row.order_no
  try { orderDetail.value = await tmsOrderDetail(no) } catch { orderDetail.value = null }
}

onMounted(() => {
  setDesc('售后退货闭环：登记退货/换货 → 受理 → 退款/重发 → 关闭')
  // 从订单详情「发起售后」带入单号：自动按单筛选并弹出登记
  if (route.query.orderNo) {
    query.orderNo = route.query.orderNo
    openRegister(route.query.orderNo)
  }
  load(); loadSnapshot()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; align-items: center; }
.toolbar .spacer { flex: 1; }
.as-note { margin: -2px 0 12px; padding: 9px 12px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.6; }
.as-note b { color: #155e75; }
.pager { margin-top: 14px; justify-content: flex-end; }
.money { color: #0891b2; font-weight: 600; }
.cur { font-size: 12px; color: #86909c; margin-left: 2px; }
.no-refund { font-size: 12px; color: #9ca3af; }
.exch { font-size: 12px; color: #0f766e; background: #ecfdf5; border: 1px solid #d1fae5; border-radius: 4px; padding: 2px 8px; }
.act-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 4px; }
.plan-desc { font-size: 12px; color: #57606a; margin: -4px 0 10px; }
.muted { color: #9ca3af; }
/* 长内容单行 + 省略号，溢出悬停显示全文 */
:deep(.el-table .cell) { white-space: nowrap; }
:deep(.el-table__row) { cursor: pointer; }
.items-cell { color: #57606a; }
.type-prefix { color: #0891b2; font-weight: 500; margin-right: 4px; }

/* 售后总览横幅（与订单/运单同款样式） */
.as-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.as-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.b-head { display: flex; align-items: center; gap: 8px; }
.b-head .el-icon { color: #0891b2; }
.b-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.b-tip { font-size: 12px; color: #9ca3af; }
.b-stats { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 26px; margin-top: 12px; }
.b-item { font-size: 14px; color: #57606a; }
.b-item b { font-size: 18px; color: #1d2129; margin-left: 2px; }
.b-warn { margin-top: 12px; font-size: 13px; color: #b7791f; background: #fef6ec; border: 1px solid #f5e1c3; border-radius: 6px; padding: 8px 12px; }
.b-warn b { color: #d97706; }
.b-warn.ok { color: #0e7490; background: #eef8fa; border-color: #d9f0f4; }

.order-pick { display: flex; gap: 8px; width: 100%; }
.drawer-section { font-weight: 600; margin: 16px 0 8px; color: #1d2129; }
.drawer-loading { color: #9ca3af; text-align: center; padding: 40px 0; }
</style>
