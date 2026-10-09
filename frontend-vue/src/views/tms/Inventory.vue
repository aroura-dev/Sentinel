<template>
  <div>
    <div class="inv-overview">
      <div class="ov-item"><span class="ov-k">库存记录（条）</span><b class="ov-v">{{ overview.totalSku }}</b></div>
      <div class="ov-item"><span class="ov-k">可发合计（件）</span><b class="ov-v">{{ overview.available }}</b></div>
      <div class="ov-item"><span class="ov-k">已占用（件）</span><b class="ov-v">{{ overview.reserved }}</b></div>
      <div class="ov-item">
        <span class="ov-k">需关注</span>
        <b class="ov-v">{{ overview.attention }}</b>
        <span class="ov-sub">低库存 {{ overview.low }} · 断货 {{ overview.out }}</span>
      </div>
    </div>
    <div class="inv-note"><b>说明：</b>在库可用指当前可发货数量，已占用指已下单待发货的占用；所有出入库均由订单 / 运单等业务单据驱动并留痕，可供追溯。低库存按可用数量 ≤ 10 件提示，出库超可用数量将被拦截。</div>

    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <b>库存台账</b>
          <el-button type="primary" size="small" @click="openAdjust()">出入库登记</el-button>
        </div>
      </template>

      <el-tabs v-model="tab">
        <el-tab-pane label="商品库存" name="stock">
          <div class="toolbar">
            <el-input v-model="stockQuery.keyword" placeholder="商品名称 / 商家" clearable style="width: 220px" @keyup.enter="loadStock" />
            <el-select v-model="stockQuery.merchantId" placeholder="全部商家" clearable style="width: 150px" @change="loadStock">
              <el-option v-for="m in merchants" :key="m.id" :label="m.merchant_name" :value="m.id" />
            </el-select>
            <el-select v-model="stockQuery.warehouseId" placeholder="全部仓库" clearable style="width: 140px" @change="loadStock">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
            </el-select>
            <el-select v-model="stockQuery.status" placeholder="库存状态" clearable style="width: 130px" @change="loadStock">
              <el-option label="需关注" value="attention" />
              <el-option label="低库存" value="low" />
              <el-option label="断货" value="out" />
            </el-select>
            <el-button type="primary" @click="loadStock">查询</el-button>
            <el-button @click="resetStockQuery">重置</el-button>
          </div>
          <el-table :data="stocks" v-loading="stockLoading" border stripe size="small" :empty-text="emptyStockText" @row-click="onStockRowClick">
            <el-table-column label="商品" min-width="190">
              <template #default="{ row }">{{ row.product_name || skuName(row.sku) }}</template>
            </el-table-column>
            <el-table-column label="商家" min-width="100">
              <template #default="{ row }">{{ merchantName(row.merchant_id) }}</template>
            </el-table-column>
            <el-table-column label="仓库" min-width="110">
              <template #default="{ row }">{{ warehouseName(row.warehouse_id) }}</template>
            </el-table-column>
            <el-table-column label="在库可用" width="110" align="center">
              <template #default="{ row }"><b>{{ row.on_hand }}</b> 件</template>
            </el-table-column>
            <el-table-column label="已占用" width="100" align="center">
              <template #default="{ row }">{{ row.reserved }} 件</template>
            </el-table-column>
            <el-table-column label="库存状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="stockStatus(row).type">{{ stockStatus(row).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后更新" width="170" align="center">
              <template #default="{ row }">{{ fmt(row.updated_at) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-link type="success" :underline="false" style="margin-right: 6px" @click.stop="openAdjust('IN', row.sku, row.warehouse_id)">入库</el-link>
                <el-link type="warning" :underline="false" @click.stop="openAdjust('OUT', row.sku, row.warehouse_id)">出库</el-link>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next" :total="stockTotal"
            :page-size="stockQuery.perPage" :current-page="stockQuery.page" @current-change="(p) => { stockQuery.page = p; loadStock() }" />
        </el-tab-pane>

        <el-tab-pane label="出入库流水" name="flow">
          <div class="toolbar">
            <el-input v-model="flowQuery.keyword" placeholder="商品名称 / 业务单号" clearable style="width: 220px" @keyup.enter="loadFlow" />
            <el-select v-model="flowQuery.bizType" placeholder="全部类型" clearable style="width: 140px" @change="loadFlow">
              <el-option label="入库" value="IN" />
              <el-option label="出库" value="OUT" />
              <el-option label="退货退回" value="REFUND" />
              <el-option label="盘点调整" value="ADJUST" />
            </el-select>
            <el-select v-model="flowQuery.warehouseId" placeholder="全部仓库" clearable style="width: 140px" @change="loadFlow">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
            </el-select>
            <el-button type="primary" @click="loadFlow">查询</el-button>
            <el-button @click="resetFlowQuery">重置</el-button>
          </div>
          <el-table :data="flows" v-loading="flowLoading" border stripe size="small" :empty-text="emptyFlowText">
            <el-table-column label="时间" width="170" align="center">
              <template #default="{ row }">{{ fmt(row.created_at) }}</template>
            </el-table-column>
            <el-table-column prop="biz_type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="flowTagType(row.biz_type)">
                  {{ typeLabel(row.biz_type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="90" align="center">
              <template #default="{ row }">
                <span :class="qtyCls(row.biz_type)">{{ qtySign(row.biz_type) + row.qty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="仓库" min-width="110">
              <template #default="{ row }">{{ warehouseName(row.warehouse_id) }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="170">
              <template #default="{ row }">{{ skuName(row.sku) }}</template>
            </el-table-column>
            <el-table-column prop="biz_no" label="业务单据号" min-width="160" />
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next" :total="flowTotal"
            :page-size="flowQuery.perPage" :current-page="flowQuery.page" @current-change="(p) => { flowQuery.page = p; loadFlow() }" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 库存详情抽屉 -->
    <el-drawer v-model="stockDetailOpen" :title="stockDetail ? (stockDetail.product_name || skuName(stockDetail.sku)) + ' · 库存详情' : '库存详情'" size="540px">
      <template v-if="stockDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="编码">{{ stockDetail.sku }}</el-descriptions-item>
          <el-descriptions-item label="库存状态"><el-tag size="small" :type="stockStatus(stockDetail).type">{{ stockStatus(stockDetail).label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="商品">{{ stockDetail.product_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ merchantName(stockDetail.merchant_id) }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ warehouseName(stockDetail.warehouse_id) }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ fmt(stockDetail.updated_at) }}</el-descriptions-item>
          <el-descriptions-item label="在库可用"><b>{{ stockDetail.on_hand }}</b> 件</el-descriptions-item>
          <el-descriptions-item label="已占用">{{ stockDetail.reserved }} 件</el-descriptions-item>
        </el-descriptions>
        <div class="dr-section">最近出入库记录</div>
        <el-table :data="stockFlows" v-loading="flowLoading2" border size="small" :empty-text="'暂无该商品的出入库记录'">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ fmt(row.created_at) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ typeLabel(row.biz_type) }}</template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column label="仓库" min-width="110">
            <template #default="{ row }">{{ warehouseName(row.warehouse_id) }}</template>
          </el-table-column>
          <el-table-column prop="biz_no" label="业务单据号" min-width="130" />
        </el-table>
        <div style="margin-top: 16px; display: flex; gap: 10px">
          <el-button type="success" size="small" @click="openAdjust('IN', stockDetail.sku, stockDetail.warehouse_id); stockDetailOpen = false">登记入库</el-button>
          <el-button type="warning" size="small" @click="openAdjust('OUT', stockDetail.sku, stockDetail.warehouse_id); stockDetailOpen = false">登记出库</el-button>
        </div>
      </template>
      <div v-else class="dr-loading">加载中…</div>
    </el-drawer>

    <!-- 出入库登记 -->
    <el-dialog v-model="adjDialog" title="出入库登记" width="460px">
      <el-form :model="adj" label-width="90px">
        <el-form-item label="商品" required>
          <el-select v-model="adj.sku" filterable placeholder="搜索并选择商品" style="width: 100%">
            <el-option v-for="pp in products" :key="pp.sku" :label="pp.name" :value="pp.sku" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" required>
          <el-select v-model="adj.warehouseId" placeholder="选择仓库（必选）" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="adj.bizType" style="width: 100%">
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
            <el-option label="退货退回" value="REFUND" />
            <el-option label="盘点调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required><el-input-number v-model="adj.qty" :min="1" :max="99999" /></el-form-item>
        <el-form-item label="业务单据号"><el-input v-model="adj.bizNo" placeholder="关联订单/运单号（可空）" /></el-form-item>
        <div v-if="adj.bizType === 'OUT' && adjAvail != null" class="avail-tip">
          该仓库当前可用库存：{{ adjAvail }} 件，本次登记后剩余 {{ Math.max(adjAvail - adj.qty, 0) }} 件。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="adjDialog = false">取消</el-button>
        <el-button type="primary" :loading="adjLoading" @click="doAdjust">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fmtDateTime } from '../../utils/format'
import { inventoryList, inventoryFlow, inventoryAdjust, productList, merchantAll, warehouseAll } from '../../api'

const setDesc = inject('setPageDesc')
const tab = ref('stock')

const merchants = ref([])
const warehouses = ref([])
const products = ref([])
const skuName = (sku) => { const p = products.value.find((x) => String(x.sku) === String(sku)); return p ? p.name : (sku || '-') }
const merchantName = (id) => { const m = merchants.value.find((x) => String(x.id) === String(id)); return m ? (m.merchant_name || '-') : '-' }
const warehouseName = (id) => { const w = warehouses.value.find((x) => String(x.id) === String(id)); return w ? w.warehouse_name : '-' }

// 全量快照 + 客户端筛选（当前库存量级较小，便于支持 商家/仓库/状态 组合筛选）
const stockAll = ref([])
const flowAll = ref([])
const stocks = ref([])
const flows = ref([])
const stockTotal = ref(0)
const flowTotal = ref(0)
const stockLoading = ref(false)
const flowLoading = ref(false)

const stockQuery = reactive({ keyword: '', merchantId: null, warehouseId: null, status: '', page: 1, perPage: 10 })
const flowQuery = reactive({ keyword: '', bizType: '', warehouseId: null, page: 1, perPage: 10 })

const overview = ref({ totalSku: 0, available: 0, reserved: 0, low: 0, out: 0, attention: 0 })
const stockDetail = ref(null)
const stockDetailOpen = ref(false)
const stockFlows = ref([])
const flowLoading2 = ref(false)

const emptyStockText = '当前没有符合条件的库存记录，可先行登记出入库后查看'
const emptyFlowText = '暂无出入库记录 — 登记出入库后会自动生成'
const fmt = fmtDateTime
const typeLabel = (v) => ({ IN: '入库', OUT: '出库', REFUND: '退货退回', ADJUST: '盘点调整', STOCKTAKE_IN: '盘盈入库', STOCKTAKE_OUT: '盘亏出库' }[v] || v || '-')
const flowTagType = (v) => (['OUT', 'STOCKTAKE_OUT'].includes(v) ? 'danger' : ['IN', 'REFUND', 'STOCKTAKE_IN'].includes(v) ? 'success' : 'info')
const qtySign = (v) => (['OUT', 'STOCKTAKE_OUT'].includes(v) ? '-' : ['IN', 'REFUND', 'STOCKTAKE_IN'].includes(v) ? '+' : '')
const qtyCls = (v) => (['OUT', 'STOCKTAKE_OUT'].includes(v) ? 'qty-out' : ['IN', 'REFUND', 'STOCKTAKE_IN'].includes(v) ? 'qty-in' : '')

const stockStatus = (r) => {
  const n = Number(r.on_hand || 0)
  if (n <= 0) return { label: '断货', type: 'danger' }
  if (n <= 10) return { label: '偏低', type: 'warning' }
  return { label: '充足', type: 'success' }
}

function inStock(row) {
  const kw = String(stockQuery.keyword || '').trim().toLowerCase()
  if (kw) {
    const text = [row.sku, row.product_name, merchantName(row.merchant_id), warehouseName(row.warehouse_id)]
      .map((v) => String(v || '').toLowerCase()).join(' ')
    if (!text.includes(kw)) return false
  }
  if (stockQuery.merchantId && String(row.merchant_id) !== String(stockQuery.merchantId)) return false
  if (stockQuery.warehouseId && String(row.warehouse_id) !== String(stockQuery.warehouseId)) return false
  const st = stockStatus(row).label
  if (stockQuery.status === 'attention' && st === '充足') return false
  if (stockQuery.status === 'low' && st !== '偏低') return false
  if (stockQuery.status === 'out' && st !== '断货') return false
  return true
}

function inFlow(row) {
  const kw = String(flowQuery.keyword || '').trim().toLowerCase()
  if (kw) {
    const text = [row.sku, row.biz_no, warehouseName(row.warehouse_id)].map((v) => String(v || '').toLowerCase()).join(' ')
    if (!text.includes(kw)) return false
  }
  if (flowQuery.bizType && row.biz_type !== flowQuery.bizType) return false
  if (flowQuery.warehouseId && String(row.warehouse_id) !== String(flowQuery.warehouseId)) return false
  return true
}

async function refreshBase() {
  try {
    const [s, f] = await Promise.all([
      inventoryList({ page: 1, perPage: 5000 }),
      inventoryFlow({ page: 1, perPage: 5000 })
    ])
    stockAll.value = s.rows || []
    flowAll.value = f.rows || []
  } catch (e) {
    stockAll.value = []
    flowAll.value = []
  }
  computeOverview()
  applyStock()
  applyFlow()
}

function computeOverview() {
  const rows = stockAll.value
  const low = rows.filter((r) => Number(r.on_hand) > 0 && Number(r.on_hand) <= 10).length
  const out = rows.filter((r) => Number(r.on_hand) <= 0).length
  overview.value = {
    totalSku: rows.length,
    available: rows.reduce((s, r) => s + Number(r.on_hand || 0), 0),
    reserved: rows.reduce((s, r) => s + Number(r.reserved || 0), 0),
    low,
    out,
    attention: low + out
  }
}

function applyStock() {
  const filtered = stockAll.value.filter(inStock)
  stockTotal.value = filtered.length
  const start = (stockQuery.page - 1) * stockQuery.perPage
  stocks.value = filtered.slice(start, start + stockQuery.perPage)
}

function applyFlow() {
  const filtered = flowAll.value.filter(inFlow)
  flowTotal.value = filtered.length
  const start = (flowQuery.page - 1) * flowQuery.perPage
  flows.value = filtered.slice(start, start + flowQuery.perPage)
}

async function loadStock() {
  stockLoading.value = true
  try { applyStock() } finally { stockLoading.value = false }
}

async function loadFlow() {
  flowLoading.value = true
  try { applyFlow() } finally { flowLoading.value = false }
}

function resetStockQuery() {
  Object.assign(stockQuery, { keyword: '', merchantId: null, warehouseId: null, status: '', page: 1 })
  loadStock()
}

function resetFlowQuery() {
  Object.assign(flowQuery, { keyword: '', bizType: '', warehouseId: null, page: 1 })
  loadFlow()
}

// 出入库登记
const adjDialog = ref(false)
const adjLoading = ref(false)
const adj = reactive({ sku: '', warehouseId: null, bizType: 'OUT', qty: 1, bizNo: '' })
const adjAvail = ref(null)

function openAdjust(type, sku, warehouseId) {
  Object.assign(adj, { sku: sku || '', warehouseId: warehouseId || null, bizType: type || 'OUT', qty: 1, bizNo: '' })
  refreshAdjAvail()
  adjDialog.value = true
}

function refreshAdjAvail() {
  adjAvail.value = null
  if (!adj.sku || !adj.warehouseId) return
  const row = stockAll.value.find((r) => String(r.sku) === String(adj.sku) && String(r.warehouse_id) === String(adj.warehouseId))
  adjAvail.value = row == null ? null : Number(row.on_hand || 0)
}

async function doAdjust() {
  if (!adj.sku) return ElMessage.warning('请选择商品')
  if (!adj.warehouseId) return ElMessage.warning('请选择仓库')
  if (adj.bizType === 'OUT') {
    refreshAdjAvail()
    if (adjAvail.value != null && adj.qty > adjAvail.value) {
      return ElMessage.warning('该仓库可用库存为 ' + adjAvail.value + ' 件，本次出库 ' + adj.qty + ' 件将导致负库存，请调整数量')
    }
  }
  adjLoading.value = true
  try {
    await inventoryAdjust({
      sku: adj.sku,
      bizType: adj.bizType,
      qty: adj.qty,
      warehouseId: adj.warehouseId,
      bizNo: adj.bizNo || null
    })
    ElMessage.success('已登记：' + skuName(adj.sku) + ' ' + typeLabel(adj.bizType) + ' ' + adj.qty + ' 件，库存已更新')
    adjDialog.value = false
    await refreshBase()
  } finally { adjLoading.value = false }
}

// 详情抽屉
function onStockRowClick(row, column, event) {
  if (event && event.target && event.target.closest && event.target.closest('a, button')) return
  openStockDetail(row)
}

async function openStockDetail(row) {
  stockDetail.value = row
  stockDetailOpen.value = true
  stockFlows.value = []
  flowLoading2.value = true
  try {
    const data = await inventoryFlow({ sku: row.sku, page: 1, perPage: 5 })
    stockFlows.value = data.rows || []
  } catch (e) { stockFlows.value = [] } finally { flowLoading2.value = false }
}

onMounted(async () => {
  setDesc('业务单据驱动库存台账：商品库存与出入库流水全程留痕')
  const [ms, ws, ps] = await Promise.all([merchantAll(), warehouseAll(), productList({ page: 1, perPage: 5000 })])
  merchants.value = ms || []
  warehouses.value = ws || []
  products.value = (ps && ps.rows) || []
  await refreshBase()
})
</script>

<style scoped>
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.inv-overview { display: flex; flex-wrap: wrap; gap: 10px 34px; margin-bottom: 14px; padding: 12px 18px; background: #fff; border: 1px solid #e5e6eb; border-radius: 10px; }
.ov-item { display: flex; align-items: baseline; gap: 6px; }
.ov-k { font-size: 13px; color: #57606a; }
.ov-v { font-size: 20px; color: #1d2129; }
.ov-sub { font-size: 12px; color: #d97706; }
.inv-note { margin-bottom: 16px; padding: 10px 14px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.7; }
.inv-note b { color: #155e75; }
.dr-section { font-weight: 600; margin: 18px 0 10px; color: #1d2129; }
.dr-loading { color: #9ca3af; text-align: center; padding: 40px 0; }
.avail-tip { font-size: 12px; color: #b45309; line-height: 1.6; padding: 4px 0; }
.pager { margin-top: 14px; justify-content: flex-end; }
.qty-in { color: #10b981; font-weight: 600; }
.qty-out { color: #f56c6c; font-weight: 600; }
:deep(.el-table__row) { cursor: pointer; }
</style>