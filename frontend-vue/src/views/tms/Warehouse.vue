<template>
  <DataTable ref="dt" :action-width="72" title="仓库管理" note="仓库是库存与发货的物理归属：同一 SKU 可分布在多个仓库，出入库与库存查询都按仓库维度记录。" :columns="columns" :load="warehouseList" :query="query">
    <template #query="{ reload }">
      <el-input v-model="query.keyword" placeholder="仓库编码/名称" clearable style="width: 220px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增仓库</el-button>
    </template>
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

  <el-dialog v-model="dialog" :title="form.id ? '编辑仓库' : '新增仓库'" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="仓库编码"><el-input v-model="form.warehouseCode" placeholder="如 WH-SZ" /></el-form-item>
      <el-form-item label="仓库名称" required><el-input v-model="form.warehouseName" /></el-form-item>
      <el-form-item label="国家"><el-input v-model="form.country" placeholder="CN" /></el-form-item>
      <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
      <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
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
import { warehouseList, warehouseSave, warehouseUpdate, warehouseDelete } from '../../api'

const columns = [
  { prop: 'warehouse_code', label: '编码', width: 110 },
  { prop: 'warehouse_name', label: '仓库名称', minWidth: 160 },
  { prop: 'country', label: '国家', width: 80 },
  { prop: 'city', label: '城市', width: 100 },
  { prop: 'address', label: '地址', minWidth: 200 },
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
    id: row.id, warehouseCode: row.warehouse_code, warehouseName: row.warehouse_name,
    country: row.country, city: row.city, address: row.address, status: Number(row.status ?? 1)
  })
  else Object.assign(form, { country: 'CN', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.warehouseName) return ElMessage.warning('请填写仓库名称')
  saving.value = true
  try {
    if (form.id) await warehouseUpdate(form.id, form)
    else await warehouseSave(form)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除仓库「${row.warehouse_name}」？`, '提示', { type: 'warning' })
  await warehouseDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>
