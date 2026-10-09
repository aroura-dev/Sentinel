<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="类型">
          <el-input v-model="query.type" placeholder="如 customs_delay" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="openAdd">新增知识条目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="status_code" label="状态码" width="120" />
        <el-table-column prop="type" label="类型" width="140" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column prop="avg_duration_hours" label="平均滞留(h)" width="110" />
        <el-table-column prop="suggestion" label="处理建议" min-width="260" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.perPage"
        :current-page="query.page"
        @current-change="onPageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增知识条目" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="状态码"><el-input v-model="form.statusCode" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.type" placeholder="customs_delay/lost/..." /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="平均滞留(h)"><el-input-number v-model="form.avgDurationHours" :min="0" /></el-form-item>
        <el-form-item label="处理建议"><el-input v-model="form.suggestion" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeList, knowledgeSave, knowledgeDelete } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ type: '', page: 1, perPage: 10 })
const dialogVisible = ref(false)
const form = ref({ statusCode: '', type: '', description: '', avgDurationHours: 24, suggestion: '' })

async function load() {
  loading.value = true
  try {
    const data = await knowledgeList(query.value)
    rows.value = data.rows || []
    total.value = data.count || 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  query.value.page = p
  load()
}

function openAdd() {
  form.value = { statusCode: '', type: '', description: '', avgDurationHours: 24, suggestion: '' }
  dialogVisible.value = true
}

async function save() {
  await knowledgeSave(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(row) {
  await knowledgeDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
