<template>
  <div class="aa-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="aa-header">
          <div class="aa-header-title">
            <span class="aa-badge">析</span>
            <span>智能分析</span>
            <el-tag size="small" effect="plain">用大白话问数据</el-tag>
          </div>
        </div>
      </template>

      <!-- 服务不可用空态 -->
      <el-alert
        v-if="serviceDown"
        type="warning" :closable="false" show-icon
        title="AI 服务建设中"
        description="智能分析功能正在接入，暂不可用。您可以先通过「运营总览」查看核心数据。"
        class="aa-down-alert"
      />

      <div class="aa-ask">
        <el-input
          v-model="question"
          placeholder="问一句大白话，如：本月各渠道发了多少单？哪些承运商延误最多？"
          :disabled="serviceDown"
          @keyup.enter="ask"
        />
        <el-button type="primary" :loading="loading" :disabled="serviceDown" @click="ask">分析</el-button>
      </div>

      <div class="aa-samples">
        <el-tag
          v-for="s in samples" :key="s"
          size="small" effect="plain" class="aa-sample" @click="quickAsk(s)"
        >{{ s }}</el-tag>
      </div>

      <div v-if="loading" class="aa-loading">
        <el-icon class="is-loading" :size="20"><Loading /></el-icon>
        <span>AI 正在分析数据…</span>
      </div>

      <template v-if="result">
        <el-alert
          v-if="result.degraded"
          type="warning" :closable="false" show-icon class="aa-degraded"
          title="AI 服务暂不可用，以下为固定规则聚合结果"
        />
        <el-card shadow="never" class="aa-answer">
          <div class="aa-answer-label">分析结论</div>
          <div class="aa-answer-text">{{ result.answer }}</div>
        </el-card>

        <div v-if="result.data && result.data.rows && result.data.rows.length" class="aa-chart" :key="chartKey">
          <div class="aa-answer-label">数据明细</div>
          <div class="aa-bars">
            <div v-for="(row, ri) in barRows" :key="ri" class="aa-bar-row">
              <span class="aa-bar-label">{{ barLabel(row) }}</span>
              <div class="aa-bar-track">
                <div class="aa-bar-fill" :style="{ width: barWidth(row) + '%' }"></div>
              </div>
              <span class="aa-bar-val">{{ barValue(row) }}</span>
            </div>
          </div>
        </div>

        <el-table v-if="result.data && result.data.rows && result.data.rows.length" :data="tableRows" size="small" border class="aa-table">
          <el-table-column
            v-for="(col, ci) in tableColumns"
            :key="ci"
            :prop="'c' + ci"
            :label="col"
          />
        </el-table>

        <div v-if="result.metric_note" class="aa-note">口径说明：{{ result.metric_note }}</div>
        <div v-if="result.trace_id" class="aa-trace">调用 ID：{{ result.trace_id }}（可在「AI 过程追踪」中查看本次分析）</div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { agentAnalytics } from '../../api'

const question = ref('')
const loading = ref(false)
const serviceDown = ref(false)
const result = ref(null)
const chartKey = ref(0)

const samples = ['本月各渠道订单量', '各承运商发货量对比', '当前各状态订单分布', '哪些线路延误最多']

function quickAsk(s) {
  question.value = s
  ask()
}

async function ask() {
  if (!question.value) return
  loading.value = true
  try {
    result.value = await agentAnalytics({ question: question.value })
    chartKey.value++
  } catch (e) {
    serviceDown.value = true
  } finally {
    loading.value = false
  }
}

// 动态表格列：columns 数组 → el-table column
const tableColumns = computed(() => (result.value && result.value.data ? result.value.data.columns || [] : []))
const tableRows = computed(() => {
  const rows = (result.value && result.value.data ? result.value.data.rows || [] : [])
  return rows.map((r) => {
    const o = {}
    r.forEach((v, i) => { o['c' + i] = v })
    return o
  })
})

// 极简条形图：找数值列（最后一列）做归一化条形
function numericIndex() {
  const rows = (result.value && result.value.data ? result.value.data.rows || [] : [])
  if (!rows.length) return -1
  const last = rows[0][rows[0].length - 1]
  return typeof last === 'number' || /^\d/.test(String(last)) ? rows[0].length - 1 : -1
}
function maxNum() {
  const rows = (result.value && result.value.data ? result.value.data.rows || [] : [])
  const idx = numericIndex()
  if (idx < 0) return 1
  return Math.max(1, ...rows.map((r) => Number(r[idx]) || 0))
}
const barRows = computed(() => (result.value && result.value.data ? result.value.data.rows || [] : []).slice(0, 10))
function barLabel(row) {
  return String(row[0])
}
function barValue(row) {
  const idx = numericIndex()
  return idx < 0 ? '' : String(row[idx])
}
function barWidth(row) {
  const idx = numericIndex()
  if (idx < 0) return 0
  return Math.max(2, Math.round(((Number(row[idx]) || 0) / maxNum()) * 100))
}
</script>

<style scoped>
.aa-header {
  display: flex;
  align-items: center;
}
.aa-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.aa-badge {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #0891b2;
  color: #fff;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.aa-down-alert {
  margin-bottom: 12px;
}

.aa-ask {
  display: flex;
  gap: 10px;
}
.aa-samples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 16px;
}
.aa-sample {
  cursor: pointer;
  transition: all .2s;
}
.aa-sample:hover {
  border-color: #0891b2;
  color: #0891b2;
}

.aa-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0891b2;
  font-size: 14px;
  padding: 24px 0;
}
.aa-degraded {
  margin-bottom: 12px;
}

.aa-answer {
  margin-bottom: 12px;
}
.aa-answer-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8px;
}
.aa-answer-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
}

.aa-chart {
  margin-bottom: 12px;
}
.aa-bars {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
}
.aa-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.aa-bar-row:last-child {
  margin-bottom: 0;
}
.aa-bar-label {
  width: 120px;
  font-size: 13px;
  color: #606266;
  text-align: right;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.aa-bar-track {
  flex: 1;
  height: 14px;
  background: #eef0f2;
  border-radius: 7px;
  overflow: hidden;
}
.aa-bar-fill {
  height: 100%;
  background: #0891b2;
  border-radius: 7px;
  transition: width .4s ease;
}
.aa-bar-val {
  width: 70px;
  font-size: 13px;
  color: #303133;
  font-weight: 600;
}

.aa-table {
  margin-bottom: 12px;
}
.aa-note {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.aa-trace {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
