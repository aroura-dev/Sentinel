<template>
  <!-- 运单总览：温馨 + SLA 违约提醒 -->
  <div class="wb-banner">
    <div class="b-head">
      <el-icon :size="15"><Document /></el-icon>
      <span class="b-title">运单总览</span>
      <span class="b-tip">货发出去之后，在这里看每一票的走向和运费入账</span>
    </div>
    <div class="b-stats">
      <span class="b-item">在途 <b>{{ summary.inTransit }}</b> 单</span>
      <span class="b-item">已签收 <b>{{ summary.delivered }}</b> 单</span>
      <span class="b-item">待入账 <b>{{ summary.unbilled }}</b> 单</span>
      <span class="b-item b-risk" :class="{ hot: summary.risk }">临近超时 <b>{{ summary.risk }}</b></span>
      <span class="b-item b-alert" :class="{ hot: summary.breached }">SLA 违约 <b>{{ summary.breached }}</b></span>
    </div>
    <div v-if="summary.breached || summary.risk" class="b-warn">
      有 <b>{{ summary.breached }}</b> 单已超时违约、<b>{{ summary.risk }}</b> 单临近约定送达时效，建议优先跟进配送、及时安抚收件人。
    </div>
    <div v-else class="b-warn ok">目前在途运单都在承诺时效内送达，一切正常，辛苦了。</div>
  </div>

  <DataTable ref="dt" title="运单管理" :columns="columns" :load="waybillList" :query="query">
    <template #query="{ reload }">
      <el-input v-model="query.waybillNo" placeholder="运单号" clearable style="width: 200px" @keyup.enter="reload" />
      <el-input v-model="query.trackingNo" placeholder="跟踪号" clearable style="width: 180px" @keyup.enter="reload" />
      <el-select v-model="query.channelId" placeholder="渠道" filterable clearable style="width: 180px" @change="reload">
        <el-option v-for="c in channels" :key="c.id" :label="c.channel_name" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
    </template>
    <template #cell-merchant_id="{ row }">{{ merchantMap[row.merchant_id] || '-' }}</template>
    <template #cell-items_json="{ row }"><span class="items-cell">{{ itemSummary(row) }}</span></template>
    <template #cell-billable_weight_kg="{ row }">{{ Number(row.billable_weight_kg || 0).toFixed(3) }} kg</template>
    <template #cell-status="{ row }"><StatusTag :status="row.status" /></template>
    <template #cell-freight_cost="{ row }"><span class="money">¥{{ Number(row.freight_cost).toFixed(2) }}</span></template>
    <template #cell-billed="{ row }"><el-tag size="small" :type="Number(row.billed) ? 'success' : 'warning'">{{ Number(row.billed) ? '已入账' : '未入账' }}</el-tag></template>
    <template #actions="{ row }">
      <el-button size="small" link type="primary" @click="goDetail(row)">详情</el-button>
      <el-button v-if="!Number(row.billed)" size="small" type="warning" @click="openBill(row)">手动入账</el-button>
    </template>
  </DataTable>

  <el-dialog v-model="billDialog" title="手动入账（加入草稿账单）" width="440px">
    <el-form label-width="100px">
      <el-form-item label="运单">{{ cur?.waybill_no }}（¥{{ cur ? Number(cur.freight_cost).toFixed(2) : '' }}）</el-form-item>
      <el-form-item label="目标账单" required>
        <el-select v-model="targetBillId" filterable placeholder="选择 DRAFT 账单" style="width: 100%">
          <el-option v-for="b in draftBills" :key="b.id" :label="`${b.bill_no}（¥${Number(b.total_amount).toFixed(2)}）`" :value="b.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="billDialog = false">取消</el-button>
      <el-button type="primary" :loading="billing" @click="doBill">入账</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { waybillList, tmsOrderList, tmsChannelAll, billList, billAddWaybill, merchantList, productList } from '../../api'

const router = useRouter()
const channels = ref([])
const draftBills = ref([])
const merchantMap = ref({})
const productMap = ref({})
const allWb = ref([])
const orderSla = ref({})
const query = reactive({ waybillNo: '', trackingNo: '', channelId: null })

// 运单总览：在途 / 已签收 / 待入账 / SLA 违约（违约状态取自关联订单）
const summary = computed(() => {
  const rows = allWb.value
  return {
    inTransit: rows.filter((w) => w.status === 'ACTIVE').length,
    delivered: rows.filter((w) => w.status === 'DELIVERED').length,
    unbilled: rows.filter((w) => !Number(w.billed)).length,
    breached: rows.filter((w) => orderSla.value[w.order_no] === 'BREACHED').length,
    risk: rows.filter((w) => orderSla.value[w.order_no] === 'RISK').length
  }
})
const billDialog = ref(false)
const billing = ref(false)
const cur = ref(null)
const targetBillId = ref(null)
const columns = [
  { prop: 'waybill_no', label: '运单号', minWidth: 230 },
  { prop: 'merchant_id', label: '商家', minWidth: 110 },
  { prop: 'items_json', label: '商品', minWidth: 180 },
  { prop: 'order_no', label: '订单号', minWidth: 230 },
  { prop: 'tracking_no', label: '跟踪号', minWidth: 180 },
  { prop: 'billable_weight_kg', label: '计费重量', width: 100 },
  { prop: 'freight_cost', label: '运费', width: 110, align: 'right' },
  { prop: 'status', label: '状态', width: 100, type: 'tag' },
  { prop: 'billed', label: '入账', width: 90 },
  { prop: 'promise_eta', label: '预计送达', width: 160, type: 'datetime' }
]

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('运单执行总览：每一票货的走向、运费入账与时效履约一目了然')
  const [ch, mch, prod, wbPage, orderPage] = await Promise.all([
    tmsChannelAll().catch(() => []),
    merchantList({ page: 1, perPage: 500 }).catch(() => null),
    productList({ page: 1, perPage: 500 }).catch(() => null),
    waybillList({ page: 1, perPage: 500 }).catch(() => null),
    tmsOrderList({ page: 1, perPage: 1000 }).catch(() => null)
  ])
  channels.value = ch || []
  merchantMap.value = {}
  ;(mch?.rows || []).forEach((r) => { merchantMap.value[r.id] = r.merchant_name })
  productMap.value = {}
  ;(prod?.rows || []).forEach((r) => { productMap.value[r.id] = r.name })
  allWb.value = wbPage?.rows || []
  orderSla.value = {}
  ;(orderPage?.rows || []).forEach((o) => { if (o.order_no) orderSla.value[o.order_no] = o.sla_status })
})

async function loadDraftBills() {
  draftBills.value = (await billList({ status: 'DRAFT', page: 1, perPage: 50 })).rows || []
}

async function openBill(row) {
  cur.value = row
  targetBillId.value = null
  await loadDraftBills()
  if (!draftBills.value.length) return ElMessage.warning('没有可用的 DRAFT 账单，请先生成账单')
  billDialog.value = true
}

async function doBill() {
  if (!targetBillId.value) return ElMessage.warning('请选择目标账单')
  billing.value = true
  try {
    await billAddWaybill(targetBillId.value, cur.value.waybill_no)
    ElMessage.success('入账成功')
    billDialog.value = false
    dt.value.reload()
  } finally { billing.value = false }
}

function goDetail(row) { router.push(`/tms/waybill/${row.waybill_no}`) }

function safeParse(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}
function itemSummary(row) {
  const arr = safeParse(row.items_json)
  if (!arr.length) return '-'
  return arr.map((it) => `${productMap.value[it.product_id] || it.sku}×${it.qty}`).join('、')
}
</script>

<style scoped>
.items-cell { color: #57606a; }
.money { color: #0891b2; font-weight: 700; }
/* 运单总览横幅 */
.wb-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.wb-banner::before {
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
.b-item.b-risk b { color: #f59e0b; }
.b-item.b-alert b { color: #ef4444; }
.b-item.b-risk:not(.hot) b,
.b-item.b-alert:not(.hot) b { color: #9ca3af; font-size: 18px; }
.b-warn { margin-top: 12px; font-size: 13px; color: #b7791f; background: #fef6ec; border: 1px solid #f5e1c3; border-radius: 6px; padding: 8px 12px; }
.b-warn b { color: #d97706; }
.b-warn.ok { color: #0e7490; background: #eef8fa; border-color: #d9f0f4; }
</style>
