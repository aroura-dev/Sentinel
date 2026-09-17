import { createRouter, createWebHistory } from 'vue-router'
import { MENU, rolesForPath, pathAllowed, landingPath } from '../utils/menu'

// 菜单叶子路径集合（详情页等非菜单路由不受菜单权限拦截）
const MENU_LEAF_PATHS = new Set()
for (const g of MENU) {
  if (g.children) {
    for (const c of g.children) {
      MENU_LEAF_PATHS.add(c.index)
    }
  }
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'workbench', name: 'Workbench', component: () => import('../views/Workbench.vue'), meta: { title: '今日待办' } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '运营总览' } },
      { path: 'logistics/order', redirect: '/tms/order' },
      { path: 'logistics/track', name: 'TrackQuery', component: () => import('../views/logistics/TrackQuery.vue'), meta: { title: '物流跟踪' } },
      { path: 'notification/list', name: 'NotifList', component: () => import('../views/notification/List.vue'), meta: { title: '通知中心' } },
      { path: 'agent/log', name: 'AgentLog', component: () => import('../views/agent/LogList.vue'), meta: { title: 'AI 使用记录' } },
      { path: 'agent/trace', name: 'AgentTrace', component: () => import('../views/agent/Trace.vue'), meta: { title: 'AI 过程追踪' } },
      { path: 'ai/chat', name: 'AiChat', component: () => import('../views/agent/AiChat.vue'), meta: { title: '智能问答' } },
      { path: 'ai/analytics', name: 'AiAnalytics', component: () => import('../views/agent/AiAnalytics.vue'), meta: { title: '智能分析' } },
      { path: 'agent/ops', name: 'AgentOps', component: () => import('../views/agent/AgentOps.vue'), meta: { title: '异常自动处置' } },
      { path: 'agent/cs', name: 'CsChat', component: () => import('../views/agent/CsChat.vue'), meta: { title: '智能客服' } },
      { path: 'workorder', name: 'Workorder', component: () => import('../views/workorder/List.vue'), meta: { title: '问题工单' } },
      { path: 'config/knowledge', name: 'Knowledge', component: () => import('../views/config/Knowledge.vue'), meta: { title: '知识库' } },
      { path: 'audit', name: 'AuditLog', component: () => import('../views/audit/List.vue'), meta: { title: '操作日志' } },
      { path: 'tms/fulfillment', name: 'FulfillmentCreate', component: () => import('../views/tms/FulfillmentCreate.vue'), meta: { title: '新建订单' } },
      { path: 'tms/order', name: 'TmsOrderList', component: () => import('../views/tms/OrderList.vue'), meta: { title: '订单管理' } },
      { path: 'tms/order/:orderNo', name: 'TmsOrderDetail', component: () => import('../views/tms/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'tms/waybill', name: 'WaybillList', component: () => import('../views/tms/WaybillList.vue'), meta: { title: '运单管理' } },
      { path: 'tms/waybill/:waybillNo', name: 'WaybillDetail', component: () => import('../views/tms/WaybillDetail.vue'), meta: { title: '运单详情' } },
      { path: 'tms/merchant', name: 'Merchant', component: () => import('../views/tms/Merchant.vue'), meta: { title: '商家管理' } },
      { path: 'tms/carrier', name: 'Carrier', component: () => import('../views/tms/Carrier.vue'), meta: { title: '承运商' } },
      { path: 'tms/channel', name: 'TmsChannel', component: () => import('../views/tms/Channel.vue'), meta: { title: '物流渠道' } },
      { path: 'tms/rate', name: 'Rate', component: () => import('../views/tms/Rate.vue'), meta: { title: '运费报价' } },
      { path: 'tms/warehouse', name: 'Warehouse', component: () => import('../views/tms/Warehouse.vue'), meta: { title: '仓库' } },
      { path: 'tms/product', name: 'Product', component: () => import('../views/tms/Product.vue'), meta: { title: '商品管理' } },
      { path: 'tms/master', name: 'MasterData', component: () => import('../views/tms/MasterData.vue'), meta: { title: '资料中心' } },
      { path: 'tms/master-home', name: 'MasterHome', component: () => import('../views/tms/MasterHome.vue'), meta: { title: '资料工作台' } },
      { path: 'tms/bill', name: 'BillList', component: () => import('../views/tms/BillList.vue'), meta: { title: '账单结算' } },
      { path: 'tms/bill/:id', name: 'BillDetail', component: () => import('../views/tms/BillDetail.vue'), meta: { title: '账单详情' } },
      { path: 'tms/seller-dashboard', name: 'SellerDashboard', component: () => import('../views/tms/SellerDashboard.vue'), meta: { title: '卖家看板' } },
      { path: 'tms/after-sale', name: 'AfterSale', component: () => import('../views/tms/AfterSale.vue'), meta: { title: '售后退货' } },
      { path: 'tms/risk-alert', name: 'RiskAlert', component: () => import('../views/tms/RiskAlert.vue'), meta: { title: '风险预警' } },
      { path: 'tms/inventory', name: 'Inventory', component: () => import('../views/tms/Inventory.vue'), meta: { title: '库存查询' } },
      { path: 'tms/api-console', name: 'ApiConsole', component: () => import('../views/tms/ApiConsole.vue'), meta: { title: '开放接口' } },
      { path: 'tms/order-review', name: 'OrderReview', component: () => import('../views/tms/OrderReview.vue'), meta: { title: '订单审核' } },
      { path: 'tms/order-merge', name: 'OrderMerge', component: () => import('../views/tms/OrderMerge.vue'), meta: { title: '合并运单' } },
      { path: 'tms/sign-back', name: 'SignBack', component: () => import('../views/tms/SignBack.vue'), meta: { title: '签收回单' } },
      { path: 'tms/claim', name: 'Claim', component: () => import('../views/tms/Claim.vue'), meta: { title: '理赔流程' } },
      { path: 'tms/reconcile', name: 'Reconcile', component: () => import('../views/tms/Reconcile.vue'), meta: { title: '差异对账' } },
      { path: 'tms/stock-in-out', name: 'StockInOut', component: () => import('../views/tms/StockInOut.vue'), meta: { title: '出入库登记' } },
      { path: 'tms/stocktake', name: 'Stocktake', component: () => import('../views/tms/Stocktake.vue'), meta: { title: '移库盘点' } },
      { path: 'tms/sla-analysis', name: 'SlaAnalysis', component: () => import('../views/tms/SlaAnalysis.vue'), meta: { title: '时效分析' } },
      { path: 'tms/carrier-kpi', name: 'CarrierKpi', component: () => import('../views/tms/CarrierKpi.vue'), meta: { title: '承运商考核' } },
      { path: 'sys/user', name: 'SysUser', component: () => import('../views/sys/User.vue'), meta: { title: '用户管理' } },
      { path: 'sys/role', name: 'SysRole', component: () => import('../views/sys/Role.vue'), meta: { title: '角色权限' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 守卫：token 认证 + 角色鉴权；无权限时跳到角色可用落地页，避免重定向循环
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('sentinel_token')
  const role = localStorage.getItem('sentinel_role')
  let perms = []
  try { perms = JSON.parse(localStorage.getItem('sentinel_permissions') || '[]') } catch (e) { perms = [] }

  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next(landingPath(perms, role))
    return
  }

  if (Array.isArray(perms) && perms.length) {
    if (MENU_LEAF_PATHS.has(to.path) && !pathAllowed(to.path, perms)) {
      const fallback = landingPath(perms, role)
      if (fallback && fallback !== to.path) {
        next(fallback)
        return
      }
    }
  } else {
    const roles = rolesForPath(to.path)
    if (roles && role && !roles.includes(role)) {
      const fallback = landingPath([], role)
      if (fallback && fallback !== to.path) {
        next(fallback)
        return
      }
    }
  }
  next()
})

export default router