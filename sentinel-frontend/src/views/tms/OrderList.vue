<template>
  <div>
    <!-- 订单总览：温馨横幅（参考运单总览） -->
    <div v-if="orderStat" class="od-banner">
      <div class="b-head">
        <el-icon :size="15"><List /></el-icon>
        <span class="b-title">订单总览</span>
        <span class="b-tip">从下单到签收，这里看每一单现在走到哪一步</span>
      </div>
      <div class="b-stats">
        <span class="b-item">待审核 <b>{{ orderStat.pending }}</b> 单</span>
        <span class="b-item">待发货 <b>{{ orderStat.toShip }}</b> 单</span>
        <span class="b-item">运输中 <b>{{ orderStat.inTransit }}</b> 单</span>
        <span class="b-item">已签收 <b>{{ orderStat.delivered }}</b> 单</span>
      </div>
      <div v-if="orderStat.pending" class="b-warn">
        有 <b>{{ orderStat.pending }}</b> 单在等你审核，审核通过后就能安排发货。
      </div>
      <div v-else-if="orderStat.toShip" class="b-warn ok">
        有 <b>{{ orderStat.toShip }}</b> 单已通过审核、可以发货了，勾选后点「批量发货」即可。
      </div>
      <div v-else class="b-warn ok">最近没有待处理的单，订单流转顺畅，辛苦啦。</div>
    </div>

    <DataTable ref="dt" title="订单" empty-text="还没有订单。点击『新建订单』创建第一单，或『批量导入』快速建档。"
      :columns="columns" :load="tmsOrderList" :query="query" selectable :action-width="300">
      <!-- 筛选区：条件 + 查询/重置 -->
      <template #query="{ reload }">
        <div class="filter-bar">
          <div class="f-item">
            <span class="f-label">商家</span>
            <el-select v-model="query.merchantId" placeholder="全部商家" filterable clearable :teleported="false" style="width: 140px" @change="reload">
              <el-option v-for="m in merchants" :key="m.id" :label="m.merchant_name" :value="m.id" />
            </el-select>
          </div>
          <div class="f-item">
            <span class="f-label">渠道</span>
            <el-select v-model="query.channelId" placeholder="全部渠道" filterable clearable :teleported="false" style="width: 140px" @change="reload">
              <el-option v-for="c in channels" :key="c.id" :label="c.channel_name" :value="c.id" />
            </el-select>
          </div>
          <div class="f-item">
            <span class="f-label">节点</span>
            <el-select v-model="query.node" placeholder="全部节点" clearable :teleported="false" style="width: 130px" @change="reload">
              <el-option v-for="n in nodes" :key="n.codeEn" :label="n.description" :value="n.codeEn" />
            </el-select>
          </div>
          <div class="f-item">
            <span class="f-label">订单号</span>
            <el-input v-model="query.keyword" placeholder="输入订单号" clearable style="width: 140px" @keyup.enter="reload" />
          </div>
          <div class="f-item">
            <span class="f-label">创建时间</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px"
              @change="onDateChange"
            />
          </div>
          <div class="f-actions">
            <el-button :type="query.node === 'ANOMALY' ? 'danger' : 'default'" plain @click="toggleAnomaly(reload)">
              <el-icon><Warning /></el-icon>只看异常
            </el-button>
            <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
            <el-button @click="resetQuery(reload)"><el-icon><RefreshRight /></el-icon>重置</el-button>
          </div>
        </div>
      </template>

      <template #cell-items_json="{ row }">
        <div class="cell-prod">
          <el-image v-if="firstSkuImg(row)" :src="firstSkuImg(row)" fit="cover" class="prod-img"
            :preview-src-list="[firstSkuImg(row)]" preview-teleported />
          <span v-else class="prod-img prod-img-empty">-</span>
          <span class="prod-name" :title="firstSkuName(row)">{{ firstSkuName(row) }}<i v-if="itemQty(row) > 1">×{{ itemQty(row) }}</i></span>
        </div>
      </template>
      <template #cell-current_node="{ row }"><StatusTag :status="row.current_node" /></template>
      <template #cell-sla_status="{ row }"><StatusTag :status="row.sla_status" /></template>
      <template #cell-review_status="{ row }">
        <el-tag :type="(reviewMeta[row.review_status] && reviewMeta[row.review_status].type) || 'info'" size="small">
          {{ (reviewMeta[row.review_status] && reviewMeta[row.review_status].label) || row.review_status }}
        </el-tag>
      </template>
      <template #cell-destination_country="{ row }">{{ countryLabel(row.destination_country) }}</template>
      <template #cell-freight_cost="{ row }"><span class="cell-freight">¥{{ Number(row.freight_cost || 0).toFixed(2) }}</span></template>
      <template #actions="{ row }">
        <!-- 待发货：整单发货 / 分批发货 / 更多 -->
        <template v-if="row.current_node === 'CREATED' && !row.waybill_no">
          <el-button v-if="canOps" size="small" type="success" :loading="genLoading === row.order_no" @click="outbound(row)"><el-icon><Promotion /></el-icon>整单发货</el-button>
          <el-button v-if="canOps" size="small" type="warning" @click="openPartial(row)"><el-icon><Van /></el-icon>分批发货</el-button>
          <el-dropdown @command="(c) => rowMore(row, c)">
            <el-button size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="cancel" divided>取消订单</el-dropdown-item>
                <el-dropdown-item command="detail">详情</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <!-- 运输中 / 终态：打印发货单 / 详情 -->
        <template v-else>
          <el-button v-if="row.waybill_no" size="small" :loading="printing === row.order_no" @click="printOne(row)"><el-icon><Printer /></el-icon>打印发货单</el-button>
          <el-button size="small" link type="primary" @click="goDetail(row)">详情</el-button>
        </template>
      </template>
    </DataTable>

    <!-- 批量操作栏：贴近表格，勾选后在这里操作 -->
    <div class="batch-bar">
      <span class="bb-tip">勾选订单后可批量操作：</span>
      <el-button @click="exportCsv"><el-icon><Download /></el-icon>导出</el-button>
      <el-button @click="openImport"><el-icon><Upload /></el-icon>批量导入</el-button>
      <el-button type="info" :disabled="!printableSelected.length" @click="batchPrint">
        <el-icon><Printer /></el-icon>批量打印发货单（{{ printableSelected.length }}）
      </el-button>
      <el-button v-if="canOps" type="success" :disabled="!selected.length" :loading="batchLoading" @click="batchOutbound">
        <el-icon><Promotion /></el-icon>批量发货（{{ selected.length }}）
      </el-button>
    </div>

    <!-- 编辑订单弹窗：基础信息 + 业务备注（已发货收货信息锁定） -->
    <el-dialog v-model="editDialog" title="编辑订单信息" width="520px">
      <el-form :model="editForm" label-width="96px">
        <el-form-item label="收件人电话">
          <el-input v-model="editForm.buyerPhone" :disabled="outboundLock" />
        </el-form-item>
        <el-form-item label="收件人语言">
          <el-select v-model="editForm.buyerLanguage" :disabled="outboundLock" style="width: 100%">
            <el-option label="简体中文" value="zh" />
          </el-select>
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="editForm.buyerAddress" :disabled="outboundLock" />
        </el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.buyerCity" :disabled="outboundLock" /></el-form-item>
        <el-form-item label="邮编"><el-input v-model="editForm.buyerPostal" :disabled="outboundLock" /></el-form-item>
        <el-form-item label="业务备注">
          <el-input v-model="editForm.businessNotes" type="textarea" :rows="3" placeholder="跨角色协同批注：备注内容对相关人员可见，发货后仍可补充" />
        </el-form-item>
        <el-alert v-if="outboundLock" type="warning" :closable="false" title="订单已发货，收货信息已锁定" description="如需修改收件人 / 地址，请走「申请退回」流程；此处仅可补充业务备注。" />
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分批发货弹窗：勾选本次发货商品 -->
    <el-dialog v-model="partialDialog" title="分批发货 — 选择本次发货商品" width="640px">
      <div v-if="partialOrder">
        <p style="margin-top:0;color:#606266">订单 <b>{{ partialOrder.order_no }}</b> · {{ partialOrder.merchant_name }} · 剩余可发商品如下，勾选后生成新运单（可分多次发出，订单保持整单可查）。</p>
        <el-table :data="partialItems" border size="small" @selection-change="onPartialSelect">
          <el-table-column type="selection" width="40" />
          <el-table-column align="left" prop="sku" label="SKU" min-width="120" />
          <el-table-column prop="qty" label="订单数量" width="90" />
          <el-table-column label="剩余可发" width="90">
            <template #default="{ row }"><b :class="{ zero: row.remaining <= 0 }">{{ row.remaining }}</b></template>
          </el-table-column>
          <el-table-column label="本次发货数量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.shipQty" :min="0" :max="row.remaining" size="small" :disabled="row.remaining <= 0" />
            </template>
          </el-table-column>
        </el-table>
        <p style="color:#9ca3af;font-size:12px">本次发货数量默认为剩余可发数量；可分多次把商品分批发出，每次生成一张运单。</p>
      </div>
      <template #footer>
        <el-button @click="partialDialog = false">取消</el-button>
        <el-button type="warning" :disabled="!partialSelected.length" :loading="partialLoading" @click="submitPartial">生成发货运单</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importDialog" title="批量导入订单" width="560px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        :title="`CSV 格式：${IMPORT_HEADER}`"
        description="每行一单；商品明细用「SKU:数量」，多商品用分号分隔。导入后新单进入「订单审核」待放行。" />
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-button @click="downloadImportTemplate"><el-icon><Download /></el-icon>下载导入模板</el-button>
      </div>
      <el-upload drag :auto-upload="false" accept=".csv" :show-file-list="false" :on-change="onImportFile">
        <el-icon style="font-size:40px;color:#c0c4cc"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 CSV 到此处，或<em>点击选择文件</em></div>
      </el-upload>
      <el-alert v-if="importResult" :type="importResult.fail ? 'warning' : 'success'" :closable="false" style="margin-top:12px">
        <template #title>
          导入完成：成功 {{ importResult.ok }} 单，失败 {{ importResult.fail }} 单
        </template>
        <div v-for="(msg, i) in importResult.fails" :key="i" style="font-size:12px;color:#b3271d">{{ msg }}</div>
      </el-alert>
    </el-dialog>

    <!-- 发货单打印：弹窗渲染发货单，浏览器打印 -->
    <el-dialog v-model="printDialog" title="发货单打印预览" width="720px" class="print-dialog">
      <div class="print-area">
        <div v-for="slip in printSlips" :key="slip.waybill_no" class="slip">
          <div class="slip-head">
            <div class="slip-title">货物发货单</div>
            <div class="slip-no">运单号：{{ slip.waybill_no }}</div>
          </div>
          <div class="slip-row"><span>订单号：{{ slip.order_no }}</span><span>商家：{{ slip.merchant_name }}</span></div>
          <div class="slip-row"><span>承运商：{{ slip.carrier_code || '-' }}</span><span>追踪号：{{ slip.tracking_no || '-' }}</span></div>
          <div class="slip-row"><span>收件人：{{ slip.buyer_id }}</span><span>电话：{{ slip.buyer_phone || '-' }}</span></div>
          <div class="slip-row"><span>地址：{{ [slip.buyer_address, slip.buyer_city, slip.buyer_postal].filter(Boolean).join(' ') }}</span></div>
          <div class="slip-row"><span>收费重量：{{ slip.weight_kg ?? '-' }} kg</span><span>运费：¥{{ Number(slip.freight_cost || 0).toFixed(2) }}</span><span>发货日期：{{ slip.date }}</span></div>
          <table class="slip-table">
            <thead><tr><th>SKU</th><th>数量</th><th>单件重量(kg)</th><th>单件价值(¥)</th></tr></thead>
            <tbody>
              <tr v-for="(it, i) in slip.items" :key="i">
                <td>{{ it.sku }}</td><td>{{ it.qty }}</td>
                <td>{{ it.unit_weight_kg ?? '-' }}</td><td>{{ it.unit_declared_value ?? '-' }}</td>
              </tr>
              <tr v-if="!slip.items.length"><td colspan="4" style="text-align:center;color:#9ca3af">无商品明细</td></tr>
            </tbody>
          </table>
          <div class="slip-foot">此单据为发货凭证，与运单号一致，供发货仓与承运商交接使用。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="printDialog = false">关闭</el-button>
        <el-button type="primary" @click="doPrint">打印发货单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useAuthStore } from '../../store/auth'
import { fmtDateTime } from '../../utils/format'
import { countryLabel } from '../../utils/country'
import { tmsOrderList, tmsOrderDetail, waybillGenerate, waybillList, waybillDetail,
  tmsChannelAll, logisticsNodes, merchantAll,
  tmsCancelOrder, tmsUpdateOrder, notifSend, tmsOrderCreate, productByMerchant, productList } from '../../api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const channels = ref([])
const nodes = ref([])
const merchants = ref([])
const genLoading = ref('')
const batchLoading = ref(false)
const printing = ref('')
const imgMap = ref({})
const skuNameMap = ref({})

// ---------- 角色权限（按钮级） ----------
const canOps = computed(() => ['ADMIN', 'OPERATOR'].includes(auth.role))
const canCreate = computed(() => ['ADMIN', 'OPERATOR', 'MERCHANT'].includes(auth.role))

// ---------- 筛选记忆（localStorage，用户条件持久化） ----------
const QKEY = 'sentinel_tms_order_query'
function loadSavedQuery() {
  try { return JSON.parse(localStorage.getItem(QKEY) || 'null') } catch { return null }
}
function persistQuery() {
  try { localStorage.setItem(QKEY, JSON.stringify({ ...query, dateRange: dateRange.value })) } catch { /* 忽略 */ }
}

const saved = loadSavedQuery()
const query = reactive({
  merchantId: route.query.merchantId ? Number(route.query.merchantId) : (saved?.merchantId || null),
  channelId: route.query.channelId ? Number(route.query.channelId) : (saved?.channelId || null),
  slaStatus: route.query.sla || (saved?.slaStatus || ''),
  node: route.query.node || (saved?.node || ''),
  startDate: route.query.start || (saved?.startDate || ''),
  endDate: route.query.end || (saved?.endDate || ''),
  keyword: saved?.keyword || ''
})
const dateRange = ref(query.startDate && query.endDate ? [query.startDate, query.endDate] : null)
watch(() => ({ ...query }), persistQuery, { deep: true })

// 顶部统计卡片 / 跨模块下钻：路由 query 变化时重新应用筛选并刷新（如 ?node=IN_TRANSIT / ?sla=BREACHED）
watch(() => route.query, (q) => {
  const hasFilter = q.merchantId || q.channelId || q.sla || q.node || q.start || q.end
  if (!hasFilter) return // 无筛选参数（回到列表页）不覆盖当前条件
  query.merchantId = q.merchantId ? Number(q.merchantId) : query.merchantId
  query.channelId = q.channelId ? Number(q.channelId) : query.channelId
  query.slaStatus = q.sla || query.slaStatus
  query.node = q.node || query.node
  if (q.start && q.end) {
    query.startDate = q.start
    query.endDate = q.end
    dateRange.value = [q.start, q.end]
  }
  dt.value?.reload()
}, { deep: true })

const dt = ref(null)
const terminal = (node) => ['DELIVERED', 'LOST', 'RETURNED', 'CANCELED'].includes(node)

// SLA 违约 / 异常节点订单行：浅红高亮提醒
function resetQuery(reload) {
  query.merchantId = null
  query.channelId = null
  query.slaStatus = ''
  query.node = ''
  query.startDate = ''
  query.endDate = ''
  query.keyword = ''
  dateRange.value = null
  try { localStorage.removeItem(QKEY) } catch { /* 忽略 */ }
  if (reload) reload()
}

function onDateChange(range) {
  if (range && range.length === 2) {
    query.startDate = range[0]
    query.endDate = range[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
}

// 异常快捷筛选：一次筛出全部异常节点（中转延误/派送失败/丢件/退回）
function toggleAnomaly(reload) {
  query.node = query.node === 'ANOMALY' ? '' : 'ANOMALY'
  reload()
}
const selected = computed(() => dt.value?.selectedRows || [])
const printableSelected = computed(() => selected.value.filter((r) => r.waybill_no))

const reviewMeta = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

const columns = [
  { prop: 'items_json', label: '商品', width: 200 },
  { prop: 'order_no', label: '订单号', minWidth: 170 },
  { prop: 'merchant_name', label: '商家', minWidth: 100 },
  { prop: 'destination_country', label: '目的地', width: 90 },
  { prop: 'review_status', label: '审核', width: 90 },
  { prop: 'current_node', label: '节点', width: 110, type: 'tag' },
  { prop: 'sla_status', label: '时效', width: 90, type: 'tag' },
  { prop: 'freight_cost', label: '运费', width: 110, align: 'right' },
  { prop: 'declared_value', label: '货值', width: 110, type: 'money', align: 'right' },
  { prop: 'promise_eta', label: '预计送达时间', width: 150, type: 'datetime' }
]

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('审核通过后的订单；可整单 / 分批发货、取消、合并、打印发货单')
  channels.value = (await tmsChannelAll()) || []
  nodes.value = (await logisticsNodes()) || []
  // 已取消 / 异常 节点补入筛选项（StatusTag 已中文映射，避免下拉显示英文码）
  if (!nodes.value.some((n) => n.codeEn === 'CANCELED')) {
    nodes.value = [...nodes.value, { codeEn: 'CANCELED', description: '已取消' }]
  }
  if (!nodes.value.some((n) => n.codeEn === 'ANOMALY')) {
    nodes.value = [...nodes.value, { codeEn: 'ANOMALY', description: '异常' }]
  }
  merchants.value = (await merchantAll()) || []
  loadProductsImg()
  loadOrderStat()
})

// ---------- 订单总览（温馨横幅，参考运单总览） ----------
const orderStat = ref(null)

async function loadOrderStat() {
  try {
    const r = await tmsOrderList({ page: 1, perPage: 500 })
    const rows = r.rows || []
    const ANOMALY = ['CUSTOMS_DELAY', 'DELIVERY_FAILED', 'LOST', 'RETURNED']
    let pending = 0, toShip = 0, inTransit = 0, delivered = 0
    for (const o of rows) {
      const node = o.current_node
      if (node === 'DELIVERED') delivered++
      else if (ANOMALY.includes(node) || node === 'CANCELED') { /* 异常/已取消不进横幅统计 */ }
      else if (!o.waybill_no && String(o.review_status) === 'PENDING') pending++
      else if (!o.waybill_no) toShip++
      else inTransit++
    }
    orderStat.value = { pending, toShip, inTransit, delivered }
  } catch { orderStat.value = null }
}

// 商品图：SKU → 图片映射（列表第一件商品缩略图）
async function loadProductsImg() {
  try {
    const data = await productList({ page: 1, perPage: 500 })
    const m = {}
    const n = {}
    for (const p of (data.rows || [])) {
      if (p.sku && p.image_url) m[p.sku] = p.image_url
      if (p.sku && p.name) n[p.sku] = p.name
    }
    imgMap.value = m
    skuNameMap.value = n
  } catch { /* 图片加载失败忽略 */ }
}
function firstSkuImg(row) {
  const list = safeJson(row.items_json)
  const sku = list && list[0] && list[0].sku
  return sku ? (imgMap.value[sku] || '') : ''
}
function firstSkuName(row) {
  const list = safeJson(row.items_json)
  const sku = list && list[0] && list[0].sku
  return sku ? (skuNameMap.value[sku] || sku) : ''
}
function itemQty(row) {
  return safeJson(row.items_json).reduce((s, it) => s + Number(it.qty || 0), 0)
}

// ---------- 行内「更多」操作 ----------
async function rowMore(row, cmd) {
  if (cmd === 'edit') openEdit(row)
  else if (cmd === 'cancel') cancelOrder(row)
  else if (cmd === 'detail') goDetail(row)
  else if (cmd === 'print') printOne(row)
}

// ---------- 导出（前端 CSV） ----------
async function exportCsv() {
  try {
    const r = await tmsOrderList({ ...query, page: 1, perPage: 10000 })
    const rows = r.rows || []
    if (!rows.length) return ElMessage.warning('无数据可导出')
    const headers = ['订单号', '商家', '目的地', '审核', '节点', '时效', '运费', '货值', '创建时间']
    const lines = rows.map((o) => [o.order_no, o.merchant_name, countryLabel(o.destination_country), o.review_status, o.current_node, o.sla_status, o.freight_cost, o.declared_value, o.created_at])
    const csv = [headers, ...lines].map((r) => r.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `订单导出_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success(`已导出 ${rows.length} 条订单`)
  } catch (e) { ElMessage.error('导出失败') }
}

// ---------- 发货 ----------
async function outbound(row) {
  genLoading.value = row.order_no
  try {
    await ElMessageBox.confirm(`确认对订单 ${row.order_no} 整单发货并生成运单？`, '整单发货', { type: 'warning' })
    const res = await waybillGenerate(row.order_no)
    ElMessage.success(`已发货，运单号 ${res.waybill.waybill_no} 已生成，运费 ¥${res.quote.freight}`)
    dt.value.reload()
  } finally { genLoading.value = '' }
}

async function batchOutbound() {
  const pending = selected.value.filter((r) => r.current_node === 'CREATED' && !r.waybill_no)
  if (!pending.length) return ElMessage.warning('请选择待发货订单')
  await ElMessageBox.confirm(`确认对 ${pending.length} 个订单批量发货？`, '批量发货', { type: 'warning' })
  batchLoading.value = true
  let ok = 0
  try {
    for (const row of pending) {
      try { await waybillGenerate(row.order_no); ok++ } catch { /* 单条失败跳过 */ }
    }
    ElMessage.success(`批量发货完成：${ok}/${pending.length}`)
    dt.value.reload()
  } finally { batchLoading.value = false }
}

// ---------- 分批发货 ----------
const partialDialog = ref(false)
const partialOrder = ref(null)
const partialItems = ref([])
const partialSelected = ref([])
const partialLoading = ref(false)

async function openPartial(row) {
  const order = await tmsOrderDetail(row.order_no)
  const orderItems = safeJson(order.items_json)
  // 已发商品汇总（运单 items_json 累加）
  const shipped = {}
  try {
    const wbList = await waybillList({ orderNo: row.order_no, page: 1, perPage: 50 })
    for (const wb of (wbList.rows || [])) {
      for (const it of safeJson(wb.items_json)) {
        shipped[it.sku] = (shipped[it.sku] || 0) + Number(it.qty || 0)
      }
    }
  } catch { /* 已发商品查询失败不影响 */ }
  partialOrder.value = order
  partialItems.value = orderItems.map((it) => {
    const remaining = Math.max(0, Number(it.qty || 0) - (shipped[it.sku] || 0))
    return { ...it, remaining, shipQty: remaining }
  })
  partialSelected.value = []
  partialDialog.value = true
}

function onPartialSelect(sel) {
  partialSelected.value = sel
}

async function submitPartial() {
  const order = partialOrder.value
  // 取勾选行中发货数量 > 0 的条目，保留商品完整字段（重量/货值/货币）
  const subset = partialSelected.value.filter((it) => it.shipQty > 0).map((it) => ({ ...it, qty: it.shipQty }))
  if (!subset.length) return ElMessage.warning('请勾选并设置本次发货数量')
  await ElMessageBox.confirm(
    `确认分批发货订单 ${order.order_no} 的 ${subset.length} 个商品？将生成一张新运单，商品可在后续继续分批发出。`,
    '分批发货', { type: 'warning' })
  partialLoading.value = true
  try {
    const res = await waybillGenerate(order.order_no, { items: JSON.stringify(subset), partial: true })
    ElMessage.success(`分批发货完成：${res.waybill.waybill_no}，运费 ¥${res.quote.freight}`)
    partialDialog.value = false
    dt.value.reload()
  } finally { partialLoading.value = false }
}

// ---------- 编辑 + 业务备注 ----------
const editDialog = ref(false)
const editForm = reactive({})
const saving = ref(false)
const outboundLock = ref(false)
const editOrderNo = ref('')

function openEdit(row) {
  editOrderNo.value = row.order_no
  Object.assign(editForm, {
    buyerPhone: row.buyer_phone || '',
    buyerLanguage: row.buyer_language || 'zh',
    buyerAddress: row.buyer_address || '',
    buyerCity: row.buyer_city || '',
    buyerPostal: row.buyer_postal || '',
    businessNotes: row.business_notes || ''
  })
  outboundLock.value = !!row.waybill_no
  editDialog.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await tmsUpdateOrder(editOrderNo.value, editForm)
    ElMessage.success('订单信息已更新')
    editDialog.value = false
    dt.value.reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally { saving.value = false }
}

// ---------- 取消订单 ----------
async function cancelOrder(row) {
  await ElMessageBox.confirm(
    `取消后订单 ${row.order_no} 将结束、不可恢复。尚未发货可直接取消；已发货请走「申请退回」。确定取消吗？`,
    '取消订单', { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' })
  try {
    await tmsCancelOrder(row.order_no)
    // 站内消息推送取消通知
    try { await notifSend({ orderNo: row.order_no, node: 'CANCELED', role: 'merchant', channel: 'push' }) } catch { /* 通知失败不影响 */ }
    ElMessage.success('订单已取消，已同步通知商家')
    dt.value.reload()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '取消失败')
  }
}

// ---------- 打印发货单 ----------
const printDialog = ref(false)
const printSlips = ref([])

async function buildSlips(rows) {
  const slips = []
  for (const row of rows) {
    let order = row
    if (!order.items_json || !order.buyer_address) {
      try { order = await tmsOrderDetail(row.order_no) } catch { /* 用行数据兜底 */ }
    }
    let wb = null
    if (row.waybill_no) {
      try { wb = await waybillDetail(row.waybill_no) } catch { /* 运单缺失 */ }
    }
    slips.push({
      waybill_no: row.waybill_no || wb?.waybill_no || '-',
      order_no: row.order_no,
      merchant_name: order.merchant_name,
      carrier_code: wb?.carrier_code,
      tracking_no: wb?.tracking_no,
      buyer_id: order.buyer_id,
      buyer_phone: order.buyer_phone,
      buyer_address: order.buyer_address,
      buyer_city: order.buyer_city,
      buyer_postal: order.buyer_postal,
      weight_kg: wb?.weight_kg ?? order.weight_kg,
      freight_cost: wb?.freight_cost ?? order.freight_cost,
      items: safeJson(wb?.items_json || order.items_json),
      date: fmtDateTime(new Date())
    })
  }
  return slips
}

async function printOne(row) {
  printing.value = row.order_no
  try {
    printSlips.value = await buildSlips([row])
    printDialog.value = true
  } finally { printing.value = '' }
}

async function batchPrint() {
  const rows = printableSelected.value
  if (!rows.length) return ElMessage.warning('请选择已发货订单')
  await ElMessageBox.confirm(`确认打印 ${rows.length} 张发货单？`, '批量打印', { type: 'info' })
  printSlips.value = await buildSlips(rows)
  printDialog.value = true
}

function doPrint() {
  window.print()
}

// ---------- 批量导入 ----------
const IMPORT_HEADER = '商家编码,目的地,渠道编码,商品明细(SKU:数量;分号分隔),收件人编号,手机号,收货地址,城市,邮编'
const importDialog = ref(false)
const importResult = ref(null)

function openImport() {
  if (!canCreate.value) return ElMessage.warning('当前角色无创建订单权限')
  importResult.value = null
  importDialog.value = true
}

function downloadImportTemplate() {
  const sample = '商家编码,目的地,渠道编码,商品明细,收件人编号,手机号,收货地址,城市,邮编\nMCH-0001,GD,GD-EXPR,"SKU001:2;SKU002:1",张三,13800138000,广东省深圳市南山区科技园,深圳,518000'
  const blob = new Blob(['﻿' + IMPORT_HEADER + '\n' + sample], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '订单导入模板.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

async function onImportFile(file) {
  const text = await readFile(file.raw)
  importResult.value = await runImport(text)
}

function readFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = reject
    reader.readAsText(file, 'utf-8')
  })
}

function parseCsvLine(line) {
  // 简易 CSV 解析：支持引号包裹
  const cells = []
  let cur = ''
  let inQuote = false
  for (const ch of line) {
    if (ch === '"') { inQuote = !inQuote; continue }
    if (ch === ',' && !inQuote) { cells.push(cur); cur = ''; continue }
    cur += ch
  }
  cells.push(cur)
  return cells.map((c) => c.trim())
}

async function runImport(text) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean).slice(1)
  if (!lines.length) return { ok: 0, fail: 0, fails: ['文件没有数据行'] }
  const [merchants, channels] = await Promise.all([merchantAll(), tmsChannelAll()])
  // 商品按商家加载：逐行解析时按商家缓存
  const productCache = {}
  let ok = 0
  const fails = []
  for (let idx = 0; idx < lines.length; idx++) {
    const cells = parseCsvLine(lines[idx])
    try {
      const [merchantCode, dest, channelCode, itemsStr, buyerId, buyerPhone, address, city, postal] = cells
      if (!merchantCode || !dest || !channelCode || !itemsStr) throw new Error('缺少必填列：商家编码/目的地/渠道编码/商品明细')
      const merchant = merchants.find((m) => String(m.merchant_code) === merchantCode.trim())
      if (!merchant) throw new Error(`商家编码 ${merchantCode} 不存在`)
      const channel = channels.find((c) => String(c.channel_code) === channelCode.trim())
      if (!channel) throw new Error(`渠道编码 ${channelCode} 不存在`)
      if (!productCache[merchant.id]) {
        try { productCache[merchant.id] = await productByMerchant(merchant.id) || [] } catch { productCache[merchant.id] = [] }
      }
      // 商品明细解析：SKU:数量;...，同一行重复 SKU 自动累加数量
      const itemMap = {}
      for (const s of itemsStr.split(';').map((x) => x.trim()).filter(Boolean)) {
        const [sku, qty] = s.split(':')
        if (!sku) throw new Error(`商品格式错误：${s}`)
        const key = sku.trim()
        itemMap[key] = (itemMap[key] || 0) + Math.max(1, parseInt(qty, 10) || 1)
      }
      const items = Object.keys(itemMap).map((sku) => ({ sku, qty: itemMap[sku] }))
      await tmsOrderCreate({
        merchantId: merchant.id,
        destinationCountry: dest.trim(),
        channelId: channel.id,
        items,
        buyerId: buyerId || '',
        buyerPhone: buyerPhone || '',
        buyerLanguage: 'zh',
        buyerAddress: address || '',
        buyerCity: city || '',
        buyerPostal: postal || ''
      })
      ok++
    } catch (e) {
      fails.push(`第 ${idx + 2} 行：${e?.message || e}`)
    }
  }
  if (ok > 0) {
    ElMessage.success(`批量导入完成：成功 ${ok} 单，已进入「订单审核」待放行`)
    dt.value.reload()
  }
  return { ok, fail: fails.length, fails: fails.slice(0, 20) }
}

// ---------- 详情 ----------
function goDetail(row) { router.push(`/tms/order/${row.order_no}`) }

function safeJson(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}
</script>

<style scoped>
/* 订单总览横幅（参考运单总览） */
.od-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.od-banner::before {
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

/* 发货单打印样式：仅打印发货单区域 */
.print-area {
  display: block;
}
.slip {
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 14px;
  break-inside: avoid;
  page-break-inside: avoid;
  font-size: 13px;
  color: #1f2328;
}
.slip-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-bottom: 2px solid #0891b2;
  padding-bottom: 8px;
  margin-bottom: 10px;
}
.slip-title { font-size: 17px; font-weight: 700; color: #0891b2; }
.slip-no { color: #57606a; }
.slip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 5px;
}
.slip-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}
.slip-table th, .slip-table td {
  border: 1px solid #d0d7de;
  padding: 5px 8px;
  text-align: left;
}
.slip-table th { background: #f6f8fa; }
.slip-foot {
  margin-top: 12px;
  color: #9ca3af;
  font-size: 12px;
}
.zero { color: #c0c4cc; }
/* 模块顶部：筛选与操作拉开间距、不挤 */
:deep(.query-table .qt-head) { margin-bottom: 14px; }
:deep(.query-table .qt-extra) { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
:deep(.query-table .qt-query) { gap: 14px; row-gap: 12px; }
/* 批量操作栏：贴合表格底部 */
.batch-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  padding: 12px 16px;
  background: #f7fbfc;
  border: 1px solid #e3f0f3;
  border-radius: 10px;
}
.batch-bar .bb-tip { font-size: 13px; color: #0e7490; font-weight: 500; }
/* 筛选区：柔和品牌底容器，条件带标签、动作靠右 */
:deep(.query-table .qt-query .filter-bar) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  width: 100%;
  padding: 14px 16px;
  background: #f7fbfc;
  border: 1px solid #e3f0f3;
  border-radius: 10px;
}
:deep(.query-table .qt-query .f-item) { display: flex; align-items: center; gap: 6px; }
:deep(.query-table .qt-query .f-label) { font-size: 13px; color: #0e7490; flex: none; font-weight: 500; }
:deep(.query-table .qt-query .f-actions) { margin-left: auto; display: flex; align-items: center; gap: 10px; }
:deep(.query-table .qt-query .filter-bar .el-select),
:deep(.query-table .qt-query .filter-bar .el-input),
:deep(.query-table .qt-query .filter-bar .el-date-editor) { --el-color-primary: #0891b2; }
:deep(.query-table .qt-query .filter-bar .el-input__wrapper),
:deep(.query-table .qt-query .filter-bar .el-select__wrapper) { background: #fff; }
/* 商品列：图片 + 商品名（超框省略号） */
.cell-prod { display: flex; align-items: center; gap: 8px; min-width: 0; }
.prod-img { width: 40px; height: 40px; border-radius: 6px; flex: none; display: inline-flex; align-items: center; justify-content: center; }
.prod-img-empty { background: #f5f6f7; color: #c0c4cc; }
.prod-name {
  font-size: 13px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}
.prod-name i { font-style: normal; color: #86909c; margin-left: 2px; }
/* 运费（收款）金额突出 */
.cell-freight { color: #0891b2; font-weight: 700; font-size: 14px; }
/* 行距更透气、文字不顶框 */
:deep(.el-table .el-table__cell) { padding: 10px 8px; }
:deep(.el-table th.el-table__cell) { padding: 10px 8px; }

@media print {
  body * { visibility: hidden !important; }
  .print-area, .print-area * { visibility: visible !important; }
  .print-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
  .slip { border-color: #333; break-after: page; }
}
</style>
