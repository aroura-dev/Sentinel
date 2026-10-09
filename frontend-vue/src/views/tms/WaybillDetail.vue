<template>
  <div v-if="wb">
    <el-card shadow="never" class="mb">
      <template #header><b>运单详情：{{ wb.waybill_no }}</b><span class="tip">跟踪号 {{ wb.tracking_no || '-' }}</span></template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="订单号">
          <router-link :to="'/tms/order/' + wb.order_no" style="color: #0891b2; text-decoration: none">{{ wb.order_no }} →</router-link>
        </el-descriptions-item>
        <el-descriptions-item label="商家">{{ order?.merchant_name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目的地">{{ countryLabel(order?.destination_country) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物流渠道">{{ channelName }}</el-descriptions-item>
        <el-descriptions-item label="承运商">{{ carrierName }}</el-descriptions-item>
        <el-descriptions-item label="实重">{{ Number(wb.weight_kg || 0).toFixed(3) }} kg</el-descriptions-item>
        <el-descriptions-item label="计费重">{{ Number(wb.billable_weight_kg || 0).toFixed(3) }} kg</el-descriptions-item>
        <el-descriptions-item label="货值">¥{{ Number(wb.declared_value || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="运费"><b style="color:#0891b2">¥{{ Number(wb.freight_cost || 0).toFixed(2) }}</b></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="wb.status" /></el-descriptions-item>
        <el-descriptions-item label="入账"><el-tag size="small" :type="Number(wb.billed) ? 'success' : 'info'">{{ Number(wb.billed) ? '已入账' : '未入账' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="预计送达">{{ fmt(wb.promise_eta) }}</el-descriptions-item>
        <el-descriptions-item label="实际妥投">{{ fmt(wb.actual_delivered_at) }}</el-descriptions-item>
      </el-descriptions>
      <div class="eta-bar">
        <el-button type="primary" plain :loading="etaLoading" @click="predictEta"><el-icon><TrendCharts /></el-icon>预计送达预测</el-button>
        <el-alert v-if="etaResult" type="success" :closable="false" style="margin-top: 10px">
          <template #title>
            预计剩余 <b>{{ etaResult.remainingDays ?? '-' }}</b> 天（承诺 {{ etaResult.transitDaysMax ?? '-' }} 天）
          </template>
          {{ etaResult.reason || etaResult.note || '' }}
        </el-alert>
      </div>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header><b>商品明细</b><span class="tip">整单出库为订单全量；分批出库为本次出库子集；合并运单为各订单合并</span></template>
      <el-table v-if="items.length" :data="items" border size="small">
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">{{ row.product_name }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="90" align="center" />
        <el-table-column label="单件重量" width="120" align="center">
          <template #default="{ row }">{{ row.unit_weight_kg ?? '-' }} kg</template>
        </el-table-column>
        <el-table-column label="单件货值" width="120" align="center">
          <template #default="{ row }">¥{{ Number(row.unit_declared_value ?? 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <span v-else style="color:#9ca3af">无商品明细</span>
    </el-card>

    <el-card shadow="never">
      <template #header><b>物流轨迹</b></template>
      <NodeTimeline :tracks="tracks" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import NodeTimeline from '../../components/NodeTimeline.vue'
import { waybillDetail, waybillTracks, etaPredict, tmsOrderDetail, tmsChannelList, carrierList, productList } from '../../api'
import { fmtDateTime } from '../../utils/format'
import { countryLabel } from '../../utils/country'

const route = useRoute()
const wb = ref(null)
const tracks = ref([])
const etaResult = ref(null)
const etaLoading = ref(false)
const order = ref(null)
const channelMap = ref({})
const carrierMap = ref({})
const productMap = ref({})

const items = computed(() => {
  const arr = safeParse(wb.value?.items_json)
  return arr.map((it) => ({ ...it, product_name: productMap.value[it.product_id] || it.sku }))
})

const channelName = computed(() => (wb.value ? channelMap.value[wb.value.channel_id] || '-' : '-'))
const carrierName = computed(() => (wb.value ? carrierMap.value[wb.value.carrier_code] || wb.value.carrier_code || '-' : '-'))

onMounted(async () => {
  wb.value = await waybillDetail(route.params.waybillNo)
  tracks.value = (await waybillTracks(route.params.waybillNo)) || []
  // 并行补齐：源订单（商家 / 目的地）+ 渠道 / 承运商 / 商品 名称映射
  const [o, ch, car, prod] = await Promise.all([
    wb.value?.order_no ? tmsOrderDetail(wb.value.order_no).catch(() => null) : null,
    tmsChannelList({ page: 1, perPage: 500 }).catch(() => null),
    carrierList({ page: 1, perPage: 500 }).catch(() => null),
    productList({ page: 1, perPage: 500 }).catch(() => null)
  ])
  order.value = o
  channelMap.value = {}
  ;(ch?.rows || []).forEach((r) => { channelMap.value[r.id] = r.channel_name })
  carrierMap.value = {}
  ;(car?.rows || []).forEach((r) => { carrierMap.value[r.carrier_code] = r.carrier_name })
  productMap.value = {}
  ;(prod?.rows || []).forEach((r) => { productMap.value[r.id] = r.name })
})

function safeParse(v) {
  if (!v) return []
  try { return JSON.parse(String(v)) } catch { return [] }
}
function fmt(v) { return fmtDateTime(v) }

async function predictEta() {
  etaLoading.value = true
  try { etaResult.value = await etaPredict({ orderNo: wb.value.order_no }) } finally { etaLoading.value = false }
}
</script>

<style scoped>
.mb { margin-bottom: 16px; }
.tip { margin-left: 12px; color: #9ca3af; font-weight: 400; font-size: 12px; }
.eta-bar { margin-top: 14px; }
</style>
