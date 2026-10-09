<template>
  <el-timeline v-if="tracks && tracks.length">
    <el-timeline-item v-for="(t, i) in tracks" :key="i" :timestamp="time(t.track_time)" placement="top"
      :type="typeOf(t.node)">
      <div class="nt-card">
        <div class="nt-node"><StatusTag :status="t.node" /></div>
        <div class="nt-desc">{{ t.raw_desc || '-' }}</div>
        <div class="nt-loc">{{ t.location || '' }}</div>
      </div>
    </el-timeline-item>
  </el-timeline>
  <el-empty v-else description="暂无轨迹" :image-size="60" />
</template>

<script setup>
import StatusTag from './StatusTag.vue'

defineProps({
  tracks: { type: Array, default: () => [] }
})

function time(v) {
  if (!v) return ''
  return String(v).slice(0, 19)
}
function typeOf(node) {
  if (['DELIVERED'].includes(node)) return 'success'
  if (['CUSTOMS_DELAY', 'DELIVERY_FAILED', 'LOST', 'RETURNED'].includes(node)) return 'danger'
  if (['IN_TRANSIT', 'IMPORT_CUSTOMS', 'LAST_MILE'].includes(node)) return 'primary'
  return 'info'
}
</script>

<style scoped>
.nt-card { background: #f7f8fa; border-radius: 8px; padding: 8px 12px; }
.nt-node { display: flex; align-items: center; gap: 8px; }
.nt-desc { margin-top: 4px; font-size: 13px; color: #111827; }
.nt-loc { margin-top: 2px; font-size: 12px; color: #9ca3af; }
</style>
