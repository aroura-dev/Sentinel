<template>
  <el-card shadow="never" class="query-table">
    <div v-if="$slots.extra" class="qt-head">
      <div class="qt-extra"><slot name="extra" /></div>
    </div>
    <div v-if="note" class="qt-note">{{ note }}</div>
    <div class="qt-query"><slot name="query" :reload="reload" /></div>
    <el-table :data="rows" v-loading="loading" border stripe size="small" :row-class-name="rowClassName" :empty-text="emptyText" @selection-change="onSelection">
      <el-table-column v-if="selectable" type="selection" width="45" />
      <el-table-column v-for="col in columns" :key="col.prop" :resizable="false" :prop="col.prop" :label="col.label"
                :width="col.width != null ? (widths[col.prop] || col.width) : undefined"
        :min-width="col.width == null ? (widths[col.prop] || colWidth(col)) : undefined"
        :align="col.align || columnAlign(col)">
        <template #default="{ row }">
          <template v-if="col.type === 'tag'"><StatusTag :status="String(row[col.prop] ?? '')" :map="col.tagMap" /></template>
          <template v-else-if="$slots['cell-' + col.prop]"><slot :name="'cell-' + col.prop" :row="row" /></template>
          <template v-else>{{ formatCell(row, col) }}</template>
        </template>
      </el-table-column>
      <el-table-column v-if="$slots.actions" label="操作" :width="actionWidth" align="center">
        <template #default="{ row }"><slot name="actions" :row="row" /></template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="count > pageSize" class="qt-page" background layout="total, prev, pager, next, sizes"
      :total="count" v-model:current-page="page" :page-size="pageSize" :page-sizes="[10, 20, 50]"
      @current-change="reload" @size-change="onSize" />
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import StatusTag from './StatusTag.vue'
import { fmtDateTime } from '../utils/format'

const props = defineProps({
  title: { type: String, default: '' },
  note: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  load: { type: Function, required: true },
  query: { type: Object, default: () => ({}) },
  pageSize: { type: Number, default: 10 },
  actionWidth: { type: Number, default: 180 },
  selectable: { type: Boolean, default: false },
  rowClassName: { type: Function, default: null },
  emptyText: { type: String, default: '暂无数据' }
})

const rows = ref([])
const count = ref(0)
const page = ref(1)
const perPage = ref(props.pageSize)
const loading = ref(false)
const selectedRows = ref([])

function onSelection(sel) { selectedRows.value = sel }

const widths = reactive({})

// 按文字数量估算列宽：中文≈14px/字，西文/数字≈7.6px/字符，另加留白
function textPx(s) {
  let px = 0
  const str = String(s == null ? '' : s)
  for (let i = 0; i < str.length; i++) px += str.charCodeAt(i) > 255 ? 14 : 7.6
  return px
}
function estimateWidth(col, rows) {
  if (col.width != null) return col.width
  if (col.type === 'tag') return 100
  if (col.type === 'money') return 110
  if (col.type === 'datetime') return 168
  const samples = [col.label || col.prop || '']
  for (const r of rows) {
    const v = r ? r[col.prop] : null
    if (v != null && v !== '') samples.push(String(v))
  }
  let px = 24
  for (const s of samples) {
    const p = textPx(s)
    if (p > px) px = p
  }
  const min = col.minWidth != null ? col.minWidth : 88
  return Math.max(min, Math.min(Math.ceil(px), 420))
}

function colWidth(col) {
  if (col.width != null) return col.width
  if (col.minWidth != null) return col.minWidth
  return 120
}

function columnAlign(col) {
  const prop = String(col.prop || '')
  const label = String(col.label || '')
  if (/(_no|_code|_sn|_num)$/i.test(prop) || /单号|编号|编码|code/i.test(label)) return 'left'
  return 'center'
}

async function reload() {
  loading.value = true
  try {
    const r = await props.load({ page: page.value, perPage: perPage.value, ...props.query })
    rows.value = r.rows || []
    count.value = r.count || 0
    const wm = {}
    for (const col of props.columns) {
      if (col.prop) wm[col.prop] = estimateWidth(col, rows.value)
    }
    for (const k of Object.keys(widths)) if (!(k in wm)) delete widths[k]
    Object.assign(widths, wm)
  } finally {
    loading.value = false
  }
}

function onSize() {
  page.value = 1
  reload()
}

function formatCell(row, col) {
  const v = row[col.prop]
  if (v == null || v === '') return '-'
  if (col.type === 'money') return '¥' + Number(v).toFixed(2)
  if (col.type === 'datetime') return fmtDateTime(v)
  return String(v)
}

watch(() => props.query, reload, { deep: true })
onMounted(reload)
function clearSelection() { selectedRows.value = [] }
defineExpose({ reload, selectedRows, rows, clearSelection })
</script>

<style scoped>
/* 卡片容器约束内部溢出：批量出库按钮区等只能在卡片内渲染，禁止向下溢出穿透 */
.query-table {
  overflow: hidden;
}
.qt-head { display: flex; align-items: center; justify-content: flex-end; margin-bottom: 12px; }
/* 查询/批量出库按钮区：保持常规流，无定位无 z-index，不产生浮动层级 */
.qt-note { margin: -2px 0 12px; padding: 10px 14px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.7; }
.qt-query { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-bottom: 14px; }
.qt-page { margin-top: 14px; justify-content: flex-end; }

/* 行高亮：SLA 违约 / 异常节点订单行浅红提醒 */
:deep(.el-table .row-alert td.el-table__cell) {
  background: #fdf3f3 !important;
}
:deep(.el-table .row-alert:hover > td.el-table__cell) {
  background: #fbe9e9 !important;
}

/* 表格滚动条美化：细、圆角、灰蓝配色 */
:deep(.el-table__body-wrapper::-webkit-scrollbar),
:deep(.el-table::-webkit-scrollbar) {
  height: 8px;
  width: 8px;
}
:deep(.el-table__body-wrapper::-webkit-scrollbar-thumb),
:deep(.el-table::-webkit-scrollbar-thumb) {
  background: #c9d4dc;
  border-radius: 4px;
}
:deep(.el-table__body-wrapper::-webkit-scrollbar-thumb:hover),
:deep(.el-table::-webkit-scrollbar-thumb:hover) {
  background: #a9bcc8;
}
:deep(.el-table__body-wrapper::-webkit-scrollbar-track),
:deep(.el-table::-webkit-scrollbar-track) {
  background: transparent;
}
</style>
