<template>
  <div>
    <div class="sio-cards">
      <div class="sio-card">
        <div class="sio-num">{{ today.in }}</div>
        <div class="sio-label">今日入库</div>
      </div>
      <div class="sio-card">
        <div class="sio-num">{{ today.out }}</div>
        <div class="sio-label">今日出库</div>
      </div>
      <div class="sio-card">
        <div class="sio-num">{{ today.other }}</div>
        <div class="sio-label">今日退回/调整</div>
      </div>
    </div>
    <div class="sio-tip">说明：以下为今日已登记的出入库流水汇总（入库 {{ today.in }}、出库 {{ today.out }}、退回/调整 {{ today.other }}），登记完成后对应 SKU 库存实时更新。</div>

    <DataTable ref="dt" title="出入库流水" :columns="columns" :load="loadFlows" :query="flowQuery" page-size="20">
      <template #extra>
        <el-button type="primary" @click="openAdjust">登记出入库</el-button>
      </template>
      <template #query="{ reload }">
        <el-input v-model="flowQuery.sku" placeholder="商品名称 / 业务单号" clearable style="width: 220px" @keyup.enter="reload" />
        <el-select v-model="flowQuery.bizType" placeholder="全部类型" clearable style="width: 140px" @change="reload">
          <el-option label="入库" value="IN" />
          <el-option label="出库" value="OUT" />
          <el-option label="退货退回" value="REFUND" />
          <el-option label="盘点调整" value="ADJUST" />
        </el-select>
        <el-select v-model="flowQuery.warehouseId" placeholder="全部仓库" clearable style="width: 150px" @change="reload">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
      </template>
      <template #cell-biz_type="{ row }">
        <el-tag :type="(typeMeta[row.biz_type] && typeMeta[row.biz_type].type) || 'info'" size="small">
          {{ (typeMeta[row.biz_type] && typeMeta[row.biz_type].label) || row.biz_type }}
        </el-tag>
      </template>
      <template #cell-qty="{ row }">
        <span :class="{ 'sio-out': isDecrease(row.biz_type) }">
          {{ isIncrease(row.biz_type) ? '+' : isDecrease(row.biz_type) ? '-' : '' }}{{ row.qty }}
        </span>
      </template>
      <template #cell-sku="{ row }">{{ skuName(row.sku) }}</template>
      <template #cell-warehouse_id="{ row }">{{ warehouseName(row.warehouse_id) }}</template>
    </DataTable>

    <el-dialog v-model="dialogVisible" title="登记出入库" width="460px">
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="form.bizType" style="width: 100%">
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
            <el-option label="退货退回" value="REFUND" />
            <el-option label="盘点调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" required>
          <el-select v-model="form.warehouseId" placeholder="选择仓库（必选）" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" required>
          <el-select v-model="form.sku" filterable placeholder="搜索并选择商品" style="width: 100%">
            <el-option v-for="pp in products" :key="pp.sku" :label="pp.name" :value="pp.sku" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="form.qty" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="业务单据号">
          <el-input v-model="form.bizNo" placeholder="关联订单/运单号（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import { inventoryFlow, inventoryAdjust, inventoryList, productList, warehouseAll } from '../../api'

const dt = ref(null)
const warehouses = ref([])
const products = ref([])
const skuName = (sku) => { const p = products.value.find((x) => String(x.sku) === String(sku)); return p ? p.name : (sku || '-') }
const warehouseName = (id) => { const w = warehouses.value.find((x) => String(x.id) === String(id)); return w ? w.warehouse_name : '-' }
const dialogVisible = ref(false)
const saving = ref(false)
const flowQuery = reactive({ sku: '', bizType: '', warehouseId: null })
let flowCache = null
async function loadFlows(params) {
  if (!flowCache) {
    const data = (await inventoryFlow({ page: 1, perPage: 5000 })) || {}
    flowCache = data.rows || []
  }
  const kw = String(params.sku || '').trim().toLowerCase()
  const rows = flowCache.filter((r) => {
    if (kw) {
      const text = [r.sku, skuName(r.sku), r.biz_no, warehouseName(r.warehouse_id)].map((v) => String(v || '').toLowerCase()).join(' ')
      if (!text.includes(kw)) return false
    }
    if (params.bizType && r.biz_type !== params.bizType) return false
    if (params.warehouseId && String(r.warehouse_id) !== String(params.warehouseId)) return false
    return true
  })
  const start = (params.page - 1) * params.perPage
  return { rows: rows.slice(start, start + params.perPage), count: rows.length }
}
const today = reactive({ in: 0, out: 0, other: 0 })

const typeMeta = {
  IN: { label: '入库', type: 'success' },
  OUT: { label: '出库', type: 'warning' },
  REFUND: { label: '退回', type: 'danger' },
  ADJUST: { label: '调整', type: 'primary' },
  STOCKTAKE_IN: { label: '盘盈入库', type: 'success' },
  STOCKTAKE_OUT: { label: '盘亏出库', type: 'danger' }
}
const isIncrease = (v) => ['IN', 'REFUND', 'STOCKTAKE_IN'].includes(v)
const isDecrease = (v) => ['OUT', 'STOCKTAKE_OUT'].includes(v)

const columns = [
  { prop: 'created_at', label: '时间', width: 170, type: 'datetime' },
  { prop: 'biz_type', label: '类型', width: 100 },
  { prop: 'qty', label: '数量', width: 90 },
  { prop: 'warehouse_id', label: '仓库', width: 150 },
  { prop: 'sku', label: '商品', minWidth: 180 },
  { prop: 'biz_no', label: '业务单据号', minWidth: 170 }
]

const form = reactive({ sku: '', bizType: 'IN', qty: 1, warehouseId: null, bizNo: '' })

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('仓库进出库登记与流水台账，库存自动增减')
  warehouses.value = (await warehouseAll()) || []
  const ps = await productList({ page: 1, perPage: 5000 }).catch(() => null)
  products.value = (ps && ps.rows) || []
  loadToday()
})

async function loadToday() {
  try {
    const data = (await inventoryFlow({ perPage: 200 })) || {}
    const rows = data.rows || []
    const todayStr = new Date().toISOString().slice(0, 10)
    today.in = 0
    today.out = 0
    today.other = 0
    for (const r of rows) {
      if (!r.created_at || String(r.created_at).slice(0, 10) !== todayStr) continue
      if (r.biz_type === 'IN') today.in += Number(r.qty || 0)
      else if (r.biz_type === 'OUT') today.out += Number(r.qty || 0)
      else today.other += Number(r.qty || 0)
    }
  } catch (e) { /* 已提示 */ }
}

function openAdjust() {
  form.sku = ''
  form.bizType = 'IN'
  form.qty = 1
  form.warehouseId = null
  form.bizNo = ''
  dialogVisible.value = true
}

async function save() {
  if (!form.sku) return ElMessage.warning('请输入 SKU')
  if (!form.warehouseId) return ElMessage.warning('请选择仓库')
  if (form.bizType === 'OUT') {
    try {
      const data = await inventoryList({ sku: form.sku, page: 1, perPage: 50 })
      const row = (data.rows || []).find((r) => String(r.warehouse_id) === String(form.warehouseId))
      const avail = row == null ? null : Number(row.on_hand || 0)
      if (avail != null && form.qty > avail) {
        return ElMessage.warning('该仓库可用库存为 ' + avail + ' 件，本次登记 ' + form.qty + ' 件将导致负库存，请核对后调整数量或选择其它仓库')
      }
    } catch (e) { /* 库存查询失败不阻断登记 */ }
  }
  saving.value = true
  try {
    await inventoryAdjust({
      sku: form.sku,
      bizType: form.bizType,
      qty: form.qty,
      warehouseId: form.warehouseId,
      bizNo: form.bizNo || null
    })
    const typeName = { IN: '入库', OUT: '出库', REFUND: '退货退回', ADJUST: '盘点调整' }[form.bizType] || form.bizType
    ElMessage.success('已登记：' + skuName(form.sku) + ' ' + typeName + ' ' + form.qty + ' 件，库存已更新')
    dialogVisible.value = false
    flowCache = null
    dt.value.reload()
    loadToday()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sio-cards {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}
.sio-card {
  flex: 1;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 20px;
}
.sio-num {
  font-size: 24px;
  font-weight: 700;
  color: #0891b2;
}
.sio-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.sio-out {
  color: #f56c6c;
}
.sio-tip { margin: -6px 0 16px; font-size: 13px; color: #0e7490; line-height: 1.7; }
</style>
