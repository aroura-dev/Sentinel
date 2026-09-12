<template>
  <DataTable ref="dt" :action-width="72" title="承运商管理" note="承运商是实际承运方：渠道与价卡都挂在承运商下，停用前请确认没有在途订单依赖。" :columns="columns" :load="carrierList" :query="query">
    <template #query="{ reload }">
      <el-input v-model="query.keyword" placeholder="承运商编码/名称" clearable style="width: 220px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增承运商</el-button>
    </template>
    <template #cell-type="{ row }"><el-tag size="small">{{ typeLabel(row.type) }}</el-tag></template>
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

  <el-dialog v-model="dialog" :title="form.id ? '编辑承运商' : '新增承运商'" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="编码"><el-input v-model="form.carrierCode" placeholder="如 CARGOWAY" /></el-form-item>
      <el-form-item label="名称" required><el-input v-model="form.carrierName" /></el-form-item>
      <el-form-item label="默认运输">
        <el-select v-model="form.type">
          <el-option label="铁路" value="rail" /><el-option label="空运" value="air" />
          <el-option label="海运" value="sea" /><el-option label="快递" value="express" />
        </el-select>
      </el-form-item>
      <el-form-item label="国家"><el-input v-model="form.country" placeholder="CN" /></el-form-item>
      <el-form-item label="对接地址"><el-input v-model="form.apiEndpoint" placeholder="预留轨迹对接接口" /></el-form-item>
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
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import StatusTag from '../../components/StatusTag.vue'
import { carrierList, carrierSave, carrierUpdate, carrierDelete } from '../../api'

const TYPE_LABEL = { rail: '铁路', air: '空运', sea: '海运', express: '快递' }
const typeLabel = (t) => TYPE_LABEL[t] || t
const columns = [
  { prop: 'carrier_code', label: '编码', width: 110 },
  { prop: 'carrier_name', label: '承运商名称', minWidth: 160 },
  { prop: 'type', label: '运输方式', width: 100 },
  { prop: 'country', label: '国家', width: 80 },
  { prop: 'status', label: '状态', width: 80, type: 'tag' }
]
const query = reactive({ keyword: '' })
const dt = ref(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})

function onAction(row, cmd) {
  if (cmd === 'edit') openDialog(row)
  else if (cmd === 'del') del(row)
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, {
    id: row.id, carrierCode: row.carrier_code, carrierName: row.carrier_name,
    type: row.type, country: row.country, apiEndpoint: row.api_endpoint, status: Number(row.status ?? 1)
  })
  else Object.assign(form, { type: 'air', country: 'CN', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.carrierName) return ElMessage.warning('请填写承运商名称')
  saving.value = true
  try {
    if (form.id) await carrierUpdate(form.id, form)
    else await carrierSave(form)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除承运商「${row.carrier_name}」？`, '提示', { type: 'warning' })
  await carrierDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>
