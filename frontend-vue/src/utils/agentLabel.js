// Agent 日志 / 链路追踪的“人话”翻译层
// 把英文 Agent 名、状态、Tools、TraceId 原始串转成业务人员可读的中文展示

const AGENT_LABELS = {
  java_planner: '总控规划（Java）',
  java_diagnose: '异常诊断（Java）',
  java_content: '文案生成（Java）',
  java_ops: '编排执行（Java）',
  java_ops_summary: '运营摘要（Java）',
  JavaKnowledgeAgent: '知识问答智能体',
  JavaAnalyticsAgent: '运营分析智能体',
  ContentGenAgent: '文案生成智能体',
  AnomalyDiagnoseAgent: '异常诊断智能体',
  WorkorderAgent: '工单处理智能体',
  ChannelRouteAgent: '渠道选择智能体',
  UnsubscribePredictAgent: '退订预测智能体',
  CsRouteAgent: '客服路由智能体',
  RouteAdviceAgent: '运输方案智能体',
  EtaPredictAgent: '时效预测智能体'
}

const TOOL_LABELS = {
  queryKnowledge: '查询知识库',
  getExistingWorkorder: '查询已有工单',
  createWorkorder: '创建问题工单',
  notify: '发送通知',
  queryOrder: '查询订单',
  queryTrack: '查询物流轨迹'
}

const STATUS_META = {
  success: { text: '成功', type: 'success' },
  degraded: { text: '降级', type: 'warning' },
  pending_approval: { text: '待审批', type: 'warning' },
  failed: { text: '失败', type: 'danger' },
  timeout: { text: '超时', type: 'info' }
}

const NODE_LABELS = {
  CREATED: '已创建',
  WAREHOUSE_OUT: '仓库出库',
  DOMESTIC_PICKED: '国内揽收',
  EXPORT_CUSTOMS: '中转分拨',
  IN_TRANSIT: '干线运输',
  IMPORT_CUSTOMS: '到达分拨',
  LAST_MILE: '末端派送',
  DELIVERED: '已签收',
  CUSTOMS_DELAY: '分拨滞留',
  DELIVERY_FAILED: '派送失败',
  LOST: '丢件',
  RETURNED: '退回',
  CANCELED: '已取消'
}

const ROLE_LABELS = {
  buyer: '买家',
  merchant: '商家',
  cs: '客服',
  customer_service: '客服'
}

const LANG_LABELS = {
  zh: '中文',
}

export function agentLabel(name) {
  if (!name) return '-'
  return AGENT_LABELS[name] || name
}

export function statusMeta(status) {
  return STATUS_META[status] || { text: status || '-', type: 'info' }
}

export function toolNames(tools) {
  if (!tools) return []
  return String(tools)
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
    .map((t) => TOOL_LABELS[t] || t)
}

/**
 * 将原始 trace_id（形如 OMT-CA-0001|CUSTOMS_DELAY|zh|buyer）转成可读描述：
 * 订单 OMT-CA-0001 · 分拨滞留 · 买家（中文）
 */
export function friendlyTraceId(traceId) {
  if (!traceId) return '-'
  const s = String(traceId)
  const parts = s.split('|')
  if (parts.length < 2) return s
  const orderNo = parts[0]
  const node = NODE_LABELS[parts[1]] || parts[1]
  const lang = LANG_LABELS[parts[2]]
  const role = ROLE_LABELS[parts[3]]
  const segs = []
  if (orderNo) segs.push(`订单 ${orderNo}`)
  segs.push(node)
  if (role) segs.push(role)
  if (lang) segs.push(`（${lang}）`)
  return segs.join(' · ')
}
export const AGENT_OPTIONS = Object.entries(AGENT_LABELS).map(([code, label]) => ({ code, label }))
