<template>
  <DataTable ref="dt" :action-width="72" title="商品管理（商品编码）" note="商品是订单履约的最小单位，SKU 全局唯一；货值、重量与体积会直接影响运费和申报。" :columns="columns" :load="productList" :query="query">
    <template #query="{ reload }">
      <el-input v-model="query.keyword" placeholder="商品编码/名称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openDialog()">新增商品</el-button>
    </template>
    <template #cell-image_url="{ row }">
      <el-image
        v-if="row.image_url"
        :src="row.image_url"
        fit="cover"
        style="width: 44px; height: 44px; border-radius: 6px; vertical-align: middle"
        :preview-src-list="[row.image_url]"
        preview-teleported
      />
      <span v-else style="color: #c0c4cc; font-size: 12px">无图</span>
    </template>
    <template #cell-merchant_id="{ row }">{{ merchantName(row.merchant_id) }}</template>
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

  <el-dialog v-model="dialog" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="所属商家" required>
        <el-select v-model="form.merchantId" filterable style="width: 100%">
          <el-option v-for="m in merchants" :key="m.id" :label="`${m.merchant_name} (${m.merchant_code})`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品编码" required><el-input v-model="form.sku" placeholder="全局唯一，如 SKU-LJ-001" /></el-form-item>
      <el-form-item label="商品名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="商品编码"><el-input v-model="form.hsCode" placeholder="如 8471.30" /></el-form-item>
      <el-form-item label="货值"><el-input-number v-model="form.declaredValue" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="币种"><el-input v-model="form.currency" style="width: 100px" /></el-form-item>
      <el-form-item label="单件实重kg" required><el-input-number v-model="form.weightKg" :min="0" :precision="3" /></el-form-item>
      <el-form-item label="单件体积L"><el-input-number v-model="form.volumeL" :min="0" :precision="3" /></el-form-item>
      <el-form-item label="产地"><el-input v-model="form.originCountry" style="width: 100px" /></el-form-item>
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
import { productList, productSave, productUpdate, productDelete, merchantAll } from '../../api'

const columns = [
  { prop: 'image_url', label: '图片', width: 76 },
  { prop: 'merchant_id', label: '商家', width: 130 },
  { prop: 'sku', label: '商品编码', width: 130 },
  { prop: 'name', label: '商品名称', minWidth: 160 },
  { prop: 'hs_code', label: '商品编码', width: 100 },
  { prop: 'declared_value', label: '货值', width: 100, type: 'money' },
  { prop: 'weight_kg', label: '实重kg', width: 90 },
  { prop: 'volume_l', label: '体积L', width: 90 },
  { prop: 'status', label: '状态', width: 80, type: 'tag' }
]
const query = reactive({ keyword: '' })
const dt = ref(null)
const dialog = ref(false)
const saving = ref(false)
const merchants = ref([])
const merchantName = (id) => { const m = merchants.value.find((x) => String(x.id) === String(id)); return m ? (m.merchant_name || '-') : '-' }
const form = reactive({})

onMounted(async () => { merchants.value = (await merchantAll()) || [] })

function onAction(row, cmd) {
  if (cmd === 'edit') openDialog(row)
  else if (cmd === 'del') del(row)
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, {
    id: row.id, merchantId: row.merchant_id, sku: row.sku, name: row.name, hsCode: row.hs_code,
    declaredValue: Number(row.declared_value ?? 0), currency: row.currency, weightKg: Number(row.weight_kg),
    volumeL: Number(row.volume_l ?? 0), originCountry: row.origin_country, status: Number(row.status ?? 1)
  })
  else Object.assign(form, { currency: 'CNY', declaredValue: 0, weightKg: 0, volumeL: 0, originCountry: 'CN', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.merchantId || !form.sku || !form.name) return ElMessage.warning('请填写商家/SKU/名称')
  saving.value = true
  try {
    if (form.id) await productUpdate(form.id, form)
    else await productSave(form)
    ElMessage.success('保存成功')
    dialog.value = false
    dt.value.reload()
  } finally { saving.value = false }
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」？`, '提示', { type: 'warning' })
  await productDelete(row.id)
  ElMessage.success('已删除')
  dt.value.reload()
}
</script>
