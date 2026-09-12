<template>
  <div v-if="data">
    <el-card shadow="never" class="mb">
      <template #header>
        <b>账单：{{ data.bill.bill_no }}</b>
        <StatusTag :status="data.bill.status" class="status-tag" />
        <el-button-group style="margin-left: 12px">
          <el-button v-if="data.bill.status === 'DRAFT'" type="success" size="small" @click="openAdd">添加运单</el-button>
          <el-button v-if="data.bill.status === 'DRAFT'" type="primary" size="small" @click="act('submit')">提交</el-button>
          <el-button v-if="data.bill.status === 'SUBMITTED'" type="warning" size="small" @click="act('verify')">核销</el-button>
          <el-button v-if="data.bill.status === 'VERIFIED'" type="success" size="small" @click="act('settle')">结算</el-button>
          <el-button v-if="['SUBMITTED','VERIFIED'].includes(data.bill.status)" type="danger" size="small" @click="reject">驳回</el-button>
          <el-button v-if="data.bill.status === 'REJECTED'" size="small" @click="act('reopen')">重开</el-button>
        </el-button-group>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="承运商">{{ carrierName(data.bill.carrier_id) }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ String(data.bill.period_start).slice(0,10) }} ~ {{ String(data.bill.period_end).slice(0,10) }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ data.bill.currency }}</el-descriptions-item>
        <el-descriptions-item label="总额"><b style="color:#0891b2">¥{{ Number(data.bill.total_amount).toFixed(2) }}</b></el-descriptions-item>
        <el-descriptions-item label="提交">{{ data.bill.submitted_by }} {{ fmt(data.bill.submitted_at) }}</el-descriptions-item>
        <el-descriptions-item label="核销">{{ data.bill.verified_by }} {{ fmt(data.bill.verified_at) }}</el-descriptions-item>
        <el-descriptions-item label="结算">{{ data.bill.settled_by }} {{ fmt(data.bill.settled_at) }}</el-descriptions-item>
        <el-descriptions-item v-if="data.bill.reject_reason" label="驳回原因">{{ data.bill.reject_reason }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header><b>账单明细（{{ items.length }} 单）</b></template>
      <el-table :data="items" border stripe size="small">
        <el-table-column align="left" label="运单号" min-width="170">
          <template #default="{ row }">
            <router-link :to="'/tms/waybill/' + row.waybill_no" style="color: #0891b2; text-decoration: none">{{ row.waybill_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column align="left" label="订单号" min-width="170">
          <template #default="{ row }">
            <router-link :to="'/tms/order/' + row.order_no" style="color: #0891b2; text-decoration: none">{{ row.order_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column align="left" prop="tracking_no" label="跟踪号" min-width="150" />
        <el-table-column label="商家" width="110"><template #default="{ row }">{{ merchantName(row.merchant_id) }}</template></el-table-column>
        <el-table-column prop="billable_weight_kg" label="计费重" width="90" />
        <el-table-column label="运费" width="100">
          <template #default="{ row }">¥{{ Number(row.freight_cost).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="70" />
      </el-table>
      <el-dialog v-model="addDialog" title="手动入账运单" width="420px">
        <el-form label-width="90px">
          <el-form-item label="运单号">
            <el-input v-model="addWaybillNo" placeholder="如 WB-US-SEED-0003" @keyup.enter="doAdd" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addDialog = false">取消</el-button>
          <el-button type="primary" :loading="addLoading" @click="doAdd">入账</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import { billDetail, billSubmit, billVerify, billSettle, billReject, billReopen, billAddWaybill, carrierAll, merchantAll } from '../../api'

const route = useRoute()
const data = ref(null)
const carriers = ref([])
const merchants = ref([])
const carrierName = (id) => { const c = carriers.value.find((x) => String(x.id) === String(id)); return c ? c.carrier_name : '-' }
const merchantName = (id) => { const m = merchants.value.find((x) => String(x.id) === String(id)); return m ? (m.merchant_name || m.name || '-') : '-' }
const items = computed(() => data.value.items || [])
const addDialog = ref(false)
const addLoading = ref(false)
const addWaybillNo = ref('')

onMounted(async () => {
  const [d, cars, mers] = await Promise.all([billDetail(route.params.id), carrierAll(), merchantAll()])
  data.value = d
  carriers.value = cars || []
  merchants.value = mers || []
})

function openAdd() { addWaybillNo.value = ''; addDialog.value = true }

async function doAdd() {
  if (!addWaybillNo.value) return ElMessage.warning('请输入运单号')
  addLoading.value = true
  try {
    await billAddWaybill(data.value.bill.id, addWaybillNo.value)
    ElMessage.success('入账成功')
    addDialog.value = false
    data.value = await billDetail(route.params.id)
  } finally { addLoading.value = false }
}

function fmt(v) { return v ? String(v).slice(0, 19) : '-' }

async function act(action) {
  const fn = { submit: billSubmit, verify: billVerify, settle: billSettle, reopen: billReopen }[action]
  await fn(data.value.bill.id)
  ElMessage.success('操作成功')
  data.value = await billDetail(route.params.id)
}

async function reject() {
  const { value } = await ElMessageBox.prompt('驳回原因', '驳回账单', { inputPlaceholder: '请输入驳回原因' })
  await billReject(data.value.bill.id, value || '')
  ElMessage.success('已驳回')
  data.value = await billDetail(route.params.id)
}
</script>

<style scoped>
.mb { margin-bottom: 16px; }
.status-tag { margin-left: 10px; }
</style>
