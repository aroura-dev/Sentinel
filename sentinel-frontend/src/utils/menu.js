// 角色矩阵单一来源：菜单与路由 meta.roles 共用
export const ROLES = {
  ADMIN: 'ADMIN',
  OPERATOR: 'OPERATOR',
  CUSTOMER_SERVICE: 'CUSTOMER_SERVICE',
  MERCHANT: 'MERCHANT',
  FINANCE: 'FINANCE'
}

const ALL = [ROLES.ADMIN, ROLES.OPERATOR, ROLES.CUSTOMER_SERVICE, ROLES.MERCHANT, ROLES.FINANCE]

// 菜单定义：index 与路由 path 一一对应；roles 决定可见性与路由守卫
// 结构：国内物流数智协同平台，两级组织，顶层 10 个业务域（工作台/订单/运单/售后/结算/仓储/运营/资料/设置/智能中心）。
// 命名企业化 + 人文化（不用"履约/逆向/协同/台账/审计"等黑话），见字即懂、见分类即知业务。
// 占位项（🆕规划中）暂走统一 Placeholder 页，功能开发后替换为真实路由。
export const MENU = [
  // 我的工作台：每天进来先看这页——今日待办 + 数据看板
  {
    index: 'home',
    title: '我的工作台',
    icon: 'HomeFilled',
    children: [
      { index: '/workbench', title: '今日待办', roles: ALL },
      { index: '/dashboard', title: '运营总览', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.FINANCE] },
      { index: '/tms/seller-dashboard', title: '卖家看板', roles: [ROLES.MERCHANT, ROLES.ADMIN, ROLES.OPERATOR] }
    ]
  },
  // 订单：接单——货主的单子进来、审核放行
  {
    index: 'order',
    title: '订单',
    icon: 'Van',
    children: [
      { index: '/tms/order', title: '订单管理', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.MERCHANT, ROLES.FINANCE] },
      { index: '/tms/fulfillment', title: '新建订单', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.MERCHANT] },
      { index: '/tms/order-review', title: '订单审核', roles: [ROLES.ADMIN, ROLES.OPERATOR] },
      { index: '/tms/order-merge', title: '合并运单', roles: [ROLES.ADMIN, ROLES.OPERATOR] }
    ]
  },
  // 运单：发货——订单变成一票货，交给承运商，全程可查
  {
    index: 'waybill',
    title: '运单',
    icon: 'Document',
    children: [
      { index: '/tms/waybill', title: '运单管理', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.MERCHANT, ROLES.FINANCE] },
      { index: '/logistics/track', title: '物流跟踪', roles: ALL },
      { index: '/tms/sign-back', title: '签收回单', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.CUSTOMER_SERVICE] }
    ]
  },
  // 售后：出问题——退货、工单、理赔
  {
    index: 'after-sale',
    title: '售后',
    icon: 'Service',
    children: [
      { index: '/tms/after-sale', title: '售后退货', roles: ALL },
      { index: '/workorder', title: '问题工单', roles: ALL },
      { index: '/tms/claim', title: '理赔流程', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.CUSTOMER_SERVICE, ROLES.FINANCE] }
    ]
  },
  // 结算：收钱——账单、报价、对账
  {
    index: 'finance',
    title: '结算',
    icon: 'Money',
    children: [
      { index: '/tms/bill', title: '账单结算', roles: [ROLES.FINANCE, ROLES.ADMIN] },
      { index: '/tms/rate', title: '运费报价', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.FINANCE] },
      { index: '/tms/reconcile', title: '差异对账', roles: [ROLES.ADMIN, ROLES.FINANCE] }
    ]
  },
  // 仓储：管货——库存、进出、盘点
  {
    index: 'warehouse',
    title: '仓储',
    icon: 'Goods',
    children: [
      { index: '/tms/inventory', title: '库存查询', roles: ALL },
      { index: '/tms/stock-in-out', title: '出入库登记', roles: [ROLES.ADMIN, ROLES.OPERATOR] },
      { index: '/tms/stocktake', title: '移库盘点', roles: [ROLES.ADMIN, ROLES.OPERATOR] }
    ]
  },
  // 运营：日常——风控、通知、时效、考核
  {
    index: 'ops',
    title: '运营',
    icon: 'Monitor',
    children: [
      { index: '/tms/risk-alert', title: '风险预警', roles: ALL },
      { index: '/notification/list', title: '通知中心', roles: ALL },
      { index: '/tms/sla-analysis', title: '时效分析', roles: [ROLES.ADMIN, ROLES.OPERATOR] },
      { index: '/tms/carrier-kpi', title: '承运商考核', roles: [ROLES.ADMIN, ROLES.OPERATOR] }
    ]
  },
  // 资料：合作对象和货——商家、承运商、渠道、仓库、商品
  {
    index: 'master',
    title: '资料',
    icon: 'Box',
    children: [
      { index: '/tms/master-home', title: '资料工作台', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.MERCHANT] },
      { index: '/tms/master', title: '资料中心', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.MERCHANT] }
    ]
  },
  // 设置：系统本身——谁能用、谁干了什么、怎么对接外面
  {
    index: 'sys',
    title: '设置',
    icon: 'Setting',
    children: [
      { index: '/sys/user', title: '用户管理', roles: [ROLES.ADMIN] },
      { index: '/sys/role', title: '角色权限', roles: [ROLES.ADMIN] },
      { index: '/audit', title: '操作日志', roles: [ROLES.ADMIN] },
      { index: '/tms/api-console', title: '开放接口', roles: [ROLES.ADMIN] }
    ]
  },
  // 智能中心：Java Agent 服务——客服、知识与调用留痕
  {
    index: 'ai',
    title: '智能中心',
    icon: 'Cpu',
    children: [
      { index: '/ai/chat', title: '智能问答', roles: [ROLES.ADMIN, ROLES.OPERATOR, ROLES.CUSTOMER_SERVICE] },
      { index: '/ai/analytics', title: '智能分析', roles: [ROLES.ADMIN, ROLES.OPERATOR] },
      { index: '/agent/ops', title: '异常自动处置', roles: [ROLES.ADMIN, ROLES.OPERATOR] },
      { index: '/agent/cs', title: '智能客服', roles: [ROLES.ADMIN, ROLES.CUSTOMER_SERVICE] },
      { index: '/config/knowledge', title: '知识库', roles: [ROLES.ADMIN] },
      { index: '/agent/log', title: 'AI 使用记录', roles: [ROLES.ADMIN] },
      { index: '/agent/trace', title: 'AI 过程追踪', roles: [ROLES.ADMIN] }
    ]
  }
]

/** 按角色过滤出可见菜单 */
export function visibleMenu(role) {
  return MENU
    .map((item) => {
      if (item.children) {
        const children = item.children.filter((c) => c.roles.includes(role))
        return children.length ? { ...item, children } : null
      }
      return item.roles.includes(role) ? item : null
    })
    .filter(Boolean)
}

/** 菜单入口的历史别名：数据库里旧授权路径 → 新统一入口（如 资料中心） */
const LEGACY_ALIAS = {
  '/tms/master-home': ['/tms/merchant', '/tms/carrier', '/tms/channel', '/tms/warehouse', '/tms/product'],
  '/tms/master': ['/tms/merchant', '/tms/carrier', '/tms/channel', '/tms/warehouse', '/tms/product']
}

/** 按允许路径过滤菜单（保留有可见子项的组），用于动态菜单权限 */
export function filterMenuByPaths(menu = MENU, allowedPaths = []) {
  const set = new Set(allowedPaths)
  return menu
    .map((item) => {
      if (item.children) {
        const children = item.children.filter((c) => set.has(c.index) || (LEGACY_ALIAS[c.index] || []).some((p) => set.has(p)))
        return children.length ? { ...item, children } : null
      }
      return set.has(item.index) ? item : null
    })
    .filter(Boolean)
}

/** 路径是否被授权：直接授权，或通过“历史别名”间接授权（如 资料中心 兼容旧 5 项） */
export function pathAllowed(path, perms = []) {
  if (perms.includes(path)) return true
  const aliases = LEGACY_ALIAS[path] || []
  return aliases.some((p) => perms.includes(p))
}

/** 路径 → 允许角色（路由守卫用）；未配置返回 null（不限） */
export function rolesForPath(path) {
  for (const item of MENU) {
    if (item.children) {
      for (const c of item.children) {
        if (c.index === path) {
          return c.roles
        }
      }
    } else if (item.index === path) {
      return item.roles
    }
  }
  return null
}

// —— 布局改造归一化（最小改动：只追加，不改 40+ 项）——
// landing：点击左侧一级菜单进入的模块默认页
const MODULE_LANDING = {
  home: '/workbench', order: '/tms/order', waybill: '/tms/waybill', 'after-sale': '/tms/after-sale',
  finance: '/tms/bill', warehouse: '/tms/inventory', ops: '/tms/risk-alert', master: '/tms/master-home',
  sys: '/sys/user', ai: '/agent/cs'
}
// role：primary=模块头部主按钮 | more=低频→【更多】下拉 | 其余默认 text 文字按钮
const ACTION_ROLE = {
  '/tms/fulfillment': 'primary',
  '/tms/order-merge': 'more', '/tms/sign-back': 'more', '/tms/claim': 'more', '/tms/reconcile': 'more',
  '/tms/stocktake': 'more', '/tms/carrier-kpi': 'more', '/tms/product': 'more', '/audit': 'more',
  '/tms/api-console': 'more', '/config/knowledge': 'more', '/agent/log': 'more', '/agent/trace': 'more',
  '/tms/seller-dashboard': 'more'
}
// desc：模块级灰色业务描述（页面未注册 pageDesc 时的兜底文案，头部描述行左侧）
const MODULE_DESC = {
  home: '聚合今日待办、运营数据与最近异常工单',
  order: '订单从创建、审核到出库的全流程管理',
  waybill: '运单执行、物流跟踪与签收管理',
  'after-sale': '售后退货、问题工单与理赔处理',
  finance: '账单结算、运费报价与差异对账',
  warehouse: '库存查询、出入库登记与移库盘点',
  ops: '风险预警、通知中心与运营分析',
  master: '商家、承运商、渠道、仓库与商品资料',
  sys: '系统用户、角色权限与操作审计',
  ai: '智能客服、知识库与调用追踪'
}
MENU.forEach((m) => {
  m.landing = MODULE_LANDING[m.index] || (m.children && m.children[0].index) || m.index
  m.desc = MODULE_DESC[m.index] || ''
  m.children.forEach((c) => { c.role = ACTION_ROLE[c.index] || 'text' })
})

/** 由路径反查所属模块（精确或前缀匹配，详情页也能归到模块） */
export function moduleOfPath(menu = MENU, path) {
  let best = null
  let bestLen = 0
  for (const m of menu) {
    for (const a of m.children || []) {
      if (path === a.index || path.startsWith(a.index + '/')) {
        if (a.index.length > bestLen) { best = m; bestLen = a.index.length }
      }
    }
  }
  return best || menu[0] || null
}
