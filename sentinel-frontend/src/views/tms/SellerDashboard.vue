<template>
  <div>
    <el-card shadow="never" class="mb">
      <el-form inline>
        <el-form-item label="商家">
          <el-select v-if="!isMerchant" v-model="merchantId" placeholder="全部商家" clearable style="width: 220px" @change="load">
            <el-option v-for="m in merchants" :key="m.id" :label="`${m.merchant_name}（${m.merchant_code}）`" :value="m.id" />
          </el-select>
          <el-tag v-else type="info">当前商家：{{ merchantName }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="cards">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never">
          <div class="card-label">{{ c.label }}</div>
          <div class="card-value">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-head">
          <b>账单状态分布</b>
          <span v-if="billRows.length" class="bill-summary">共 <b>{{ billTotal.cnt }}</b> 单 · 合计 <b>¥{{ billTotal.amount.toFixed(2) }}</b></span>
        </div>
      </template>
      <div v-if="!billRows.length" class="empty-block">
        <el-icon :size="40" class="empty-icon"><Money /></el-icon>
        <div class="empty-text">暂无账单数据</div>
      </div>
      <BaseChart v-else :option="billOption" height="300px" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from '../../components/BaseChart.vue'
import { sellerDashboard, merchantAll } from '../../api'

const isMerchant = localStorage.getItem('sentinel_role') === 'MERCHANT'
const merchantId = ref(null)
const merchants = ref([])
const merchantName = ref('')
const data = ref(null)
const loading = ref(false)

const cards = computed(() => {
  if (!data.value) return []
  const d = data.value
  return [
    { label: '总订单', value: d.totalOrders },
    { label: '在途', value: d.inTransit },
    { label: '已妥投', value: d.delivered },
    { label: 'SLA预警/违约', value: d.slaRisk }
  ]
})

const BILL_STATUS = { DRAFT: '待提交', SUBMITTED: '待审核', VERIFIED: '待结算', SETTLED: '已结算', REJECTED: '已驳回', CLOSED: '已关闭' }

const billRows = computed(() => (data.value && data.value.bills) || [])
const billTotal = computed(() => billRows.value.reduce((acc, b) => {
  acc.cnt += Number(b.cnt) || 0
  acc.amount += Number(b.amount) || 0
  return acc
}, { cnt: 0, amount: 0 }))

const billOption = computed(() => {
  const bills = billRows.value
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const b = bills[params[0] && params[0].dataIndex]
        if (!b) return ''
        return `${BILL_STATUS[b.status] || b.status}<br/>单数：${b.cnt}<br/>金额：¥${Number(b.amount).toFixed(2)}`
      }
    },
    grid: { left: 8, right: 8, top: 44, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: bills.map((b) => BILL_STATUS[b.status] || b.status), axisLabel: { color: '#4e5969' } },
    yAxis: { type: 'value', name: '金额', axisLabel: { color: '#86909c' } },
    series: [{
      type: 'bar',
      // 细柱，避免柱子太粗难看
      barWidth: 26,
      barMaxWidth: 34,
      data: bills.map((b) => Number(b.amount)),
      itemStyle: { color: '#0891b2', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', formatter: '¥{c}', color: '#1d2129', fontSize: 12 }
    }]
  }
})

async function load() {
  loading.value = true
  try {
    data.value = await sellerDashboard(merchantId.value)
    if (isMerchant) {
      const all = (await merchantAll()) || []
      const me = all.find((m) => m.user_id && String(m.id) === String(merchantId.value))
      merchantName.value = me ? me.merchant_name : ''
    }
  } catch (e) {
    ElMessage.error('看板加载失败，请确认角色权限')
  } finally { loading.value = false }
}

onMounted(async () => {
  if (!isMerchant) merchants.value = (await merchantAll()) || []
  await load()
})
</script>

<style scoped>
.cards { margin-bottom: 16px; }
.card-label { color: #6b7280; font-size: 13px; }
.card-value { font-size: 26px; font-weight: 600; margin-top: 6px; color: #1d2129; }
.mb { margin-bottom: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.bill-summary { font-size: 13px; color: #6b7280; font-weight: 400; }
.bill-summary b { color: #1d2129; font-weight: 600; }
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 52px 0;
}
.empty-icon { color: #c0c4cc; }
.empty-text { margin-top: 10px; font-size: 13px; color: #9ca3af; }
</style>
