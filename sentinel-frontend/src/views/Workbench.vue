<template>
  <div>
    <!-- 1. 业务统计指标卡片：点击跳转对应业务页 -->
    <el-row :gutter="16">
      <el-col v-for="s in statCards" :key="s.key" :span="statSpan">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '18px' }" @click="go(s.to)">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
              <el-icon :size="20"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-label">{{ s.label }}</div>
              <div class="stat-value" :style="{ color: s.color }">{{ values[s.key] }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 待办任务 + 3. 快捷操作 -->
    <el-row :gutter="16" class="block">
      <el-col :span="14">
        <el-card shadow="never" v-if="todos.length">
          <template #header>待办任务</template>
          <div v-for="t in todos" :key="t.label" class="todo-row">
            <div class="todo-info">
              <el-icon :size="16" :style="{ color: t.color }"><component :is="t.icon" /></el-icon>
              <span class="todo-label">{{ t.label }}</span>
              <span class="todo-count" :style="{ background: t.bg, color: t.color }">{{ values[t.key] }}</span>
            </div>
            <div class="todo-actions">
              <el-button size="small" type="primary" @click="go(t.to)">处理</el-button>
              <el-button size="small" link type="primary" @click="go(t.to)">查看</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" v-if="quicks.length">
          <template #header>快捷操作</template>
          <div class="quick">
            <div v-for="q in quicks" :key="q.label" class="quick-item" @click="go(q.to)">
              <el-icon :size="18" class="quick-icon"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4. 业务数据：最近订单/账单 + 异常运单监控（订单卡片形式，展示重要信息） -->
    <el-row :gutter="16" class="block">
      <el-col :span="12">
        <el-card shadow="never" v-loading="bizLoading">
          <template #header>
            <div class="card-head">
              <span>{{ bizTitle }}</span>
              <el-button link type="primary" size="small" @click="go(bizMoreTo)">更多</el-button>
            </div>
          </template>
          <!-- 最近订单：卡片展示重要信息，点卡片进详情 -->
          <template v-if="!isFin">
            <div v-if="!recentOrders.length" class="empty-mini">暂无最近订单</div>
            <div v-for="o in recentOrders" :key="o.order_no" class="order-card" @click="goOrder(o)">
              <div class="oc-head">
                <router-link :to="'/tms/order/' + o.order_no" class="oc-no" @click.stop>{{ o.order_no }}</router-link>
                <StatusTag :status="o.current_node" />
              </div>
              <div class="oc-meta">
                <span class="oc-merchant">{{ o.merchant_name }}</span>
                <span>{{ countryLabel(o.destination_country) }}</span>
                <span class="oc-fee">¥{{ Number(o.freight_cost || 0).toFixed(2) }}</span>
                <span class="oc-time">{{ timeOf(o.created_at) }}</span>
              </div>
            </div>
          </template>
          <!-- 最近账单（财务）：账单卡片 -->
          <template v-else>
            <div v-if="!recentBills.length" class="empty-mini">暂无账单</div>
            <div v-for="b in recentBills" :key="b.id" class="order-card">
              <div class="oc-head">
                <span class="oc-no">{{ b.bill_no }}</span>
                <el-tag size="small">{{ BILL_LABEL[b.status] || b.status }}</el-tag>
              </div>
              <div class="oc-meta">
                <span class="oc-merchant">{{ b.carrier_name }}</span>
                <span class="oc-fee">¥{{ Number(b.total_amount || 0).toFixed(2) }}</span>
                <span class="oc-time">{{ timeOf(b.created_at) }}</span>
              </div>
            </div>
          </template>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" v-loading="bizLoading">
          <template #header>
            <div class="card-head">
              <span>异常运单监控</span>
              <el-button link type="primary" size="small" @click="go('/tms/risk-alert')">更多</el-button>
            </div>
          </template>
          <div v-if="!anomalyOrders.length" class="empty-mini">暂无异常运单</div>
          <div v-for="o in anomalyOrders" :key="o.order_no" class="order-card" @click="goOrder(o)">
            <div class="oc-head">
              <router-link :to="'/tms/order/' + o.order_no" class="oc-no" @click.stop>{{ o.order_no }}</router-link>
              <StatusTag :status="o.current_node" />
              <el-tag v-if="o.sla_status === 'BREACHED'" size="small" type="danger">SLA违约</el-tag>
              <el-tag v-else-if="o.sla_status === 'RISK'" size="small" type="warning">SLA预警</el-tag>
            </div>
            <div class="oc-meta">
              <span class="oc-merchant">{{ o.merchant_name }}</span>
              <span>{{ countryLabel(o.destination_country) }}</span>
              <span class="oc-time">{{ timeOf(o.created_at) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 5. 消息通知 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-head">
          <span>消息通知</span>
          <el-button link type="primary" size="small" @click="go('/notification/list')">更多</el-button>
        </div>
      </template>
      <div v-if="!notifs.length" class="notif-empty">暂无消息</div>
      <div v-for="n in notifs" :key="n.id" class="notif-row" :class="{ unread: isUnread(n) }" @click="openNotif(n)">
        <span class="notif-dot" :class="{ on: isUnread(n) }"></span>
        <span class="notif-tag" :style="{ color: notifColor(n), background: notifBg(n) }">{{ notifTag(n) }}</span>
        <div class="notif-body">
          <div class="notif-title">{{ notifTitle(n) }}</div>
          <div class="notif-content">{{ notifContent(n) }}</div>
        </div>
        <div class="notif-meta">
          <span class="notif-time">{{ relTime(n.created_at) }}</span>
          <span v-if="isUnread(n)" class="notif-unread">未读</span>
          <span v-else class="notif-read">已读</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { tmsOverview, tmsOrderList, orderReviewList, billList, riskOrders, notifList } from '../api'
import { ROLES } from '../utils/menu'
import StatusTag from '../components/StatusTag.vue'
import { countryLabel } from '../utils/country'

const router = useRouter()
const authStore = useAuthStore()
const setActions = inject('setPageActions')

// —— 角色控制（6. 按登录用户角色动态渲染模块）——
const role = computed(() => authStore.role)
const isOps = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const isFin = computed(() => role.value === ROLES.FINANCE)
const isMch = computed(() => role.value === ROLES.MERCHANT)
const isCS = computed(() => role.value === ROLES.CUSTOMER_SERVICE)

const values = reactive({ today: 0, pending: 0, review: 0, inTransit: 0, anomaly: 0, bills: 0, workorders: 0 })

// 统计卡片：点击跳转对应业务页
const statDefs = [
  { key: 'today', label: '今日订单', icon: 'Document', to: '/tms/order', color: '#0891b2', bg: 'rgba(8,145,178,.10)', roles: ['ADMIN', 'OPERATOR', 'MERCHANT', 'CUSTOMER_SERVICE', 'FINANCE'] },
  { key: 'pending', label: '待处理订单', icon: 'Tickets', to: '/tms/order?node=CREATED', color: '#0891b2', bg: 'rgba(8,145,178,.10)', roles: ['ADMIN', 'OPERATOR', 'MERCHANT'] },
  { key: 'review', label: '待审核订单', icon: 'Stamp', to: '/tms/order-review', color: '#f59e0b', bg: 'rgba(245,158,11,.12)', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE'] },
  { key: 'inTransit', label: '在途运单', icon: 'Van', to: '/tms/order?node=IN_TRANSIT', color: '#0891b2', bg: 'rgba(8,145,178,.10)', roles: ['ADMIN', 'OPERATOR', 'MERCHANT'] },
  { key: 'anomaly', label: '异常运单', icon: 'Warning', to: '/workorder', color: '#ef4444', bg: 'rgba(239,68,68,.10)', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE', 'FINANCE'] },
  { key: 'bills', label: '待结算单据', icon: 'Money', to: '/tms/bill', color: '#8b5cf6', bg: 'rgba(139,92,246,.10)', roles: ['ADMIN', 'OPERATOR', 'FINANCE'] }
]
const statCards = computed(() => statDefs.filter((s) => s.roles.includes(role.value)))
const statSpan = computed(() => Math.max(4, Math.floor(24 / statCards.value.length)))

// 待办任务：数量 + 处理/查看入口
const todoDefs = [
  { key: 'review', label: '待审核订单', icon: 'Stamp', to: '/tms/order-review', color: '#f59e0b', bg: 'rgba(245,158,11,.12)', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE'] },
  { key: 'pending', label: '待出库任务', icon: 'Tickets', to: '/tms/order?node=CREATED', color: '#0891b2', bg: 'rgba(8,145,178,.10)', roles: ['ADMIN', 'OPERATOR', 'MERCHANT'] },
  { key: 'anomaly', label: '运单异常', icon: 'Warning', to: '/workorder', color: '#ef4444', bg: 'rgba(239,68,68,.10)', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE', 'MERCHANT'] },
  { key: 'workorders', label: '售后工单', icon: 'Service', to: '/tms/after-sale', color: '#8b5cf6', bg: 'rgba(139,92,246,.10)', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE', 'MERCHANT'] },
  { key: 'bills', label: '待结算处理', icon: 'Money', to: '/tms/bill', color: '#8b5cf6', bg: 'rgba(139,92,246,.10)', roles: ['FINANCE'] }
]
const todos = computed(() => todoDefs.filter((t) => t.roles.includes(role.value)))

// 快捷操作：高频功能按钮，完整操作跳转对应菜单页
const quickDefs = [
  { label: '新建订单', icon: 'EditPen', to: '/tms/fulfillment', roles: ['ADMIN', 'OPERATOR', 'MERCHANT'] },
  { label: '新建运单', icon: 'Van', to: '/tms/waybill', roles: ['ADMIN', 'OPERATOR'] },
  { label: '批量出库', icon: 'TakeawayBox', to: '/tms/order', roles: ['ADMIN', 'OPERATOR'] },
  { label: '异常处理', icon: 'WarningFilled', to: '/workorder', roles: ['ADMIN', 'OPERATOR', 'CUSTOMER_SERVICE'] },
  { label: '生成账单', icon: 'Money', to: '/tms/bill', roles: ['FINANCE'] },
  { label: '差异对账', icon: 'DataAnalysis', to: '/tms/reconcile', roles: ['FINANCE'] }
]
const quicks = computed(() => quickDefs.filter((q) => q.roles.includes(role.value)))

// 业务数据：最近订单/账单 + 异常运单监控（仅少量关键条目）
const recentOrders = ref([])
const recentBills = ref([])
const anomalyOrders = ref([])
const notifs = ref([])
const bizLoading = ref(false)

const bizTitle = computed(() => (isFin.value ? '最近账单' : '最近订单'))
const bizMoreTo = computed(() => (isFin.value ? '/tms/bill' : '/tms/order'))

const BILL_LABEL = { DRAFT: '待提交', SUBMITTED: '待审核', VERIFIED: '待结算', SETTLED: '已结算', REJECTED: '已驳回', CLOSED: '已关闭' }

function go(to) { if (to) router.push(to) }
function goOrder(o) { if (o.order_no) router.push(`/tms/order/${o.order_no}`) }
function timeOf(v) { return v ? String(v).replace('T', ' ').slice(5, 16) : '' }

// —— 消息通知：人文化展示 + 未读/已读 + 相对时间 ——
const READ_KEY = 'sentinel_read_notifs'
function readSet() {
  try { return new Set(JSON.parse(localStorage.getItem(READ_KEY) || '[]')) } catch { return new Set() }
}
function isUnread(n) { return !readSet().has(n.id) }

// 节点 → 人文化标题
const NODE_MSG = {
  CREATED: '订单已创建', WAREHOUSE_OUT: '包裹已出库', DOMESTIC_PICKED: '包裹已揽收',
  EXPORT_CUSTOMS: '已进入中转分拨', IN_TRANSIT: '正在干线运输', IMPORT_CUSTOMS: '已到达分拨',
  LAST_MILE: '正在末端派送', DELIVERED: '包裹已签收', CUSTOMS_DELAY: '中转延误提醒',
  DELIVERY_FAILED: '派送失败提醒', LOST: '包裹丢失', RETURNED: '包裹已退回'
}
function notifTitle(n) { return NODE_MSG[n.node] || '物流动态' }
function notifContent(n) {
  const base = n.content || NODE_MSG[n.node] || '物流动态'
  return n.order_no ? `${base}（订单 ${n.order_no}）` : base
}
// 相对时间：刚刚 / N 分钟前 / N 小时前 / N 天前，超过 7 天显示日期
function relTime(v) {
  if (!v) return ''
  const t = new Date(String(v).replace(/-/g, '/').replace('T', ' '))
  const diff = Date.now() - t.getTime()
  if (isNaN(diff)) return String(v).replace('T', ' ').slice(5, 16)
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return String(v).replace('T', ' ').slice(5, 16)
}
function notifTag(n) {
  if (!n.node || n.node === 'SYSTEM' || n.node === 'ANNOUNCE') return '系统公告'
  if (['SENT', 'FAILED'].includes(n.status)) return '业务提醒'
  return '运单变更'
}
function openNotif(n) {
  const s = readSet()
  s.add(n.id)
  localStorage.setItem(READ_KEY, JSON.stringify([...s]))
  if (n.order_no) router.push(`/tms/order/${n.order_no}`)
}
function notifColor(n) {
  const t = notifTag(n)
  return t === '系统公告' ? '#8b5cf6' : t === '业务提醒' ? '#f59e0b' : '#0891b2'
}
function notifBg(n) {
  const t = notifTag(n)
  return t === '系统公告' ? 'rgba(139,92,246,.10)' : t === '业务提醒' ? 'rgba(245,158,11,.12)' : 'rgba(8,145,178,.10)'
}

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function load() {
  loadStats()
  loadBiz()
  loadNotifs()
}

async function loadStats() {
  if (isOps.value || isFin.value) {
    try {
      const t = await tmsOverview()
      values.inTransit = t.inTransit ?? 0
      values.anomaly = t.anomaly ?? 0
      values.workorders = t.openWorkorders ?? 0
    } catch (e) { /* 无权限忽略 */ }
  }
  try { values.today = (await tmsOrderList({ page: 1, perPage: 1, startDate: todayStr(), endDate: todayStr() })).count || 0 } catch (e) { /* 忽略 */ }
  try { values.pending = (await tmsOrderList({ page: 1, perPage: 1, node: 'CREATED' })).count || 0 } catch (e) { /* 忽略 */ }
  if (isOps.value) {
    try { values.review = (await orderReviewList({ page: 1, perPage: 1, status: 'PENDING' })).count || 0 } catch (e) { /* 忽略 */ }
  }
  if (role.value === ROLES.ADMIN || isFin.value) {
    try {
      const [d, s] = await Promise.all([
        billList({ status: 'DRAFT', page: 1, perPage: 1 }),
        billList({ status: 'SUBMITTED', page: 1, perPage: 1 })
      ])
      values.bills = (d.count || 0) + (s.count || 0)
    } catch (e) { /* 忽略 */ }
  }
}

async function loadBiz() {
  bizLoading.value = true
  try {
    if (isFin.value) {
      recentBills.value = (await billList({ page: 1, perPage: 5 })).rows || []
    } else {
      recentOrders.value = (await tmsOrderList({ page: 1, perPage: 5 })).rows || []
    }
    anomalyOrders.value = isMch.value ? [] : ((await riskOrders({ page: 1, perPage: 5 })).rows || [])
  } catch (e) { /* 忽略 */ } finally { bizLoading.value = false }
}

async function loadNotifs() {
  try { notifs.value = (await notifList({ page: 1, perPage: 6 })).rows || [] } catch (e) { /* 忽略 */ }
}

onMounted(() => {
  setActions([{ text: '刷新', type: 'primary', onClick: load }])
  load()
})
</script>

<style scoped>
.block {
  margin-top: 16px;
}

/* 同一行内模块等高对齐：卡片拉伸填满列高，左右功能模块底部对齐 */
.el-row :deep(.el-col) {
  display: flex;
}
.el-row :deep(.el-card) {
  flex: 1;
  width: 100%;
}

/* 模块头部：左标题右「更多」 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(29, 33, 41, 0.06);
}
.stat-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}
.stat-text {
  min-width: 0;
}
.stat-label {
  font-size: 12px;
  color: #86909c;
  white-space: nowrap;
}
.stat-value {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

/* 待办任务 */
.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 2px;
  border-bottom: 1px solid #f1f3f6;
}
.todo-row:last-child {
  border-bottom: none;
}
.todo-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.todo-label {
  font-size: 14px;
  color: #1d2129;
}
.todo-count {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 10px;
}
.todo-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 快捷操作 */
.quick {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.quick-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  cursor: pointer;
  color: #4e5969;
  font-size: 13px;
  transition: border-color 0.15s, color 0.15s, background 0.15s;
}
.quick-item:hover {
  border-color: #0891b2;
  color: #0891b2;
  background: #f0f9fb;
}
.quick-icon {
  color: #0891b2;
}

.link {
  color: #0891b2;
  text-decoration: none;
  white-space: normal;
  word-break: break-all;
}
.link:hover {
  text-decoration: underline;
}

/* 最近订单 / 异常运单：订单卡片形式 */
.order-card {
  padding: 10px 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.order-card:hover {
  border-color: #0891b2;
  box-shadow: 0 2px 8px rgba(8, 145, 178, 0.08);
}
.oc-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.oc-no {
  color: #0891b2;
  font-weight: 600;
  font-size: 13px;
  text-decoration: none;
  word-break: break-all;
  min-width: 0;
}
.oc-no:hover {
  text-decoration: underline;
}
.oc-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: #86909c;
}
.oc-merchant {
  color: #4e5969;
}
.oc-fee {
  color: #1d2129;
  font-weight: 600;
}
.oc-time {
  margin-left: auto;
  flex: none;
}
.empty-mini {
  padding: 24px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

/* 消息通知：人文化 + 未读/已读 + 相对时间 */
.notif-empty {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px 0;
}
.notif-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 6px;
  border-bottom: 1px solid #f1f3f6;
  cursor: pointer;
}
.notif-row:hover {
  background: #f7f9fa;
}
.notif-row.unread {
  background: #f4fbfc;
}
.notif-row.unread:hover {
  background: #eaf7f9;
}
/* 未读小圆点 */
.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e5ea;
  margin-top: 6px;
  flex: none;
}
.notif-dot.on {
  background: #0891b2;
}
.notif-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  flex: none;
  margin-top: 2px;
}
.notif-body {
  flex: 1;
  min-width: 0;
}
.notif-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}
.notif-content {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notif-meta {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}
.notif-time {
  font-size: 12px;
  color: #c0c4cc;
}
.notif-unread {
  font-size: 11px;
  font-weight: 600;
  color: #0891b2;
}
.notif-read {
  font-size: 11px;
  color: #c0c4cc;
}
</style>
