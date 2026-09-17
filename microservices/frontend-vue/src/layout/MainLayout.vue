<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <LogoMark :size="40" />
        <div class="logo-text">
          <div class="logo-name">SENTINEL</div>
          <div class="logo-sub">物流异常智能处置平台</div>
        </div>
      </div>
      <el-menu :default-active="activeModule?.landing" router class="menu">
        <el-menu-item v-for="m in modules" :key="m.index" :index="m.landing">
          <el-icon><component :is="m.icon" /></el-icon><span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏：左侧=面包屑(父模块>子模块)+子模块导航，右侧=搜索框/通知/用户，同一行，消除空白 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="›" class="header-breadcrumb">
            <el-breadcrumb-item v-for="(b, i) in breadcrumbs" :key="i" :to="b.to">{{ b.label }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-popover placement="bottom-start" :width="420" trigger="click" :visible="searchOpen">
            <template #reference>
              <div class="search-wrap">
                <el-input
                  v-model="searchKw"
                  placeholder="搜索订单 / 运单 / 商家"
                  clearable
                  style="width: 300px"
                  @input="onSearchInput"
                  @keyup.enter="doSearch"
                  @focus="doSearch"
                />
              </div>
            </template>
            <div class="search-panel">
              <div v-if="!searchResults" style="color: #9ca3af; font-size: 13px; padding: 8px">输入关键词开始搜索…</div>
              <template v-else>
                <div v-if="!searchResults.orders.length && !searchResults.waybills.length && !searchResults.merchants.length"
                  style="color: #9ca3af; font-size: 13px; padding: 8px">无匹配结果</div>
                <div v-if="searchResults.orders.length" class="search-group">
                  <div class="search-group-title">订单</div>
                  <div v-for="o in searchResults.orders" :key="o.order_no" class="search-item" @click="goOrder(o)">
                    <span class="si-main">{{ o.order_no }}</span>
                    <span class="si-sub">{{ o.merchant_name }} · {{ o.current_node }}</span>
                  </div>
                </div>
                <div v-if="searchResults.waybills.length" class="search-group">
                  <div class="search-group-title">运单</div>
                  <div v-for="w in searchResults.waybills" :key="w.waybill_no" class="search-item" @click="goWaybill(w)">
                    <span class="si-main">{{ w.waybill_no }}</span>
                    <span class="si-sub">{{ w.tracking_no }} · {{ w.carrier_code }}</span>
                  </div>
                </div>
                <div v-if="searchResults.merchants.length" class="search-group">
                  <div class="search-group-title">商家</div>
                  <div v-for="m in searchResults.merchants" :key="m.id" class="search-item" @click="goMerchant(m)">
                    <span class="si-main">{{ m.merchant_name }}</span>
                    <span class="si-sub">{{ m.merchant_code }}</span>
                  </div>
                </div>
              </template>
            </div>
          </el-popover>
          <el-popover placement="bottom-end" :width="360" trigger="click" :teleported="false" @show="loadNotifs">
            <template #reference>
              <el-badge :value="notifUnread" :hidden="!notifUnread" :max="99" class="notif-badge">
                <button class="notif-bell" type="button">
                  <el-icon :size="18"><Bell /></el-icon>
                </button>
              </el-badge>
            </template>
            <div class="notif-panel">
              <div class="notif-head">
                <span class="notif-title">通知</span>
                <div class="notif-actions">
                  <el-button v-if="notifUnread" link type="primary" size="small" @click="markAllRead">全部已读</el-button>
                  <router-link to="/notification/list" class="notif-all">查看全部 →</router-link>
                </div>
              </div>
              <div v-if="!notifs.length" class="notif-empty">暂无通知</div>
              <div v-else class="notif-list">
                <div
                  v-for="n in notifs"
                  :key="n.id"
                  class="notif-item"
                  :class="{ unread: isUnread(n) }"
                  @click="openNotif(n)"
                >
                  <div class="notif-item-top">
                    <span class="notif-item-title">{{ notifTitle(n) }}</span>
                    <span class="notif-item-time">{{ timeFor(n.created_at) }}</span>
                  </div>
                  <div class="notif-item-content">{{ n.content || n.order_no }}</div>
                  <div class="notif-item-meta">
                    <el-tag size="small" effect="plain">{{ nodeLabel(n.node) }}</el-tag>
                    <el-tag size="small" effect="light" :type="statusType(n.status)">{{ n.status }}</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
          <div class="header-user">
            <el-popover placement="bottom-end" :width="236" trigger="click" popper-class="user-panel-popover">
              <template #reference>
                <el-avatar :size="30" class="header-avatar" :src="authStore.avatar || undefined" :style="authStore.avatar ? {} : avatarStyle">
                  <span v-if="!authStore.avatar">{{ avatarText }}</span>
                </el-avatar>
              </template>
              <div class="user-panel">
                <div class="user-panel-main">
                  <el-avatar :size="42" class="user-panel-avatar" :src="authStore.avatar || undefined" :style="authStore.avatar ? {} : avatarStyle">
                    <span v-if="!authStore.avatar">{{ avatarText }}</span>
                  </el-avatar>
                  <div class="user-panel-meta">
                    <div class="user-panel-name">{{ displayName }}</div>
                    <el-tag size="small" effect="light" class="user-panel-role">{{ roleLabel }}</el-tag>
                  </div>
                </div>
                <div class="user-panel-divider"></div>
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  :on-change="onAvatarChange"
                >
                  <el-button type="primary" plain size="small" class="user-panel-upload">更换头像</el-button>
                </el-upload>
              </div>
            </el-popover>
            <el-tooltip content="退出登录" placement="bottom" popper-class="header-tip">
              <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
                <el-icon :size="18"><SwitchButton /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </div>
      </el-header>

      <!-- 页内工具行：左=子模块tabs，右=页面级操作按钮，独立固定条，与顶栏同级通栏无缝衔接 -->
      <div v-if="!hideViewNav" class="view-nav">
        <div class="view-tabs">
            <el-button v-for="a in navFlat" :key="a.index" text size="small" :class="{ 'nav-active': isActive(a.index) }" @click="go(a.index)">{{ a.title }}</el-button>
            <el-dropdown v-if="navDropdown.length" @command="go">
              <el-button size="small" text>更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="a in navDropdown" :key="a.index" :command="a.index">{{ a.title }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        <div class="desc-actions">
          <el-button v-for="a in primaryActions" :key="a.index" type="primary" size="small" @click="go(a.index)">{{ a.title }}</el-button>
          <el-button v-for="a in pageActions" :key="a.text" :type="a.type || 'primary'" size="small" @click="a.onClick">{{ a.text }}</el-button>
        </div>
      </div>

      <el-main class="main">
        <keep-alive v-if="!hideStats">
          <ModuleStats :module="activeModule" :key="activeModule?.index" />
        </keep-alive>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, provide, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { logout, globalSearch, notifList, myPermissions, me, updateAvatar } from '../api'
import { MENU, ROLES, visibleMenu, filterMenuByPaths, moduleOfPath } from '../utils/menu'
import LogoMark from '../components/LogoMark.vue'
import ModuleStats from '../components/ModuleStats.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 全局搜索
const searchKw = ref('')
const searchResults = ref(null)
const searchOpen = ref(false)
let searchTimer = null

async function doSearch() {
  const kw = (searchKw.value || '').trim()
  if (!kw) { searchResults.value = null; return }
  searchOpen.value = true
  try { searchResults.value = await globalSearch(kw) } catch { searchResults.value = { orders: [], waybills: [], merchants: [] } }
}
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(doSearch, 300)
}
function goOrder(o) { searchOpen.value = false; router.push(`/tms/order/${o.order_no}`) }
function goWaybill(w) { searchOpen.value = false; router.push(`/tms/waybill/${w.waybill_no}`) }
function goMerchant(m) { searchOpen.value = false; router.push(`/tms/merchant`) }

// 站内通知中心
const READ_KEY = 'sentinel_read_notifs'
const notifs = ref([])
const notifUnread = ref(0)
let notifTimer = null

function readSet() {
  try { return new Set(JSON.parse(localStorage.getItem(READ_KEY) || '[]')) } catch { return new Set() }
}
function saveReadSet(s) { localStorage.setItem(READ_KEY, JSON.stringify([...s])) }

async function loadNotifs() {
  try {
    const data = await notifList({ page: 1, perPage: 15 })
    notifs.value = data.rows || []
    notifUnread.value = notifs.value.filter((n) => !readSet().has(n.id)).length
  } catch { /* 无权限忽略 */ }
}
function isUnread(n) { return !readSet().has(n.id) }
function markRead(n) {
  const s = readSet()
  if (s.has(n.id)) return
  s.add(n.id)
  saveReadSet(s)
  notifUnread.value = Math.max(0, notifUnread.value - 1)
}
function markAllRead() {
  const s = readSet()
  notifs.value.forEach((n) => s.add(n.id))
  saveReadSet(s)
  notifUnread.value = 0
}
function openNotif(n) {
  markRead(n)
  if (n.order_no) router.push(`/tms/order/${n.order_no}`)
  else if (n.trace_id) router.push({ path: '/agent/trace', query: { traceId: n.trace_id } })
}
function notifTitle(n) {
  const map = { DELIVERED: '包裹已签收', CUSTOMS_DELAY: '中转延误提醒', DELIVERY_FAILED: '派送失败', LOST: '包裹丢失', RETURNED: '包裹退回' }
  return map[n.node] || `物流通知 · ${nodeLabel(n.node)}`
}
function timeFor(v) { return v ? String(v).replace('T', ' ').slice(5, 16) : '' }
function nodeLabel(node) {
  return {
    CREATED: '已创建', WAREHOUSE_OUT: '已出库', DOMESTIC_PICKED: '揽收', EXPORT_CUSTOMS: '中转分拨',
    IN_TRANSIT: '干线运输', IMPORT_CUSTOMS: '进口中转', LAST_MILE: '末端派送', DELIVERED: '已签收',
    CUSTOMS_DELAY: '中转延误', DELIVERY_FAILED: '派送失败', LOST: '丢件', RETURNED: '退回'
  }[node] || node
}
function statusType(s) { return { SENT: 'success', FAILED: 'danger', PENDING: 'warning' }[s] || 'info' }

watch(() => route.path, () => { loadNotifs() })
async function onAvatarChange(uploadFile) {
  const file = uploadFile && uploadFile.raw
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }
  const form = new FormData()
  form.append('file', file)
  try {
    const res = await updateAvatar(form)
    authStore.setAvatar(res && res.avatar)
    ElMessage.success('头像已更新')
  } catch (e) {
    // 请求拦截器已提示错误
  }
}
async function syncProfile() {
  try {
    const user = await me()
    if (user) authStore.setProfile(user.username, user.role, user.nickname, user.avatar)
  } catch (e) {
    // 用户资料刷新失败不阻塞页面
  }
}
onMounted(async () => {
  syncProfile()
  loadNotifs()
  notifTimer = setInterval(loadNotifs, 60000)
  if (!authStore.permissions.length) {
    try {
      const perms = await myPermissions()
      if (perms && perms.paths) authStore.setPermissions(perms.paths)
    } catch (e) {
      // 回退硬编码角色过滤
    }
  }
})
onBeforeUnmount(() => { if (notifTimer) clearInterval(notifTimer) })

// 数据驱动菜单
const menus = computed(() =>
  authStore.permissions.length
    ? filterMenuByPaths(MENU, authStore.permissions)
    : visibleMenu(authStore.role || ROLES.ADMIN)
)

// 布局：侧边栏只放一级模块；子功能上移顶栏，与搜索框同一行
const modules = computed(() => menus.value.map(({ children, ...m }) => m))
const activeModule = computed(() => moduleOfPath(menus.value, route.path))
const pageTitle = computed(() =>
  route.meta.title
  || activeModule.value?.children?.find((a) => isActive(a.index))?.title
  || activeModule.value?.title
  || '')
const primaryActions = computed(() => activeModule.value?.children?.filter((a) => a.role === 'primary') || [])
const nav = computed(() => {
  const items = activeModule.value?.children?.filter((a) => a.role !== 'primary') || []
  if (items.length <= 4) return { flat: items, dropdown: [] }
  const moreCount = items.filter((a) => a.role === 'more').length
  const needMore = Math.min(Math.max(2, moreCount), items.length - 1)
  return { flat: items.slice(0, items.length - needMore), dropdown: items.slice(items.length - needMore) }
})
const navFlat = computed(() => nav.value.flat)
const navDropdown = computed(() => nav.value.dropdown)
function go(path) { if (path) router.push(path) }
function isActive(path) { return route.path === path || route.path.startsWith(path + '/') }

const ROLE_LABELS = {
  [ROLES.ADMIN]: '管理员',
  [ROLES.OPERATOR]: '运营',
  [ROLES.CUSTOMER_SERVICE]: '客服',
  [ROLES.MERCHANT]: '商家',
  [ROLES.FINANCE]: '财务'
}
const roleLabel = computed(() => ROLE_LABELS[authStore.role] || authStore.role)
const DISPLAY_NAMES = {
  admin: '张伟', zhangwei: '张伟',
  operator: '刘洋', liuyang: '刘洋',
  cs: '王芳', wangfang: '王芳',
  merchant: '陈浩', chenhao: '陈浩',
  finance: '赵敏', zhaomin: '赵敏'
}
const isPhoneLike = (value) => /^1[3-9]\d{9}$/.test(String(value || '').trim())
const displayName = computed(() => {
  const nickname = String(authStore.nickname || '').trim()
  if (nickname && !isPhoneLike(nickname)) return nickname
  return DISPLAY_NAMES[authStore.username] || roleLabel.value || '用户'
})
// 用户图标悬停提示：只显示一个（昵称优先，回退用户名）
const userTip = computed(() => displayName.value)
const avatarText = computed(() => {
  const name = displayName.value
  return name.slice(0, 2).toUpperCase()
})
const avatarStyle = computed(() => {
  const seed = authStore.username || authStore.nickname || 'user'
  let hash = 0
  for (const ch of seed) hash = (hash * 31 + ch.charCodeAt(0)) % 360
  return {
    background: `linear-gradient(135deg, hsl(${hash}, 68%, 42%), hsl(${(hash + 38) % 360}, 76%, 58%))`,
    color: '#fff',
    fontWeight: 600,
    flex: 'none'
  }
})

// 面包屑：父模块 / 当前子页面；父级可点跳模块默认页，末级为当前页
const breadcrumbs = computed(() => {
  const m = activeModule.value
  if (!m) return [{ label: pageTitle.value }]
  return [
    { label: m.title, to: m.landing },
    { label: pageTitle.value }
  ]
})

// 页面页头：子页面注册说明与右侧操作按钮
const pageDesc = ref('')
const pageActions = ref([])
const pageDescText = computed(() => pageDesc.value || activeModule.value?.desc || '')
provide('setPageDesc', (d) => { pageDesc.value = d })
provide('setPageActions', (a) => { pageActions.value = a || [] })
watch(() => route.path, () => { pageDesc.value = ''; pageActions.value = [] }, { immediate: true })

// 专注表单页隐藏子导航（仅新建订单）
const HIDE_NAV_PATHS = ['/tms/fulfillment', '/tms/master', '/tms/master-home']
const hideViewNav = computed(() => HIDE_NAV_PATHS.some((p) => route.path.startsWith(p)))
// 统计卡概览仅在订单管理页展示；新建/审核/合并不展示
const HIDE_STATS_PATHS = ['/tms/fulfillment', '/tms/order-review', '/tms/order-merge']
const hideStats = computed(() => HIDE_STATS_PATHS.some((p) => route.path.startsWith(p)))

async function handleLogout() {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  try {
    await logout()
  } catch (e) {
    // 忽略退出接口异常
  }
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

/* —— 浅色侧边栏 —— */
.aside {
  background: #fff;
  /* 侧边栏与主内容区清晰竖直分割线：边框 + 右缘轻投影 */
  border-right: 1px solid #dcdde1;
  box-shadow: 1px 0 3px rgba(29, 33, 41, 0.06);
  display: flex;
  flex-direction: column;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e6eb;
  flex: none;
}
.logo-name {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #1d2129;
  line-height: 1.2;
}
.logo-sub {
  font-size: 11px;
  color: #86909c;
  letter-spacing: 0.5px;
  margin-top: 1px;
  line-height: 1.2;
}

/* 菜单：浅底 + 深灰字 + 青绿高亮 */
.menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  background: transparent;
  padding: 8px;
}
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  color: #4e5969;
  font-size: 14px;
  margin: 2px 6px;
  border-radius: 8px;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: #f6f7f9;
  color: #0891b2;
}
.menu :deep(.el-menu-item.is-active) {
  background: #eef7fa;
  color: #0891b2;
  font-weight: 600;
  position: relative;
}
.menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: #06b6d4;
}
.menu :deep(.el-menu .el-menu-item) {
  font-size: 13px;
  height: 40px;
  line-height: 40px;
}
.menu :deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  margin: 2px 6px;
}
.menu :deep(.el-icon) {
  font-size: 16px;
}

/* —— 白色顶栏：左=面包屑+子模块导航，右=搜索/通知/用户，同一行 —— */
.header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
  box-shadow: 0 1px 2px rgba(29, 33, 41, 0.03);
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
  flex: 1;
  min-width: 0;
}
/* 面包屑：父模块可点、末级灰色不可点 */
.header-breadcrumb :deep(.el-breadcrumb__inner) {
  color: #86909c;
  font-size: 13px;
  font-weight: 400;
  white-space: nowrap;
}
.header-breadcrumb :deep(.el-breadcrumb__inner.is-link) {
  color: #1d2129;
  font-weight: 600;
}
.header-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #0891b2;
}
/* 页内工具行：左=子模块tabs，右=页面级操作按钮 */
/* 独立固定条（与顶栏同级、通栏同宽、无缝衔接顶栏）；白色实底盖住滚动内容 */
/* 底部一条灰色分隔线：与下方内容区分 */
.view-nav {
  flex: none;
  background: #fff;
  /* 上下等距内边距：子模块功能在栏内垂直居中 */
  padding: 8px 20px;
  border-bottom: 1px solid #e5e6eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.view-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
}
/* 子模块页签：常规灰色、悬停品牌色、激活品牌色（保持简洁，无额外下划线/面板） */
.view-nav :deep(.el-button.is-text) {
  color: #4e5969;
  font-weight: 500;
  height: 32px;
  padding: 0 12px;
  border-radius: 6px;
}
.view-nav :deep(.el-button.is-text:hover) {
  color: #0891b2;
}
.view-nav :deep(.el-button.is-text.nav-active) {
  color: #0891b2;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: none;
}
.search-wrap {
  display: flex;
  align-items: center;
}
.search-wrap :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #f2f3f5;
  box-shadow: 0 0 0 1px #f2f3f5 inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}
.search-wrap :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #e2e4e8 inset;
}
.search-wrap :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1.5px #0891b2 inset;
}
.search-wrap :deep(.el-input__inner) {
  font-size: 13px;
}
.search-panel {
  max-height: 360px;
  overflow-y: auto;
}
.search-group-title {
  font-size: 12px;
  color: #86909c;
  padding: 6px 4px 2px;
  border-bottom: 1px solid #e5e6eb;
}
.search-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.search-item:hover {
  background: #e8f7fa;
}
.si-main {
  font-size: 13px;
  font-weight: 600;
  color: #0891b2;
}
.si-sub {
  font-size: 12px;
  color: #86909c;
}
.header-avatar {
  cursor: pointer;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(16, 24, 40, 0.14);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.header-avatar:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 9px rgba(16, 24, 40, 0.18);
}
.user-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 2px 0;
}
.user-panel-main {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-panel-avatar {
  flex: none;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.14);
}
.user-panel-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.user-panel-name {
  max-width: 150px;
  overflow: hidden;
  color: #1d2129;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-panel-role {
  margin: 0;
}
.user-panel-divider {
  height: 1px;
  background: #eef1f4;
}
.user-panel-upload {
  width: 100%;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 4px;
}
.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  transition: background 0.2s ease, color 0.2s ease;
}
.icon-btn:hover {
  background: #f3f4f6;
  color: #0891b2;
}

/* —— 内容区 —— */
.main {
  background: #f7f8fa;
  /* 四周留白：内容不紧贴容器四边（收紧上下留白，减少页面右侧滚动区域） */
  padding: 14px 24px 20px;
}

/* 操作按钮：并入页内工具行右侧 */
.desc-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.notif-badge {
  margin-right: 8px;
  vertical-align: middle;
}
.notif-bell {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
}
.notif-bell:hover {
  background: #f3f4f6;
}
</style>

<style>
/* 顶栏图标悬停提示（teleport 到 body，需非 scoped）：文字灰色 */
.header-tip .el-popper__html,
.header-tip .el-tooltip__content {
  color: #86909c;
}
/* 通知弹层内容（teleport 到 body，需非 scoped） */
.notif-panel { padding: 2px 0; }
.notif-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px 10px;
}
.notif-title { font-weight: 600; color: #1d2129; }
.notif-actions { display: flex; align-items: center; gap: 10px; }
.notif-all { font-size: 12px; color: #0891b2; text-decoration: none; }
.notif-empty {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 24px 0;
}
.notif-list { max-height: 360px; overflow-y: auto; }
.notif-item {
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
}
.notif-item:hover { background: #f5f7fa; }
.notif-item.unread { background: #e6f4f7; }
.notif-item.unread:hover { background: #c9ecf4; }
.notif-item-top { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; }
.notif-item-title { font-size: 13px; font-weight: 600; color: #1d2129; }
.notif-item.unread .notif-item-title::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0891b2;
  margin-right: 6px;
  vertical-align: 1px;
}
.notif-item-time { font-size: 12px; color: #c0c4cc; white-space: nowrap; }
.notif-item-content {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notif-item-meta { display: flex; gap: 6px; margin-top: 6px; }
</style>
