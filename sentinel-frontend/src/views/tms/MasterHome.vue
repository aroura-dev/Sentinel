<template>
  <div>
    <div class="mh-stats">
      <div class="mh-stat"><div class="mh-num">{{ stat.total ?? 0 }}</div><div class="mh-label">资料总量（条）</div></div>
      <div class="mh-stat"><div class="mh-num mh-warn">{{ issues.length }}</div><div class="mh-label">待完善事项</div></div>
      <div class="mh-stat"><div class="mh-num">{{ changes.length }}</div><div class="mh-label">近期变动</div></div>
      <div class="mh-stat"><div class="mh-num mh-sm">{{ stat.recent }}</div><div class="mh-label">最近维护</div></div>
    </div>

    <div class="mh-cols">
      <div class="mh-panel">
        <div class="mh-ph">
          <b>需要你处理</b><span class="mh-count">{{ issues.length }}</span>
        </div>
        <template v-if="loading"><div class="mh-empty">加载中…</div></template>
        <template v-else-if="issues.length">
          <div v-for="(it, i) in issues" :key="i" class="mh-item" @click="go(it.path)">
            <span class="mh-dot" :style="{ background: it.color }"></span>
            <div class="mh-it-body">
              <div class="mh-it-title">{{ it.title }}</div>
              <div class="mh-it-desc">{{ it.desc }}</div>
            </div>
            <span class="mh-tag">建议处理</span>
            <span class="mh-go">处理 ›</span>
          </div>
        </template>
        <div v-else class="mh-empty">资料都比较健康，暂时没有需要处理的项。</div>
      </div>

      <div class="mh-panel">
        <div class="mh-ph"><b>最近变动</b><span class="mh-count">{{ changes.length }}</span></div>
        <template v-if="loading"><div class="mh-empty">加载中…</div></template>
        <template v-else-if="changes.length">
          <div v-for="(c, i) in changes" :key="i" class="mh-feed" @click="go(c.path)">
            <span class="mh-fdot"></span>
            <div class="mh-it-body">
              <div class="mh-feed-line"><span class="mh-when">{{ c.time }}</span> · <b>{{ c.name }}</b><span class="mh-type">{{ c.type }}</span></div>
              <div class="mh-it-desc">{{ c.desc }}</div>
            </div>
          </div>
        </template>
        <div v-else class="mh-empty">暂无变动记录。</div>
      </div>
    </div>

    <div class="mh-entry">
      <div>
        <div class="mh-e-title">资料中心</div>
        <div class="mh-e-desc">逐类查看并维护商家、承运商、物流渠道、仓库与商品等基础资料</div>
      </div>
      <el-button type="primary" @click="go('/tms/master')">进入资料中心</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { merchantList, carrierList, tmsChannelList, warehouseList, productList, slaByCarrier } from '../../api'

const router = useRouter()
const setDesc = inject('setPageDesc')
const loading = ref(true)
const role = (localStorage.getItem('sentinel_role') || 'ADMIN').toUpperCase()

const CATS = [
  { key: 'merchant', title: '商家', path: '/tms/merchant', api: merchantList, roles: ['ADMIN', 'OPERATOR'], color: '#0891b2' },
  { key: 'carrier', title: '承运商', path: '/tms/carrier', api: carrierList, roles: ['ADMIN', 'OPERATOR'], color: '#6366f1' },
  { key: 'channel', title: '物流渠道', path: '/tms/channel', api: tmsChannelList, roles: ['ADMIN', 'OPERATOR'], color: '#0ea5e9' },
  { key: 'warehouse', title: '仓库', path: '/tms/warehouse', api: warehouseList, roles: ['ADMIN', 'OPERATOR'], color: '#f59e0b' },
  { key: 'product', title: '商品', path: '/tms/product', api: productList, roles: ['ADMIN', 'OPERATOR', 'MERCHANT'], color: '#10b981' }
]
const dataMap = {} // key -> {rows, count}
const issues = ref([])
const changes = ref([])
const stat = ref({ total: 0, recent: '—' })

const pad = (n) => String(n).padStart(2, '0')
function toDate(v) {
  if (v == null || v === '') return null
  const s = String(v).trim()
  if (/^\d+$/.test(s)) {
    const n = Number(s)
    const d = new Date(s.length >= 13 ? n : n * 1000)
    return isNaN(d.getTime()) ? null : d
  }
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  return isNaN(d.getTime()) ? null : d
}
function fmtTime(v) {
  const d = toDate(v)
  return d ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}` : null
}

function pickIssues(key, title, path, color, rule) {
  const rows = (dataMap[key] && dataMap[key].rows) || []
  const bad = rows.filter(rule)
  if (!bad.length) return
  issues.value.push({ title, desc: `${bad.length} 条记录${bad.length > 1 ? '存在该问题' : '存在该问题'}，建议核对补全`, path, color })
}
function empty(o) { return o == null || String(o).trim() === '' }

async function load() {
  loading.value = true
  try {
    const allowed = CATS.filter((c) => c.roles.includes(role))
    await Promise.all(allowed.map(async (c) => {
      try {
        const d = (await c.api({ page: 1, perPage: 100 })) || {}
        dataMap[c.key] = { rows: d.rows || [], count: d.count || 0 }
      } catch {
        dataMap[c.key] = { rows: [], count: 0 }
      }
    }))
    // 待办：缺关键字段（真实行数据驱动，无问题则不出项）
    issues.value = []
    pickIssues('merchant', '商家缺少联系电话', '/tms/merchant', '#d97706', (r) => empty(r.contact_phone))
    pickIssues('merchant', '商家缺少联系邮箱', '/tms/merchant', '#d97706', (r) => empty(r.contact_email))
    pickIssues('carrier', '承运商缺少联系人', '/tms/carrier', '#5b5bd6', (r) => empty(r.contact_name) && empty(r.contact))
    pickIssues('warehouse', '仓库缺少地址', '/tms/warehouse', '#e8972b', (r) => empty(r.address))
    pickIssues('product', '商品未上传图片', '/tms/product', '#0c8f62', (r) => empty(r.image_url))
    // 承运商时效异常（真实聚合，可复核）
    try {
      const sla = (await slaByCarrier()) || []
      sla.filter((r) => Number(r.anomalyCount || 0) > 0).slice(0, 3).forEach((r) => {
        issues.value.push({
          title: `${r.carrier_name} 近30天异常单 ${r.anomalyCount} 单`,
          desc: '异常会影响时效与服务体验，建议复核承运商表现',
          path: '/tms/carrier-kpi',
          color: '#e5534b'
        })
      })
    } catch { /* 忽略 */ }
    // 变动流：取各类最近维护记录
    const list = []
    allowed.forEach((c) => {
      const rows = (dataMap[c.key] && dataMap[c.key].rows) || []
      rows.forEach((r) => {
        const ts = r.updated_at || r.created_at
        const t = toDate(ts)
        if (!t) return
        const name = r.merchant_name || r.carrier_name || r.warehouse_name || r.channel_name || r.name || r.merchant_code || r.sku || r.code || String(r.id)
        list.push({ name, type: c.title, path: c.path, time: fmtTime(ts), ts: t.getTime(), desc: '资料更新' })
      })
    })
    list.sort((a, b) => b.ts - a.ts)
    changes.value = list.slice(0, 6)
    // 统计
    const total = allowed.reduce((s, c) => s + (Number(dataMap[c.key] && dataMap[c.key].count) || 0), 0)
    const tsAll = list.map((x) => x.ts).filter(Boolean)
    stat.value = { total, recent: tsAll.length ? fmtTime(Math.max(...tsAll)) : '—' }
  } finally {
    loading.value = false
  }
}

function go(path) { router.push(path) }

onMounted(() => {
  setDesc('资料待办与变动：先处理该补全的资料，再进入资料中心维护')
  load()
})
</script>

<style scoped>
.mh-stats { display: flex; gap: 14px; margin-bottom: 18px; }
.mh-stat { flex: 1; background: #fff; border: 1px solid #e5e6eb; border-radius: 12px; padding: 14px 20px; }
.mh-num { font-size: 24px; font-weight: 700; color: #0891b2; }
.mh-num.mh-warn { color: #d97706; }
.mh-num.mh-sm { font-size: 15px; padding-top: 8px; color: #1d2129; }
.mh-label { margin-top: 4px; font-size: 13px; color: #909399; }

.mh-cols { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 16px; }
.mh-panel { background: #fff; border: 1px solid #e5e6eb; border-radius: 14px; overflow: hidden; }
.mh-ph { padding: 13px 18px; border-bottom: 1px solid #eef1f4; display: flex; align-items: center; gap: 8px; }
.mh-count { font-size: 12px; background: #eef4f6; color: #5a6772; border-radius: 10px; padding: 0 8px; }
.mh-item { display: flex; align-items: flex-start; gap: 12px; padding: 13px 18px; border-bottom: 1px solid #f1f4f6; cursor: pointer; transition: .15s; }
.mh-item:hover { background: #f7fbfc; }
.mh-dot { margin-top: 7px; width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.mh-it-body { flex: 1; min-width: 0; }
.mh-it-title { font-weight: 600; }
.mh-it-desc { color: #5a6772; font-size: 13px; margin-top: 2px; }
.mh-tag { font-size: 12px; color: #b4604a; background: #fdeceb; border: 1px solid #f8cdca; border-radius: 6px; padding: 0 8px; flex-shrink: 0; }
.mh-go { color: #0891b2; font-size: 13px; flex-shrink: 0; visibility: hidden; align-self: center; }
.mh-item:hover .mh-go { visibility: visible; }
.mh-feed { display: flex; align-items: flex-start; gap: 12px; padding: 11px 18px; border-bottom: 1px dashed #eef1f4; cursor: pointer; }
.mh-feed:hover { background: #f7fbfc; }
.mh-fdot { margin-top: 7px; width: 7px; height: 7px; border-radius: 50%; background: #bcdde6; flex-shrink: 0; }
.mh-feed-line { font-size: 13px; }
.mh-when { color: #8b95a1; }
.mh-type { margin-left: 6px; font-size: 12px; color: #0b7fa0; background: #e8f4f7; border-radius: 4px; padding: 0 6px; }
.mh-empty { padding: 40px 16px; text-align: center; color: #aab4bd; font-size: 13px; }

.mh-entry { margin-top: 18px; background: #fff; border: 1px solid #e5e6eb; border-radius: 14px; padding: 18px 22px; display: flex; align-items: center; justify-content: space-between; }
.mh-e-title { font-size: 16px; font-weight: 700; }
.mh-e-desc { margin-top: 4px; font-size: 13px; color: #8b95a1; }
</style>