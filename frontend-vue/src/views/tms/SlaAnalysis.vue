<template>
  <div>
    <el-card shadow="never" class="sla-toolbar">
      <div class="sla-tb-left">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" style="width: 280px" clearable @change="onRange" />
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="sla-tb-tip">按日期查看各渠道、各承运商的时效表现，及时发现送得慢的线路，异常运单可到「风险预警」跟进。</div>
    </el-card>

    <div class="sla-kpis">
      <div class="sla-kpi"><div class="sla-num">{{ ov.total || 0 }}</div><div class="sla-label">运单量</div></div>
      <div class="sla-kpi"><div class="sla-num">{{ fmt(ov.onTimeRate) }}%</div><div class="sla-label">准时率</div></div>
      <div class="sla-kpi"><div class="sla-num">{{ hours(ov.avgTransitHours) }}</div><div class="sla-label">平均时效(小时)</div></div>
      <div class="sla-kpi"><div class="sla-num">{{ ov.inTransit || 0 }}</div><div class="sla-label">在途</div></div>
      <div class="sla-kpi"><div class="sla-num">{{ ov.anomalyCount || 0 }}</div><div class="sla-label">异常单</div></div>
    </div>

    <div class="sla-grid">
      <el-card shadow="never">
        <template #header><b>渠道准时率</b></template>
        <el-table :data="channels" v-loading="loading" border stripe size="small" resizable="false" :empty-text="'该时间段暂无运单数据'">
          <el-table-column :resizable="false" prop="channel_name" label="渠道" align="left" min-width="140" />
          <el-table-column :resizable="false" prop="total" label="运单" align="center" width="90" sortable />
          <el-table-column :resizable="false" label="准时率" align="center" min-width="150">
            <template #default="{ row }">
              <div class="sla-bar"><div class="sla-bar-fill" :style="{ width: rate(row) + '%' }"></div></div>
            </template>
          </el-table-column>
          <el-table-column :resizable="false" label="准时率%" align="center" width="100" sortable prop="onTimeRate">
            <template #default="{ row }">{{ row.onTimeRate }}%</template>
          </el-table-column>
          <el-table-column :resizable="false" label="平均时效(h)" align="center" width="120">
            <template #default="{ row }">{{ hours(row.avgTransitHours) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header><b>承运商时效</b></template>
        <el-table :data="carriers" v-loading="loading" border stripe size="small" resizable="false" :empty-text="'该时间段暂无运单数据'">
          <el-table-column :resizable="false" prop="carrier_name" label="承运商" align="left" min-width="130" />
          <el-table-column :resizable="false" prop="total" label="运单" align="center" width="90" sortable />
          <el-table-column :resizable="false" label="准时率%" align="center" width="100" sortable prop="onTimeRate">
            <template #default="{ row }">{{ row.onTimeRate }}%</template>
          </el-table-column>
          <el-table-column :resizable="false" label="平均时效(h)" align="center" width="120">
            <template #default="{ row }">{{ hours(row.avgTransitHours) }}</template>
          </el-table-column>
          <el-table-column :resizable="false" prop="anomalyCount" label="异常数" align="center" width="90" sortable />
        </el-table>
      </el-card>
    </div>

    <el-card shadow="never" class="sla-delay-card">
      <template #header><b>延误 Top 原因</b></template>
      <div v-if="!delays.length" class="sla-empty">该时间段暂无延误工单数据</div>
      <el-tag v-for="d in delays" :key="d.type" size="large" effect="plain" class="sla-delay-tag">
        {{ delayLabel(d.type) }} × {{ d.cnt }}
      </el-tag>
    </el-card>
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { slaOverview, slaByChannel, slaByCarrier, slaTopDelay } from '../../api'

const ov = ref({})
const channels = ref([])
const carriers = ref([])
const delays = ref([])
const loading = ref(false)
const range = ref(null)
const setDesc = inject('setPageDesc')

const delayMap = { customs_delay: '中转延误', delivery_failed: '派送失败', lost: '丢件', returned: '退回', sla_breach: 'SLA 违约' }
const delayLabel = (k) => delayMap[k] || k

function rate(row) {
  const delivered = Number(row.delivered || 0)
  if (!delivered) return '0.0'
  return (Number(row.onTime || 0) / delivered * 100).toFixed(1)
}
const hours = (v) => Number(v || 0).toFixed(1)
const fmt = (v) => Number(v || 0).toFixed(1)

function params() {
  const p = {}
  if (range.value && range.value[0]) p.start = range.value[0]
  if (range.value && range.value[1]) p.end = range.value[1]
  return p
}

async function load() {
  loading.value = true
  try {
    const p = params()
    const [o, c, ca, d] = await Promise.all([slaOverview(p), slaByChannel(p), slaByCarrier(p), slaTopDelay(p)])
    ov.value = o || {}
    channels.value = (c || []).map((r) => ({ ...r, total: Number(r.total || 0), onTimeRate: Number(rate(r)) }))
    carriers.value = (ca || []).map((r) => ({ ...r, total: Number(r.total || 0), onTimeRate: Number(rate(r)), anomalyCount: Number(r.anomalyCount || 0) }))
    delays.value = d || []
  } finally {
    loading.value = false
  }
}

function onRange() { load() }

function reset() {
  range.value = null
  load()
}

onMounted(() => {
  setDesc('以时间范围为视角的时效看板：准时率、平均时效、渠道与承运商表现')
  load()
})
</script>

<style scoped>
.sla-toolbar { margin-bottom: 16px; }
.sla-tb-left { display: flex; gap: 10px; align-items: center; }
.sla-tb-tip { margin-top: 10px; font-size: 12px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 6px; padding: 8px 12px; line-height: 1.7; }
.sla-kpis { display: flex; gap: 14px; margin-bottom: 16px; }
.sla-kpi { flex: 1; background: #f5f7fa; border: 1px solid #ebeef5; border-radius: 10px; padding: 16px 20px; }
.sla-num { font-size: 24px; font-weight: 700; color: #0891b2; }
.sla-label { font-size: 13px; color: #909399; margin-top: 4px; }
.sla-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.sla-bar { height: 12px; background: #eef0f2; border-radius: 6px; overflow: hidden; }
.sla-bar-fill { height: 100%; background: #0891b2; border-radius: 6px; transition: width .4s ease; }
.sla-delay-card .sla-empty { color: #c0c4cc; font-size: 13px; padding: 16px 0; text-align: center; }
.sla-delay-tag { margin: 0 10px 10px 0; font-size: 14px; }
</style>