<template>
  <div>
    <!-- 理赔总览横幅（与订单/运单同款） -->
    <div v-if="statsLoaded" class="cl-banner">
      <div class="b-head">
        <el-icon :size="15"><Money /></el-icon>
        <span class="b-title">理赔总览</span>
        <span class="b-tip">索赔从登记到审批、赔付/驳回，责任与金额一眼看全</span>
      </div>
      <div class="b-stats">
        <span class="b-item">待审批 <b>{{ stats.submitted ?? 0 }}</b> 单</span>
        <span class="b-item">待赔付 <b>{{ stats.approved ?? 0 }}</b> 单</span>
        <span class="b-item">已赔付 <b>{{ stats.paid ?? 0 }}</b> 单</span>
        <span class="b-item">本月赔付 <b>¥{{ paidTotalText }}</b></span>
      </div>
      <div v-if="stats.submitted" class="b-warn">
        有 <b>{{ stats.submitted }}</b> 单理赔待审批，请在「理赔流程」中做责任认定。
      </div>
      <div v-else-if="stats.approved" class="b-warn ok">
        有 <b>{{ stats.approved }}</b> 单已审批通过、待赔付，请财务尽快执行。
      </div>
      <div v-else class="b-warn ok">暂无待处理的理赔，售后赔付流转顺畅，辛苦啦。</div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.claimStatus" placeholder="理赔状态" clearable style="width: 150px" @change="load">
          <el-option v-for="(v, k) in CLAIM_LABELS" :key="k" :label="v" :value="k" />
        </el-select>
        <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 200px" @clear="load" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div class="spacer" />
        <el-button type="success" @click="openRegister()">登记理赔</el-button>
      </div>

      <div class="cl-note">
        <b>关于金额：</b>「索赔额」是买家或商家申请赔付的金额，「核定额」是平台审核后确定赔付的金额，一般不会超过订单货值。审核通过后，财务会安排打款。
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small" :empty-text="emptyText" @row-click="onRowClick">
        <el-table-column label="订单号" min-width="150" align="left">
          <template #default="{ row }">
            <router-link :to="'/tms/order/' + row.order_no" style="color: #0891b2; text-decoration: none">{{ row.order_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="商家" width="120" align="center">
          <template #default="{ row }">{{ row.merchant_name || '-' }}</template>
        </el-table-column>
        <el-table-column label="责任方" width="84" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.liability" size="small" :type="liabilityType(row.liability)">{{ liabilityLabel(row.liability) }}</el-tag>
            <span v-else>待认定</span>
          </template>
        </el-table-column>
        <el-table-column label="货值" width="104" align="center">
          <template #default="{ row }">
            <span v-if="row.declared_value != null" class="refv">¥{{ Number(row.declared_value).toFixed(2) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="索赔额" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="Number(row.claim_amount) > 0" :content="claimTip(row)" placement="top" :show-after="200">
              <span class="money" :class="{ over: claimOver(row) }">¥{{ Number(row.claim_amount || 0).toFixed(2) }}<span v-if="row.currency && row.currency !== 'CNY'" class="cur">{{ row.currency }}</span></span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="核定额" width="104" align="center">
          <template #default="{ row }">
            <span v-if="Number(row.compensation_amount) > 0" class="comp">¥{{ Number(row.compensation_amount).toFixed(2) }}</span>
            <span v-else class="muted">未核定</span>
          </template>
        </el-table-column>
        <el-table-column label="理赔状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.claim_status)">{{ CLAIM_LABELS[row.claim_status] || row.claim_status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="理赔事由" width="110" align="center">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="176" align="center">
          <template #default="{ row }">{{ fmt(row.claim_submitted_at || row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" style="margin-right: 8px" @click="openDetail(row, false)">详情</el-link>
            <el-link v-if="['SUBMITTED','APPROVED'].includes(row.claim_status)" type="success" :underline="false" @click="openDetail(row, true)">处理</el-link>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
        :page-size="query.perPage" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />
    </el-card>

    <!-- 登记理赔 -->
    <el-dialog v-model="regDialog" title="登记理赔（关联异常工单）" width="500px">
      <el-form :model="reg" label-width="96px">
        <el-form-item label="订单号" required><el-input v-model="reg.orderNo" placeholder="输入发生破损/丢件/延误的订单号" /></el-form-item>
        <el-form-item label="异常类型">
          <el-select v-model="reg.type" style="width: 100%">
            <el-option v-for="(v, k) in TYPE_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任方">
          <el-select v-model="reg.liability" style="width: 100%">
            <el-option v-for="(v, k) in LIABILITY_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="索赔金额" required><el-input-number v-model="reg.claimAmount" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item>
        <el-form-item label="情况说明"><el-input v-model="reg.description" type="textarea" :rows="3" placeholder="丢件/破损程度、赔付依据等" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regDialog = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="doRegister">提交审批</el-button>
      </template>
    </el-dialog>

    <!-- 审批通过：确定核定理赔额 -->
    <el-dialog v-model="approveDialog" title="理赔审批" width="460px">
      <el-form label-width="110px">
        <el-form-item label="工单"><span>#{{ approveRow?.id }} · {{ approveRow?.order_no }}</span></el-form-item>
        <el-form-item label="责任方"><span>{{ liabilityLabel(approveRow?.liability) }}</span></el-form-item>
        <el-form-item label="索赔金额"><span>¥{{ Number(approveRow?.claim_amount || 0).toFixed(2) }}</span></el-form-item>
        <el-form-item v-if="approveRow && Number(approveRow.declared_value) > 0" label="订单货值"><span>¥{{ Number(approveRow.declared_value).toFixed(2) }}</span></el-form-item>
        <el-form-item v-if="approveRow && Number(approveRow.freight_cost) > 0" label="订单运费"><span>¥{{ Number(approveRow.freight_cost).toFixed(2) }}</span></el-form-item>
        <el-form-item label="核定赔付金额" required>
          <el-input-number v-model="compAmount" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-alert type="info" :closable="false" title="审批通过后进入「待赔付」，由财务执行赔付。" />
      </el-form>
      <template #footer>
        <el-button @click="approveDialog = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailOpen" :title="detail ? `理赔 #${detail.id}（${detail.order_no}）` : '理赔详情'" size="520px">
      <template v-if="detail">
        <el-alert v-if="!detailAct" type="info" :closable="false" show-icon title="查看模式" description="如需执行审批、赔付、驳回等处理，请在列表中点击「处理」进入。" style="margin-bottom: 12px" />
        <el-alert v-if="detailAct" type="success" :closable="false" show-icon title="处理方案" description="待审批 → 审批通过并核定赔付金额 → 待赔付 → 财务确认赔付；依据不足可驳回并填写原因。" style="margin-bottom: 12px" />
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">
            <router-link :to="'/tms/order/' + detail.order_no" style="color: #0891b2; text-decoration: none">{{ detail.order_no }}</router-link>
          </el-descriptions-item>
          <el-descriptions-item label="商家">{{ detail.merchant_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
          <el-descriptions-item label="责任方">
            <el-tag v-if="detail.liability" size="small" :type="liabilityType(detail.liability)">{{ liabilityLabel(detail.liability) }}</el-tag>
            <span v-else>待认定</span>
          </el-descriptions-item>
          <el-descriptions-item label="当前节点"><StatusTag :status="detail.current_node" /></el-descriptions-item>
          <el-descriptions-item label="货值"><span class="money">¥{{ Number(detail.declared_value || 0).toFixed(2) }}</span></el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ Number(detail.freight_cost || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="目的地">{{ detail.destination_country || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.waybill_no" label="运单号">{{ detail.waybill_no }}</el-descriptions-item>
          <el-descriptions-item label="索赔金额"><span class="money">¥{{ Number(detail.claim_amount || 0).toFixed(2) }}</span></el-descriptions-item>
          <el-descriptions-item label="核定赔付">¥{{ Number(detail.compensation_amount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="理赔状态"><el-tag size="small" :type="tagType(detail.claim_status)">{{ CLAIM_LABELS[detail.claim_status] || detail.claim_status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="情况说明">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.resolution" label="处理结果">{{ detail.resolution }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.claim_reject_reason" label="驳回原因" class="reject-text">{{ detail.claim_reject_reason }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmt(detail.claim_submitted_at || detail.created_at) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.claim_paid_at" label="赔付时间">{{ fmt(detail.claim_paid_at) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <div v-else class="drawer-loading">加载中…</div>
      <template #footer>
        <el-button @click="detailOpen = false">返回</el-button>
        <el-button v-if="detailAct && detail && detail.claim_status === 'SUBMITTED'" type="primary" @click="drawerAct('approve')">审批通过</el-button>
        <el-button v-if="detailAct && detail && detail.claim_status === 'APPROVED'" type="success" @click="drawerAct('pay')">确认赔付</el-button>
        <el-button v-if="detailAct && detail && ['SUBMITTED','APPROVED'].includes(detail.claim_status)" type="danger" plain @click="drawerAct('reject')">驳回</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { claimStats, claimList, claimDetail, claimRegister, claimApprove, claimPay, claimReject } from '../../api'
import { fmtDateTime } from '../../utils/format'

const CLAIM_LABELS = { SUBMITTED: '待审批', APPROVED: '待赔付', PAID: '已赔付', REJECTED: '已驳回', NONE: '未理赔' }
const TYPE_LABELS = {
  CLAIM: '理赔申请',
  lost: '丢件', damaged: '破损', returned: '退回', delivery_failed: '派送失败',
  customs_delay: '中转延误', sla_breach: '时效违约', OTHER: '其他异常'
}
const LIABILITY_LABELS = { carrier: '承运商', merchant: '商家', platform: '平台' }
const tagType = (s) => ({ SUBMITTED: 'warning', APPROVED: 'primary', PAID: 'success', REJECTED: 'info', NONE: 'info' }[s] || 'info')
const liabilityLabel = (l) => LIABILITY_LABELS[l] || l || '-'
const liabilityType = (l) => ({ carrier: 'warning', merchant: 'info', platform: 'primary' }[l] || 'info')
const typeLabel = (t) => TYPE_LABELS[t] || t || '其他'
const claimOver = (r) => Number(r.declared_value || 0) > 0 && Number(r.claim_amount || 0) > Number(r.declared_value)
const claimTip = (r) => (claimOver(r) ? '索赔额已超过订单申报货值，请核实损失依据后再核定' : '索赔额 = 买家或商家提出的期望赔付金额')

const setDesc = inject('setPageDesc')
const route = useRoute()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const query = reactive({
  claimStatus: route.query.claimStatus || (route.query.orderNo ? '' : 'SUBMITTED'),
  orderNo: route.query.orderNo || '',
  page: 1,
  perPage: 10
})
const emptyText = query.claimStatus === 'SUBMITTED'
  ? '暂无待审批的理赔，可在「问题工单 → 索赔」或右上角「登记理赔」发起'
  : '暂无理赔记录'

const statsLoaded = ref(false)
const paidTotalText = computed(() => (stats.value.paidTotal != null ? Number(stats.value.paidTotal).toFixed(2) : '0.00'))
const fmt = (v) => fmtDateTime(v)

const regDialog = ref(false)
const regLoading = ref(false)
const reg = reactive({ orderNo: '', type: 'CLAIM', liability: 'carrier', claimAmount: 0, description: '' })

const approveDialog = ref(false)
const acting = ref(false)
const approveRow = ref(null)
const compAmount = ref(0)

const detailOpen = ref(false)
const detail = ref(null)
const detailAct = ref(false)

async function loadStats() {
  try { stats.value = await claimStats() } catch { stats.value = {} } finally { statsLoaded.value = true }
}

async function load() {
  loading.value = true
  try {
    const data = await claimList({ ...query })
    rows.value = data.rows || []
    total.value = data.count || 0
  } catch (e) { rows.value = []; total.value = 0 } finally { loading.value = false }
}

function openRegister(orderNo) {
  Object.assign(reg, { orderNo: orderNo || '', type: 'CLAIM', liability: 'carrier', claimAmount: 0, description: '' })
  regDialog.value = true
}

async function doRegister() {
  if (!reg.orderNo) return ElMessage.warning('请填写订单号')
  if (!(reg.claimAmount > 0)) return ElMessage.warning('请填写索赔金额')
  regLoading.value = true
  try {
    const res = await claimRegister({ ...reg })
    ElMessage.success(res.status ? `理赔已登记（${CLAIM_LABELS[res.status] || res.status}）` : '理赔已登记，进入待审批')
    regDialog.value = false
    load(); loadStats()
  } catch (e) {
    ElMessage.error((e && e.message) || '登记失败，请确认订单是否存在且未在理赔流程中')
  } finally { regLoading.value = false }
}

function openApprove(row) {
  approveRow.value = row
  compAmount.value = Number(row.compensation_amount > 0 ? row.compensation_amount : row.claim_amount)
  approveDialog.value = true
}

async function doApprove() {
  if (!(compAmount.value >= 0)) return ElMessage.warning('请填写核定赔付金额')
  acting.value = true
  try {
    await claimApprove(approveRow.value.id, { compensationAmount: compAmount.value })
    ElMessage.success('审批通过，进入「待赔付」')
    approveDialog.value = false
    load(); loadStats()
  } finally { acting.value = false }
}

async function pay(row) {
  await ElMessageBox.confirm(
    `确认对工单 #${row.id}（${row.order_no}）执行赔付 ¥${Number(row.compensation_amount || 0).toFixed(2)}？`,
    '确认赔付', { type: 'warning' }
  )
  await claimPay(row.id)
  ElMessage.success('已赔付完成')
  load(); loadStats()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('填写驳回原因（必填）', '驳回理赔', {
    inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因'),
    type: 'warning'
  }).catch(() => ({ value: null }))
  if (value == null) return
  await claimReject(row.id, { reason: value })
  ElMessage.success('已驳回理赔')
  load(); loadStats()
}

// 操作列统一下拉：detail 看详情，其余走对应流程动作
function onCmd(row, cmd) {
  if (cmd === 'detail') return openDetail(row)
  if (cmd === 'approve') return openApprove(row)
  if (cmd === 'pay') return pay(row)
  if (cmd === 'reject') return reject(row)
}
function onRowClick(row, column, event) {
  if (event && event.target && event.target.closest && event.target.closest('a, button')) return
  openDetail(row, true)
}
// 抽屉内动作：先收起详情，再走对应流程（审批弹窗 / 赔付确认 / 驳回原因）
function drawerAct(name) {
  const row = detail.value
  detailOpen.value = false
  if (name === 'approve') return openApprove(row)
  if (name === 'pay') return pay(row)
  if (name === 'reject') return reject(row)
}

async function openDetail(row, act = true) {
  detail.value = null
  detailAct.value = act !== false
  detailOpen.value = true
  try { detail.value = await claimDetail(row.id) } catch { detail.value = null }
}

onMounted(() => {
  setDesc('理赔流程闭环：工单/退货中发起索赔 → 责任认定审批 → 赔付执行 → 结案')
  if (route.query.reg === '1' && route.query.orderNo) openRegister(route.query.orderNo)
  loadStats()
  load()
})
</script>

<style scoped>
/* 理赔总览横幅（与订单/运单同款样式） */
.cl-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.cl-banner::before {
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
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; align-items: center; }
.toolbar .spacer { flex: 1; }
.pager { margin-top: 14px; justify-content: flex-end; }
.money { color: #d97706; font-weight: 600; }
.money.over { color: #ef4444; }
.cl-note { margin: -2px 0 12px; padding: 9px 12px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.6; }
.cl-note b { color: #155e75; }
.refv { color: #57606a; }
.cur { font-size: 12px; color: #86909c; margin-left: 2px; }
.muted { color: #9ca3af; }
.comp { color: #10b981; font-weight: 600; }
.type-prefix { color: #0891b2; font-weight: 500; margin-right: 4px; }
.drawer-loading { color: #9ca3af; text-align: center; padding: 40px 0; }
.reject-text :deep(.el-descriptions__content) { color: #ef4444; }
/* 长内容单行 + 省略号，溢出悬停显示全文 */
:deep(.el-table .cell) { white-space: nowrap; }
</style>
