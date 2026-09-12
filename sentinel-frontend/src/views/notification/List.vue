<template>
  <div>
    <div class="nt-banner">
      <div class="nt-head"><el-icon :size="15"><Bell /></el-icon><span class="nt-title">通知中心</span><span class="nt-tip">订单走到关键节点，系统会主动给买家/商家发消息</span></div>
      <div class="nt-stats">
        <div class="nt-stat"><div class="nt-num">{{ stats.todayCount ?? 0 }}</div><div class="nt-label">今日通知</div></div>
        <div class="nt-stat"><div class="nt-num">{{ fmtRate(stats.successRate) }}%</div><div class="nt-label">送达成功率</div></div>
        <div class="nt-stat"><div class="nt-num">{{ stats.totalChannels ?? 0 }}</div><div class="nt-label">在用触达渠道</div></div>
        <div class="nt-stat"><div class="nt-num">{{ stats.activeOrders ?? 0 }}</div><div class="nt-label">近7日触达订单</div></div>
      </div>
    </div>

    <el-card shadow="never" class="nt-card">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 210px" @keyup.enter="reload" @clear="reload" />
        </el-form-item>
        <el-form-item label="触达渠道">
          <el-select v-model="query.channel" placeholder="全部渠道" clearable style="width: 150px" @change="reload">
            <el-option label="App 推送" value="push" />
            <el-option label="短信" value="sms" />
            <el-option label="邮件" value="email" />
            <el-option label="飞书" value="feishu" />
          </el-select>
        </el-form-item>
        <el-form-item label="送达状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="reload">
            <el-option label="已送达" value="SENT" />
            <el-option label="待发送" value="PENDING" />
            <el-option label="发送失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="nt-card">
      <div class="nt-feed" v-loading="loading">
        <div v-if="!rows.length && !loading" class="nt-empty">{{ emptyText }}</div>
        <div v-for="row in rows" :key="row.id" class="nt-item">
          <div class="nt-ico" :class="icoClass(row.status)">
            <el-icon :size="16"><Message /></el-icon>
          </div>
          <div class="nt-main">
            <div class="nt-line1">
              <span class="nt-order">{{ row.order_no }}</span>
              <span class="nt-node">{{ nodeLabel(row.node) }}</span>
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </div>
            <div class="nt-content">{{ row.content }}</div>
            <div class="nt-meta">{{ roleLabel(row.role) }} · {{ channelLabel(row.channel) }} · {{ langLabel(row.language) }} · {{ fmt(row.created_at) }}</div>
          </div>
          <div class="nt-actions">
            <el-button v-if="row.status === 'FAILED'" size="small" link type="warning" :loading="resendingId === row.id" @click="resend(row)">补发</el-button>
            <el-button v-else size="small" link type="primary" @click="openDetail(row)">详情</el-button>
          </div>
        </div>
      </div>
      <el-pagination style="margin-top: 16px; justify-content: flex-end" background layout="total, prev, pager, next"
        :total="total" :page-size="query.perPage" :current-page="query.page" @current-change="onPageChange" />
    </el-card>

    <el-dialog v-model="detailOpen" title="通知详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order_no }}</el-descriptions-item>
          <el-descriptions-item label="接收对象">{{ roleLabel(detail.role) }} · {{ langLabel(detail.language) }} · {{ channelLabel(detail.channel) }}</el-descriptions-item>
          <el-descriptions-item label="触发节点">{{ nodeLabel(detail.node) }}</el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ fmt(detail.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="通知内容">{{ detail.content }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { notifList, notifStats, notifResend } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const detailOpen = ref(false)
const detail = ref(null)
const resendingId = ref(null)
const emptyText = '暂时没有通知记录，订单走到关键节点时，这里会自动出现发给买家/商家的消息'
const query = reactive({ orderNo: '', channel: '', status: '', page: 1, perPage: 10 })
const NODE_LABELS = { DELIVERED: '已签收', IN_TRANSIT: '干线运输', IMPORT_CUSTOMS: '到达分拨', EXPORT_CUSTOMS: '中转分拨', LAST_MILE: '末端派送', CUSTOMS_DELAY: '中转延误', DELIVERY_FAILED: '派送失败', LOST: '丢件', RETURNED: '退回', WAREHOUSE_OUT: '仓库出库', CREATED: '已下单' }
const ROLE_LABELS = { ADMIN: '管理员', OPERATOR: '运营', CUSTOMER_SERVICE: '客服', MERCHANT: '商家', FINANCE: '财务', buyer: '买家', merchant: '商家' }
const CHANNEL_LABELS = { push: 'App 推送', sms: '短信', email: '邮件', feishu: '飞书' }
const LANG_LABELS = { zh: '中文', 'zh-CN': '中文' }
const STATUS_LABELS = { SENT: '已送达', PENDING: '待发送', FAILED: '发送失败' }
const nodeLabel = (v) => NODE_LABELS[v] || v || '-'
const roleLabel = (v) => ROLE_LABELS[v] || v || '-'
const channelLabel = (v) => CHANNEL_LABELS[v] || v || '-'
const langLabel = (v) => LANG_LABELS[v] || v || '-'
const statusLabel = (v) => STATUS_LABELS[v] || v || '-'
const statusType = (s) => (s === 'SENT' ? 'success' : s === 'FAILED' ? 'danger' : 'info')
const icoClass = (s) => (s === 'SENT' ? 'ok' : s === 'FAILED' ? 'bad' : 'pending')
const fmt = (v) => (v ? String(v).slice(0, 19) : '-')
const fmtRate = (v) => (v == null ? '0.0' : Number(v).toFixed(1))

const setDesc = inject('setPageDesc')

async function loadStats() {
  try { stats.value = (await notifStats()) || {} } catch { stats.value = {} }
}

async function load() {
  loading.value = true
  try {
    const data = await notifList({ orderNo: query.orderNo, channel: query.channel, status: query.status, page: query.page, perPage: query.perPage })
    rows.value = data.rows || []
    total.value = data.count || 0
  } finally {
    loading.value = false
  }
}

function reload() { query.page = 1; load() }

function onPageChange(p) { query.page = p; load() }

async function resend(row) {
  await ElMessageBox.confirm(`确认给订单 ${row.order_no} 重新发一次这条通知？`, '补发通知', { type: 'warning' })
  resendingId.value = row.id
  try {
    await notifResend(row.id)
    ElMessage.success('已重新发送，可回到列表查看最新状态')
    load(); loadStats()
  } finally { resendingId.value = null }
}

async function openDetail(row) {
  detail.value = row
  detailOpen.value = true
}

onMounted(() => {
  setDesc('订单关键节点自动触达买家/商家，送达失败可一键补发')
  load(); loadStats()
})
</script>

<style scoped>
.nt-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.nt-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.nt-head { display: flex; align-items: center; gap: 8px; }
.nt-head .el-icon { color: #0891b2; }
.nt-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.nt-tip { font-size: 12px; color: #9ca3af; }
.nt-stats { display: flex; flex-wrap: wrap; gap: 10px 24px; margin-top: 12px; }
.nt-stat { display: flex; align-items: baseline; gap: 8px; }
.nt-num { font-size: 18px; font-weight: 700; color: #0891b2; }
.nt-label { font-size: 13px; color: #86909c; }
.nt-card { margin-bottom: 16px; }
.nt-empty { padding: 46px 0; text-align: center; color: #aab4bd; font-size: 13px; }
.nt-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px solid #f0f2f5;
}
.nt-item:last-child { border-bottom: none; }
.nt-ico {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.nt-ico.ok { background: #ecfdf5; color: #10b981; }
.nt-ico.pending { background: #eef8fa; color: #0891b2; }
.nt-ico.bad { background: #fef0f0; color: #f56c6c; }
.nt-main { flex: 1; min-width: 0; }
.nt-line1 { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.nt-order { font-weight: 600; color: #1d2129; }
.nt-node { font-size: 12px; color: #0e7490; background: #eef8fa; border-radius: 4px; padding: 1px 8px; }
.nt-content { margin-top: 6px; color: #4e5969; font-size: 13px; line-height: 1.7; word-break: break-word; }
.nt-meta { margin-top: 6px; font-size: 12px; color: #9aa3ad; }
.nt-actions { flex-shrink: 0; padding-top: 6px; }
</style>