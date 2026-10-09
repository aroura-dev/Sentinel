<template>
  <div v-if="cards.length" class="stats-strip">
    <!-- ElOnlyChild：el-tooltip 默认插槽只允许一个根节点，标签/数值需包在一个容器内 -->
    <div v-for="c in cards" :key="c.label" class="stat-card-wrap">
      <el-tooltip :content="c.tip" placement="top" :disabled="!c.tip">
        <div class="stat-card" :class="{ clickable: c.to, alert: c.alert }" @click="go(c.to)">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value" :class="{ 'text-alert': c.alert }">{{ c.value }}</div>
        </div>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
// 模块统计看板：复用 tmsOverview 聚合字段；无配置的模块不渲染。
// 卡片可点击跳转到对应筛选视图（带 query 参数），SLA风险/异常数字标红告警并带悬浮说明。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { tmsOverview } from '../api'

const props = defineProps({ module: { type: Object, default: null } })
const data = ref({})
const router = useRouter()

const CARDS = {
  // 订单模块：在途运输 / SLA 风险 / 待处理工单 / 异常单 四张统计卡已按要求移除
  order: [],
  // 运单模块：在途运单 / SLA 违约 两张统计卡已按要求移除
  waybill: [],
  // 售后模块：与订单/运单一致，不再叠加模块级统计卡；各子页自带单行业务概览
  'after-sale': []
}

const cards = computed(() => {
  const cfg = CARDS[props.module?.index]
  if (!cfg) return []
  return cfg.map((c) => ({ ...c, value: c.get(data.value) ?? '-' }))
})

function go(to) { if (to) router.push(to) }

onMounted(async () => {
  if (!CARDS[props.module?.index]) return
  try { data.value = await tmsOverview() } catch { /* 无权限/接口异常忽略 */ }
})
</script>

<style scoped>
.stats-strip {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card-wrap {
  flex: 1;
  min-width: 0;
}
.stat-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 14px 20px;
  cursor: default;
}
.stat-card.clickable {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, border-color 0.15s;
}
.stat-card.clickable:hover {
  transform: translateY(-1px);
  border-color: #0891b2;
  box-shadow: 0 3px 10px rgba(29, 33, 41, 0.06);
}
.stat-label {
  font-size: 13px;
  color: #86909c;
}
.stat-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}
/* SLA 风险 / 异常单：数字标红告警 */
.stat-value.text-alert {
  color: #ef4444;
}
</style>
