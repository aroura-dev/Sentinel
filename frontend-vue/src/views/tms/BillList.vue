<template>
  <div>
    <div v-if="statsLoaded" class="fn-banner">
      <div class="fn-head"><el-icon :size="15"><Money /></el-icon><span class="fn-title">账单结算总览</span><span class="fn-tip">从生成账单、对账核销到打款结算，一眼看清每张承运商账单</span></div>
      <div class="fn-stats">
        <span class="fn-item">全部账单 <b>{{ stats.total ?? 0 }}</b> 张</span>
        <span class="fn-item">草稿 <b>{{ stats.draft ?? 0 }}</b></span>
        <span class="fn-item">待核销 <b>{{ stats.submitted ?? 0 }}</b></span>
        <span class="fn-item">已结算 <b>{{ stats.settled ?? 0 }}</b></span>
      </div>
      <div v-if="(stats.submitted || 0) > 0" class="fn-warn">有 <b>{{ stats.submitted }}</b> 张账单已提交待核对，请尽快「核销」，核对无误即可结算。</div>
      <div v-else-if="(stats.draft || 0) > 0" class="fn-warn ok">有 <b>{{ stats.draft }}</b> 张草稿账单，补充运单后点「提交」即可进入核销流程。</div>
      <div v-else class="fn-warn ok">暂无待处理账单，可按承运商和账期点右上角「生成账单」。</div>
    </div>
    <DataTable ref="dt" title="计费结算（承运商 × 账期）" :columns="columns" :load="billList" :query="query" :action-width="150">
    <template #query="{ reload }">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="s in [['DRAFT','草稿'],['SUBMITTED','待核销'],['VERIFIED','待结算'],['SETTLED','已结算'],['REJECTED','已驳回']]" :key="s[0]" :label="s[1]" :value="s[0]" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openGen">生成账单</el-button>
    </template>
    <template #cell-status="{ row }"><StatusTag :status="row.status" /></template>
    <template #cell-total_amount="{ row }">¥{{ Number(row.total_amount).toFixed(2) }}</template>
    <template #cell-carrier_id="{ row }">{{ carrierName(row.carrier_id) }}</template>
    <template #actions="{ row }">
      <div class="ops">
        <el-button size="small" link type="primary" @click="goDetail(row)">详情</el-button>
        <el-button v-if="handlesOf(row).length" size="small" type="success" @click="openHandle(row)">处理</el-button>
        <el-button v-else size="small" type="success" class="ops-ph" tabindex="-1" aria-hidden="true">处理</el-button>
      </div>
    </template>
  </DataTable>

  <el-dialog v-model="genDialog" title="生成账单" width="460px">
    <el-form label-width="90px">
      <el-form-item label="承运商" required>
        <el-select v-model="gen.carrierId" filterable style="width: 100%">
          <el-option v-for="c in carriers" :key="c.id" :label="c.carrier_name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期起" required><el-input v-model="gen.periodStart" placeholder="2026-08-01" /></el-form-item>
      <el-form-item label="账期止" required><el-input v-model="gen.periodEnd" placeholder="2026-08-31" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="genDialog = false">取消</el-button>
      <el-button type="primary" :loading="genLoading" @click="doGenerate">生成</el-button>
    </template>
  </el-dialog>

  <!-- 统一“处理”入口：先选择处理方式，再执行 -->
  <el-dialog v-model="handleDialog" :title="handleRow ? '处理账单：' + handleRow.bill_no : '处理账单'" width="460px">
    <el-form label-position="top">
      <el-form-item label="选择处理方式">
        <el-radio-group v-model="handleOp" class="hd-group">
          <div v-for="op in handleOpts" :key="op.key" class="hd-opt">
            <el-radio :value="op.key">{{ op.label }}</el-radio>
            <div v-if="handleOp === op.key" class="hd-tip">{{ op.tip }}</div>
          </div>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="handleOp === 'reject'" label="驳回原因" required>
        <el-input v-model="handleReason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请填写驳回原因（必填）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleDialog = false">取消</el-button>
      <el-button type="primary" :loading="handleLoading" @click="confirmHandle">处理</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { billList, billGenerate, billSubmit, billVerify, billSettle, billReject, billReopen, billStats, carrierAll } from '../../api'
const carrierName = (id) => { const c = carriers.value.find((x) => String(x.id) === String(id)); return c ? c.carrier_name : '-' }

// 每种状态下可选的“处理方式”
const HANDLES = {
  DRAFT: [{ key: 'submit', label: '提交', tip: '提交后进入「待核销」' }],
  SUBMITTED: [
    { key: 'verify', label: '核销', tip: '核对无误后进入「待结算」' },
    { key: 'reject', label: '驳回', tip: '驳回退回草稿，需填写原因', danger: true }
  ],
  VERIFIED: [
    { key: 'settle', label: '结算', tip: '确认无误并完成打款结算' },
    { key: 'reject', label: '驳回', tip: '驳回退回草稿，需填写原因', danger: true }
  ],
  REJECTED: [{ key: 'reopen', label: '重开', tip: '重新打开为「草稿」' }]
}
const handlesOf = (row) => HANDLES[row.status] || []

const router = useRouter()
const carriers = ref([])
const query = reactive({ status: '' })
const stats = ref({})
const statsLoaded = ref(false)
const dt = ref(null)
const genDialog = ref(false)
const genLoading = ref(false)
const gen = reactive({ carrierId: null, periodStart: '', periodEnd: '' })
const handleDialog = ref(false)
const handleLoading = ref(false)
const handleRow = ref(null)
const handleOp = ref('')
const handleReason = ref('')
const handleOpts = computed(() => handlesOf(handleRow.value || {}))
const currentHandle = computed(() => handleOpts.value.find((o) => o.key === handleOp.value) || null)
const columns = [
  { prop: 'bill_no', label: '账单号', minWidth: 180 },
  { prop: 'carrier_id', label: '承运商', width: 140 },
  { prop: 'period_start', label: '账期起', width: 110, type: 'datetime' },
  { prop: 'period_end', label: '账期止', width: 110, type: 'datetime' },
  { prop: 'total_amount', label: '总额', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'tag' }
]

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('承运商账单与结算状态流转（草稿 → 提交 → 核对 → 结算）')
  carriers.value = (await carrierAll()) || []
  try { stats.value = (await billStats()) || {} } catch { stats.value = {} } finally { statsLoaded.value = true }
})

function openGen() { genDialog.value = true }

async function doGenerate() {
  if (!gen.carrierId || !gen.periodStart || !gen.periodEnd) return ElMessage.warning('请填写完整账期')
  genLoading.value = true
  try {
    const res = await billGenerate(gen)
    ElMessage.success(`账单已生成：${res.bill.bill_no}，共 ${res.itemCount} 单，总额 ¥${res.totalAmount}`)
    genDialog.value = false
    dt.value.reload()
  } finally { genLoading.value = false }
}

function openHandle(row) {
  handleRow.value = row
  const opts = handlesOf(row)
  handleOp.value = opts[0] ? opts[0].key : ''
  handleReason.value = ''
  handleDialog.value = true
}

async function confirmHandle() {
  const row = handleRow.value
  const op = currentHandle.value
  if (!row || !op) return
  if (op.key === 'reject' && !(handleReason.value || '').trim()) return ElMessage.warning('请填写驳回原因')
  handleLoading.value = true
  try {
    if (op.key === 'reject') {
      await billReject(row.id, handleReason.value.trim())
    } else {
      const fn = { submit: billSubmit, verify: billVerify, settle: billSettle, reopen: billReopen }[op.key]
      await fn(row.id)
    }
    ElMessage.success(op.key === 'reject' ? '已驳回' : '处理成功')
    handleDialog.value = false
    dt.value.reload()
  } finally { handleLoading.value = false }
}

function goDetail(row) { router.push(`/tms/bill/${row.id}`) }
</script>

<style scoped>
.fn-banner {
  margin-bottom: 16px;
  padding: 14px 18px 12px 22px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.fn-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.fn-head { display: flex; align-items: center; gap: 8px; }
.fn-head .el-icon { color: #0891b2; }
.fn-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.fn-tip { font-size: 12px; color: #9ca3af; }
.fn-stats { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 26px; margin-top: 10px; }
.fn-item { font-size: 13px; color: #57606a; }
.fn-item b { font-size: 16px; color: #1d2129; margin-left: 2px; }
.fn-warn { margin-top: 10px; font-size: 13px; color: #b7791f; background: #fef6ec; border: 1px solid #f5e1c3; border-radius: 6px; padding: 7px 12px; }
.fn-warn b { color: #d97706; }
.fn-warn.ok { color: #0e7490; background: #eef8fa; border-color: #d9f0f4; }
.ops { display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.ops :deep(.el-button + .el-button) { margin-left: 0; }
.ops-ph { visibility: hidden; pointer-events: none; }
.hd-group { display: flex; flex-direction: column; align-items: stretch; row-gap: 4px; }
.hd-tip { margin: 4px 0 2px 26px; font-size: 12px; color: #86909c; }
</style>