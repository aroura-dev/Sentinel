<template>
  <div>
    <div v-if="statsLoaded" class="fn-banner">
      <div class="fn-head"><el-icon :size="15"><PriceTag /></el-icon><span class="fn-title">运费报价总览</span><span class="fn-tip">价卡决定发货运费：先选渠道，再看目的地与重量段</span></div>
      <div class="fn-stats">
        <span class="fn-item">价卡总数 <b>{{ overview.total ?? 0 }}</b> 条</span>
        <span class="fn-item">启用 <b>{{ overview.enabled ?? 0 }}</b></span>
        <span class="fn-item">停用 <b>{{ overview.disabled ?? 0 }}</b></span>
        <span class="fn-item">覆盖渠道 <b>{{ overview.channels ?? 0 }}</b> 个</span>
      </div>
      <div class="fn-warn ok">区域填 DEFAULT 表示该渠道的默认价；调价会影响对账差异，建议修改后核对一次。</div>
    </div>
    <DataTable ref="dt" title="运费价卡管理（渠道 × 区域 × 重量段）" :columns="columns" :load="rateList" :query="query">
    <template #query="{ reload }">
      <el-select v-model="query.channelId" placeholder="渠道" filterable clearable style="width: 220px" @change="reload">
        <el-option v-for="c in channels" :key="c.id" :label="`${c.channel_name} (${c.channel_code})`" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增价卡</el-button>
    </template>
    <template #cell-channel_id="{ row }"><span>{{ channelName(row.channel_id) }}</span></template>
    <template #cell-mode="{ row }"><el-tag size="small" :type="row.mode === 'PER_KG' ? 'primary' : 'warning'">{{ modeLabel(row.mode) }}</el-tag></template>
    <template #cell-min_weight_kg="{ row }">{{ weightText(row) }}</template>
    <template #cell-price="{ row }"><span class="rate-price">{{ priceText(row) }}</span></template>
    <template #cell-zone="{ row }">{{ zoneLabel(row.zone) }}</template>
    <template #cell-status="{ row }"><StatusTag :status="row.status" /></template>
    <template #actions="{ row }">
      <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
      <el-button size="small" link type="danger" @click="del(row)">删除</el-button>
    </template>
  </DataTable>

  <el-dialog v-model="dialog" :title="form.id ? '编辑价卡' : '新增价卡'" width="620px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="渠道" required>
        <el-select v-model="form.channelId" filterable style="width: 100%">
          <el-option v-for="c in channels" :key="c.id" :label="`${c.channel_name} (${c.channel_code})`" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="区域"><el-input v-model="form.zone" placeholder="目的地或 DEFAULT" style="width: 200px" /></el-form-item>
      <el-form-item label="重量段">
        <el-input-number v-model="form.minWeightKg" :min="0" :precision="3" /> ~
        <el-input-number v-model="form.maxWeightKg" :min="0" :precision="3" placeholder="留空=上限开放" />
      </el-form-item>
      <el-form-item label="计费方式">
        <el-radio-group v-model="form.mode">
          <el-radio label="PER_KG">单价/kg</el-radio>
          <el-radio label="FIRST_CONTINUED">首续重</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="form.mode === 'FIRST_CONTINUED'">
        <el-form-item label="首重(kg)"><el-input-number v-model="form.firstWeightKg" :min="0" :precision="3" /></el-form-item>
        <el-form-item label="首重价格"><el-input-number v-model="form.firstPrice" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="续重(kg)"><el-input-number v-model="form.continuedWeightKg" :min="0" :precision="3" /></el-form-item>
        <el-form-item label="续重单价"><el-input-number v-model="form.continuedPrice" :min="0" :precision="4" /></el-form-item>
      </template>
      <el-form-item v-else label="单价(元/kg)"><el-input-number v-model="form.price" :min="0" :precision="4" /></el-form-item>
      <el-form-item label="币种"><el-input v-model="form.currency" style="width: 120px" /></el-form-item>
      <el-form-item label="生效日期"><el-input v-model="form.effectiveFrom" placeholder="yyyy-MM-dd" style="width: 160px" /></el-form-item>
      <el-form-item label="失效日期"><el-input v-model="form.effectiveTo" placeholder="留空=长期" style="width: 160px" /></el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status"><el-radio :label="1">启用</el-radio><el-radio :label="0">停用</el-radio></el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { rateList, rateSave, rateUpdate, rateDelete, tmsChannelAll } from '../../api'
import { zoneLabel } from '../../utils/country'

const columns = [
  { prop: 'channel_id', label: '渠道', width: 170 },
  { prop: 'zone', label: '区域', width: 100 },
  { prop: 'min_weight_kg', label: '重量段', width: 120 },
  { prop: 'mode', label: '计费方式', width: 110 },
  { prop: 'price', label: '计费价格', width: 150 },
  { prop: 'effective_from', label: '生效日期', width: 110, type: 'datetime' },
  { prop: 'effective_to', label: '失效日期', width: 110, type: 'datetime' },
  { prop: 'status', label: '状态', width: 80, type: 'tag' }
]
const query = reactive({ channelId: null })
const dt = ref(null)
const overview = ref({ total: 0, enabled: 0, disabled: 0, channels: 0 })
const statsLoaded = ref(false)
const dialog = ref(false)
const saving = ref(false)
const channels = ref([])
function channelName(id) { const c = channels.value.find((x) => String(x.id) === String(id)); return c ? c.channel_name : '-' }
function modeLabel(m) { return m === 'PER_KG' ? '单价/kg' : m === 'FIRST_CONTINUED' ? '首续重' : m }
function weightText(r) {
  const min = r.min_weight_kg == null ? 0 : Number(r.min_weight_kg)
  const max = r.max_weight_kg == null ? null : Number(r.max_weight_kg)
  return min + ' ~ ' + (max == null ? '不限' : max) + ' kg'
}
function trimNum(v) { return String(Number(v || 0).toFixed(4)).replace(/\.?0+$/, '') }
function priceText(r) {
  if (r.mode === 'PER_KG') return '¥' + trimNum(r.price) + ' / kg'
  return '首 ¥' + trimNum(r.first_price) + '，续 ¥' + trimNum(r.continued_price)
}
const form = reactive({})

onMounted(async () => {
  channels.value = (await tmsChannelAll()) || []
  try {
    const r = await rateList({ page: 1, perPage: 1000 })
    const rows = r.rows || []
    const enabled = rows.filter((x) => Number(x.status) === 1).length
    overview.value = { total: rows.length, enabled, disabled: rows.length - enabled, channels: new Set(rows.map((x) => String(x.channel_id))).size }
  } catch { /* 统计失败不阻断 */ }
  statsLoaded.value = true
})

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, {
    id: row.id, channelId: row.channel_id, zone: row.zone,
    minWeightKg: Number(row.min_weight_kg), maxWeightKg: row.max_weight_kg == null ? null : Number(row.max_weight_kg),
    mode: row.mode, firstWeightKg: Number(row.first_weight_kg ?? 0), firstPrice: Number(row.first_price ?? 0),
    continuedWeightKg: Number(row.continued_weight_kg ?? 0), continuedPrice: Number(row.continued_price ?? 0),
    price: Number(row.price ?? 0), currency: row.currency, effectiveFrom: String(row.effective_from).slice(0, 10),
    effectiveTo: row.effective_to ? String(row.effective_to).slice(0, 10) : '', status: Number(row.status ?? 1)
  })
  else Object.assign(form, { zone: 'DEFAULT', mode: 'PER_KG', minWeightKg: 0, maxWeightKg: null, price: 0, currency: 'CNY', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.channelId) return ElMessage.warning('请选择渠道')
  saving.value = true
  try {
    const payload = { ...form, effectiveTo: form.effectiveTo || null }
    if (form.id) await rateUpdate(form.id, payload)
    else await rateSave(payload)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm('确认删除该价卡？', '提示', { type: 'warning' })
  await rateDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>

<style scoped>
.rate-price { color: #b45309; font-weight: 600; }
.fn-banner {
  margin-bottom: 16px;
  padding: 14px 18px 12px 22px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.fn-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #06b6d4, #06748e);
}
.fn-head { display: flex; align-items: center; gap: 8px; }
.fn-head .el-icon { color: #0891b2; }
.fn-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.fn-tip { font-size: 12px; color: #9ca3af; }
.fn-stats { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 26px; margin-top: 10px; }
.fn-item { font-size: 13px; color: #57606a; }
.fn-item b { font-size: 16px; color: #1d2129; margin-left: 2px; }
.fn-warn { margin-top: 10px; font-size: 13px; color: #b7791f; background: #fef6ec; border: 1px solid #f5e1c3; border-radius: 6px; padding: 7px 12px; }
.fn-warn b { color: #d97706; }
.fn-warn.ok { color: #0e7490; background: #eef8fa; border-color: #d9f0f4; }
</style>
