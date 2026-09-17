<template>
  <DataTable ref="dt" selectable title="用户管理" note="统一维护账号资料、角色与启停状态；用户名用于登录，昵称用于界面展示。" :columns="columns" :load="loadUsers" :query="query" :action-width="88">
    <template #query="{ reload }">
      <el-input v-model="query.keyword" placeholder="用户名/昵称/手机号/邮箱" clearable style="width: 220px" @keyup.enter="reload" />
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px" @change="reload">
        <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="reload">
        <el-option label="启用" value="1" />
        <el-option label="停用" value="0" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button type="success" @click="openCreate">新增用户</el-button>
    </template>

    <template #cell-avatar="{ row }">
      <el-avatar :size="30" :src="row.avatar || undefined" :style="avatarStyle(row)">{{ userInitial(row) }}</el-avatar>
    </template>
    <template #cell-role="{ row }">
      <el-tag :type="(roleMeta[row.role] && roleMeta[row.role].type) || 'info'" size="small">{{ roleName(row.role) }}</el-tag>
    </template>
    <template #cell-status="{ row }">
      <el-tag :type="isEnabled(row.status) ? 'success' : 'danger'" size="small">{{ isEnabled(row.status) ? '启用' : '停用' }}</el-tag>
    </template>
    <template #actions="{ row }">
      <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
    </template>
  </DataTable>

  <el-drawer v-model="detailVisible" :title="detail ? '用户详情 · ' + detail.username : '用户详情'" size="480px" destroy-on-close>
    <template v-if="detail">
      <div class="user-profile">
        <el-avatar :size="64" :src="detail.avatar || undefined" :style="avatarStyle(detail)">{{ userInitial(detail) }}</el-avatar>
        <div>
          <div class="user-profile-name">{{ detail.nickname || '-' }}</div>
          <div class="user-profile-account">@{{ detail.username }}</div>
        </div>
      </div>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="用户名">{{ detail.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色"><el-tag :type="(roleMeta[detail.role] && roleMeta[detail.role].type) || 'info'" size="small">{{ roleName(detail.role) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="isEnabled(detail.status) ? 'success' : 'danger'" size="small">{{ isEnabled(detail.status) ? '启用' : '停用' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmtDateTime(detail.created_at) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ fmtDateTime(detail.updated_at) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="ud-actions">
        <el-button type="primary" @click="editFromDetail">编辑资料</el-button>
        <el-button type="warning" @click="resetFromDetail">重置密码</el-button>
        <el-button v-if="!isSelf(detail)" :type="isEnabled(detail.status) ? 'danger' : 'success'" plain @click="toggleFromDetail">{{ isEnabled(detail.status) ? '停用账号' : '启用账号' }}</el-button>
        <el-button v-else disabled>当前登录账号不可停用</el-button>
      </div>
    </template>
  </el-drawer>

  <div v-if="selected.length" class="batch-bar">
    <div class="batch-tip">已选择 <b>{{ selected.length }}</b> 个用户</div>
    <div class="batch-actions">
      <el-button size="small" type="success" :loading="saving" @click="batchToggle('1')">批量启用</el-button>
      <el-button size="small" type="danger" :disabled="!batchDisableable.length" @click="batchToggle('0')">批量停用</el-button>
      <el-button size="small" @click="dt.clearSelection()">取消选择</el-button>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="560px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px" label-position="left">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="!!editId" placeholder="字母开头，3-32 位" maxlength="32" style="width: 100%" />
      </el-form-item>
      <el-form-item v-if="!editId" label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="6-64 位" maxlength="64" style="width: 100%" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="真实姓名或常用显示名" maxlength="32" style="width: 100%" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="选填，11 位手机号" maxlength="11" style="width: 100%" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填，如 name@example.com" maxlength="128" style="width: 100%" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-switch v-model="form.status" active-value="1" inactive-value="0" active-text="启用" inactive-text="停用" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="resetVisible" :title="`重置密码 · ${resetName}`" width="440px">
    <el-form label-width="88px" label-position="left">
      <el-form-item label="新密码" required>
        <el-input v-model="resetPwd" type="password" show-password placeholder="至少 6 位" show-word-limit maxlength="64" style="width: 100%" />
      </el-form-item>
      <el-form-item label="确认密码" required>
        <el-input v-model="resetPwd2" type="password" show-password placeholder="再次输入新密码" maxlength="64" style="width: 100%" @keyup.enter="saveReset" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="resetVisible = false">取消</el-button>
      <el-button type="warning" :loading="saving" @click="saveReset">确认重置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, inject, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../../components/DataTable.vue'
import { fmtDateTime } from '../../utils/format'
import { userList, userRoles, userCreate, userUpdate, userToggle, userResetPwd } from '../../api'

const dt = ref(null)
const formRef = ref(null)
const roles = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const saving = ref(false)
const resetVisible = ref(false)
const resetId = ref(null)
const resetName = ref('')
const resetPwd = ref('')
const resetPwd2 = ref('')
const detailVisible = ref(false)
const detail = ref(null)
const query = reactive({ keyword: '', role: '', status: '' })
const me = localStorage.getItem('sentinel_username') || sessionStorage.getItem('sentinel_username') || ''

const selected = computed(() => (dt.value && dt.value.selectedRows) || [])
const isEnabled = (status) => String(status) === '1'
const batchDisableable = computed(() => selected.value.filter((r) => !(r.username === me && isEnabled(r.status))))

const roleMeta = {
  ADMIN: { name: '管理员', type: 'primary' },
  OPERATOR: { name: '运营', type: 'success' },
  CUSTOMER_SERVICE: { name: '客服', type: 'warning' },
  MERCHANT: { name: '商家', type: 'info' },
  FINANCE: { name: '财务', type: 'danger' }
}
const roleName = (code) => (roleMeta[code] && roleMeta[code].name) || code || '-'
const isSelf = (row) => row.username === me
const AVATAR_COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#f43f5e', '#14b8a6', '#6366f1']
const userInitial = (row) => String(row.nickname || row.username || '用').trim().slice(0, 1).toUpperCase()
const avatarStyle = (row) => {
  if (row && row.avatar) return {}
  const key = String((row && (row.username || row.nickname)) || '')
  let hash = 0
  for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) >>> 0
  return { background: AVATAR_COLORS[hash % AVATAR_COLORS.length], color: '#fff', fontWeight: 600 }
}

async function loadUsers(params) {
  const { page = 1, perPage = 10, ...query } = params || {}
  const result = await userList({ ...query, page: 1, perPage: 1000 })
  const rows = [...((result && result.rows) || [])].sort((a, b) => {
    const aAdmin = a.role === 'ADMIN'
    const bAdmin = b.role === 'ADMIN'
    if (aAdmin !== bAdmin) return aAdmin ? -1 : 1
    return Number(b.id || 0) - Number(a.id || 0)
  })
  const start = (Number(page) - 1) * Number(perPage)
  return { rows: rows.slice(start, start + Number(perPage)), count: rows.length }
}
const columns = [
  { prop: 'avatar', label: '头像', width: 68, align: 'center' },
  { prop: 'nickname', label: '用户名', minWidth: 130 },
  { prop: 'phone', label: '手机号', minWidth: 135 },
  { prop: 'email', label: '邮箱', minWidth: 190 },
  { prop: 'role', label: '角色', width: 105 },
  { prop: 'status', label: '状态', width: 86 },
  { prop: 'created_at', label: '创建时间', width: 170, type: 'datetime' }
]

const form = reactive({ username: '', password: '', nickname: '', phone: '', email: '', role: 'OPERATOR', status: '1' })

const validatePhone = (rule, value, callback) => {
  if (!value || /^1[3-9]\d{9}$/.test(value)) return callback()
  callback(new Error('请输入有效的 11 位手机号'))
}
const validateEmail = (rule, value, callback) => {
  if (!value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return callback()
  callback(new Error('请输入有效的邮箱地址'))
}
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9._-]{2,31}$/, message: '需字母开头，仅支持字母、数字、点号、下划线和短横线', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 64, message: '密码长度需为 6-64 位', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }, { min: 2, max: 32, message: '昵称长度需为 2-32 位', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('管理平台账号：维护完整资料、分配角色、重置密码、批量启停')
  roles.value = (await userRoles()) || []
})

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}
function editFromDetail() {
  detailVisible.value = false
  openEdit(detail.value)
}
function resetFromDetail() {
  detailVisible.value = false
  openReset(detail.value)
}
async function toggleFromDetail() {
  const row = detail.value
  detailVisible.value = false
  await toggle(row)
}

function resetForm() {
  Object.assign(form, { username: '', password: '', nickname: '', phone: '', email: '', role: 'OPERATOR', status: '1' })
  nextTick(() => formRef.value && formRef.value.clearValidate())
}
function openCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}
function openEdit(row) {
  editId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    nickname: row.nickname || '',
    phone: row.phone || '',
    email: row.email || '',
    role: row.role,
    status: String(row.status ?? '1')
  })
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  const payload = {
    nickname: form.nickname.trim(),
    phone: form.phone.trim(),
    email: form.email.trim(),
    role: form.role,
    status: form.status
  }
  saving.value = true
  try {
    if (editId.value) {
      await userUpdate(editId.value, payload)
      ElMessage.success('用户资料已更新')
    } else {
      await userCreate({ username: form.username.trim(), password: form.password, ...payload })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await dt.value.reload()
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  resetId.value = row.id
  resetName.value = row.username
  resetPwd.value = ''
  resetPwd2.value = ''
  resetVisible.value = true
}
async function saveReset() {
  if ((resetPwd.value || '').length < 6) return ElMessage.warning('新密码至少 6 位')
  if (resetPwd.value !== resetPwd2.value) return ElMessage.warning('两次输入的密码不一致')
  saving.value = true
  try {
    await userResetPwd(resetId.value, resetPwd.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  const action = isEnabled(row.status) ? '停用' : '启用'
  const guard = action === '停用' && row.role === 'ADMIN' ? '系统会自动保留至少一个启用的管理员。' : ''
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」？${guard}`, '提示', { type: 'warning' })
  const updated = await userToggle(row.id)
  row.status = updated && updated.status != null ? updated.status : (isEnabled(row.status) ? '0' : '1')
  ElMessage.success(`已${action}`)
  await dt.value.reload()
}

async function batchToggle(target) {
  const rows = target === '0' ? batchDisableable.value : selected.value
  if (!rows.length) return ElMessage.warning('请先勾选用户')
  await ElMessageBox.confirm(`确认将选中的 ${rows.length} 个用户${target === '0' ? '停用' : '启用'}？`, '批量操作', { type: 'warning' })
  saving.value = true
  try {
    for (const r of rows) {
      if (target === '0' && isSelf(r)) continue
      if ((target === '0' && isEnabled(r.status)) || (target === '1' && !isEnabled(r.status))) {
        const updated = await userToggle(r.id)
        r.status = updated && updated.status != null ? updated.status : target
      }
    }
    ElMessage.success('批量操作完成')
    await dt.value.reload()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.user-profile { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; padding: 14px; border: 1px solid #dcecf1; border-radius: 12px; background: #f6fafb; }
.user-profile-name { font-size: 17px; font-weight: 600; color: #1f2937; }
.user-profile-account { margin-top: 4px; font-size: 13px; color: #6b7280; }
.batch-bar { margin-top: 18px; padding: 12px 18px; background: #f6fafb; border: 1px solid #dcecf1; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.batch-tip { font-size: 13px; color: #57606a; }
.batch-tip b { color: #0891b2; font-size: 15px; }
.batch-actions { display: flex; gap: 8px; }
.ud-actions { margin-top: 18px; display: flex; gap: 10px; flex-wrap: wrap; }
</style>