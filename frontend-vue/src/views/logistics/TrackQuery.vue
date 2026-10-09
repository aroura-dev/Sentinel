<template>
  <div class="tq-wrap">
    <!-- 左：可跟踪的在途/已发出对象 -->
    <el-card shadow="never" class="tq-side">
      <template #header>
        <div class="side-head">
          <b>可跟踪对象</b>
          <span class="side-sub">{{ shippedList.length }} 单已发出</span>
        </div>
      </template>
      <el-input v-model="kw" placeholder="按订单号 / 商家筛选" clearable size="small" class="side-filter" />
      <div v-loading="listLoading" class="tq-list">
        <div v-for="o in filtered" :key="o.order_no" class="tq-item" :class="{ active: o.order_no === orderNo }" @click="pick(o)">
          <div class="tq-no">{{ o.order_no }}</div>
          <div class="tq-sub">{{ o.merchant_name || '-' }} 发往 {{ countryLabel(o.destination_country) || '-' }}</div>
          <div class="tq-node"><StatusTag :status="o.current_node" /></div>
        </div>
        <el-empty v-if="!filtered.length && !listLoading" description="暂无在途订单" :image-size="60" />
      </div>
    </el-card>

    <!-- 右：物流轨迹 -->
    <el-card shadow="never" class="tq-main">
      <template #header>
        <div class="head-wrap"><b>物流轨迹</b><span class="head-tip">点左侧对象，或直接输入订单号查询</span></div>
      </template>
      <div class="tq-query">
        <el-input v-model="orderNo" placeholder="输入订单号" clearable style="width: 260px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询轨迹</el-button>
      </div>

      <div class="tq-scroll">
        <el-timeline v-if="tracks.length">
          <el-timeline-item
            v-for="(t, i) in tracks"
            :key="i"
            :timestamp="formatTime(t.track_time)"
            :color="nodeMeta(t.node).color"
            :hollow="i !== tracks.length - 1"
            placement="top"
          >
            <div class="track-node" :class="{ anomaly: nodeMeta(t.node).anomaly }">
              <el-icon :size="15" class="tk-ic"><component :is="nodeMeta(t.node).icon" /></el-icon>{{ nodeMeta(t.node).label }}
              <el-tag v-if="i === tracks.length - 1" size="small" type="primary" effect="light" class="latest-tag">最新</el-tag>
            </div>
            <div class="track-desc">{{ t.raw_desc }}</div>
            <div class="track-loc">{{ t.location }}</div>
          </el-timeline-item>
        </el-timeline>
        <!-- 未选择任何订单：温馨引导 -->
        <div v-else-if="!orderNo" class="track-guide">
          <el-icon :size="30" class="tg-ic"><Van /></el-icon>
          <div class="tg-title">想看看货到哪了？</div>
          <div class="tg-sub">点左侧的在途运单，或在上面输入订单号，就能看到它从发出到送达的每一步物流轨迹。</div>
        </div>
        <el-empty v-else description="这张运单暂时还没有物流轨迹" :image-size="80" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import { track, tmsOrderList } from '../../api'
import { countryLabel } from '../../utils/country'

const route = useRoute()
const orderNo = ref(route.query.orderNo || '')
const tracks = ref([])
const all = ref([])
const listLoading = ref(false)
const kw = ref('')

// 已发出（有运单）的订单即为可跟踪对象，去重并保持顺序
const shippedList = computed(() => {
  const seen = new Set()
  const out = []
  for (const o of all.value) {
    if (!o.waybill_no || o.current_node === 'CANCELED') continue
    if (seen.has(o.order_no)) continue
    seen.add(o.order_no)
    out.push(o)
  }
  return out
})

const filtered = computed(() => {
  const k = (kw.value || '').trim().toLowerCase()
  if (!k) return shippedList.value
  return shippedList.value.filter((o) =>
    o.order_no.toLowerCase().includes(k) || (o.merchant_name || '').toLowerCase().includes(k)
  )
})

onMounted(async () => {
  await loadList()
  if (orderNo.value) load()
})

async function loadList() {
  listLoading.value = true
  try {
    const r = await tmsOrderList({ page: 1, perPage: 500 })
    all.value = r.rows || []
  } finally { listLoading.value = false }
}

function pick(o) {
  orderNo.value = o.order_no
  load()
}

// 清空订单号 → 轨迹一并清空，回到温馨引导
watch(orderNo, (v) => {
  if (!v) tracks.value = []
})

async function load() {
  if (!orderNo.value) { tracks.value = []; return }
  tracks.value = (await track(orderNo.value)) || []
}

function formatTime(t) {
  if (!t) return ''
  // 直接截取 "YYYY-MM-DD HH:MM:SS"，避免 new Date 对空格分隔格式解析出 Invalid Date
  return String(t).slice(0, 19).replace('T', ' ')
}

const NODE_META = {
  CREATED: { label: '已创建', icon: 'Document', color: '#909399' },
  WAREHOUSE_OUT: { label: '已出库', icon: 'Box', color: '#909399' },
  DOMESTIC_PICKED: { label: '揽收', icon: 'Van', color: '#909399' },
  EXPORT_CUSTOMS: { label: '中转分拨', icon: 'Connection', color: '#909399' },
  IN_TRANSIT: { label: '干线运输', icon: 'Van', color: '#909399' },
  IMPORT_CUSTOMS: { label: '到达分拨', icon: 'Connection', color: '#909399' },
  LAST_MILE: { label: '末端派送', icon: 'Location', color: '#909399' },
  DELIVERED: { label: '已签收', icon: 'CircleCheck', color: '#67c23a' },
  CUSTOMS_DELAY: { label: '中转延误', icon: 'Warning', color: '#f56c6c', anomaly: true },
  DELIVERY_FAILED: { label: '派送失败', icon: 'Warning', color: '#f56c6c', anomaly: true },
  LOST: { label: '丢件', icon: 'CircleClose', color: '#f56c6c', anomaly: true },
  RETURNED: { label: '已退回', icon: 'RefreshLeft', color: '#e6a23c', anomaly: true }
}
const nodeMeta = (node) => NODE_META[node] || { label: node, icon: 'Location', color: '#909399', anomaly: false }
</script>

<style scoped>
/* 左右两栏等高，各自内部滚动：右侧轨迹滚动不带动左侧 */
.tq-wrap { display: flex; gap: 16px; align-items: stretch; }
.tq-side, .tq-main {
  height: calc(100vh - 236px);
  min-height: 400px;
  display: flex;
  flex-direction: column;
}
.tq-side { width: 380px; flex: none; }
.tq-main { flex: 1; min-width: 0; }
.tq-side :deep(.el-card__body),
.tq-main :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.side-head { display: flex; align-items: center; gap: 8px; }
.side-sub { font-size: 12px; color: #9ca3af; font-weight: 400; }
.side-filter { margin-bottom: 10px; flex: none; }
.tq-list { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.tq-item {
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
}
.tq-item:hover { background: #f6fafb; }
.tq-item.active { background: #eef8fa; border-color: #b7e3ea; }
.tq-no { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 13px; color: #1d2129; }
.tq-sub { font-size: 12px; color: #57606a; margin-top: 2px; }
.tq-node { margin-top: 6px; }
.head-wrap { display: flex; align-items: center; gap: 10px; }
.head-tip { font-size: 12px; color: #9ca3af; font-weight: 400; }
.tq-query { flex: none; margin-bottom: 14px; display: flex; gap: 10px; }
/* 右侧轨迹独立滚动区 */
.tq-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 2px 8px 4px 2px; }
/* 未选择订单时的温馨引导 */
.track-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  padding: 40px 30px;
  color: #0e7490;
}
.tg-ic { color: #0891b2; opacity: .75; }
.tg-title { margin-top: 14px; font-size: 15px; font-weight: 600; color: #1d2129; }
.tg-sub { margin-top: 8px; font-size: 13px; line-height: 1.8; color: #6b7280; max-width: 300px; }
/* 轨迹文字：正常字体、清晰排版 */
.track-node {
  font-weight: 600;
  font-size: 14px;
  color: #1d2129;
  display: flex;
  align-items: center;
  font-family: inherit;
}
.track-node.anomaly { color: #d4380d; }
.tk-ic { margin-right: 5px; flex: none; }
.latest-tag { margin-left: 6px; }
.track-desc { font-size: 13px; color: #4b5563; margin-top: 4px; line-height: 1.7; }
.track-loc { font-size: 12px; color: #9ca3af; margin-top: 3px; }
@media (max-width: 1000px) {
  .tq-wrap { flex-direction: column; }
  .tq-side { width: 100%; height: auto; }
  .tq-main { width: 100%; }
}
</style>
