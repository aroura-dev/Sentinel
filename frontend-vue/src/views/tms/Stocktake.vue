<template>
  <div>
    <div class="stk-overview">
      <div class="ov-item"><span class="ov-k">盘点单</span><b class="ov-v">{{ overview.total }}</b></div>
      <div class="ov-item"><span class="ov-k">草稿</span><b class="ov-v">{{ overview.draft }}</b></div>
      <div class="ov-item"><span class="ov-k">已完成</span><b class="ov-v">{{ overview.done }}</b></div>
      <div class="ov-item"><span class="ov-k">已取消</span><b class="ov-v">{{ overview.canceled }}</b></div>
    </div>
    <div class="stk-note"><b>说明：</b>盘点用于将仓库实物数量与系统账目对齐。新建盘点单后逐商品录入实盘数量，系统自动计算差异；确认完成后自动生成库存调整流水并留痕，库存同步更新。</div>

    <el-card shadow="never">
      <div class="stk-toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="load">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">新建盘点</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small" :empty-text="emptyText" @row-click="openDetail">
        <el-table-column align="left" prop="stocktake_no" label="盘点单号" min-width="190" />
        <el-table-column prop="warehouse_name" label="仓库" min-width="110" />
        <el-table-column label="范围" width="100" align="center">
          <template #default="{ row }">{{ scopeLabel(row.scope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="已盘 / 差异" width="120" align="center">
          <template #default="{ row }">
            {{ row.total_sku || 0 }} / <span :class="Number(row.diff_sku) > 0 ? 'diff-warn' : ''">{{ row.diff_sku || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170" align="center">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" size="small" link type="primary" @click.stop="openDetail(row)">继续盘点</el-button>
            <el-button v-else size="small" link type="primary" @click.stop="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" link type="danger" @click.stop="cancel(row)">取消盘点</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
        :page-size="query.perPage" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />
    </el-card>

    <!-- 新建盘点 -->
    <el-dialog v-model="createOpen" title="新建盘点单" width="520px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="仓库" required>
          <el-select v-model="form.warehouseId" filterable style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouse_name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点范围">
          <el-radio-group v-model="form.scope">
            <el-radio label="ALL">该仓全部商品</el-radio>
            <el-radio label="MERCHANT">按商家</el-radio>
            <el-radio label="MANUAL">按商品指定</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scope === 'MERCHANT'" label="商家" required>
          <el-select v-model="form.merchantId" filterable style="width: 100%">
            <el-option v-for="m in merchants" :key="m.id" :label="m.merchant_name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.scope === 'MANUAL'" label="指定商品" required>
          <el-select v-model="form.skus" multiple filterable collapse-tags placeholder="搜索并选择要盘点的商品" style="width: 100%">
            <el-option v-for="pp in products" :key="pp.sku" :label="pp.name" :value="pp.sku" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="盘点事由等（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建并开始盘点</el-button>
      </template>
    </el-dialog>

    <!-- 盘点详情/录入 -->
    <el-drawer v-model="detailOpen" :title="detail ? detail.stocktake.stocktake_no + ' · 盘点详情' : '盘点详情'" size="720px">
      <template v-if="detail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="仓库">{{ detail.stocktake.warehouse_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="范围">{{ scopeLabel(detail.stocktake.scope) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag size="small" :type="statusMeta(detail.stocktake.status).type">{{ statusMeta(detail.stocktake.status).label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.stocktake.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmt(detail.stocktake.created_at) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.stocktake.finished_at" label="完成时间">{{ fmt(detail.stocktake.finished_at) }}</el-descriptions-item>
        </el-descriptions>

        <div class="stk-summary">
          已录实盘 <b>{{ countedTotal }}</b> / {{ detail.items.length }} 个商品 ·
          <span :class="diffTotal ? 'diff-warn' : ''">差异 {{ diffTotal }} 个商品</span>
          <span v-if="detail.stocktake.status === 'DRAFT'" style="color: #57606a">（录入后点“保存实盘”，全部核对后点“完成盘点”）</span>
        </div>

        <template v-if="detailError">
          <el-alert type="error" :closable="false" show-icon title="明细加载失败" :description="'错误原因：' + (detailErrorMsg || '请检查网络后重试')" style="margin: 10px 0" />
          <el-button size="small" type="primary" plain @click="openDetail(detail.stocktake)">重新加载</el-button>
        </template>
        <el-table v-else :data="detail.items" v-loading="itemsLoading" border stripe size="small" :empty-text="'该范围暂无库存可盘点'">
          <el-table-column label="商品" min-width="230">
            <template #default="{ row }">{{ row.product_name || row.sku }}</template>
          </el-table-column>
          <el-table-column prop="expected" label="账面数量" width="100" align="center" />
          <el-table-column label="实盘数量" width="150" align="center">
            <template #default="{ row }">
              <el-input-number v-if="detail.stocktake.status === 'DRAFT'" v-model="row.counted" :min="0" :controls="false" size="small" @change="() => saveCounts()" />
              <span v-else>{{ row.counted == null ? '-' : row.counted }}</span>
            </template>
          </el-table-column>
          <el-table-column label="差异" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.counted != null" :class="diffClass(row.counted - row.expected)">
                {{ row.counted - row.expected === 0 ? '0' : (row.counted - row.expected > 0 ? '+' : '') + (row.counted - row.expected) }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

      </template>
      <div v-else class="dr-loading">加载中…</div>
      <template #footer>
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <el-button @click="detailOpen = false">关闭</el-button>
          <el-button v-if="detail && detail.stocktake.status === 'DRAFT'" type="warning" @click="cancel(detail.stocktake)">取消盘点</el-button>
          <el-button v-if="detail && detail.stocktake.status === 'DRAFT'" type="success" :loading="finishing" @click="finish">完成盘点并入账</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fmtDateTime } from '../../utils/format'
import { stocktakeList, stocktakeDetail, stocktakeCreate, stocktakeSaveCount, stocktakeFinish, stocktakeCancel, warehouseAll, merchantAll, productList } from '../../api'

const setDesc = inject('setPageDesc')

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ status: '', page: 1, perPage: 10 })
const warehouses = ref([])
const merchants = ref([])
const products = ref([])
const overview = ref({ total: 0, draft: 0, done: 0, canceled: 0 })
const emptyText = '当前没有符合条件的盘点单，点「新建盘点」开始第一次盘点'

const scopeLabel = (v) => ({ ALL: '该仓全部商品', MERCHANT: '按商家', MANUAL: '按商品指定' }[v] || v || '-')
const statusMeta = (s) => ({ DRAFT: { label: '草稿', type: 'warning' }, DONE: { label: '已完成', type: 'success' }, CANCELED: { label: '已取消', type: 'info' } }[s] || { label: s, type: 'info' })
const fmt = fmtDateTime
const diffClass = (d) => (d === 0 ? 'diff-zero' : d > 0 ? 'diff-up' : 'diff-down')

const createOpen = ref(false)
const creating = ref(false)
const form = reactive({ warehouseId: null, scope: 'ALL', merchantId: null, skus: [], remark: '' })

const detailOpen = ref(false)
const detail = ref(null)
const itemsLoading = ref(false)
const detailError = ref(false)
const detailErrorMsg = ref('')
const finishing = ref(false)

const countedTotal = computed(() => (detail.value ? detail.value.items.filter((i) => i.counted != null).length : 0))
const diffTotal = computed(() => (detail.value ? detail.value.items.filter((i) => i.counted != null && Number(i.counted) !== Number(i.expected)).length : 0))

async function load() {
  loading.value = true
  try {
    const data = await stocktakeList({ ...query })
    rows.value = data.rows || []
    total.value = data.count || 0
  } finally { loading.value = false }
}

async function loadOverview() {
  try {
    const data = await stocktakeList({ page: 1, perPage: 500 })
    const all = data.rows || []
    overview.value = {
      total: all.length,
      draft: all.filter((r) => r.status === 'DRAFT').length,
      done: all.filter((r) => r.status === 'DONE').length,
      canceled: all.filter((r) => r.status === 'CANCELED').length
    }
  } catch (e) { /* 忽略 */ }
}

function openCreate() {
  Object.assign(form, { warehouseId: null, scope: 'ALL', merchantId: null, skus: [], remark: '' })
  createOpen.value = true
}

async function doCreate() {
  if (!form.warehouseId) return ElMessage.warning('请选择仓库')
  if (form.scope === 'MERCHANT' && !form.merchantId) return ElMessage.warning('请选择商家')
  let skus = []
  if (form.scope === 'MANUAL') {
    skus = (form.skus || []).map((s) => String(s))
    if (!skus.length) return ElMessage.warning('请选择至少一个要盘点的商品')
  }
  creating.value = true
  try {
    const data = await stocktakeCreate({
      warehouseId: form.warehouseId,
      scope: form.scope,
      merchantId: form.scope === 'MERCHANT' ? form.merchantId : null,
      skus,
      remark: form.remark || null
    })
    ElMessage.success('盘点单已创建，可开始录入实盘')
    createOpen.value = false
    await load()
    await loadOverview()
    detail.value = data
    detailOpen.value = true
  } finally { creating.value = false }
}

async function openDetail(row, isRetry) {
  detailOpen.value = true
  // 先用列表行数据立即渲染头部，明细再异步加载，避免长时间空白
  detail.value = { stocktake: row, items: [] }
  itemsLoading.value = true
  detailError.value = false
  const loadOnce = async () => stocktakeDetail(row.id)
  try {
    detail.value = await loadOnce()
  } catch (e1) {
    // 首次失败：等待 1 秒自动重试一次（后端刚重启/代理抖动时可自愈）
    try {
      await new Promise((r) => setTimeout(r, 1000))
      detail.value = await loadOnce()
    } catch (e2) {
      const e = e2 || e1
      detailError.value = true
      const status = e && e.response ? e.response.status : ''
      detailErrorMsg.value = (status ? 'HTTP ' + status + '：' : '') + ((e && (e.message || e.msg)) || '请求失败')
      console.error('stocktakeDetail error:', e)
    }
  } finally {
    itemsLoading.value = false
  }
}

async function saveCounts() {
  if (!detail.value || detail.value.stocktake.status !== 'DRAFT') return
  const items = detail.value.items.map((i) => ({ sku: i.sku, counted: i.counted == null ? null : Number(i.counted) })).filter((i) => i.counted != null)
  try {
    await stocktakeSaveCount(detail.value.stocktake.id, { items })
  } catch (e) { /* 提示由请求层统一处理 */ }
}

async function finish() {
  if (!detail.value) return
  const d = detail.value
  const filled = d.items.filter((i) => i.counted != null).length
  if (!filled) return ElMessage.warning('请先录入实盘数量，再完成盘点')
  const diffN = d.items.filter((i) => i.counted != null && Number(i.counted) !== Number(i.expected)).length
  await ElMessageBox.confirm(
    '将按实盘与账面的差异生成库存调整并留痕，确认完成盘点？' + (diffN ? '（存在 ' + diffN + ' 个差异商品）' : '（全部一致）'),
    '完成盘点', { type: 'warning' }
  )
  finishing.value = true
  try {
    await stocktakeFinish(d.stocktake.id)
    ElMessage.success('盘点已完成，差异已入账，库存已更新')
    detail.value = await stocktakeDetail(d.stocktake.id)
    await load()
    await loadOverview()
  } finally { finishing.value = false }
}

async function cancel(row) {
  await ElMessageBox.confirm('确认取消该盘点单？录入的实盘数据将保留但不入账。', '取消盘点', { type: 'warning' })
  await stocktakeCancel(row.id)
  ElMessage.success('盘点单已取消')
  await load()
  await loadOverview()
  if (detail.value && detail.value.stocktake && detail.value.stocktake.id === row.id) {
    detail.value = await stocktakeDetail(row.id)
  }
}

onMounted(async () => {
  setDesc('将仓库实物数量与系统账目对齐：盘点录入、差异确认与库存调整')
  load()
  loadOverview()
  warehouseAll().then((ws) => { warehouses.value = ws || [] }).catch(() => {})
  merchantAll().then((ms) => { merchants.value = ms || [] }).catch(() => {})
  productList({ page: 1, perPage: 5000 }).then((ps) => { products.value = (ps && ps.rows) || [] }).catch(() => {})
})
</script>

<style scoped>
.stk-overview { display: flex; flex-wrap: wrap; gap: 10px 36px; margin-bottom: 14px; padding: 12px 18px; background: #fff; border: 1px solid #e5e6eb; border-radius: 10px; }
.ov-item { display: flex; align-items: baseline; gap: 6px; }
.ov-k { font-size: 13px; color: #57606a; }
.ov-v { font-size: 20px; color: #1d2129; }
.stk-note { margin-bottom: 16px; padding: 10px 14px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.7; }
.stk-toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
.pager { margin-top: 14px; justify-content: flex-end; }
.stk-summary { margin: 16px 0 10px; font-size: 13px; color: #1d2129; }
.diff-warn { color: #d97706; font-weight: 600; }
.diff-zero { color: #10b981; }
.diff-up { color: #d97706; }
.diff-down { color: #ef4444; }
.dr-loading { color: #9ca3af; text-align: center; padding: 40px 0; }
:deep(.el-table__row) { cursor: pointer; }
</style>