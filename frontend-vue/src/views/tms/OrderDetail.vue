<template>
  <div v-if="order">
    <!-- 状态头卡：订单号 + 状态标签 + 主操作 / 更多▾ / 危险操作 -->
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="head-row">
          <div class="head-left">
            <b>订单 {{ order.order_no }}</b>
            <StatusTag :status="order.current_node" class="tag" />
            <StatusTag :status="order.sla_status" class="tag" />
            <el-tag :type="reviewTag.type" size="small" class="tag">{{ reviewTag.label }}</el-tag>
          </div>
          <div class="op-bar">
            <!-- 主操作：当前状态唯一的 key action -->
            <el-button v-if="isPendingOutbound" type="primary" size="small" :loading="acting" @click="generate">
              <el-icon><Promotion /></el-icon>确认发货
            </el-button>
            <el-button v-else-if="order.waybill_no && !terminal" type="primary" size="small" :loading="acting" @click="advance">
              <el-icon><Van /></el-icon>推进物流状态
            </el-button>
            <!-- 次级操作收进「更多」 -->
            <el-dropdown @command="onMoreCommand">
              <el-button size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="predictEta"><el-icon><Clock /></el-icon>预计送达时间</el-dropdown-item>
                  <el-dropdown-item command="notify"><el-icon><Bell /></el-icon>再次通知收件人</el-dropdown-item>
                  <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑信息</el-dropdown-item>
                  <el-dropdown-item command="workorders"><el-icon><Tickets /></el-icon>相关工单（{{ workorderCount }}）</el-dropdown-item>
                  <el-dropdown-item command="afterSale" divided><el-icon><RefreshLeft /></el-icon>发起售后（退货/换货）</el-dropdown-item>
                  <el-dropdown-item command="claim"><el-icon><Wallet /></el-icon>登记理赔</el-dropdown-item>
                  <el-dropdown-item command="bill" divided><el-icon><Money /></el-icon>去结算</el-dropdown-item>
                  <el-dropdown-item v-if="order.waybill_no && !terminal" command="anomaly" divided><el-icon><Warning /></el-icon>模拟异常</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 危险操作：右对齐、独立可见 -->
            <el-button v-if="isPendingOutbound" type="danger" size="small" plain @click="cancelOrder"><el-icon><CircleClose /></el-icon>取消订单</el-button>
            <el-button v-if="isOutbound" type="warning" size="small" plain @click="requestReturn"><el-icon><RefreshLeft /></el-icon>申请退回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="商家">{{ order.merchant_name }}</el-descriptions-item>
        <el-descriptions-item label="目的地">{{ countryLabel(order.destination_country) }}</el-descriptions-item>
        <el-descriptions-item label="物流渠道">{{ channelName(order.channel_id) }}</el-descriptions-item>
        <el-descriptions-item label="收件人编号">{{ order.buyer_id }}</el-descriptions-item>
        <el-descriptions-item label="收件人语言">{{ langLabel(order.buyer_language) }}</el-descriptions-item>
        <el-descriptions-item label="收件人电话">{{ order.buyer_phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ order.buyer_city || '' }} {{ order.buyer_address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮编">{{ order.buyer_postal || '-' }}</el-descriptions-item>
        <el-descriptions-item label="货值">¥{{ Number(order.declared_value).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ Number(order.freight_cost).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="预计送达时间">{{ fmt(order.promise_eta) }}</el-descriptions-item>
        <el-descriptions-item label="运单号">
          <router-link v-if="order.waybill_no" :to="'/tms/waybill/' + order.waybill_no" style="color: #0891b2; text-decoration: none">
            {{ order.waybill_no }} →
          </router-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert v-if="etaResult" type="success" :closable="false" style="margin-top: 12px">
        <template #title>
          预计剩余 <b>{{ etaResult.remainingDays ?? '-' }}</b> 天送达（承诺 {{ etaResult.transitDaysMax ?? '-' }} 天）
        </template>
        {{ etaResult.reason || etaResult.note || '' }}
      </el-alert>
    </el-card>

    <!-- 节点进度条：让用户能"看见"订单走到哪一站 -->
    <el-card shadow="never" class="mb">
      <el-alert v-if="isCanceled" type="info" :closable="false" title="订单已取消，流程终止" description="如需重新发货，请新建订单。" />
      <template v-else>
        <el-steps :active="nodeIndex" :process-status="nodeProcessStatus" align-center>
          <el-step v-for="s in NODE_STEPS" :key="s.key" :title="s.label" />
        </el-steps>
        <el-alert v-if="isAnomalyNode" type="error" :closable="false" style="margin-top: 12px" title="订单当前处于异常状态"
          :description="`${NODE_LABEL[order.current_node] || order.current_node} — 已自动生成售后工单跟进，可在「异常售后」查看。`" />
      </template>
    </el-card>

    <!-- 主体：tabs 分区，避免超长页 -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="概览" name="overview">
        <el-card shadow="never" class="mb">
          <template #header><b>商品明细</b></template>
          <el-table :data="items" border stripe size="small">
            <el-table-column label="图片" width="76">
              <template #default="{ row }">
                <el-image
                  v-if="imgMap[row.sku] || productImage(row.sku)"
                  :src="imgMap[row.sku] || productImage(row.sku)"
                  fit="cover"
                  style="width: 44px; height: 44px; border-radius: 6px; vertical-align: middle"
                  :preview-src-list="[imgMap[row.sku] || productImage(row.sku)]"
                  preview-teleported
                />
                <span v-else style="color: #c0c4cc; font-size: 12px">-</span>
              </template>
            </el-table-column>
            <el-table-column align="left" prop="sku" label="商品编码" width="140" />
            <el-table-column prop="qty" label="数量" width="80" />
            <el-table-column label="单件重量" width="110">
              <template #default="{ row }">{{ row.unit_weight_kg }} kg</template>
            </el-table-column>
            <el-table-column label="单件价格" width="110">
              <template #default="{ row }">¥{{ Number(row.unit_declared_value).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="mb">
          <template #header><b>审核记录</b><span class="tag">审核放行后才可出库</span></template>
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="审核状态">{{ reviewTag.label }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ order.reviewed_by || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ fmt(order.reviewed_at) }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因">{{ order.reject_reason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="mb">
          <template #header><b>业务备注</b><span class="tag">跨角色协同批注，出库后也可补充</span></template>
          <div style="display:flex;align-items:center;gap:12px">
            <span v-if="order.business_notes" style="flex:1;color:#1f2328">{{ order.business_notes }}</span>
            <span v-else style="color:#9ca3af">暂无备注，可在「更多 → 编辑信息」中补充。</span>
            <el-button size="small" link type="primary" @click="openEdit">编辑备注</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="物流轨迹" name="track">
        <el-card shadow="never" class="mb">
          <template #header>
            <b>物流地图</b><span class="tag">包裹当前位置：{{ mapCurrentLabel }}</span>
          </template>
          <div class="addr-strip">
            <div class="addr-item">
              <div>
                <div class="addr-title">发货仓</div>
                <div class="addr-text">{{ mapOriginAddr || '深圳仓' }}</div>
              </div>
            </div>
            <div class="addr-arrow">→</div>
            <div class="addr-item">
              <div>
                <div class="addr-title">收件人</div>
                <div class="addr-text">{{ mapDestAddr }}</div>
              </div>
            </div>
          </div>
          <BaseMap :origin="mapOrigin" :dest="mapDest" :current="mapCurrent" :origin-addr="mapOriginAddr" :dest-addr="mapDestAddr" height="300" />
        </el-card>

        <el-card shadow="never">
          <template #header><b>物流轨迹</b></template>
          <NodeTimeline :tracks="tracks" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="发货记录" name="outbound">
        <el-card shadow="never" class="mb">
          <template #header><b>发货记录</b><span class="tag">分批发货时每个批次一条运单</span></template>
          <el-table v-if="waybills.length" :data="waybills" border size="small">
            <el-table-column align="left" label="运单号" min-width="180">
              <template #default="{ row }">
                <router-link :to="'/tms/waybill/' + row.waybill_no" style="color:#0891b2;text-decoration:none">{{ row.waybill_no }} →</router-link>
              </template>
            </el-table-column>
            <el-table-column prop="tracking_no" label="追踪号" min-width="170" />
            <el-table-column prop="carrier_code" label="承运商" width="110" />
            <el-table-column label="计费重" width="90">
              <template #default="{ row }">{{ row.billable_weight_kg ?? '-' }} kg</template>
            </el-table-column>
            <el-table-column label="运费" width="100">
              <template #default="{ row }">¥{{ Number(row.freight_cost || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column label="出库时间" width="160">
              <template #default="{ row }">{{ fmtWB(row.created_at) }}</template>
            </el-table-column>
          </el-table>
          <span v-else style="color:#9ca3af">尚未发货，可在订单管理「确认发货 / 分批发货」生成运单。</span>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="`异常售后${workorderCount ? '（' + workorderCount + '）' : ''}`" name="after">
        <el-card shadow="never" class="mb">
          <template #header><b>异常记录</b><span class="tag">异常会触发诊断并生成售后工单</span></template>
          <el-table v-if="anomalyLogs.length" :data="anomalyLogs" border size="small">
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ fmtWB(row.created_at) }}</template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="110" />
            <el-table-column prop="detail" label="异常详情" min-width="260" />
          </el-table>
          <span v-else style="color:#9ca3af">一路顺畅，暂无异常</span>
        </el-card>

        <el-card shadow="never" class="mb">
          <template #header>
            <b>关联售后工单</b>
            <el-badge v-if="workorderCount" :value="workorderCount" style="margin-left:6px" />
          </template>
          <el-table v-if="workorders.length" :data="workorders" border size="small">
            <el-table-column align="center" prop="id" label="工单ID" width="80" />
            <el-table-column prop="type" label="类型" width="110" />
            <el-table-column label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="row.level === 'P0' ? 'danger' : row.level === 'P1' ? 'warning' : 'info'" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }"><el-button size="small" link type="primary" @click="goWorkorder(row.id)">查看</el-button></template>
            </el-table-column>
          </el-table>
          <span v-else style="color:#9ca3af">暂无售后工单。</span>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="audit">
        <el-card shadow="never" class="mb">
          <template #header><b>操作日志</b><span class="tag">全程留痕，不可篡改</span></template>
          <el-table :data="audits" border size="small">
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ fmtWB(row.created_at) }}</template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="110" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">{{ roleLabel(row.operator_role) }}</template>
            </el-table-column>
            <el-table-column label="模块" width="90">
              <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
            </el-table-column>
            <el-table-column label="动作" width="90">
              <template #default="{ row }">{{ actionLabel(row.action) }}</template>
            </el-table-column>
            <el-table-column prop="detail" label="操作详情" min-width="260" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 模拟异常弹窗 -->
    <el-dialog v-model="anomalyDialog" title="模拟异常" width="440px">
      <p style="margin-top:0;color:#606266">选择异常类型：将触发异常诊断并生成售后工单，客服会跟进处理。</p>
      <div class="anomaly-options">
        <el-button v-for="(label, t) in ANOMALY_TYPES" :key="t" type="danger" plain @click="injectAnomaly(t)">{{ label }}</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑订单信息" width="500px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="收件人电话"><el-input v-model="editForm.buyerPhone" :disabled="isOutbound" /></el-form-item>
        <el-form-item label="收件人语言">
          <el-select v-model="editForm.buyerLanguage" :disabled="isOutbound" style="width:100%">
            <el-option label="简体中文" value="zh" />
          </el-select>
        </el-form-item>
        <el-form-item label="收货地址"><el-input v-model="editForm.buyerAddress" :disabled="isOutbound" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.buyerCity" :disabled="isOutbound" /></el-form-item>
        <el-form-item label="邮编"><el-input v-model="editForm.buyerPostal" :disabled="isOutbound" /></el-form-item>
        <el-form-item label="业务备注">
          <el-input v-model="editForm.businessNotes" type="textarea" :rows="3" placeholder="跨角色协同批注，出库后仍可补充" />
        </el-form-item>
        <el-alert v-if="isOutbound" type="warning" :closable="false" title="订单已出库，收货信息已锁定"
          description="如需修改收件人 / 地址请走「申请退回」流程；此处仅可补充业务备注。" />
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import NodeTimeline from '../../components/NodeTimeline.vue'
import BaseMap from '../../components/BaseMap.vue'
import { tmsOrderDetail, tmsOrderTracks, waybillGenerate, tmsAdvance, tmsInjectAnomaly, etaPredict,
  tmsUpdateOrder, tmsCancelOrder, notifSend, woList, warehouseAll, productList,
  waybillList, auditList, afterSaleRegister, tmsChannelAll } from '../../api'
import { fmtDateTime } from '../../utils/format'
import { ORIGIN, WAREHOUSE_COORDS, destCoord, nodeProgress, routePoint } from '../../utils/geo'
import { countryLabel } from '../../utils/country'
import { productImage } from '../../utils/productImage'
import { useAuthStore } from '../../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const orderNo = route.params.orderNo
const order = ref(null)
const tracks = ref([])
const acting = ref(false)
const etaLoading = ref(false)
const etaResult = ref(null)
const notifying = ref(false)
const workorderCount = ref(0)
const editDialog = ref(false)
const saving = ref(false)
const editForm = reactive({})
const activeTab = ref('overview')
const anomalyDialog = ref(false)

// 详情页分组数据：出库记录 / 操作日志 / 关联售后工单
const waybills = ref([])
const audits = ref([])
const workorders = ref([])

const canOps = computed(() => ['ADMIN', 'OPERATOR'].includes(auth.role))
const isPendingOutbound = computed(() => order.value && order.value.current_node === 'CREATED' && !order.value.waybill_no)
const isOutbound = computed(() => !!order.value?.waybill_no && order.value?.current_node !== 'RETURNED')
const anomalyLogs = computed(() => audits.value.filter((a) => a.action === 'ANOMALY'))

// ---------- 节点进度条：主线 + 节点位置映射 ----------
const NODE_STEPS = [
  { key: 'CREATED', label: '已下单' },
  { key: 'OUT', label: '出库' },
  { key: 'PICKED', label: '揽收' },
  { key: 'TRANSIT', label: '干线运输' },
  { key: 'LASTMILE', label: '末端派送' },
  { key: 'DELIVERED', label: '已签收' }
]
const NODE_MAP = {
  CREATED: 0, WAREHOUSE_OUT: 1, DOMESTIC_PICKED: 2,
  EXPORT_CUSTOMS: 3, IN_TRANSIT: 3, IMPORT_CUSTOMS: 4, LAST_MILE: 4, DELIVERED: 5,
  CUSTOMS_DELAY: 3, DELIVERY_FAILED: 4, LOST: 3, RETURNED: 3
}
const NODE_LABEL = {
  CREATED: '已下单', WAREHOUSE_OUT: '仓库出库', DOMESTIC_PICKED: '揽收', EXPORT_CUSTOMS: '中转分拨',
  IN_TRANSIT: '干线运输', IMPORT_CUSTOMS: '到达分拨', LAST_MILE: '末端派送', DELIVERED: '已签收',
  CUSTOMS_DELAY: '中转延误', DELIVERY_FAILED: '派送失败', LOST: '丢件', RETURNED: '退回', CANCELED: '已取消'
}
const nodeIndex = computed(() => (order.value ? (NODE_MAP[order.value.current_node] ?? 0) : 0))
const isAnomalyNode = computed(() => order.value && ['CUSTOMS_DELAY', 'DELIVERY_FAILED', 'LOST', 'RETURNED'].includes(order.value.current_node))
const isCanceled = computed(() => order.value?.current_node === 'CANCELED')
const nodeProcessStatus = computed(() => (isAnomalyNode.value ? 'error' : 'process'))
const ANOMALY_TYPES = { customs_delay: '中转延误', delivery_failed: '派送失败', lost: '丢件', returned: '退回' }

// ---------- 更多操作 ----------
function onMoreCommand(cmd) {
  if (cmd === 'predictEta') predictEta()
  else if (cmd === 'notify') notifyBuyer()
  else if (cmd === 'edit') openEdit()
  else if (cmd === 'workorders') goWorkorders()
  else if (cmd === 'afterSale') goAfterSale()
  else if (cmd === 'claim') goClaim()
  else if (cmd === 'bill') goBill()
  else if (cmd === 'anomaly') anomalyDialog.value = true
}

const reviewMeta = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}
const reviewTag = computed(() => reviewMeta[order.value?.review_status] || { label: order.value?.review_status || '-', type: 'info' })
const MODULE_LABEL = { order: '订单', waybill: '运单', workorder: '工单', bill: '账单', '逆向售后': '售后', sla: '时效', notify: '通知' }
const ACTION_LABEL = {
  CREATE: '创建', GENERATE: '出库', ADVANCE: '推进', ANOMALY: '异常', CANCEL: '取消',
  MERGE: '合并', REVIEW: '审核', REJECT: '驳回', RETURN: '退回', NOTIFY: '通知'
}
const ROLE_LABEL = { ADMIN: '管理员', OPERATOR: '运营', FINANCE: '财务', MERCHANT: '商家', CUSTOMER_SERVICE: '客服' }

const terminal = computed(() => ['DELIVERED', 'LOST', 'RETURNED', 'CANCELED'].includes(order.value?.current_node))

// 物流地图定位：深圳仓 → 收件人城市，当前节点沿路线插值
const warehouses = ref([])
const mapDest = computed(() => {
  const c = destCoord(order.value?.buyer_city, order.value?.destination_country)
  return { ...c, label: order.value?.buyer_city || order.value?.destination_country }
})
const mapOrigin = computed(() => {
  const wh = warehouses.value.find((w) => Number(w.id) === Number(order.value?.warehouse_id))
  const base = (wh && WAREHOUSE_COORDS[wh.warehouse_code]) || ORIGIN
  return { ...base }
})
const mapCurrent = computed(() => {
  const d = mapDest.value
  const o = mapOrigin.value
  return { ...routePoint(o, d, nodeProgress(order.value?.current_node)), label: '包裹当前位置' }
})
const mapCurrentLabel = computed(() => {
  if (!order.value) return '-'
  const n = order.value.current_node
  const progress = Math.round(nodeProgress(n) * 100)
  return `${NODE_LABEL[n] || n}（全程 ${progress}%）`
})
const mapOriginAddr = computed(() => {
  if (!order.value?.warehouse_id) return ''
  const wh = warehouses.value.find((w) => Number(w.id) === Number(order.value.warehouse_id))
  return wh ? `${wh.warehouse_name} · ${wh.city} ${wh.address || ''}` : ''
})
const mapDestAddr = computed(() => {
  if (!order.value) return ''
  return [order.value.buyer_address, order.value.buyer_city, order.value.buyer_postal]
    .filter(Boolean).join(', ')
})
const items = computed(() => {
  try { return JSON.parse(order.value?.items_json || '[]') } catch { return [] }
})
const imgMap = ref({})

async function loadProducts() {
  try {
    const data = await productList({ page: 1, perPage: 100 })
    const m = {}
    for (const p of (data.rows || [])) if (p.sku && (p.image_url || productImage(p.sku))) m[p.sku] = p.image_url || productImage(p.sku)
    imgMap.value = m
  } catch { /* 图片加载失败忽略 */ }
}

onMounted(async () => {
  warehouses.value = (await warehouseAll()) || []
  loadChannels()
  loadProducts()
  await load()
})

// 渠道 / 语言：把裸 ID 或编码转成用户看得懂的名称
const channels = ref([])
async function loadChannels() {
  try { channels.value = (await tmsChannelAll()) || [] } catch { channels.value = [] }
}
function channelName(id) {
  const c = channels.value.find((x) => Number(x.id) === Number(id))
  return c ? c.channel_name : (id ?? '-')
}
function langLabel(l) { return l === 'zh' ? '简体中文' : (l || '-') }

async function load() {
  order.value = await tmsOrderDetail(orderNo)
  tracks.value = (await tmsOrderTracks(orderNo)) || []
  loadExtras()
}

// 加载详情页分组数据：出库记录（含分批）、操作日志、关联售后工单
async function loadExtras() {
  try {
    waybills.value = (await waybillList({ orderNo, page: 1, perPage: 20 })).rows || []
  } catch { waybills.value = [] }
  try {
    audits.value = (await auditList({ targetNo: orderNo, page: 1, perPage: 50 })).rows || []
  } catch { audits.value = [] }
  try {
    const wo = await woList({ orderNo, page: 1, perPage: 20 })
    workorders.value = wo.rows || []
    workorderCount.value = wo.count || 0
  } catch { workorders.value = []; workorderCount.value = 0 }
}

// 取消订单：仅未出库（文案说明后果与退路）
async function cancelOrder() {
  await ElMessageBox.confirm(
    '取消后订单将结束、不可恢复。尚未出库可直接取消；已出库请改走「申请退回」。确定取消吗？',
    '取消订单', { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' })
  try {
    await tmsCancelOrder(orderNo)
    try { await notifSend({ orderNo, node: 'CANCELED', role: 'merchant', channel: 'push' }) } catch { /* 通知失败不影响 */ }
    ElMessage.success('订单已取消，已同步通知商家')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '取消失败')
  }
}

// 申请退回：已出库订单走售后退回流程
async function requestReturn() {
  const { value } = await ElMessageBox.prompt('请填写退回原因，客服会据此跟进处理', '申请退回', {
    inputPlaceholder: '如：收件人拒收 / 地址错误',
    inputValidator: (v) => !!v || '原因不能为空'
  })
  await afterSaleRegister({ orderNo, reason: value, type: 'RETURN' })
  ElMessage.success('已登记退回申请，售后工单已创建，客服将跟进处理')
  await load()
}

// 结算跳转：订单只保存运费，结算在结算模块处理
function goBill() { router.push('/tms/bill') }

function goWorkorders() {
  router.push({ path: '/workorder', query: { orderNo } })
}

function goAfterSale() {
  // 从订单详情发起售后：售后退货页按单筛选并弹出登记（退货/换货）
  router.push({ path: '/tms/after-sale', query: { orderNo } })
}

function goClaim() {
  // 从订单详情登记理赔：理赔流程页带单自动弹出登记，未通过前停留在该页
  router.push({ path: '/tms/claim', query: { orderNo, reg: '1' } })
}

function goWorkorder() { router.push(`/workorder?orderNo=${orderNo}`) }

function moduleLabel(m) { return MODULE_LABEL[m] || m }
function actionLabel(a) { return ACTION_LABEL[a] || a }
function roleLabel(r) { return ROLE_LABEL[r] || r }
function fmtWB(v) { return fmtDateTime(v) }

async function notifyBuyer() {
  notifying.value = true
  try {
    await notifSend({ orderNo, node: order.value?.current_node || 'WAREHOUSE_OUT', role: 'buyer', channel: 'push' })
    ElMessage.success('已重新触发收件人通知')
  } catch (e) {
    ElMessage.error('通知补发失败，请检查通知链路')
  } finally { notifying.value = false }
}

function fmt(v) { return fmtDateTime(v) }

async function generate() {
  acting.value = true
  try {
    const res = await waybillGenerate(orderNo)
    ElMessage.success(`已发货，运单号 ${res.waybill.waybill_no} 已生成，可打印发货单交给承运商。`)
    await load()
  } finally { acting.value = false }
}

async function advance() {
  acting.value = true
  try {
    const res = await tmsAdvance(orderNo)
    ElMessage.success(`已推进到「${res.description}」`)
    await load()
  } finally { acting.value = false }
}

async function injectAnomaly(type) {
  const label = ANOMALY_TYPES[type]
  await ElMessageBox.confirm(`将模拟「${label}」异常并生成售后工单，客服会跟进处理。确定执行吗？`, '模拟异常', { type: 'warning' })
  const res = await tmsInjectAnomaly(orderNo, type)
  anomalyDialog.value = false
  ElMessage.success(`已模拟异常：${res.description}，工单将自动生成`)
  await load()
}

async function predictEta() {
  etaLoading.value = true
  try { etaResult.value = await etaPredict({ orderNo }) } finally { etaLoading.value = false }
}

function openEdit() {
  Object.assign(editForm, {
    buyerPhone: order.value.buyer_phone || '', buyerLanguage: order.value.buyer_language || 'zh',
    buyerAddress: order.value.buyer_address || '', buyerCity: order.value.buyer_city || '', buyerPostal: order.value.buyer_postal || '',
    businessNotes: order.value.business_notes || ''
  })
  editDialog.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    order.value = await tmsUpdateOrder(orderNo, editForm)
    ElMessage.success('订单信息已更新')
    editDialog.value = false
  } finally { saving.value = false }
}
</script>

<style scoped>
.mb { margin-bottom: 16px; }
.tag { margin-left: 8px; }
.head-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.op-bar { display: inline-flex; gap: 6px; align-items: center; margin-left: 12px; }
.anomaly-options { display: flex; flex-wrap: wrap; gap: 8px; }
.addr-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
}
.addr-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.addr-title { font-size: 12px; color: #9ca3af; }
.addr-text { font-size: 13px; font-weight: 500; color: #1f2937; line-height: 1.4; }
.addr-arrow { color: #0891b2; font-weight: 600; font-size: 16px; }
.detail-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
