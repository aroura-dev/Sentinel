<template>
  <div>
    <!-- 合并通知横幅 -->
    <div v-if="groups.length" class="merge-banner">
      <div class="mb-top">
        <el-icon :size="16"><Connection /></el-icon>
        <span>可以拼成一车</span>
      </div>
      <div class="mb-mid">
        <span class="mb-num"><b>{{ totalMergeable }}</b> 单 · <b>{{ groups.length }}</b> 组</span>
        <span class="mb-freight">运费合计 <b>¥{{ mergeableFreight.toFixed(2) }}</b></span>
      </div>
      <div class="mb-tip">勾选后点「合并成一张运单」就能一起发出</div>
    </div>

    <!-- 空态 -->
    <el-empty v-if="!loading && !groups.length" description="暂时没有可合并的订单。同商家、同目的地、已审核的订单凑齐 2 单就能拼成一车、统一发货。" />

    <!-- 可合并组合卡片 -->
    <div v-for="g in groups" :key="g.key" class="group-card">
      <div class="group-head">
        <div class="gh-left">
          <b class="gh-merchant">{{ g.merchant_name }}</b>
          <el-tag size="small" effect="plain" class="gh-tag">{{ countryLabel(g.destination) }}</el-tag>
        </div>
        <div class="gh-right">
          <span class="gh-count">可合并 <b>{{ g.mergeable.length }}</b> 单</span>
        </div>
      </div>

      <el-table :data="g.mergeable" border size="small" @selection-change="(sel) => onGroupSelect(g.key, sel)"
        :row-class-name="({ row }) => (isRowSelected(g.key, row.order_no) ? 'row-selected' : '')"
        empty-text="该组合暂无满足条件的订单">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="order_no" label="订单号" min-width="200" align="left" />
        <el-table-column label="商品" min-width="170" align="left">
          <template #default="{ row }"><span class="items-cell">{{ itemSummary(row) }}</span></template>
        </el-table-column>
        <el-table-column label="运费" width="95" align="left">
          <template #default="{ row }"><span class="money">¥{{ Number(row.freight_cost || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150" align="left">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
      </el-table>

      <div class="group-sum">
        <span>商品合计 <b>{{ g.itemTotal }}</b> 件</span>
        <span class="sep">｜</span>
        <span>运费合计 <b class="sum-freight">¥{{ g.freightTotal.toFixed(2) }}</b></span>
      </div>

      <div class="group-foot">
        <div class="gf-left">
          <span v-if="selectedCount(g) >= 2" class="sel-sum">
            已选 <b>{{ selectedCount(g) }}</b> 单 · 运费 <b>¥{{ selectedFreight(g) }}</b>
          </span>
          <span v-if="g.blockedCount" class="blocked-note">
            另外 {{ g.blockedCount }} 单还没审核通过或已发货，暂不能一起合并
          </span>
        </div>
        <el-tooltip :disabled="canMerge(g)" :content="mergeHint(g)" placement="top">
          <el-button type="primary" :disabled="!canMerge(g)" @click="mergeGroup(g)" class="merge-btn">
            <el-icon><Connection /></el-icon>合并成一张运单
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 合并预览：先看清楚再合并 -->
    <el-dialog v-model="previewVisible" title="合并预览" width="560px">
      <div v-if="previewData">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="商家">{{ previewData.merchant_name }}</el-descriptions-item>
          <el-descriptions-item label="目的地">{{ countryLabel(previewData.destination) }}</el-descriptions-item>
          <el-descriptions-item label="合并订单">{{ previewData.orderNos.length }} 单</el-descriptions-item>
          <el-descriptions-item label="运费合计"><b style="color:#0891b2">¥{{ previewData.freightTotal.toFixed(2) }}</b></el-descriptions-item>
        </el-descriptions>

        <h4 class="pv-sec">合并后商品合计</h4>
        <el-table :data="previewData.items" border size="small">
          <el-table-column prop="name" label="商品" min-width="140" />
          <el-table-column prop="qty" label="数量" width="100" align="center" />
        </el-table>

        <el-alert type="info" :closable="false" style="margin-top: 12px"
          title="确认后生成一张运单" description="合并后各订单进入运输中状态，运费明细可在「结算」查看。" />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">再想想</el-button>
        <el-button type="primary" :loading="mergingKey === previewGroupKey" @click="confirmMerge"><el-icon><CircleCheck /></el-icon>确认合并</el-button>
      </template>
    </el-dialog>

    <!-- 历史合并记录 -->
    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="hc-head">
          <b>历史合并记录</b>
          <span class="tip">每次合并都会记一笔，方便日后核对</span>
          <el-link type="primary" :underline="false" class="hc-more" @click="goAudit">查看全部 →</el-link>
        </div>
      </template>

      <el-empty v-if="!historyLoading && !historyList.length" description="还没有合并记录，勾选订单试试看。" />
      <div v-else v-loading="historyLoading" class="wb-list">
        <div v-for="r in historyList" :key="r.id" class="wb-card">
          <div class="wb-head">
            <span class="wb-route">{{ r.merchant_name || '商家' }} <i>发往</i> {{ countryLabel(r.destination) }}</span>
            <el-tag size="small" type="success" effect="light">已合并</el-tag>
            <span class="wb-time">{{ fmt(r.created_at) }}</span>
          </div>
          <div class="wb-body">
            <div class="wb-row">
              <span class="wb-k">运单号</span>
              <el-link type="primary" :underline="false" @click="goWaybill(r._waybillNo)">{{ r._waybillNo }}</el-link>
              <span class="wb-k" style="margin-left: 20px">物流渠道</span>
              <span class="wb-v">{{ r.channelName || '-' }}</span>
            </div>
            <div v-if="r.items.length" class="wb-row">
              <span class="wb-k">商品</span>
              <span class="wb-v">{{ r.items.map((it) => `${it.name}×${it.qty}`).join('、') }}</span>
            </div>
            <div class="wb-row">
              <span class="wb-k">运费</span><span class="wb-v money">¥{{ r._freight != null ? r._freight.toFixed(2) : '-' }}</span>
              <span class="wb-op" style="margin-left: 20px">共合并 {{ r._orderNos.length }} 单</span>
            </div>
            <div v-if="r._orderNos.length" class="wb-foot">
              <span class="wb-k">订单</span>
              <div class="order-tags">
                <el-tag v-for="no in r._orderNos" :key="no" size="small" class="order-tag order-tag-link"
                  :title="'查看订单 ' + no" @click="goOrder(no)">{{ no }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { tmsOrderList, tmsWaybillMerge, auditList, waybillDetail, tmsOrderDetail, tmsChannelList, productList } from '../../api'
import { countryLabel } from '../../utils/country'
import { fmtDateTime } from '../../utils/format'

const router = useRouter()
const groups = ref([])
const loading = ref(false)
const historyList = ref([])
const historyLoading = ref(false)
const productMap = ref({})
const mergingKey = ref('')
const selectedMap = reactive({})
const previewVisible = ref(false)
const previewData = ref(null)
const previewGroupKey = ref('')

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('同商家同目的地、已审核且未发货的订单可拼成一张运单，统一发货、集中管理')
  await loadProducts()
  await Promise.all([loadMergeable(), loadHistory()])
})

// 商品「编号 → 名称」映射：把 SKU 翻译成用户看得懂的商品名
async function loadProducts() {
  const r = await productList({ page: 1, perPage: 500 }).catch(() => null)
  const m = {}
  ;(r?.rows || []).forEach((p) => { m[p.id] = p.name })
  productMap.value = m
}

function productName(it) {
  return productMap.value[it.product_id] || it.sku
}

async function loadMergeable() {
  loading.value = true
  try {
    // 拉取全部待发货订单，前端按「商家 + 目的地」分组并区分可合并/受阻
    const r = await tmsOrderList({ node: 'CREATED', page: 1, perPage: 1000 })
    const rows = r.rows || []
    const map = {}
    for (const row of rows) {
      const key = `${row.merchant_id}_${row.destination_country}`
      if (!map[key]) {
        map[key] = {
          key,
          merchant_name: row.merchant_name,
          merchant_id: row.merchant_id,
          destination: row.destination_country,
          mergeable: [],
          blockedCount: 0
        }
      }
      const approved = String(row.review_status) === 'APPROVED'
      const outbound = !!row.waybill_no
      if (approved && !outbound) {
        map[key].mergeable.push(row)
      } else {
        map[key].blockedCount++
      }
    }
    // 只显示有可合并订单的组合；没有满足条件订单的公司直接不展示
    groups.value = Object.values(map)
      .filter((g) => g.mergeable.length > 0)
      .map((g) => ({
        ...g,
        itemTotal: g.mergeable.reduce((s, o) => s + itemCount(o), 0),
        freightTotal: g.mergeable.reduce((s, o) => s + Number(o.freight_cost || 0), 0)
      }))
  } finally { loading.value = false }
}

const totalMergeable = computed(() => groups.value.reduce((s, g) => s + g.mergeable.length, 0))
const mergeableFreight = computed(() => groups.value.reduce((s, g) => s + g.freightTotal, 0))

function itemCount(o) {
  return safeJson(o.items_json).reduce((s, it) => s + Number(it.qty || 0), 0)
}
function itemSummary(o) {
  return safeJson(o.items_json).map((it) => `${productName(it)}×${it.qty}`).join('、') || '-'
}
function selectedFreight(g) {
  const nos = selectedMap[g.key] || []
  return g.mergeable.filter((r) => nos.includes(r.order_no)).reduce((s, o) => s + Number(o.freight_cost || 0), 0).toFixed(2)
}

function onGroupSelect(key, sel) {
  selectedMap[key] = (sel || []).map((r) => r.order_no)
}

function isRowSelected(key, no) {
  return (selectedMap[key] || []).includes(no)
}

function selectedCount(g) {
  return (selectedMap[g.key] || []).length
}

function canMerge(g) {
  return g.mergeable.length >= 2 && selectedCount(g) >= 2
}

function mergeHint(g) {
  if (g.mergeable.length < 2) return '该组合可合并订单不足 2 单，请先创建 / 审核通过更多订单'
  if (selectedCount(g) < 2) return '请至少勾选 2 个订单'
  return ''
}

// 点合并：先展示合并预览，确认后再真正合并
function mergeGroup(g) {
  const orderNos = selectedMap[g.key] || []
  if (orderNos.length < 2) return
  const rows = g.mergeable.filter((r) => orderNos.includes(r.order_no))
  const itemMap = {}
  for (const r of rows) {
    for (const it of safeJson(r.items_json)) {
      const id = it.product_id
      if (!itemMap[id]) itemMap[id] = { name: productName(it), qty: 0 }
      itemMap[id].qty += Number(it.qty || 0)
    }
  }
  previewGroupKey.value = g.key
  previewData.value = {
    merchant_name: g.merchant_name,
    destination: g.destination,
    orderNos,
    freightTotal: rows.reduce((s, o) => s + Number(o.freight_cost || 0), 0),
    items: Object.values(itemMap).map((v) => ({ name: v.name, qty: v.qty }))
  }
  previewVisible.value = true
}

async function confirmMerge() {
  const key = previewGroupKey.value
  const orderNos = selectedMap[key] || []
  mergingKey.value = key
  try {
    const res = await tmsWaybillMerge(orderNos)
    ElMessage.success(`已合并成一张运单，运单号 ${res.waybill.waybill_no}`)
    selectedMap[key] = []
    previewVisible.value = false
    await Promise.all([loadMergeable(), loadHistory()])
    router.push(`/tms/waybill/${res.waybill.waybill_no}`)
  } finally { mergingKey.value = '' }
}

// ---------- 历史合并记录：审计过滤 MERGE 动作，并把 detail 解析成结构化字段，按时间轴倒序展示 ----------
// 渠道「编号 → 名称」映射：把渠道编号翻译成看得懂的渠道名
async function buildLookups() {
  const ch = await tmsChannelList({ page: 1, perPage: 500 }).catch(() => null)
  const channelMap = {}
  ;(ch?.rows || []).forEach((r) => { channelMap[r.id] = r.channel_name })
  return { channelMap }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const [r, { channelMap }] = await Promise.all([
      auditList({ module: 'waybill', action: 'MERGE', page: 1, perPage: 8 }),
      buildLookups()
    ])
    const base = (r.rows || []).map(enrichMerge)
    // 用运单号补齐运单信息，让历史以用户能看懂的运单卡片形式呈现
    const cards = []
    for (const rec of base) {
      let wb = null
      let order = null
      if (rec._waybillNo && rec._waybillNo !== '-') {
        try { wb = await waybillDetail(rec._waybillNo) } catch { wb = null }
        if (wb?.order_no) { try { order = await tmsOrderDetail(wb.order_no) } catch { order = null } }
      }
      cards.push({
        ...rec,
        wb,
        merchant_name: order?.merchant_name || '',
        destination: order?.destination_country || '',
        channelName: wb ? channelMap[wb.channel_id] || '-' : '-',
        items: wb ? safeJson(wb.items_json).map((it) => ({ name: productName(it), qty: it.qty })) : []
      })
    }
    historyList.value = cards
  } finally { historyLoading.value = false }
}

function goAudit() {
  router.push('/audit')
}

function enrichMerge(row) {
  const detail = row.detail || ''
  const wb = (detail.match(/WB[\w-]+/) || [''])[0]
  const nos = (detail.match(/包含订单:\s*([^，。]+)/) || ['', ''])[1].split(',').map((s) => s.trim()).filter(Boolean)
  const fre = (detail.match(/运费合计=([\d.]+)/) || [])[1]
  return {
    ...row,
    _waybillNo: wb || '-',
    _orderNos: nos,
    _freight: fre != null ? Number(fre) : null
  }
}

function goWaybill(no) {
  if (no && no !== '-') router.push(`/tms/waybill/${no}`)
}

function goOrder(no) {
  if (no) router.push(`/tms/order/${no}`)
}

function fmt(v) { return fmtDateTime(v) }
function safeJson(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}
</script>

<style scoped>
.tip { margin-left: 12px; color: #9ca3af; font-weight: 400; font-size: 12px; }
.merge-banner {
  position: relative;
  margin-bottom: 14px;
  padding: 16px 20px 16px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  overflow: hidden;
}
.merge-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.mb-top { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1d2129; }
.mb-top .el-icon { color: #0891b2; }
.mb-mid {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 12px;
}
.mb-num { font-size: 24px; font-weight: 600; line-height: 1.1; color: #1d2129; }
.mb-num b { color: #0891b2; }
.mb-freight { font-size: 16px; color: #57606a; }
.mb-freight b { color: #0891b2; font-weight: 700; }
.mb-tip { margin-top: 10px; font-size: 12px; color: #9ca3af; }
.group-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.gh-left { display: flex; align-items: center; min-width: 0; }
.gh-merchant { font-size: 15px; color: #1d2129; }
.gh-tag { margin-left: 10px; }
.gh-right { display: flex; align-items: center; gap: 18px; flex: none; }
.gh-count { font-size: 13px; color: #57606a; }
.gh-count b { color: #1d2129; }
.items-cell { color: #57606a; }
.money { color: #0891b2; font-weight: 600; }
.group-sum {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 7px 12px;
  background: #f6fafb;
  border: 1px solid #e8f3f5;
  border-radius: 6px;
  font-size: 13px;
  color: #57606a;
}
.group-sum b { color: #1d2129; font-weight: 600; }
.group-sum .sum-freight { color: #0891b2; }
.group-sum .sep { color: #c9d4dc; }
:deep(.el-table .row-selected td.el-table__cell) {
  background: #ecf8fa !important;
}
:deep(.el-table .row-selected:hover > td.el-table__cell) {
  background: #e2f4f7 !important;
}
.group-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.gf-left { display: flex; align-items: center; gap: 14px; min-width: 0; flex-wrap: wrap; }
.sel-sum { font-size: 13px; color: #1d2129; }
.sel-sum b { color: #0891b2; }
.blocked-note { color: #b7791f; font-size: 12px; }
.merge-btn { flex: none; }
.pv-sec { margin: 16px 0 8px; color: #1d2129; }
.history-card { margin-top: 18px; }
.hc-head { display: flex; align-items: center; }
.hc-more { margin-left: auto; }
.order-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.order-tag { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.order-tag-link { cursor: pointer; }
.order-tag-link:hover { border-color: #0891b2; color: #0891b2; }
.wb-list { display: flex; flex-direction: column; gap: 12px; }
.wb-card {
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.wb-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 14px;
  background: #f6fafb;
  border-bottom: 1px solid #e8f3f5;
}
.wb-no { font-size: 14px; color: #1d2129; font-weight: 600; }
.wb-route { font-size: 14px; color: #1d2129; font-weight: 600; }
.wb-route i { font-style: normal; color: #9ca3af; font-weight: 400; margin: 0 6px; }
.wb-time { margin-left: auto; font-size: 12px; color: #9ca3af; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.wb-body { padding: 10px 14px; }
.wb-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  padding: 3px 0;
  font-size: 13px;
}
.wb-k { color: #9ca3af; font-size: 12px; flex: none; }
.wb-v { color: #1d2129; }
.wb-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #eceef1;
}
.wb-op { font-size: 12px; color: #9ca3af; flex: none; }
</style>
