<template>
  <div>
    <div class="ra-banner">
      <div class="ra-head"><el-icon :size="15"><Warning /></el-icon><span class="ra-title">风险预警</span><span class="ra-tip">按承诺时效识别临近超时与已超时订单，自动/手动处置</span></div>
      <div class="ra-stats">
        <span class="ra-item">预警中 <b>{{ ov.risk ?? 0 }}</b></span>
        <span class="ra-item">已违约 <b>{{ ov.breach ?? 0 }}</b></span>
        <span class="ra-item">启用自动规则 <b>{{ ov.enabledRules ?? 0 }}</b> / {{ ov.totalRules ?? 0 }}</span>
        <span class="ra-item">触发场景 <b>{{ ov.sceneCount ?? 0 }}</b> 类</span>
      </div>
    </div>
    <div class="ra-note">对临近超时与已超时的订单主动预警：既可配置自动通知、自动建工单，也支持逐单手动处置。</div>
    <!-- 处置规则配置 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <b>自动处置规则</b>
          <el-button type="primary" size="small" @click="openRule()">新增规则</el-button>
        </div>
      </template>
      <el-table :data="rules" v-loading="ruleLoading" border stripe size="small">
        <el-table-column prop="name" label="规则名称" min-width="160" />
        <el-table-column prop="scene" label="场景" width="90">
          <template #default="{ row }">{{ row.scene === 'ANOMALY' ? '运输异常' : '时效 SLA' }}</template>
        </el-table-column>
        <el-table-column prop="trigger_status" label="触发状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.trigger_status === 'BREACHED' ? 'danger' : 'warning'">{{ row.trigger_status === 'BREACHED' ? '违约' : '预警' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="处置动作" width="150">
          <template #default="{ row }">{{ row.action === 'CREATE_WORKORDER' ? '自动建工单' : '自动通知' }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openRule(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="delRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- SLA 风险订单 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <b>SLA 风险订单</b>
          <div>
            <el-select v-model="slaStatus" placeholder="SLA 状态" clearable style="width: 140px" @change="loadOrders">
              <el-option label="预警 RISK" value="RISK" />
              <el-option label="违约 BREACHED" value="BREACHED" />
            </el-select>
            <el-button type="primary" size="small" style="margin-left: 8px" @click="loadOrders">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="orders" v-loading="orderLoading" border stripe size="small">
        <el-table-column align="left" prop="order_no" label="订单号" min-width="170">
          <template #default="{ row }">
            <router-link :to="'/tms/order/' + row.order_no" style="color: #0891b2; text-decoration: none">{{ row.order_no }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="merchant_name" label="商家" min-width="120" />
        <el-table-column label="目的地" width="100"><template #default="{ row }">{{ countryLabel(row.destination_country) }}</template></el-table-column>
        <el-table-column label="当前节点" width="120"><template #default="{ row }"><StatusTag :status="row.current_node" /></template></el-table-column>
        <el-table-column prop="sla_status" label="时效状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sla_status === 'BREACHED' ? 'danger' : 'warning'">{{ row.sla_status === 'BREACHED' ? '违约' : '预警' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="promise_eta" label="承诺送达" width="170" type="datetime" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" link type="warning" @click="openHandle(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="total, prev, pager, next" :total="orderTotal"
        :page-size="orderQuery.perPage" :current-page="orderQuery.page" @current-change="(p) => { orderQuery.page = p; loadOrders() }" />
    </el-card>

    <el-dialog v-model="ruleDialog" :title="ruleForm.id ? '编辑规则' : '新增规则'" width="460px">
      <el-form :model="ruleForm" label-width="90px">
        <el-form-item label="规则名称" required><el-input v-model="ruleForm.name" /></el-form-item>
        <el-form-item label="场景">
          <el-select v-model="ruleForm.scene" style="width: 100%">
            <el-option label="SLA 时效" value="SLA" />
            <el-option label="运输异常" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发状态">
          <el-select v-model="ruleForm.triggerStatus" clearable style="width: 100%">
            <el-option label="预警 RISK" value="RISK" />
            <el-option label="违约 BREACHED" value="BREACHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置动作">
          <el-select v-model="ruleForm.action" style="width: 100%">
            <el-option label="自动通知相关方" value="NOTIFY" />
            <el-option label="自动创建异常工单" value="CREATE_WORKORDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
    <!-- 风险订单处置：先点「处置」，再选择具体规则执行 -->
    <el-dialog v-model="handleDialog" :title="handleRow ? '处置订单 ' + handleRow.order_no : '处置风险订单'" width="460px">
      <div v-if="!enabledRules.length" class="ra-empty">暂无启用中的处置规则，请先在「自动处置规则」中新增并启用规则。</div>
      <template v-else>
        <el-radio-group v-model="handleRuleId">
          <div v-for="r in enabledRules" :key="r.id" class="ra-opt">
            <el-radio :value="r.id">{{ r.name }}</el-radio>
            <div class="ra-opt-tip">{{ r.scene === 'ANOMALY' ? '运输异常' : '时效 SLA' }} · {{ r.action === 'CREATE_WORKORDER' ? '自动建工单' : '自动通知相关方' }}</div>
          </div>
        </el-radio-group>
      </template>
      <template #footer>
        <el-button @click="handleDialog = false">取消</el-button>
        <el-button type="warning" :disabled="!handleRuleId" :loading="handleLoading" @click="confirmHandle">执行处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riskRuleList, riskRuleSave, riskRuleUpdate, riskRuleDelete, riskOrders, riskExecute } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { countryLabel } from '../../utils/country'

const setDesc = inject('setPageDesc')
const rules = ref([])
const ruleLoading = ref(false)
const ruleDialog = ref(false)
const ruleSaving = ref(false)
const ruleForm = reactive({ id: null, name: '', scene: 'SLA', triggerStatus: 'RISK', action: 'NOTIFY', enabled: 1 })

const orders = ref([])
const ov = ref({})
const orderTotal = ref(0)
const orderLoading = ref(false)
const slaStatus = ref('')
const orderQuery = reactive({ page: 1, perPage: 10 })

async function loadRules() {
  ruleLoading.value = true
  try { rules.value = (await riskRuleList()) || [] } catch (e) { rules.value = [] } finally { ruleLoading.value = false }
}

async function loadOverview() {
  try {
    const [risk, breach, list] = await Promise.all([
      riskOrders({ slaStatus: 'RISK', page: 1, perPage: 1 }),
      riskOrders({ slaStatus: 'BREACHED', page: 1, perPage: 1 }),
      riskRuleList()
    ])
    const rulesArr = list || []
    ov.value = {
      risk: (risk && risk.count) || 0,
      breach: (breach && breach.count) || 0,
      enabledRules: rulesArr.filter((r) => Number(r.enabled) === 1).length,
      totalRules: rulesArr.length,
      sceneCount: new Set(rulesArr.map((r) => r.scene)).size
    }
  } catch { /* 请求层统一提示 */ }
}

function openRule(row) {
  if (row) Object.assign(ruleForm, { id: row.id, name: row.name, scene: row.scene, triggerStatus: row.trigger_status, action: row.action, enabled: row.enabled })
  else Object.assign(ruleForm, { id: null, name: '', scene: 'SLA', triggerStatus: 'RISK', action: 'NOTIFY', enabled: 1 })
  ruleDialog.value = true
}

async function saveRule() {
  if (!ruleForm.name) return ElMessage.warning('请填写规则名称')
  ruleSaving.value = true
  try {
    if (ruleForm.id) await riskRuleUpdate(ruleForm.id, { ...ruleForm })
    else await riskRuleSave({ ...ruleForm })
    ElMessage.success('规则已保存')
    ruleDialog.value = false
    loadRules(); loadOverview()
  } finally { ruleSaving.value = false }
}

async function delRule(row) {
  await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '提示', { type: 'warning' })
  await riskRuleDelete(row.id)
  ElMessage.success('已删除')
  loadRules(); loadOverview()
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const data = await riskOrders({ slaStatus: slaStatus.value, page: orderQuery.page, perPage: orderQuery.perPage })
    orders.value = (data.rows || []).map((r) => ({ ...r, _ruleId: rules.value[0]?.id }))
    orderTotal.value = data.count || 0
  } catch (e) { orders.value = [] } finally { orderLoading.value = false }
}

async function doExecute(row) {
  if (!row._ruleId) return ElMessage.warning('请选择处置规则')
  const res = await riskExecute({ ruleId: row._ruleId, orderNo: row.order_no })
  ElMessage.success(res.message || '处置动作已执行')
  loadOrders(); loadOverview()
}
const enabledRules = computed(() => rules.value.filter((r) => Number(r.enabled) === 1))
const handleDialog = ref(false)
const handleLoading = ref(false)
const handleRow = ref(null)
const handleRuleId = ref(null)

function openHandle(row) {
  handleRow.value = row
  handleRuleId.value = enabledRules.value[0] ? enabledRules.value[0].id : null
  handleDialog.value = true
}

async function confirmHandle() {
  if (!handleRuleId.value) return ElMessage.warning('请选择处置规则')
  handleLoading.value = true
  try {
    const res = await riskExecute({ ruleId: handleRuleId.value, orderNo: handleRow.value.order_no })
    ElMessage.success(res.message || '处置动作已执行')
    handleDialog.value = false
    loadOrders(); loadOverview()
  } finally {
    handleLoading.value = false
  }
}

onMounted(() => {
  setDesc('SLA 风险预警与自动处置动作配置（通知 / 自动建工单）')
  loadRules()
  loadOrders()
  loadOverview()
})
</script>

<style scoped>
.pager { margin-top: 14px; justify-content: flex-end; }
.ra-empty { padding: 18px 0; text-align: center; color: #909399; font-size: 13px; }
.ra-opt { margin-bottom: 2px; }
.ra-opt-tip { margin: 2px 0 6px 26px; font-size: 12px; color: #86909c; }
.ra-note { margin-bottom: 16px; padding: 10px 14px; font-size: 13px; color: #7c2d12; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; line-height: 1.7; }
.ra-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.ra-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #f59e0b, #d97706);
}
.ra-head { display: flex; align-items: center; gap: 8px; }
.ra-head .el-icon { color: #d97706; }
.ra-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.ra-tip { font-size: 12px; color: #9ca3af; }
.ra-stats { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 26px; margin-top: 10px; }
.ra-item { font-size: 13px; color: #57606a; }
.ra-item b { font-size: 16px; color: #1d2129; margin-left: 2px; }
.ra-note b { color: #9a3412; }
</style>
