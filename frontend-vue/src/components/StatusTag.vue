<template>
  <el-tag :type="tagType" size="small" effect="light">{{ display }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: [String, Number], default: '' },
  map: { type: Object, default: null }
})

const DEFAULT_MAP = {
  // 物流节点
  CREATED: 'info', WAREHOUSE_OUT: 'info', DOMESTIC_PICKED: 'info', EXPORT_CUSTOMS: 'info',
  IN_TRANSIT: 'primary', IMPORT_CUSTOMS: 'primary', LAST_MILE: 'primary', DELIVERED: 'success',
  CUSTOMS_DELAY: 'warning', DELIVERY_FAILED: 'danger', LOST: 'danger', RETURNED: 'warning',
  // SLA
  NORMAL: 'success', RISK: 'warning', BREACHED: 'danger', NA: 'info',
  // 工单
  OPEN: 'danger', PROCESSING: 'warning', RESOLVED: 'success', CLOSED: 'info', PUSHED: 'primary',
  // 账单
  DRAFT: 'info', SUBMITTED: 'primary', VERIFIED: 'warning', SETTLED: 'success', REJECTED: 'danger',
  // 通知
  PENDING: 'warning', SENT: 'success', FAILED: 'danger',
  // 运单
  ACTIVE: 'primary', CANCELED: 'info',
  // 通用状态
  1: 'success', 0: 'info'
}

const NODE_LABEL = {
  // 物流节点
  CREATED: '已下单', WAREHOUSE_OUT: '仓库出库', DOMESTIC_PICKED: '揽收', EXPORT_CUSTOMS: '中转分拨',
  IN_TRANSIT: '干线运输', IMPORT_CUSTOMS: '到达分拨', LAST_MILE: '末端派送', DELIVERED: '已签收',
  CUSTOMS_DELAY: '中转延误', DELIVERY_FAILED: '派送失败', LOST: '丢件', RETURNED: '退回',
  // SLA / 工单 / 账单 / 通知 / 运单 / 通用
  NORMAL: '正常', RISK: '预警', BREACHED: '违约', NA: '—',
  OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭', PUSHED: '已推送',
  DRAFT: '草稿', SUBMITTED: '已提交', VERIFIED: '已核销', SETTLED: '已结算', REJECTED: '已驳回',
  PENDING: '待发送', SENT: '已发送', FAILED: '失败', ACTIVE: '在途', CANCELED: '已取消',
  1: '启用', 0: '停用'
}

const tagType = computed(() => (props.map || DEFAULT_MAP)[String(props.status)] || 'info')
const display = computed(() => NODE_LABEL[String(props.status ?? '')] ?? String(props.status ?? '-'))
</script>
