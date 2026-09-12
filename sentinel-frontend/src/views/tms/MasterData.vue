<template>
  <div>
    <!-- 资料数据概览 -->
    <div v-if="!loading" class="md-stats">
      <div class="md-stat">
        <div class="md-stat-num">{{ stat.total ?? 0 }}</div>
        <div class="md-stat-label">资料总量（条）</div>
      </div>
      <div class="md-stat">
        <div class="md-stat-num">{{ stat.rate }}%</div>
        <div class="md-stat-label">启用占比</div>
      </div>
      <div class="md-stat">
        <div class="md-stat-num">{{ stat.new7 }}</div>
        <div class="md-stat-label">近7日新增</div>
      </div>
      <div class="md-stat">
        <div class="md-stat-num md-num-sm">{{ stat.recent }}</div>
        <div class="md-stat-label">最近维护</div>
      </div>
    </div>

    <div class="md-grid">
      <div v-for="c in filtered" :key="c.key" class="md-card" @click="enter(c)">
        <div class="md-card-top">
          <div class="md-ico" :style="{ background: c.bg, color: c.color }"><el-icon :size="20"><component :is="c.icon" /></el-icon></div>
          <el-icon class="md-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="md-card-title">{{ c.title }}</div>
        <div class="md-card-desc">{{ c.desc }}</div>
        <template v-if="c.hasStatus">
          <div class="md-bar"><div class="md-bar-fill" :style="{ width: c.enableRate + '%', background: c.color }"></div></div>
          <div class="md-rate-row"><span>启用 <b>{{ c.enabled }}</b> · 停用 <b>{{ c.disabled }}</b></span><span>启用率 {{ c.enableRate }}%</span></div>
        </template>
        <div class="md-card-foot">
          <span class="md-count"><b>{{ c.count }}</b> 条<template v-if="c.hasCreated"> · 近7日新增 {{ c.new7 }}</template></span>
          <span class="md-recent">最近维护 {{ c.recent }}</span>
        </div>
      </div>
    </div>
    <div v-if="!loading && !filtered.length" class="md-empty">暂无资料</div>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Shop, Van, Connection, House, Goods } from '@element-plus/icons-vue'
import { merchantList, carrierList, tmsChannelList, warehouseList, productList } from '../../api'

const router = useRouter()
const setDesc = inject('setPageDesc')
const loading = ref(true)
const role = (localStorage.getItem('sentinel_role') || 'ADMIN').toUpperCase()

const CATS = [
  { key: 'merchant', title: '商家', desc: '货主（卖家）主数据，订单、对账、售后按商家归属', icon: Shop, path: '/tms/merchant', api: merchantList, roles: ['ADMIN', 'OPERATOR'], color: '#0891b2', bg: '#e6f4f7' },
  { key: 'carrier', title: '承运商', desc: '实际承运方，物流渠道与价卡都挂靠其下', icon: Van, path: '/tms/carrier', api: carrierList, roles: ['ADMIN', 'OPERATOR'], color: '#6366f1', bg: '#eef0fe' },
  { key: 'channel', title: '物流渠道', desc: '发往目的地的运输方式，价卡按渠道生效', icon: Connection, path: '/tms/channel', api: tmsChannelList, roles: ['ADMIN', 'OPERATOR'], color: '#0ea5e9', bg: '#e8f6fd' },
  { key: 'warehouse', title: '仓库', desc: '库存与发货的物理归属，支持多仓分布', icon: House, path: '/tms/warehouse', api: warehouseList, roles: ['ADMIN', 'OPERATOR'], color: '#f59e0b', bg: '#fef4e6' },
  { key: 'product', title: '商品', desc: '订单履约的最小单位，SKU 全局唯一', icon: Goods, path: '/tms/product', api: productList, roles: ['ADMIN', 'OPERATOR', 'MERCHANT'], color: '#10b981', bg: '#ecfdf5' }
]

const cards = ref([])
const visible = computed(() => cards.value.filter((c) => c.roles.includes(role)))
const filtered = computed(() => visible.value)
const totalCount = computed(() => visible.value.reduce((s, c) => s + (Number(c.count) || 0), 0))
const stat = computed(() => {
  const total = totalCount.value
  const enabled = visible.value.reduce((s, c) => s + (Number(c.enabled) || 0), 0)
  const new7 = visible.value.reduce((s, c) => s + (Number(c.new7) || 0), 0)
  const rate = total ? Math.round(enabled / total * 100) : 0
  const items = visible.value.map((c) => c.ts).filter(Boolean)
  let recent = '—'
  if (items.length) recent = fmtTime(Math.max(...items))
  return { total, enabled, new7, rate, recent }
})

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
function fmtDate(v) {
  const d = toDate(v)
  return d ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` : '—'
}
function fmtTime(v) {
  const d = toDate(v)
  return d ? `${fmtDate(v)} ${pad(d.getHours())}:${pad(d.getMinutes())}` : '—'
}

async function loadOne(cat) {
  try {
    const data = (await cat.api({ page: 1, perPage: 200 })) || {}
    const rows = data.rows || []
    const count = data.count || rows.length || 0
    const hasStatus = rows.some((r) => Object.prototype.hasOwnProperty.call(r, 'status'))
    const hasCreated = rows.some((r) => Object.prototype.hasOwnProperty.call(r, 'created_at'))
    const enabled = hasStatus ? rows.filter((r) => Number(r.status) === 1).length : null
    const disabled = hasStatus ? rows.length - (enabled || 0) : null
    const now = Date.now()
    const week = 7 * 24 * 3600 * 1000
    const new7 = hasCreated ? rows.filter((r) => {
      const d = toDate(r.created_at)
      return d && now - d.getTime() < week
    }).length : null
    let ts = null
    for (const r of rows) {
      const t = toDate(r.updated_at || r.created_at)
      if (t && (!ts || t.getTime() > ts)) ts = t.getTime()
    }
    return {
      ...cat,
      count,
      enabled: enabled == null ? null : enabled,
      disabled: disabled == null ? null : disabled,
      new7: new7 == null ? null : new7,
      hasStatus,
      hasCreated,
      enableRate: hasStatus && count ? Math.round((enabled || 0) / count * 100) : 0,
      recent: ts ? fmtTime(ts) : '—',
      ts
    }
  } catch {
    return { ...cat, count: 0, enabled: null, disabled: null, new7: null, hasStatus: false, hasCreated: false, enableRate: 0, recent: '—', ts: null }
  }
}

async function load() {
  loading.value = true
  try {
    const allowed = CATS.filter((c) => c.roles.includes(role))
    cards.value = await Promise.all(allowed.map(loadOne))
  } finally {
    loading.value = false
  }
}

function enter(c) {
  router.push(c.path)
}

onMounted(() => {
  setDesc('基础资料集中管理：商家、承运商、渠道、仓库、商品')
  load()
})
</script>

<style scoped>
.md-stats {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}
.md-stat {
  flex: 1;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  padding: 14px 20px;
}
.md-stat-num { font-size: 24px; font-weight: 700; color: #0891b2; }
.md-stat-num.md-num-sm { font-size: 15px; padding-top: 8px; color: #1d2129; }
.md-stat-label { margin-top: 4px; font-size: 13px; color: #909399; }

.md-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.md-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  padding: 18px 20px 14px;
  cursor: pointer;
  transition: all .2s ease;
}
.md-card:hover {
  border-color: #67c8dc;
  box-shadow: 0 8px 20px rgba(8, 145, 178, 0.10);
  transform: translateY(-2px);
}
.md-card-top { display: flex; align-items: flex-start; justify-content: space-between; }
.md-ico { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.md-arrow { color: #c0c8d0; transition: color .2s; }
.md-card:hover .md-arrow { color: #0891b2; }
.md-card-title { margin-top: 12px; font-size: 16px; font-weight: 600; color: #1d2129; }
.md-card-desc { margin-top: 6px; font-size: 13px; color: #86909c; line-height: 1.7; min-height: 44px; }
.md-bar { margin-top: 10px; height: 6px; background: #f0f2f5; border-radius: 3px; overflow: hidden; }
.md-bar-fill { height: 100%; border-radius: 3px; transition: width .4s ease; }
.md-rate-row { margin-top: 6px; display: flex; justify-content: space-between; font-size: 12px; color: #57606a; }
.md-rate-row b { color: #1d2129; }
.md-card-foot { margin-top: 12px; padding-top: 10px; border-top: 1px solid #f0f2f5; display: flex; justify-content: space-between; font-size: 12px; color: #9aa3ad; }
.md-count b { font-size: 15px; color: #1d2129; margin-right: 2px; }
.md-empty { padding: 40px 0; text-align: center; color: #aab4bd; }
</style>