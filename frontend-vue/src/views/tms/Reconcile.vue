<template>
  <div>
    <!-- ============ 状态一：账单列表 / 发起对账 ============ -->
    <template v-if="!active">
      <el-card shadow="never" class="rc-top-card">
        <div class="rc-guide">
          <div class="rc-g-head">
            <div class="rc-g-icon"><el-icon :size="18"><DataAnalysis /></el-icon></div>
            <div>
              <div class="rc-guide-title">差异对账</div>
              <div class="rc-guide-sub">系统入账金额与承运商账单金额逐笔核对 · 差异原因全程留痕</div>
            </div>
          </div>
          <div class="rc-guide-note">对账以系统入账金额与承运商账单金额为准，系统将同一账单内的运单金额逐笔比对：金额一致的运单自动通过，存在差异的运单列入差异明细；为差异运单标注原因后，处理记录将完整留存，作为财务复核与结算的依据。</div>
          <div class="rc-terms">
            <div class="rc-term">
              <div class="rc-term-head"><span class="rc-dot dot-sys"></span><span class="rc-term-name">系统入账金额</span></div>
              <div class="rc-term-desc">账单记录的入账运费</div>
            </div>
            <div class="rc-term">
              <div class="rc-term-head"><span class="rc-dot dot-car"></span><span class="rc-term-name">承运商账单金额</span></div>
              <div class="rc-term-desc">按承运商报价核算的运费</div>
            </div>
            <div class="rc-term">
              <div class="rc-term-head"><span class="rc-dot dot-diff"></span><span class="rc-term-name">差异金额</span></div>
              <div class="rc-term-desc">承运商账单金额 − 系统入账金额</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="rc-block">
        <template #header>
          <div class="rc-bh">
            <b>对账账单</b>
            <span class="rc-bh-meta">共 {{ bills.length }} 张 · 待核销 {{ cnt('SUBMITTED') }} · 待结算 {{ cnt('VERIFIED') }} · 已结算 {{ cnt('SETTLED') }}</span>
          </div>
        </template>
        <el-table :data="bills" v-loading="loadingBills" border stripe size="small" :row-class-name="rowCls" :empty-text="'暂无账单，请先在「账单结算」中生成账单'">
          <el-table-column :resizable="false" prop="bill_no" label="账单号" align="left" width="210" />
          <el-table-column :resizable="false" label="承运商" align="center" width="150">
            <template #default="{ row }">{{ carrierName(row.carrier_id) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" label="账期" align="center" width="235">
            <template #default="{ row }">{{ fmtPeriod(row) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" label="账单金额" align="center" width="120">
            <template #default="{ row }">¥{{ Number(row.total_amount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" label="状态" align="center" width="100">
            <template #default="{ row }"><el-tag :type="tagType(row.status)" size="small">{{ BILL_LABELS[row.status] || row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column :resizable="false" label="操作" align="center" width="100">
            <template #default="{ row }">
              <el-button size="small" link type="primary" :loading="loadingBillId === row.id" @click="goReconcile(row)">对账</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- ============ 状态二：对账结果 ============ -->
    <template v-else-if="result">
      <el-card ref="resultRef" shadow="never" class="rc-block">
        <div class="rc-result-head">
          <div>
            <div class="rc-result-title">{{ result.bill.bill_no }}<el-tag class="rc-bill-tag" :type="tagType(result.bill.status)" size="small">{{ BILL_LABELS[result.bill.status] || result.bill.status }}</el-tag></div>
            <div class="rc-result-sub">{{ carrierName(result.bill.carrier_id) }} · 账期 {{ fmtPeriod(result.bill) }}</div>
          </div>
          <el-button size="small" @click="backToList">返回账单列表</el-button>
        </div>

        <div class="rc-cards">
          <div class="rc-card">
            <div class="rc-card-num">¥{{ Number(result.sysTotal || 0).toFixed(2) }}</div>
            <div class="rc-card-label">系统入账金额</div>
          </div>
          <div class="rc-card">
            <div class="rc-card-num">¥{{ Number(result.expectedTotal || 0).toFixed(2) }}</div>
            <div class="rc-card-label">承运商账单金额</div>
          </div>
          <div class="rc-card" :class="{ warn: Number(result.diffTotal || 0) !== 0 }">
            <div class="rc-card-num">{{ (Number(result.diffTotal) > 0 ? '+' : '') + Number(result.diffTotal || 0).toFixed(2) }}</div>
            <div class="rc-card-label">差异金额（{{ result.diffCount }} 单）</div>
          </div>
        </div>

        <div class="rc-note">差异 = 承运商账单金额 − 系统入账金额：正数表示承运商金额偏高，负数表示偏低。请为存在差异的运单标注原因。</div>

        <el-table :data="result.rows" v-loading="loading" border stripe size="small" :empty-text="'该账单内未发现金额差异'">
          <el-table-column :resizable="false" prop="waybillNo" label="运单号" align="left" width="200" />
          <el-table-column :resizable="false" prop="orderNo" label="订单号" align="left" width="180" />
          <el-table-column :resizable="false" label="系统金额" width="105" align="center">
            <template #default="{ row }">¥{{ Number(row.sys || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" label="承运商金额" width="115" align="center">
            <template #default="{ row }">{{ row.expected == null ? '—' : '¥' + Number(row.expected).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" label="差异" width="115" align="center">
            <template #default="{ row }">
              <span v-if="row.diff != null" :class="['rc-diff', Number(row.diff) > 0 ? 'up' : Number(row.diff) < 0 ? 'down' : '']">
                {{ (Number(row.diff) > 0 ? '+' : '') + Number(row.diff).toFixed(2) }}
              </span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column :resizable="false" label="差异状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.diffStatus === 'HIGHER'" type="warning" size="small">偏高</el-tag>
              <el-tag v-else-if="row.diffStatus === 'LOWER'" type="danger" size="small">偏低</el-tag>
              <el-tag v-else-if="row.diffStatus === 'EQUAL'" type="success" size="small">一致</el-tag>
              <el-tag v-else type="info" size="small">无价卡</el-tag>
            </template>
          </el-table-column>
          <el-table-column :resizable="false" prop="note" label="备注" align="center" width="100" />
          <el-table-column :resizable="false" label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="mark(row)">标记原因</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { inject, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reconcileBills, reconcileDetail, reconcileMark, carrierAll } from '../../api'

const bills = ref([])
const carriers = ref([])
const billId = ref(null)
const result = ref(null)
const resultRef = ref(null)
const active = ref(false)
const loading = ref(false)
const loadingBills = ref(false)
const loadingBillId = ref(null)
const BILL_LABELS = { DRAFT: '草稿', SUBMITTED: '待核销', VERIFIED: '待结算', SETTLED: '已结算', REJECTED: '已驳回' }
const TAG_TYPES = { DRAFT: 'info', SUBMITTED: 'primary', VERIFIED: 'warning', SETTLED: 'success', REJECTED: 'danger' }
const carrierName = (id) => { const c = carriers.value.find((x) => String(x.id) === String(id)); return c ? c.carrier_name : '-' }
const tagType = (s) => TAG_TYPES[s] || 'info'
const cnt = (s) => bills.value.filter((b) => b.status === s).length
function fmtPeriod(row) {
  const s = row && row.period_start ? String(row.period_start).slice(0, 10) : ''
  const e = row && row.period_end ? String(row.period_end).slice(0, 10) : ''
  return s && e ? s + ' ~ ' + e : '-'
}
function rowCls({ row }) { return row.id === billId.value ? 'rc-active-row' : '' }

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('逐笔核对系统入账与承运商账单金额，标记差异原因，保障结算准确')
  loadingBills.value = true
  try {
    const data = (await reconcileBills()) || {}
    bills.value = data.rows || []
    carriers.value = (await carrierAll()) || []
  } catch { /* 请求层统一提示 */ } finally { loadingBills.value = false }
})

async function goReconcile(row) {
  billId.value = row.id
  try {
    await load()
    active.value = true
    const d = result.value ? Number(result.value.diffCount || 0) : 0
    ElMessage.success(d > 0 ? '对账完成：发现 ' + d + ' 单差异，请逐一标记原因' : '对账完成：该账单金额一致，无差异')
    nextTick(() => {
      if (resultRef.value && resultRef.value.$el) {
        resultRef.value.$el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    })
  } catch { /* 错误提示由请求层统一处理 */ }
}

function backToList() {
  active.value = false
  result.value = null
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function load() {
  if (!billId.value) return
  loading.value = true
  loadingBillId.value = billId.value
  try {
    result.value = await reconcileDetail(billId.value)
  } finally {
    loading.value = false
    loadingBillId.value = null
  }
}

async function mark(row) {
  const { value } = await ElMessageBox.prompt('请填写该运单的差异原因', '标记差异原因', {
    inputPlaceholder: '如：重量/运费调整、计价规则不一致',
    inputValidator: (v) => !!v || '原因不能为空'
  })
  await reconcileMark(billId.value, row.waybillNo, value)
  ElMessage.success('已记录差异原因，便于后续复核')
  load()
}
</script>

<style scoped>
.rc-top-card {
  margin-bottom: 16px;
}
.rc-guide {
  position: relative;
  padding: 16px 20px 14px 24px;
  background: linear-gradient(180deg, #f7fcff, #edf7fc);
  border: 1px solid #d8edf6;
  border-radius: 10px;
  overflow: hidden;
}
.rc-guide::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.rc-g-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rc-g-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #bfe7f2;
  border-radius: 8px;
  color: #0891b2;
  flex-shrink: 0;
}
.rc-guide-title { font-size: 16px; font-weight: 600; color: #1d2129; }
.rc-guide-sub { margin-top: 2px; font-size: 12px; color: #7a8794; }
.rc-guide-note { margin-top: 12px; font-size: 13px; color: #475569; line-height: 1.8; }
.rc-terms {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}
.rc-term {
  flex: 1;
  min-width: 210px;
  background: #fff;
  border: 1px solid #e3eef5;
  border-radius: 8px;
  padding: 10px 14px;
}
.rc-term-head {
  display: flex;
  align-items: center;
  gap: 7px;
}
.rc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
.dot-sys { background: #0891b2; }
.dot-car { background: #2563eb; }
.dot-diff { background: #f56c6c; }
.rc-term-name { font-size: 13px; font-weight: 600; color: #1d2129; }
.rc-term-desc { margin-top: 5px; padding-left: 15px; font-size: 12px; color: #86909c; }
.rc-block {
  margin-bottom: 16px;
}
.rc-bh {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.rc-bh-meta {
  font-size: 12px;
  color: #909399;
}
.rc-result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}
.rc-result-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 10px;
}
.rc-bill-tag {
  margin-left: 2px;
}
.rc-result-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #86909c;
}
.rc-cards {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
}
.rc-card {
  flex: 1;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 18px;
}
.rc-card.warn {
  background: #fef0f0;
  border-color: #fbc4c4;
}
.rc-card-num {
  font-size: 22px;
  font-weight: 700;
  color: #0891b2;
}
.rc-card.warn .rc-card-num {
  color: #f56c6c;
}
.rc-card-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.rc-note {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 10px;
}
.rc-diff.up {
  color: #e6a23c;
}
.rc-diff.down {
  color: #f56c6c;
}
:deep(.el-table .rc-active-row td.el-table__cell) {
  background: #eef8fa !important;
}
</style>