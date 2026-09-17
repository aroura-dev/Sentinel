import request from './request'

// ===================== 登录 =====================
export const login = (username, password, remember = false) =>
  request.post('/auth/login', null, { params: { username, password, remember } })
export const logout = () => request.post('/auth/logout')
export const me = () => request.get('/auth/me')
// 手机验证码：scene=login(须已注册)/register(须未注册)；remember 决定长效会话
export const smsSend = (phone, scene = 'login') =>
  request.post('/auth/sms/send', null, { params: { phone, scene } })
export const smsLogin = (phone, code, remember = false) =>
  request.post('/auth/sms/login', null, { params: { phone, code, remember } })
// 注册：手机验证码 + 设密码（username=phone，默认角色由后端配置）；重置：验证码 + 新密码
export const smsRegister = (phone, code, password, nickname = '') =>
  request.post('/auth/sms/register', null, { params: { phone, code, password, nickname } })
// 邮箱验证码：scene=login(须已注册)/register(须未注册)/reset
export const emailSend = (email, scene = 'login') =>
  request.post('/auth/email/send', null, { params: { email, scene } })
export const emailLogin = (email, code, remember = false) =>
  request.post('/auth/email/login', null, { params: { email, code, remember } })
export const emailRegister = (email, code, password, nickname = '') =>
  request.post('/auth/email/register', null, { params: { email, code, password, nickname } })
export const emailReset = (email, code, newPassword) =>
  request.post('/auth/email/reset', null, { params: { email, code, newPassword } })

export const smsReset = (phone, code, newPassword) =>
  request.post('/auth/sms/reset', null, { params: { phone, code, newPassword } })

// ===================== 物流 =====================
export const orderList = (params) => request.get('/logistics/order/list', { params })
export const orderDetail = (orderNo) => request.get(`/logistics/order/${orderNo}`)
export const track = (orderNo) => request.get(`/logistics/track/${orderNo}`)
export const logisticsNodes = () => request.get('/logistics/nodes')

// ===================== 通知 =====================
export const notifStats = () => request.get('/notification/stats')
export const notifList = (params) => request.get('/notification/list', { params })
export const notifSend = (params) => request.post('/notification/send', null, { params })
export const notifResend = (id) => request.post(/notification//resend)

// ===================== 工单 =====================
export const woStats = () => request.get('/workorder/stats')
export const woList = (params) => request.get('/workorder/list', { params })
export const woDetail = (id) => request.get(`/workorder/${id}`)
export const woProcess = (params) => request.post('/workorder/process', null, { params })
export const woPush = (workorderId) => request.post('/workorder/push', null, { params: { workorderId } })
export const woStatus = (id, status) => request.post(`/workorder/${id}/status`, null, { params: { status } })
export const woClaim = (id, params) => request.post(`/workorder/${id}/claim`, null, { params })
export const woDiagnose = (id) => request.post(`/workorder/${id}/diagnose`)

// ===================== Agent 直调 =====================
export const contentGenerate = (params) => request.post('/agent/content/generate', null, { params })
export const anomalyDiagnose = (params) => request.post('/agent/anomaly/diagnose', null, { params })
export const agentWorkorderProcess = (params) => request.post('/agent/workorder/process', null, { params })
export const csChat = (params) => request.post('/agent/cs/chat', null, { params })
export const csLastResult = () => request.get('/agent/cs/lastResult')
export const csHistory = () => request.get('/agent/cs/history')
export const agentChat = (data) => request.post('/agent/chat', data)
export const agentAnalytics = (data) => request.post('/agent/analytics', data)
export const agentOpsRun = (data) => request.post('/agent/ops/run', data)
export const agentOpsTasks = (params) => request.get('/agent/ops/tasks', { params })
export const agentOpsTrace = (traceId) => request.get(`/agent/ops/trace/${traceId}`)
export const agentOpsApprove = (callLogId, data) => request.post(`/agent/ops/steps/${callLogId}/approve`, data || {})
export const agentOpsReject = (callLogId, data) => request.post(`/agent/ops/steps/${callLogId}/reject`, data || {})
export const agentOpsHealth = () => request.get('/agent/ops/health')

// ===================== Agent 日志 =====================
export const agentStats = () => request.get('/agent/log/stats')
export const agentList = (params) => request.get('/agent/log/list', { params })
export const agentDetail = (id) => request.get(`/agent/log/${id}`)
export const agentTrace = (traceId) => request.get(`/agent/log/trace/${traceId}`)
export const agentTraceLatest = () => request.get('/agent/log/trace/latest')

// ===================== 看板 =====================
export const channelDistribution = () => request.get('/dashboard/channel-distribution')
export const agentTrend = () => request.get('/dashboard/agent-trend')

// ===================== 模板 =====================
export const templateList = (params) => request.get('/template/list', { params })
export const templateSave = (data) => request.post('/template/save', data)
export const templateDelete = (id) => request.delete(`/template/delete/${id}`)

// ===================== 渠道 =====================
export const channelList = (params) => request.get('/channel/list', { params })
export const channelSave = (data) => request.post('/channel/save', data)
export const channelDelete = (id) => request.delete(`/channel/delete/${id}`)

// ===================== 知识库 =====================
export const knowledgeList = (params) => request.get('/knowledge/list', { params })
export const knowledgeSave = (params) => request.post('/knowledge/save', null, { params })
export const knowledgeDelete = (id) => request.delete(`/knowledge/delete/${id}`)

// ===================== TMS 主数据 =====================
export const merchantList = (params) => request.get('/tms/merchant/list', { params })
export const merchantAll = () => request.get('/tms/merchant/all')
export const merchantSave = (data) => request.post('/tms/merchant', data)
export const merchantUpdate = (id, data) => request.put(`/tms/merchant/${id}`, data)
export const merchantDelete = (id) => request.delete(`/tms/merchant/${id}`)

export const carrierList = (params) => request.get('/tms/carrier/list', { params })
export const carrierAll = () => request.get('/tms/carrier/all')
export const carrierSave = (data) => request.post('/tms/carrier', data)
export const carrierUpdate = (id, data) => request.put(`/tms/carrier/${id}`, data)
export const carrierDelete = (id) => request.delete(`/tms/carrier/${id}`)

export const tmsChannelList = (params) => request.get('/tms/channel/list', { params })
export const tmsChannelAll = () => request.get('/tms/channel/all')
export const tmsChannelByCountry = (destCountry) => request.get('/tms/channel/by-country', { params: { destCountry } })
export const tmsChannelSave = (data) => request.post('/tms/channel', data)
export const tmsChannelUpdate = (id, data) => request.put(`/tms/channel/${id}`, data)
export const tmsChannelDelete = (id) => request.delete(`/tms/channel/${id}`)

export const rateList = (params) => request.get('/tms/rate/list', { params })
export const rateByChannel = (channelId) => request.get('/tms/rate/by-channel', { params: { channelId } })
export const rateSave = (data) => request.post('/tms/rate', data)
export const rateUpdate = (id, data) => request.put(`/tms/rate/${id}`, data)
export const rateDelete = (id) => request.delete(`/tms/rate/${id}`)

export const warehouseList = (params) => request.get('/tms/warehouse/list', { params })
export const warehouseAll = () => request.get('/tms/warehouse/all')
export const warehouseSave = (data) => request.post('/tms/warehouse', data)
export const warehouseUpdate = (id, data) => request.put(`/tms/warehouse/${id}`, data)
export const warehouseDelete = (id) => request.delete(`/tms/warehouse/${id}`)

export const productList = (params) => request.get('/tms/product/list', { params })
export const productByMerchant = (merchantId) => request.get('/tms/product/by-merchant', { params: { merchantId } })
export const productSave = (data) => request.post('/tms/product', data)
export const productUpdate = (id, data) => request.put(`/tms/product/${id}`, data)
export const productDelete = (id) => request.delete(`/tms/product/${id}`)

// ===================== TMS 履约闭环 =====================
export const tmsQuote = (data) => request.post('/tms/quote', data)
export const tmsQuoteCompare = (data) => request.post('/tms/quote/compare', data)
export const globalSearch = (keyword) => request.get('/tms/search', { params: { keyword } })
export const tmsOrderCreate = (data) => request.post('/tms/order/create', data)
export const tmsOrderList = (params) => request.get('/tms/order/list', { params })
export const tmsOrderDetail = (orderNo) => request.get(`/tms/order/${orderNo}`)
export const tmsOrderTracks = (orderNo) => request.get(`/tms/order/${orderNo}/tracks`)
export const tmsAdvance = (orderNo) => request.post(`/tms/order/${orderNo}/advance`)
export const tmsInjectAnomaly = (orderNo, type) => request.post(`/tms/order/${orderNo}/anomaly`, null, { params: { type } })
export const tmsUpdateOrder = (orderNo, data) => request.put(`/tms/order/${orderNo}`, data)
export const tmsCancelOrder = (orderNo) => request.post(`/tms/order/${orderNo}/cancel`)
// 出库生成运单：整单（默认）/ 分批（传 items 子集 JSON 字符串 + partial=true）
export const waybillGenerate = (orderNo, { items, partial } = {}) =>
  request.post('/tms/waybill/generate', null, { params: { orderNo, items: items || null, partial: !!partial } })
// 合并运单：同商家同目的地未出库已审核订单合并生成一张运单
export const tmsWaybillMerge = (orderNos) => request.post('/tms/waybill/merge', { orderNos })
export const waybillList = (params) => request.get('/tms/waybill/list', { params })
export const waybillDetail = (waybillNo) => request.get(`/tms/waybill/${waybillNo}`)
export const waybillTracks = (waybillNo) => request.get(`/tms/waybill/${waybillNo}/tracks`)

// ===================== TMS 计费结算 =====================
export const billGenerate = (params) => request.post('/tms/bill/generate', null, { params })
export const billSubmit = (id) => request.post(`/tms/bill/${id}/submit`)
export const billVerify = (id) => request.post(`/tms/bill/${id}/verify`)
export const billSettle = (id) => request.post(`/tms/bill/${id}/settle`)
export const billReject = (id, reason) => request.post(`/tms/bill/${id}/reject`, null, { params: { reason } })
export const billReopen = (id) => request.post(`/tms/bill/${id}/reopen`)
export const billList = (params) => request.get('/tms/bill/list', { params })
export const billDetail = (id) => request.get(`/tms/bill/${id}`)
export const billStats = () => request.get('/tms/bill/stats')
export const billMerchantSelf = () => request.get('/tms/bill/merchant/self')
export const billAddWaybill = (id, waybillNo) => request.post(`/tms/bill/${id}/add-waybill`, null, { params: { waybillNo } })

// ===================== TMS 看板 =====================
export const tmsOverview = () => request.get('/tms/dashboard/overview')
export const tmsOrderTrend = () => request.get('/tms/dashboard/order-trend')
export const tmsCarrierVolume = () => request.get('/tms/dashboard/carrier-volume')
export const sellerDashboard = (merchantId) => request.get('/tms/dashboard/seller', { params: { merchantId } })
export const channelMix = () => request.get('/tms/dashboard/channel-mix')
export const anomalyRate = () => request.get('/tms/dashboard/anomaly-rate')
export const settlement = () => request.get('/tms/dashboard/settlement')

// ===================== TMS AI =====================
export const routeAdvice = (params) => request.post('/tms/agent/route-advice', null, { params })
export const etaPredict = (params) => request.post('/tms/agent/eta-predict', null, { params })

// ===================== 操作审计 =====================
export const auditList = (params) => request.get('/audit/list', { params })

// ===================== 逆向售后 =====================
export const afterSaleList = (params) => request.get('/tms/after-sale/list', { params })
export const afterSaleDetail = (id) => request.get(`/tms/after-sale/${id}`)
export const afterSaleRegister = (data) => request.post('/tms/after-sale/register', data)
export const afterSaleRefund = (id) => request.post(`/tms/after-sale/${id}/refund`)
export const afterSaleReship = (id) => request.post(`/tms/after-sale/${id}/reship`)
export const afterSaleClose = (id) => request.post(`/tms/after-sale/${id}/close`)

// ===================== 理赔流程（售后·工单索赔的赔付闭环） =====================
export const claimStats = () => request.get('/tms/claim/stats')
export const claimList = (params) => request.get('/tms/claim/list', { params })
export const claimDetail = (id) => request.get(`/tms/claim/${id}`)
export const claimRegister = (data) => request.post('/tms/claim/register', data)
export const claimApprove = (id, data) => request.post(`/tms/claim/${id}/approve`, data)
export const claimPay = (id) => request.post(`/tms/claim/${id}/pay`)
export const claimReject = (id, data) => request.post(`/tms/claim/${id}/reject`, data)

// ===================== 风险预警 =====================
export const riskRuleList = () => request.get('/tms/risk/rule/list')
export const riskRuleSave = (data) => request.post('/tms/risk/rule', data)
export const riskRuleUpdate = (id, data) => request.put(`/tms/risk/rule/${id}`, data)
export const riskRuleDelete = (id) => request.delete(`/tms/risk/rule/${id}`)
export const riskOrders = (params) => request.get('/tms/risk/orders', { params })
export const riskExecute = (params) => request.post('/tms/risk/execute', null, { params })

// ===================== 库存台账 =====================
export const inventoryList = (params) => request.get('/tms/inventory/list', { params })
export const inventoryFlow = (params) => request.get('/tms/inventory/flow', { params })
export const inventoryAdjust = (data) => request.post('/tms/inventory/adjust', data)
export const stocktakeList = (params) => request.get('/tms/stocktake/list', { params })
export const stocktakeDetail = (id) => request.get(`/tms/stocktake/${id}`)
export const stocktakeCreate = (data) => request.post('/tms/stocktake/create', data)
export const stocktakeSaveCount = (id, data) => request.post(`/tms/stocktake/${id}/save-count`, data)
export const stocktakeFinish = (id) => request.post(`/tms/stocktake/${id}/finish`)
export const stocktakeCancel = (id) => request.post(`/tms/stocktake/${id}/cancel`)

// ===================== 开放 API =====================
export const apiKeyList = () => request.get('/tms/api-key/list')
export const apiKeyCreate = (data) => request.post('/tms/api-key/create', data)
export const apiKeyToggle = (id) => request.post(`/tms/api-key/${id}/toggle`)
export const apiKeyDelete = (id) => request.delete(`/tms/api-key/${id}`)

// ===================== 用户管理 =====================
export const userList = (params) => request.get('/sys/user/list', { params })
export const userRoles = () => request.get('/sys/user/roles')
export const userCreate = (data) => request.post('/sys/user/create', data)
export const userUpdate = (id, data) => request.put(`/sys/user/${id}`, data)
export const userResetPwd = (id, password) => request.post(`/sys/user/${id}/reset-password`, { password })
export const userToggle = (id) => request.post(`/sys/user/${id}/toggle`)

// ===================== 角色权限 =====================
export const roleList = () => request.get('/sys/role/list')
export const roleCreate = (data) => request.post('/sys/role/create', data)
export const roleUpdate = (id, data) => request.put(`/sys/role/${id}`, data)
export const roleToggle = (id) => request.post(`/sys/role/${id}/toggle`)
export const roleMenus = (code) => request.get(`/sys/role/${code}/menus`)
export const roleSaveMenus = (code, paths) => request.post(`/sys/role/${code}/menus`, { paths })
export const myPermissions = () => request.get('/sys/me/permissions')

// ===================== 订单审核 =====================
export const orderReviewList = (params) => request.get('/tms/order/review-list', { params })
export const orderReview = (orderNo, approve, reason) =>
  request.post(`/tms/order/${orderNo}/review`, null, { params: { approve, reason } })

// ===================== 差异对账 =====================
export const reconcileBills = () => request.get('/tms/reconcile/bills')
export const reconcileDetail = (billId) => request.get(`/tms/reconcile/${billId}`)
export const reconcileMark = (billId, waybillNo, reason) =>
  request.post(`/tms/reconcile/${billId}/mark`, null, { params: { waybillNo, reason } })

// ===================== 时效分析 =====================
export const slaOverview = (params) => request.get('/tms/sla/overview', { params })
export const slaByChannel = (params) => request.get('/tms/sla/by-channel', { params })
export const slaByCarrier = (params) => request.get('/tms/sla/by-carrier', { params })
export const slaTopDelay = (params) => request.get('/tms/sla/top-delay', { params })
