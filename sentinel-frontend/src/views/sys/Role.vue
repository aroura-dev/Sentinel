<template>
  <div>
    <div class="role-note">配置各角色可访问的模块与页面；保存后，该角色成员重新登录即可按新权限使用。</div>
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <b>角色权限</b>
          <el-button type="primary" size="small" @click="openCreate">新增角色</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column :resizable="false" align="left" prop="code" label="角色编码" width="160" />
        <el-table-column :resizable="false" prop="name" label="角色名" width="140" />
        <el-table-column :resizable="false" label="成员数" width="90" align="center">
          <template #default="{ row }"><b>{{ membersMap[row.code] ?? 0 }}</b> 人</template>
        </el-table-column>
        <el-table-column :resizable="false" prop="description" label="描述" min-width="180" />
        <el-table-column :resizable="false" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'" size="small">{{ row.status === '1' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" label="操作" width="88" align="center">
          <template #default="{ row }"><el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 配置权限：菜单勾选树 -->
    <el-dialog v-model="menuDialog" :title="`配置权限 · ${currentRole ? currentRole.name : ''}`" width="560px">
      <div class="tree-tip">当前角色共 <b>{{ membersCount }}</b> 名成员；权限保存后，成员重新登录即可生效。</div>
      <div class="tree-tools">
        <el-input v-model="treeKeyword" placeholder="搜索菜单名称 / 路径" clearable style="width: 220px" />
        <el-button size="small" @click="selectAllLeaves">全选叶子</el-button>
        <el-button size="small" @click="clearAll">清空</el-button>
      </div>
      <el-tree
        ref="treeRef"
        :data="MENU"
        node-key="index"
        :props="{ label: 'title', children: 'children' }"
        show-checkbox
        default-expand-all
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
      />
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenus">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色详情：点“详情”后在此选择操作 -->
    <el-drawer v-model="detailVisible" :title="detailRole ? '角色详情 · ' + detailRole.name : '角色详情'" size="440px" destroy-on-close>
      <template v-if="detailRole">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="角色编码">{{ detailRole.code }}</el-descriptions-item>
          <el-descriptions-item label="角色名">{{ detailRole.name }}</el-descriptions-item>
          <el-descriptions-item label="成员数"><b>{{ membersMap[detailRole.code] ?? 0 }}</b> 人</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detailRole.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="detailRole.status === '1' ? 'success' : 'danger'" size="small">{{ detailRole.status === '1' ? '启用' : '停用' }}</el-tag></el-descriptions-item>
        </el-descriptions>
        <div class="rd-actions">
          <el-button type="primary" @click="permFromDetail">配置权限</el-button>
          <el-button @click="editFromDetail">编辑角色</el-button>
          <el-button v-if="detailRole.code !== 'ADMIN'" :type="detailRole.status === '1' ? 'danger' : 'success'" plain @click="toggleFromDetail">{{ detailRole.status === '1' ? '停用角色' : '启用角色' }}</el-button>
          <el-button v-else disabled>内置角色 · 保持启用</el-button>
        </div>
      </template>
    </el-drawer>
    <!-- 新增/编辑角色 -->
    <el-dialog v-model="roleDialog" :title="editId ? '编辑角色' : '新增角色'" width="500px">
      <el-form label-width="96px" label-position="left">
        <el-form-item label="角色编码">
          <el-input v-model="form.code" :disabled="!!editId" placeholder="如 AUDITOR（大写）" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色名">
          <el-input v-model="form.name" placeholder="如 审计员" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="该角色的职责说明" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MENU } from '../../utils/menu'
import { roleList, roleCreate, roleUpdate, roleToggle, roleMenus, roleSaveMenus, userList } from '../../api'

const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const membersMap = reactive({})
const membersCount = ref(0)

const menuDialog = ref(false)
const treeRef = ref(null)
const treeKeyword = ref('')
const currentRole = ref(null)
const detailVisible = ref(false)
const detailRole = ref(null)

const roleDialog = ref(false)
const editId = ref(null)
const form = ref({ code: '', name: '', description: '' })

const setDesc = inject('setPageDesc')

function leafPaths(nodes = MENU) {
  const out = []
  const walk = (list) => list.forEach((n) => {
    if (n.children && n.children.length) walk(n.children)
    else out.push(n.index)
  })
  walk(nodes)
  return out
}
function filterNode(value, data) {
  if (!value) return true
  return (data.title || '').includes(value) || (data.index || '').includes(value)
}

onMounted(async () => {
  setDesc('配置角色与菜单权限：谁能看哪个菜单，保存后重新登录生效')
  load()
})

async function load() {
  loading.value = true
  try {
    rows.value = (await roleList()) || []
    for (const r of rows.value) {
      try {
        const d = (await userList({ role: r.code, page: 1, perPage: 1 })) || {}
        membersMap[r.code] = d.count || 0
      } catch {
        membersMap[r.code] = 0
      }
    }
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detailRole.value = row
  detailVisible.value = true
}
function permFromDetail() {
  detailVisible.value = false
  openMenus(detailRole.value)
}
function editFromDetail() {
  detailVisible.value = false
  openEdit(detailRole.value)
}
async function toggleFromDetail() {
  const row = detailRole.value
  detailVisible.value = false
  await toggle(row)
}

function onManage(row, cmd) {
  if (cmd === 'perm') openMenus(row)
  else if (cmd === 'edit') openEdit(row)
  else if (cmd === 'toggle') toggle(row)
}

async function openMenus(row) {
  currentRole.value = row
  menuDialog.value = true
  membersCount.value = membersMap[row.code] ?? 0
  await nextTick()
  treeRef.value.filter('')
  treeKeyword.value = ''
  const data = await roleMenus(row.code)
  treeRef.value.setCheckedKeys((data && data.paths) || [])
}

async function saveMenus() {
  saving.value = true
  try {
    const keys = (treeRef.value.getCheckedKeys() || []).filter((k) => String(k).startsWith('/'))
    await roleSaveMenus(currentRole.value.code, keys)
    ElMessage.success(`权限已保存（影响 ${membersCount.value} 个成员）`)
    menuDialog.value = false
  } finally {
    saving.value = false
  }
}

function selectAllLeaves() {
  treeRef.value.setCheckedKeys(leafPaths())
}
function clearAll() {
  treeRef.value.setCheckedKeys([])
}

function openCreate() {
  editId.value = null
  form.value = { code: '', name: '', description: '' }
  roleDialog.value = true
}
function openEdit(row) {
  editId.value = row.id
  form.value = { code: row.code, name: row.name, description: row.description || '' }
  roleDialog.value = true
}
async function saveRole() {
  saving.value = true
  try {
    if (editId.value) {
      await roleUpdate(editId.value, { name: form.value.name, description: form.value.description })
      ElMessage.success('角色已更新')
    } else {
      await roleCreate(form.value)
      ElMessage.success('角色已创建')
    }
    roleDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function toggle(row) {
  const action = row.status === '1' ? '停用' : '启用'
  const count = membersMap[row.code] ?? 0
  const extra = count > 0 ? `该角色当前有 ${count} 个成员，停用后他们将无法登录对应菜单。` : ''
  await ElMessageBox.confirm(`确认${action}角色「${row.name}」？${extra}`, '提示', { type: 'warning' })
  await roleToggle(row.id)
  ElMessage.success(`已${action}`)
  load()
}
</script>

<style scoped>
.role-note { margin-bottom: 14px; padding: 10px 14px; font-size: 13px; color: #0e7490; background: #eef8fa; border: 1px solid #d9f0f4; border-radius: 8px; line-height: 1.7; }
.tree-tip {
  font-size: 12px;
  color: #606266;
  background: #f2f6fa;
  border: 1px solid #e2e9f0;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
  line-height: 1.7;
}
.tree-tip b { color: #1d2129; }
.tree-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}
.rd-actions { margin-top: 18px; display: flex; gap: 10px; flex-wrap: wrap; }
</style>