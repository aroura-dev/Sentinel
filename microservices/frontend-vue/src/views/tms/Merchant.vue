<template>
  <DataTable ref="dt" :action-width="72" title="商家管理（卖家主数据）" note="商家是货主（卖家）主数据：新增商家后，订单、对账、售后都按商家归属；请保持编码与名称规范。" :columns="columns" :load="merchantList" :query="query">
    <template #query="{ reload }">
      <el-input v-model="query.keyword" placeholder="商家编码/名称" clearable style="width: 220px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增商家</el-button>
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

  <el-dialog v-model="dialog" :title="form.id ? '编辑商家' : '新增商家'" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="商家编码"><el-input v-model="form.merchantCode" placeholder="如 MCH-0005" /></el-form-item>
      <el-form-item label="商家名称" required><el-input v-model="form.merchantName" /></el-form-item>
      <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
      <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
      <el-form-item label="联系邮箱"><el-input v-model="form.contactEmail" /></el-form-item>
      <el-form-item label="所在国家"><el-input v-model="form.country" placeholder="CN" /></el-form-item>
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
import { merchantList, merchantSave, merchantUpdate, merchantDelete } from '../../api'

const columns = [
  { prop: 'merchant_code', label: '商家编码', width: 120 },
  { prop: 'merchant_name', label: '商家名称', minWidth: 140 },
  { prop: 'contact_name', label: '联系人', width: 100 },
  { prop: 'contact_phone', label: '联系电话', width: 130 },
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
    id: row.id, merchantCode: row.merchant_code, merchantName: row.merchant_name,
    contactName: row.contact_name, contactPhone: row.contact_phone, contactEmail: row.contact_email,
    country: row.country, status: Number(row.status ?? 1)
  })
  else Object.assign(form, { status: 1, country: 'CN' })
  dialog.value = true
}

async function save() {
  if (!form.merchantName) return ElMessage.warning('请填写商家名称')
  saving.value = true
  try {
    if (form.id) await merchantUpdate(form.id, form)
    else await merchantSave(form)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除商家「${row.merchant_name}」？`, '提示', { type: 'warning' })
  await merchantDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>
