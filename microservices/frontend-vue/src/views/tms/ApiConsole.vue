<template>
  <div>
    <div class="ak-note">
      <b>开放接口凭证管理</b>：为对接系统（ERP / OMS / 电商平台 / 物流承运商）签发 API 调用凭证，按授权范围调用开放接口。
      凭证由 <code>API Key</code> 与 <code>Secret</code> 组成，<b>Secret 仅在创建时展示一次</b>，请妥善保存；凭证停用后调用立即暂停，吊销后永久失效且不可恢复。
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="ak-card-head">
          <div>
            <b>开放接口凭证</b>
            <span class="ak-card-sub">共 {{ keys.length }} 个对接系统</span>
          </div>
          <el-button type="primary" @click="openCreate">新建凭证</el-button>
        </div>
      </template>

            <el-table :data="keys" v-loading="loading" border stripe>
        <el-table-column label="对接方" min-width="220">
          <template #default="{ row }">
            <div class="ak-primary">{{ displayName(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="AppKey" min-width="200">
          <template #default="{ row }"><code class="ak-key">{{ maskKey(row.api_key) }}</code></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="ak-status" :class="row.status === 1 ? 'on' : 'off'"><i></i>{{ row.status === 1 ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <div class="ak-primary">{{ fmtTime(row.created_at) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">管理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="ak-card-head">
          <div>
            <b>开放接口与权限说明</b>
            <span class="ak-card-sub">授权范围决定可调用的开放接口</span>
          </div>
        </div>
      </template>
      <div class="cap-list">
        <div v-for="d in docs" :key="d.method + d.path" class="cap">
          <div class="cap-head">
            <span class="doc-method" :class="'m-' + d.method.toLowerCase()">{{ d.method }}</span>
            <code class="cap-path">{{ d.path }}</code>
            <span class="cap-scope" v-if="capScope(d.desc)">{{ capScope(d.desc) }}</span>
            <span class="cap-name">{{ capName(d.desc) }}</span>
          </div>
        </div>
      </div>
      <el-alert type="info" :closable="false" style="margin-top: 12px">
        调用方式：在请求头中携带 <code>Authorization: Bearer {api_key}</code>，按授权范围访问；未获授权的接口将返回 403 状态码。
      </el-alert>
    </el-card>

    <!-- 凭证详情 -->
    <el-drawer v-model="detailVisible" :title="detailKey ? '凭证管理 · ' + detailKey.app_name : '凭证管理'" size="460px" destroy-on-close>
      <template v-if="detailKey">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="对接系统">{{ detailKey.app_name }}</el-descriptions-item>
          <el-descriptions-item label="所属企业">{{ detailKey.company || '-' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailKey.contact_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailKey.contact_phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ detailKey.contact_email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="授权范围">
            <template v-if="scopesOf(detailKey.scope).length">
              <el-tag v-for="s in scopesOf(detailKey.scope)" :key="s" size="small" type="info" style="margin-right: 4px">{{ s }}</el-tag>
            </template>
            <span v-else>order:read（默认）</span>
          </el-descriptions-item>
          <el-descriptions-item label="用途说明">{{ detailKey.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="API Key">
            <code style="font-size: 12px; word-break: break-all">{{ detailKey.api_key }}</code>
            <el-button size="small" link type="primary" @click="copyKey(detailKey.api_key)">复制</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="凭证状态">
            <el-tag size="small" :type="detailKey.status === 1 ? 'success' : 'info'">{{ detailKey.status === 1 ? '启用' : '停用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建信息">{{ fmtTime(detailKey.created_at) }} · {{ detailKey.created_by || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="warning" :closable="false" style="margin-top: 14px">
          Secret 不支持二次查看。如已遗失，请吊销当前凭证并重新创建。
        </el-alert>
        <div class="kd-actions">
          <el-button :type="detailKey.status === 1 ? 'warning' : 'success'" plain @click="toggleFromDetail">
            {{ detailKey.status === 1 ? '停用（暂停调用）' : '启用（恢复调用）' }}
          </el-button>
          <el-button type="danger" plain @click="revokeFromDetail">吊销凭证</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 新建凭证 -->
    <el-dialog v-model="createDialog" :title="created ? '凭证创建成功' : '新建凭证'" width="600px" destroy-on-close>
      <template v-if="!created">
        <div class="ak-warn">带 <span class="ak-req">*</span> 为必填项。Secret 仅在本次创建时展示一次，关闭弹窗后无法再次查看，请妥善保存。</div>
        <el-form label-width="104px" label-position="left">
          <el-form-item label="对接系统" required>
            <el-input v-model="form.appName" maxlength="60" placeholder="如：金蝶云星空（订单同步）" show-word-limit />
            <div class="ak-tip">填写具体业务系统名称，便于在列表中直接识别。</div>
          </el-form-item>
          <el-form-item label="所属企业" required>
            <el-input v-model="form.company" maxlength="120" placeholder="如：深圳市前海国际供应链有限公司" show-word-limit />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.contactName" maxlength="40" placeholder="如：张三（系统对接人）" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" maxlength="30" placeholder="如：13800138000" />
          </el-form-item>
          <el-form-item label="联系邮箱">
            <el-input v-model="form.contactEmail" maxlength="120" placeholder="如：it@company.com" />
          </el-form-item>
          <el-form-item label="授权范围">
            <el-select v-model="form.scope" multiple filterable allow-create default-first-option :reserve-keyword="false" placeholder="选择或输入权限范围（未选择默认 order:read）" style="width: 100%">
              <el-option v-for="s in SCOPE_OPTIONS" :key="s" :label="s" :value="s" />
            </el-select>
            <div class="ak-tip">按开放接口目录勾选；需新增范围可直接输入后回车。</div>
          </el-form-item>
          <el-form-item label="用途说明">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" placeholder="如：ERP 订单下发与运单状态回传" show-word-limit />
          </el-form-item>
        </el-form>
      </template>

      <template v-else>
        <div class="ak-created">
          <div class="ak-created-title">凭证创建成功，Secret 仅本次可见</div>
          <div class="ak-key-row">
            <span class="ak-key-label">API Key</span>
            <code class="ak-key-val">{{ created.api_key }}</code>
            <el-button size="small" type="primary" plain @click="copyKey(created.api_key)">复制</el-button>
          </div>
          <div class="ak-key-row">
            <span class="ak-key-label">Secret</span>
            <code class="ak-key-val">{{ created.secret }}</code>
            <el-button size="small" type="primary" plain @click="copyKey(created.secret)">复制</el-button>
          </div>
          <el-checkbox v-model="savedConfirmed" style="margin-top: 6px">我已复制并妥善保存 Secret，知悉关闭后无法再次查看</el-checkbox>
        </div>
      </template>

      <template #footer>
        <el-button @click="closeCreate">取消</el-button>
        <template v-if="!created">
          <el-button type="primary" :loading="creating" @click="doCreate">生成凭证</el-button>
        </template>
        <template v-else>
          <el-button @click="resetCreate">继续创建</el-button>
          <el-button type="primary" :disabled="!savedConfirmed" @click="closeCreate">已完成保存</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiKeyList, apiKeyCreate, apiKeyToggle, apiKeyDelete } from '../../api'

const setDesc = inject('setPageDesc')
const keys = ref([])
const loading = ref(false)
const createDialog = ref(false)
const creating = ref(false)
const created = ref(null)
const savedConfirmed = ref(false)
const detailVisible = ref(false)
const detailKey = ref(null)
const form = reactive({ appName: '', company: '', contactName: '', contactPhone: '', contactEmail: '', scope: [], remark: '' })

const SCOPE_OPTIONS = ['order:read', 'order:write', 'waybill:read']
const docs = [
  { method: 'GET', path: '/api/tms/order/list', desc: '履约订单列表（order:read）' },
  { method: 'GET', path: '/api/tms/order/{orderNo}', desc: '订单详情（order:read）' },
  { method: 'POST', path: '/api/tms/order/create', desc: '创建履约订单（order:write）' },
  { method: 'GET', path: '/api/tms/waybill/list', desc: '运单列表（order:read）' },
  { method: 'GET', path: '/api/tms/search', desc: '全局搜索（order:read）' }
]

const maskKey = (k) => (k && k.length > 12 ? k.slice(0, 6) + '****' + k.slice(-4) : k || '-')
function displayName(row) {
  return row.company || row.app_name || '-'
}
function scopesOf(scope) {
  return String(scope || '').split(',').map((s) => s.trim()).filter(Boolean)
}
function capName(desc) {
  const s = String(desc || '')
  const i = s.search(/[（(]/)
  return i > 0 ? s.slice(0, i) : s
}
function capScope(desc) {
  const s = String(desc || '')
  const m = s.match(/[（(]([^）)]+)[）)]/)
  return m ? m[1].trim() : ''
}
const pad = (n) => String(n).padStart(2, '0')
function toDate(v) {
  if (v == null || v === '') return null
  const s = String(v).trim()
  if (/^\d+$/.test(s)) {
    const n = Number(s)
    const d = new Date(s.length >= 13 ? n : n * 1000)
    return isNaN(d.getTime()) ? null : d
  }
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  return isNaN(d.getTime()) ? null : d
}
function fmtTime(v) {
  const d = toDate(v)
  return d ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}` : '-'
}
async function copyKey(text) {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

async function load() {
  loading.value = true
  try { keys.value = (await apiKeyList()) || [] } catch (e) { keys.value = [] } finally { loading.value = false }
}

function resetForm() {
  Object.assign(form, { appName: '', company: '', contactName: '', contactPhone: '', contactEmail: '', scope: [], remark: '' })
}
function openCreate() {
  created.value = null
  savedConfirmed.value = false
  resetForm()
  createDialog.value = true
}

async function doCreate() {
  if (!form.appName || !form.appName.trim()) return ElMessage.warning('请填写对接系统名称')
  if (!form.company || !form.company.trim()) return ElMessage.warning('请填写所属企业')
  creating.value = true
  try {
    created.value = await apiKeyCreate({ ...form, scope: form.scope.join(',') })
    savedConfirmed.value = false
    ElMessage.success('对接凭证创建成功，Secret 仅本次可见，请妥善保存')
    load()
  } finally { creating.value = false }
}

function openDetail(row) {
  detailKey.value = row
  detailVisible.value = true
}
async function toggleFromDetail() {
  const row = detailKey.value
  detailVisible.value = false
  await doToggle(row)
}
async function revokeFromDetail() {
  const row = detailKey.value
  detailVisible.value = false
  await doDelete(row)
}

function closeCreate() {
  createDialog.value = false
  created.value = null
  savedConfirmed.value = false
}
function resetCreate() {
  created.value = null
  savedConfirmed.value = false
  resetForm()
}

async function doToggle(row) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(
    `确认${action}系统「${row.app_name}」的访问凭证？${action === '停用' ? '停用后该系统调用开放接口将立即失败。' : ''}`,
    `${action}凭证`,
    { type: 'warning', confirmButtonText: '确认' }
  )
  await apiKeyToggle(row.id)
  ElMessage.success(`已${action}`)
  load()
}

async function doDelete(row) {
  const { value } = await ElMessageBox.prompt(
    `吊销后系统「${row.app_name}」将无法调用任何开放接口，且凭证不可恢复，如需继续集成请重新创建。请输入对接系统名称确认吊销。`,
    '吊销凭证 · 二次确认',
    {
      inputPlaceholder: row.app_name,
      inputValidator: (v) => (v || '').trim() === row.app_name || '输入内容与应用名称不一致'
    }
  )
  await apiKeyDelete(row.id)
  ElMessage.success('凭证已吊销')
  load()
}

onMounted(() => {
  setDesc('开放接口接入管理：对接系统凭证签发、授权范围与生命周期管理')
  load()
})
</script>

<style scoped>
.ak-note { margin-bottom: 14px; padding: 12px 16px; font-size: 13px; color: #2f3d4c; background: #f0f6fb; border: 1px solid #d8e6f1; border-left: 4px solid #2563a8; border-radius: 6px; line-height: 1.8; }
.ak-note b { color: #1d4f8a; }
.ak-note code { background: #fff; border: 1px solid #dce6ee; border-radius: 4px; padding: 0 5px; color: #b3541e; }
.ak-card-head { display: flex; align-items: center; justify-content: space-between; }
.ak-card-sub { margin-left: 10px; font-size: 12px; color: #8b95a1; font-weight: 400; }
.ak-primary { font-weight: 600; color: #1d2129; line-height: 1.5; }
.ak-sub { font-size: 12px; color: #8b95a1; margin-top: 2px; }
.ak-warn { margin: -4px 0 14px; padding: 10px 12px; font-size: 13px; color: #7a4b12; background: #fff8ec; border: 1px solid #f7e3bf; border-radius: 6px; line-height: 1.8; }
.ak-req { color: #d93026; font-weight: 600; }
.ak-tip { font-size: 12px; color: #8b95a1; margin-top: 4px; line-height: 1.6; }
.ak-created { background: #f6fafb; border: 1px solid #dcecf1; border-radius: 8px; padding: 14px 16px; }
.ak-created-title { font-size: 14px; font-weight: 600; color: #0b7fa0; margin-bottom: 10px; }
.ak-key-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.ak-key-label { width: 76px; font-size: 13px; color: #5a6772; flex-shrink: 0; }
.ak-key-val { flex: 1; min-width: 0; font-size: 12px; background: #fff; border: 1px solid #e5e9ec; border-radius: 6px; padding: 6px 10px; word-break: break-all; }
.kd-actions { margin-top: 18px; display: flex; gap: 10px; flex-wrap: wrap; }
.cap-list { display: flex; flex-direction: column; }
.cap { display: flex; align-items: center; gap: 10px; padding: 11px 4px; border-bottom: 1px solid #eef1f4; flex-wrap: wrap; }
.cap:last-child { border-bottom: none; }
.cap-name { font-size: 13px; color: #4e5969; margin-left: auto; }
.doc-method { font-size: 12px; font-weight: 700; border-radius: 5px; padding: 2px 10px; flex-shrink: 0; }
.doc-method.m-get { background: #e8f8f1; color: #0c8f62; }
.doc-method.m-post { background: #fff3e0; color: #b76e00; }
.doc-method.m-put { background: #e6f4f7; color: #0b7fa0; }
.doc-method.m-delete { background: #fdeceb; color: #c0392b; }
.cap-scope { font-size: 12px; color: #2563a8; background: #eef4fb; border-radius: 4px; padding: 1px 8px; }
.cap-path { font-size: 12px; color: #57606a; background: #f6f8f9; border-radius: 4px; padding: 2px 8px; word-break: break-all; }
.ak-status { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: #57606a; }
.ak-status i { width: 7px; height: 7px; border-radius: 50%; display: inline-block; }
.ak-status.on { color: #0c8f62; } .ak-status.on i { background: #12b981; }
.ak-status.off { color: #8b95a1; } .ak-status.off i { background: #b8c1ca; }
</style>