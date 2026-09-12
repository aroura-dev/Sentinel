<template>
  <DataTable ref="dt" :action-width="72" title="物流渠道管理（承运商 × 目的地 × 时效）" note="渠道 = 发往哪个目的地的哪种运输方式；价卡按渠道生效，停用后该渠道不能再用于下单。" :columns="columns" :load="tmsChannelList" :query="query">
    <template #query="{ reload }">
      <el-select v-model="query.destCountry" placeholder="目的地" clearable style="width: 150px" @change="reload">
        <el-option v-for="c in COUNTRY_CODES" :key="c" :label="countryLabel(c)" :value="c" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增渠道</el-button>
    </template>
    <template #cell-type="{ row }"><el-tag size="small">{{ typeLabel(row.type) }}</el-tag></template>
    <template #cell-dest_country="{ row }">{{ countryLabel(row.dest_country) }}</template>
    <template #cell-status="{ row }"><StatusTag :status="row.status" /></template>
    <template #actions="{ row }">
      <el-dropdown trigger="click" @command="(cmd) => onAction(row, cmd)">
        <el-button text class="row-more" aria-label="更多操作"><el-icon><MoreFilled /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit">编辑</el-dropdown-item>
            <el-dropdown-item command="del" divided>删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </DataTable>

  <el-dialog v-model="dialog" :title="form.id ? '编辑渠道' : '新增渠道'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="承运商" required>
        <el-select v-model="form.carrierId" filterable style="width: 100%">
          <el-option v-for="c in carriers" :key="c.id" :label="c.carrier_name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道编码"><el-input v-model="form.channelCode" placeholder="如 GD-EXPR" /></el-form-item>
      <el-form-item label="渠道名称" required><el-input v-model="form.channelName" /></el-form-item>
      <el-form-item label="运输方式">
        <el-select v-model="form.type">
          <el-option label="铁路" value="rail" /><el-option label="空运" value="air" />
          <el-option label="海运" value="sea" /><el-option label="快递" value="express" />
        </el-select>
      </el-form-item>
      <el-form-item label="目的地">
        <el-select v-model="form.destCountry">
          <el-option v-for="c in COUNTRY_CODES" :key="c" :label="countryLabel(c)" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="时效(天)">
        <el-input-number v-model="form.transitDaysMin" :min="1" /> ~
        <el-input-number v-model="form.transitDaysMax" :min="1" />
      </el-form-item>
      <el-form-item label="跟踪号前缀"><el-input v-model="form.trackingPrefix" style="width: 160px" /></el-form-item>
      <el-form-item label="最小计费重kg"><el-input-number v-model="form.minBillableWeightKg" :min="0" :precision="3" /></el-form-item>
      <el-form-item label="体积重系数"><el-input-number v-model="form.volDivisor" :min="1000" :step="500" /></el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status"><el-radio :label="1">启用</el-radio><el-radio :label="0">停用</el-radio></el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { tmsChannelList, tmsChannelSave, tmsChannelUpdate, tmsChannelDelete, carrierAll } from '../../api'
import { countryLabel, COUNTRY_CODES } from '../../utils/country'

const TYPE_LABEL = { rail: '铁路', air: '空运', sea: '海运', express: '快递' }
const typeLabel = (t) => TYPE_LABEL[t] || t
const columns = [
  { prop: 'channel_code', label: '编码', width: 120 },
  { prop: 'channel_name', label: '渠道名称', minWidth: 160 },
  { prop: 'carrier_name', label: '承运商', minWidth: 110 },
  { prop: 'type', label: '方式', width: 80 },
  { prop: 'dest_country', label: '目的地', width: 90 },
  { prop: 'transit_days_min', label: '时效', width: 80 },
  { prop: 'transit_days_max', label: '承诺(天)', width: 90 },
  { prop: 'status', label: '状态', width: 80, type: 'tag' }
]
const query = reactive({ destCountry: '' })
const dt = ref(null)
const dialog = ref(false)
const saving = ref(false)
const carriers = ref([])
const form = reactive({})

onMounted(async () => { carriers.value = (await carrierAll()) || [] })

function onAction(row, cmd) {
  if (cmd === 'edit') openDialog(row)
  else if (cmd === 'del') del(row)
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, {
    id: row.id, carrierId: row.carrier_id, channelCode: row.channel_code, channelName: row.channel_name,
    type: row.type, destCountry: row.dest_country, transitDaysMin: Number(row.transit_days_min),
    transitDaysMax: Number(row.transit_days_max), trackingPrefix: row.tracking_prefix,
    minBillableWeightKg: Number(row.min_billable_weight_kg ?? 0), volDivisor: Number(row.vol_divisor ?? 5000),
    status: Number(row.status ?? 1)
  })
  else Object.assign(form, { type: 'express', destCountry: 'GD', transitDaysMin: 5, transitDaysMax: 10, minBillableWeightKg: 0, volDivisor: 5000, status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.carrierId || !form.channelName) return ElMessage.warning('请填写承运商与渠道名称')
  saving.value = true
  try {
    if (form.id) await tmsChannelUpdate(form.id, form)
    else await tmsChannelSave(form)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除渠道「${row.channel_name}」？`, '提示', { type: 'warning' })
  await tmsChannelDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>
