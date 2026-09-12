<template>
  <div>
    <div class="kpi-banner">
      <div class="kpi-head"><el-icon :size="15"><Trophy /></el-icon><span class="kpi-title">承运商考核</span><span class="kpi-tip">包裹送得是否准时、签收是否顺利、异常多不多，这里一表看清</span></div>
      <div class="kpi-stats">
        <div class="kpi-item">承运商 <b>{{ rows.length }}</b> 家</div>
        <div class="kpi-item">覆盖运单 <b>{{ statSum('total') }}</b> 票</div>
        <div class="kpi-item">平均准时率 <b>{{ avgRate('onTimeRate') }}%</b></div>
        <div class="kpi-item">A/B 级承运商 <b>{{ passCount }}</b> 家</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="kpi-bh">
          <b>承运商服务表现</b>
          <div class="kpi-bh-right">
            <el-input v-model="keyword" placeholder="搜索承运商" clearable style="width: 200px" @input="applyFilter" />
            <el-button size="small" :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </template>
      <div class="kpi-note">基于准时率、妥投率与时效表现自动评分，异常运单按单扣分，并按得分划分 A–D 服务等级。</div>
      <el-table :data="shown" v-loading="loading" border stripe size="small" resizable="false" :empty-text="'暂无承运商运单数据'">
        <el-table-column :resizable="false" label="承运商" align="left" min-width="180">
          <template #default="{ row }">
            <div class="kpi-carrier">
              <span class="kpi-carrier-name">{{ row.carrier_name }}</span>
              <span class="kpi-carrier-code">{{ row.carrier_code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" prop="total" label="运单量" align="center" width="90" sortable />
        <el-table-column :resizable="false" label="妥投率" align="center" width="100" sortable prop="deliveredRate">
          <template #default="{ row }">{{ row.deliveredRate }}%</template>
        </el-table-column>
        <el-table-column :resizable="false" label="准时率" align="center" width="110" sortable prop="onTimeRate">
          <template #default="{ row }"><span :class="['kpi-rate', row.onTimeRate >= 90 ? 'ok' : row.onTimeRate >= 75 ? 'mid' : 'bad']">{{ row.onTimeRate }}%</span></template>
        </el-table-column>
        <el-table-column :resizable="false" label="平均时效(h)" align="center" width="120" sortable prop="avgTransitHours">
          <template #default="{ row }">{{ row.avgTransitHours }}</template>
        </el-table-column>
        <el-table-column :resizable="false" prop="anomalyCount" label="异常单" align="center" width="90" sortable />
        <el-table-column :resizable="false" label="履约得分" align="center" width="120" sortable prop="score">
          <template #default="{ row }">
            <div class="kpi-score">
              <el-progress :percentage="row.score" :stroke-width="8" :show-text="false" :color="scoreColor(row.score)" />
              <span class="kpi-score-num">{{ row.score }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" label="评级" align="center" width="90">
          <template #default="{ row }"><el-tag size="small" :type="gradeType(row.grade)">{{ row.grade }}</el-tag></template>
        </el-table-column>
        <el-table-column :resizable="false" label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">运单明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  
    <el-dialog v-model="detailOpen" :title="detailCarrier ? detailCarrier.carrier_name + ' · 运单明细' : '运单明细'" width="900px" top="6vh">
      <el-table :data="wbRows" v-loading="wbLoading" border stripe size="small" :empty-text="'暂无运单数据'">
        <el-table-column :resizable="false" label="运单号" align="left" width="200">
          <template #default="{ row }"><router-link :to="'/tms/waybill/' + row.waybill_no" style="color: #0891b2; text-decoration: none">{{ row.waybill_no }}</router-link></template>
        </el-table-column>
        <el-table-column :resizable="false" prop="order_no" label="订单号" align="left" min-width="170" />
        <el-table-column :resizable="false" prop="tracking_no" label="跟踪号" align="center" min-width="150" />
        <el-table-column :resizable="false" label="状态" align="center" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column :resizable="false" label="创建时间" align="center" width="170">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 12px; justify-content: flex-end" background layout="total, prev, pager, next"
        :total="wbTotal" :page-size="wbQuery.perPage" :current-page="wbQuery.page" @current-change="onWbPage" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { slaByCarrier, waybillList } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const shown = ref([])
const loading = ref(false)
const keyword = ref('')

const setDesc = inject('setPageDesc')

const passCount = computed(() => rows.value.filter((r) => r.grade === 'A' || r.grade === 'B').length)

function scoreColor(s) { return s >= 90 ? '#10b981' : s >= 75 ? '#0891b2' : s >= 60 ? '#e6a23c' : '#f56c6c' }
function gradeType(g) { return g === 'A' ? 'success' : g === 'B' ? 'primary' : g === 'C' ? 'warning' : 'danger' }

function statSum(k) { return rows.value.reduce((s, r) => s + Number(r[k] || 0), 0) }
function avgRate(k) {
  const arr = rows.value.filter((r) => Number(r[k] || 0) > 0)
  if (!arr.length) return '0.0'
  return (arr.reduce((s, r) => s + Number(r[k] || 0), 0) / arr.length).toFixed(1)
}

function build(row) {
  const total = Number(row.total || 0)
  const delivered = Number(row.delivered || 0)
  const onTime = Number(row.onTime || 0)
  const anomaly = Number(row.anomalyCount || 0)
  const avgH = Number(row.avgTransitHours || 0)
  const deliveredRate = total > 0 ? (delivered / total * 100) : 0
  const onTimeRate = delivered > 0 ? (onTime / delivered * 100) : 0
  const timeScore = Math.max(0, 100 - avgH * 2)
  let score = onTimeRate * 0.6 + deliveredRate * 0.25 + timeScore * 0.15 - anomaly * 1.5
  score = Math.round(Math.max(0, Math.min(100, score)))
  const grade = score >= 90 ? 'A' : score >= 75 ? 'B' : score >= 60 ? 'C' : 'D'
  return {
    carrier_id: row.carrier_id != null ? Number(row.carrier_id) : null,
    carrier_code: row.carrier_code,
    carrier_name: row.carrier_name,
    total,
    deliveredRate: deliveredRate.toFixed(1),
    onTimeRate: onTimeRate.toFixed(1),
    avgTransitHours: avgH.toFixed(1),
    anomalyCount: anomaly,
    score,
    grade
  }
}

function applyFilter() {
  const kw = (keyword.value || '').trim()
  shown.value = kw ? rows.value.filter((r) => (r.carrier_name || '').includes(kw) || (r.carrier_code || '').toLowerCase().includes(kw.toLowerCase())) : rows.value
}

const detailOpen = ref(false)
const detailCarrier = ref(null)
const wbRows = ref([])
const wbTotal = ref(0)
const wbLoading = ref(false)
const wbQuery = reactive({ page: 1, perPage: 10 })
const fmt = (v) => (v ? String(v).slice(0, 19) : '-')

async function openDetail(row) {
  detailCarrier.value = row
  wbQuery.page = 1
  detailOpen.value = true
  await loadWbs()
}

async function loadWbs() {
  const c = detailCarrier.value
  if (!c || !c.carrier_id) return
  wbLoading.value = true
  try {
    const data = await waybillList({ carrierId: c.carrier_id, page: wbQuery.page, perPage: wbQuery.perPage })
    wbRows.value = data.rows || []
    wbTotal.value = data.count || 0
  } finally {
    wbLoading.value = false
  }
}

function onWbPage(p) { wbQuery.page = p; loadWbs() }

async function load() {
  loading.value = true
  try {
    const data = (await slaByCarrier()) || []
    rows.value = data.map(build).sort((a, b) => b.score - a.score)
    applyFilter()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setDesc('以妥投与准时为核心口径的承运商履约考核与评级')
  load()
})
</script>

<style scoped>
.kpi-banner {
  margin-bottom: 16px;
  padding: 16px 20px 14px 24px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.kpi-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.kpi-head { display: flex; align-items: center; gap: 8px; }
.kpi-head .el-icon { color: #0891b2; }
.kpi-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.kpi-tip { font-size: 12px; color: #9ca3af; }
.kpi-stats { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 26px; margin-top: 10px; }
.kpi-item { font-size: 13px; color: #57606a; }
.kpi-item b { font-size: 16px; color: #1d2129; margin-left: 2px; }
.kpi-bh { display: flex; align-items: center; justify-content: space-between; }
.kpi-bh-right { display: flex; gap: 8px; }
.kpi-note { margin: -4px 0 12px; padding: 8px 12px; font-size: 12px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 6px; line-height: 1.7; }
.kpi-carrier { display: flex; flex-direction: column; }
.kpi-carrier-name { color: #1d2129; font-weight: 500; }
.kpi-carrier-code { font-size: 12px; color: #9ca3af; }
.kpi-rate.ok { color: #10b981; font-weight: 600; }
.kpi-rate.mid { color: #e6a23c; font-weight: 600; }
.kpi-rate.bad { color: #f56c6c; font-weight: 600; }
.kpi-score { display: flex; align-items: center; gap: 8px; }
.kpi-score .el-progress { flex: 1; }
.kpi-score-num { min-width: 30px; font-weight: 700; color: #1d2129; }
</style>