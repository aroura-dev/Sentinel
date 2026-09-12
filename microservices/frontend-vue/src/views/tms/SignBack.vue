<template>
  <div>
    <!-- 签收说明 -->
    <div class="sign-tip">
      <el-icon :size="15"><Check /></el-icon>
      <span>货物送到收件人手上，确认「签收」后即标记为已妥投，运费随之完成结算；这里能看到每一票的签收情况。</span>
    </div>

    <el-tabs v-model="tab" @tab-change="reload">
      <el-tab-pane name="PENDING">
        <template #label><span>待签收<b v-if="pending.length" class="count">{{ pending.length }}</b></span></template>
      </el-tab-pane>
      <el-tab-pane name="DELIVERED">
        <template #label><span>已签收<b v-if="delivered.length" class="count">{{ delivered.length }}</b></span></template>
      </el-tab-pane>
    </el-tabs>

    <el-empty v-if="!pagedRows.length && !loading" :description="tab === 'PENDING' ? '暂时没有在途待签收的运单' : '还没有已签收的运单'" />
    <el-table v-else v-loading="loading" :data="pagedRows" border stripe size="small" class="sign-table">
      <el-table-column label="运单号" min-width="150" align="left">
        <template #default="{ row }">
          <el-link v-if="row.waybill_no" type="primary" :underline="false" @click="goWaybill(row.waybill_no)">{{ row.waybill_no }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="商家" min-width="90" align="left">
        <template #default="{ row }">{{ row.merchant_name || '-' }}</template>
      </el-table-column>
      <el-table-column label="发往" min-width="76" align="left">
        <template #default="{ row }">{{ countryLabel(row.destination_country) }}</template>
      </el-table-column>
      <el-table-column label="商品" min-width="150" align="left">
        <template #default="{ row }"><span class="items-cell">{{ itemSummary(row) }}</span></template>
      </el-table-column>
      <el-table-column label="当前进度" min-width="96" align="left">
        <template #default="{ row }"><StatusTag :status="row.current_node" /></template>
      </el-table-column>
      <el-table-column label="预计送达" min-width="140" align="left">
        <template #default="{ row }">{{ fmt(row.promise_eta) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="180" align="center">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="goOrder(row)">查看订单</el-button>
          <el-button v-if="tab === 'PENDING'" size="small" type="success" :loading="acting === row.order_no" @click="confirmSign(row)">确认签收</el-button>
          <el-button v-else-if="tab === 'DELIVERED'" size="small" link type="warning" @click="goAfterSale(row)">发起售后</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="rows.length > pageSize" class="page-bar" background layout="total, prev, pager, next"
      :total="rows.length" :page-size="pageSize" v-model:current-page="page" />
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { tmsOrderList, tmsAdvance, productList } from '../../api'
import { fmtDateTime } from '../../utils/format'
import { countryLabel } from '../../utils/country'

const router = useRouter()
const tab = ref('PENDING')
const all = ref([])
const loading = ref(false)
const acting = ref('')
const productMap = ref({})
const page = ref(1)
const pageSize = 10

const PENDING_NODES = ['WAREHOUSE_OUT', 'DOMESTIC_PICKED', 'IN_TRANSIT', 'IMPORT_CUSTOMS', 'LAST_MILE']

const pending = computed(() => all.value.filter((o) => PENDING_NODES.includes(o.current_node) && o.waybill_no))
const delivered = computed(() => all.value.filter((o) => o.current_node === 'DELIVERED'))
const rows = computed(() => (tab.value === 'PENDING' ? pending.value : delivered.value))
const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return rows.value.slice(start, start + pageSize)
})

watch(tab, () => { page.value = 1 })

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('货物送达收件人后的签收确认与妥投回单，签收后可查看运费结算')
  const prod = await productList({ page: 1, perPage: 500 }).catch(() => null)
  productMap.value = {}
  ;(prod?.rows || []).forEach((p) => { productMap.value[p.id] = p.name })
  await load()
})

async function load() {
  loading.value = true
  try {
    const r = await tmsOrderList({ page: 1, perPage: 1000 })
    all.value = r.rows || []
  } finally { loading.value = false }
}
function reload() { load() }

async function confirmSign(row) {
  await ElMessageBox.confirm(
    `确认订单 ${row.order_no} 已妥投签收？确认后将推进至「已签收」，运费进入结算。`,
    '确认签收', { type: 'warning' }
  )
  acting.value = row.order_no
  try {
    let guard = 0
    let node = row.current_node
    while (node !== 'DELIVERED' && guard < 8) {
      const res = await tmsAdvance(row.order_no)
      node = res?.node || ''
      guard++
    }
    ElMessage.success('已确认签收，进入已签收列表')
    await load()
  } finally { acting.value = '' }
}

function itemSummary(o) {
  const arr = safeParse(o.items_json)
  if (!arr.length) return '-'
  return arr.map((it) => `${productMap.value[it.product_id] || it.sku}×${it.qty}`).join('、')
}
function safeParse(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}
function fmt(v) { return fmtDateTime(v) }
function goWaybill(no) { router.push(`/tms/waybill/${no}`) }
function goOrder(row) { router.push(`/tms/order/${row.order_no}`) }
// 已签收订单如收货方发起退换/投诉：直接进售后退货登记（带单自动弹出）
function goAfterSale(row) { router.push({ path: '/tms/after-sale', query: { orderNo: row.order_no } }) }
</script>

<style scoped>
.sign-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 12px 16px;
  background: linear-gradient(90deg, #eef8fa, #f8fcfc);
  border: 1px solid #d9f0f4;
  border-radius: 8px;
  color: #0e7490;
  font-size: 13px;
}
.sign-tip .el-icon { color: #0891b2; }
.count {
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #0891b2;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}
.items-cell { color: #57606a; }
.page-bar { margin-top: 14px; justify-content: flex-end; }
</style>
