<template>
  <div>
    <!-- KPI 核心指标 -->
    <el-row :gutter="16">
      <el-col :span="8" v-for="card in kpis" :key="card.label">
        <el-card shadow="hover" class="kpi-card" :body-style="{ padding: '16px 20px' }">
          <div class="kpi" :class="{ clickable: card.to }" @click="card.to && goDrill(card.to)">
            <div class="kpi-icon" :style="{ background: card.bg, color: card.color }">
              <el-icon :size="18"><component :is="card.icon" /></el-icon>
            </div>
            <div class="kpi-meta">
              <div class="kpi-label">{{ card.label }}</div>
              <div class="kpi-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近 7 天订单趋势 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header><b>近 7 天订单趋势</b></template>
      <div ref="trendRef" class="chart"></div>
    </el-card>

    <!-- 渠道与承运商业务量 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><b>渠道运量对比</b></template>
          <div ref="channelRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><b>承运商业务量</b></template>
          <div ref="carrierRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { inject, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { tmsOverview, channelMix, tmsOrderTrend, tmsCarrierVolume } from '../api'

// 单色系（低饱和蓝灰），异常用暖色警示
const ACCENT = '#0891b2'
const ACCENT2 = '#5f7d99'
const SPLIT = '#f0f2f5'
const AXIS = '#dcdfe6'

const kpis = ref([
  { label: '履约订单', value: '-', color: ACCENT, bg: 'rgba(8,145,178,.10)', icon: 'Tickets', to: '/tms/order' },
  { label: '在途', value: '-', color: ACCENT, bg: 'rgba(8,145,178,.10)', icon: 'Van', to: '/tms/order?node=IN_TRANSIT' },
  { label: '已签收', value: '-', color: ACCENT, bg: 'rgba(8,145,178,.10)', icon: 'CircleCheck', to: '/tms/order?node=DELIVERED' },
  { label: '异常单', value: '-', color: '#e6653c', bg: 'rgba(230,101,60,.10)', icon: 'Warning', to: '/tms/order?node=CUSTOMS_DELAY' },
  { label: '运费总额(¥)', value: '-', color: ACCENT, bg: 'rgba(8,145,178,.10)', icon: 'Money', to: '/tms/order' },
  { label: '待处理工单', value: '-', color: ACCENT, bg: 'rgba(8,145,178,.10)', icon: 'Document', to: '/workorder' }
])

const router = useRouter()
function goDrill(to) { if (to) router.push(to) }

const setDesc = inject('setPageDesc')
const setActions = inject('setPageActions')

const channelRef = ref(null)
const carrierRef = ref(null)
const trendRef = ref(null)
const charts = []

function initChart(el) {
  const c = echarts.init(el)
  charts.push(c)
  return c
}

async function loadKpis() {
  try {
    const t = await tmsOverview()
    kpis.value[0].value = t.totalOrders ?? 0
    kpis.value[1].value = t.inTransit ?? 0
    kpis.value[2].value = t.delivered ?? 0
    kpis.value[3].value = t.anomaly ?? 0
    kpis.value[4].value = Number(t.totalFreight ?? 0).toFixed(2)
    kpis.value[5].value = t.openWorkorders ?? 0
  } catch (e) { /* 无权限忽略 */ }
}

// 近 7 天订单趋势（折线，单色，无面积/标注）
async function loadTrend() {
  try {
    const trend = await tmsOrderTrend()
    const dates = []
    const cnts = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const key = d.toISOString().slice(0, 10)
      dates.push(key.slice(5))
      const hit = trend.find((t) => String(t.date).slice(0, 10) === key)
      cnts.push(hit ? Number(hit.cnt) : 0)
    }
    const c = initChart(trendRef.value)
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 20, top: 24, bottom: 8, containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates, axisLabel: { fontSize: 12 }, axisLine: { lineStyle: { color: AXIS } } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: SPLIT } } },
      series: [{
        type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: cnts,
        itemStyle: { color: ACCENT },
        lineStyle: { width: 2, color: ACCENT }
      }]
    })
  } catch (e) { /* 已由拦截器提示 */ }
}

// 渠道运量对比（柱状，单色，无标注）
async function loadChannel() {
  try {
    const mix = await channelMix()
    const c = initChart(channelRef.value)
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 20, top: 24, bottom: 8, containLabel: true },
      xAxis: { type: 'category', data: mix.map((m) => m.channel_name), axisLabel: { interval: 0, rotate: 30, fontSize: 12 }, axisLine: { lineStyle: { color: AXIS } } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: SPLIT } } },
      series: [{
        name: '运量', type: 'bar', barWidth: 20,
        data: mix.map((m) => m.cnt),
        itemStyle: { color: ACCENT }
      }]
    })
  } catch (e) { /* 已由拦截器提示 */ }
}

// 承运商业务量（条形，单色，无标注）
async function loadCarrier() {
  try {
    const cv = await tmsCarrierVolume()
    const c = initChart(carrierRef.value)
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
      xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: SPLIT } } },
      yAxis: { type: 'category', data: cv.map((x) => x.carrier_name).reverse(), axisLabel: { fontSize: 12 } },
      series: [{
        name: '业务量', type: 'bar', barWidth: 14,
        data: cv.map((x) => x.cnt).reverse(),
        itemStyle: { color: ACCENT2 }
      }]
    })
  } catch (e) { /* 已由拦截器提示 */ }
}

function handleResize() { charts.forEach((c) => c && c.resize()) }

onMounted(() => {
  setDesc('履约、渠道、承运商与订单趋势核心指标')
  setActions([{ text: '刷新数据', type: 'primary', onClick: () => { loadKpis(); loadTrend(); loadChannel(); loadCarrier() } }])
  loadKpis()
  loadTrend()
  loadChannel()
  loadCarrier()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c && c.dispose())
})
</script>

<style scoped>
.kpi-card {
  transition: transform 0.15s ease;
}
.kpi {
  display: flex;
  align-items: center;
  gap: 14px;
}
.kpi.clickable {
  cursor: pointer;
}
.kpi.clickable:hover {
  transform: translateY(-2px);
}
.kpi-icon {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.kpi-label {
  font-size: 13px;
  color: #86909c;
}
.kpi-value {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #1d2129;
}
.chart {
  height: 280px;
}
</style>
